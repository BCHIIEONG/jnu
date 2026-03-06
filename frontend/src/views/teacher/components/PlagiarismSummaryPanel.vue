<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { apiData, downloadBlob, fetchBlob } from '../../../api/http'
import { useAuthStore } from '../../../stores/auth'

type Fragment = { text: string; score: number }
type EvidenceItem =
  | { type: 'SUBMISSION_TEXT'; score: number; detail: { fragments: Fragment[] } }
  | { type: 'ATTACHMENT_TEXT'; score: number; detail: { attachmentId: number; fileName: string } }
  | {
      type: 'ATTACHMENT_IMAGE';
      score: number;
      detail: { attachmentIdA: number; attachmentIdB: number; fileNameA: string; fileNameB: string; similarity: number };
    }
  | { type: string; score: number; detail: any }

type TopMatchStudent = { id: number; username: string; displayName: string }
type SkippedAttachment = { fileName: string; contentType?: string | null; reason: string }

type PlagiarismSummary = {
  runId: number | null
  taskId: number
  maxScore: number
  topMatchStudent: TopMatchStudent | null
  imagesProcessed: number
  imagesSkipped: number
  textAttachmentsProcessed: number
  textAttachmentsSkipped: number
  evidence: EvidenceItem[]
  skippedAttachments: SkippedAttachment[]
}

type HistoryStudent = { id: number; username?: string | null; displayName?: string | null }
type HistoryTop = { id: number; username?: string | null; displayName?: string | null }
type HistoryVersion = {
  submissionId: number
  versionNo: number
  submittedAt?: string | null
  maxScore: number
  topMatchStudent: HistoryTop | null
  hasResult: boolean
}
type PlagiarismHistory = {
  runId: number | null
  taskId: number
  student: HistoryStudent
  currentVersionNo: number
  maxAcrossVersions: number
  maxEarlierVersions: number
  versions: HistoryVersion[]
}

const props = defineProps<{
  submissionId: number
  contentMd: string
  compact?: boolean
}>()

const auth = useAuthStore()

const loading = ref(false)
const running = ref(false)
const summary = ref<PlagiarismSummary | null>(null)
const historyLoading = ref(false)
const history = ref<PlagiarismHistory | null>(null)

const viewMode = ref<'plain' | 'highlight'>('highlight')

const compareDialog = ref(false)
const compareAUrl = ref<string | null>(null)
const compareBUrl = ref<string | null>(null)
const compareTitle = ref('')

function closeCompare() {
  if (compareAUrl.value) URL.revokeObjectURL(compareAUrl.value)
  if (compareBUrl.value) URL.revokeObjectURL(compareBUrl.value)
  compareAUrl.value = null
  compareBUrl.value = null
  compareTitle.value = ''
  compareDialog.value = false
}

const maxScorePercent = computed(() => {
  const s = summary.value?.maxScore ?? 0
  return Math.round(s * 1000) / 10
})

const historyMaxAcrossPercent = computed(() => {
  const s = history.value?.maxAcrossVersions ?? 0
  return Math.round(s * 1000) / 10
})

const historyMaxEarlierPercent = computed(() => {
  const s = history.value?.maxEarlierVersions ?? 0
  return Math.round(s * 1000) / 10
})

const hasHistoryRiskWash = computed(() => {
  if (!summary.value?.runId) return false
  const cur = Number(summary.value.maxScore ?? 0)
  const earlier = Number(history.value?.maxEarlierVersions ?? 0)
  return earlier >= 0.9 && cur + 1e-9 < earlier
})

const historyHintText = computed(() => {
  if (!history.value) return ''
  const v = history.value.versions ?? []
  if (v.length <= 1) return ''
  return `历史最高 ${historyMaxAcrossPercent.value}%`
})

const imageStatsText = computed(() => {
  if (!summary.value?.runId) return ''
  const p = Number(summary.value.imagesProcessed ?? 0)
  const k = Number(summary.value.imagesSkipped ?? 0)
  return `图片参与 ${p}，跳过 ${k}`
})

const textAttStatsText = computed(() => {
  if (!summary.value?.runId) return ''
  const p = Number(summary.value.textAttachmentsProcessed ?? 0)
  const k = Number(summary.value.textAttachmentsSkipped ?? 0)
  return `文本附件参与 ${p}，跳过 ${k}`
})

