<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiData, downloadBlob, fetchBlob, uploadFormData } from '../../api/http'
import { useAuthStore } from '../../stores/auth'
import UiModeToggle from '../common/UiModeToggle.vue'
import { useUiStore } from '../../stores/ui'
import QRCode from 'qrcode'
import PlagiarismSummaryPanel from './components/PlagiarismSummaryPanel.vue'

type TaskVO = {
  id: number
  title: string
  description?: string
  publisherName?: string
  deadlineAt?: string | null
  status?: string
  createdAt?: string
  attachments?: TaskAttachmentVO[]
}

type TaskAttachmentVO = {
  id: number
  taskId: number
  fileName: string
  fileSize: number
  contentType?: string | null
  uploadedAt: string
  uploadedBy?: number | null
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
type AttendanceStaticCodeVO = { code: string }
type TeacherClassVO = { id: number; name: string; departmentName?: string | null }
type PageResult<T> = { page: number; size: number; total: number; items: T[] }
type AttendanceHistoryItem = {
  sessionId: number
  courseName: string
  classId: number
  classDisplayName: string
  grade?: number | null
  labRoomName?: string | null
  lessonDate?: string | null
  slotName?: string | null
  startedAt: string
  endedAt?: string | null
  status: string
  checkedInCount: number
  absentCount: number
  totalCount: number
}
type AttendanceHistoryStudent = {
  studentId: number
  studentUsername: string
  studentDisplayName: string
  status: 'CHECKED_IN' | 'NOT_CHECKED_IN'
  method?: string | null
  checkedInAt?: string | null
}
type AttendanceHistoryDetail = AttendanceHistoryItem & {
  roster: AttendanceHistoryStudent[]
}
type ProgressAttachmentVO = {
  id: number
  progressLogId: number
  fileName: string
  fileSize: number
  contentType?: string | null
  uploadedAt: string
}
type TaskProgressVO = {
  id: number
  taskId: number
  studentId: number
  stepNo: number
  content?: string | null
  createdAt: string
  attachments: ProgressAttachmentVO[]
}
type TaskCompletionVO = {
  taskId: number
  studentId: number
  status: 'NONE' | 'PENDING_CONFIRM' | 'CONFIRMED'
  requestedAt?: string | null
  confirmedAt?: string | null
  confirmedBy?: number | null
  confirmedByDisplayName?: string | null
}
type TeacherTaskProgressStudentVO = {
  studentId: number
  studentUsername: string
  studentDisplayName: string
  classDisplayName?: string | null
  progressCount: number
  completionStatus: 'NONE' | 'PENDING_CONFIRM' | 'CONFIRMED'
  latestUpdatedAt?: string | null
  requestedAt?: string | null
  confirmedAt?: string | null
}
type TeacherTaskProgressDetailVO = {
  taskId: number
  studentId: number
  studentUsername: string
  studentDisplayName: string
  classDisplayName?: string | null
  completionStatus: 'NONE' | 'PENDING_CONFIRM' | 'CONFIRMED'
  requestedAt?: string | null
  confirmedAt?: string | null
  confirmedByDisplayName?: string | null
  logs: TaskProgressVO[]
}
type TaskDeviceConfigVO = {
  deviceId: number
  deviceCode: string
  deviceName: string
  deviceStatus: string
  totalQuantity: number
  configuredQuantity: number
  reservedQuantity: number
  availableQuantity: number
}
type TaskDeviceRequestVO = {
  id: number
  taskId: number
  studentId: number
  studentUsername: string
  studentDisplayName: string
  deviceId: number
  deviceCode: string
  deviceName: string
  quantity: number
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'BORROWED' | 'RETURNED'
  note?: string | null
  createdAt: string
  approvedAt?: string | null
  checkoutAt?: string | null
  returnAt?: string | null
}

const auth = useAuthStore()
const ui = useUiStore()
const router = useRouter()

const isMobile = computed(() => ui.effectiveMode === 'mobile')

const activeTab = ref<'flow' | 'report' | 'schedule' | 'history'>('flow')
const flowSubTab = ref<'progress' | 'device'>('progress')

const tasks = ref<TaskVO[]>([])
const loadingTasks = ref(false)
const selectedTaskId = ref<number | null>(null)
const taskDetail = ref<TaskVO | null>(null)

const submissions = ref<SubmissionVO[]>([])
const loadingSubs = ref(false)

type SubmissionGroup = {
  key: string
  studentUsername: string
  studentDisplayName: string
  latest: SubmissionVO
  versions: SubmissionVO[]
}

const submissionQ = ref('')

const createDialog = ref(false)
const editTaskDialog = ref(false)
const createForm = reactive({
  title: '',
  description: '',
  deadlineAt: null as string | null,
  classIds: [] as number[],
})
const editTaskForm = reactive({
  title: '',
})
const createTaskFiles = ref<File[]>([])
const createTaskFileInput = ref<HTMLInputElement | null>(null)
const creating = ref(false)
const updatingTaskTitle = ref(false)
const classScope = ref<'mine' | 'all'>('mine')
const classOptions = ref<TeacherClassVO[]>([])
const loadingClasses = ref(false)
const historyClassOptions = ref<TeacherClassVO[]>([])
const loadingHistoryClasses = ref(false)

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
const previewKind = ref<'image' | 'text'>('image')
const previewText = ref<string>('')
const taskAttachmentUploadFiles = ref<File[]>([])
const taskAttachmentUploadInput = ref<HTMLInputElement | null>(null)
const taskAttachmentUploading = ref(false)
const progressStudents = ref<TeacherTaskProgressStudentVO[]>([])
const loadingProgressStudents = ref(false)
const progressStudentQ = ref('')
const progressDetailDialog = ref(false)
const progressDetailLoading = ref(false)
const progressDetail = ref<TeacherTaskProgressDetailVO | null>(null)
const progressImageUrls = ref<Record<number, string>>({})
const confirmingCompletion = ref<number | null>(null)
const taskDevices = ref<TaskDeviceConfigVO[]>([])
const loadingTaskDevices = ref(false)
const deviceConfigDialog = ref(false)
const deviceConfigDraft = ref<Array<{ deviceId: number; deviceName: string; deviceCode: string; totalQuantity: number; deviceStatus: string; maxQuantity: number }>>([])
const savingTaskDevices = ref(false)
const deviceRequests = ref<TaskDeviceRequestVO[]>([])
const loadingDeviceRequests = ref(false)
const deviceRequestQ = ref('')
const deviceRequestStatus = ref('')
const actioningRequestId = ref<number | null>(null)

// Schedule/attendance state
const semesters = ref<Semester[]>([])
const timeSlots = ref<TimeSlot[]>([])
const scheduleSemesterId = ref<number | null>(null)
const weekStartDate = ref<string>(toYmd(new Date()))
const weekItems = ref<WeekScheduleItem[]>([])
const loadingWeek = ref(false)
const scheduleError = ref('')

const classDialog = ref(false)
const selectedCell = ref<WeekScheduleItem | null>(null)
const roster = ref<RosterStudent[]>([])
const loadingRoster = ref(false)

const session = ref<AttendanceSession | null>(null)
const tokenInfo = ref<AttendanceTokenVO | null>(null)
const qrDataUrl = ref<string | null>(null)
const staticCode = ref<string | null>(null)

const QR_MODE_KEY = 'labflow.att.qrMode'
const QR_REFRESH_SECONDS_KEY = 'labflow.att.qrRefreshSeconds'
type QrMode = 'dynamic' | 'static'

function initialQrMode(): QrMode {
  try {
    const v = window.localStorage.getItem(QR_MODE_KEY)
    if (v === 'static' || v === 'dynamic') return v
  } catch {
    // ignore
  }
  return 'dynamic'
}

function initialRefreshSeconds(): number {
  try {
    const v = window.localStorage.getItem(QR_REFRESH_SECONDS_KEY)
    const n = v ? Number(v) : NaN
    if (Number.isFinite(n) && n > 0) return Math.floor(n)
  } catch {
    // ignore
  }
  return 5
}

const qrMode = ref<QrMode>(initialQrMode())
const qrRefreshSeconds = ref<number>(initialRefreshSeconds())

function normalizeRefreshSeconds(n: number): number {
  const raw = Math.floor(Number(n))
  const min = 1
  const max = 60
  if (!Number.isFinite(raw)) return 5
  return Math.max(min, Math.min(max, raw))
}

function desiredTokenTtlSeconds(): number {
  // Keep a small buffer so that a token stays valid until the next refresh.
  // ttl = refresh + 1, capped to 60 and bounded to >= 3.
  const refresh = normalizeRefreshSeconds(qrRefreshSeconds.value)
  return Math.min(60, Math.max(3, refresh + 1))
}

const MOBILE_BASE_KEY = 'labflow.mobileBase'
const lanIps = ref<string[]>([])
const loadingLanIps = ref(false)

function initialMobileBase(): string {
  try {
    const saved = window.localStorage.getItem(MOBILE_BASE_KEY)
    if (saved && saved.trim()) return saved.trim()
  } catch {
    // ignore
  }
  return (import.meta.env.VITE_MOBILE_BASE_URL as string | undefined) ?? window.location.origin
}

const mobileBase = ref<string>(initialMobileBase())

watch(
  mobileBase,
  (v) => {
    try {
      window.localStorage.setItem(MOBILE_BASE_KEY, (v ?? '').trim())
    } catch {
      // ignore
    }
  },
  { flush: 'post' },
)

watch(
  qrMode,
  (v) => {
    try {
      window.localStorage.setItem(QR_MODE_KEY, v)
    } catch {
      // ignore
    }
    // Apply immediately if a session is open.
    if (session.value) {
      startLoops()
      if (v === 'dynamic') {
        refreshToken()
        // Keep backend TTL aligned with current refresh seconds.
        syncTokenTtl()
      }
      else refreshStaticCode()
    }
  },
  { flush: 'post' },
)

watch(
  qrRefreshSeconds,
  (v) => {
    const normalized = normalizeRefreshSeconds(v)
    if (normalized !== v) {
      qrRefreshSeconds.value = normalized
      return
    }
    try {
      window.localStorage.setItem(QR_REFRESH_SECONDS_KEY, String(normalized))
    } catch {
      // ignore
    }
    if (session.value && qrMode.value === 'dynamic') syncTokenTtlDebounced()
  },
  { flush: 'post' },
)

let syncTtlTimer: number | null = null
function syncTokenTtlDebounced() {
  if (syncTtlTimer) window.clearTimeout(syncTtlTimer)
  syncTtlTimer = window.setTimeout(() => {
    syncTtlTimer = null
    syncTokenTtl()
  }, 350)
}

function isLocalhostBase(value: string): boolean {
  try {
    const u = new URL(value)
    return u.hostname === 'localhost' || u.hostname === '127.0.0.1' || u.hostname === '::1'
  } catch {
    return false
  }
}

function buildBaseForIp(ip: string): string {
  const protocol = window.location.protocol || 'http:'
  const port = window.location.port ? `:${window.location.port}` : ''
  return `${protocol}//${ip}${port}`
}

function useRecommendedMobileBase() {
  const ip = lanIps.value[0]
  if (!ip) return
  mobileBase.value = buildBaseForIp(ip)
}

async function loadLanIps() {
  if (loadingLanIps.value) return
  loadingLanIps.value = true
  try {
    const payload = await apiData<{ ips: string[]; recommended?: string | null }>(
      '/api/ping/lan-ips',
      { method: 'GET' },
      auth.token,
    )
    lanIps.value = payload.ips ?? []

    // If current is localhost and user never set a base, auto-fill from recommended IP.
    let saved: string | null = null
    try {
      saved = window.localStorage.getItem(MOBILE_BASE_KEY)
    } catch {
      saved = null
    }

    if ((!saved || !saved.trim()) && isLocalhostBase(mobileBase.value) && payload.recommended) {
      mobileBase.value = buildBaseForIp(payload.recommended)
    }
  } catch {
    // optional helper; ignore failures
  } finally {
    loadingLanIps.value = false
  }
}

const records = ref<AttendanceRecord[]>([])
const loadingRecords = ref(false)
const historyLoading = ref(false)
const historyDetailLoading = ref(false)
const historyData = ref<PageResult<AttendanceHistoryItem>>({ page: 1, size: 20, total: 0, items: [] })
const historyFilter = reactive({
  grade: undefined as number | undefined,
  classId: undefined as number | undefined,
  roomKeyword: '',
  from: '' as string,
  to: '' as string,
  status: '',
  page: 1,
  size: 20,
})
const historyDetailDialog = ref(false)
const historyDetail = ref<AttendanceHistoryDetail | null>(null)

let tokenTimer: number | null = null
let recordsTimer: number | null = null

function parseTime(s: string | undefined | null): number {
  if (!s) return 0
  const t = Date.parse(s)
  return Number.isFinite(t) ? t : 0
}

function completionStatusText(status?: TeacherTaskProgressStudentVO['completionStatus'] | TaskCompletionVO['status']) {
  if (status === 'PENDING_CONFIRM') return '待确认'
  if (status === 'CONFIRMED') return '已确认'
  return '未登记'
}

function completionStatusType(status?: TeacherTaskProgressStudentVO['completionStatus'] | TaskCompletionVO['status']) {
  if (status === 'PENDING_CONFIRM') return 'warning'
  if (status === 'CONFIRMED') return 'success'
  return 'info'
}

function isImageLike(name?: string | null, contentType?: string | null) {
  const ct = (contentType ?? '').toLowerCase()
  const n = (name ?? '').toLowerCase()
  return ct.startsWith('image/') || /\.(png|jpe?g|gif|webp|bmp|svg)$/.test(n)
}

function revokeProgressImageUrls() {
  for (const v of Object.values(progressImageUrls.value)) {
    if (v) URL.revokeObjectURL(v)
  }
  progressImageUrls.value = {}
}

const submissionGroups = computed<SubmissionGroup[]>(() => {
  const by = new Map<string, SubmissionGroup>()
  for (const s of submissions.value) {
    if (!s) continue
    const key = (s.studentUsername || '').trim() || `sid:${s.id}`
    const g = by.get(key)
    if (!g) {
      by.set(key, {
        key,
        studentUsername: s.studentUsername,
        studentDisplayName: s.studentDisplayName,
        latest: s,
        versions: [s],
      })
      continue
    }
    g.versions.push(s)
    // pick latest: prefer higher version; if tie, later submittedAt
    const a = g.latest
    const aVer = Number(a.versionNo || 0)
    const bVer = Number(s.versionNo || 0)
    if (bVer > aVer) {
      g.latest = s
    } else if (bVer === aVer) {
      if (parseTime(s.submittedAt) > parseTime(a.submittedAt)) g.latest = s
    }
  }

  const groups = Array.from(by.values())
  for (const g of groups) {
    g.versions.sort((x, y) => {
      const vx = Number(x.versionNo || 0)
      const vy = Number(y.versionNo || 0)
      if (vy !== vx) return vy - vx
      return parseTime(y.submittedAt) - parseTime(x.submittedAt)
    })
    g.latest = g.versions[0]!
  }

  groups.sort((a, b) => parseTime(b.latest.submittedAt) - parseTime(a.latest.submittedAt))
  return groups
})

const filteredSubmissionGroups = computed(() => {
  const q = (submissionQ.value || '').trim().toLowerCase()
  if (!q) return submissionGroups.value
  return submissionGroups.value.filter((g) => {
    const a = (g.studentDisplayName || '').toLowerCase()
    const b = (g.studentUsername || '').toLowerCase()
    return a.includes(q) || b.includes(q)
  })
})

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
  await loadTaskDetail(taskId)
  if (activeTab.value === 'report') {
    await loadSubmissions()
    return
  }
  if (activeTab.value === 'flow') {
    await Promise.all([loadProgressStudents(), loadTaskDevices(), loadDeviceRequests()])
    return
  }
  await loadSubmissions()
}

