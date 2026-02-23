<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { apiData } from '../../api/http'
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
  versionNo: number
  contentMd: string
  submittedAt: string
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
const router = useRouter()

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
  await loadMySubmissions()
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
        <el-button size="small" @click="logout">退出</el-button>
      </div>
    </el-header>
    <el-container>
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
          <el-table-column label="操作" width="140">
              <template #default="{ row }: { row: SubmissionVO }">
                <el-button size="small" @click="viewReview(row.id)">查看批阅</el-button>
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
