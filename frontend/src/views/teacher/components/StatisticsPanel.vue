<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { apiData, downloadBlob } from '../../../api/http'
import { useAuthStore } from '../../../stores/auth'
import { useUiStore } from '../../../stores/ui'
import StatsChart from '../../common/StatsChart.vue'

type SemesterOption = {
  id: number
  name: string
  startDate: string
  endDate: string
}

type ChartSeries = {
  name: string
  type: string
  data: Array<number | null>
}

type ChartData = {
  categories: string[]
  series: ChartSeries[]
}

type Dashboard = {
  filters: {
    semesterId: number
    semesterName: string
    from: string
    to: string
    semesters: SemesterOption[]
  }
  summary: {
    taskCount: number
    openTaskCount: number
    submissionCount: number
    reviewedSubmissionCount: number
    avgScore: number
    confirmedCompletionCount: number
    attendanceSessionCount: number
    avgAttendanceRate: number
    experimentCourseCount: number
    activeEnrollmentCount: number
    pendingDeviceRequestCount: number
  }
  charts: {
    taskTrend: ChartData
    attendanceTrend: ChartData
  }
  tables: {
    taskTable: Array<{
      taskId: number
      taskTitle: string
      submissionCount: number
      reviewedSubmissionCount: number
      avgScore: number
      confirmedCompletionCount: number
    }>
    experimentCourseTable: Array<{
      courseId: number
      courseTitle: string
      slotCount: number
      activeEnrollmentCount: number
      attendanceSessionCount: number
    }>
    deviceRequestTable: Array<{
      taskId: number
      taskTitle: string
      pendingCount: number
      approvedCount: number
      borrowedCount: number
      returnedCount: number
    }>
  }
}

const auth = useAuthStore()
const ui = useUiStore()

const isMobile = computed(() => ui.effectiveMode === 'mobile')
const loading = ref(false)
const dashboard = ref<Dashboard | null>(null)
const query = reactive({
  semesterId: undefined as number | undefined,
  from: '',
  to: '',
})

const summaryCards = computed(() => {
  if (!dashboard.value) return []
  return [
    { label: '任务数', value: dashboard.value.summary.taskCount },
    { label: '开放任务', value: dashboard.value.summary.openTaskCount },
    { label: '提交数', value: dashboard.value.summary.submissionCount },
    { label: '批阅数', value: dashboard.value.summary.reviewedSubmissionCount },
    { label: '平均分', value: dashboard.value.summary.avgScore },
    { label: '已完成登记', value: dashboard.value.summary.confirmedCompletionCount },
    { label: '签到场次', value: dashboard.value.summary.attendanceSessionCount },
    { label: '平均到课率', value: formatRate(dashboard.value.summary.avgAttendanceRate) },
    { label: '实验课程数', value: dashboard.value.summary.experimentCourseCount },
    { label: '有效报名数', value: dashboard.value.summary.activeEnrollmentCount },
    { label: '待处理借用', value: dashboard.value.summary.pendingDeviceRequestCount },
  ]
})

function buildQueryString() {
  const params = new URLSearchParams()
  if (query.semesterId) params.set('semesterId', String(query.semesterId))
  if (query.from) params.set('from', query.from)
  if (query.to) params.set('to', query.to)
  return params.toString()
}

async function loadDashboard() {
  loading.value = true
  try {
    const suffix = buildQueryString()
    const data = await apiData<Dashboard>(`/api/teacher/statistics/dashboard${suffix ? `?${suffix}` : ''}`, { method: 'GET' }, auth.token)
    dashboard.value = data
    if (!query.semesterId) query.semesterId = data.filters.semesterId
    if (!query.from) query.from = data.filters.from
    if (!query.to) query.to = data.filters.to
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '统计数据加载失败')
  } finally {
    loading.value = false
  }
}

