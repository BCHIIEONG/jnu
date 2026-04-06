<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { apiData } from '../../api/http'
import { useAuthStore } from '../../stores/auth'

type DiscussionMessageVO = {
  id: number
  threadId: number
  authorId: number
  authorUsername: string
  authorDisplayName: string
  authorRole: 'STUDENT' | 'TEACHER' | 'ADMIN'
  content: string
  createdAt: string
}

type DiscussionThreadVO = {
  id: number
  taskId: number
  taskTitle: string
  type: 'NORMAL' | 'ASK_TEACHER'
  creatorId: number
  creatorUsername: string
  creatorDisplayName: string
  latestMessagePreview?: string | null
  latestMessageAt?: string | null
  latestTeacherReplyPreview?: string | null
  latestTeacherReplyAt?: string | null
  unreadCount: number
  messages: DiscussionMessageVO[]
}

type DiscussionTaskViewVO = {
  teacherQaThreads: DiscussionThreadVO[]
  normalThreads: DiscussionThreadVO[]
  unreadTeacherReplyCount: number
}

const props = defineProps<{
  taskId: number | null
  mode: 'student' | 'teacher'
  highlightThreadId?: number | null
  pollingEnabled?: boolean
}>()

const emit = defineEmits<{
  changed: []
}>()

const auth = useAuthStore()

const loading = ref(false)
const refreshing = ref(false)
const posting = ref(false)
const discussion = ref<DiscussionTaskViewVO | null>(null)
const newThreadType = ref<'NORMAL' | 'ASK_TEACHER'>('ASK_TEACHER')
const newThreadContent = ref('')
const expandedThreadIds = ref<number[]>([])
const replyDrafts = ref<Record<number, string>>({})
const hasLoadedOnce = ref(false)
let discussionRefreshTimer: number | null = null

const isStudentMode = computed(() => props.mode === 'student')
const teacherThreads = computed(() => discussion.value?.teacherQaThreads ?? [])
const normalThreads = computed(() => discussion.value?.normalThreads ?? [])
const allThreadsEmpty = computed(() => teacherThreads.value.length === 0 && normalThreads.value.length === 0)

function endpointBase() {
  if (!props.taskId) return ''
  return props.mode === 'student'
    ? `/api/tasks/${props.taskId}/discussion`
    : `/api/teacher/tasks/${props.taskId}/discussion`
}

function threadReplyPlaceholder(thread: DiscussionThreadVO) {
  return thread.type === 'ASK_TEACHER' ? '继续补充问题或回复老师' : '补充讨论内容'
}

function threadSummary(thread: DiscussionThreadVO) {
  if (thread.type === 'ASK_TEACHER' && thread.latestTeacherReplyPreview) {
    return `老师回复：${thread.latestTeacherReplyPreview}`
  }
  return thread.latestMessagePreview || '暂无内容'
}

function isExpanded(threadId: number) {
  return expandedThreadIds.value.includes(threadId)
}

function isTeacherReply(message: DiscussionMessageVO) {
  return message.authorRole === 'TEACHER' || message.authorRole === 'ADMIN'
}

function isThreadUnread(thread: DiscussionThreadVO) {
  return (thread.unreadCount || 0) > 0
}

async function loadDiscussion() {
  if (!props.taskId) {
    discussion.value = null
    expandedThreadIds.value = []
    replyDrafts.value = {}
    hasLoadedOnce.value = false
    return
  }
  loading.value = !hasLoadedOnce.value
  refreshing.value = hasLoadedOnce.value
  try {
    discussion.value = await apiData<DiscussionTaskViewVO>(endpointBase(), { method: 'GET' }, auth.token)
    hasLoadedOnce.value = true
  } catch (e: any) {
    if (!hasLoadedOnce.value) {
      discussion.value = null
    }
    ElMessage.error(e?.message ?? '加载讨论区失败')
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

type ViewportSnapshot = {
  scrollTop: number
  atBottom: boolean
}

function captureViewportSnapshot(): ViewportSnapshot {
  const doc = document.documentElement
  const scrollTop = window.scrollY || doc.scrollTop || 0
  const viewportBottom = scrollTop + window.innerHeight
  const pageHeight = Math.max(doc.scrollHeight, document.body.scrollHeight)
  return {
    scrollTop,
    atBottom: pageHeight - viewportBottom <= 24,
  }
}

async function restoreViewportSnapshot(snapshot: ViewportSnapshot) {
  await nextTick()
  await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()))
  const doc = document.documentElement
  const pageHeight = Math.max(doc.scrollHeight, document.body.scrollHeight)
  const targetTop = snapshot.atBottom
    ? Math.max(0, pageHeight - window.innerHeight)
    : snapshot.scrollTop
  window.scrollTo({ top: targetTop, behavior: 'auto' })
}

async function refreshDiscussion(notify = false, preserveScroll = true) {
  const snapshot = preserveScroll ? captureViewportSnapshot() : null
  await loadDiscussion()
  if (snapshot) {
    await restoreViewportSnapshot(snapshot)
  }
  if (notify) {
    emit('changed')
  }
}

