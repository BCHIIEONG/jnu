<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { apiData, downloadBlob, fetchBlob } from '../../api/http'
import { useAuthStore } from '../../stores/auth'
import UiModeToggle from '../common/UiModeToggle.vue'
import { useUiStore } from '../../stores/ui'
import QRCode from 'qrcode'

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

type Semester = { id: number; name: string; startDate?: string | null; endDate?: string | null }
type TimeSlot = { id: number; code: string; name: string; startTime: string; endTime: string }
type WeekScheduleItem = {
  id: number
  semesterId: number
  classId: number
  className: string
  labRoomName?: string | null
  lessonDate: string
  slotId: number
  slotName: string
  courseName?: string | null
}
type RosterStudent = { id: number; username: string; displayName: string }
type AttendanceSession = {
  id: number
  scheduleId?: number | null
  semesterId: number
  classId: number
  teacherId: number
  status: string
  startedAt: string
  endedAt?: string | null
}
type AttendanceRecord = {
  id: number
  studentId: number
  studentUsername: string
  studentDisplayName: string
  method: string
  checkedInAt: string
}
type AttendanceTokenVO = { token: string; issuedAtEpochSec: number; ttlSeconds: number }

const auth = useAuthStore()
const ui = useUiStore()
const router = useRouter()

const isMobile = computed(() => ui.effectiveMode === 'mobile')

const activeTab = ref<'report' | 'schedule'>('report')

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

// Schedule/attendance state
const semesters = ref<Semester[]>([])
const timeSlots = ref<TimeSlot[]>([])
const scheduleSemesterId = ref<number | null>(null)
const weekStartDate = ref<string>(toYmd(new Date()))
const weekItems = ref<WeekScheduleItem[]>([])
const loadingWeek = ref(false)

const classDialog = ref(false)
const selectedCell = ref<WeekScheduleItem | null>(null)
const roster = ref<RosterStudent[]>([])
const loadingRoster = ref(false)

const session = ref<AttendanceSession | null>(null)
const tokenInfo = ref<AttendanceTokenVO | null>(null)
const qrDataUrl = ref<string | null>(null)
const mobileBase = ref<string>((import.meta.env.VITE_MOBILE_BASE_URL as string | undefined) ?? window.location.origin)

const records = ref<AttendanceRecord[]>([])
const loadingRecords = ref(false)

let tokenTimer: number | null = null
let recordsTimer: number | null = null

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

watch(activeTab, async (tab) => {
  if (tab !== 'schedule') return
  if (semesters.value.length > 0 && timeSlots.value.length > 0) return
  try {
    semesters.value = await apiData<Semester[]>('/api/teacher/semesters', { method: 'GET' }, auth.token)
    timeSlots.value = await apiData<TimeSlot[]>('/api/teacher/time-slots', { method: 'GET' }, auth.token)
    scheduleSemesterId.value = scheduleSemesterId.value ?? semesters.value[0]?.id ?? null
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载课表元数据失败')
  }
})

onBeforeUnmount(() => {
  stopLoops()
})