async function exportExcel(path: string, fallbackFilename: string) {
  try {
    const suffix = buildQueryString()
    await downloadBlob(`${path}${suffix ? `?${suffix}` : ''}`, {
      token: auth.token,
      fallbackFilename,
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
  <div class="statsPanel" v-loading="loading">
    <el-card shadow="never" class="block">
      <div :class="isMobile ? 'filterStack' : 'filterRow'">
        <el-select v-model="query.semesterId" placeholder="选择学期" clearable :style="isMobile ? 'width: 100%' : 'width: 220px'">
          <el-option
            v-for="item in dashboard?.filters.semesters || []"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
        <el-date-picker v-model="query.from" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" :style="isMobile ? 'width: 100%' : 'width: 180px'" />
        <el-date-picker v-model="query.to" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" :style="isMobile ? 'width: 100%' : 'width: 180px'" />
        <div class="filterActions">
          <el-button @click="loadDashboard">查询</el-button>
        </div>
      </div>
    </el-card>

    <div class="summaryGrid">
      <el-card v-for="item in summaryCards" :key="item.label" shadow="never" class="summaryCard">
        <div class="summaryLabel">{{ item.label }}</div>
        <div class="summaryValue">{{ item.value }}</div>
      </el-card>
    </div>

    <div :class="isMobile ? 'chartStack' : 'chartGrid'">
      <StatsChart v-if="dashboard" title="提交与批阅趋势" :chart="dashboard.charts.taskTrend" />
      <StatsChart v-if="dashboard" title="签到与到课率趋势" :chart="dashboard.charts.attendanceTrend" />
    </div>

    <el-card shadow="never" class="block">
      <template #header>
        <div class="sectionHeader">
          <span>任务统计</span>
          <div class="filterActions">
            <el-button size="small" type="primary" plain @click="exportExcel('/api/teacher/statistics/reports/tasks/export/excel', 'teacher-task-stats.xlsx')">导出 Excel</el-button>
          </div>
        </div>
      </template>
      <template v-if="!isMobile">
        <el-table :data="dashboard?.tables.taskTable || []" stripe>
          <el-table-column prop="taskTitle" label="任务" min-width="220" />
          <el-table-column prop="submissionCount" label="提交数" width="100" />
          <el-table-column prop="reviewedSubmissionCount" label="批阅数" width="100" />
          <el-table-column prop="avgScore" label="平均分" width="110" />
          <el-table-column prop="confirmedCompletionCount" label="已确认完成" width="120" />
        </el-table>
      </template>
      <div v-else class="mobileCardList">
        <el-card v-for="row in dashboard?.tables.taskTable || []" :key="row.taskId" shadow="never" class="mobileStatCard">
          <div class="mobileStatTitle">{{ row.taskTitle }}</div>
          <div class="mobileStatMeta">提交 {{ row.submissionCount }} / 批阅 {{ row.reviewedSubmissionCount }}</div>
          <div class="mobileStatMeta">平均分 {{ row.avgScore }} / 已确认完成 {{ row.confirmedCompletionCount }}</div>
        </el-card>
      </div>
    </el-card>

    <el-card shadow="never" class="block">
      <template #header>
        <div class="sectionHeader">
          <span>实验课程统计</span>
          <div class="filterActions">
            <el-button size="small" type="primary" plain @click="exportExcel('/api/teacher/statistics/reports/experiment-courses/export/excel', 'teacher-experiment-course-stats.xlsx')">导出 Excel</el-button>
          </div>
        </div>
      </template>
      <template v-if="!isMobile">
        <el-table :data="dashboard?.tables.experimentCourseTable || []" stripe>
          <el-table-column prop="courseTitle" label="课程" min-width="220" />
          <el-table-column prop="slotCount" label="场次数" width="100" />
          <el-table-column prop="activeEnrollmentCount" label="有效报名" width="100" />
          <el-table-column prop="attendanceSessionCount" label="签到场次" width="100" />
        </el-table>
      </template>
      <div v-else class="mobileCardList">
        <el-card v-for="row in dashboard?.tables.experimentCourseTable || []" :key="row.courseId" shadow="never" class="mobileStatCard">
          <div class="mobileStatTitle">{{ row.courseTitle }}</div>
          <div class="mobileStatMeta">场次数 {{ row.slotCount }} / 有效报名 {{ row.activeEnrollmentCount }}</div>
          <div class="mobileStatMeta">签到场次 {{ row.attendanceSessionCount }}</div>
        </el-card>
      </div>
    </el-card>

    <el-card shadow="never" class="block">
      <template #header>
        <div class="sectionHeader">
          <span>设备借用统计</span>
          <div class="filterActions">
            <el-button size="small" type="primary" plain @click="exportExcel('/api/teacher/statistics/reports/device-requests/export/excel', 'teacher-device-request-stats.xlsx')">导出 Excel</el-button>
          </div>
        </div>
      </template>
      <template v-if="!isMobile">
        <el-table :data="dashboard?.tables.deviceRequestTable || []" stripe>
          <el-table-column prop="taskTitle" label="任务" min-width="220" />
          <el-table-column prop="pendingCount" label="待审批" width="90" />
          <el-table-column prop="approvedCount" label="已批准" width="90" />
          <el-table-column prop="borrowedCount" label="借出中" width="90" />
          <el-table-column prop="returnedCount" label="已归还" width="90" />
        </el-table>
      </template>
      <div v-else class="mobileCardList">
        <el-card v-for="row in dashboard?.tables.deviceRequestTable || []" :key="row.taskId" shadow="never" class="mobileStatCard">
          <div class="mobileStatTitle">{{ row.taskTitle }}</div>
          <div class="mobileStatMeta">待审批 {{ row.pendingCount }} / 已批准 {{ row.approvedCount }}</div>
          <div class="mobileStatMeta">借出中 {{ row.borrowedCount }} / 已归还 {{ row.returnedCount }}</div>
        </el-card>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.statsPanel {
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