const scoreLevel = computed<'low' | 'mid' | 'high'>(() => {
  const s = summary.value?.maxScore ?? 0
  if (s >= 0.9) return 'high'
  if (s >= 0.8) return 'mid'
  return 'low'
})

function levelColor(): string {
  if (scoreLevel.value === 'high') return '#b42318'
  if (scoreLevel.value === 'mid') return '#b54708'
  return '#667085'
}

function escapeHtml(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function toPlainText(md: string): string {
  if (!md) return ''
  let s = md
  s = s.split('```').join('')
  s = s.replace(/`([^`]*)`/g, '$1')
  s = s.replace(/\[([^\]]+)\]\(([^)]+)\)/g, '$1')
  s = s.replace(/^\s{0,3}(#+\s+|>\s+|[-*+]\s+|\d+\.\s+)/gm, '')
  s = s.replace(/\*/g, ' ').replace(/_/g, ' ')
  s = s.replace(/\r|\t/g, ' ')
  return s
}

const fragments = computed<Fragment[]>(() => {
  const ev = summary.value?.evidence ?? []
  const item = ev.find((x) => x.type === 'SUBMISSION_TEXT') as any
  const fr = item?.detail?.fragments
  if (!Array.isArray(fr)) return []
  return fr
    .map((x: any) => ({ text: String(x.text ?? ''), score: Number(x.score ?? 0) }))
    .filter((x) => x.text.trim().length > 0)
})

const highlightedHtml = computed(() => {
  const base = toPlainText(props.contentMd || '')
  let html = escapeHtml(base)
  const list = [...fragments.value]
  // Prefer longer fragments first to avoid partial overlaps.
  list.sort((a, b) => b.text.length - a.text.length)
  for (const f of list) {
    const needle = escapeHtml(f.text)
    if (!needle) continue
    html = html.split(needle).join(`<mark>${needle}</mark>`)
  }
  return html.replace(/\n/g, '<br/>')
})

async function load() {
  if (!auth.token) return
  loading.value = true
  try {
    summary.value = await apiData<PlagiarismSummary>(
      `/api/teacher/submissions/${props.submissionId}/plagiarism-summary`,
      { method: 'GET' },
      auth.token,
    )

    historyLoading.value = true
    try {
      history.value = await apiData<PlagiarismHistory>(
        `/api/teacher/submissions/${props.submissionId}/plagiarism-history`,
        { method: 'GET' },
        auth.token,
      )
    } catch {
      history.value = null
    } finally {
      historyLoading.value = false
    }
  } catch (e: any) {
    summary.value = null
    ElMessage.error(e?.message ?? '加载查重摘要失败')
  } finally {
    loading.value = false
  }
}

function historyRowClass({ row }: { row: HistoryVersion }) {
  const cur = history.value?.currentVersionNo
  if (cur != null && row?.versionNo === cur) return 'is-current'
  return ''
}

async function runNow() {
  if (!summary.value?.taskId) return
  running.value = true
  try {
    await apiData(`/api/teacher/tasks/${summary.value.taskId}/plagiarism/run`, { method: 'POST' }, auth.token)
    ElMessage.success('查重已运行')
    await load()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '运行查重失败')
  } finally {
    running.value = false
  }
}

async function downloadAttachment(attachmentId: number, fileName?: string) {
  try {
    await downloadBlob(`/api/attachments/${attachmentId}/download`, {
      token: auth.token,
      fallbackFilename: fileName || `attachment-${attachmentId}`,
    })
  } catch (e: any) {
    ElMessage.error(e?.message ?? '下载失败')
  }
}

async function previewCompare(item: any) {
  if (!item?.detail?.attachmentIdA || !item?.detail?.attachmentIdB) return
  const aId = Number(item.detail.attachmentIdA)
  const bId = Number(item.detail.attachmentIdB)
  if (!Number.isFinite(aId) || !Number.isFinite(bId)) return
  try {
    closeCompare()
    const [a, b] = await Promise.all([
      fetchBlob(`/api/attachments/${aId}/download`, { token: auth.token }),
      fetchBlob(`/api/attachments/${bId}/download`, { token: auth.token }),
    ])
    compareAUrl.value = URL.createObjectURL(a.blob)
    compareBUrl.value = URL.createObjectURL(b.blob)
    compareTitle.value = `${item.detail.fileNameA || 'A'}  vs  ${item.detail.fileNameB || 'B'}`
    compareDialog.value = true
  } catch (e: any) {
    ElMessage.error(e?.message ?? '预览失败')
  }
}

onMounted(load)
watch(
  () => props.submissionId,
  () => load(),
)
</script>

<template>
  <el-card shadow="never" v-loading="loading" style="margin-top: 12px">
    <div style="display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap">
      <div>
        <div class="meta" style="margin-bottom: 6px">查重摘要</div>
        <div v-if="summary?.runId" style="font-size: 16px">
          最高相似度：
          <span :style="{ color: levelColor(), fontWeight: '700' }">{{ maxScorePercent }}%</span>
          <span v-if="summary?.topMatchStudent" class="meta" style="margin-left: 10px">
            最相似对象：{{ summary.topMatchStudent.displayName }}（{{ summary.topMatchStudent.username }}）
          </span>
        </div>
        <div v-if="summary?.runId" class="meta" style="margin-top: 6px">
          <span v-if="imageStatsText">{{ imageStatsText }}</span>
          <span v-if="textAttStatsText" style="margin-left: 10px">{{ textAttStatsText }}</span>
          <span
            v-if="(summary?.imagesProcessed ?? 0) === 0 && (summary?.imagesSkipped ?? 0) > 0"
            style="margin-left: 10px; color: #b54708"
          >
            图片未参与查重（原因见下方“已跳过附件”）
          </span>
        </div>
        <div v-else class="meta">暂无查重结果（还未运行过查重）。</div>
      </div>
      <div style="display: flex; gap: 8px; align-items: center; flex-wrap: wrap">
        <el-radio-group v-model="viewMode" size="small">
          <el-radio-button label="plain">原文</el-radio-button>
          <el-radio-button label="highlight">高亮</el-radio-button>
        </el-radio-group>
        <el-button v-if="!summary?.runId" size="small" type="primary" :loading="running" @click="runNow">先运行查重</el-button>
        <el-button v-else size="small" :loading="running" @click="runNow">重新运行</el-button>
      </div>
    </div>

    <div v-if="viewMode === 'highlight'" style="margin-top: 10px">
      <div v-if="fragments.length === 0" class="meta">未发现可高亮的相似句子（可能相似度较低或内容太短）。</div>
      <div v-else class="highlight-box" v-html="highlightedHtml"></div>
    </div>
    <div v-else class="meta" style="margin-top: 10px">提示：原文在上方“报告内容”里查看；此处提供高亮视图。</div>

    <el-divider />
    <div>
      <div style="display: flex; justify-content: space-between; align-items: center; gap: 10px; flex-wrap: wrap">
        <div class="meta">该学生历史版本查重（本任务）</div>
        <div style="display: flex; gap: 8px; align-items: center; flex-wrap: wrap">
          <span v-if="historyHintText" class="meta">{{ historyHintText }}</span>
        </div>
      </div>

      <div v-if="hasHistoryRiskWash" style="margin-top: 8px; color: #b54708">
        当前版本查重率较低，但历史版本最高达 {{ historyMaxEarlierPercent }}%。建议查看历史版本是否存在抄袭风险。
      </div>

      <div v-if="historyLoading" class="meta" style="margin-top: 8px">加载历史版本中...</div>
      <div v-else-if="!history" class="meta" style="margin-top: 8px">暂无历史版本信息。</div>
      <div v-else style="margin-top: 8px">
        <div class="historyTableBox">
          <el-table :data="history.versions" size="small" border :row-class-name="historyRowClass">
            <el-table-column prop="versionNo" label="版本" width="80" />
            <el-table-column label="查重率" width="120">
              <template #default="{ row }">
                <span v-if="row.hasResult">{{ Math.round((Number(row.maxScore || 0) * 1000)) / 10 }}%</span>
                <span v-else class="meta">未参与</span>
              </template>
            </el-table-column>
            <el-table-column label="最相似对象" min-width="180">
              <template #default="{ row }">
                <span v-if="row.hasResult && row.topMatchStudent">
                  {{ row.topMatchStudent.displayName || '-' }}（{{ row.topMatchStudent.username || row.topMatchStudent.id }}）
                </span>
                <span v-else class="meta">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="submittedAt" label="提交时间" min-width="180" />
            <el-table-column label="说明" width="200">
              <template #default="{ row }">
                <span v-if="!row.hasResult" class="meta">未参与本次查重（需重新运行）</span>
                <span v-else class="meta">submissionId={{ row.submissionId }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div class="meta" style="margin-top: 6px">提示：历史列表使用本任务“最新一次查重运行”的结果。</div>
      </div>
    </div>

    <div v-if="summary?.evidence?.length" style="margin-top: 12px">
      <div class="meta" style="margin-bottom: 6px">证据（Top 5）</div>
      <el-table :data="summary.evidence" size="small" border>
        <el-table-column prop="type" label="类型" width="160" />
        <el-table-column prop="score" label="相似度" width="120">
          <template #default="{ row }">
            <span>{{ Math.round((Number(row.score || 0) * 1000)) / 10 }}%</span>
          </template>
        </el-table-column>
        <el-table-column label="说明" min-width="260">
          <template #default="{ row }">
            <template v-if="row.type === 'ATTACHMENT_TEXT'">
              {{ row.detail?.fileName || '附件' }}
            </template>
            <template v-else-if="row.type === 'ATTACHMENT_IMAGE'">
              {{ row.detail?.fileNameA || 'A' }} vs {{ row.detail?.fileNameB || 'B' }}
            </template>
            <template v-else-if="row.type === 'SUBMISSION_TEXT'">正文相似句子高亮</template>
            <template v-else> - </template>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <template v-if="row.type === 'ATTACHMENT_TEXT'">
              <el-button size="small" @click="downloadAttachment(Number(row.detail?.attachmentId), row.detail?.fileName)">下载</el-button>
            </template>
            <template v-else-if="row.type === 'ATTACHMENT_IMAGE'">
              <el-button size="small" @click="previewCompare(row)">预览对比</el-button>
              <el-button size="small" @click="downloadAttachment(Number(row.detail?.attachmentIdA), row.detail?.fileNameA)">下载A</el-button>
              <el-button size="small" @click="downloadAttachment(Number(row.detail?.attachmentIdB), row.detail?.fileNameB)">下载B</el-button>
            </template>
            <template v-else> - </template>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div v-if="summary?.skippedAttachments?.length" style="margin-top: 12px">
      <div class="meta" style="margin-bottom: 6px">已跳过附件</div>
      <el-table :data="summary.skippedAttachments" size="small" border>
        <el-table-column prop="fileName" label="文件名" min-width="260" />
        <el-table-column prop="reason" label="原因" width="200" />
      </el-table>
    </div>
  </el-card>

  <el-dialog v-model="compareDialog" :title="compareTitle || '图片对比'" width="980px" @closed="closeCompare">
    <div style="display: flex; gap: 12px; flex-wrap: wrap">
      <div style="flex: 1; min-width: 320px">
        <div class="meta" style="margin-bottom: 6px">图片 A</div>
        <img v-if="compareAUrl" :src="compareAUrl" style="max-width: 100%; max-height: 70vh; display: block" />
      </div>
      <div style="flex: 1; min-width: 320px">
        <div class="meta" style="margin-bottom: 6px">图片 B</div>
        <img v-if="compareBUrl" :src="compareBUrl" style="max-width: 100%; max-height: 70vh; display: block" />
      </div>
    </div>
  </el-dialog>
</template>

<style scoped>
.meta {
  color: #667085;
  font-size: 12px;
}

.el-table :deep(.is-current td) {
  background: #eff8ff;
}

.highlight-box {
  padding: 10px 12px;
  border: 1px solid #e4e7ec;
  border-radius: 8px;
  background: #fcfcfd;
  white-space: normal;
  line-height: 1.6;
  font-size: 14px;
}
.highlight-box :deep(mark) {
  background: #ffeda6;
  color: inherit;
  padding: 0 2px;
  border-radius: 3px;
}
</style>