function toYmd(d: Date): string {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function addDays(ymd: string, days: number): string {
  const parts = ymd.split('-')
  if (parts.length !== 3) return toYmd(new Date())
  const y = Number(parts[0])
  const m = Number(parts[1])
  const d = Number(parts[2])
  if (!Number.isFinite(y) || !Number.isFinite(m) || !Number.isFinite(d)) return toYmd(new Date())
  const date = new Date(y, m - 1, d)
  date.setDate(date.getDate() + days)
  return toYmd(date)
}

const weekDays = computed(() => {
  const names = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  const start = weekStartDate.value
  const parts = start.split('-')
  if (parts.length !== 3) return []
  const y = Number(parts[0])
  const m = Number(parts[1])
  const d = Number(parts[2])
  if (!Number.isFinite(y) || !Number.isFinite(m) || !Number.isFinite(d)) return []
  const base = new Date(y, m - 1, d)
  return Array.from({ length: 7 }).map((_, i) => {
    const dd = new Date(base)
    dd.setDate(dd.getDate() + i)
    return { date: toYmd(dd), label: names[dd.getDay()] ?? '' }
  })
})

function itemAt(slotId: number, date: string): WeekScheduleItem | null {
  return weekItems.value.find((x) => x.slotId === slotId && x.lessonDate === date) ?? null
}

async function loadWeek() {
  if (!scheduleSemesterId.value) {
    ElMessage.warning('请选择学期')
    return
  }
  if (!weekStartDate.value) {
    ElMessage.warning('请输入周起始日期（YYYY-MM-DD）')
    return
  }
  loadingWeek.value = true
  try {
    weekItems.value = await apiData<WeekScheduleItem[]>(
      `/api/teacher/schedule/week?semesterId=${scheduleSemesterId.value}&weekStartDate=${encodeURIComponent(weekStartDate.value)}`,
      { method: 'GET' },
      auth.token,
    )
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载课表失败')
  } finally {
    loadingWeek.value = false
  }
}

function prevWeek() {
  weekStartDate.value = addDays(weekStartDate.value, -7)
  loadWeek()
}

function nextWeek() {
  weekStartDate.value = addDays(weekStartDate.value, 7)
  loadWeek()
}

async function openClass(item: WeekScheduleItem) {
  selectedCell.value = item
  classDialog.value = true
  stopLoops()
  session.value = null
  tokenInfo.value = null
  qrDataUrl.value = null
  records.value = []
  await loadRoster()
}

async function loadRoster() {
  if (!selectedCell.value) return
  loadingRoster.value = true
  try {
    roster.value = await apiData<RosterStudent[]>(
      `/api/teacher/classes/${selectedCell.value.classId}/roster`,
      { method: 'GET' },
      auth.token,
    )
  } catch (e: any) {
    roster.value = []
    ElMessage.error(e?.message ?? '加载班级名册失败')
  } finally {
    loadingRoster.value = false
  }
}

async function startSession() {
  if (!selectedCell.value) return
  try {
    session.value = await apiData<AttendanceSession>(
      `/api/attendance/sessions`,
      { method: 'POST', body: { scheduleId: selectedCell.value.id } },
      auth.token,
    )
    ElMessage.success('签到已开启')
    await refreshToken()
    await refreshRecords()
    startLoops()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '开启签到失败')
  }
}

async function closeSession() {
  if (!session.value) return
  try {
    session.value = await apiData<AttendanceSession>(`/api/attendance/sessions/${session.value.id}/close`, { method: 'POST' }, auth.token)
    ElMessage.success('签到已结束')
    stopLoops()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '结束签到失败')
  }
}

async function refreshToken() {
  if (!session.value) return
  try {
    tokenInfo.value = await apiData<AttendanceTokenVO>(
      `/api/attendance/sessions/${session.value.id}/token`,
      { method: 'GET' },
      auth.token,
    )
    const link = checkinLink.value
    qrDataUrl.value = await QRCode.toDataURL(link, { width: 220, margin: 1 })
  } catch (e: any) {
    ElMessage.error(e?.message ?? '刷新二维码失败')
  }
}

async function refreshRecords() {
  if (!session.value) return
  loadingRecords.value = true
  try {
    records.value = await apiData<AttendanceRecord[]>(
      `/api/attendance/sessions/${session.value.id}/records`,
      { method: 'GET' },
      auth.token,
    )
  } catch (e: any) {
    records.value = []
    ElMessage.error(e?.message ?? '加载签到记录失败')
  } finally {
    loadingRecords.value = false
  }
}

function startLoops() {
  stopLoops()
  tokenTimer = window.setInterval(refreshToken, 5000)
  recordsTimer = window.setInterval(refreshRecords, 2000)
}

function stopLoops() {
  if (tokenTimer) window.clearInterval(tokenTimer)
  if (recordsTimer) window.clearInterval(recordsTimer)
  tokenTimer = null
  recordsTimer = null
}

