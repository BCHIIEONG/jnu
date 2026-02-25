<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { apiData, downloadBlob, fetchBlob, uploadFormData } from '../../api/http'
import { useAuthStore } from '../../stores/auth'
import UiModeToggle from '../common/UiModeToggle.vue'
import { useUiStore } from '../../stores/ui'

type TaskVO = {
  id: number
  title: string
  description?: string
  publisherName?: string
  deadlineAt?: string | null
  status?: string
  createdAt?: string
}

type SubmissionVO = {
  id: number
  taskId: number
  versionNo: number
  contentMd: string
  submittedAt: string
}

type AttachmentVO = {
  id: number
  submissionId: number
  fileName: string
  fileSize: number
  contentType?: string | null
  uploadedAt: string
}

type ReviewVO = {
  id: number
  submissionId: number
  teacherDisplayName?: string
  score: number
  comment?: string | null
  reviewedAt: string
}

const auth = useAuthStore()
const ui = useUiStore()
const router = useRouter()

const isMobile = computed(() => ui.effectiveMode === 'mobile')
const mobilePage = ref<'list' | 'task'>('list')

const tasks = ref<TaskVO[]>([])
const loadingTasks = ref(false)
const selectedTaskId = ref<number | null>(null)
const taskDetail = ref<TaskVO | null>(null)

const mySubmissions = ref<SubmissionVO[]>([])
const loadingSubs = ref(false)

const submitMd = ref('# 实验报告\n内容')
const submitting = ref(false)

const reviewDialogOpen = ref(false)
const currentReview = ref<ReviewVO | null>(null)
const loadingReview = ref(false)

const attachDialogOpen = ref(false)
const attachTarget = ref<SubmissionVO | null>(null)
const attachments = ref<AttachmentVO[]>([])
const loadingAttachments = ref(false)
const uploading = ref(false)
const uploadFile = ref<File | null>(null)

const previewDialog = ref(false)
const previewUrl = ref<string | null>(null)
const previewTitle = ref('')

async function loadTasks() {
  loadingTasks.value = true
  try {
    tasks.value = await apiData<TaskVO[]>('/api/tasks', { method: 'GET' }, auth.token)
    if (!isMobile.value && tasks.value.length > 0 && selectedTaskId.value === null) {
      const first = tasks.value[0]
      if (first) await selectTask(first.id)
    }
  } finally {
    loadingTasks.value = false
  }
}

async function selectTask(taskId: number) {
  selectedTaskId.value = taskId
  taskDetail.value = await apiData<TaskVO>(`/api/tasks/${taskId}`, { method: 'GET' }, auth.token)
  await loadMySubmissions()
}

async function openTaskMobile(taskId: number) {
  await selectTask(taskId)
  mobilePage.value = 'task'
}

function backToListMobile() {
  mobilePage.value = 'list'
  selectedTaskId.value = null
  taskDetail.value = null
  mySubmissions.value = []
}

async function loadMySubmissions() {
  if (!selectedTaskId.value) return
  loadingSubs.value = true
  try {
    mySubmissions.value = await apiData<SubmissionVO[]>(
      `/api/tasks/${selectedTaskId.value}/submissions/me`,
      { method: 'GET' },
      auth.token,
    )
  } finally {
    loadingSubs.value = false
  }
}

async function submit() {
  if (!selectedTaskId.value) return
  if (!submitMd.value.trim()) {
    ElMessage.warning('请输入报告内容')
    return
  }
  submitting.value = true
  try {
    await apiData(
      `/api/tasks/${selectedTaskId.value}/submissions`,
      { method: 'POST', body: { contentMd: submitMd.value } },
      auth.token,
    )
    ElMessage.success('提交成功')
    await loadMySubmissions()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '提交失败')
  } finally {
    submitting.value = false
  }
}