async function loadTaskDetail(taskId: number) {
  taskDetail.value = await apiData<TaskVO>(`/api/tasks/${taskId}`, { method: 'GET' }, auth.token)
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

async function loadProgressStudents() {
  if (!selectedTaskId.value) return
  loadingProgressStudents.value = true
  try {
    const q = progressStudentQ.value.trim()
    const query = q ? `?q=${encodeURIComponent(q)}` : ''
    progressStudents.value = await apiData<TeacherTaskProgressStudentVO[]>(
      `/api/teacher/tasks/${selectedTaskId.value}/progress${query}`,
      { method: 'GET' },
      auth.token,
    )
  } catch (e: any) {
    progressStudents.value = []
    ElMessage.error(e?.message ?? '加载实验过程失败')
  } finally {
    loadingProgressStudents.value = false
  }
}

async function ensureProgressDetailImageUrls(logs: TaskProgressVO[]) {
  revokeProgressImageUrls()
  for (const log of logs) {
    for (const att of log.attachments || []) {
      if (!isImageLike(att.fileName, att.contentType)) continue
      try {
        const { blob } = await fetchBlob(`/api/progress-attachments/${att.id}/download`, { token: auth.token })
        progressImageUrls.value[att.id] = URL.createObjectURL(blob)
      } catch {
        // ignore preview failures
      }
    }
  }
}

async function openProgressDetail(row: TeacherTaskProgressStudentVO) {
  if (!selectedTaskId.value) return
  progressDetailDialog.value = true
  progressDetailLoading.value = true
  progressDetail.value = null
  try {
    progressDetail.value = await apiData<TeacherTaskProgressDetailVO>(
      `/api/teacher/tasks/${selectedTaskId.value}/progress/${row.studentId}`,
      { method: 'GET' },
      auth.token,
    )
    await ensureProgressDetailImageUrls(progressDetail.value.logs || [])
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载学生过程失败')
    progressDetailDialog.value = false
  } finally {
    progressDetailLoading.value = false
  }
}

async function confirmCompletion(row: TeacherTaskProgressStudentVO) {
  if (!selectedTaskId.value) return
  try {
    await ElMessageBox.confirm(
      `确认 ${row.studentDisplayName}（${row.studentUsername}）已完成全部实验步骤并登记？`,
      '确认登记',
      { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  confirmingCompletion.value = row.studentId
  try {
    await apiData(
      `/api/teacher/tasks/${selectedTaskId.value}/completion/${row.studentId}/confirm`,
      { method: 'POST' },
      auth.token,
    )
    ElMessage.success('已确认登记')
    await loadProgressStudents()
    if (progressDetail.value?.studentId === row.studentId) {
      await openProgressDetail(row)
    }
  } catch (e: any) {
    ElMessage.error(e?.message ?? '确认失败')
  } finally {
    confirmingCompletion.value = null
  }
}

async function loadTaskDevices() {
  if (!selectedTaskId.value) return
  loadingTaskDevices.value = true
  try {
    taskDevices.value = await apiData<TaskDeviceConfigVO[]>(
      `/api/teacher/tasks/${selectedTaskId.value}/devices`,
      { method: 'GET' },
      auth.token,
    )
  } catch (e: any) {
    taskDevices.value = []
    ElMessage.error(e?.message ?? '加载任务设备配置失败')
  } finally {
    loadingTaskDevices.value = false
  }
}

function openDeviceConfigDialog() {
  deviceConfigDraft.value = taskDevices.value.map((item) => ({
    deviceId: item.deviceId,
    deviceName: item.deviceName,
    deviceCode: item.deviceCode,
    totalQuantity: item.totalQuantity,
    deviceStatus: item.deviceStatus,
    maxQuantity: item.configuredQuantity,
  }))
  deviceConfigDialog.value = true
}

async function saveTaskDevices() {
  if (!selectedTaskId.value) return
  savingTaskDevices.value = true
  try {
    await apiData(
      `/api/teacher/tasks/${selectedTaskId.value}/devices`,
      {
        method: 'PUT',
        body: deviceConfigDraft.value.map((item) => ({
          deviceId: item.deviceId,
          maxQuantity: Math.max(0, Math.floor(Number(item.maxQuantity || 0))),
        })),
      },
      auth.token,
    )
    ElMessage.success('任务设备配置已更新')
    deviceConfigDialog.value = false
    await loadTaskDevices()
    await loadDeviceRequests()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '保存配置失败')
  } finally {
    savingTaskDevices.value = false
  }
}

async function loadDeviceRequests() {
  if (!selectedTaskId.value) return
  loadingDeviceRequests.value = true
  try {
    const q = new URLSearchParams()
    if (deviceRequestStatus.value) q.set('status', deviceRequestStatus.value)
    if (deviceRequestQ.value.trim()) q.set('q', deviceRequestQ.value.trim())
    const suffix = q.toString() ? `?${q.toString()}` : ''
    deviceRequests.value = await apiData<TaskDeviceRequestVO[]>(
      `/api/teacher/tasks/${selectedTaskId.value}/device-requests${suffix}`,
      { method: 'GET' },
      auth.token,
    )
  } catch (e: any) {
    deviceRequests.value = []
    ElMessage.error(e?.message ?? '加载设备申请失败')
  } finally {
    loadingDeviceRequests.value = false
  }
}

async function changeDeviceRequestStatus(
  row: TaskDeviceRequestVO,
  action: 'approve' | 'reject' | 'checkout' | 'return',
  successMessage: string,
) {
  const actionTextMap = {
    approve: '通过',
    reject: '驳回',
    checkout: '登记借出',
    return: '登记归还',
  } as const
  try {
    await ElMessageBox.confirm(
      `确认对 ${row.studentDisplayName}（${row.studentUsername}）的设备申请执行“${actionTextMap[action]}”吗？`,
      '确认操作',
      { type: action === 'reject' ? 'warning' : 'info', confirmButtonText: '确认', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  actioningRequestId.value = row.id
  try {
    await apiData(`/api/teacher/device-requests/${row.id}/${action}`, { method: 'POST' }, auth.token)
    ElMessage.success(successMessage)
    await loadTaskDevices()
    await loadDeviceRequests()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '操作失败')
  } finally {
    actioningRequestId.value = null
  }
}

async function exportDeviceRequests() {
  if (!selectedTaskId.value) return
  try {
    await downloadBlob(`/api/teacher/tasks/${selectedTaskId.value}/device-requests/export`, {
      token: auth.token,
      fallbackFilename: `task-${selectedTaskId.value}-device-requests.csv`,
    })
    ElMessage.success('已开始下载借用记录 CSV')
  } catch (e: any) {
    ElMessage.error(e?.message ?? '导出失败')
  }
}

async function previewProgressAttachment(row: ProgressAttachmentVO) {
  try {
    closePreview()
    const { blob, contentType } = await fetchBlob(`/api/progress-attachments/${row.id}/download`, { token: auth.token })
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

async function downloadProgressAttachment(row: ProgressAttachmentVO) {
  try {
    await downloadBlob(`/api/progress-attachments/${row.id}/download`, {
      token: auth.token,
      fallbackFilename: row.fileName || `progress-attachment-${row.id}`,
    })
  } catch (e: any) {
    ElMessage.error(e?.message ?? '下载失败')
  }
}

async function createTask() {
  if (!createForm.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  creating.value = true
  try {
    const created = await apiData<TaskVO>(
      '/api/tasks',
      {
        method: 'POST',
        body: {
          title: createForm.title,
          description: createForm.description,
          deadlineAt: createForm.deadlineAt,
          classIds: createForm.classIds,
        },
      },
      auth.token,
    )
    if (createTaskFiles.value.length > 0) {
      const fd = new FormData()
      for (const file of createTaskFiles.value) fd.append('files', file)
      await uploadFormData(`/api/tasks/${created.id}/attachments`, { token: auth.token, formData: fd })
    }
    ElMessage.success('任务创建成功')
    createDialog.value = false
    createForm.title = ''
    createForm.description = ''
    createForm.deadlineAt = null
    createForm.classIds = []
    createTaskFiles.value = []
    if (createTaskFileInput.value) createTaskFileInput.value.value = ''
    await loadTasks()
    await selectTask(created.id)
  } catch (e: any) {
    ElMessage.error(e?.message ?? '创建失败')
  } finally {
    creating.value = false
  }
}

function onPickCreateTaskFiles(ev: Event) {
  const input = ev.target as HTMLInputElement
  createTaskFiles.value = Array.from(input.files ?? [])
  createTaskFileInput.value = input
}

function onPickTaskAttachmentFiles(ev: Event) {
  const input = ev.target as HTMLInputElement
  taskAttachmentUploadFiles.value = Array.from(input.files ?? [])
  taskAttachmentUploadInput.value = input
}

async function uploadTaskAttachments() {
  if (!selectedTaskId.value) return
  if (taskAttachmentUploadFiles.value.length === 0) {
    ElMessage.warning('请选择要上传的任务附件')
    return
  }
  taskAttachmentUploading.value = true
  try {
    const fd = new FormData()
    for (const file of taskAttachmentUploadFiles.value) fd.append('files', file)
    const updated = await uploadFormData<TaskAttachmentVO[]>(`/api/tasks/${selectedTaskId.value}/attachments`, {
      token: auth.token,
      formData: fd,
    })
    if (taskDetail.value) taskDetail.value.attachments = updated
    taskAttachmentUploadFiles.value = []
    if (taskAttachmentUploadInput.value) taskAttachmentUploadInput.value.value = ''
    ElMessage.success('任务附件上传成功')
  } catch (e: any) {
    ElMessage.error(e?.message ?? '上传任务附件失败')
  } finally {
    taskAttachmentUploading.value = false
  }
}

async function deleteTaskAttachment(row: TaskAttachmentVO) {
  if (!selectedTaskId.value) return
  try {
    await ElMessageBox.confirm(`确定删除任务附件《${row.fileName}》？`, '删除附件', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await apiData(`/api/tasks/${selectedTaskId.value}/attachments/${row.id}`, { method: 'DELETE' }, auth.token)
    if (taskDetail.value) {
      taskDetail.value.attachments = (taskDetail.value.attachments || []).filter((item) => item.id !== row.id)
    }
    ElMessage.success('任务附件已删除')
  } catch (e: any) {
    ElMessage.error(e?.message ?? '删除失败')
  }
}

async function downloadTaskAttachment(row: TaskAttachmentVO) {
  try {
    await downloadBlob(`/api/task-attachments/${row.id}/download`, {
      token: auth.token,
      fallbackFilename: row.fileName || `task-attachment-${row.id}`,
    })
  } catch (e: any) {
    ElMessage.error(e?.message ?? '下载失败')
  }
}

async function previewTaskAttachment(row: TaskAttachmentVO) {
  try {
    closePreview()
    const { blob, contentType } = await fetchBlob(`/api/task-attachments/${row.id}/download`, { token: auth.token })
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

async function loadClassOptions() {
  loadingClasses.value = true
  try {
    classOptions.value = await apiData<TeacherClassVO[]>(
      `/api/teacher/classes?scope=${classScope.value}`,
      { method: 'GET' },
      auth.token,
    )
  } catch (e: any) {
    classOptions.value = []
    ElMessage.error(e?.message ?? '加载班级列表失败')
  } finally {
    loadingClasses.value = false
  }
}

async function loadHistoryClassOptions() {
  loadingHistoryClasses.value = true
  try {
    historyClassOptions.value = await apiData<TeacherClassVO[]>(
      '/api/teacher/classes?scope=mine',
      { method: 'GET' },
      auth.token,
    )
  } catch (e: any) {
    historyClassOptions.value = []
    ElMessage.error(e?.message ?? '加载历史签到班级失败')
  } finally {
    loadingHistoryClasses.value = false
  }
}

function buildHistoryQuery() {
  const q = new URLSearchParams()
  if (historyFilter.grade) q.set('grade', String(historyFilter.grade))
  if (historyFilter.classId) q.set('classId', String(historyFilter.classId))
  if (historyFilter.roomKeyword.trim()) q.set('roomKeyword', historyFilter.roomKeyword.trim())
  if (historyFilter.from) q.set('from', historyFilter.from)
  if (historyFilter.to) q.set('to', historyFilter.to)
  if (historyFilter.status) q.set('status', historyFilter.status)
  q.set('page', String(historyFilter.page))
  q.set('size', String(historyFilter.size))
  return q.toString()
}

async function loadHistorySessions(resetPage = false) {
  if (resetPage) historyFilter.page = 1
  historyLoading.value = true
  try {
    historyData.value = await apiData<PageResult<AttendanceHistoryItem>>(
      `/api/teacher/attendance/sessions?${buildHistoryQuery()}`,
      { method: 'GET' },
      auth.token,
    )
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载历史签到失败')
  } finally {
    historyLoading.value = false
  }
}

async function openHistoryDetail(row: AttendanceHistoryItem) {
  historyDetailDialog.value = true
  historyDetailLoading.value = true
  historyDetail.value = null
  try {
    historyDetail.value = await apiData<AttendanceHistoryDetail>(
      `/api/teacher/attendance/sessions/${row.sessionId}/detail`,
      { method: 'GET' },
      auth.token,
    )
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载签到详情失败')
    historyDetailDialog.value = false
  } finally {
    historyDetailLoading.value = false
  }
}

async function exportHistoryAttendanceCsv(sessionId: number) {
  try {
    await downloadBlob(`/api/attendance/sessions/${sessionId}/export`, {
      token: auth.token,
      fallbackFilename: `attendance-session-${sessionId}.csv`,
    })
    ElMessage.success('已开始下载签到 CSV')
  } catch (e: any) {
    ElMessage.error(e?.message ?? '导出失败')
  }
}

watch(
  () => createDialog.value,
  async (open) => {
    if (!open) {
      createTaskFiles.value = []
      if (createTaskFileInput.value) createTaskFileInput.value.value = ''
      return
    }
    await loadClassOptions()
  },
)

watch(classScope, async () => {
  if (!createDialog.value) return
  createForm.classIds = []
  await loadClassOptions()
})

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

async function setTaskStatus(status: 'OPEN' | 'CLOSED') {
  if (!taskDetail.value) return
  try {
    await apiData(
      `/api/tasks/${taskDetail.value.id}/status`,
      { method: 'PUT', body: { status } },
      auth.token,
    )
    ElMessage.success(status === 'OPEN' ? '任务已重新开放' : '任务已关闭')
    await loadTasks()
    if (selectedTaskId.value) {
      await selectTask(selectedTaskId.value)
    }
  } catch (e: any) {
    ElMessage.error(e?.message ?? '更新任务状态失败')
  }
}

async function confirmSetTaskStatus(status: 'OPEN' | 'CLOSED') {
  if (!taskDetail.value) return
  const isClose = status === 'CLOSED'
  try {
    await ElMessageBox.confirm(
      isClose ? '关闭后学生将无法继续提交新版本报告，确定要关闭任务吗？' : '确定要重新开放任务吗？',
      isClose ? '确认关闭' : '确认开放',
      { type: isClose ? 'warning' : 'info', confirmButtonText: '确定', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  await setTaskStatus(status)
}

async function deleteTask() {
  if (!taskDetail.value) return
  const deletingId = taskDetail.value.id
  try {
    await ElMessageBox.confirm(
      `确定删除任务《${taskDetail.value.title}》？仅允许删除没有提交、实验步骤、完成登记、设备申请和查重记录的任务。`,
      '删除任务',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }

  try {
    await apiData(`/api/tasks/${deletingId}`, { method: 'DELETE' }, auth.token)
    ElMessage.success('任务已删除')
    if (selectedTaskId.value === deletingId) {
      selectedTaskId.value = null
      taskDetail.value = null
    }
    await loadTasks()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '删除任务失败')
  }
}

function openEditTaskDialog() {
  if (!taskDetail.value) return
  editTaskForm.title = taskDetail.value.title || ''
  editTaskDialog.value = true
}

async function updateTaskTitle() {
  if (!taskDetail.value) return
  if (!editTaskForm.title.trim()) {
    ElMessage.warning('请输入任务名')
    return
  }
  updatingTaskTitle.value = true
  try {
    const updated = await apiData<TaskVO>(
      `/api/tasks/${taskDetail.value.id}/title`,
      { method: 'PUT', body: { title: editTaskForm.title.trim() } },
      auth.token,
    )
    taskDetail.value = updated
    await loadTasks()
    editTaskDialog.value = false
    ElMessage.success('任务名已更新')
  } catch (e: any) {
    ElMessage.error(e?.message ?? '更新任务名失败')
  } finally {
    updatingTaskTitle.value = false
  }
}

onMounted(loadTasks)

watch(activeTab, async (tab) => {
  if (tab === 'flow') {
    if (selectedTaskId.value) {
      await Promise.all([loadProgressStudents(), loadTaskDevices(), loadDeviceRequests()])
    }
    return
  }
  if (tab === 'report') {
    if (selectedTaskId.value) {
      await loadSubmissions()
    }
    return
  }
  if (tab === 'schedule') {
    try {
      await ensureScheduleMeta()
      await loadWeek()
    } catch (e: any) {
      ElMessage.error(e?.message ?? '加载课表元数据失败')
    }
    return
  }
  if (tab === 'history') {
    if (historyClassOptions.value.length === 0) {
      await loadHistoryClassOptions()
    }
    if (historyData.value.items.length === 0) {
      await loadHistorySessions()
    }
  }
})

onBeforeUnmount(() => {
  stopLoops()
  revokeProgressImageUrls()
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

function getWeekStartYmd(date: Date): string {
  const copy = new Date(date)
  const day = copy.getDay()
  const diff = day === 0 ? -6 : 1 - day
  copy.setDate(copy.getDate() + diff)
  return toYmd(copy)
}

function semesterContainsDate(semester: Semester, ymd: string) {
  if (!semester.startDate || !semester.endDate) return false
  return semester.startDate <= ymd && ymd <= semester.endDate
}

function weekIntersectsSemester(weekStart: string, semester: Semester) {
  if (!semester.startDate || !semester.endDate) return true
  const end = new Date(`${weekStart}T00:00:00`)
  end.setDate(end.getDate() + 6)
  return toYmd(end) >= semester.startDate && weekStart <= semester.endDate
}

function pickDefaultSemesterId(list: Semester[]) {
  const today = toYmd(new Date())
  return list.find((s) => semesterContainsDate(s, today))?.id ?? list[0]?.id ?? null
}

function alignWeekStartToSemester(semesterId: number | null) {
  if (!semesterId) return
  const semester = semesters.value.find((s) => s.id === semesterId)
  if (!semester?.startDate) return
  if (weekIntersectsSemester(weekStartDate.value, semester)) return
  weekStartDate.value = getWeekStartYmd(new Date(`${semester.startDate}T00:00:00`))
}

async function ensureScheduleMeta() {
  if (semesters.value.length > 0 && timeSlots.value.length > 0) return
  semesters.value = await apiData<Semester[]>('/api/teacher/semesters', { method: 'GET' }, auth.token)
  timeSlots.value = await apiData<TimeSlot[]>('/api/teacher/time-slots', { method: 'GET' }, auth.token)
  scheduleSemesterId.value = pickDefaultSemesterId(semesters.value)
  alignWeekStartToSemester(scheduleSemesterId.value)
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

const scheduleEmptyHint = computed(
  () => !loadingWeek.value && !scheduleError.value && timeSlots.value.length > 0 && weekItems.value.length === 0,
)

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
  scheduleError.value = ''
  try {
    weekItems.value = await apiData<WeekScheduleItem[]>(
      `/api/teacher/schedule/week?semesterId=${scheduleSemesterId.value}&weekStartDate=${encodeURIComponent(weekStartDate.value)}`,
      { method: 'GET' },
      auth.token,
    )
  } catch (e: any) {
    weekItems.value = []
    scheduleError.value = e?.message ?? '加载课表失败'
    ElMessage.error(scheduleError.value)
  } finally {
    loadingWeek.value = false
  }
}

watch(
  () => scheduleSemesterId.value,
  (id) => {
    alignWeekStartToSemester(id)
  },
)

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
  await loadLanIps()
  stopLoops()
  session.value = null
  tokenInfo.value = null
  qrDataUrl.value = null
  staticCode.value = null
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
      {
        method: 'POST',
        body:
          qrMode.value === 'dynamic'
            ? { scheduleId: selectedCell.value.id, tokenTtlSeconds: desiredTokenTtlSeconds() }
            : { scheduleId: selectedCell.value.id },
      },
      auth.token,
    )
    ElMessage.success('签到已开启')
    if (qrMode.value === 'dynamic') {
      await refreshToken()
    } else {
      await refreshStaticCode()
    }
    await refreshRecords()
    startLoops()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '开启签到失败')
  }
}

async function syncTokenTtl() {
  if (!session.value) return
  if (qrMode.value !== 'dynamic') return

  const ttl = desiredTokenTtlSeconds()
  if (tokenInfo.value?.ttlSeconds === ttl) {
    startLoops()
    return
  }
  try {
    await apiData(
      `/api/attendance/sessions/${session.value.id}/token-ttl`,
      { method: 'PUT', body: { tokenTtlSeconds: ttl } },
      auth.token,
    )
    await refreshToken()
    startLoops()
  } catch (e: any) {
    // Non-fatal: QR refresh can still work; keep timer alive.
    startLoops()
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
  } catch (e: any) {
    ElMessage.error(e?.message ?? '刷新二维码失败')
  }
}

async function refreshStaticCode() {
  if (!session.value) return
  try {
    const res = await apiData<AttendanceStaticCodeVO>(
      `/api/attendance/sessions/${session.value.id}/static-code`,
      { method: 'GET' },
      auth.token,
    )
    staticCode.value = res.code
  } catch (e: any) {
    ElMessage.error(e?.message ?? '获取静态二维码失败')
  }
}

async function refreshQr() {
  if (qrMode.value === 'dynamic') {
    await refreshToken()
  } else {
    await refreshStaticCode()
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
  // QR refresh
  if (qrMode.value === 'dynamic') {
    const seconds = normalizeRefreshSeconds(qrRefreshSeconds.value)
    qrRefreshSeconds.value = seconds
    tokenTimer = window.setInterval(refreshToken, seconds * 1000)
  }
  recordsTimer = window.setInterval(refreshRecords, 2000)
}

function stopLoops() {
  if (tokenTimer) window.clearInterval(tokenTimer)
  if (recordsTimer) window.clearInterval(recordsTimer)
  tokenTimer = null
  recordsTimer = null
  if (syncTtlTimer) window.clearTimeout(syncTtlTimer)
  syncTtlTimer = null
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
  if (qrMode.value === 'static') {
    const c = staticCode.value ?? ''
    if (!c) return ''
    const base = (mobileBase.value ?? '').trim().replace(/\/$/, '')
    return `${base}/m/checkin?c=${encodeURIComponent(c)}`
  }

  const t = tokenInfo.value?.token ?? ''
  if (!t) return ''
  const base = (mobileBase.value ?? '').trim().replace(/\/$/, '')
  return `${base}/m/checkin?t=${encodeURIComponent(t)}`
})

let qrGenSeq = 0
watch(
  checkinLink,
  async (link) => {
    if (!session.value) return
    if (!link) {
      qrDataUrl.value = null
      return
    }
    const seq = ++qrGenSeq
    try {
      const url = await QRCode.toDataURL(link, { width: 220, margin: 1 })
      if (seq === qrGenSeq) qrDataUrl.value = url
    } catch {
      // ignore
    }
  },
  { flush: 'post' },
)

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
      <el-aside v-if="(activeTab === 'report' || activeTab === 'flow') && !isMobile" width="360px" class="aside">
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
          <el-tab-pane label="实验过程管理" name="flow">
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
                <div style="display: flex; justify-content: space-between; align-items: center; gap: 10px; flex-wrap: wrap">
                  <div style="display: flex; align-items: center; gap: 10px; flex-wrap: wrap">
                    <div>{{ taskDetail.title }}</div>
                    <el-tag v-if="taskDetail.status" :type="taskDetail.status === 'OPEN' ? 'success' : 'info'">
                      {{ taskDetail.status }}
                    </el-tag>
                  </div>
                  <div style="display: flex; align-items: center; gap: 10px; flex-wrap: wrap">
                    <div class="meta">截止：{{ taskDetail.deadlineAt || '-' }}</div>
                    <el-button size="small" @click="openEditTaskDialog">修改任务名</el-button>
                    <el-button size="small" type="danger" plain @click="deleteTask">删除任务</el-button>
                  </div>
                </div>
              </template>
              <div style="white-space: pre-wrap">{{ taskDetail.description || '（无说明）' }}</div>
              <div style="margin-top: 14px">
                <div class="meta" style="margin-bottom: 8px">任务附件/资料</div>
                <template v-if="!isMobile">
                  <el-table :data="taskDetail.attachments || []" size="small" empty-text="暂无任务附件">
                    <el-table-column prop="fileName" label="文件名" min-width="280" />
                    <el-table-column prop="fileSize" label="大小(Byte)" width="120" />
                    <el-table-column prop="uploadedAt" label="上传时间" min-width="180" />
                    <el-table-column label="操作" width="260">
                      <template #default="{ row }: { row: TaskAttachmentVO }">
                        <el-button size="small" @click="downloadTaskAttachment(row)">下载</el-button>
                        <el-button size="small" @click="previewTaskAttachment(row)">预览</el-button>
                        <el-button size="small" type="danger" @click="deleteTaskAttachment(row)">删除</el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </template>
                <template v-else>
                  <div v-if="(taskDetail.attachments || []).length === 0" class="meta">暂无任务附件</div>
                  <el-card v-else v-for="att in taskDetail.attachments || []" :key="att.id" shadow="never" class="attCard">
                    <div class="attName">{{ att.fileName }}</div>
                    <div class="meta" style="margin-top: 6px">大小：{{ att.fileSize }} Byte</div>
                    <div class="meta">上传时间：{{ att.uploadedAt }}</div>
                    <div style="display: flex; gap: 10px; margin-top: 10px; flex-wrap: wrap">
                      <el-button size="small" @click="downloadTaskAttachment(att)">下载</el-button>
                      <el-button size="small" @click="previewTaskAttachment(att)">预览</el-button>
                      <el-button size="small" type="danger" @click="deleteTaskAttachment(att)">删除</el-button>
                    </div>
                  </el-card>
                </template>
                <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap; margin-top: 10px">
                  <input type="file" multiple @change="onPickTaskAttachmentFiles" />
                  <el-button type="primary" size="small" :loading="taskAttachmentUploading" @click="uploadTaskAttachments">追加上传</el-button>
                  <div class="meta" v-if="taskAttachmentUploadFiles.length > 0">已选择 {{ taskAttachmentUploadFiles.length }} 个文件</div>
                </div>
              </div>
            </el-card>

            <el-card class="block" shadow="never" v-if="selectedTaskId">
              <template #header>
                <div style="display: flex; justify-content: space-between; align-items: center; gap: 10px; flex-wrap: wrap">
                  <div>实验过程管理</div>
                  <div style="display: flex; gap: 10px; flex-wrap: wrap">
                    <el-radio-group v-model="flowSubTab" size="small">
                      <el-radio-button label="progress">过程监督</el-radio-button>
                      <el-radio-button label="device">设备审批</el-radio-button>
                    </el-radio-group>
                  </div>
                </div>
              </template>

              <template v-if="flowSubTab === 'progress'">
                <div class="toolbar" style="margin-bottom: 12px">
                  <el-input v-model="progressStudentQ" clearable placeholder="搜索学生姓名/账号" style="width: 280px" />
                  <el-button type="primary" :loading="loadingProgressStudents" @click="loadProgressStudents">查询</el-button>
                  <el-button :loading="loadingProgressStudents" @click="loadProgressStudents">刷新</el-button>
                </div>

                <template v-if="!isMobile">
                  <el-table :data="progressStudents" v-loading="loadingProgressStudents" stripe>
                    <el-table-column prop="studentDisplayName" label="学生姓名" min-width="140" />
                    <el-table-column prop="studentUsername" label="用户名" width="140" />
                    <el-table-column prop="classDisplayName" label="班级" min-width="150" />
                    <el-table-column prop="progressCount" label="进度条数" width="100" />
                    <el-table-column label="登记状态" width="120">
                      <template #default="{ row }: { row: TeacherTaskProgressStudentVO }">
                        <el-tag size="small" :type="completionStatusType(row.completionStatus)">{{ completionStatusText(row.completionStatus) }}</el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column label="最近更新时间" min-width="180">
                      <template #default="{ row }: { row: TeacherTaskProgressStudentVO }">
                        {{ row.latestUpdatedAt || '-' }}
                      </template>
                    </el-table-column>
                    <el-table-column label="操作" width="220" fixed="right">
                      <template #default="{ row }: { row: TeacherTaskProgressStudentVO }">
                        <el-button size="small" @click="openProgressDetail(row)">查看过程</el-button>
                        <el-button
                          v-if="row.completionStatus === 'PENDING_CONFIRM'"
                          size="small"
                          type="primary"
                          :loading="confirmingCompletion === row.studentId"
                          @click="confirmCompletion(row)"
                        >
                          确认登记
                        </el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </template>
                <template v-else>
                  <div v-if="loadingProgressStudents" class="meta">加载中...</div>
                  <div v-else-if="progressStudents.length === 0" class="meta">暂无学生过程记录</div>
                  <el-card v-else v-for="row in progressStudents" :key="row.studentId" shadow="never" class="stuCard">
                    <div class="stuHead">
                      <div>
                        <div class="stuName">{{ row.studentDisplayName }}</div>
                        <div class="meta">{{ row.studentUsername }}</div>
                      </div>
                      <el-tag size="small" :type="completionStatusType(row.completionStatus)">{{ completionStatusText(row.completionStatus) }}</el-tag>
                    </div>
                    <div class="meta" style="margin-top: 8px">班级：{{ row.classDisplayName || '-' }}</div>
                    <div class="meta">进度条数：{{ row.progressCount }}</div>
                    <div class="meta">最近更新时间：{{ row.latestUpdatedAt || '-' }}</div>
                    <div class="stuActions">
                      <el-button size="small" @click="openProgressDetail(row)">查看过程</el-button>
                      <el-button
                        v-if="row.completionStatus === 'PENDING_CONFIRM'"
                        size="small"
                        type="primary"
                        :loading="confirmingCompletion === row.studentId"
                        @click="confirmCompletion(row)"
                      >
                        确认登记
                      </el-button>
                    </div>
                  </el-card>
                </template>
              </template>

              <template v-else>
                <div class="toolbar" style="margin-bottom: 12px">
                  <el-button type="primary" @click="openDeviceConfigDialog">任务设备配置</el-button>
                  <el-button @click="exportDeviceRequests">借用历史导出</el-button>
                  <el-input v-model="deviceRequestQ" clearable placeholder="搜索学生姓名/账号" style="width: 240px" />
                  <el-select v-model="deviceRequestStatus" clearable placeholder="状态" style="width: 160px">
                    <el-option label="PENDING" value="PENDING" />
                    <el-option label="APPROVED" value="APPROVED" />
                    <el-option label="REJECTED" value="REJECTED" />
                    <el-option label="BORROWED" value="BORROWED" />
                    <el-option label="RETURNED" value="RETURNED" />
                  </el-select>
                  <el-button type="primary" :loading="loadingDeviceRequests" @click="loadDeviceRequests">查询</el-button>
                </div>

                <el-card shadow="never" class="block">
                  <template #header>
                    <div>当前任务可借设备</div>
                  </template>
                  <div v-if="loadingTaskDevices" class="meta">加载中...</div>
                  <div v-else-if="taskDevices.length === 0" class="meta">当前任务未配置设备</div>
                  <div v-else class="deviceGrid">
                    <div v-for="device in taskDevices" :key="device.deviceId" class="deviceBox">
                      <div class="deviceTitle">{{ device.deviceName }}</div>
                      <div class="meta">编码：{{ device.deviceCode }}</div>
                      <div class="meta">任务上限：{{ device.configuredQuantity }}</div>
                      <div class="meta">总库存：{{ device.totalQuantity }}</div>
                      <div class="meta">已占用：{{ device.reservedQuantity }}</div>
                      <div class="meta">当前可借：{{ device.availableQuantity }}</div>
                      <div class="meta">状态：{{ device.deviceStatus }}</div>
                    </div>
                  </div>
                </el-card>

                <template v-if="!isMobile">
                  <el-table :data="deviceRequests" v-loading="loadingDeviceRequests" stripe>
                    <el-table-column prop="createdAt" label="申请时间" min-width="170" />
                    <el-table-column prop="studentDisplayName" label="学生姓名" width="140" />
                    <el-table-column prop="studentUsername" label="用户名" width="140" />
                    <el-table-column prop="deviceName" label="设备名称" min-width="150" />
                    <el-table-column prop="quantity" label="申请数量" width="100" />
                    <el-table-column prop="status" label="状态" width="140" />
                    <el-table-column label="备注/时间" min-width="220">
                      <template #default="{ row }: { row: TaskDeviceRequestVO }">
                        <div v-if="row.note" class="meta">说明：{{ row.note }}</div>
                        <div v-if="row.approvedAt" class="meta">通过：{{ row.approvedAt }}</div>
                        <div v-if="row.checkoutAt" class="meta">借出：{{ row.checkoutAt }}</div>
                        <div v-if="row.returnAt" class="meta">归还：{{ row.returnAt }}</div>
                      </template>
                    </el-table-column>
                    <el-table-column label="操作" width="260" fixed="right">
                      <template #default="{ row }: { row: TaskDeviceRequestVO }">
                        <div class="tableActions">
                          <el-button v-if="row.status === 'PENDING'" size="small" type="primary" :loading="actioningRequestId === row.id" @click="changeDeviceRequestStatus(row, 'approve', '审批通过')">通过</el-button>
                          <el-button v-if="row.status === 'PENDING'" size="small" type="danger" :loading="actioningRequestId === row.id" @click="changeDeviceRequestStatus(row, 'reject', '已驳回')">驳回</el-button>
                          <el-button v-if="row.status === 'APPROVED'" size="small" :loading="actioningRequestId === row.id" @click="changeDeviceRequestStatus(row, 'checkout', '已登记借出')">登记借出</el-button>
                          <el-button v-if="row.status === 'BORROWED'" size="small" :loading="actioningRequestId === row.id" @click="changeDeviceRequestStatus(row, 'return', '已登记归还')">登记归还</el-button>
                        </div>
                      </template>
                    </el-table-column>
                  </el-table>
                </template>
                <template v-else>
                  <div v-if="loadingDeviceRequests" class="meta">加载中...</div>
                  <div v-else-if="deviceRequests.length === 0" class="meta">暂无设备申请记录</div>
                  <el-card v-else v-for="row in deviceRequests" :key="row.id" shadow="never" class="stuCard">
                    <div class="stuHead">
                      <div>
                        <div class="stuName">{{ row.studentDisplayName }}</div>
                        <div class="meta">{{ row.studentUsername }}</div>
                      </div>
                      <el-tag size="small">{{ row.status }}</el-tag>
                    </div>
                    <div class="meta" style="margin-top: 8px">设备：{{ row.deviceName }} / 数量：{{ row.quantity }}</div>
                    <div class="meta">申请时间：{{ row.createdAt }}</div>
                    <div v-if="row.note" class="meta">说明：{{ row.note }}</div>
                    <div v-if="row.approvedAt" class="meta">通过：{{ row.approvedAt }}</div>
                    <div v-if="row.checkoutAt" class="meta">借出：{{ row.checkoutAt }}</div>
                    <div v-if="row.returnAt" class="meta">归还：{{ row.returnAt }}</div>
                    <div class="stuActions">
                      <el-button v-if="row.status === 'PENDING'" size="small" type="primary" :loading="actioningRequestId === row.id" @click="changeDeviceRequestStatus(row, 'approve', '审批通过')">通过</el-button>
                      <el-button v-if="row.status === 'PENDING'" size="small" type="danger" :loading="actioningRequestId === row.id" @click="changeDeviceRequestStatus(row, 'reject', '已驳回')">驳回</el-button>
                      <el-button v-if="row.status === 'APPROVED'" size="small" :loading="actioningRequestId === row.id" @click="changeDeviceRequestStatus(row, 'checkout', '已登记借出')">登记借出</el-button>
                      <el-button v-if="row.status === 'BORROWED'" size="small" :loading="actioningRequestId === row.id" @click="changeDeviceRequestStatus(row, 'return', '已登记归还')">登记归还</el-button>
                    </div>
                  </el-card>
                </template>
              </template>
            </el-card>
          </el-tab-pane>

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
                  <div style="display: flex; align-items: center; gap: 10px; flex-wrap: wrap">
                    <div>{{ taskDetail.title }}</div>
                    <el-tag v-if="taskDetail.status" :type="taskDetail.status === 'OPEN' ? 'success' : 'info'">
                      {{ taskDetail.status }}
                    </el-tag>
                  </div>
                  <div>
                    <el-button size="small" @click="loadSubmissions" :loading="loadingSubs">刷新提交</el-button>
                    <el-button size="small" @click="openEditTaskDialog">修改任务名</el-button>
                    <el-button type="primary" size="small" @click="exportCsv">导出 CSV</el-button>
                    <el-button type="danger" plain size="small" @click="deleteTask">删除任务</el-button>
                    <el-button
                      v-if="taskDetail.status === 'OPEN'"
                      type="danger"
                      size="small"
                      @click="confirmSetTaskStatus('CLOSED')"
                    >
                      关闭任务
                    </el-button>
                    <el-button
                      v-else-if="taskDetail.status === 'CLOSED'"
                      type="success"
                      size="small"
                      @click="confirmSetTaskStatus('OPEN')"
                    >
                      重新开放
                    </el-button>
                  </div>
                </div>
              </template>
              <div class="meta" style="margin-bottom: 10px">截止：{{ taskDetail.deadlineAt || '-' }}</div>
              <div style="white-space: pre-wrap">{{ taskDetail.description || '（无说明）' }}</div>
              <div style="margin-top: 14px">
                <div class="meta" style="margin-bottom: 8px">任务附件/资料</div>
                <template v-if="!isMobile">
                  <el-table :data="taskDetail.attachments || []" size="small" empty-text="暂无任务附件">
                    <el-table-column prop="fileName" label="文件名" min-width="280" />
                    <el-table-column prop="fileSize" label="大小(Byte)" width="120" />
                    <el-table-column prop="uploadedAt" label="上传时间" min-width="180" />
                    <el-table-column label="操作" width="260">
                      <template #default="{ row }: { row: TaskAttachmentVO }">
                        <el-button size="small" @click="downloadTaskAttachment(row)">下载</el-button>
                        <el-button size="small" @click="previewTaskAttachment(row)">预览</el-button>
                        <el-button size="small" type="danger" @click="deleteTaskAttachment(row)">删除</el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </template>
                <template v-else>
                  <div v-if="(taskDetail.attachments || []).length === 0" class="meta">暂无任务附件</div>
                  <el-card v-else v-for="att in taskDetail.attachments || []" :key="att.id" shadow="never" class="attCard">
                    <div class="attName">{{ att.fileName }}</div>
                    <div class="meta" style="margin-top: 6px">大小：{{ att.fileSize }} Byte</div>
                    <div class="meta">上传时间：{{ att.uploadedAt }}</div>
                    <div style="display: flex; gap: 10px; margin-top: 10px; flex-wrap: wrap">
                      <el-button size="small" @click="downloadTaskAttachment(att)">下载</el-button>
                      <el-button size="small" @click="previewTaskAttachment(att)">预览</el-button>
                      <el-button size="small" type="danger" @click="deleteTaskAttachment(att)">删除</el-button>
                    </div>
                  </el-card>
                </template>
                <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap; margin-top: 10px">
                  <input type="file" multiple @change="onPickTaskAttachmentFiles" />
                  <el-button type="primary" size="small" :loading="taskAttachmentUploading" @click="uploadTaskAttachments">追加上传</el-button>
                  <div class="meta" v-if="taskAttachmentUploadFiles.length > 0">已选择 {{ taskAttachmentUploadFiles.length }} 个文件</div>
                </div>
              </div>
            </el-card>

            <el-card class="block" shadow="never" v-if="selectedTaskId">
              <template #header>
                <div style="display: flex; justify-content: space-between; align-items: center">
                  <div>学生提交列表</div>
                  <div class="meta">点击“批阅”对提交打分</div>
                </div>
              </template>

              <template v-if="!isMobile">
                <div class="groupBar">
                  <el-input
                    v-model="submissionQ"
                    placeholder="搜索学生姓名/账号"
                    clearable
                    style="width: 280px"
                  />
                  <div class="meta">共 {{ filteredSubmissionGroups.length }} 人（{{ submissions.length }} 条提交）</div>
                </div>

                <el-table
                  :data="filteredSubmissionGroups"
                  size="small"
                  v-loading="loadingSubs"
                  row-key="key"
                  class="groupTable"
                >
                  <el-table-column type="expand" width="48">
                    <template #default="{ row }">
                      <div class="expandBox">
                        <div class="meta" style="margin-bottom: 6px">全部版本（按版本号倒序）</div>
                        <el-table :data="row.versions" size="small" border>
                          <el-table-column prop="versionNo" label="版本" width="80" />
                          <el-table-column prop="submittedAt" label="提交时间" min-width="180" />
                          <el-table-column label="操作" width="220">
                            <template #default="{ row: s }: { row: SubmissionVO }">
                              <el-button size="small" @click="openReport(s)">查看报告</el-button>
                              <el-button size="small" type="primary" @click="openReview(s)">批阅</el-button>
                            </template>
                          </el-table-column>
                        </el-table>
                      </div>
                    </template>
                  </el-table-column>

                  <el-table-column label="学生" min-width="180">
                    <template #default="{ row }">
                      <div style="font-weight: 700">{{ row.studentDisplayName }}</div>
                      <div class="meta">{{ row.studentUsername }}</div>
                    </template>
                  </el-table-column>

                  <el-table-column label="最新版本" width="120">
                    <template #default="{ row }">
                      <el-tag size="small" type="info">v{{ row.latest.versionNo }}</el-tag>
                    </template>
                  </el-table-column>

                  <el-table-column label="最近提交" min-width="180">
                    <template #default="{ row }">
                      {{ row.latest.submittedAt }}
                    </template>
                  </el-table-column>

                  <el-table-column label="版本数" width="90">
                    <template #default="{ row }">
                      <el-tag size="small" type="success">{{ row.versions.length }}</el-tag>
                    </template>
                  </el-table-column>

                  <el-table-column label="操作" width="220">
                    <template #default="{ row }">
                      <el-button size="small" @click="openReport(row.latest)">查看最新</el-button>
                      <el-button size="small" type="primary" @click="openReview(row.latest)">批阅最新</el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </template>
              <template v-else>
                <div v-if="loadingSubs" class="meta">加载中...</div>
                <div v-else-if="submissionGroups.length === 0" class="meta">暂无提交</div>

                <el-card v-else v-for="g in submissionGroups" :key="g.key" shadow="never" class="stuCard">
                  <div class="stuHead">
                    <div>
                      <div class="stuName">{{ g.studentDisplayName }}</div>
                      <div class="meta" v-if="g.studentUsername">{{ g.studentUsername }}</div>
                    </div>
                    <div style="display: flex; align-items: center; gap: 8px">
                      <el-tag size="small" type="info">最新 v{{ g.latest.versionNo }}</el-tag>
                      <el-tag size="small" type="success">{{ g.versions.length }} 版</el-tag>
                    </div>
                  </div>

                  <div class="meta" style="margin-top: 6px">最近提交：{{ g.latest.submittedAt }}</div>

                  <div class="stuActions">
                    <el-button size="small" @click="openReport(g.latest)">查看最新</el-button>
                    <el-button size="small" type="primary" @click="openReview(g.latest)">批阅最新</el-button>
                  </div>

                  <el-collapse class="stuCollapse">
                    <el-collapse-item :title="`查看全部版本（${g.versions.length}）`" :name="g.key">
                      <div v-for="s in g.versions" :key="s.id" class="verRow">
                        <div class="verLeft">
                          <div class="verTitle">v{{ s.versionNo }}</div>
                          <div class="meta">{{ s.submittedAt }}</div>
                        </div>
                        <div class="verBtns">
                          <el-button size="small" @click="openReport(s)">查看</el-button>
                          <el-button size="small" type="primary" @click="openReview(s)">批阅</el-button>
                        </div>
                      </div>
                    </el-collapse-item>
                  </el-collapse>
                </el-card>
              </template>
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
              <div v-if="scheduleError" class="scheduleEmpty" style="margin: 8px 0">{{ scheduleError }}</div>
              <div v-else-if="scheduleEmptyHint" class="scheduleEmpty" style="margin: 8px 0">当前周暂无课表，请切换周次或学期</div>

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

          <el-tab-pane label="历史签到" name="history">
            <el-card class="block" shadow="never">
              <div class="toolbar">
                <el-input-number v-model="historyFilter.grade" :min="2000" :max="2100" placeholder="年级" style="width: 140px" />
                <el-select v-model="historyFilter.classId" clearable filterable :loading="loadingHistoryClasses" placeholder="班级" style="width: 240px">
                  <el-option v-for="c in historyClassOptions" :key="c.id" :label="c.name" :value="c.id" />
                </el-select>
                <el-input v-model="historyFilter.roomKeyword" placeholder="课室关键字" style="width: 180px" />
                <el-date-picker
                  v-model="historyFilter.from"
                  type="datetime"
                  clearable
                  placeholder="开始时间"
                  format="YYYY-MM-DD HH:mm:ss"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                  style="width: 220px"
                />
                <el-date-picker
                  v-model="historyFilter.to"
                  type="datetime"
                  clearable
                  placeholder="结束时间"
                  format="YYYY-MM-DD HH:mm:ss"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                  style="width: 220px"
                />
                <el-select v-model="historyFilter.status" clearable placeholder="状态" style="width: 140px">
                  <el-option label="OPEN" value="OPEN" />
                  <el-option label="CLOSED" value="CLOSED" />
                </el-select>
                <el-button type="primary" @click="loadHistorySessions(true)">查询</el-button>
                <el-button :loading="historyLoading" @click="loadHistorySessions()">刷新</el-button>
              </div>

              <div class="meta" style="margin: 8px 0 12px">
                集中查看历史签到场次，可按班级、年级、课室和时间筛选，并重新下载签到 CSV。
              </div>

              <template v-if="!isMobile">
                <el-table :data="historyData.items" v-loading="historyLoading" stripe>
                  <el-table-column prop="courseName" label="课程" min-width="140" />
                  <el-table-column prop="classDisplayName" label="班级" min-width="160" />
                  <el-table-column label="年级" width="90">
                    <template #default="{ row }: { row: AttendanceHistoryItem }">
                      {{ row.grade ? `${row.grade}级` : '-' }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="labRoomName" label="课室" min-width="120" />
                  <el-table-column label="日期/节次" min-width="170">
                    <template #default="{ row }: { row: AttendanceHistoryItem }">
                      <div>{{ row.lessonDate || '-' }}</div>
                      <div class="meta">{{ row.slotName || '-' }}</div>
                    </template>
                  </el-table-column>
                  <el-table-column prop="startedAt" label="开始时间" min-width="180" />
                  <el-table-column prop="endedAt" label="结束时间" min-width="180" />
                  <el-table-column prop="status" label="状态" width="100" />
                  <el-table-column label="统计" width="130">
                    <template #default="{ row }: { row: AttendanceHistoryItem }">
                      {{ row.checkedInCount }}/{{ row.totalCount }}
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="200" fixed="right">
                    <template #default="{ row }: { row: AttendanceHistoryItem }">
                      <el-button size="small" @click="openHistoryDetail(row)">查看详情</el-button>
                      <el-button size="small" @click="exportHistoryAttendanceCsv(row.sessionId)">导出 CSV</el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </template>
              <template v-else>
                <div v-if="historyLoading" class="meta">加载中...</div>
                <div v-else-if="historyData.items.length === 0" class="meta">暂无历史签到记录</div>
                <el-card v-else v-for="row in historyData.items" :key="row.sessionId" shadow="never" class="historyCard">
                  <div class="stuHead">
                    <div>
                      <div class="stuName">{{ row.courseName }}</div>
                      <div class="meta">{{ row.classDisplayName }}</div>
                    </div>
                    <el-tag size="small" :type="row.status === 'OPEN' ? 'success' : 'info'">{{ row.status }}</el-tag>
                  </div>
                  <div class="meta" style="margin-top: 8px">课室：{{ row.labRoomName || '-' }}</div>
                  <div class="meta">时间：{{ row.lessonDate || '-' }} / {{ row.slotName || '-' }}</div>
                  <div class="meta">开始：{{ row.startedAt }}</div>
                  <div class="meta">统计：已到 {{ row.checkedInCount }} / 应到 {{ row.totalCount }} / 未到 {{ row.absentCount }}</div>
                  <div class="stuActions">
                    <el-button size="small" @click="openHistoryDetail(row)">查看详情</el-button>
                    <el-button size="small" @click="exportHistoryAttendanceCsv(row.sessionId)">导出 CSV</el-button>
                  </div>
                </el-card>
              </template>

              <div style="margin-top: 12px; display: flex; justify-content: flex-end">
                <el-pagination
                  background
                  layout="prev, pager, next, total"
                  :current-page="historyData.page"
                  :page-size="historyData.size"
                  :total="historyData.total"
                  @current-change="(p: number) => { historyFilter.page = p; loadHistorySessions() }"
                />
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
      <el-form-item label="截止时间（可选）">
        <el-date-picker
          v-model="createForm.deadlineAt"
          type="datetime"
          placeholder="不设置则不限时"
          style="width: 100%"
          clearable
          format="YYYY-MM-DD HH:mm:ss"
          value-format="YYYY-MM-DDTHH:mm:ss"
        />
      </el-form-item>
      <el-form-item label="发布班级（不选 = 全体学生）">
        <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap; width: 100%">
          <el-radio-group v-model="classScope" size="small">
            <el-radio-button label="mine">我的课表班级</el-radio-button>
            <el-radio-button label="all">全部班级</el-radio-button>
          </el-radio-group>
          <el-select
            v-model="createForm.classIds"
            multiple
            filterable
            clearable
            collapse-tags
            collapse-tags-tooltip
            :loading="loadingClasses"
            placeholder="选择班级（可多选）"
            style="width: 100%"
          >
            <el-option
              v-for="c in classOptions"
              :key="c.id"
              :label="c.departmentName ? `${c.departmentName} / ${c.name}` : c.name"
              :value="c.id"
            />
          </el-select>
        </div>
        <div class="meta" style="margin-top: 6px">
          默认仅显示你课表里出现的班级；切换到“全部班级”可看到全系统班级。
        </div>
      </el-form-item>
      <el-form-item label="任务附件（可选）">
        <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap; width: 100%">
          <input type="file" multiple @change="onPickCreateTaskFiles" />
          <div class="meta" v-if="createTaskFiles.length > 0">已选择 {{ createTaskFiles.length }} 个文件，创建任务后会自动上传。</div>
          <div class="meta" v-else>支持一次选择多个任务资料文件，学生端可直接下载。</div>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createDialog = false">取消</el-button>
      <el-button type="primary" :loading="creating" @click="createTask">创建</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="editTaskDialog" title="修改任务名" width="420px">
    <el-form label-position="top">
      <el-form-item label="任务名">
        <el-input v-model="editTaskForm.title" placeholder="请输入新的任务名" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="editTaskDialog = false">取消</el-button>
      <el-button type="primary" :loading="updatingTaskTitle" @click="updateTaskTitle">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="reviewDialog" title="批阅评分" width="900px">
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
    <PlagiarismSummaryPanel v-if="reviewTarget" :submission-id="reviewTarget.id" :content-md="reviewTarget.contentMd" :compact="true" />
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

      <PlagiarismSummaryPanel v-if="reportTarget" :submission-id="reportTarget.id" :content-md="reportTarget.contentMd" />

      <el-form-item label="附件列表">
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
      </el-form-item>
    </el-form>
  </el-dialog>

  <el-dialog v-model="progressDetailDialog" title="学生实验过程" width="980px" @closed="revokeProgressImageUrls">
    <div v-if="progressDetail" v-loading="progressDetailLoading">
      <div class="groupBar" style="margin-bottom: 12px">
        <div>
          <div class="stuName">{{ progressDetail.studentDisplayName }}</div>
          <div class="meta">{{ progressDetail.studentUsername }} / {{ progressDetail.classDisplayName || '-' }}</div>
          <div class="meta">
            登记状态：
            <el-tag size="small" :type="completionStatusType(progressDetail.completionStatus)">{{ completionStatusText(progressDetail.completionStatus) }}</el-tag>
          </div>
          <div v-if="progressDetail.requestedAt" class="meta">申请时间：{{ progressDetail.requestedAt }}</div>
          <div v-if="progressDetail.confirmedAt" class="meta">确认时间：{{ progressDetail.confirmedAt }}</div>
          <div v-if="progressDetail.confirmedByDisplayName" class="meta">确认教师：{{ progressDetail.confirmedByDisplayName }}</div>
        </div>
        <div>
          <el-button
            v-if="progressDetail.completionStatus === 'PENDING_CONFIRM'"
            type="primary"
            :loading="confirmingCompletion === progressDetail.studentId"
            @click="confirmCompletion({
              studentId: progressDetail.studentId,
              studentUsername: progressDetail.studentUsername,
              studentDisplayName: progressDetail.studentDisplayName,
              classDisplayName: progressDetail.classDisplayName,
              progressCount: progressDetail.logs.length,
              completionStatus: progressDetail.completionStatus,
              latestUpdatedAt: progressDetail.logs[progressDetail.logs.length - 1]?.createdAt,
              requestedAt: progressDetail.requestedAt,
              confirmedAt: progressDetail.confirmedAt,
            })"
          >
            确认登记
          </el-button>
        </div>
      </div>

      <div v-if="progressDetail.logs.length === 0" class="meta">该学生还没有实验步骤记录</div>
      <el-card v-else v-for="log in progressDetail.logs" :key="log.id" shadow="never" class="progressCard">
        <template #header>
          <div class="taskRow">
            <div class="deviceTitle">步骤 {{ log.stepNo }}</div>
            <div class="meta">{{ log.createdAt }}</div>
          </div>
        </template>
        <div style="white-space: pre-wrap">{{ log.content || '（无文字说明）' }}</div>

        <div v-if="log.attachments.length > 0" class="progressAtts" style="margin-top: 12px">
          <div v-for="att in log.attachments" :key="att.id" class="progressAtt">
            <template v-if="isImageLike(att.fileName, att.contentType) && progressImageUrls[att.id]">
              <img :src="progressImageUrls[att.id]" :alt="att.fileName" class="progressThumb" />
            </template>
            <div class="attName" style="margin-top: 6px">{{ att.fileName }}</div>
            <div class="meta">大小：{{ att.fileSize }} Byte</div>
            <div style="display: flex; gap: 8px; margin-top: 8px; flex-wrap: wrap">
              <el-button size="small" @click="previewProgressAttachment(att)">预览</el-button>
              <el-button size="small" @click="downloadProgressAttachment(att)">下载</el-button>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </el-dialog>

  <el-dialog v-model="deviceConfigDialog" title="任务设备配置" width="920px">
    <div class="meta" style="margin-bottom: 12px">从管理员设备台账选择本次实验允许申请的设备，并设置单次任务上限数量。设置为 0 表示该任务不可申请该设备。</div>
    <el-table :data="deviceConfigDraft" stripe>
      <el-table-column prop="deviceCode" label="编码" width="140" />
      <el-table-column prop="deviceName" label="设备名称" min-width="160" />
      <el-table-column prop="deviceStatus" label="台账状态" width="120" />
      <el-table-column prop="totalQuantity" label="总库存" width="100" />
      <el-table-column label="任务上限数量" width="180">
        <template #default="{ row }">
          <el-input-number v-model="row.maxQuantity" :min="0" :max="row.totalQuantity || 999" />
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="deviceConfigDialog = false">取消</el-button>
      <el-button type="primary" :loading="savingTaskDevices" @click="saveTaskDevices">保存配置</el-button>
    </template>
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

  <el-dialog v-model="classDialog" title="班级管理 / 签到" width="980px" @closed="stopLoops">
    <div v-if="selectedCell" class="meta" style="margin-bottom: 10px">
      {{ selectedCell.lessonDate }} / {{ selectedCell.slotName }} / {{ selectedCell.className }}
      <span style="margin-left: 10px">实验室：{{ selectedCell.labRoomName || '-' }}</span>
    </div>

    <el-card shadow="never" style="margin-bottom: 12px">
      <div class="toolbar">
        <el-input v-model="mobileBase" placeholder="手机端访问基址（建议填电脑局域网IP）" style="width: 360px" />
        <el-button
          v-if="lanIps.length > 0 && isLocalhostBase(mobileBase)"
          size="small"
          :loading="loadingLanIps"
          @click="useRecommendedMobileBase"
        >
          使用推荐IP
        </el-button>

        <el-select v-model="qrMode" placeholder="二维码模式" style="width: 140px">
          <el-option label="动态" value="dynamic" />
          <el-option label="静态" value="static" />
        </el-select>
        <el-input-number
          v-if="qrMode === 'dynamic'"
          v-model="qrRefreshSeconds"
          :min="1"
          :max="60"
          :step="1"
          controls-position="right"
          style="width: 160px"
        />
        <span v-if="qrMode === 'dynamic'" class="meta">秒刷新</span>

        <el-button size="small" @click="copyLink" :disabled="!checkinLink">复制签到链接</el-button>
        <el-button size="small" @click="exportAttendanceCsv" :disabled="!session">导出签到 CSV</el-button>
        <el-button size="small" type="primary" @click="startSession" :disabled="!!session">开启签到</el-button>
        <el-button size="small" type="danger" @click="closeSession" :disabled="!session">结束签到</el-button>
        <el-button size="small" @click="refreshQr" :disabled="!session">刷新二维码</el-button>
        <el-button size="small" @click="refreshRecords" :disabled="!session">刷新名单</el-button>
      </div>

      <div v-if="isLocalhostBase(mobileBase)" class="meta" style="margin-top: 6px; color: #b42318">
        当前基址是 localhost，手机扫码会访问到手机自己的 localhost，必定打不开。建议改成电脑局域网 IP。
      </div>
      <div v-else-if="qrMode === 'dynamic' && tokenInfo?.ttlSeconds" class="meta" style="margin-top: 6px">
        提示：动态二维码后端 TTL 会自动按“刷新秒数 + 1（最大 60）”设置；需要更久请切换“静态”。当前后端 TTL = {{ tokenInfo.ttlSeconds }} 秒。
      </div>

      <div v-if="session" class="meta" style="margin-top: 6px">
        场次ID：{{ session.id }}，状态：{{ session.status }}，开始：{{ session.startedAt }}
      </div>

        <div v-if="session" style="display: flex; gap: 16px; align-items: flex-start; margin-top: 12px; flex-wrap: wrap">
          <div>
            <div class="meta" style="margin-bottom: 6px">
              <span v-if="qrMode === 'dynamic'">动态二维码（每 {{ qrRefreshSeconds }} 秒刷新）</span>
              <span v-else>静态二维码（不自动刷新，可点“刷新二维码”）</span>
            </div>
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

  <el-dialog v-model="historyDetailDialog" title="签到详情" width="980px">
    <div v-if="historyDetail" v-loading="historyDetailLoading">
      <div class="groupBar" style="margin-bottom: 12px">
        <div>
          <div class="stuName" style="font-size: 18px">{{ historyDetail.courseName }}</div>
          <div class="meta">{{ historyDetail.classDisplayName }} / {{ historyDetail.lessonDate || '-' }} / {{ historyDetail.slotName || '-' }}</div>
          <div class="meta">课室：{{ historyDetail.labRoomName || '-' }} / 状态：{{ historyDetail.status }}</div>
        </div>
        <div class="meta">
          应到 {{ historyDetail.totalCount }} / 已到 {{ historyDetail.checkedInCount }} / 未到 {{ historyDetail.absentCount }}
        </div>
      </div>

      <template v-if="!isMobile">
        <el-table :data="historyDetail.roster" stripe>
          <el-table-column prop="studentUsername" label="学号/账号" width="160" />
          <el-table-column prop="studentDisplayName" label="姓名" width="140" />
          <el-table-column prop="status" label="状态" width="140" />
          <el-table-column prop="method" label="方式" width="120" />
          <el-table-column prop="checkedInAt" label="签到时间" min-width="180" />
        </el-table>
      </template>
      <template v-else>
        <el-card v-for="item in historyDetail.roster" :key="item.studentId" shadow="never" class="historyCard">
          <div class="stuHead">
            <div>
              <div class="stuName" style="font-size: 16px">{{ item.studentDisplayName }}</div>
              <div class="meta">{{ item.studentUsername }}</div>
            </div>
            <el-tag size="small" :type="item.status === 'CHECKED_IN' ? 'success' : 'info'">{{ item.status }}</el-tag>
          </div>
          <div class="meta" style="margin-top: 8px">方式：{{ item.method || '-' }}</div>
          <div class="meta">签到时间：{{ item.checkedInAt || '-' }}</div>
        </el-card>
      </template>
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
.tabs :deep(.el-tabs__content) {
  padding-top: 8px;
}
.toolbar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}
.groupBar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}
.groupTable :deep(.el-table__expanded-cell) {
  background: #fafafa;
}
.expandBox {
  padding: 6px 6px 10px;
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
.subCard {
  margin-bottom: 10px;
}
.taskRow {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}
.historyCard {
  margin-bottom: 10px;
}
.subTop {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}
.stuCard {
  margin-bottom: 10px;
}
.stuHead {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}
.stuName {
  font-weight: 800;
  font-size: 16px;
  word-break: break-word;
}
.stuActions {
  display: flex;
  gap: 10px;
  margin-top: 10px;
  flex-wrap: wrap;
}
.stuCollapse {
  margin-top: 10px;
}
.verRow {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-top: 1px solid #eee;
}
.verRow:first-child {
  border-top: none;
}
.verLeft {
  min-width: 0;
}
.verTitle {
  font-weight: 700;
}
.verBtns {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.attCard {
  margin-bottom: 10px;
}
.attName {
  font-weight: 700;
  word-break: break-word;
}
.tableActions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.deviceGrid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px;
}
.deviceBox {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 12px;
  background: #fafcff;
}
.deviceTitle {
  font-weight: 700;
}
.progressCard {
  margin-bottom: 10px;
}
.progressAtts {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px;
}
.progressAtt {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 10px;
  background: #fafcff;
}
.progressThumb {
  display: block;
  width: 100%;
  max-height: 220px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #ebeef5;
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
