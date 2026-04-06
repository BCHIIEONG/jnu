<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiData, downloadBlob, fetchBlob, uploadFormData } from '../../api/http'
import { useAuthStore } from '../../stores/auth'
import UiModeToggle from '../common/UiModeToggle.vue'
import { useUiStore } from '../../stores/ui'
import TaskDiscussionPanel from '../common/TaskDiscussionPanel.vue'

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

type TaskDiscussionUnreadItemVO = {
  taskId: number
  unreadCount: number
}

type StudentDiscussionUnreadSummaryVO = {
  totalUnreadCount: number
  items: TaskDiscussionUnreadItemVO[]
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
  status: string
  note?: string | null
  createdAt: string
  approvedAt?: string | null
  checkoutAt?: string | null
  returnAt?: string | null
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

const reviewIssueLabelMap: Record<string, string> = {
  FORMAT: '格式不规范',
  STEPS: '实验步骤不完整',
  ANALYSIS: '结果分析不足',
  CONCLUSION: '结论不清晰',
  DATA: '数据异常',
  CHART: '图表缺失或错误',
  CODE: '代码/原理错误',
  PLAGIARISM: '抄袭疑似',
  OTHER: '其他',
}

type Semester = { id: number; name: string; startDate?: string | null; endDate?: string | null }
type TimeSlot = { id: number; code: string; name: string; startTime: string; endTime: string }
type WeekScheduleItem = {
  id: number
  sourceType?: 'CLASS_SCHEDULE' | 'EXPERIMENT_COURSE'
  experimentCourseId?: number | null
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
  instances: {
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
  }[]
}
type StudentExperimentCourseVO = {
  id: number
  title: string
  description?: string | null
  teacherId: number
  teacherDisplayName?: string | null
  semesterId: number
  semesterName?: string | null
  status: 'OPEN' | 'CLOSED'
  enrollDeadlineAt: string
  enrolled: boolean
  blocked: boolean
  blockedReason?: string | null
  selectedSlotId?: number | null
  selectedAt?: string | null
  slots: ExperimentCourseSlotVO[]
}

const auth = useAuthStore()
const ui = useUiStore()
const router = useRouter()

const isMobile = computed(() => ui.effectiveMode === 'mobile')
const activeTab = ref<'tasks' | 'discussion' | 'courses' | 'schedule'>('tasks')
const mobilePage = ref<'list' | 'task' | 'discussion' | 'courses' | 'schedule'>('list')

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
const nextProgressStepNo = computed(() => {
  const max = progressLogs.value.reduce((m, item) => Math.max(m, item.stepNo || 0), 0)
  return max + 1
})

const previewDialog = ref(false)
const previewUrl = ref<string | null>(null)
const previewKind = ref<'image' | 'text'>('image')
const previewText = ref<string>('')
const previewTitle = ref('')
const progressLogs = ref<TaskProgressVO[]>([])
const loadingProgress = ref(false)
const progressContent = ref('')
const progressSubmitting = ref(false)
const progressFiles = ref<File[]>([])
const progressFileInput = ref<HTMLInputElement | null>(null)
const progressImageUrls = ref<Record<number, string>>({})
const completionInfo = ref<TaskCompletionVO | null>(null)
const completionLoading = ref(false)
const completionSubmitting = ref(false)
const taskDevices = ref<TaskDeviceConfigVO[]>([])
const loadingTaskDevices = ref(false)
const deviceRequests = ref<TaskDeviceRequestVO[]>([])
const loadingDeviceRequests = ref(false)
const deviceSubmitting = ref(false)
const deviceForm = ref({
  deviceId: undefined as number | undefined,
  quantity: 1,
  note: '',
})

const semesters = ref<Semester[]>([])
const timeSlots = ref<TimeSlot[]>([])
const availableCourses = ref<StudentExperimentCourseVO[]>([])
const myCourses = ref<StudentExperimentCourseVO[]>([])
const loadingCourses = ref(false)
const enrollingCourseId = ref<number | null>(null)
const discussionUnreadByTaskId = ref<Record<number, number>>({})
const discussionUnreadTotal = ref(0)
let discussionUnreadTimer: number | null = null
let discussionPageUnreadTimer: number | null = null
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

function formatWeekdayLabel(date: string): string {
  const parsed = new Date(`${date}T00:00:00`)
  const labels = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return labels[parsed.getDay()] ?? date
}

function experimentCourseSlotSummary(slot: ExperimentCourseSlotVO, index = 0): string {
  const baseName = slot.name?.trim() || `场次${index + 1}`
  if (slot.mode === 'SINGLE') {
    return `${baseName} / 单次课`
  }
  const repeatLabel = slot.repeatPattern === 'ODD_WEEK' ? '单周' : '每周'
  const rangeLabel = slot.rangeMode === 'SEMESTER' ? '整学期' : `${slot.rangeStartDate} ~ ${slot.rangeEndDate}`
  return `${baseName} / 多次课 / ${repeatLabel} / ${rangeLabel}`
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

const scheduleEmptyHint = computed(
  () => !loadingWeek.value && !scheduleError.value && timeSlots.value.length > 0 && weekItems.value.length === 0,
)

function completionStatusText(status?: TaskCompletionVO['status']) {
  if (status === 'PENDING_CONFIRM') return '待教师确认'
  if (status === 'CONFIRMED') return '已确认'
  return '未登记'
}

function completionStatusType(status?: TaskCompletionVO['status']) {
  if (status === 'PENDING_CONFIRM') return 'warning'
  if (status === 'CONFIRMED') return 'success'
  return 'info'
}

function completionSourceText(source?: TaskCompletionVO['completionSource']) {
  if (source === 'TEACHER_DIRECT') return '教师直接登记完成'
  if (source === 'STUDENT_REQUEST') return '学生申请完成'
  return '未登记'
}

function taskDiscussionUnreadCount(taskId: number) {
  return discussionUnreadByTaskId.value[taskId] ?? 0
}

async function loadDiscussionUnreadSummary() {
  try {
    const summary = await apiData<StudentDiscussionUnreadSummaryVO>(
      '/api/tasks/discussion/unread-summary',
      { method: 'GET' },
      auth.token,
    )
    const nextMap: Record<number, number> = {}
    for (const item of summary.items || []) {
      nextMap[item.taskId] = item.unreadCount
    }
    discussionUnreadByTaskId.value = nextMap
    discussionUnreadTotal.value = summary.totalUnreadCount ?? 0
  } catch {
    discussionUnreadByTaskId.value = {}
    discussionUnreadTotal.value = 0
  }
}

function startDiscussionUnreadPolling() {
  stopDiscussionUnreadPolling()
  discussionUnreadTimer = window.setInterval(() => {
    if (isDiscussionPageOpen()) return
    loadDiscussionUnreadSummary()
  }, 30000)
}

function stopDiscussionUnreadPolling() {
  if (discussionUnreadTimer) {
    window.clearInterval(discussionUnreadTimer)
  }
  discussionUnreadTimer = null
}

function isDiscussionPageOpen() {
  return activeTab.value === 'discussion' || mobilePage.value === 'discussion'
}

function stopDiscussionPageUnreadPolling() {
  if (discussionPageUnreadTimer) {
    window.clearInterval(discussionPageUnreadTimer)
  }
  discussionPageUnreadTimer = null
}

function startDiscussionPageUnreadPolling() {
  stopDiscussionPageUnreadPolling()
  if (!isDiscussionPageOpen()) return
  discussionPageUnreadTimer = window.setInterval(() => {
    loadDiscussionUnreadSummary()
  }, 16000)
}

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

async function loadExperimentCourses() {
  loadingCourses.value = true
  try {
    const [available, mine] = await Promise.all([
      apiData<StudentExperimentCourseVO[]>('/api/student/experiment-courses', { method: 'GET' }, auth.token),
      apiData<StudentExperimentCourseVO[]>('/api/student/experiment-courses/my', { method: 'GET' }, auth.token),
    ])
    availableCourses.value = available
    myCourses.value = mine
  } catch (e: any) {
    availableCourses.value = []
    myCourses.value = []
    ElMessage.error(e?.message ?? '加载实验课程失败')
  } finally {
    loadingCourses.value = false
  }
}

async function enrollExperimentCourse(courseId: number, slotId: number) {
  enrollingCourseId.value = courseId
  try {
    await apiData(
      `/api/student/experiment-courses/${courseId}/enroll`,
      { method: 'POST', body: { slotId } },
      auth.token,
    )
    ElMessage.success('选课成功')
    await Promise.all([loadExperimentCourses(), loadTasks(), loadWeek()])
  } catch (e: any) {
    ElMessage.error(e?.message ?? '选课失败')
  } finally {
    enrollingCourseId.value = null
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
    scheduleSemesterId.value = pickDefaultSemesterId(semesterList)
    alignWeekStartToSemester(scheduleSemesterId.value)
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
  await Promise.all([loadMySubmissions(), loadFlowData()])
}

async function openTaskMobile(taskId: number) {
  await selectTask(taskId)
  mobilePage.value = 'task'
}

async function openDiscussionTab() {
  if (!selectedTaskId.value && tasks.value.length > 0) {
    await selectTask(tasks.value[0]!.id)
  }
  activeTab.value = 'discussion'
  await loadDiscussionUnreadSummary()
}

async function openDiscussionMobile() {
  if (!selectedTaskId.value) return
  mobilePage.value = 'discussion'
  await loadDiscussionUnreadSummary()
}

async function openScheduleMobile() {
  mobilePage.value = 'schedule'
  if (!semesters.value.length || !timeSlots.value.length) {
    await loadScheduleMeta()
  }
  await loadWeek()
}

async function openCoursesMobile() {
  mobilePage.value = 'courses'
  await loadExperimentCourses()
}

function backToListMobile() {
  revokeProgressImageUrls()
  mobilePage.value = 'list'
  selectedTaskId.value = null
  taskDetail.value = null
  mySubmissions.value = []
  progressLogs.value = []
  completionInfo.value = null
  taskDevices.value = []
  deviceRequests.value = []
}

function backToTaskMobile() {
  mobilePage.value = 'task'
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

async function ensureProgressImageUrls(logs: TaskProgressVO[]) {
  revokeProgressImageUrls()
  for (const log of logs) {
    for (const att of log.attachments || []) {
      if (!isImageLike(att.fileName, att.contentType)) continue
      try {
        const { blob } = await fetchBlob(`/api/progress-attachments/${att.id}/download`, { token: auth.token })
        progressImageUrls.value[att.id] = URL.createObjectURL(blob)
      } catch {
        // ignore broken preview and keep download available
      }
    }
  }
}

async function loadProgressLogs() {
  if (!selectedTaskId.value) return
  loadingProgress.value = true
  try {
    progressLogs.value = await apiData<TaskProgressVO[]>(`/api/tasks/${selectedTaskId.value}/progress/me`, { method: 'GET' }, auth.token)
    await ensureProgressImageUrls(progressLogs.value)
  } catch (e: any) {
    progressLogs.value = []
    revokeProgressImageUrls()
    ElMessage.error(e?.message ?? '加载实验步骤失败')
  } finally {
    loadingProgress.value = false
  }
}

async function loadCompletionInfo() {
  if (!selectedTaskId.value) return
  completionLoading.value = true
  try {
    completionInfo.value = await apiData<TaskCompletionVO>(`/api/tasks/${selectedTaskId.value}/completion/me`, { method: 'GET' }, auth.token)
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载登记状态失败')
  } finally {
    completionLoading.value = false
  }
}

async function loadTaskDevices() {
  if (!selectedTaskId.value) return
  loadingTaskDevices.value = true
  try {
    taskDevices.value = await apiData<TaskDeviceConfigVO[]>(`/api/tasks/${selectedTaskId.value}/devices`, { method: 'GET' }, auth.token)
    if (!taskDevices.value.find((d) => d.deviceId === deviceForm.value.deviceId)) {
      const first = taskDevices.value.find((d) => d.availableQuantity > 0)
      deviceForm.value.deviceId = first?.deviceId
      deviceForm.value.quantity = 1
    }
  } catch (e: any) {
    taskDevices.value = []
    ElMessage.error(e?.message ?? '加载设备列表失败')
  } finally {
    loadingTaskDevices.value = false
  }
}

async function loadDeviceRequests() {
  if (!selectedTaskId.value) return
  loadingDeviceRequests.value = true
  try {
    deviceRequests.value = await apiData<TaskDeviceRequestVO[]>(`/api/tasks/${selectedTaskId.value}/device-requests/me`, { method: 'GET' }, auth.token)
  } catch (e: any) {
    deviceRequests.value = []
    ElMessage.error(e?.message ?? '加载设备申请记录失败')
  } finally {
    loadingDeviceRequests.value = false
  }
}

async function loadFlowData() {
  await Promise.all([loadProgressLogs(), loadCompletionInfo(), loadTaskDevices(), loadDeviceRequests()])
}

function onPickProgressFiles(ev: Event) {
  const input = ev.target as HTMLInputElement
  progressFiles.value = Array.from(input.files ?? [])
  progressFileInput.value = input
}

async function submitProgress() {
  if (!selectedTaskId.value) return
  if (!progressContent.value.trim() && progressFiles.value.length === 0) {
    ElMessage.warning('请填写步骤说明或上传附件')
    return
  }
  progressSubmitting.value = true
  try {
    const fd = new FormData()
    fd.append('content', progressContent.value ?? '')
    for (const f of progressFiles.value) fd.append('files', f)
    await uploadFormData(`/api/tasks/${selectedTaskId.value}/progress`, { token: auth.token, formData: fd })
    ElMessage.success('步骤记录成功')
    progressContent.value = ''
    progressFiles.value = []
    if (progressFileInput.value) progressFileInput.value.value = ''
    await loadProgressLogs()
    await loadCompletionInfo()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '步骤提交失败')
  } finally {
    progressSubmitting.value = false
  }
}

async function submitCompletionRequest() {
  if (!selectedTaskId.value) return
  completionSubmitting.value = true
  try {
    await apiData(`/api/tasks/${selectedTaskId.value}/completion`, { method: 'POST' }, auth.token)
    ElMessage.success('已提交完成登记，等待教师确认')
    await loadCompletionInfo()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '提交登记失败')
  } finally {
    completionSubmitting.value = false
  }
}

async function submitDeviceRequest() {
  if (!selectedTaskId.value) return
  if (!deviceForm.value.deviceId) {
    ElMessage.warning('请选择设备')
    return
  }
  if (!deviceForm.value.quantity || deviceForm.value.quantity < 1) {
    ElMessage.warning('数量至少为 1')
    return
  }
  deviceSubmitting.value = true
  try {
    await apiData(
      `/api/tasks/${selectedTaskId.value}/device-requests`,
      {
        method: 'POST',
        body: {
          deviceId: deviceForm.value.deviceId,
          quantity: deviceForm.value.quantity,
          note: deviceForm.value.note || null,
        },
      },
      auth.token,
    )
    ElMessage.success('设备申请已提交')
    deviceForm.value.quantity = 1
    deviceForm.value.note = ''
    await loadTaskDevices()
    await loadDeviceRequests()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '设备申请失败')
  } finally {
    deviceSubmitting.value = false
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

function logout() {
  auth.logout()
  router.replace('/login')
}

watch(
  () => activeTab.value,
  async (tab) => {
    if (tab === 'courses') {
      await loadExperimentCourses()
      stopDiscussionPageUnreadPolling()
      return
    }
    if (tab === 'discussion') {
      if (!selectedTaskId.value && tasks.value.length > 0) {
        await selectTask(tasks.value[0]!.id)
      }
      await loadDiscussionUnreadSummary()
      startDiscussionPageUnreadPolling()
      return
    }
    stopDiscussionPageUnreadPolling()
    if (tab !== 'schedule') return
    if (!semesters.value.length || !timeSlots.value.length) {
      await loadScheduleMeta()
    }
    alignWeekStartToSemester(scheduleSemesterId.value)
    await loadWeek()
  },
)

watch(
  () => scheduleSemesterId.value,
  (id) => {
    alignWeekStartToSemester(id)
  },
)

watch(
  () => mobilePage.value,
  async (page) => {
    if (page === 'discussion') {
      await loadDiscussionUnreadSummary()
      startDiscussionPageUnreadPolling()
      return
    }
    stopDiscussionPageUnreadPolling()
  },
)

onMounted(async () => {
  await loadTasks()
  await loadExperimentCourses()
  await loadScheduleMeta()
  await loadDiscussionUnreadSummary()
  startDiscussionUnreadPolling()
})

onBeforeUnmount(() => {
  stopDiscussionUnreadPolling()
  stopDiscussionPageUnreadPolling()
  revokeProgressImageUrls()
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
          <div style="display: flex; gap: 8px">
            <el-button size="small" @click="openCoursesMobile">实验课程</el-button>
            <el-button size="small" @click="openScheduleMobile">我的课表</el-button>
          </div>
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
            <div style="display: flex; align-items: center; gap: 8px">
              <el-badge :value="taskDiscussionUnreadCount(t.id)" :hidden="taskDiscussionUnreadCount(t.id) <= 0" :max="99" />
              <el-tag size="small">{{ t.status ?? 'OPEN' }}</el-tag>
            </div>
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

        <el-card v-if="taskDetail" class="block" shadow="never">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center; gap: 10px">
              <div>任务附件/任务资料</div>
              <div class="meta">可下载老师布置的资料</div>
            </div>
          </template>
          <div v-if="(taskDetail.attachments || []).length === 0" class="meta">暂无任务附件</div>
          <div v-else class="progressAtts">
            <div v-for="att in taskDetail.attachments || []" :key="att.id" class="progressAtt">
              <div class="attName">{{ att.fileName }}</div>
              <div class="meta">大小：{{ att.fileSize }} Byte</div>
              <div class="meta">上传时间：{{ att.uploadedAt }}</div>
              <div style="display: flex; gap: 8px; margin-top: 8px; flex-wrap: wrap">
                <el-button size="small" @click="downloadTaskAttachment(att)">下载</el-button>
                <el-button size="small" @click="previewTaskAttachment(att)">预览</el-button>
              </div>
            </div>
          </div>
        </el-card>

        <el-card v-if="selectedTaskId" class="block" shadow="never">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center; gap: 10px; flex-wrap: wrap">
              <div>讨论区</div>
              <el-tag v-if="taskDiscussionUnreadCount(selectedTaskId) > 0" size="small" type="danger">
                {{ taskDiscussionUnreadCount(selectedTaskId) }} 条老师回复未读
              </el-tag>
            </div>
          </template>
          <div class="taskRow">
            <div class="meta">讨论区已拆成独立页面，避免和实验过程、报告提交通道混在一起。</div>
            <el-button type="primary" size="small" @click="openDiscussionMobile">进入讨论区</el-button>
          </div>
        </el-card>

          <el-collapse accordion class="block">
            <el-collapse-item :title="`实验步骤（下一步：${nextProgressStepNo}）`" name="progress">
              <el-card shadow="never" class="innerCard">
                <template #header>
                  <div style="display: flex; justify-content: space-between; align-items: center; gap: 10px">
                    <div>步骤 {{ nextProgressStepNo }}</div>
                    <el-button type="primary" size="small" :loading="progressSubmitting" @click="submitProgress">提交步骤</el-button>
                  </div>
                </template>
                <el-input
                  v-model="progressContent"
                  type="textarea"
                  :rows="6"
                  placeholder="记录本步骤的实验内容、现象、问题或处理过程"
                />
                <div style="margin-top: 10px">
                  <div class="meta" style="margin-bottom: 6px">步骤附件（可选，图片会直接显示）</div>
                  <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap">
                    <input type="file" multiple @change="onPickProgressFiles" />
                    <div class="meta" v-if="progressFiles.length > 0">已选择 {{ progressFiles.length }} 个文件</div>
                  </div>
                </div>
              </el-card>

              <div style="margin-top: 12px">
                <div v-if="loadingProgress" class="meta">加载中...</div>
                <div v-else-if="progressLogs.length === 0" class="meta">还没有实验步骤记录</div>
                <el-card v-else v-for="log in progressLogs" :key="log.id" shadow="never" class="progressCard">
                  <template #header>
                    <div class="taskRow">
                      <div class="progressTitle">步骤 {{ log.stepNo }}</div>
                      <div class="meta">{{ log.createdAt }}</div>
                    </div>
                  </template>
                  <div style="white-space: pre-wrap">{{ log.content || '（无文字说明）' }}</div>

                  <div v-if="log.attachments.length > 0" class="progressAtts">
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
            </el-collapse-item>

            <el-collapse-item title="完成登记" name="completion">
              <div class="taskRow" style="margin-bottom: 10px">
                <div class="meta">完成状态</div>
                <el-tag :type="completionStatusType(completionInfo?.status)">{{ completionStatusText(completionInfo?.status) }}</el-tag>
              </div>
              <div v-if="completionInfo?.status === 'CONFIRMED'" class="meta">登记来源：{{ completionSourceText(completionInfo?.completionSource) }}</div>
              <div v-if="completionInfo?.requestedAt" class="meta">申请时间：{{ completionInfo.requestedAt }}</div>
              <div v-if="completionInfo?.confirmedAt" class="meta">确认时间：{{ completionInfo.confirmedAt }}</div>
              <div v-if="completionInfo?.confirmedByDisplayName" class="meta">确认教师：{{ completionInfo.confirmedByDisplayName }}</div>
              <div style="margin-top: 12px">
                <el-button
                  type="primary"
                  :loading="completionSubmitting || completionLoading"
                  :disabled="completionInfo?.status !== 'NONE'"
                  @click="submitCompletionRequest"
                >
                  完成全部步骤并提交登记
                </el-button>
              </div>
            </el-collapse-item>

            <el-collapse-item title="设备申请" name="device">
              <el-card shadow="never" class="innerCard">
                <template #header>
                  <div>可申请设备</div>
                </template>
                <div v-if="loadingTaskDevices" class="meta">加载中...</div>
                <div v-else-if="taskDevices.length === 0" class="meta">当前任务未配置可借设备</div>
                <div v-else class="deviceGrid">
                  <div v-for="device in taskDevices" :key="device.deviceId" class="deviceBox">
                    <div class="progressTitle">{{ device.deviceName }}</div>
                    <div class="meta">编码：{{ device.deviceCode }}</div>
                    <div class="meta">任务上限：{{ device.configuredQuantity }}</div>
                    <div class="meta">总库存：{{ device.totalQuantity }}</div>
                    <div class="meta">已占用：{{ device.reservedQuantity }}</div>
                    <div class="meta">当前可申请：{{ device.availableQuantity }}</div>
                  </div>
                </div>
              </el-card>

              <el-card v-if="taskDevices.length > 0" shadow="never" class="innerCard" style="margin-top: 12px">
                <template #header>
                  <div style="display: flex; justify-content: space-between; align-items: center; gap: 10px">
                    <div>发起设备申请</div>
                    <el-button type="primary" size="small" :loading="deviceSubmitting" @click="submitDeviceRequest">提交申请</el-button>
                  </div>
                </template>
                <div class="deviceForm">
                  <el-select v-model="deviceForm.deviceId" placeholder="选择设备">
                    <el-option
                      v-for="device in taskDevices"
                      :key="device.deviceId"
                      :label="`${device.deviceName}（可申请 ${device.availableQuantity}）`"
                      :value="device.deviceId"
                    />
                  </el-select>
                  <el-input-number v-model="deviceForm.quantity" :min="1" :max="999" />
                  <el-input v-model="deviceForm.note" type="textarea" :rows="3" placeholder="申请说明（可选）" />
                </div>
              </el-card>

              <div style="margin-top: 12px">
                <div class="taskRow" style="margin-bottom: 10px">
                  <div>我的申请记录</div>
                  <el-button size="small" :loading="loadingDeviceRequests" @click="loadDeviceRequests">刷新</el-button>
                </div>
                <div v-if="loadingDeviceRequests" class="meta">加载中...</div>
                <div v-else-if="deviceRequests.length === 0" class="meta">暂无设备申请记录</div>
                <el-card v-else v-for="row in deviceRequests" :key="row.id" shadow="never" class="subCard">
                  <div class="taskRow">
                    <div class="progressTitle">{{ row.deviceName }}</div>
                    <el-tag size="small">{{ row.status }}</el-tag>
                  </div>
                  <div class="meta">数量：{{ row.quantity }}</div>
                  <div class="meta">申请时间：{{ row.createdAt }}</div>
                  <div v-if="row.note" class="meta">说明：{{ row.note }}</div>
                  <div v-if="row.approvedAt" class="meta">通过时间：{{ row.approvedAt }}</div>
                  <div v-if="row.checkoutAt" class="meta">借出时间：{{ row.checkoutAt }}</div>
                  <div v-if="row.returnAt" class="meta">归还时间：{{ row.returnAt }}</div>
                </el-card>
              </div>
            </el-collapse-item>

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

      <div v-else-if="mobilePage === 'courses'">
        <div class="mobileTop">
          <el-button size="small" @click="backToListMobile">返回任务列表</el-button>
          <el-button size="small" :loading="loadingCourses" @click="loadExperimentCourses">刷新课程</el-button>
        </div>
        <el-card class="block" shadow="never">
          <template #header><div>可选课程</div></template>
          <div v-if="loadingCourses" class="meta">加载中...</div>
          <div v-else-if="availableCourses.length === 0" class="meta">暂无可选实验课程</div>
          <el-card v-else v-for="course in availableCourses" :key="course.id" shadow="never" class="subCard">
            <div class="taskRow">
              <div class="taskName">{{ course.title }}</div>
              <el-tag size="small" :type="course.enrolled ? 'success' : course.status === 'OPEN' ? 'warning' : 'info'">
                {{ course.enrolled ? '已选' : course.status }}
              </el-tag>
            </div>
            <div class="meta" style="margin-top: 6px">教师：{{ course.teacherDisplayName || '-' }}</div>
            <div class="meta">截止：{{ course.enrollDeadlineAt }}</div>
            <div class="meta" v-if="course.description">{{ course.description }}</div>
            <div class="progressAtts" style="margin-top: 10px">
              <div v-for="(slot, slotIndex) in course.slots" :key="slot.id" class="progressAtt">
                <div class="attName">{{ experimentCourseSlotSummary(slot, slotIndex) }}</div>
                <div class="meta">首次上课：{{ slot.firstLessonDate }} {{ slot.slotName }} / {{ slot.labRoomName || '-' }}</div>
                <div class="meta">剩余名额：{{ slot.remainingCapacity }}</div>
                <div class="meta" v-for="instance in slot.instances.slice(0, 3)" :key="instance.id">
                  {{ instance.displayName }} / {{ instance.lessonDate }} / {{ formatWeekdayLabel(instance.lessonDate) }}
                </div>
                <div class="meta" v-if="slot.instances.length > 3">共 {{ slot.instances.length }} 次课</div>
                <el-button
                  size="small"
                  type="primary"
                  style="margin-top: 8px"
                  :disabled="course.enrolled || course.selectedSlotId === slot.id || course.status !== 'OPEN' || course.blocked"
                  :loading="enrollingCourseId === course.id"
                  @click="enrollExperimentCourse(course.id, slot.id)"
                >
                  {{
                    course.blocked
                      ? '暂不可选'
                      : course.selectedSlotId === slot.id
                        ? '已选本场次'
                        : '选择该场次'
                  }}
                </el-button>
                <div v-if="course.blockedReason" class="meta" style="margin-top: 6px; color: #c45656">
                  {{ course.blockedReason }}
                </div>
              </div>
            </div>
          </el-card>
        </el-card>

        <el-card class="block" shadow="never">
          <template #header><div>我的已选课程</div></template>
          <div v-if="loadingCourses" class="meta">加载中...</div>
          <div v-else-if="myCourses.length === 0" class="meta">暂无已选实验课程</div>
          <el-card v-else v-for="course in myCourses" :key="course.id" shadow="never" class="subCard">
            <div class="taskRow">
              <div class="taskName">{{ course.title }}</div>
              <el-tag size="small" type="success">已选</el-tag>
            </div>
            <div class="meta" style="margin-top: 6px">教师：{{ course.teacherDisplayName || '-' }}</div>
            <div class="meta">截止：{{ course.enrollDeadlineAt }}</div>
            <div v-for="slot in course.slots.filter((item) => item.id === course.selectedSlotId)" :key="slot.id" style="margin-top: 6px">
              <div class="meta">已选场次：{{ experimentCourseSlotSummary(slot) }}</div>
              <div class="meta">地点节次：{{ slot.labRoomName || '-' }} / {{ slot.slotName }}</div>
              <div class="meta" v-for="instance in slot.instances" :key="instance.id">
                {{ instance.displayName }} / {{ instance.lessonDate }} / {{ formatWeekdayLabel(instance.lessonDate) }}
              </div>
            </div>
          </el-card>
        </el-card>
      </div>

      <div v-else-if="mobilePage === 'discussion'">
        <div class="mobileTop">
          <el-button size="small" @click="backToTaskMobile">返回任务详情</el-button>
          <el-button size="small" @click="loadDiscussionUnreadSummary">刷新未读</el-button>
        </div>

        <el-card v-if="taskDetail" class="block" shadow="never">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center; gap: 10px; flex-wrap: wrap">
              <div style="font-weight: 700">{{ taskDetail.title }}</div>
              <el-tag v-if="selectedTaskId && taskDiscussionUnreadCount(selectedTaskId) > 0" size="small" type="danger">
                {{ taskDiscussionUnreadCount(selectedTaskId) }} 条老师回复未读
              </el-tag>
            </div>
          </template>
          <TaskDiscussionPanel
            :task-id="selectedTaskId"
            mode="student"
            :polling-enabled="mobilePage === 'discussion'"
            @changed="loadDiscussionUnreadSummary"
          />
        </el-card>
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
        <div v-else-if="scheduleEmptyHint" class="scheduleEmpty">当前周暂无课表，请切换周次或学期</div>

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
          <el-table-column label="讨论" width="90" align="center">
            <template #default="{ row }: { row: TaskVO }">
              <el-badge :value="taskDiscussionUnreadCount(row.id)" :hidden="taskDiscussionUnreadCount(row.id) <= 0" :max="99" />
            </template>
          </el-table-column>
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

            <el-card v-if="taskDetail" class="block" shadow="never">
              <template #header>
                <div style="display: flex; justify-content: space-between; align-items: center">
                  <div>任务附件/任务资料</div>
                  <div class="meta">可下载老师布置的资料</div>
                </div>
              </template>
              <template v-if="!isMobile">
                <el-table :data="taskDetail.attachments || []" size="small" empty-text="暂无任务附件">
                  <el-table-column prop="fileName" label="文件名" min-width="280" />
                  <el-table-column prop="fileSize" label="大小(Byte)" width="120" />
                  <el-table-column prop="uploadedAt" label="上传时间" min-width="180" />
                  <el-table-column label="操作" width="200">
                    <template #default="{ row }: { row: TaskAttachmentVO }">
                      <el-button size="small" @click="downloadTaskAttachment(row)">下载</el-button>
                      <el-button size="small" @click="previewTaskAttachment(row)">预览</el-button>
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
                  </div>
                </el-card>
              </template>
            </el-card>

            <el-card class="block" shadow="never" v-if="selectedTaskId">
              <template #header>
                <div style="display: flex; justify-content: space-between; align-items: center; gap: 10px; flex-wrap: wrap">
                  <div>讨论区</div>
                  <el-tag v-if="taskDiscussionUnreadCount(selectedTaskId) > 0" size="small" type="danger">
                    {{ taskDiscussionUnreadCount(selectedTaskId) }} 条老师回复未读
                  </el-tag>
                </div>
              </template>
              <div class="taskRow">
                <div class="meta">讨论区已拆成独立页面，避免和实验步骤、完成登记、提交报告混排。</div>
                <el-button type="primary" size="small" @click="openDiscussionTab">进入讨论区</el-button>
              </div>
            </el-card>

            <el-card class="block" shadow="never" v-if="selectedTaskId">
              <template #header>
                <div style="display: flex; justify-content: space-between; align-items: center">
                  <div>实验步骤</div>
                  <el-tag type="info">下一步：步骤 {{ nextProgressStepNo }}</el-tag>
                </div>
              </template>
              <div class="progressComposer">
                <el-input
                  v-model="progressContent"
                  type="textarea"
                  :rows="6"
                  placeholder="记录本步骤的实验内容、现象、问题或处理过程"
                />
                <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap">
                  <input type="file" multiple @change="onPickProgressFiles" />
                  <div class="meta" v-if="progressFiles.length > 0">已选择 {{ progressFiles.length }} 个文件</div>
                  <el-button type="primary" size="small" :loading="progressSubmitting" @click="submitProgress">提交步骤</el-button>
                </div>
              </div>

              <div style="margin-top: 12px">
                <div v-if="loadingProgress" class="meta">加载中...</div>
                <div v-else-if="progressLogs.length === 0" class="meta">还没有实验步骤记录</div>
                <el-card v-else v-for="log in progressLogs" :key="log.id" shadow="never" class="progressCard">
                  <template #header>
                    <div class="taskRow">
                      <div class="progressTitle">步骤 {{ log.stepNo }}</div>
                      <div class="meta">{{ log.createdAt }}</div>
                    </div>
                  </template>
                  <div style="white-space: pre-wrap">{{ log.content || '（无文字说明）' }}</div>
                  <div v-if="log.attachments.length > 0" class="progressAtts">
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
            </el-card>

            <el-card class="block" shadow="never" v-if="selectedTaskId">
              <template #header>
                <div style="display: flex; justify-content: space-between; align-items: center">
                  <div>完成登记</div>
                  <el-tag :type="completionStatusType(completionInfo?.status)">{{ completionStatusText(completionInfo?.status) }}</el-tag>
                </div>
              </template>
              <div class="meta">当前状态：{{ completionStatusText(completionInfo?.status) }}</div>
              <div v-if="completionInfo?.status === 'CONFIRMED'" class="meta">登记来源：{{ completionSourceText(completionInfo?.completionSource) }}</div>
              <div v-if="completionInfo?.requestedAt" class="meta">申请时间：{{ completionInfo.requestedAt }}</div>
              <div v-if="completionInfo?.confirmedAt" class="meta">确认时间：{{ completionInfo.confirmedAt }}</div>
              <div v-if="completionInfo?.confirmedByDisplayName" class="meta">确认教师：{{ completionInfo.confirmedByDisplayName }}</div>
              <div style="margin-top: 12px">
                <el-button
                  type="primary"
                  :loading="completionSubmitting || completionLoading"
                  :disabled="completionInfo?.status !== 'NONE'"
                  @click="submitCompletionRequest"
                >
                  完成全部步骤并提交登记
                </el-button>
              </div>
            </el-card>

            <el-card class="block" shadow="never" v-if="selectedTaskId">
              <template #header>
                <div style="display: flex; justify-content: space-between; align-items: center">
                  <div>设备申请</div>
                  <el-button size="small" :loading="loadingDeviceRequests" @click="loadDeviceRequests">刷新申请记录</el-button>
                </div>
              </template>
              <div v-if="loadingTaskDevices" class="meta">加载中...</div>
              <div v-else-if="taskDevices.length === 0" class="meta">当前任务未配置可借设备</div>
              <div v-else class="deviceGrid">
                <div v-for="device in taskDevices" :key="device.deviceId" class="deviceBox">
                  <div class="progressTitle">{{ device.deviceName }}</div>
                  <div class="meta">编码：{{ device.deviceCode }}</div>
                  <div class="meta">任务上限：{{ device.configuredQuantity }}</div>
                  <div class="meta">总库存：{{ device.totalQuantity }}</div>
                  <div class="meta">已占用：{{ device.reservedQuantity }}</div>
                  <div class="meta">当前可申请：{{ device.availableQuantity }}</div>
                </div>
              </div>

              <div v-if="taskDevices.length > 0" class="progressComposer" style="margin-top: 12px">
                <el-select v-model="deviceForm.deviceId" placeholder="选择设备" style="width: 320px">
                  <el-option
                    v-for="device in taskDevices"
                    :key="device.deviceId"
                    :label="`${device.deviceName}（可申请 ${device.availableQuantity}）`"
                    :value="device.deviceId"
                  />
                </el-select>
                <el-input-number v-model="deviceForm.quantity" :min="1" :max="999" />
                <el-input v-model="deviceForm.note" type="textarea" :rows="3" placeholder="申请说明（可选）" />
                <div>
                  <el-button type="primary" :loading="deviceSubmitting" @click="submitDeviceRequest">提交设备申请</el-button>
                </div>
              </div>

              <div style="margin-top: 12px">
                <el-table v-if="!isMobile" :data="deviceRequests" size="small" v-loading="loadingDeviceRequests">
                  <el-table-column prop="deviceName" label="设备" min-width="160" />
                  <el-table-column prop="quantity" label="数量" width="80" />
                  <el-table-column prop="status" label="状态" width="140" />
                  <el-table-column prop="createdAt" label="申请时间" min-width="180" />
                  <el-table-column label="记录">
                    <template #default="{ row }: { row: TaskDeviceRequestVO }">
                      <div v-if="row.note" class="meta">说明：{{ row.note }}</div>
                      <div v-if="row.approvedAt" class="meta">通过：{{ row.approvedAt }}</div>
                      <div v-if="row.checkoutAt" class="meta">借出：{{ row.checkoutAt }}</div>
                      <div v-if="row.returnAt" class="meta">归还：{{ row.returnAt }}</div>
                    </template>
                  </el-table-column>
                </el-table>
                <div v-else-if="deviceRequests.length === 0" class="meta">暂无设备申请记录</div>
                <el-card v-else v-for="row in deviceRequests" :key="row.id" shadow="never" class="subCard">
                  <div class="taskRow">
                    <div class="progressTitle">{{ row.deviceName }}</div>
                    <el-tag size="small">{{ row.status }}</el-tag>
                  </div>
                  <div class="meta">数量：{{ row.quantity }}</div>
                  <div class="meta">申请时间：{{ row.createdAt }}</div>
                  <div v-if="row.note" class="meta">说明：{{ row.note }}</div>
                  <div v-if="row.approvedAt" class="meta">通过时间：{{ row.approvedAt }}</div>
                  <div v-if="row.checkoutAt" class="meta">借出时间：{{ row.checkoutAt }}</div>
                  <div v-if="row.returnAt" class="meta">归还时间：{{ row.returnAt }}</div>
                </el-card>
              </div>
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

          <el-tab-pane name="discussion">
            <template #label>
              <el-badge :value="discussionUnreadTotal" :hidden="discussionUnreadTotal <= 0" :max="99">
                <span>任务讨论</span>
              </el-badge>
            </template>
            <el-card v-if="selectedTaskId && taskDetail" class="block" shadow="never">
              <template #header>
                <div style="display: flex; justify-content: space-between; align-items: center; gap: 10px; flex-wrap: wrap">
                  <div>{{ taskDetail.title }}</div>
                  <div style="display: flex; align-items: center; gap: 8px; flex-wrap: wrap">
                    <el-tag v-if="taskDiscussionUnreadCount(selectedTaskId) > 0" size="small" type="danger">
                      {{ taskDiscussionUnreadCount(selectedTaskId) }} 条老师回复未读
                    </el-tag>
                    <el-button size="small" @click="activeTab = 'tasks'">返回任务</el-button>
                  </div>
                </div>
              </template>
              <TaskDiscussionPanel
                :task-id="selectedTaskId"
                mode="student"
                :polling-enabled="activeTab === 'discussion'"
                @changed="loadDiscussionUnreadSummary"
              />
            </el-card>
            <el-card v-else class="block" shadow="never">
              <div class="meta">请先从左侧选择一个任务进入讨论区。</div>
            </el-card>
          </el-tab-pane>

          <el-tab-pane label="实验课程" name="courses">
            <el-card class="block" shadow="never">
              <template #header>
                <div style="display: flex; justify-content: space-between; align-items: center">
                  <div>可选课程</div>
                  <el-button size="small" :loading="loadingCourses" @click="loadExperimentCourses">刷新</el-button>
                </div>
              </template>
              <div v-if="loadingCourses" class="meta">加载中...</div>
              <div v-else-if="availableCourses.length === 0" class="meta">暂无可选实验课程</div>
              <div v-else class="deviceGrid">
                <div v-for="course in availableCourses" :key="course.id" class="deviceBox">
                  <div class="progressTitle">{{ course.title }}</div>
                  <div class="meta">教师：{{ course.teacherDisplayName || '-' }}</div>
                  <div class="meta">截止：{{ course.enrollDeadlineAt }}</div>
                  <div class="meta" v-if="course.description">{{ course.description }}</div>
                  <div v-for="(slot, slotIndex) in course.slots" :key="slot.id" class="progressAtt" style="margin-top: 10px">
                    <div class="attName">{{ experimentCourseSlotSummary(slot, slotIndex) }}</div>
                    <div class="meta">首次上课：{{ slot.firstLessonDate }} {{ slot.slotName }} / {{ slot.labRoomName || '-' }}</div>
                    <div class="meta">剩余名额：{{ slot.remainingCapacity }}</div>
                    <div class="meta" v-for="instance in slot.instances.slice(0, 3)" :key="instance.id">
                      {{ instance.displayName }} / {{ instance.lessonDate }} / {{ formatWeekdayLabel(instance.lessonDate) }}
                    </div>
                    <div class="meta" v-if="slot.instances.length > 3">共 {{ slot.instances.length }} 次课</div>
                    <el-button
                      size="small"
                      type="primary"
                      style="margin-top: 8px"
                      :disabled="course.enrolled || course.selectedSlotId === slot.id || course.status !== 'OPEN' || course.blocked"
                      :loading="enrollingCourseId === course.id"
                      @click="enrollExperimentCourse(course.id, slot.id)"
                    >
                      {{
                        course.blocked
                          ? '暂不可选'
                          : course.selectedSlotId === slot.id
                            ? '已选本场次'
                            : '选择该场次'
                      }}
                    </el-button>
                    <div v-if="course.blockedReason" class="meta" style="margin-top: 6px; color: #c45656">
                      {{ course.blockedReason }}
                    </div>
                  </div>
                </div>
              </div>
            </el-card>

            <el-card class="block" shadow="never">
              <template #header><div>我的已选课程</div></template>
              <div v-if="loadingCourses" class="meta">加载中...</div>
              <div v-else-if="myCourses.length === 0" class="meta">暂无已选实验课程</div>
              <div v-else class="deviceGrid">
                <div v-for="course in myCourses" :key="course.id" class="deviceBox">
                  <div class="progressTitle">{{ course.title }}</div>
                  <div class="meta">教师：{{ course.teacherDisplayName || '-' }}</div>
                  <div v-for="slot in course.slots.filter((item) => item.id === course.selectedSlotId)" :key="slot.id" style="margin-top: 6px">
                    <div class="meta">场次：{{ experimentCourseSlotSummary(slot) }}</div>
                    <div class="meta">地点节次：{{ slot.labRoomName || '-' }} / {{ slot.slotName }}</div>
                    <div class="meta" v-for="instance in slot.instances" :key="instance.id">
                      {{ instance.displayName }} / {{ instance.lessonDate }} / {{ formatWeekdayLabel(instance.lessonDate) }}
                    </div>
                  </div>
                </div>
              </div>
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
              <div v-else-if="scheduleEmptyHint" class="scheduleEmpty" style="margin-top: 12px">当前周暂无课表，请切换周次或学期</div>
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
                            <div class="meta">{{ itemAt(ts.id, d.date)!.sourceType === 'EXPERIMENT_COURSE' ? '实验课程' : '班级课表' }}</div>
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
      <div style="margin-top: 10px">
        问题标签：
        <template v-if="currentReview.issueTags && currentReview.issueTags.length">
          {{ currentReview.issueTags.map((item) => reviewIssueLabelMap[item] || item).join('、') }}
        </template>
        <template v-else>（无）</template>
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
.innerCard {
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
.progressComposer {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.progressCard {
  margin-bottom: 10px;
}
.progressTitle {
  font-weight: 700;
}
.progressAtts {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px;
  margin-top: 12px;
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
.deviceForm {
  display: flex;
  flex-direction: column;
  gap: 10px;
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
