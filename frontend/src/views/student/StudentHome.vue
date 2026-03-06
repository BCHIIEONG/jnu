<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
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

type Semester = { id: number; name: string; startDate?: string | null; endDate?: string | null }
type TimeSlot = { id: number; code: string; name: string; startTime: string; endTime: string }
type WeekScheduleItem = {
  id: number
  semesterId: number
  classId: number
  className: string
  teacherId: number
  teacherDisplayName?: string | null
  labRoomId?: number | null
  labRoomName?: string | null
  lessonDate: string
  slotId: number
  slotCode?: string | null
  slotName: string
  slotStartTime: string
  slotEndTime: string
  courseName?: string | null
}

const auth = useAuthStore()
const ui = useUiStore()
const router = useRouter()

const isMobile = computed(() => ui.effectiveMode === 'mobile')
const activeTab = ref<'tasks' | 'schedule'>('tasks')
const mobilePage = ref<'list' | 'task' | 'schedule'>('list')

const tasks = ref<TaskVO[]>([])
const loadingTasks = ref(false)
const selectedTaskId = ref<number | null>(null)
const taskDetail = ref<TaskVO | null>(null)

const mySubmissions = ref<SubmissionVO[]>([])
const loadingSubs = ref(false)

const submitMd = ref('')
const submitting = ref(false)
const submitFiles = ref<File[]>([])
const submitFileInput = ref<HTMLInputElement | null>(null)

const reviewDialogOpen = ref(false)
const currentReview = ref<ReviewVO | null>(null)
const loadingReview = ref(false)

const contentDialogOpen = ref(false)
const contentTarget = ref<SubmissionVO | null>(null)

const attachDialogOpen = ref(false)
const attachTarget = ref<SubmissionVO | null>(null)
const attachments = ref<AttachmentVO[]>([])
const loadingAttachments = ref(false)
const uploading = ref(false)
const uploadFile = ref<File | null>(null)

const latestSubmission = computed(() => (mySubmissions.value.length > 0 ? mySubmissions.value[0]! : null))

const previewDialog = ref(false)
const previewUrl = ref<string | null>(null)
const previewKind = ref<'image' | 'text'>('image')
const previewText = ref<string>('')
const previewTitle = ref('')

const semesters = ref<Semester[]>([])
const timeSlots = ref<TimeSlot[]>([])
const scheduleSemesterId = ref<number | null>(null)
const weekStartDate = ref(getWeekStartYmd(new Date()))
const weekItems = ref<WeekScheduleItem[]>([])
const loadingWeek = ref(false)
const scheduleError = ref('')