async function viewReview(submissionId: number) {
  reviewDialogOpen.value = true
  currentReview.value = null
  loadingReview.value = true
  try {
    currentReview.value = await apiData<ReviewVO>(`/api/submissions/${submissionId}/review`, { method: 'GET' }, auth.token)
  } catch (e: any) {
    ElMessage.warning(e?.message ?? '尚未批阅')
  } finally {
    loadingReview.value = false
  }
}

async function openAttachments(row: SubmissionVO) {
  attachTarget.value = row
  attachDialogOpen.value = true
  uploadFile.value = null
  await loadAttachments(row.id)
}

async function loadAttachments(submissionId: number) {
  loadingAttachments.value = true
  try {
    attachments.value = await apiData<AttachmentVO[]>(
      `/api/submissions/${submissionId}/attachments`,
      { method: 'GET' },
      auth.token,
    )
  } catch (e: any) {
    attachments.value = []
    ElMessage.error(e?.message ?? '加载附件失败')
  } finally {
    loadingAttachments.value = false
  }
}

function onPickFile(ev: Event) {
  const input = ev.target as HTMLInputElement
  const f = input.files?.[0] ?? null
  uploadFile.value = f
}

async function uploadAttachment() {
  if (!attachTarget.value) return
  if (!uploadFile.value) {
    ElMessage.warning('请选择文件')
    return
  }
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', uploadFile.value)
    await uploadFormData(`/api/submissions/${attachTarget.value.id}/attachments`, { token: auth.token, formData: fd })
    ElMessage.success('上传成功')
    uploadFile.value = null
    await loadAttachments(attachTarget.value.id)
  } catch (e: any) {
    ElMessage.error(e?.message ?? '上传失败')
  } finally {
    uploading.value = false
  }
}

async function downloadAttachment(row: AttachmentVO) {
  try {
    await downloadBlob(`/api/attachments/${row.id}/download`, {
      token: auth.token,
      fallbackFilename: row.fileName || `attachment-${row.id}`,
    })
  } catch (e: any) {
    ElMessage.error(e?.message ?? '下载失败')
  }
}

function closePreview() {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = null
  previewTitle.value = ''
  previewDialog.value = false
}

async function previewAttachment(row: AttachmentVO) {
  const ct = row.contentType ?? ''
  if (!ct.startsWith('image/')) {
    ElMessage.info('暂只支持图片在线预览，请下载查看')
    return
  }
  try {
    closePreview()
    const { blob } = await fetchBlob(`/api/attachments/${row.id}/download`, { token: auth.token })
    previewUrl.value = URL.createObjectURL(blob)
    previewTitle.value = row.fileName
    previewDialog.value = true
  } catch (e: any) {
    ElMessage.error(e?.message ?? '预览失败')
  }
}

function logout() {
  auth.logout()
  router.replace('/login')
}

onMounted(loadTasks)
</script>

