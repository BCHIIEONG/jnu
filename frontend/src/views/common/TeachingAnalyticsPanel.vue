<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { apiData, downloadBlob } from '../../api/http'
import { useAuthStore } from '../../stores/auth'
import { useUiStore } from '../../stores/ui'
import StatsChart from './StatsChart.vue'

type Mode = 'teacher' | 'admin'
type Option = { id: number; label: string }
type SemesterOption = { id: number; name: string; startDate: string; endDate: string }
type ChartSeries = { name: string; type: string; data: Array<number | null> }
type ChartData = { categories: string[]; series: ChartSeries[] }

type Dashboard = {
  filters: {
    semesterId: number
    semesterName: string
    from: string
    to: string
    teacherId?: number | null
    classId?: number | null
    studentId?: number | null
    semesters: SemesterOption[]
    teachers: Option[]
    classes: Option[]
    students: Option[]
  }
  experimentAnalysis: {
    completionRateChart: ChartData
    averageScoreChart: ChartData
    taskTable: Array<{
      taskId: number
      taskTitle: string
      teacherName: string
      visibleStudentCount: number
      submissionCount: number
      reviewedSubmissionCount: number
      confirmedCompletionCount: number
      completionRate: number
      avgScore: number
      maxScore: number
      minScore: number
    }>
  }
  studentAnalysis: {
    scoreTrendChart: ChartData
    riskStudentTable: Array<{
      studentId: number
      studentDisplayName: string
      studentUsername: string
      classDisplayName?: string | null
      submittedTaskCount: number
      reviewedTaskCount: number
      avgScore: number
      completionRate: number
      recentThreeAvgScore: number
      trend: string
      riskReasons: string
    }>
    weakTaskTable: Array<{
      taskId: number
      taskTitle: string
      teacherName: string
      avgScore: number
      completionRate: number
      unsubmittedRate: number
    }>
  }
  reportQualityAnalysis: {
    issueTagChart: ChartData
    plagiarismRiskChart: ChartData
    issueTagTable: Array<{
      tagCode: string
      tagLabel: string
      occurrenceCount: number
      studentCount: number
      avgScore: number
    }>
  }
}

const props = defineProps<{
  mode: Mode
}>()

const auth = useAuthStore()
const ui = useUiStore()
const mode = computed(() => props.mode)
const isMobile = computed(() => ui.effectiveMode === 'mobile')
const loading = ref(false)
const dashboard = ref<Dashboard | null>(null)
const query = reactive({
  semesterId: undefined as number | undefined,
  from: '',
  to: '',
  teacherId: undefined as number | undefined,
  classId: undefined as number | undefined,
  studentId: undefined as number | undefined,
})

const experimentSummary = computed(() => {
  const rows = dashboard.value?.experimentAnalysis.taskTable || []
  return {
    avgCompletionRate: rows.length
      ? rows.reduce((sum, item) => sum + (item.completionRate || 0), 0) / rows.length
      : 0,
    avgScore: rows.length ? rows.reduce((sum, item) => sum + (item.avgScore || 0), 0) / rows.length : 0,
    taskCount: rows.length,
  }
})

function buildQueryString() {
  const params = new URLSearchParams()
  if (query.semesterId) params.set('semesterId', String(query.semesterId))
  if (query.from) params.set('from', query.from)
  if (query.to) params.set('to', query.to)
  if (props.mode === 'admin' && query.teacherId) params.set('teacherId', String(query.teacherId))
  if (query.classId) params.set('classId', String(query.classId))
  if (query.studentId) params.set('studentId', String(query.studentId))
  return params.toString()
}

async function loadDashboard() {
  loading.value = true
  try {
    const suffix = buildQueryString()
    const path = props.mode === 'teacher' ? '/api/teacher/analytics/teaching' : '/api/admin/analytics/teaching'
    const data = await apiData<Dashboard>(`${path}${suffix ? `?${suffix}` : ''}`, { method: 'GET' }, auth.token)
    dashboard.value = data
    if (!query.semesterId) query.semesterId = data.filters.semesterId
    if (!query.from) query.from = data.filters.from
    if (!query.to) query.to = data.filters.to
    query.teacherId = data.filters.teacherId ?? query.teacherId
    query.classId = data.filters.classId ?? query.classId
    query.studentId = data.filters.studentId ?? query.studentId
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '教学分析加载失败')
  } finally {
    loading.value = false
  }
}