const checkedSet = computed(() => new Set(records.value.map((r) => r.studentId)))
function isCheckedIn(studentId: number): boolean {
  return checkedSet.value.has(studentId)
}

async function manualCheckin(studentId: number) {
  if (!session.value) return
  try {
    await apiData(
      `/api/attendance/sessions/${session.value.id}/manual-checkin`,
      { method: 'POST', body: { studentId } },
      auth.token,
    )
    ElMessage.success('已补签')
    await refreshRecords()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '补签失败')
  }
}

async function exportAttendanceCsv() {
  if (!session.value) return
  try {
    await downloadBlob(`/api/attendance/sessions/${session.value.id}/export`, {
      token: auth.token,
      fallbackFilename: `attendance-session-${session.value.id}.csv`,
    })
    ElMessage.success('已开始下载 CSV')
  } catch (e: any) {
    ElMessage.error(e?.message ?? '导出失败')
  }
}

const checkinLink = computed(() => {
  const t = tokenInfo.value?.token ?? ''
  if (!t) return ''
  const base = (mobileBase.value ?? '').trim().replace(/\/$/, '')
  return `${base}/m/checkin?t=${encodeURIComponent(t)}`
})

async function copyLink() {
  const link = checkinLink.value
  if (!link) return
  try {
    await navigator.clipboard.writeText(link)
    ElMessage.success('已复制链接')
  } catch {
    ElMessage.info('复制失败，请手动复制')
  }
}
</script>

