<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { apiData, downloadBlob, fetchBlob } from '../../api/http'
import { useAuthStore } from '../../stores/auth'

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
  studentUsername: string
  studentDisplayName: string
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

const auth = useAuthStore()
const router = useRouter()

const tasks = ref<TaskVO[]>([])
const loadingTasks = ref(false)
const selectedTaskId = ref<number | null>(null)
const taskDetail = ref<TaskVO | null>(null)

const submissions = ref<SubmissionVO[]>([])
const loadingSubs = ref(false)

const createDialog = ref(false)
const createForm = reactive({
  title: '',
  description: '',
})
const creating = ref(false)

const reviewDialog = ref(false)
const reviewTarget = ref<SubmissionVO | null>(null)
const reviewForm = reactive({
  score: 95,
  comment: '完成很好',
})
const reviewing = ref(false)

const reportDialog = ref(false)
const reportTarget = ref<SubmissionVO | null>(null)
const attachments = ref<AttachmentVO[]>([])
const loadingAttachments = ref(false)

const previewDialog = ref(false)
const previewUrl = ref<string | null>(null)
const previewTitle = ref('')

function logout() {
  auth.logout()
  router.replace('/login')
}

async function loadTasks() {
  loadingTasks.value = true
  try {
    tasks.value = await apiData<TaskVO[]>('/api/tasks', { method: 'GET' }, auth.token)
    if (tasks.value.length > 0 && selectedTaskId.value === null) {
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
  await loadSubmissions()
}

async function loadSubmissions() {
  if (!selectedTaskId.value) return
  loadingSubs.value = true
  try {
    submissions.value = await apiData<SubmissionVO[]>(
      `/api/tasks/${selectedTaskId.value}/submissions`,
      { method: 'GET' },
      auth.token,
    )
  } finally {
    loadingSubs.value = false
  }
}

async function createTask() {
  if (!createForm.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  creating.value = true
  try {
    await apiData('/api/tasks', { method: 'POST', body: { title: createForm.title, description: createForm.description } }, auth.token)
    ElMessage.success('任务创建成功')
    createDialog.value = false
    createForm.title = ''
    createForm.description = ''
    await loadTasks()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '创建失败')
  } finally {
    creating.value = false
  }
}

function openReview(row: SubmissionVO) {
  reviewTarget.value = row
  reviewDialog.value = true
}

async function openReport(row: SubmissionVO) {
  reportTarget.value = row
  reportDialog.value = true
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

async function downloadReportText() {
  if (!reportTarget.value) return
  try {
    await downloadBlob(`/api/submissions/${reportTarget.value.id}/content/download`, {
      token: auth.token,
      fallbackFilename: `submission-${reportTarget.value.id}.md`,
    })
    ElMessage.success('已开始下载文本')
  } catch (e: any) {
    ElMessage.error(e?.message ?? '下载失败')
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

async function submitReview() {
  if (!reviewTarget.value) return
  reviewing.value = true
  try {
    await apiData(
      `/api/submissions/${reviewTarget.value.id}/review`,
      { method: 'POST', body: { score: reviewForm.score, comment: reviewForm.comment } },
      auth.token,
    )
    ElMessage.success('批阅成功')
    reviewDialog.value = false
    await loadSubmissions()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '批阅失败')
  } finally {
    reviewing.value = false
  }
}

async function exportCsv() {
  if (!selectedTaskId.value) return
  try {
    await downloadBlob(`/api/tasks/${selectedTaskId.value}/scores/export`, {
      token: auth.token,
      fallbackFilename: `task-${selectedTaskId.value}-scores.csv`,
    })
    ElMessage.success('已开始下载 CSV')
  } catch (e: any) {
    ElMessage.error(e?.message ?? '导出失败')
  }
}

onMounted(loadTasks)
</script>

<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="brand">教师端</div>
      <div class="right">
        <span class="user">{{ auth.displayName }}</span>
        <el-button size="small" @click="createDialog = true">新建任务</el-button>
        <el-button size="small" @click="logout">退出</el-button>
      </div>
    </el-header>
    <el-container>
      <el-aside width="360px" class="aside">
        <div class="aside-title">任务列表</div>
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
              <div>
                <el-button size="small" @click="loadSubmissions" :loading="loadingSubs">刷新提交</el-button>
                <el-button type="primary" size="small" @click="exportCsv">导出 CSV</el-button>
              </div>
            </div>
          </template>
          <div class="meta" style="margin-bottom: 10px">截止：{{ taskDetail.deadlineAt || '-' }}</div>
          <div style="white-space: pre-wrap">{{ taskDetail.description || '（无说明）' }}</div>
        </el-card>

        <el-card class="block" shadow="never" v-if="selectedTaskId">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <div>学生提交列表</div>
              <div class="meta">点击“批阅”对提交打分</div>
            </div>
          </template>
          <el-table :data="submissions" size="small" v-loading="loadingSubs">
            <el-table-column prop="studentDisplayName" label="学生" min-width="120" />
            <el-table-column prop="versionNo" label="版本" width="80" />
            <el-table-column prop="submittedAt" label="提交时间" min-width="180" />
            <el-table-column label="操作" width="220">
              <template #default="{ row }: { row: SubmissionVO }">
                <el-button size="small" @click="openReport(row)">查看报告</el-button>
                <el-button size="small" type="primary" @click="openReview(row)">批阅</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-main>
    </el-container>
  </el-container>

  <el-dialog v-model="createDialog" title="新建任务" width="560px">
    <el-form label-position="top">
      <el-form-item label="标题">
        <el-input v-model="createForm.title" placeholder="例如：第1次实验报告" />
      </el-form-item>
      <el-form-item label="说明">
        <el-input v-model="createForm.description" type="textarea" :rows="5" placeholder="任务要求/提交说明" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createDialog = false">取消</el-button>
      <el-button type="primary" :loading="creating" @click="createTask">创建</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="reviewDialog" title="批阅评分" width="560px">
    <div v-if="reviewTarget" class="meta" style="margin-bottom: 10px">
      学生：{{ reviewTarget.studentDisplayName }}（v{{ reviewTarget.versionNo }}）
    </div>
    <el-form label-position="top">
      <el-form-item label="分数（0-100）">
        <el-input-number v-model="reviewForm.score" :min="0" :max="100" :step="0.5" style="width: 100%" />
      </el-form-item>
      <el-form-item label="评语">
        <el-input v-model="reviewForm.comment" type="textarea" :rows="4" placeholder="写一句评价即可" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="reviewDialog = false">取消</el-button>
      <el-button type="primary" :loading="reviewing" @click="submitReview">提交批阅</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="reportDialog" title="查看报告" width="900px" @closed="closePreview">
    <div v-if="reportTarget" class="meta" style="margin-bottom: 10px; display: flex; justify-content: space-between">
      <div>学生：{{ reportTarget.studentDisplayName }}（v{{ reportTarget.versionNo }}）</div>
      <div>
        <el-button size="small" @click="downloadReportText">下载文本</el-button>
        <el-button size="small" @click="loadAttachments(reportTarget.id)" :loading="loadingAttachments">刷新附件</el-button>
      </div>
    </div>

    <el-form label-position="top">
      <el-form-item label="报告内容（Markdown 原文）">
        <el-input
          v-if="reportTarget"
          :model-value="reportTarget.contentMd"
          type="textarea"
          :rows="10"
          readonly
        />
      </el-form-item>

      <el-form-item label="附件列表">
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
      </el-form-item>
    </el-form>
  </el-dialog>

  <el-dialog v-model="previewDialog" :title="previewTitle || '附件预览'" width="900px" @closed="closePreview">
    <div v-if="!previewUrl">加载中...</div>
    <img v-else :src="previewUrl" style="max-width: 100%; max-height: 70vh; display: block; margin: 0 auto" />
  </el-dialog>
</template>

<style scoped>
.layout {
  min-height: 100vh;
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
:deep(.el-table .is-active td) {
  background: #eef6ff !important;
}
</style>