async function exportExcel(section: 'experiment' | 'student' | 'report-quality') {
  try {
    const suffix = buildQueryString()
    const path = props.mode === 'teacher'
      ? `/api/teacher/analytics/teaching/${section}/export/excel`
      : `/api/admin/analytics/teaching/${section}/export/excel`
    const prefix = props.mode === 'teacher' ? 'teacher' : 'admin'
    await downloadBlob(`${path}${suffix ? `?${suffix}` : ''}`, {
      token: auth.token,
      fallbackFilename: `${prefix}-teaching-${section}.xlsx`,
    })
    ElMessage.success('导出成功')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导出失败')
  }
}

function formatRate(value?: number | null) {
  if (typeof value !== 'number' || Number.isNaN(value)) return '0.00%'
  return `${(value * 100).toFixed(2)}%`
}

onMounted(async () => {
  await loadDashboard()
})
</script>

<template>
  <div class="panel" v-loading="loading">
    <el-card shadow="never" class="block">
      <div :class="isMobile ? 'filterStack' : 'filterRow'">
        <el-select v-model="query.semesterId" placeholder="选择学期" clearable :style="isMobile ? 'width: 100%' : 'width: 180px'">
          <el-option v-for="item in dashboard?.filters.semesters || []" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-if="mode === 'admin'" v-model="query.teacherId" placeholder="按教师筛选" clearable :style="isMobile ? 'width: 100%' : 'width: 180px'">
          <el-option v-for="item in dashboard?.filters.teachers || []" :key="item.id" :label="item.label" :value="item.id" />
        </el-select>
        <el-select v-model="query.classId" placeholder="按班级筛选" clearable :style="isMobile ? 'width: 100%' : 'width: 220px'">
          <el-option v-for="item in dashboard?.filters.classes || []" :key="item.id" :label="item.label" :value="item.id" />
        </el-select>
        <el-select v-model="query.studentId" placeholder="按学生筛选" clearable :style="isMobile ? 'width: 100%' : 'width: 240px'">
          <el-option v-for="item in dashboard?.filters.students || []" :key="item.id" :label="item.label" :value="item.id" />
        </el-select>
        <el-date-picker v-model="query.from" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" :style="isMobile ? 'width: 100%' : 'width: 180px'" />
        <el-date-picker v-model="query.to" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" :style="isMobile ? 'width: 100%' : 'width: 180px'" />
        <div class="filterActions">
          <el-button @click="loadDashboard">查询</el-button>
        </div>
      </div>
    </el-card>

    <div class="summaryGrid">
      <el-card shadow="never" class="summaryCard">
        <div class="summaryLabel">实验任务数</div>
        <div class="summaryValue">{{ experimentSummary.taskCount }}</div>
      </el-card>
      <el-card shadow="never" class="summaryCard">
        <div class="summaryLabel">平均完成率</div>
        <div class="summaryValue">{{ formatRate(experimentSummary.avgCompletionRate) }}</div>
      </el-card>
      <el-card shadow="never" class="summaryCard">
        <div class="summaryLabel">平均分</div>
        <div class="summaryValue">{{ experimentSummary.avgScore.toFixed?.(2) ?? experimentSummary.avgScore }}</div>
      </el-card>
      <el-card shadow="never" class="summaryCard">
        <div class="summaryLabel">高风险学生数</div>
        <div class="summaryValue">{{ dashboard?.studentAnalysis.riskStudentTable.length || 0 }}</div>
      </el-card>
    </div>

    <el-card shadow="never" class="block">
      <template #header>
        <div class="sectionHeader">
          <span>实验数据分析</span>
          <el-button size="small" type="primary" plain @click="exportExcel('experiment')">导出 Excel</el-button>
        </div>
      </template>
      <div :class="isMobile ? 'chartStack' : 'chartGrid'">
        <StatsChart v-if="dashboard" title="实验完成率排行" :chart="dashboard.experimentAnalysis.completionRateChart" />
        <StatsChart v-if="dashboard" title="实验平均分排行" :chart="dashboard.experimentAnalysis.averageScoreChart" />
      </div>
      <template v-if="!isMobile">
        <el-table :data="dashboard?.experimentAnalysis.taskTable || []" stripe>
          <el-table-column prop="taskTitle" label="任务" min-width="220" />
          <el-table-column prop="teacherName" label="教师" width="140" />
          <el-table-column prop="visibleStudentCount" label="可见" width="90" />
          <el-table-column prop="submissionCount" label="已提交" width="90" />
          <el-table-column prop="reviewedSubmissionCount" label="已批阅" width="90" />
          <el-table-column prop="confirmedCompletionCount" label="已完成" width="90" />
          <el-table-column label="完成率" width="100">
            <template #default="{ row }">{{ formatRate(row.completionRate) }}</template>
          </el-table-column>
          <el-table-column prop="avgScore" label="平均分" width="100" />
          <el-table-column prop="maxScore" label="最高分" width="100" />
          <el-table-column prop="minScore" label="最低分" width="100" />
        </el-table>
      </template>
      <div v-else class="mobileCardList">
        <el-card v-for="row in dashboard?.experimentAnalysis.taskTable || []" :key="row.taskId" shadow="never" class="mobileStatCard">
          <div class="mobileStatTitle">{{ row.taskTitle }}</div>
          <div class="mobileStatMeta">教师 {{ row.teacherName }} / 完成率 {{ formatRate(row.completionRate) }}</div>
          <div class="mobileStatMeta">可见 {{ row.visibleStudentCount }} / 提交 {{ row.submissionCount }} / 批阅 {{ row.reviewedSubmissionCount }} / 完成 {{ row.confirmedCompletionCount }}</div>
          <div class="mobileStatMeta">平均 {{ row.avgScore }} / 最高 {{ row.maxScore }} / 最低 {{ row.minScore }}</div>
        </el-card>
      </div>
    </el-card>

    <el-card shadow="never" class="block">
      <template #header>
        <div class="sectionHeader">
          <span>学生表现分析</span>
          <el-button size="small" type="primary" plain @click="exportExcel('student')">导出 Excel</el-button>
        </div>
      </template>
      <StatsChart v-if="dashboard" title="学生成绩趋势" :chart="dashboard.studentAnalysis.scoreTrendChart" />
      <div :class="isMobile ? 'chartStack' : 'tableGrid'">
        <el-card shadow="never" class="nestedCard">
          <template #header><span>高风险学生</span></template>
          <template v-if="!isMobile">
            <el-table :data="dashboard?.studentAnalysis.riskStudentTable || []" stripe size="small">
              <el-table-column prop="studentDisplayName" label="学生" min-width="140" />
              <el-table-column prop="studentUsername" label="用户名" width="120" />
              <el-table-column prop="classDisplayName" label="班级" min-width="180" />
              <el-table-column prop="avgScore" label="平均分" width="90" />
              <el-table-column label="完成率" width="100">
                <template #default="{ row }">{{ formatRate(row.completionRate) }}</template>
              </el-table-column>
              <el-table-column prop="recentThreeAvgScore" label="最近3次均分" width="120" />
              <el-table-column prop="trend" label="趋势" width="90" />
              <el-table-column prop="riskReasons" label="风险原因" min-width="220" />
            </el-table>
          </template>
          <div v-else class="mobileCardList">
            <el-card v-for="row in dashboard?.studentAnalysis.riskStudentTable || []" :key="row.studentId" shadow="never" class="mobileStatCard">
              <div class="mobileStatTitle">{{ row.studentDisplayName }} / {{ row.studentUsername }}</div>
              <div class="mobileStatMeta">{{ row.classDisplayName || '未分班级' }} / 趋势 {{ row.trend }}</div>
              <div class="mobileStatMeta">平均分 {{ row.avgScore }} / 完成率 {{ formatRate(row.completionRate) }} / 最近3次均分 {{ row.recentThreeAvgScore }}</div>
              <div class="mobileStatMeta">风险：{{ row.riskReasons }}</div>
            </el-card>
          </div>
        </el-card>

        <el-card shadow="never" class="nestedCard">
          <template #header><span>薄弱实验识别</span></template>
          <template v-if="!isMobile">
            <el-table :data="dashboard?.studentAnalysis.weakTaskTable || []" stripe size="small">
              <el-table-column prop="taskTitle" label="任务" min-width="200" />
              <el-table-column prop="teacherName" label="教师" width="120" />
              <el-table-column prop="avgScore" label="平均分" width="90" />
              <el-table-column label="完成率" width="100">
                <template #default="{ row }">{{ formatRate(row.completionRate) }}</template>
              </el-table-column>
              <el-table-column label="未提交率" width="100">
                <template #default="{ row }">{{ formatRate(row.unsubmittedRate) }}</template>
              </el-table-column>
            </el-table>
          </template>
          <div v-else class="mobileCardList">
            <el-card v-for="row in dashboard?.studentAnalysis.weakTaskTable || []" :key="row.taskId" shadow="never" class="mobileStatCard">
              <div class="mobileStatTitle">{{ row.taskTitle }}</div>
              <div class="mobileStatMeta">教师 {{ row.teacherName }}</div>
              <div class="mobileStatMeta">平均分 {{ row.avgScore }} / 完成率 {{ formatRate(row.completionRate) }} / 未提交率 {{ formatRate(row.unsubmittedRate) }}</div>
            </el-card>
          </div>
        </el-card>
      </div>
    </el-card>

    <el-card shadow="never" class="block">
      <template #header>
        <div class="sectionHeader">
          <span>报告质量分析</span>
          <el-button size="small" type="primary" plain @click="exportExcel('report-quality')">导出 Excel</el-button>
        </div>
      </template>
      <div :class="isMobile ? 'chartStack' : 'chartGrid'">
        <StatsChart v-if="dashboard" title="错误类型排行" :chart="dashboard.reportQualityAnalysis.issueTagChart" />
        <StatsChart v-if="dashboard" title="查重风险分布" :chart="dashboard.reportQualityAnalysis.plagiarismRiskChart" />
      </div>
      <template v-if="!isMobile">
        <el-table :data="dashboard?.reportQualityAnalysis.issueTagTable || []" stripe>
          <el-table-column prop="tagLabel" label="问题标签" min-width="180" />
          <el-table-column prop="occurrenceCount" label="出现次数" width="100" />
          <el-table-column prop="studentCount" label="涉及学生数" width="110" />
          <el-table-column prop="avgScore" label="对应平均分" width="120" />
        </el-table>
      </template>
      <div v-else class="mobileCardList">
        <el-card v-for="row in dashboard?.reportQualityAnalysis.issueTagTable || []" :key="row.tagCode" shadow="never" class="mobileStatCard">
          <div class="mobileStatTitle">{{ row.tagLabel }}</div>
          <div class="mobileStatMeta">出现次数 {{ row.occurrenceCount }} / 涉及学生数 {{ row.studentCount }}</div>
          <div class="mobileStatMeta">对应平均分 {{ row.avgScore }}</div>
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.block {
  margin-bottom: 0;
}

.filterRow,
.filterStack {
  display: flex;
  gap: 10px;
}

.filterRow {
  align-items: center;
  flex-wrap: wrap;
}

.filterStack {
  flex-direction: column;
}

.filterActions {
  display: flex;
  gap: 8px;
}

.summaryGrid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 12px;
}

.summaryCard {
  min-height: 96px;
}

.summaryLabel {
  color: #606266;
  font-size: 13px;
}

.summaryValue {
  margin-top: 12px;
  font-size: 26px;
  font-weight: 700;
}

.chartGrid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.tableGrid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-top: 14px;
}

.chartStack {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.sectionHeader {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.nestedCard {
  border: 1px solid #ebeef5;
}

.mobileCardList {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.mobileStatCard {
  border: 1px solid #ebeef5;
}

.mobileStatTitle {
  font-weight: 600;
  margin-bottom: 8px;
}

.mobileStatMeta {
  color: #606266;
  line-height: 1.8;
}
</style>