function toYmd(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function getWeekStartYmd(date: Date): string {
  const copy = new Date(date)
  const day = copy.getDay()
  const diff = day === 0 ? -6 : 1 - day
  copy.setDate(copy.getDate() + diff)
  return toYmd(copy)
}

function formatWeekdayLabel(date: string): string {
  const parsed = new Date(`${date}T00:00:00`)
  const labels = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return labels[parsed.getDay()] ?? date
}

const weekDays = computed(() => {
  const base = new Date(`${weekStartDate.value}T00:00:00`)
  return Array.from({ length: 7 }).map((_, idx) => {
    const d = new Date(base)
    d.setDate(base.getDate() + idx)
    const date = toYmd(d)
    return { date, label: formatWeekdayLabel(date) }
  })
})

function itemAt(slotId: number, date: string): WeekScheduleItem | null {
  return weekItems.value.find((x) => x.slotId === slotId && x.lessonDate === date) ?? null
}

const mobileScheduleDays = computed(() =>
  weekDays.value.map((day) => ({
    ...day,
    items: timeSlots.value
      .map((slot) => itemAt(slot.id, day.date))
      .filter((item): item is WeekScheduleItem => Boolean(item)),
  })),
)

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

async function loadScheduleMeta() {
  try {
    const [semesterList, slotList] = await Promise.all([
      apiData<Semester[]>('/api/student/semesters', { method: 'GET' }, auth.token),
      apiData<TimeSlot[]>('/api/student/time-slots', { method: 'GET' }, auth.token),
    ])
    semesters.value = semesterList
    timeSlots.value = slotList
    if (!scheduleSemesterId.value && semesterList.length > 0) {
      scheduleSemesterId.value = semesterList[0]!.id
    }
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载课表元数据失败')
  }
}

async function loadWeek() {
  if (!scheduleSemesterId.value) {
    ElMessage.warning('请先选择学期')
    return
  }
  loadingWeek.value = true
  scheduleError.value = ''
  try {
    weekItems.value = await apiData<WeekScheduleItem[]>(
      `/api/student/schedule/week?semesterId=${scheduleSemesterId.value}&weekStartDate=${encodeURIComponent(weekStartDate.value)}`,
      { method: 'GET' },
      auth.token,
    )
  } catch (e: any) {
    weekItems.value = []
    scheduleError.value = e?.message ?? '加载课表失败'
  } finally {
    loadingWeek.value = false
  }
}

function prevWeek() {
  const d = new Date(`${weekStartDate.value}T00:00:00`)
  d.setDate(d.getDate() - 7)
  weekStartDate.value = toYmd(d)
  loadWeek()
}

function nextWeek() {
  const d = new Date(`${weekStartDate.value}T00:00:00`)
  d.setDate(d.getDate() + 7)
  weekStartDate.value = toYmd(d)
  loadWeek()
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

async function openScheduleMobile() {
  mobilePage.value = 'schedule'
  if (!semesters.value.length || !timeSlots.value.length) {
    await loadScheduleMeta()
  }
  await loadWeek()
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

  const normalize = (s: string) => (s || '').replace(/\r\n/g, '\n').trim()
  const latest = latestSubmission.value
  const current = normalize(submitMd.value)
  const latestText = latest ? normalize(latest.contentMd) : ''
  const hasText = current.length > 0
  const hasFiles = submitFiles.value.length > 0

  if (!hasText && !hasFiles) {
    ElMessage.warning('正文为空且未选择附件，无法提交')
    return
  }
  if (latest && current === latestText && !hasFiles) {
    ElMessage.warning('正文没有新增内容，禁止重复提交')
    return
  }

  let confirmEmptyContent = false
  if (latest && latestText.length > 0 && !hasText) {
    try {
      await ElMessageBox.confirm('本次提交没有正文内容，确定要继续吗？', '提示', {
        type: 'warning',
        confirmButtonText: '继续提交',
        cancelButtonText: '取消',
      })
      confirmEmptyContent = true
    } catch {
      return
    }
  }

  if (!hasFiles) {
    try {
      await ElMessageBox.confirm('本次未提交附件，确定继续吗？', '提示', {
        type: 'info',
        confirmButtonText: '继续',
        cancelButtonText: '取消',
      })
    } catch {
      return
    }
  }

  submitting.value = true
  try {
    if (hasFiles) {
      const fd = new FormData()
      fd.append('contentMd', submitMd.value ?? '')
      if (confirmEmptyContent) fd.append('confirmEmptyContent', 'true')
      for (const f of submitFiles.value) fd.append('files', f)
      await uploadFormData(`/api/tasks/${selectedTaskId.value}/submissions/multipart`, { token: auth.token, formData: fd })
    } else {
      await apiData(
        `/api/tasks/${selectedTaskId.value}/submissions`,
        { method: 'POST', body: { contentMd: submitMd.value } },
        auth.token,
      )
    }
    ElMessage.success('提交成功')
    await loadMySubmissions()
    submitMd.value = ''
    submitFiles.value = []
    if (submitFileInput.value) submitFileInput.value.value = ''
  } catch (e: any) {
    ElMessage.error(e?.message ?? '提交失败')
  } finally {
    submitting.value = false
  }
}

function onPickSubmitFiles(ev: Event) {
  const input = ev.target as HTMLInputElement
  const fs = Array.from(input.files ?? [])
  submitFiles.value = fs
  submitFileInput.value = input
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

function openContent(row: SubmissionVO) {
  contentTarget.value = row
  contentDialogOpen.value = true
}

async function downloadContent(row: SubmissionVO) {
  try {
    await downloadBlob(`/api/submissions/${row.id}/content/download`, {
      token: auth.token,
      fallbackFilename: `submission-${row.id}.md`,
    })
    ElMessage.success('已开始下载文本')
  } catch (e: any) {
    ElMessage.error(e?.message ?? '下载失败')
  }
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
  previewText.value = ''
  previewDialog.value = false
}

async function previewAttachment(row: AttachmentVO) {
  try {
    closePreview()
    const { blob, contentType } = await fetchBlob(`/api/attachments/${row.id}/download`, { token: auth.token })
    const ct = (row.contentType ?? contentType ?? '').toLowerCase()
    const name = (row.fileName ?? '').toLowerCase()
    const ext = name.includes('.') ? name.split('.').pop() || '' : ''
    const isTextLike =
      ct.startsWith('text/') ||
      ct.includes('json') ||
      ct.includes('xml') ||
      ct.includes('yaml') ||
      ['txt', 'md', 'log', 'json', 'xml', 'yml', 'yaml', 'sql', 'java', 'py', 'js', 'ts', 'vue', 'html', 'css', 'c', 'cpp', 'h', 'hpp', 'sh', 'ps1'].includes(ext)

    if (ct.startsWith('image/')) {
      previewKind.value = 'image'
      previewUrl.value = URL.createObjectURL(blob)
      previewTitle.value = row.fileName
      previewDialog.value = true
      return
    }

    if (isTextLike) {
      if (blob.size > 200 * 1024) {
        ElMessage.info('文件较大，建议下载查看')
        return
      }
      const buf = await blob.arrayBuffer()
      previewKind.value = 'text'
      previewText.value = new TextDecoder('utf-8').decode(buf)
      previewTitle.value = row.fileName
      previewDialog.value = true
      return
    }

    ElMessage.info('该类型暂不支持在线预览，请下载查看')
  } catch (e: any) {
    ElMessage.error(e?.message ?? '预览失败')
  }
}

function logout() {
  auth.logout()
  router.replace('/login')
}

watch(
  () => activeTab.value,
  async (tab) => {
    if (tab !== 'schedule') return
    if (!semesters.value.length || !timeSlots.value.length) {
      await loadScheduleMeta()
    }
    await loadWeek()
  },
)

onMounted(async () => {
  await loadTasks()
  await loadScheduleMeta()
})
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
        <div class="mobileTitleRow">
          <div class="mobileTitle">实验任务</div>
          <el-button size="small" @click="openScheduleMobile">我的课表</el-button>
        </div>
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

      <div v-else-if="mobilePage === 'task'">
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
              <el-input v-model="submitMd" type="textarea" :rows="10" placeholder="Markdown 内容（可空，若你提交了附件）" />
              <div style="margin-top: 10px">
                <div class="meta" style="margin-bottom: 6px">附件（可选，本次提交一起上传）</div>
                <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap">
                  <input type="file" multiple @change="onPickSubmitFiles" />
                  <div class="meta" v-if="submitFiles.length > 0">已选择 {{ submitFiles.length }} 个文件</div>
                </div>
              </div>
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
                <el-button size="small" @click="openContent(s)">查看内容</el-button>
                <el-button size="small" @click="openAttachments(s)">附件</el-button>
              </div>
            </el-card>
            <div v-if="loadingSubs" class="meta">加载中...</div>
            <div v-else-if="mySubmissions.length === 0" class="meta">暂无提交</div>
          </el-collapse-item>
        </el-collapse>
      </div>

      <div v-else>
        <div class="mobileTop">
          <el-button size="small" @click="backToListMobile">返回首页</el-button>
          <el-button size="small" :loading="loadingWeek" @click="loadWeek">刷新课表</el-button>
        </div>

        <el-card class="block" shadow="never">
          <div class="toolbar">
            <el-select v-model="scheduleSemesterId" placeholder="选择学期" style="width: 100%">
              <el-option v-for="s in semesters" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
            <el-input v-model="weekStartDate" placeholder="周起始日期(YYYY-MM-DD)" />
            <div class="mobileWeekBtns">
              <el-button @click="prevWeek">上周</el-button>
              <el-button @click="nextWeek">下周</el-button>
              <el-button type="primary" :loading="loadingWeek" @click="loadWeek">加载</el-button>
            </div>
          </div>
        </el-card>

        <div v-if="scheduleError" class="scheduleEmpty">{{ scheduleError }}</div>

        <el-card v-for="day in mobileScheduleDays" :key="day.date" shadow="never" class="block">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <div>{{ day.label }}</div>
              <div class="meta">{{ day.date }}</div>
            </div>
          </template>

          <div v-if="day.items.length === 0" class="meta">无课程安排</div>

          <div v-for="item in day.items" :key="item.id" class="scheduleCard">
            <div class="scheduleHead">
              <div class="scheduleName">{{ item.courseName || '（未命名课程）' }}</div>
              <el-tag size="small">{{ item.slotName }}</el-tag>
            </div>
            <div class="meta">时间：{{ item.slotStartTime }} - {{ item.slotEndTime }}</div>
            <div class="meta">教师：{{ item.teacherDisplayName || '-' }}</div>
            <div class="meta">实验室：{{ item.labRoomName || '-' }}</div>
            <div class="meta">班级：{{ item.className }}</div>
          </div>
        </el-card>
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
        <el-tabs v-model="activeTab" class="tabs">
          <el-tab-pane label="实验任务" name="tasks">
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
              <div style="margin-top: 10px">
                <div class="meta" style="margin-bottom: 6px">附件（可选，本次提交一起上传）</div>
                <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap">
                  <input type="file" multiple @change="onPickSubmitFiles" />
                  <div class="meta" v-if="submitFiles.length > 0">已选择 {{ submitFiles.length }} 个文件</div>
                </div>
              </div>
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
                <el-table-column label="操作" width="280">
                  <template #default="{ row }: { row: SubmissionVO }">
                    <el-button size="small" @click="viewReview(row.id)">查看批阅</el-button>
                    <el-button size="small" @click="openContent(row)">查看内容</el-button>
                    <el-button size="small" @click="openAttachments(row)">附件</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-tab-pane>

          <el-tab-pane label="我的课表" name="schedule">
            <el-card class="block" shadow="never">
              <div class="toolbar">
                <el-select v-model="scheduleSemesterId" placeholder="选择学期" style="width: 220px">
                  <el-option v-for="s in semesters" :key="s.id" :label="s.name" :value="s.id" />
                </el-select>
                <el-input v-model="weekStartDate" placeholder="周起始日期(YYYY-MM-DD)" style="width: 220px" />
                <el-button @click="prevWeek">上周</el-button>
                <el-button @click="nextWeek">下周</el-button>
                <el-button type="primary" :loading="loadingWeek" @click="loadWeek">加载课表</el-button>
              </div>
              <div v-if="scheduleError" class="scheduleEmpty" style="margin-top: 12px">{{ scheduleError }}</div>
              <div v-else class="grid" v-loading="loadingWeek">
                <table class="week-table">
                  <thead>
                    <tr>
                      <th class="col-slot">节次</th>
                      <th v-for="d in weekDays" :key="d.date" class="col-day">
                        <div>{{ d.label }}</div>
                        <div class="meta">{{ d.date }}</div>
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="ts in timeSlots" :key="ts.id">
                      <td class="cell slot">
                        <div class="slot-name">{{ ts.name }}</div>
                        <div class="meta">{{ ts.startTime }} - {{ ts.endTime }}</div>
                      </td>
                      <td v-for="d in weekDays" :key="d.date + '-' + ts.id" class="cell">
                        <div class="cell-box" :class="{ has: !!itemAt(ts.id, d.date) }">
                          <template v-if="itemAt(ts.id, d.date)">
                            <div class="title">{{ itemAt(ts.id, d.date)!.courseName || '（未命名课程）' }}</div>
                            <div class="meta">{{ itemAt(ts.id, d.date)!.teacherDisplayName || '-' }}</div>
                            <div class="meta">{{ itemAt(ts.id, d.date)!.labRoomName || '-' }}</div>
                            <div class="meta">{{ itemAt(ts.id, d.date)!.className }}</div>
                          </template>
                          <template v-else>
                            <div class="meta">-</div>
                          </template>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </el-card>
          </el-tab-pane>
        </el-tabs>
      </el-main>
    </el-container>
  </el-container>

  <el-dialog v-model="contentDialogOpen" title="查看正文" width="900px">
    <div v-if="contentTarget" class="meta" style="margin-bottom: 10px; display: flex; justify-content: space-between">
      <div>v{{ contentTarget.versionNo }}（ID: {{ contentTarget.id }}）</div>
      <el-button size="small" @click="downloadContent(contentTarget)">下载正文</el-button>
    </div>
    <el-input v-if="contentTarget" :model-value="contentTarget.contentMd" type="textarea" :rows="16" readonly />
  </el-dialog>

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

    <template v-if="!isMobile">
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
    </template>
    <template v-else>
      <div v-if="loadingAttachments" class="meta">加载中...</div>
      <div v-else-if="attachments.length === 0" class="meta">暂无附件</div>
      <el-card v-else v-for="a in attachments" :key="a.id" shadow="never" class="attCard">
        <div class="attName">{{ a.fileName }}</div>
        <div class="meta" style="margin-top: 6px">大小：{{ a.fileSize }} Byte</div>
        <div class="meta">时间：{{ a.uploadedAt }}</div>
        <div style="display: flex; gap: 10px; margin-top: 10px; flex-wrap: wrap">
          <el-button size="small" @click="downloadAttachment(a)">下载</el-button>
          <el-button size="small" @click="previewAttachment(a)">预览</el-button>
        </div>
      </el-card>
    </template>
    <div class="meta" style="margin-top: 6px">支持图片与小型文本/代码在线预览（大文件请下载）。</div>
  </el-dialog>

  <el-dialog v-model="previewDialog" :title="previewTitle || '附件预览'" width="900px" @closed="closePreview">
    <div v-if="previewKind === 'image'">
      <div v-if="!previewUrl">加载中...</div>
      <img v-else :src="previewUrl" style="max-width: 100%; max-height: 70vh; display: block; margin: 0 auto" />
    </div>
    <div v-else style="max-height: 70vh; overflow: auto; border: 1px solid #eee; padding: 10px; border-radius: 6px">
      <pre style="margin: 0; white-space: pre-wrap; word-break: break-word">{{ previewText }}</pre>
    </div>
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
.mobileTitleRow {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.mobileTitle {
  font-weight: 800;
  font-size: 18px;
  margin: 6px 0;
}
.taskCard {
  margin-bottom: 10px;
  cursor: pointer;
}
.subCard {
  margin-bottom: 10px;
}
.attCard {
  margin-bottom: 10px;
}
.attName {
  font-weight: 700;
  word-break: break-word;
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
.toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
.grid {
  overflow-x: auto;
}
.week-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}
.week-table th,
.week-table td {
  border: 1px solid #ebeef5;
  vertical-align: top;
}
.col-slot {
  width: 120px;
  background: #fafafa;
}
.col-day {
  min-width: 120px;
  background: #fafafa;
}
.cell {
  padding: 8px;
}
.slot-name {
  font-weight: 700;
}
.cell-box {
  min-height: 92px;
}
.cell-box.has {
  background: #f8fbff;
}
.title {
  font-weight: 700;
  margin-bottom: 4px;
  word-break: break-word;
}
.scheduleEmpty {
  color: #c45656;
  background: #fef0f0;
  border: 1px solid #fbc4c4;
  border-radius: 8px;
  padding: 12px;
}
.mobileWeekBtns {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.scheduleCard {
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  margin-bottom: 10px;
  background: #fafcff;
}
.scheduleHead {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 8px;
}
.scheduleName {
  font-weight: 700;
  word-break: break-word;
}
:deep(.el-table .is-active td) {
  background: #eef6ff !important;
}
</style>
