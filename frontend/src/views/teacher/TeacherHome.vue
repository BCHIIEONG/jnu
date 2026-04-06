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
import LabRoomManager from '../common/LabRoomManager.vue'
import StatisticsPanel from './components/StatisticsPanel.vue'
import TaskDiscussionPanel from '../common/TaskDiscussionPanel.vue'
import TeachingAnalyticsPanel from '../common/TeachingAnalyticsPanel.vue'

type TaskVO = {
  id: number
  title: string
  description?: string
  publisherName?: string
  experimentCourseId?: number | null
  experimentCourseTitle?: string | null
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

type ReviewVO = {
  id: number
  submissionId: number
  teacherDisplayName?: string
  score: number
  comment?: string | null
  reviewedAt: string
  issueTags?: string[]
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
  sourceType?: 'CLASS_SCHEDULE' | 'EXPERIMENT_COURSE'
  experimentCourseId?: number | null
  experimentCourseSlotId?: number | null
  experimentCourseInstanceId?: number | null
  semesterId: number
  classId?: number | null
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
  sourceType?: 'CLASS_SCHEDULE' | 'EXPERIMENT_COURSE'
  scheduleId?: number | null
  semesterId: number
  classId?: number | null
  teacherId: number
  experimentCourseId?: number | null
  experimentCourseSlotId?: number | null
  experimentCourseInstanceId?: number | null
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
type MetaOptionVO = { id: number; name: string; description?: string | null }
type SemesterMetaOptionVO = { id: number; name: string; startDate: string; endDate: string }
type SemesterManageResultVO = {
  id: number
  name: string
  startDate?: string | null
  endDate?: string | null
  createdAt?: string
  updatedAt?: string
  affectedCourseCount: number
  affectedInstanceCount: number
  hasOutOfRangeCourses: boolean
  warningMessage?: string | null
}
type ExperimentCourseSlotInstanceVO = {
  id: number
  slotGroupId: number
  lessonDate: string
  teachingWeek: number
  displayName: string
  slotId: number
  slotCode?: string | null
  slotName: string
  slotStartTime?: string | null
  slotEndTime?: string | null
  labRoomId: number
  labRoomName?: string | null
  capacity: number
}
type ExperimentCourseSlotVO = {
  id: number
  courseId: number
  name?: string | null
  mode: 'SINGLE' | 'RECURRING'
  firstLessonDate: string
  repeatPattern?: 'EVERY_WEEK' | 'ODD_WEEK' | null
  rangeMode?: 'SEMESTER' | 'DATE_RANGE' | null
  rangeStartDate?: string | null
  rangeEndDate?: string | null
  slotId: number
  slotCode?: string | null
  slotName: string
  slotStartTime?: string | null
  slotEndTime?: string | null
  labRoomId: number
  labRoomName?: string | null
  capacity: number
  enrolledCount: number
  remainingCapacity: number
  instances: ExperimentCourseSlotInstanceVO[]
}
type ExperimentCourseVO = {
  id: number
  title: string
  description?: string | null
  teacherId: number
  teacherDisplayName?: string | null
  semesterId: number
  semesterName?: string | null
  status: 'OPEN' | 'CLOSED'
  enrollDeadlineAt: string
  targetClassIds: number[]
  targetStudentIds: number[]
  slots: ExperimentCourseSlotVO[]
  totalEnrolled?: number | null
  createdAt?: string
  updatedAt?: string
}
type ExperimentCourseEnrollmentRowVO = {
  slotId: number
  lessonDate: string
  slotName?: string | null
  slotStartTime?: string | null
  slotEndTime?: string | null
  labRoomName?: string | null
  capacity?: number | null
  enrolledCount?: number | null
  studentId: number
  studentUsername: string
  studentDisplayName: string
  classDisplayName?: string | null
  joinSource?: 'STUDENT_SELF' | 'TEACHER_MANUAL' | null
  selectedAt: string
}
type ExperimentCourseBlockedStudentVO = {
  studentId: number
  studentUsername: string
  studentDisplayName: string
  classDisplayName?: string | null
  blockedAt: string
}
type ExperimentCourseRosterVO = {
  enrollments: ExperimentCourseEnrollmentRowVO[]
  blockedStudents: ExperimentCourseBlockedStudentVO[]
}
type ExperimentCourseStudentOptionVO = {
  id: number
  username: string
  displayName: string
  classDisplayName?: string | null
}
type ExperimentCourseMetaVO = {
  semesters: SemesterMetaOptionVO[]
  timeSlots: MetaOptionVO[]
  labRooms: MetaOptionVO[]
}
type ExperimentCourseFormSlot = {
  id?: number
  name: string
  mode: 'SINGLE' | 'RECURRING'
  firstLessonDate: string
  slotId: number | null
  labRoomId: number | null
  capacity: number
  repeatPattern: 'EVERY_WEEK' | 'ODD_WEEK'
  rangeMode: 'SEMESTER' | 'DATE_RANGE'
  rangeStartDate: string
  rangeEndDate: string
  configured: boolean
  previewExpanded: boolean
}
type PageResult<T> = { page: number; size: number; total: number; items: T[] }
type AttendanceHistoryItem = {
  sessionId: number
  sourceType?: 'CLASS_SCHEDULE' | 'EXPERIMENT_COURSE'
  courseName: string
  classId?: number | null
  classDisplayName: string
  grade?: number | null
  experimentCourseId?: number | null
  experimentCourseSlotId?: number | null
  experimentCourseInstanceId?: number | null
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
  completionSource?: 'STUDENT_REQUEST' | 'TEACHER_DIRECT' | null
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
  completionSource?: 'STUDENT_REQUEST' | 'TEACHER_DIRECT' | null
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
  completionSource?: 'STUDENT_REQUEST' | 'TEACHER_DIRECT' | null
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
type TeacherDiscussionAggregateItemVO = {
  threadId: number
  taskId: number
  taskTitle: string
  studentId: number
  studentUsername: string
  studentDisplayName: string
  latestMessagePreview?: string | null
  latestTeacherReplyPreview?: string | null
  latestMessageAt?: string | null
  unreadCount: number
}
type UnreadCountVO = {
  unreadCount: number
}

const auth = useAuthStore()
const ui = useUiStore()
const router = useRouter()

const isMobile = computed(() => ui.effectiveMode === 'mobile')

const activeTab = ref<'course' | 'labroom' | 'discussion' | 'flow' | 'report' | 'schedule' | 'history' | 'stats' | 'analytics'>('course')
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
  experimentCourseId: null as number | null,
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
const experimentCourseClassOptions = ref<TeacherClassVO[]>([])
const loadingExperimentCourseClasses = ref(false)
const experimentCourses = ref<ExperimentCourseVO[]>([])
const loadingExperimentCourses = ref(false)
const selectedExperimentCourseId = ref<number | null>(null)
const experimentCourseDetail = computed(() => experimentCourses.value.find((item) => item.id === selectedExperimentCourseId.value) ?? null)
const experimentCourseDialog = ref(false)
const editingExperimentCourseId = ref<number | null>(null)
const savingExperimentCourse = ref(false)
const experimentCourseMeta = ref<ExperimentCourseMetaVO>({ semesters: [], timeSlots: [], labRooms: [] })
const semesterManageDialog = ref(false)
const semesterManageSaving = ref(false)
const semesterManageEditingId = ref<number | null>(null)
const semesterManageForm = reactive({
  name: '',
  startDate: '',
  endDate: '',
})
const experimentCourseStudentOptions = ref<ExperimentCourseStudentOptionVO[]>([])
const loadingExperimentCourseStudents = ref(false)
const loadingExperimentCourseMeta = ref(false)
const togglingExperimentCourseId = ref<number | null>(null)
const loadingCourseEnrollments = ref(false)
const experimentCourseEnrollments = ref<ExperimentCourseEnrollmentRowVO[]>([])
const experimentCourseBlockedStudents = ref<ExperimentCourseBlockedStudentVO[]>([])
const manualRosterStudentId = ref<number | null>(null)
const manualRosterSlotId = ref<number | null>(null)
const manualRosterStudentKeyword = ref('')
const enrollingRosterStudent = ref(false)
const removingRosterStudentId = ref<number | null>(null)
const unblockingRosterStudentId = ref<number | null>(null)
const experimentCourseDetailExpanded = reactive<Record<number, boolean>>({})
const experimentCourseForm = reactive({
  title: '',
  description: '',
  semesterId: null as number | null,
  enrollDeadlineAt: null as string | null,
  targetClassIds: [] as number[],
  targetStudentIds: [] as number[],
  slots: [] as ExperimentCourseFormSlot[],
})

const reviewDialog = ref(false)
const reviewTarget = ref<SubmissionVO | null>(null)
const reviewIssueOptions = [
  { code: 'FORMAT', label: '格式不规范' },
  { code: 'STEPS', label: '实验步骤不完整' },
  { code: 'ANALYSIS', label: '结果分析不足' },
  { code: 'CONCLUSION', label: '结论不清晰' },
  { code: 'DATA', label: '数据异常' },
  { code: 'CHART', label: '图表缺失或错误' },
  { code: 'CODE', label: '代码/原理错误' },
  { code: 'PLAGIARISM', label: '抄袭疑似' },
  { code: 'OTHER', label: '其他' },
]
const reviewForm = reactive({
  score: 95,
  comment: '完成很好',
  issueTags: [] as string[],
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
const teacherDiscussionItems = ref<TeacherDiscussionAggregateItemVO[]>([])
const teacherDiscussionLoading = ref(false)
const teacherDiscussionQ = ref('')
const teacherDiscussionUnreadOnly = ref(false)
const teacherDiscussionUnreadCount = ref(0)
const highlightedDiscussionThreadId = ref<number | null>(null)
let teacherDiscussionUnreadTimer: number | null = null
let teacherDiscussionListTimer: number | null = null

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

function completionSourceText(source?: TeacherTaskProgressStudentVO['completionSource'] | TaskCompletionVO['completionSource']) {
  if (source === 'TEACHER_DIRECT') return '教师直接登记完成'
  if (source === 'STUDENT_REQUEST') return '学生申请完成'
  return '未登记'
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
  if (activeTab.value === 'discussion') {
    return
  }
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

async function loadTeacherDiscussionUnreadSummary() {
  try {
    const summary = await apiData<UnreadCountVO>('/api/teacher/discussions/unread-summary', { method: 'GET' }, auth.token)
    teacherDiscussionUnreadCount.value = summary.unreadCount || 0
  } catch {
    teacherDiscussionUnreadCount.value = 0
  }
}

async function loadTeacherDiscussions() {
  teacherDiscussionLoading.value = true
  try {
    const params = new URLSearchParams()
    const keyword = teacherDiscussionQ.value.trim()
    if (keyword) params.set('q', keyword)
    if (teacherDiscussionUnreadOnly.value) params.set('unreadOnly', 'true')
    const query = params.toString() ? `?${params.toString()}` : ''
    teacherDiscussionItems.value = await apiData<TeacherDiscussionAggregateItemVO[]>(
      `/api/teacher/discussions${query}`,
      { method: 'GET' },
      auth.token,
    )
  } catch (e: any) {
    teacherDiscussionItems.value = []
    ElMessage.error(e?.message ?? '加载讨论聚合失败')
  } finally {
    teacherDiscussionLoading.value = false
  }
}

async function openTeacherDiscussionItem(item: TeacherDiscussionAggregateItemVO) {
  activeTab.value = 'discussion'
  highlightedDiscussionThreadId.value = item.threadId
  if (selectedTaskId.value !== item.taskId) {
    await selectTask(item.taskId)
  }
  await handleDiscussionChanged()
}

async function handleDiscussionChanged() {
  await Promise.all([loadTeacherDiscussionUnreadSummary(), loadTeacherDiscussions()])
}

function startTeacherDiscussionPolling() {
  stopTeacherDiscussionPolling()
  teacherDiscussionUnreadTimer = window.setInterval(() => {
    loadTeacherDiscussionUnreadSummary()
  }, 16000)
  teacherDiscussionListTimer = window.setInterval(() => {
    if (activeTab.value === 'discussion') {
      loadTeacherDiscussions()
    }
  }, 16000)
}

function stopTeacherDiscussionPolling() {
  if (teacherDiscussionUnreadTimer) window.clearInterval(teacherDiscussionUnreadTimer)
  if (teacherDiscussionListTimer) window.clearInterval(teacherDiscussionListTimer)
  teacherDiscussionUnreadTimer = null
  teacherDiscussionListTimer = null
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

async function directConfirmCompletion(row: TeacherTaskProgressStudentVO) {
  if (!selectedTaskId.value) return
  try {
    await ElMessageBox.confirm(
      `确认直接将 ${row.studentDisplayName}（${row.studentUsername}）登记为已完成？此操作不需要学生先提交完成登记。`,
      '直接登记完成',
      { type: 'warning', confirmButtonText: '直接登记', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  confirmingCompletion.value = row.studentId
  try {
    await apiData(
      `/api/teacher/tasks/${selectedTaskId.value}/completion/${row.studentId}/direct-confirm`,
      { method: 'POST' },
      auth.token,
    )
    ElMessage.success('已直接登记完成')
    await loadProgressStudents()
    if (progressDetail.value?.studentId === row.studentId) {
      await openProgressDetail(row)
    }
  } catch (e: any) {
    ElMessage.error(e?.message ?? '直接登记失败')
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

async function exportDeviceRequestsExcel() {
  if (!selectedTaskId.value) return
  try {
    await downloadBlob(`/api/teacher/tasks/${selectedTaskId.value}/device-requests/export/excel`, {
      token: auth.token,
      fallbackFilename: `task-${selectedTaskId.value}-device-requests.xlsx`,
    })
    ElMessage.success('已开始下载借用记录 Excel')
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
          experimentCourseId: createForm.experimentCourseId,
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
    createForm.experimentCourseId = null
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

async function loadExperimentCourseClassOptions() {
  loadingExperimentCourseClasses.value = true
  try {
    experimentCourseClassOptions.value = await apiData<TeacherClassVO[]>(
      '/api/teacher/classes?scope=all',
      { method: 'GET' },
      auth.token,
    )
  } catch (e: any) {
    experimentCourseClassOptions.value = []
    ElMessage.error(e?.message ?? '加载实验课程班级失败')
  } finally {
    loadingExperimentCourseClasses.value = false
  }
}

async function loadExperimentCourseMeta() {
  loadingExperimentCourseMeta.value = true
  try {
    experimentCourseMeta.value = await apiData<ExperimentCourseMetaVO>(
      '/api/teacher/experiment-courses/meta',
      { method: 'GET' },
      auth.token,
    )
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载实验课程元数据失败')
  } finally {
    loadingExperimentCourseMeta.value = false
  }
}

function resetSemesterManageForm() {
  semesterManageEditingId.value = null
  semesterManageForm.name = ''
  semesterManageForm.startDate = ''
  semesterManageForm.endDate = ''
}

function showSemesterImpactWarning(result: SemesterManageResultVO) {
  if (!result.hasOutOfRangeCourses) return
  ElMessageBox.alert(
    result.warningMessage
    ?? `已有 ${result.affectedCourseCount} 门实验课程、${result.affectedInstanceCount} 个课次超出新学期范围，系统不会自动调整，请后续手动修正课程日期或课次设置。`,
    '学期已更新',
    { type: 'warning', confirmButtonText: '知道了' },
  )
}

function openCreateSemesterDialog() {
  resetSemesterManageForm()
  semesterManageDialog.value = true
}

function openEditSemesterDialog() {
  const semester = selectedExperimentSemester()
  if (!semester) {
    ElMessage.warning('请先选择要编辑的学期')
    return
  }
  semesterManageEditingId.value = semester.id
  semesterManageForm.name = semester.name
  semesterManageForm.startDate = semester.startDate || ''
  semesterManageForm.endDate = semester.endDate || ''
  semesterManageDialog.value = true
}

async function submitSemesterDialog() {
  if (!semesterManageForm.name.trim()) {
    ElMessage.warning('请输入学期名称')
    return
  }
  if (!semesterManageForm.startDate) {
    ElMessage.warning('请选择学期开始日期')
    return
  }
  if (!semesterManageForm.endDate) {
    ElMessage.warning('请选择学期结束日期')
    return
  }
  semesterManageSaving.value = true
  try {
    const result = await apiData<SemesterManageResultVO>(
      semesterManageEditingId.value
        ? `/api/teacher/semesters/${semesterManageEditingId.value}`
        : '/api/teacher/semesters',
      {
        method: semesterManageEditingId.value ? 'PUT' : 'POST',
        body: {
          name: semesterManageForm.name.trim(),
          startDate: semesterManageForm.startDate,
          endDate: semesterManageForm.endDate,
        },
      },
      auth.token,
    )
    await loadExperimentCourseMeta()
    experimentCourseForm.semesterId = result.id
    semesterManageDialog.value = false
    ElMessage.success(semesterManageEditingId.value ? '学期已更新' : '学期已创建')
    showSemesterImpactWarning(result)
  } catch (e: any) {
    ElMessage.error(e?.message ?? '保存学期失败')
  } finally {
    semesterManageSaving.value = false
  }
}

async function searchExperimentCourseStudents(query = '') {
  loadingExperimentCourseStudents.value = true
  try {
    experimentCourseStudentOptions.value = await apiData<ExperimentCourseStudentOptionVO[]>(
      `/api/teacher/experiment-courses/student-options?q=${encodeURIComponent(query)}`,
      { method: 'GET' },
      auth.token,
    )
  } catch (e: any) {
    experimentCourseStudentOptions.value = []
    ElMessage.error(e?.message ?? '加载学生选项失败')
  } finally {
    loadingExperimentCourseStudents.value = false
  }
}

function searchManualRosterStudents(query = '') {
  manualRosterStudentKeyword.value = query
  return searchExperimentCourseStudents(query)
}

function createExperimentCourseSlot(): ExperimentCourseFormSlot {
  return {
    name: '',
    mode: 'SINGLE',
    firstLessonDate: '',
    slotId: null,
    labRoomId: null,
    capacity: 1,
    repeatPattern: 'EVERY_WEEK',
    rangeMode: 'SEMESTER',
    rangeStartDate: '',
    rangeEndDate: '',
    configured: false,
    previewExpanded: false,
  }
}

function selectedExperimentSemester() {
  return experimentCourseMeta.value.semesters.find((item) => item.id === experimentCourseForm.semesterId) ?? null
}

function parseLocalDate(value: string) {
  const parsed = new Date(`${value}T12:00:00`)
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

function toDateString(date: Date) {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

function mondayOf(date: Date) {
  const copy = new Date(date)
  const day = copy.getDay()
  const offset = (day + 6) % 7
  copy.setDate(copy.getDate() - offset)
  return copy
}

function computeTeachingWeek(dateText: string, semester: SemesterMetaOptionVO | null) {
  if (!semester) return 1
  const lesson = parseLocalDate(dateText)
  const semesterStart = parseLocalDate(semester.startDate)
  if (!lesson || !semesterStart) return 1
  const diff = mondayOf(lesson).getTime() - mondayOf(semesterStart).getTime()
  return Math.floor(diff / (7 * 24 * 60 * 60 * 1000)) + 1
}

function repeatPatternStepDays(repeatPattern: ExperimentCourseFormSlot['repeatPattern']) {
  return repeatPattern === 'ODD_WEEK' ? 14 : 7
}

function defaultSlotBaseName(slot: Pick<ExperimentCourseFormSlot, 'name'>, index: number) {
  const name = slot.name.trim()
  return name || `场次${index + 1}`
}

function buildExperimentCoursePreview(slot: ExperimentCourseFormSlot, index: number) {
  if (!slot.firstLessonDate || !slot.slotId || !slot.labRoomId || !slot.capacity) return []
  const semester = selectedExperimentSemester()
  const first = parseLocalDate(slot.firstLessonDate)
  if (!semester || !first) return []
  const baseName = defaultSlotBaseName(slot, index)
  if (first < parseLocalDate(semester.startDate)! || first > parseLocalDate(semester.endDate)!) return []
  if (slot.mode === 'SINGLE') {
    const week = computeTeachingWeek(slot.firstLessonDate, semester)
    return [{
      lessonDate: slot.firstLessonDate,
      teachingWeek: week,
      displayName: `${baseName} 第1周`,
    }]
  }
  const rangeStartText = slot.rangeMode === 'SEMESTER' ? semester.startDate : slot.rangeStartDate
  const rangeEndText = slot.rangeMode === 'SEMESTER' ? semester.endDate : slot.rangeEndDate
  const rangeStart = parseLocalDate(rangeStartText)
  const rangeEnd = parseLocalDate(rangeEndText)
  if (!rangeStart || !rangeEnd) return []
  if (rangeStart < parseLocalDate(semester.startDate)! || rangeEnd > parseLocalDate(semester.endDate)!) return []
  if (first < rangeStart || first > rangeEnd) return []
  const items: { lessonDate: string; teachingWeek: number; displayName: string }[] = []
  const stepDays = repeatPatternStepDays(slot.repeatPattern)
  for (let current = new Date(first); current <= rangeEnd; current.setDate(current.getDate() + stepDays)) {
    if (current < rangeStart) continue
    const lessonDate = toDateString(current)
    const teachingWeek = computeTeachingWeek(lessonDate, semester)
    items.push({
      lessonDate,
      teachingWeek,
      displayName: `${baseName} 第${items.length + 1}周`,
    })
  }
  return items
}

function getExperimentCourseSlotIssue(slot: ExperimentCourseFormSlot, index: number) {
  if (!slot.firstLessonDate || !slot.slotId || !slot.labRoomId || !slot.capacity || slot.capacity < 1) {
    return '请完整填写场次日期、节次、地点和人数'
  }
  const semester = selectedExperimentSemester()
  if (!semester) return '请选择有效学期'
  const semesterStart = parseLocalDate(semester.startDate)
  const semesterEnd = parseLocalDate(semester.endDate)
  const first = parseLocalDate(slot.firstLessonDate)
  if (!semesterStart || !semesterEnd || !first) return '学期或首次上课日期格式错误'
  if (first < semesterStart || first > semesterEnd) return '首次上课日期不在所选学期内'
  if (slot.mode === 'SINGLE') return null
  const rangeStartText = slot.rangeMode === 'SEMESTER' ? semester.startDate : slot.rangeStartDate
  const rangeEndText = slot.rangeMode === 'SEMESTER' ? semester.endDate : slot.rangeEndDate
  const rangeStart = parseLocalDate(rangeStartText)
  const rangeEnd = parseLocalDate(rangeEndText)
  if (!rangeStart || !rangeEnd) return '请完整填写多次课的日期范围'
  if (rangeStart > rangeEnd) return '日期区间开始时间不能晚于结束时间'
  if (rangeStart < semesterStart || rangeEnd > semesterEnd) return '多次课的日期范围必须位于所选学期内'
  if (first < rangeStart || first > rangeEnd) return '首次上课日期必须位于多次课生效范围内'
  if (buildExperimentCoursePreview(slot, index).length === 0) return '当前场次规则没有生成可用课次，请检查日期范围'
  return null
}

function formatWeekday(dateText: string) {
  const parsed = parseLocalDate(dateText)
  return parsed ? ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][parsed.getDay()] : '-'
}

function previewSlotSummary(slot: ExperimentCourseFormSlot, index: number) {
  const baseName = defaultSlotBaseName(slot, index)
  if (slot.mode === 'SINGLE') return `${baseName} / 单次课`
  const repeatLabel = slot.repeatPattern === 'ODD_WEEK' ? '单周' : '每周'
  const rangeLabel = slot.rangeMode === 'SEMESTER' ? '整学期' : '自定义区间'
  return `${baseName} / 多次课 / ${repeatLabel} / ${rangeLabel}`
}

function finalizeExperimentCourseSlot(slot: ExperimentCourseFormSlot, index: number) {
  const issue = getExperimentCourseSlotIssue(slot, index)
  if (issue) {
    ElMessage.warning(issue)
    return
  }
  slot.configured = true
  slot.previewExpanded = true
}

function resetExperimentCourseForm() {
  editingExperimentCourseId.value = null
  experimentCourseForm.title = ''
  experimentCourseForm.description = ''
  experimentCourseForm.semesterId = experimentCourseMeta.value.semesters[0]?.id ?? null
  experimentCourseForm.enrollDeadlineAt = null
  experimentCourseForm.targetClassIds = []
  experimentCourseForm.targetStudentIds = []
  experimentCourseForm.slots = [createExperimentCourseSlot()]
}

function fillExperimentCourseForm(course: ExperimentCourseVO) {
  editingExperimentCourseId.value = course.id
  experimentCourseForm.title = course.title
  experimentCourseForm.description = course.description ?? ''
  experimentCourseForm.semesterId = course.semesterId
  experimentCourseForm.enrollDeadlineAt = course.enrollDeadlineAt
  experimentCourseForm.targetClassIds = [...(course.targetClassIds || [])]
  experimentCourseForm.targetStudentIds = [...(course.targetStudentIds || [])]
  experimentCourseForm.slots = (course.slots || []).map((slot) => ({
    id: slot.id,
    name: slot.name ?? '',
    mode: slot.mode ?? 'SINGLE',
    firstLessonDate: slot.firstLessonDate,
    slotId: slot.slotId,
    labRoomId: slot.labRoomId,
    capacity: slot.capacity,
    repeatPattern: slot.repeatPattern ?? 'EVERY_WEEK',
    rangeMode: slot.rangeMode ?? 'SEMESTER',
    rangeStartDate: slot.rangeStartDate ?? slot.firstLessonDate,
    rangeEndDate: slot.rangeEndDate ?? slot.firstLessonDate,
    configured: true,
    previewExpanded: false,
  }))
}

async function openCreateExperimentCourseDialog() {
  if (!experimentCourseMeta.value.semesters.length) await loadExperimentCourseMeta()
  if (!experimentCourseClassOptions.value.length) await loadExperimentCourseClassOptions()
  await searchExperimentCourseStudents()
  resetExperimentCourseForm()
  experimentCourseDialog.value = true
}

async function openEditExperimentCourseDialog(course: ExperimentCourseVO) {
  if (!experimentCourseMeta.value.semesters.length) await loadExperimentCourseMeta()
  if (!experimentCourseClassOptions.value.length) await loadExperimentCourseClassOptions()
  await searchExperimentCourseStudents()
  fillExperimentCourseForm(course)
  experimentCourseDialog.value = true
}

function addExperimentCourseSlot() {
  experimentCourseForm.slots.push(createExperimentCourseSlot())
}

function removeExperimentCourseSlot(index: number) {
  if (experimentCourseForm.slots.length <= 1) {
    ElMessage.warning('至少保留一个场次')
    return
  }
  experimentCourseForm.slots.splice(index, 1)
}

async function loadExperimentCourses(selectId?: number | null) {
  loadingExperimentCourses.value = true
  try {
    experimentCourses.value = await apiData<ExperimentCourseVO[]>(
      '/api/teacher/experiment-courses',
      { method: 'GET' },
      auth.token,
    )
    const preferredId = selectId ?? selectedExperimentCourseId.value
    if (preferredId && experimentCourses.value.find((item) => item.id === preferredId)) {
      selectedExperimentCourseId.value = preferredId
    } else {
      selectedExperimentCourseId.value = experimentCourses.value[0]?.id ?? null
    }
    await loadExperimentCourseEnrollments()
  } catch (e: any) {
    experimentCourses.value = []
    selectedExperimentCourseId.value = null
    experimentCourseEnrollments.value = []
    experimentCourseBlockedStudents.value = []
    ElMessage.error(e?.message ?? '加载实验课程失败')
  } finally {
    loadingExperimentCourses.value = false
  }
}

async function loadExperimentCourseEnrollments() {
  if (!selectedExperimentCourseId.value) {
    experimentCourseEnrollments.value = []
    experimentCourseBlockedStudents.value = []
    manualRosterStudentId.value = null
    manualRosterSlotId.value = null
    return
  }
  loadingCourseEnrollments.value = true
  try {
    const roster = await apiData<ExperimentCourseRosterVO>(
      `/api/teacher/experiment-courses/${selectedExperimentCourseId.value}/roster`,
      { method: 'GET' },
      auth.token,
    )
    experimentCourseEnrollments.value = roster.enrollments || []
    experimentCourseBlockedStudents.value = roster.blockedStudents || []
    if (!manualRosterSlotId.value || !experimentCourseDetail.value?.slots.find((slot) => slot.id === manualRosterSlotId.value)) {
      manualRosterSlotId.value = experimentCourseDetail.value?.slots[0]?.id ?? null
    }
  } catch (e: any) {
    experimentCourseEnrollments.value = []
    experimentCourseBlockedStudents.value = []
    ElMessage.error(e?.message ?? '加载选课名单失败')
  } finally {
    loadingCourseEnrollments.value = false
  }
}

async function enrollRosterStudent() {
  if (!selectedExperimentCourseId.value) return
  if (!manualRosterStudentId.value) {
    ElMessage.warning('请先选择学生')
    return
  }
  if (!manualRosterSlotId.value) {
    ElMessage.warning('请先选择目标场次')
    return
  }
  enrollingRosterStudent.value = true
  try {
    const roster = await apiData<ExperimentCourseRosterVO>(
      `/api/teacher/experiment-courses/${selectedExperimentCourseId.value}/roster/enroll`,
      {
        method: 'POST',
        body: {
          studentId: manualRosterStudentId.value,
          slotId: manualRosterSlotId.value,
        },
      },
      auth.token,
    )
    experimentCourseEnrollments.value = roster.enrollments || []
    experimentCourseBlockedStudents.value = roster.blockedStudents || []
    manualRosterStudentId.value = null
    manualRosterStudentKeyword.value = ''
    await Promise.all([loadExperimentCourses(selectedExperimentCourseId.value), loadTasks(), loadWeek()])
    ElMessage.success('学生已加入对应场次')
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加入学生失败')
  } finally {
    enrollingRosterStudent.value = false
  }
}

async function removeRosterStudent(row: ExperimentCourseEnrollmentRowVO) {
  if (!selectedExperimentCourseId.value) return
  try {
    await ElMessageBox.confirm(
      `确认将 ${row.studentDisplayName}（${row.studentUsername}）移出当前实验课程，并加入禁选名单吗？`,
      '移出学生',
      { type: 'warning', confirmButtonText: '移出', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  removingRosterStudentId.value = row.studentId
  try {
    const roster = await apiData<ExperimentCourseRosterVO>(
      `/api/teacher/experiment-courses/${selectedExperimentCourseId.value}/roster/remove`,
      {
        method: 'POST',
        body: { studentId: row.studentId },
      },
      auth.token,
    )
    experimentCourseEnrollments.value = roster.enrollments || []
    experimentCourseBlockedStudents.value = roster.blockedStudents || []
    await Promise.all([loadExperimentCourses(selectedExperimentCourseId.value), loadTasks(), loadWeek()])
    ElMessage.success('学生已移出课程并加入禁选名单')
  } catch (e: any) {
    ElMessage.error(e?.message ?? '移出学生失败')
  } finally {
    removingRosterStudentId.value = null
  }
}

async function unblockRosterStudent(row: ExperimentCourseBlockedStudentVO) {
  if (!selectedExperimentCourseId.value) return
  unblockingRosterStudentId.value = row.studentId
  try {
    const roster = await apiData<ExperimentCourseRosterVO>(
      `/api/teacher/experiment-courses/${selectedExperimentCourseId.value}/blocked-students/${row.studentId}`,
      { method: 'DELETE' },
      auth.token,
    )
    experimentCourseEnrollments.value = roster.enrollments || []
    experimentCourseBlockedStudents.value = roster.blockedStudents || []
    ElMessage.success('已解除禁选，学生可重新自助选课')
  } catch (e: any) {
    ElMessage.error(e?.message ?? '解除禁选失败')
  } finally {
    unblockingRosterStudentId.value = null
  }
}

const availableRosterStudentOptions = computed(() => {
  const activeIds = new Set(experimentCourseEnrollments.value.map((row) => row.studentId))
  const keyword = manualRosterStudentKeyword.value.trim().toLowerCase()
  return experimentCourseStudentOptions.value.filter((student) => {
    if (activeIds.has(student.id)) return false
    if (!keyword) return true
    const text = `${student.displayName ?? ''} ${student.username ?? ''} ${student.classDisplayName ?? ''}`.toLowerCase()
    return text.includes(keyword)
  })
})

function enrollmentJoinSourceText(source?: 'STUDENT_SELF' | 'TEACHER_MANUAL' | null) {
  return source === 'TEACHER_MANUAL' ? '教师加入' : '学生自选'
}

async function saveExperimentCourse() {
  if (!experimentCourseForm.title.trim()) {
    ElMessage.warning('请输入课程标题')
    return
  }
  if (!experimentCourseForm.semesterId) {
    ElMessage.warning('请选择学期')
    return
  }
  if (!experimentCourseForm.enrollDeadlineAt) {
    ElMessage.warning('请选择截止选课时间')
    return
  }
  const firstInvalidSlot = experimentCourseForm.slots.find((slot, index) => getExperimentCourseSlotIssue(slot, index))
  if (firstInvalidSlot) {
    const slotIndex = experimentCourseForm.slots.indexOf(firstInvalidSlot)
    ElMessage.warning(`场次${slotIndex + 1}：${getExperimentCourseSlotIssue(firstInvalidSlot, slotIndex)}`)
    return
  }
  savingExperimentCourse.value = true
  try {
    const body = {
      title: experimentCourseForm.title,
      description: experimentCourseForm.description || null,
      semesterId: experimentCourseForm.semesterId,
      enrollDeadlineAt: experimentCourseForm.enrollDeadlineAt,
      targetClassIds: experimentCourseForm.targetClassIds,
      targetStudentIds: experimentCourseForm.targetStudentIds,
      slots: experimentCourseForm.slots.map((slot) => ({
        id: slot.id,
        name: slot.name || null,
        mode: slot.mode,
        firstLessonDate: slot.firstLessonDate,
        slotId: slot.slotId,
        labRoomId: slot.labRoomId,
        capacity: slot.capacity,
        repeatPattern: slot.mode === 'RECURRING' ? slot.repeatPattern : null,
        rangeMode: slot.mode === 'RECURRING' ? slot.rangeMode : null,
        rangeStartDate: slot.mode === 'RECURRING' && slot.rangeMode === 'DATE_RANGE' ? slot.rangeStartDate : null,
        rangeEndDate: slot.mode === 'RECURRING' && slot.rangeMode === 'DATE_RANGE' ? slot.rangeEndDate : null,
      })),
    }
    const saved = await apiData<ExperimentCourseVO>(
      editingExperimentCourseId.value
        ? `/api/teacher/experiment-courses/${editingExperimentCourseId.value}`
        : '/api/teacher/experiment-courses',
      {
        method: editingExperimentCourseId.value ? 'PUT' : 'POST',
        body,
      },
      auth.token,
    )
    experimentCourseDialog.value = false
    ElMessage.success(editingExperimentCourseId.value ? '实验课程已更新' : '实验课程已创建')
    await loadExperimentCourses(saved.id)
  } catch (e: any) {
    ElMessage.error(e?.message ?? '保存实验课程失败')
  } finally {
    savingExperimentCourse.value = false
  }
}

async function toggleExperimentCourseStatus(course: ExperimentCourseVO) {
  togglingExperimentCourseId.value = course.id
  try {
    const updated = await apiData<ExperimentCourseVO>(
      `/api/teacher/experiment-courses/${course.id}/status`,
      {
        method: 'PUT',
        body: { status: course.status === 'OPEN' ? 'CLOSED' : 'OPEN' },
      },
      auth.token,
    )
    ElMessage.success(updated.status === 'OPEN' ? '课程已开启' : '课程已关闭')
    await loadExperimentCourses(updated.id)
  } catch (e: any) {
    ElMessage.error(e?.message ?? '更新课程状态失败')
  } finally {
    togglingExperimentCourseId.value = null
  }
}

function buildHistoryQuery() {
  const q = new URLSearchParams()
  q.set('sourceType', 'EXPERIMENT_COURSE')
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
    const data = await apiData<PageResult<AttendanceHistoryItem>>(
      `/api/teacher/attendance/sessions?${buildHistoryQuery()}`,
      { method: 'GET' },
      auth.token,
    )
    historyData.value = {
      ...data,
      items: (data.items ?? []).filter((item) => item.sourceType === 'EXPERIMENT_COURSE'),
    }
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

async function exportHistoryAttendanceExcel(sessionId: number) {
  try {
    await downloadBlob(`/api/attendance/sessions/${sessionId}/export/excel`, {
      token: auth.token,
      fallbackFilename: `attendance-session-${sessionId}.xlsx`,
    })
    ElMessage.success('已开始下载签到 Excel')
  } catch (e: any) {
    ElMessage.error(e?.message ?? '导出失败')
  }
}

watch(
  () => createDialog.value,
  async (open) => {
    if (!open) {
      createForm.experimentCourseId = null
      createTaskFiles.value = []
      if (createTaskFileInput.value) createTaskFileInput.value.value = ''
      return
    }
    if (!experimentCourses.value.length) {
      await loadExperimentCourses()
    }
    await loadClassOptions()
  },
)

watch(classScope, async () => {
  if (!createDialog.value) return
  createForm.classIds = []
  await loadClassOptions()
})

watch(selectedExperimentCourseId, async () => {
  if (activeTab.value !== 'course') return
  await loadExperimentCourseEnrollments()
})

async function openReview(row: SubmissionVO) {
  reviewTarget.value = row
  reviewForm.score = 95
  reviewForm.comment = '完成很好'
  reviewForm.issueTags = []
  try {
    const current = await apiData<ReviewVO>(
      `/api/submissions/${row.id}/review`,
      { method: 'GET' },
      auth.token,
    )
    reviewForm.score = current.score ?? reviewForm.score
    reviewForm.comment = current.comment ?? ''
    reviewForm.issueTags = [...(current.issueTags || [])]
  } catch {
    // 未批阅时保持默认值
  }
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
      { method: 'POST', body: { score: reviewForm.score, comment: reviewForm.comment, issueTags: reviewForm.issueTags } },
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

async function exportExcel() {
  if (!selectedTaskId.value) return
  try {
    await downloadBlob(`/api/tasks/${selectedTaskId.value}/scores/export/excel`, {
      token: auth.token,
      fallbackFilename: `task-${selectedTaskId.value}-scores.xlsx`,
    })
    ElMessage.success('已开始下载 Excel')
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

onMounted(async () => {
  await loadTasks()
  await loadExperimentCourses()
  await loadTeacherDiscussionUnreadSummary()
  startTeacherDiscussionPolling()
})

watch(activeTab, async (tab) => {
  if (tab === 'course') {
    if (!experimentCourseMeta.value.semesters.length) {
      await loadExperimentCourseMeta()
    }
    await loadExperimentCourses()
    return
  }
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
  if (tab === 'discussion') {
    await Promise.all([loadTeacherDiscussionUnreadSummary(), loadTeacherDiscussions()])
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
    if (historyData.value.items.length === 0) {
      await loadHistorySessions()
    }
  }
})

onBeforeUnmount(() => {
  stopLoops()
  stopTeacherDiscussionPolling()
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
    const items = await apiData<WeekScheduleItem[]>(
      `/api/teacher/schedule/week?semesterId=${scheduleSemesterId.value}&weekStartDate=${encodeURIComponent(weekStartDate.value)}`,
      { method: 'GET' },
      auth.token,
    )
    weekItems.value = (items ?? []).filter((item) => item.sourceType === 'EXPERIMENT_COURSE')
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

async function openExperimentCourseInstanceForAttendance(
  course: ExperimentCourseVO,
  slot: ExperimentCourseSlotVO,
  instance: ExperimentCourseSlotInstanceVO,
) {
  selectedCell.value = {
    id: instance.id,
    sourceType: 'EXPERIMENT_COURSE',
    experimentCourseId: course.id,
    experimentCourseSlotId: slot.id,
    experimentCourseInstanceId: instance.id,
    semesterId: course.semesterId,
    classId: null,
    className: slot.name || `场次${slot.id}`,
    labRoomName: instance.labRoomName || slot.labRoomName || null,
    lessonDate: instance.lessonDate,
    slotId: instance.slotId,
    slotName: instance.slotName,
    courseName: course.title,
  }
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
    if (selectedCell.value.sourceType === 'EXPERIMENT_COURSE') {
      if (!selectedCell.value.experimentCourseSlotId) {
        roster.value = []
        ElMessage.error('当前实验课程场次缺少场次信息')
        return
      }
      roster.value = await apiData<RosterStudent[]>(
        `/api/teacher/experiment-course-slots/${selectedCell.value.experimentCourseSlotId}/roster`,
        { method: 'GET' },
        auth.token,
      )
    } else {
      roster.value = await apiData<RosterStudent[]>(
        `/api/teacher/classes/${selectedCell.value.classId}/roster`,
        { method: 'GET' },
        auth.token,
      )
    }
  } catch (e: any) {
    roster.value = []
    ElMessage.error(e?.message ?? '加载签到名单失败')
  } finally {
    loadingRoster.value = false
  }
}

async function startSession() {
  if (!selectedCell.value) return
  try {
    const body
      = selectedCell.value.sourceType === 'EXPERIMENT_COURSE'
        ? {
            experimentCourseInstanceId: selectedCell.value.experimentCourseInstanceId,
            ...(qrMode.value === 'dynamic' ? { tokenTtlSeconds: desiredTokenTtlSeconds() } : {}),
          }
        : (
            qrMode.value === 'dynamic'
              ? { scheduleId: selectedCell.value.id, tokenTtlSeconds: desiredTokenTtlSeconds() }
              : { scheduleId: selectedCell.value.id }
          )
    session.value = await apiData<AttendanceSession>(
      `/api/attendance/sessions`,
      {
        method: 'POST',
        body,
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

async function exportAttendanceExcel() {
  if (!session.value) return
  try {
    await downloadBlob(`/api/attendance/sessions/${session.value.id}/export/excel`, {
      token: auth.token,
      fallbackFilename: `attendance-session-${session.value.id}.xlsx`,
    })
    ElMessage.success('已开始下载 Excel')
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
      <el-aside v-if="(activeTab === 'report' || activeTab === 'flow' || activeTab === 'discussion') && !isMobile" width="360px" class="aside">
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
          <el-tab-pane label="实验课程" name="course">
            <el-card class="block" shadow="never">
              <template #header>
                <div style="display: flex; justify-content: space-between; align-items: center; gap: 10px; flex-wrap: wrap">
                  <div>实验课程</div>
                  <div style="display: flex; gap: 10px; flex-wrap: wrap">
                    <el-button size="small" :loading="loadingExperimentCourses" @click="loadExperimentCourses()">刷新课程</el-button>
                    <el-button type="primary" size="small" @click="openCreateExperimentCourseDialog">新建实验课程</el-button>
                  </div>
                </div>
              </template>
              <template v-if="!isMobile">
                <el-table
                  :data="experimentCourses"
                  size="small"
                  stripe
                  v-loading="loadingExperimentCourses"
                  @row-click="(row: ExperimentCourseVO) => { selectedExperimentCourseId = row.id }"
                  :row-class-name="({ row }: { row: ExperimentCourseVO }) => (row.id === selectedExperimentCourseId ? 'is-active' : '')"
                >
                  <el-table-column prop="title" label="课程" min-width="180" />
                  <el-table-column prop="semesterName" label="学期" min-width="140" />
                  <el-table-column prop="status" label="状态" width="100" />
                  <el-table-column prop="enrollDeadlineAt" label="截止选课时间" min-width="180" />
                  <el-table-column label="场次/人数" min-width="180">
                    <template #default="{ row }: { row: ExperimentCourseVO }">
                      {{ row.slots.length }} 个场次 / 已选 {{ row.totalEnrolled || 0 }} 人
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="220">
                    <template #default="{ row }: { row: ExperimentCourseVO }">
                      <el-button size="small" @click.stop="openEditExperimentCourseDialog(row)">编辑</el-button>
                      <el-button size="small" @click.stop="toggleExperimentCourseStatus(row)" :loading="togglingExperimentCourseId === row.id">
                        {{ row.status === 'OPEN' ? '关闭' : '开启' }}
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </template>
              <template v-else>
                <div v-if="loadingExperimentCourses" class="meta">加载中...</div>
                <div v-else-if="experimentCourses.length === 0" class="meta">暂无实验课程</div>
                <el-card v-else v-for="course in experimentCourses" :key="course.id" shadow="never" class="stuCard">
                  <div class="stuHead">
                    <div>
                      <div class="stuName">{{ course.title }}</div>
                      <div class="meta">{{ course.semesterName || '-' }}</div>
                    </div>
                    <el-tag size="small" :type="course.status === 'OPEN' ? 'success' : 'info'">{{ course.status }}</el-tag>
                  </div>
                  <div class="meta" style="margin-top: 8px">截止：{{ course.enrollDeadlineAt }}</div>
                  <div class="meta">场次：{{ course.slots.length }} / 已选 {{ course.totalEnrolled || 0 }} 人</div>
                  <div class="stuActions">
                    <el-button size="small" @click="selectedExperimentCourseId = course.id">查看名单</el-button>
                    <el-button size="small" @click="openEditExperimentCourseDialog(course)">编辑</el-button>
                    <el-button size="small" @click="toggleExperimentCourseStatus(course)" :loading="togglingExperimentCourseId === course.id">
                      {{ course.status === 'OPEN' ? '关闭' : '开启' }}
                    </el-button>
                  </div>
                </el-card>
              </template>
            </el-card>

            <el-card class="block" shadow="never" v-if="experimentCourseDetail">
              <template #header>
                <div style="display: flex; justify-content: space-between; align-items: center; gap: 10px; flex-wrap: wrap">
                  <div>{{ experimentCourseDetail.title }}</div>
                  <div class="meta">报名管理</div>
                </div>
              </template>
              <div class="meta" style="margin-bottom: 8px">截止：{{ experimentCourseDetail.enrollDeadlineAt }} / 状态：{{ experimentCourseDetail.status }}</div>
              <div class="meta" style="margin-bottom: 12px">说明：{{ experimentCourseDetail.description || '（无说明）' }}</div>
              <el-card shadow="never" style="margin-bottom: 12px; background: #fafafa">
                <div style="display: flex; justify-content: space-between; align-items: center; gap: 10px; flex-wrap: wrap">
                  <div style="font-weight: 600">手动加入学生</div>
                  <div class="meta">指定学生只代表可自助选课；这里会直接把学生加入到具体场次</div>
                </div>
                <div :class="isMobile ? 'mobileRosterEditor' : 'desktopRosterEditor'" style="margin-top: 10px">
                  <el-select
                    v-model="manualRosterStudentId"
                    filterable
                    clearable
                    remote
                    reserve-keyword
                    placeholder="搜索学生姓名/用户名"
                    :style="isMobile ? 'width: 100%' : 'width: 320px'"
                    :remote-method="searchManualRosterStudents"
                    :loading="loadingExperimentCourseStudents"
                  >
                    <el-option
                      v-for="student in availableRosterStudentOptions"
                      :key="student.id"
                      :label="`${student.displayName} / ${student.username}${student.classDisplayName ? ` / ${student.classDisplayName}` : ''}`"
                      :value="student.id"
                    />
                  </el-select>
                  <el-select v-model="manualRosterSlotId" placeholder="选择目标场次" :style="isMobile ? 'width: 100%' : 'width: 300px'">
                    <el-option
                      v-for="(slot, slotIndex) in experimentCourseDetail.slots"
                      :key="slot.id"
                      :label="`${slot.name || `场次${slotIndex + 1}`} / ${slot.mode === 'RECURRING' ? '多次课' : '单次课'} / 已选 ${slot.enrolledCount}`"
                      :value="slot.id"
                    />
                  </el-select>
                  <el-button type="primary" :loading="enrollingRosterStudent" @click="enrollRosterStudent">加入场次</el-button>
                </div>
              </el-card>
              <div v-for="(slot, slotIndex) in experimentCourseDetail.slots" :key="slot.id" class="deviceBox" style="margin-bottom: 10px">
                <div class="deviceTitle">{{ slot.name || `场次${slotIndex + 1}` }} / {{ slot.mode === 'RECURRING' ? '多次课' : '单次课' }}</div>
                <div class="meta">
                  首次上课：{{ slot.firstLessonDate }} {{ slot.slotName }} / {{ slot.labRoomName || '-' }}
                  <span v-if="slot.mode === 'RECURRING'">
                    / {{ slot.repeatPattern === 'ODD_WEEK' ? '单周' : '每周' }}
                    / {{ slot.rangeMode === 'SEMESTER' ? '整学期' : `${slot.rangeStartDate} ~ ${slot.rangeEndDate}` }}
                  </span>
                </div>
                <div class="meta">容量：{{ slot.capacity }} / 已选：{{ slot.enrolledCount }} / 剩余：{{ slot.remainingCapacity }}</div>
                <div style="margin-top: 8px">
                  <el-button size="small" text @click="experimentCourseDetailExpanded[slot.id] = !experimentCourseDetailExpanded[slot.id]">
                    {{ experimentCourseDetailExpanded[slot.id] ? '收起实例' : `展开实例（${slot.instances.length}）` }}
                  </el-button>
                </div>
                <div v-if="experimentCourseDetailExpanded[slot.id]" class="progressAtts" style="margin-top: 8px">
                  <div v-for="instance in slot.instances" :key="instance.id" class="progressAtt">
                    <div class="attName">{{ instance.displayName }}</div>
                    <div class="meta">{{ instance.lessonDate }} / {{ formatWeekday(instance.lessonDate) }} / {{ instance.slotName }} / {{ instance.labRoomName || '-' }}</div>
                    <div style="margin-top: 8px">
                      <el-button
                        size="small"
                        type="primary"
                        plain
                        @click="openExperimentCourseInstanceForAttendance(experimentCourseDetail, slot, instance)"
                      >
                        去签到
                      </el-button>
                    </div>
                  </div>
                </div>
                <template v-if="!isMobile">
                  <el-table
                    :data="experimentCourseEnrollments.filter((row) => row.slotId === slot.id)"
                    size="small"
                    v-loading="loadingCourseEnrollments"
                    empty-text="该场次暂无学生报名"
                    style="margin-top: 8px"
                  >
                    <el-table-column prop="studentDisplayName" label="学生姓名" min-width="140" />
                    <el-table-column prop="studentUsername" label="用户名" width="140" />
                    <el-table-column prop="classDisplayName" label="班级" min-width="170" />
                    <el-table-column label="加入方式" width="110">
                      <template #default="{ row }: { row: ExperimentCourseEnrollmentRowVO }">
                        {{ enrollmentJoinSourceText(row.joinSource) }}
                      </template>
                    </el-table-column>
                    <el-table-column prop="selectedAt" label="选课时间" min-width="180" />
                    <el-table-column label="操作" width="110" fixed="right">
                      <template #default="{ row }: { row: ExperimentCourseEnrollmentRowVO }">
                        <el-button
                          size="small"
                          type="danger"
                          text
                          :loading="removingRosterStudentId === row.studentId"
                          @click="removeRosterStudent(row)"
                        >
                          踢出
                        </el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </template>
                <template v-else>
                  <div v-if="loadingCourseEnrollments" class="meta" style="margin-top: 8px">加载中...</div>
                  <div v-else-if="experimentCourseEnrollments.filter((row) => row.slotId === slot.id).length === 0" class="meta" style="margin-top: 8px">
                    该场次暂无学生报名
                  </div>
                  <el-card
                    v-else
                    v-for="row in experimentCourseEnrollments.filter((item) => item.slotId === slot.id)"
                    :key="`${slot.id}-${row.studentId}`"
                    shadow="never"
                    class="stuCard"
                    style="margin-top: 8px"
                  >
                    <div class="stuHead">
                      <div>
                        <div class="stuName">{{ row.studentDisplayName }}</div>
                        <div class="meta">{{ row.studentUsername }}</div>
                      </div>
                      <el-tag size="small">{{ enrollmentJoinSourceText(row.joinSource) }}</el-tag>
                    </div>
                    <div class="meta" style="margin-top: 8px">班级：{{ row.classDisplayName || '-' }}</div>
                    <div class="meta">选课时间：{{ row.selectedAt }}</div>
                    <div class="stuActions">
                      <el-button
                        size="small"
                        type="danger"
                        :loading="removingRosterStudentId === row.studentId"
                        @click="removeRosterStudent(row)"
                      >
                        踢出
                      </el-button>
                    </div>
                  </el-card>
                </template>
              </div>
              <el-card shadow="never" style="margin-top: 12px">
                <template #header>
                  <div style="display: flex; justify-content: space-between; align-items: center; gap: 10px">
                    <div>禁选名单</div>
                    <div class="meta">被踢出的学生不能自行重新选课，需老师解除禁选或重新手动加入</div>
                  </div>
                </template>
                <template v-if="!isMobile">
                  <el-table
                    :data="experimentCourseBlockedStudents"
                    size="small"
                    v-loading="loadingCourseEnrollments"
                    empty-text="暂无禁选学生"
                  >
                    <el-table-column prop="studentDisplayName" label="学生姓名" min-width="140" />
                    <el-table-column prop="studentUsername" label="用户名" width="140" />
                    <el-table-column prop="classDisplayName" label="班级" min-width="170" />
                    <el-table-column prop="blockedAt" label="禁选时间" min-width="180" />
                    <el-table-column label="操作" width="120" fixed="right">
                      <template #default="{ row }: { row: ExperimentCourseBlockedStudentVO }">
                        <el-button
                          size="small"
                          type="primary"
                          text
                          :loading="unblockingRosterStudentId === row.studentId"
                          @click="unblockRosterStudent(row)"
                        >
                          解除禁选
                        </el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </template>
                <template v-else>
                  <div v-if="loadingCourseEnrollments" class="meta">加载中...</div>
                  <div v-else-if="experimentCourseBlockedStudents.length === 0" class="meta">暂无禁选学生</div>
                  <el-card v-else v-for="row in experimentCourseBlockedStudents" :key="row.studentId" shadow="never" class="stuCard">
                    <div class="stuHead">
                      <div>
                        <div class="stuName">{{ row.studentDisplayName }}</div>
                        <div class="meta">{{ row.studentUsername }}</div>
                      </div>
                      <el-tag size="small" type="warning">禁选</el-tag>
                    </div>
                    <div class="meta" style="margin-top: 8px">班级：{{ row.classDisplayName || '-' }}</div>
                    <div class="meta">禁选时间：{{ row.blockedAt }}</div>
                    <div class="stuActions">
                      <el-button
                        size="small"
                        type="primary"
                        :loading="unblockingRosterStudentId === row.studentId"
                        @click="unblockRosterStudent(row)"
                      >
                        解除禁选
                      </el-button>
                    </div>
                  </el-card>
                </template>
              </el-card>
            </el-card>
          </el-tab-pane>

          <el-tab-pane label="实验室管理" name="labroom">
            <div class="card card-pad">
              <div style="display: flex; justify-content: space-between; gap: 12px; align-items: center; flex-wrap: wrap">
                <div>
                  <div>实验室管理</div>
                  <div class="meta">教师端和管理员端共用同一套实验室库。这里可维护实验室基础信息与按周开放时段。</div>
                </div>
              </div>
            </div>
            <LabRoomManager api-base="/api/teacher/lab-rooms" />
          </el-tab-pane>

          <el-tab-pane name="discussion">
            <template #label>
              <el-badge :value="teacherDiscussionUnreadCount" :hidden="teacherDiscussionUnreadCount <= 0" :max="99">
                <span>讨论区</span>
              </el-badge>
            </template>

            <el-card v-if="isMobile" class="block" shadow="never">
              <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap">
                <el-select
                  v-model="selectedTaskId"
                  placeholder="选择任务"
                  style="width: 260px"
                  :loading="loadingTasks"
                  clearable
                  @change="(id: any) => id && selectTask(Number(id))"
                >
                  <el-option v-for="t in tasks" :key="t.id" :label="t.title" :value="t.id" />
                </el-select>
                <el-button size="small" :loading="loadingTasks" @click="loadTasks">刷新任务</el-button>
              </div>
            </el-card>

            <el-card class="block" shadow="never">
              <template #header>
                <div style="display: flex; justify-content: space-between; align-items: center; gap: 10px; flex-wrap: wrap">
                  <div>向老师提问聚合</div>
                </div>
              </template>
              <div class="toolbar">
                <el-input v-model="teacherDiscussionQ" clearable placeholder="搜索任务名/学生/内容摘要" style="width: 280px" />
                <el-switch v-model="teacherDiscussionUnreadOnly" active-text="仅看未读" />
                <el-button type="primary" :loading="teacherDiscussionLoading" @click="loadTeacherDiscussions">查询</el-button>
                <el-button :loading="teacherDiscussionLoading" @click="handleDiscussionChanged">刷新</el-button>
              </div>
              <template v-if="!isMobile">
                <el-table :data="teacherDiscussionItems" size="small" v-loading="teacherDiscussionLoading" empty-text="暂无提问" @row-click="(row: TeacherDiscussionAggregateItemVO) => openTeacherDiscussionItem(row)">
                  <el-table-column prop="taskTitle" label="任务" min-width="180" />
                  <el-table-column label="学生" min-width="180">
                    <template #default="{ row }: { row: TeacherDiscussionAggregateItemVO }">
                      <div>{{ row.studentDisplayName }}</div>
                      <div class="meta">{{ row.studentUsername }}</div>
                    </template>
                  </el-table-column>
                  <el-table-column prop="latestMessagePreview" label="最新摘要" min-width="260" />
                  <el-table-column prop="latestMessageAt" label="最新时间" min-width="180" />
                  <el-table-column label="未读" width="90">
                    <template #default="{ row }: { row: TeacherDiscussionAggregateItemVO }">
                      <el-badge :value="row.unreadCount" :hidden="row.unreadCount <= 0" :max="99" />
                    </template>
                  </el-table-column>
                </el-table>
              </template>
              <template v-else>
                <div v-if="teacherDiscussionLoading" class="meta">加载中...</div>
                <div v-else-if="teacherDiscussionItems.length === 0" class="meta">暂无提问</div>
                <el-card v-else v-for="item in teacherDiscussionItems" :key="item.threadId" shadow="never" class="stuCard" @click="openTeacherDiscussionItem(item)">
                  <div class="stuHead">
                    <div>
                      <div class="stuName">{{ item.taskTitle }}</div>
                      <div class="meta">{{ item.studentDisplayName }} / {{ item.studentUsername }}</div>
                    </div>
                    <el-badge :value="item.unreadCount" :hidden="item.unreadCount <= 0" :max="99" />
                  </div>
                  <div class="meta" style="margin-top: 8px">{{ item.latestMessageAt || '-' }}</div>
                  <div style="margin-top: 8px; white-space: pre-wrap; word-break: break-word">{{ item.latestTeacherReplyPreview || item.latestMessagePreview || '暂无摘要' }}</div>
                </el-card>
              </template>
            </el-card>

            <el-card v-if="selectedTaskId && taskDetail" class="block" shadow="never">
              <template #header>
                <div style="display: flex; justify-content: space-between; align-items: center; gap: 10px; flex-wrap: wrap">
                  <div>{{ taskDetail.title }}</div>
                  <div class="meta">任务讨论区</div>
                </div>
              </template>
              <TaskDiscussionPanel
                :task-id="selectedTaskId"
                mode="teacher"
                :highlight-thread-id="highlightedDiscussionThreadId"
                :polling-enabled="activeTab === 'discussion'"
                @changed="handleDiscussionChanged"
              />
            </el-card>
          </el-tab-pane>

          <el-tab-pane label="统计报表" name="stats">
            <StatisticsPanel />
          </el-tab-pane>

          <el-tab-pane label="教学分析" name="analytics">
            <TeachingAnalyticsPanel mode="teacher" />
          </el-tab-pane>

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
                    <el-table-column label="完成来源" width="140">
                      <template #default="{ row }: { row: TeacherTaskProgressStudentVO }">
                        {{ row.completionStatus === 'CONFIRMED' ? completionSourceText(row.completionSource) : '-' }}
                      </template>
                    </el-table-column>
                    <el-table-column label="操作" width="320" fixed="right">
                      <template #default="{ row }: { row: TeacherTaskProgressStudentVO }">
                        <el-button size="small" @click="openProgressDetail(row)">查看过程</el-button>
                        <el-button
                          v-if="row.completionStatus === 'NONE'"
                          size="small"
                          type="success"
                          :loading="confirmingCompletion === row.studentId"
                          @click="directConfirmCompletion(row)"
                        >
                          直接登记完成
                        </el-button>
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
                    <div v-if="row.completionStatus === 'CONFIRMED'" class="meta">登记来源：{{ completionSourceText(row.completionSource) }}</div>
                    <div class="stuActions">
                      <el-button size="small" @click="openProgressDetail(row)">查看过程</el-button>
                      <el-button
                        v-if="row.completionStatus === 'NONE'"
                        size="small"
                        type="success"
                        :loading="confirmingCompletion === row.studentId"
                        @click="directConfirmCompletion(row)"
                      >
                        直接登记完成
                      </el-button>
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
                  <el-button @click="exportDeviceRequests">导出 CSV</el-button>
                  <el-button type="primary" plain @click="exportDeviceRequestsExcel">导出 Excel</el-button>
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
                    <el-button type="primary" plain size="small" @click="exportExcel">导出 Excel</el-button>
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
                这里只显示实验课程课次。点击格子可开启/结束该课次签到，并查看当前场次报名名单与实时签到名单。
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
                            <div class="title">{{ itemAt(ts.id, d.date)!.courseName || '（未命名课程）' }}</div>
                            <div class="meta">{{ itemAt(ts.id, d.date)!.className }}</div>
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
                这里只显示实验课程签到记录，可按课室、时间和状态筛选，并重新下载签到 CSV。
              </div>

              <template v-if="!isMobile">
                <el-table :data="historyData.items" v-loading="historyLoading" stripe>
                  <el-table-column prop="courseName" label="课程" min-width="140" />
                  <el-table-column prop="classDisplayName" label="场次" min-width="180" />
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
                      <el-button size="small" type="primary" plain @click="exportHistoryAttendanceExcel(row.sessionId)">导出 Excel</el-button>
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
                      <div class="meta">{{ row.classDisplayName }} / {{ row.slotName || '-' }}</div>
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
                    <el-button size="small" type="primary" plain @click="exportHistoryAttendanceExcel(row.sessionId)">导出 Excel</el-button>
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
      <el-form-item label="关联实验课程（可选）">
        <el-select
          v-model="createForm.experimentCourseId"
          clearable
          filterable
          placeholder="选择已开的实验课程"
          style="width: 100%"
        >
          <el-option
            v-for="course in experimentCourses.filter((item) => item.status === 'OPEN')"
            :key="course.id"
            :label="`${course.title}${course.semesterName ? ` / ${course.semesterName}` : ''}`"
            :value="course.id"
          />
        </el-select>
        <div class="meta" style="margin-top: 6px">
          绑定实验课程后，当前及后续选上该课程的学生都能看到这条任务。
        </div>
      </el-form-item>
      <el-form-item label="发布班级（不选 = 全体学生）">
        <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap; width: 100%">
            <el-radio-group v-model="classScope" size="small">
              <el-radio-button label="mine">我的绑定班级</el-radio-button>
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
            默认仅显示管理员为你绑定的班级；切换到“全部班级”可看到全系统班级。
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

  <el-dialog
    v-model="experimentCourseDialog"
    :title="editingExperimentCourseId ? '编辑实验课程' : '新建实验课程'"
    :width="isMobile ? '96vw' : '760px'"
    top="4vh"
  >
    <el-form label-position="top">
      <el-form-item label="课程标题">
        <el-input v-model="experimentCourseForm.title" placeholder="例如：示波器基础实验" />
      </el-form-item>
      <el-form-item label="课程说明">
        <el-input v-model="experimentCourseForm.description" type="textarea" :rows="4" placeholder="填写实验课程介绍或选课说明" />
      </el-form-item>
      <div :class="isMobile ? 'mobileFormGrid' : 'dialogGridTwo'">
        <el-form-item label="学期">
          <div style="display: flex; flex-direction: column; gap: 8px; width: 100%">
            <el-select v-model="experimentCourseForm.semesterId" placeholder="选择学期" style="width: 100%">
              <el-option v-for="item in experimentCourseMeta.semesters" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
            <div style="display: flex; gap: 8px; flex-wrap: wrap">
              <el-button size="small" text type="primary" @click="openCreateSemesterDialog">新增学期</el-button>
              <el-button size="small" text type="primary" :disabled="!experimentCourseForm.semesterId" @click="openEditSemesterDialog">编辑当前学期</el-button>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="截止选课时间">
          <el-date-picker
            v-model="experimentCourseForm.enrollDeadlineAt"
            type="datetime"
            style="width: 100%"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
      </div>
      <el-form-item label="开放班级（并集）">
        <el-select
          v-model="experimentCourseForm.targetClassIds"
          multiple
          filterable
          clearable
          collapse-tags
          collapse-tags-tooltip
          :loading="loadingExperimentCourseClasses"
          placeholder="选择开放班级"
          style="width: 100%"
        >
          <el-option
            v-for="c in experimentCourseClassOptions"
            :key="c.id"
            :label="c.departmentName ? `${c.departmentName} / ${c.name}` : c.name"
            :value="c.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="指定学生（并集）">
        <el-select
          v-model="experimentCourseForm.targetStudentIds"
          multiple
          filterable
          remote
          reserve-keyword
          clearable
          collapse-tags
          collapse-tags-tooltip
          :remote-method="searchExperimentCourseStudents"
          :loading="loadingExperimentCourseStudents"
          placeholder="搜索学生姓名/用户名"
          style="width: 100%"
        >
          <el-option
            v-for="student in experimentCourseStudentOptions"
            :key="student.id"
            :label="`${student.displayName} / ${student.username}${student.classDisplayName ? ` / ${student.classDisplayName}` : ''}`"
            :value="student.id"
          />
        </el-select>
        <div class="meta" style="margin-top: 6px">
          这里只控制“谁可以自己选课”；如果要老师直接把学生加入某个场次，请到课程详情里的“手动加入学生”操作。
        </div>
      </el-form-item>
      <el-form-item label="可选场次">
        <div style="display: flex; flex-direction: column; gap: 10px; width: 100%">
          <div v-for="(slot, index) in experimentCourseForm.slots" :key="index" class="deviceBox">
            <div :class="isMobile ? 'mobileSectionHeader' : 'desktopSectionHeader'" style="margin-bottom: 10px">
              <div class="deviceTitle">场次 {{ index + 1 }}</div>
              <el-button size="small" type="danger" plain @click="removeExperimentCourseSlot(index)">删除场次</el-button>
            </div>
            <div :class="isMobile ? 'mobileFormGrid' : 'dialogGridTwo'">
              <el-input v-model="slot.name" placeholder="场次名称（可选）" />
              <el-radio-group v-model="slot.mode">
                <el-radio-button label="SINGLE">单次课</el-radio-button>
                <el-radio-button label="RECURRING">多次课</el-radio-button>
              </el-radio-group>
              <el-date-picker v-model="slot.firstLessonDate" type="date" value-format="YYYY-MM-DD" placeholder="首次上课日期" style="width: 100%" />
              <el-select v-model="slot.slotId" placeholder="节次" style="width: 100%">
                <el-option v-for="item in experimentCourseMeta.timeSlots" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
              <el-select v-model="slot.labRoomId" placeholder="地点" style="width: 100%">
                <el-option
                  v-for="item in experimentCourseMeta.labRooms"
                  :key="item.id"
                  :label="item.description ? `${item.name}（${item.description}）` : item.name"
                  :value="item.id"
                />
              </el-select>
              <el-input-number v-model="slot.capacity" :min="1" :max="999" style="width: 100%" />
            </div>
            <div v-if="slot.mode === 'RECURRING'" :class="isMobile ? 'mobileFormGrid' : 'dialogGridTwo'" style="margin-top: 12px">
              <el-form-item label="重复方式" style="margin-bottom: 0">
                <el-radio-group v-model="slot.repeatPattern">
                  <el-radio-button label="EVERY_WEEK">每周</el-radio-button>
                  <el-radio-button label="ODD_WEEK">单周</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="生效范围" style="margin-bottom: 0">
                <el-radio-group v-model="slot.rangeMode">
                  <el-radio-button label="SEMESTER">整学期</el-radio-button>
                  <el-radio-button label="DATE_RANGE">自定义日期区间</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <template v-if="slot.rangeMode === 'DATE_RANGE'">
                <el-date-picker v-model="slot.rangeStartDate" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" style="width: 100%" />
                <el-date-picker v-model="slot.rangeEndDate" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" style="width: 100%" />
              </template>
            </div>
            <div style="display: flex; gap: 10px; margin-top: 12px; flex-wrap: wrap">
              <el-button size="small" type="primary" plain @click="finalizeExperimentCourseSlot(slot, index)">完成场次设定</el-button>
              <el-button size="small" text @click="slot.previewExpanded = !slot.previewExpanded">
                {{ slot.previewExpanded ? '收起实例预览' : '展开实例预览' }}
              </el-button>
              <span class="meta">{{ previewSlotSummary(slot, index) }}</span>
            </div>
            <div v-if="slot.previewExpanded" class="progressAtts" style="margin-top: 10px">
              <div v-for="item in buildExperimentCoursePreview(slot, index)" :key="`${item.lessonDate}-${item.teachingWeek}`" class="progressAtt">
                <div class="attName">{{ item.displayName }}</div>
                <div class="meta">{{ item.lessonDate }} / {{ formatWeekday(item.lessonDate) }} / 学期第{{ item.teachingWeek }}周</div>
              </div>
              <div v-if="buildExperimentCoursePreview(slot, index).length === 0" class="meta">当前设定尚未生成可用课次</div>
            </div>
          </div>
          <div>
            <el-button size="small" @click="addExperimentCourseSlot">新增场次</el-button>
          </div>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="experimentCourseDialog = false">取消</el-button>
      <el-button type="primary" :loading="savingExperimentCourse" @click="saveExperimentCourse">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="semesterManageDialog"
    :title="semesterManageEditingId ? '编辑学期' : '新建学期'"
    :width="isMobile ? '92vw' : '520px'"
  >
    <el-form label-position="top">
      <el-form-item label="学期名称">
        <el-input v-model="semesterManageForm.name" placeholder="例如：2026-2027-1" />
      </el-form-item>
      <div :class="isMobile ? 'mobileFormGrid' : 'dialogGridTwo'">
        <el-form-item label="开始日期">
          <el-date-picker v-model="semesterManageForm.startDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择开始日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker v-model="semesterManageForm.endDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择结束日期" style="width: 100%" />
        </el-form-item>
      </div>
      <div class="meta">修改学期日期后，如已有实验课程或课次超出新范围，系统会允许保存并给出警告，但不会自动调整已有课程。</div>
    </el-form>
    <template #footer>
      <el-button @click="semesterManageDialog = false">取消</el-button>
      <el-button type="primary" :loading="semesterManageSaving" @click="submitSemesterDialog">保存学期</el-button>
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
      <el-form-item label="问题标签">
        <el-select v-model="reviewForm.issueTags" multiple collapse-tags collapse-tags-tooltip placeholder="选择问题标签" style="width: 100%">
          <el-option v-for="item in reviewIssueOptions" :key="item.code" :label="item.label" :value="item.code" />
        </el-select>
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
          <div v-if="progressDetail.completionStatus === 'CONFIRMED'" class="meta">登记来源：{{ completionSourceText(progressDetail.completionSource) }}</div>
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
            v-if="progressDetail.completionStatus === 'NONE'"
            type="success"
            :loading="confirmingCompletion === progressDetail.studentId"
            @click="directConfirmCompletion({
              studentId: progressDetail.studentId,
              studentUsername: progressDetail.studentUsername,
              studentDisplayName: progressDetail.studentDisplayName,
              classDisplayName: progressDetail.classDisplayName,
              progressCount: progressDetail.logs.length,
              completionStatus: progressDetail.completionStatus,
              completionSource: progressDetail.completionSource,
              latestUpdatedAt: progressDetail.logs[progressDetail.logs.length - 1]?.createdAt,
              requestedAt: progressDetail.requestedAt,
              confirmedAt: progressDetail.confirmedAt,
            })"
          >
            直接登记完成
          </el-button>
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
              completionSource: progressDetail.completionSource,
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

  <el-dialog v-model="classDialog" title="实验课程签到" width="980px" @closed="stopLoops">
    <div v-if="selectedCell" class="meta" style="margin-bottom: 10px">
      {{ selectedCell.courseName || '（未命名课程）' }} / {{ selectedCell.className }} / {{ selectedCell.lessonDate }} / {{ selectedCell.slotName }}
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
        <el-button size="small" type="primary" plain @click="exportAttendanceExcel" :disabled="!session">导出签到 Excel</el-button>
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
      <el-tab-pane label="签到名单">
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
.dialogGridTwo {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.mobileFormGrid {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 12px;
}
.desktopRosterEditor {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.mobileRosterEditor {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 10px;
}
.desktopSectionHeader {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}
.mobileSectionHeader {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
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
:root[data-ui='mobile'] .groupBar {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 12px;
}
</style>