async function createThread() {
  if (!props.taskId) return
  const content = newThreadContent.value.trim()
  if (!content) {
    ElMessage.warning('请输入讨论内容')
    return
  }
  posting.value = true
  try {
    await apiData(
      `${endpointBase()}/threads`,
      { method: 'POST', body: { type: newThreadType.value, content } },
      auth.token,
    )
    newThreadContent.value = ''
    await refreshDiscussion()
    emit('changed')
    ElMessage.success(newThreadType.value === 'ASK_TEACHER' ? '提问已发布' : '讨论已发布')
  } catch (e: any) {
    ElMessage.error(e?.message ?? '发布失败')
  } finally {
    posting.value = false
  }
}

async function markThreadRead(threadId: number) {
  if (!props.taskId) return
  try {
    await apiData(`${endpointBase()}/threads/${threadId}/read`, { method: 'POST' }, auth.token)
    emit('changed')
  } catch {
    // keep UI responsive, errors are non-critical here
  }
}

async function toggleThread(thread: DiscussionThreadVO) {
  if (isExpanded(thread.id)) {
    expandedThreadIds.value = expandedThreadIds.value.filter((id) => id !== thread.id)
    return
  }
  expandedThreadIds.value = [...expandedThreadIds.value, thread.id]
  if (thread.unreadCount > 0) {
    await markThreadRead(thread.id)
    await refreshDiscussion()
  }
}

async function replyThread(thread: DiscussionThreadVO) {
  if (!props.taskId) return
  const content = (replyDrafts.value[thread.id] || '').trim()
  if (!content) {
    ElMessage.warning('请输入回复内容')
    return
  }
  posting.value = true
  try {
    await apiData(
      `${endpointBase()}/threads/${thread.id}/messages`,
      { method: 'POST', body: { content } },
      auth.token,
    )
    replyDrafts.value = { ...replyDrafts.value, [thread.id]: '' }
    if (!isExpanded(thread.id)) {
      expandedThreadIds.value = [...expandedThreadIds.value, thread.id]
    }
    await refreshDiscussion()
    emit('changed')
    ElMessage.success('回复成功')
  } catch (e: any) {
    ElMessage.error(e?.message ?? '回复失败')
  } finally {
    posting.value = false
  }
}

async function focusThread(threadId: number | null | undefined) {
  if (!threadId || !props.taskId) return
  await nextTick()
  if (!expandedThreadIds.value.includes(threadId)) {
    expandedThreadIds.value = [...expandedThreadIds.value, threadId]
  }
  const thread = [...teacherThreads.value, ...normalThreads.value].find((item) => item.id === threadId)
  if (thread && thread.unreadCount > 0) {
    await markThreadRead(threadId)
    await refreshDiscussion()
  }
}

function stopDiscussionPolling() {
  if (discussionRefreshTimer) {
    window.clearInterval(discussionRefreshTimer)
  }
  discussionRefreshTimer = null
}

function startDiscussionPolling() {
  stopDiscussionPolling()
  if (!props.pollingEnabled || !props.taskId) return
  discussionRefreshTimer = window.setInterval(() => {
    refreshDiscussion(false, true)
  }, 16000)
}

watch(
  () => [props.taskId, props.pollingEnabled],
  async () => {
    await refreshDiscussion(false, false)
    await focusThread(props.highlightThreadId)
    startDiscussionPolling()
  },
  { immediate: true },
)

watch(
  () => props.highlightThreadId,
  async (threadId) => {
    await focusThread(threadId)
  },
)

onBeforeUnmount(() => {
  stopDiscussionPolling()
})

defineExpose({ loadDiscussion })
</script>