<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="brand">教师端</div>
      <div class="right">
        <span class="user">{{ auth.displayName }}</span>
        <UiModeToggle />
        <el-button size="small" @click="createDialog = true">新建任务</el-button>
        <el-button size="small" @click="logout">退出</el-button>
      </div>
    </el-header>
    <el-container>
      <el-aside v-if="activeTab === 'report' && !isMobile" width="360px" class="aside">
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
        <el-tabs v-model="activeTab" class="tabs">
          <el-tab-pane label="报告管理" name="report">
            <el-card v-if="isMobile" class="block" shadow="never">
              <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap">
                <el-select
                  v-model="selectedTaskId"
                  placeholder="选择任务"
                  style="width: 260px"
                  :loading="loadingTasks"
                  @change="(id: any) => id && selectTask(Number(id))"
                >
                  <el-option v-for="t in tasks" :key="t.id" :label="t.title" :value="t.id" />
                </el-select>
                <el-button size="small" :loading="loadingTasks" @click="loadTasks">刷新任务</el-button>
              </div>
            </el-card>

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
          </el-tab-pane>

          <el-tab-pane label="课表/签到" name="schedule">
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

              <div class="meta" style="margin: 8px 0">
                点击课表格子进入班级管理页，可开启/结束签到并查看实时名单。
              </div>

              <div class="grid" v-loading="loadingWeek">
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
                        <div
                          class="cell-box"
                          :class="{ has: !!itemAt(ts.id, d.date) }"
                          @click="() => { const it = itemAt(ts.id, d.date); if (it) openClass(it) }"
                        >
                          <template v-if="itemAt(ts.id, d.date)">
                            <div class="title">{{ itemAt(ts.id, d.date)!.className }}</div>
                            <div class="meta">{{ itemAt(ts.id, d.date)!.courseName || '（未命名课程）' }}</div>
                            <div class="meta">{{ itemAt(ts.id, d.date)!.labRoomName || '-' }}</div>
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

  <el-dialog v-model="classDialog" title="班级管理 / 签到" width="980px" @closed="stopLoops">
    <div v-if="selectedCell" class="meta" style="margin-bottom: 10px">
      {{ selectedCell.lessonDate }} / {{ selectedCell.slotName }} / {{ selectedCell.className }}
      <span style="margin-left: 10px">实验室：{{ selectedCell.labRoomName || '-' }}</span>
    </div>

    <el-card shadow="never" style="margin-bottom: 12px">
      <div class="toolbar">
        <el-input v-model="mobileBase" placeholder="手机端访问基址（建议填电脑局域网IP）" style="width: 360px" />
        <el-button size="small" @click="copyLink" :disabled="!checkinLink">复制签到链接</el-button>
        <el-button size="small" @click="exportAttendanceCsv" :disabled="!session">导出签到 CSV</el-button>
        <el-button size="small" type="primary" @click="startSession" :disabled="!!session">开启签到</el-button>
        <el-button size="small" type="danger" @click="closeSession" :disabled="!session">结束签到</el-button>
        <el-button size="small" @click="refreshToken" :disabled="!session">刷新二维码</el-button>
        <el-button size="small" @click="refreshRecords" :disabled="!session">刷新名单</el-button>
      </div>

      <div v-if="session" class="meta" style="margin-top: 6px">
        场次ID：{{ session.id }}，状态：{{ session.status }}，开始：{{ session.startedAt }}
      </div>

      <div v-if="session" style="display: flex; gap: 16px; align-items: flex-start; margin-top: 12px; flex-wrap: wrap">
        <div>
          <div class="meta" style="margin-bottom: 6px">动态二维码（每 5 秒刷新）</div>
          <div class="qr">
            <img v-if="qrDataUrl" :src="qrDataUrl" alt="QR" />
            <div v-else class="meta">加载中...</div>
          </div>
        </div>
        <div style="flex: 1; min-width: 320px">
          <div class="meta" style="margin-bottom: 6px">签到链接</div>
          <el-input :model-value="checkinLink" readonly />
          <div class="meta" style="margin-top: 8px">
            说明：如果用手机扫码，请把上面的基址改为电脑的局域网 IP（否则手机访问不到 localhost）。
          </div>
        </div>
      </div>
    </el-card>

    <el-tabs>
      <el-tab-pane label="班级名册">
        <el-table :data="roster" v-loading="loadingRoster" stripe height="420">
          <el-table-column prop="username" label="学号/账号" width="160" />
          <el-table-column prop="displayName" label="姓名" width="140" />
          <el-table-column label="签到状态" width="120">
            <template #default="{ row }">
              <span v-if="isCheckedIn(row.id)" style="color: #067d17">已签到</span>
              <span v-else style="color: #999">未签到</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button size="small" type="primary" :disabled="!session || isCheckedIn(row.id)" @click="manualCheckin(row.id)">补签</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="实时名单">
        <el-table :data="records" v-loading="loadingRecords" stripe height="420">
          <el-table-column prop="studentUsername" label="学号/账号" width="160" />
          <el-table-column prop="studentDisplayName" label="姓名" width="140" />
          <el-table-column prop="method" label="方式" width="100" />
          <el-table-column prop="checkedInAt" label="时间" min-width="180" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
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
.tabs :deep(.el-tabs__content) {
  padding-top: 8px;
}
.toolbar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}
.grid {
  overflow: auto;
  border: 1px solid #eee;
  border-radius: 10px;
}
.week-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 900px;
}
.week-table th,
.week-table td {
  border-bottom: 1px solid #eee;
  border-right: 1px solid #eee;
  vertical-align: top;
}
.week-table th:last-child,
.week-table td:last-child {
  border-right: none;
}
.col-slot {
  width: 160px;
  background: #fafafa;
}
.col-day {
  background: #fafafa;
}
.cell {
  padding: 8px;
}
.cell.slot {
  background: #fff;
}
.slot-name {
  font-weight: 700;
}
.cell-box {
  min-height: 72px;
  border-radius: 10px;
  padding: 8px;
  background: #fcfcfc;
  cursor: default;
}
.cell-box.has {
  background: #eef6ff;
  cursor: pointer;
}
.cell-box .title {
  font-weight: 700;
  margin-bottom: 2px;
}
.qr {
  width: 240px;
  height: 240px;
  display: grid;
  place-items: center;
  border: 1px dashed #ccc;
  border-radius: 12px;
  background: #fff;
}
.qr img {
  max-width: 220px;
  max-height: 220px;
}
:deep(.el-table .is-active td) {
  background: #eef6ff !important;
}
</style>
