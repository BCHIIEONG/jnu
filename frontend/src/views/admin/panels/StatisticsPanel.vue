<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { apiData, downloadBlob } from '../../../api/http'
import { useAuthStore } from '../../../stores/auth'
import { useUiStore } from '../../../stores/ui'
import StatsChart from '../../common/StatsChart.vue'

type Option = { id: number; label: string }
type SemesterOption = { id: number; name: string; startDate: string; endDate: string }
type ChartSeries = { name: string; type: string; data: number[] }
type ChartData = { categories: string[]; series: ChartSeries[] }

type Dashboard = {
  filters: {
    semesterId: number
    semesterName: string
    from: string
    to: string
    teacherId?: number | null
    classId?: number | null
    semesters: SemesterOption[]
    teachers: Option[]
    classes: Option[]
  }
  summary: {
    teacherCount: number
    studentCount: number
    classCount: number
    taskCount: number
    experimentCourseCount: number
    submissionCount: number
    reviewedSubmissionCount: number
    avgScore: number
    attendanceSessionCount: number
    avgAttendanceRate: number
    activeEnrollmentCount: number
    pendingDeviceRequestCount: number
  }
  charts: {
    taskTrend: ChartData
    attendanceTrend: ChartData
  }
  tables: {
    teacherTable: Array<{
      teacherId: number
      teacherName: string
      taskCount: number
      submissionCount: number
      reviewedSubmissionCount: number
      attendanceSessionCount: number
      avgAttendanceRate: number
    }>
    classTable: Array<{
      classId: number
      className: string
      studentCount: number
      submissionCount: number
      attendanceSessionCount: number
      avgAttendanceRate: number
    }>
    experimentCourseTable: Array<{
      courseId: number
      courseTitle: string
      teacherId: number
      teacherName: string
      activeEnrollmentCount: number
      slotCount: number
      attendanceSessionCount: number
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
  teacherId: undefined as number | undefined,
  classId: undefined as number | undefined,
})

const summaryCards = computed(() => {
  if (!dashboard.value) return []
  return [
    { label: '教师数', value: dashboard.value.summary.teacherCount },
    { label: '学生数', value: dashboard.value.summary.studentCount },
    { label: '班级数', value: dashboard.value.summary.classCount },
    { label: '任务数', value: dashboard.value.summary.taskCount },
    { label: '实验课程数', value: dashboard.value.summary.experimentCourseCount },
    { label: '提交数', value: dashboard.value.summary.submissionCount },
    { label: '批阅数', value: dashboard.value.summary.reviewedSubmissionCount },
    { label: '平均分', value: dashboard.value.summary.avgScore },
    { label: '签到场次', value: dashboard.value.summary.attendanceSessionCount },
    { label: '平均到课率', value: formatRate(dashboard.value.summary.avgAttendanceRate) },
    { label: '有效报名数', value: dashboard.value.summary.activeEnrollmentCount },
    { label: '待处理借用', value: dashboard.value.summary.pendingDeviceRequestCount },
  ]
})

function buildQueryString() {
  const params = new URLSearchParams()
  if (query.semesterId) params.set('semesterId', String(query.semesterId))
  if (query.from) params.set('from', query.from)
  if (query.to) params.set('to', query.to)
  if (query.teacherId) params.set('teacherId', String(query.teacherId))
  if (query.classId) params.set('classId', String(query.classId))
  return params.toString()
}

async function loadDashboard() {
  loading.value = true
  try {
    const suffix = buildQueryString()
    const data = await apiData<Dashboard>(`/api/admin/statistics/dashboard${suffix ? `?${suffix}` : ''}`, { method: 'GET' }, auth.token)
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

async function exportCsv(path: string, fallbackFilename: string) {
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
    <el-card shadow="never">
      <div :class="isMobile ? 'filterStack' : 'filterRow'">
        <el-select v-model="query.semesterId" placeholder="选择学期" clearable :style="isMobile ? 'width: 100%' : 'width: 180px'">
          <el-option v-for="item in dashboard?.filters.semesters || []" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="query.teacherId" placeholder="按教师筛选" clearable :style="isMobile ? 'width: 100%' : 'width: 180px'">
          <el-option v-for="item in dashboard?.filters.teachers || []" :key="item.id" :label="item.label" :value="item.id" />
        </el-select>
        <el-select v-model="query.classId" placeholder="按班级筛选" clearable :style="isMobile ? 'width: 100%' : 'width: 220px'">
          <el-option v-for="item in dashboard?.filters.classes || []" :key="item.id" :label="item.label" :value="item.id" />
        </el-select>
        <el-date-picker v-model="query.from" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" :style="isMobile ? 'width: 100%' : 'width: 180px'" />
        <el-date-picker v-model="query.to" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" :style="isMobile ? 'width: 100%' : 'width: 180px'" />
        <el-button @click="loadDashboard">查询</el-button>
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

    <el-card shadow="never">
      <template #header>
        <div class="sectionHeader">
          <span>教师统计</span>
          <div class="filterRow">
            <el-button size="small" @click="exportCsv('/api/admin/statistics/reports/teachers/export', 'admin-teacher-stats.csv')">导出 CSV</el-button>
            <el-button size="small" type="primary" plain @click="exportExcel('/api/admin/statistics/reports/teachers/export/excel', 'admin-teacher-stats.xlsx')">导出 Excel</el-button>
          </div>
        </div>
      </template>
      <template v-if="!isMobile">
        <el-table :data="dashboard?.tables.teacherTable || []" stripe>
          <el-table-column prop="teacherName" label="教师" min-width="180" />
          <el-table-column prop="taskCount" label="任务数" width="90" />
          <el-table-column prop="submissionCount" label="提交数" width="90" />
          <el-table-column prop="reviewedSubmissionCount" label="批阅数" width="90" />
          <el-table-column prop="attendanceSessionCount" label="签到场次" width="100" />
          <el-table-column label="平均到课率" width="120">
            <template #default="{ row }">{{ formatRate(row.avgAttendanceRate) }}</template>
          </el-table-column>
        </el-table>
      </template>
      <div v-else class="mobileCardList">
        <el-card v-for="row in dashboard?.tables.teacherTable || []" :key="row.teacherId" shadow="never" class="mobileStatCard">
          <div class="mobileStatTitle">{{ row.teacherName }}</div>
          <div class="mobileStatMeta">任务 {{ row.taskCount }} / 提交 {{ row.submissionCount }} / 批阅 {{ row.reviewedSubmissionCount }}</div>
          <div class="mobileStatMeta">签到场次 {{ row.attendanceSessionCount }} / 到课率 {{ formatRate(row.avgAttendanceRate) }}</div>
        </el-card>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="sectionHeader">
          <span>班级统计</span>
          <div class="filterRow">
            <el-button size="small" @click="exportCsv('/api/admin/statistics/reports/classes/export', 'admin-class-stats.csv')">导出 CSV</el-button>
            <el-button size="small" type="primary" plain @click="exportExcel('/api/admin/statistics/reports/classes/export/excel', 'admin-class-stats.xlsx')">导出 Excel</el-button>
          </div>
        </div>
      </template>
      <template v-if="!isMobile">
        <el-table :data="dashboard?.tables.classTable || []" stripe>
          <el-table-column prop="className" label="班级" min-width="220" />
          <el-table-column prop="studentCount" label="学生数" width="90" />
          <el-table-column prop="submissionCount" label="提交数" width="90" />
          <el-table-column prop="attendanceSessionCount" label="签到场次" width="100" />
          <el-table-column label="平均到课率" width="120">
            <template #default="{ row }">{{ formatRate(row.avgAttendanceRate) }}</template>
          </el-table-column>
        </el-table>
      </template>
      <div v-else class="mobileCardList">
        <el-card v-for="row in dashboard?.tables.classTable || []" :key="row.classId" shadow="never" class="mobileStatCard">
          <div class="mobileStatTitle">{{ row.className }}</div>
          <div class="mobileStatMeta">学生 {{ row.studentCount }} / 提交 {{ row.submissionCount }}</div>
          <div class="mobileStatMeta">签到场次 {{ row.attendanceSessionCount }} / 到课率 {{ formatRate(row.avgAttendanceRate) }}</div>
        </el-card>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="sectionHeader">
          <span>实验课程统计</span>
          <div class="filterRow">
            <el-button size="small" @click="exportCsv('/api/admin/statistics/reports/experiment-courses/export', 'admin-experiment-course-stats.csv')">导出 CSV</el-button>
            <el-button size="small" type="primary" plain @click="exportExcel('/api/admin/statistics/reports/experiment-courses/export/excel', 'admin-experiment-course-stats.xlsx')">导出 Excel</el-button>
          </div>
        </div>
      </template>
      <template v-if="!isMobile">
        <el-table :data="dashboard?.tables.experimentCourseTable || []" stripe>
          <el-table-column prop="courseTitle" label="课程" min-width="220" />
          <el-table-column prop="teacherName" label="教师" width="140" />
          <el-table-column prop="activeEnrollmentCount" label="有效报名" width="100" />
          <el-table-column prop="slotCount" label="场次数" width="90" />
          <el-table-column prop="attendanceSessionCount" label="签到场次" width="100" />
        </el-table>
      </template>
      <div v-else class="mobileCardList">
        <el-card v-for="row in dashboard?.tables.experimentCourseTable || []" :key="row.courseId" shadow="never" class="mobileStatCard">
          <div class="mobileStatTitle">{{ row.courseTitle }}</div>
          <div class="mobileStatMeta">教师 {{ row.teacherName }}</div>
          <div class="mobileStatMeta">有效报名 {{ row.activeEnrollmentCount }} / 场次数 {{ row.slotCount }} / 签到场次 {{ row.attendanceSessionCount }}</div>
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

.filterRow,
.filterStack {
  display: flex;
  gap: 10px;
}

.filterRow {
  flex-wrap: wrap;
  align-items: center;
}

.filterStack {
  flex-direction: column;
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