<template>
  <div class="discussionPanel">
    <div class="discussionUtilityRow">
      <el-button size="small" :loading="loading || refreshing" @click="refreshDiscussion(true, true)">刷新</el-button>
    </div>
    <el-card v-if="isStudentMode" shadow="never" class="discussionComposer">
      <template #header>
        <div class="discussionHeaderRow">
          <div>发起讨论</div>
          <div class="meta">向老师提问会进入教师问答聚合提醒；普通讨论不会提醒老师。</div>
        </div>
      </template>
      <div class="discussionTypeRow">
        <el-radio-group v-model="newThreadType" size="small">
          <el-radio-button label="ASK_TEACHER">向老师提问</el-radio-button>
          <el-radio-button label="NORMAL">普通讨论</el-radio-button>
        </el-radio-group>
      </div>
      <el-input
        v-model="newThreadContent"
        type="textarea"
        :rows="4"
        :placeholder="newThreadType === 'ASK_TEACHER' ? '描述你的问题，老师回复后会给你红点提醒' : '写下你的讨论内容'"
      />
      <div class="discussionActions">
        <el-button type="primary" :loading="posting" @click="createThread">发布</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="discussionBlock">
      <template #header>
        <div class="discussionHeaderRow">
          <div>教师问答</div>
          <el-tag v-if="isStudentMode && (discussion?.unreadTeacherReplyCount || 0) > 0" size="small" type="danger">
            {{ discussion?.unreadTeacherReplyCount }} 条老师回复未读
          </el-tag>
        </div>
      </template>
      <div v-if="loading && !hasLoadedOnce" class="meta">加载中...</div>
      <div v-else-if="teacherThreads.length === 0" class="meta">暂无教师问答</div>
      <div v-else class="threadList">
        <el-card v-for="thread in teacherThreads" :key="thread.id" shadow="never" class="threadCard" :id="`discussion-thread-${thread.id}`">
          <div class="threadTop">
            <div>
              <div class="threadTitle">
                <span>{{ thread.creatorDisplayName }} / {{ thread.creatorUsername }}</span>
                <el-tag v-if="isThreadUnread(thread)" size="small" type="danger">{{ thread.unreadCount }}</el-tag>
              </div>
              <div class="meta">最新时间：{{ thread.latestMessageAt || '-' }}</div>
            </div>
            <el-button size="small" text @click="toggleThread(thread)">{{ isExpanded(thread.id) ? '收起' : '展开' }}</el-button>
          </div>
          <div class="threadSummary">{{ threadSummary(thread) }}</div>
          <div v-if="isExpanded(thread.id)" class="threadMessages">
            <div v-for="message in thread.messages" :key="message.id" class="threadMessage" :class="{ teacher: isTeacherReply(message) }">
              <div class="threadMeta">
                <span>{{ message.authorDisplayName }} / {{ message.authorUsername }}</span>
                <span>{{ message.createdAt }}</span>
              </div>
              <div class="threadContent">{{ message.content }}</div>
            </div>
            <div class="replyBox">
              <el-input
                v-model="replyDrafts[thread.id]"
                type="textarea"
                :rows="3"
                :placeholder="threadReplyPlaceholder(thread)"
              />
              <div class="discussionActions">
                <el-button type="primary" size="small" :loading="posting" @click="replyThread(thread)">回复</el-button>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </el-card>

    <el-card shadow="never" class="discussionBlock">
      <template #header>
        <div>问题讨论区</div>
      </template>
      <div v-if="loading && !hasLoadedOnce" class="meta">加载中...</div>
      <div v-else-if="normalThreads.length === 0" class="meta">暂无普通讨论</div>
      <div v-else class="threadList">
        <el-card v-for="thread in normalThreads" :key="thread.id" shadow="never" class="threadCard">
          <div class="threadTop">
            <div>
              <div class="threadTitle">{{ thread.creatorDisplayName }} / {{ thread.creatorUsername }}</div>
              <div class="meta">最新时间：{{ thread.latestMessageAt || '-' }}</div>
            </div>
            <el-button size="small" text @click="toggleThread(thread)">{{ isExpanded(thread.id) ? '收起' : '展开' }}</el-button>
          </div>
          <div class="threadSummary">{{ threadSummary(thread) }}</div>
          <div v-if="isExpanded(thread.id)" class="threadMessages">
            <div v-for="message in thread.messages" :key="message.id" class="threadMessage" :class="{ teacher: isTeacherReply(message) }">
              <div class="threadMeta">
                <span>{{ message.authorDisplayName }} / {{ message.authorUsername }}</span>
                <span>{{ message.createdAt }}</span>
              </div>
              <div class="threadContent">{{ message.content }}</div>
            </div>
            <div class="replyBox">
              <el-input
                v-model="replyDrafts[thread.id]"
                type="textarea"
                :rows="3"
                placeholder="继续补充讨论内容"
              />
              <div class="discussionActions">
                <el-button type="primary" size="small" :loading="posting" @click="replyThread(thread)">回复</el-button>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </el-card>

    <div v-if="!loading && !refreshing && allThreadsEmpty && !isStudentMode" class="meta">当前任务还没有任何讨论</div>
  </div>
</template>

<style scoped>
.discussionPanel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.discussionComposer,
.discussionBlock {
  border-radius: 14px;
}
.discussionUtilityRow {
  display: flex;
  justify-content: flex-end;
}
.discussionHeaderRow {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.discussionTypeRow {
  margin-bottom: 10px;
}
.discussionActions {
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
}
.threadList {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.threadCard {
  background: #fafcff;
}
.threadTop {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}
.threadTitle {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  flex-wrap: wrap;
}
.threadSummary {
  margin-top: 8px;
  white-space: pre-wrap;
  word-break: break-word;
  color: #303133;
}
.threadMessages {
  margin-top: 12px;
  border-top: 1px solid #ebeef5;
  padding-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.threadMessage {
  padding: 10px 12px;
  border-radius: 12px;
  background: #f5f7fa;
}
.threadMessage.teacher {
  background: #ecf5ff;
}
.threadMeta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 12px;
  color: #666;
  margin-bottom: 6px;
}
.threadContent {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
}
.replyBox {
  padding-top: 4px;
}
.meta {
  color: #666;
  font-size: 12px;
}
@media (max-width: 768px) {
  .threadTop,
  .discussionHeaderRow {
    align-items: stretch;
  }
  .discussionActions {
    justify-content: stretch;
  }
  .discussionActions :deep(.el-button) {
    width: 100%;
  }
}
</style>