<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="brand">学生端</div>
      <div class="right">
        <span class="user">{{ auth.displayName }}</span>
        <UiModeToggle />
        <el-button size="small" @click="logout">退出</el-button>
      </div>
    </el-header>

    <el-main v-if="isMobile" class="main">
      <div v-if="mobilePage === 'list'">
        <div class="mobileTitle">实验任务</div>
        <el-card
          v-for="t in tasks"
          :key="t.id"
          shadow="never"
          class="taskCard"
          @click="openTaskMobile(t.id)"
        >
          <div class="taskRow">
            <div class="taskName">{{ t.title }}</div>
            <el-tag size="small">{{ t.status ?? 'OPEN' }}</el-tag>
          </div>
          <div class="meta">截止：{{ t.deadlineAt || '-' }}</div>
        </el-card>
        <div v-if="loadingTasks" class="meta">加载中...</div>
        <div v-else-if="tasks.length === 0" class="meta">暂无任务</div>
      </div>

      <div v-else>
        <div class="mobileTop">
          <el-button size="small" @click="backToListMobile">返回任务列表</el-button>
          <el-button size="small" @click="loadTasks" :loading="loadingTasks">刷新任务</el-button>
        </div>

        <el-card v-if="taskDetail" class="block" shadow="never">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center; gap: 10px">
              <div style="font-weight: 700">{{ taskDetail.title }}</div>
              <div class="meta">发布：{{ taskDetail.publisherName || '-' }}</div>
            </div>
          </template>
          <div class="meta" style="margin-bottom: 10px">截止：{{ taskDetail.deadlineAt || '-' }}</div>
          <div style="white-space: pre-wrap">{{ taskDetail.description || '（无说明）' }}</div>
        </el-card>

        <el-collapse accordion class="block">
          <el-collapse-item title="提交报告" name="submit">
            <div style="display: flex; justify-content: flex-end; margin-bottom: 10px">
              <el-button type="primary" size="small" :loading="submitting" @click="submit">提交</el-button>
            </div>
            <el-input v-model="submitMd" type="textarea" :rows="10" placeholder="Markdown 内容" />
          </el-collapse-item>

          <el-collapse-item title="我的提交" name="subs">
            <div style="display: flex; justify-content: flex-end; margin-bottom: 10px">
              <el-button size="small" @click="loadMySubmissions" :loading="loadingSubs">刷新</el-button>
            </div>
            <el-card v-for="s in mySubmissions" :key="s.id" shadow="never" class="subCard">
              <div class="taskRow">
                <div>v{{ s.versionNo }}</div>
                <div class="meta">{{ s.submittedAt }}</div>
              </div>
              <div style="display: flex; gap: 10px; margin-top: 8px; flex-wrap: wrap">
                <el-button size="small" @click="viewReview(s.id)">查看批阅</el-button>
                <el-button size="small" @click="openAttachments(s)">附件</el-button>
              </div>
            </el-card>
            <div v-if="loadingSubs" class="meta">加载中...</div>
            <div v-else-if="mySubmissions.length === 0" class="meta">暂无提交</div>
          </el-collapse-item>
        </el-collapse>
      </div>
    </el-main>

    <el-container v-else>
      <el-aside width="360px" class="aside">
        <div class="aside-title">实验任务</div>
        <el-table
          :data="tasks"
          size="small"
          height="calc(100vh - 130px)"
          v-loading="loadingTasks"
          @row-click="(row: TaskVO) => selectTask(row.id)"
          :row-class-name="({ row }: { row: TaskVO }) => (row.id === selectedTaskId ? 'is-active' : '')"
        >
          <el-table-column prop="title" label="标题" />
          <el-table-column prop="status" label="状态" width="90" />
        </el-table>
      </el-aside>
      <el-main class="main">
        <el-card v-if="taskDetail" class="block" shadow="never">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <div>{{ taskDetail.title }}</div>
              <div class="meta">发布：{{ taskDetail.publisherName || '-' }}</div>
            </div>
          </template>
          <div class="meta" style="margin-bottom: 10px">截止：{{ taskDetail.deadlineAt || '-' }}</div>
          <div style="white-space: pre-wrap">{{ taskDetail.description || '（无说明）' }}</div>
        </el-card>

        <el-card class="block" shadow="never" v-if="selectedTaskId">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <div>提交报告</div>
              <el-button type="primary" size="small" :loading="submitting" @click="submit">提交</el-button>
            </div>
          </template>
          <el-input v-model="submitMd" type="textarea" :rows="8" placeholder="Markdown 内容" />
        </el-card>

        <el-card class="block" shadow="never" v-if="selectedTaskId">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <div>我的提交</div>
              <el-button size="small" @click="loadMySubmissions" :loading="loadingSubs">刷新</el-button>
            </div>
          </template>
          <el-table :data="mySubmissions" size="small" v-loading="loadingSubs">
            <el-table-column prop="versionNo" label="版本" width="80" />
            <el-table-column prop="submittedAt" label="提交时间" min-width="180" />
            <el-table-column label="操作" width="220">
              <template #default="{ row }: { row: SubmissionVO }">
                <el-button size="small" @click="viewReview(row.id)">查看批阅</el-button>
                <el-button size="small" @click="openAttachments(row)">附件</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-main>
    </el-container>
  </el-container>

  <el-dialog v-model="reviewDialogOpen" title="批阅结果" width="520px">
    <div v-if="loadingReview">加载中...</div>
    <div v-else-if="!currentReview">暂无批阅</div>
    <div v-else>
      <div class="meta">教师：{{ currentReview.teacherDisplayName || '-' }}</div>
      <div class="meta">时间：{{ currentReview.reviewedAt }}</div>
      <div style="margin-top: 10px; font-size: 16px">
        分数：<b>{{ currentReview.score }}</b>
      </div>
      <div style="margin-top: 10px; white-space: pre-wrap">评语：{{ currentReview.comment || '（无）' }}</div>
    </div>
  </el-dialog>

  <el-dialog v-model="attachDialogOpen" title="附件管理" width="900px" @closed="closePreview">
    <div v-if="attachTarget" class="meta" style="margin-bottom: 10px; display: flex; justify-content: space-between">
      <div>提交：v{{ attachTarget.versionNo }}（ID: {{ attachTarget.id }}）</div>
      <div>
        <el-button size="small" @click="loadAttachments(attachTarget.id)" :loading="loadingAttachments">刷新</el-button>
      </div>
    </div>

    <el-card shadow="never" style="margin-bottom: 12px">
      <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap">
        <input type="file" @change="onPickFile" />
        <el-button type="primary" size="small" :loading="uploading" @click="uploadAttachment">上传</el-button>
        <div class="meta">支持图片/文件；老师端可下载与预览图片。</div>
      </div>
    </el-card>

    <el-table :data="attachments" size="small" v-loading="loadingAttachments">
      <el-table-column prop="fileName" label="文件名" min-width="260" />
      <el-table-column prop="fileSize" label="大小(Byte)" width="120" />
      <el-table-column prop="uploadedAt" label="上传时间" min-width="180" />
      <el-table-column label="操作" width="180">
        <template #default="{ row }: { row: AttachmentVO }">
          <el-button size="small" @click="downloadAttachment(row)">下载</el-button>
          <el-button size="small" @click="previewAttachment(row)">预览</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="meta" style="margin-top: 6px">目前仅支持图片在线预览（其他类型请下载）。</div>
  </el-dialog>

  <el-dialog v-model="previewDialog" :title="previewTitle || '附件预览'" width="900px" @closed="closePreview">
    <div v-if="!previewUrl">加载中...</div>
    <img v-else :src="previewUrl" style="max-width: 100%; max-height: 70vh; display: block; margin: 0 auto" />
  </el-dialog>
</template>

<style scoped>
.layout {
  min-height: 100vh;
  min-height: 100dvh;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #eee;
  background: #fff;
}
.brand {
  font-weight: 700;
}
.right {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
.user {
  color: #333;
}
.aside {
  border-right: 1px solid #eee;
  background: #fff;
  padding: 10px;
}
.aside-title {
  font-weight: 700;
  margin-bottom: 8px;
}
.main {
  padding: 14px;
}
.block {
  margin-bottom: 14px;
}
.meta {
  color: #666;
  font-size: 12px;
}
.mobileTitle {
  font-weight: 800;
  font-size: 18px;
  margin: 6px 0 10px;
}
.taskCard {
  margin-bottom: 10px;
  cursor: pointer;
}
.subCard {
  margin-bottom: 10px;
}
.taskRow {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}
.taskName {
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mobileTop {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 10px;
  flex-wrap: wrap;
}
:deep(.el-table .is-active td) {
  background: #eef6ff !important;
}
</style>
