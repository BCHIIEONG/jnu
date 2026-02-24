<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiData } from '../../../api/http'
import { useAuthStore } from '../../../stores/auth'

type Semester = { id: number; name: string }
type ClassItem = { id: number; name: string; departmentName?: string }
type LabRoom = { id: number; name: string }
type TimeSlot = { id: number; code: string; name: string; startTime: string; endTime: string }
type TeacherUser = { id: number; username: string; displayName: string }

type CourseSchedule = {
  id: number
  semesterId: number
  classId: number
  className: string
  teacherId: number
  teacherDisplayName: string
  labRoomId?: number | null
  labRoomName?: string | null
  lessonDate: string
  slotId: number
  slotCode: string
  slotName: string
  slotStartTime: string
  slotEndTime: string
  courseName?: string | null
}

const auth = useAuthStore()
const token = computed(() => auth.token)

const loading = ref(false)
const rows = ref<CourseSchedule[]>([])

const semesters = ref<Semester[]>([])
const classes = ref<ClassItem[]>([])
const labRooms = ref<LabRoom[]>([])
const timeSlots = ref<TimeSlot[]>([])
const teachers = ref<TeacherUser[]>([])

const filter = reactive({
  semesterId: undefined as number | undefined,
  classId: undefined as number | undefined,
  teacherId: undefined as number | undefined,
  from: '' as string,
  to: '' as string,
})

const dialog = ref(false)
const editing = ref<CourseSchedule | null>(null)
const form = reactive({
  semesterId: undefined as number | undefined,
  classId: undefined as number | undefined,
  teacherId: undefined as number | undefined,
  labRoomId: undefined as number | undefined,
  lessonDate: '',
  slotId: undefined as number | undefined,
  courseName: '实验课（示例）',
})

async function loadMeta() {
  semesters.value = await apiData<Semester[]>('/api/admin/semesters', { method: 'GET' }, token.value)
  classes.value = await apiData<any[]>('/api/admin/classes', { method: 'GET' }, token.value)
  labRooms.value = await apiData<LabRoom[]>('/api/admin/lab-rooms', { method: 'GET' }, token.value)
  timeSlots.value = await apiData<TimeSlot[]>('/api/admin/time-slots', { method: 'GET' }, token.value)

  const page = await apiData<{ items: any[]; total: number; page: number; size: number }>(
    '/api/admin/users?roleCode=ROLE_TEACHER&page=1&size=100',
    { method: 'GET' },
    token.value,
  )
  teachers.value = (page.items ?? []).map((u) => ({ id: u.id, username: u.username, displayName: u.displayName }))
}

async function load() {
  loading.value = true
  try {
    const q = new URLSearchParams()
    if (filter.semesterId) q.set('semesterId', String(filter.semesterId))
    if (filter.classId) q.set('classId', String(filter.classId))
    if (filter.teacherId) q.set('teacherId', String(filter.teacherId))
    if (filter.from) q.set('from', filter.from)
    if (filter.to) q.set('to', filter.to)
    rows.value = await apiData<CourseSchedule[]>(`/api/admin/course-schedules?${q.toString()}`, { method: 'GET' }, token.value)
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.semesterId = filter.semesterId ?? semesters.value[0]?.id
  form.classId = filter.classId
  form.teacherId = filter.teacherId
  form.labRoomId = undefined
  form.lessonDate = ''
  form.slotId = timeSlots.value[0]?.id
  form.courseName = '实验课（示例）'
  dialog.value = true
}

function openEdit(row: CourseSchedule) {
  editing.value = row
  form.semesterId = row.semesterId
  form.classId = row.classId
  form.teacherId = row.teacherId
  form.labRoomId = row.labRoomId ?? undefined
  form.lessonDate = row.lessonDate
  form.slotId = row.slotId
  form.courseName = row.courseName ?? ''
  dialog.value = true
}

async function submit() {
  try {
    if (!form.semesterId) return ElMessage.warning('semesterId 不能为空')
    if (!form.classId) return ElMessage.warning('classId 不能为空')
    if (!form.teacherId) return ElMessage.warning('teacherId 不能为空')
    if (!form.lessonDate) return ElMessage.warning('lessonDate 不能为空')
    if (!form.slotId) return ElMessage.warning('slotId 不能为空')

    const body: any = {
      semesterId: form.semesterId,
      classId: form.classId,
      teacherId: form.teacherId,
      labRoomId: form.labRoomId || null,
      lessonDate: form.lessonDate,
      slotId: form.slotId,
      courseName: form.courseName || null,
    }

    if (editing.value) {
      await apiData(`/api/admin/course-schedules/${editing.value.id}`, { method: 'PUT', body }, token.value)
      ElMessage.success('更新成功')
    } else {
      await apiData(`/api/admin/course-schedules`, { method: 'POST', body }, token.value)
      ElMessage.success('创建成功')
    }

    dialog.value = false
    await load()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '提交失败')
  }
}

async function del(row: CourseSchedule) {
  try {
    await ElMessageBox.confirm(`确认删除课表项: ${row.lessonDate} ${row.slotName} / ${row.className}?`, '提示', { type: 'warning' })
    await apiData(`/api/admin/course-schedules/${row.id}`, { method: 'DELETE' }, token.value)
    ElMessage.success('已删除')
    await load()
  } catch (e: any) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.message ?? '删除失败')
  }
}

onMounted(async () => {
  try {
    await loadMeta()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载元数据失败')
  }
  await load()
})
</script>

<template>
  <div class="panel">
    <div class="toolbar">
      <el-select v-model="filter.semesterId" clearable placeholder="学期" style="width: 180px">
        <el-option v-for="s in semesters" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <el-select v-model="filter.classId" clearable placeholder="班级" style="width: 220px">
        <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-select v-model="filter.teacherId" clearable placeholder="教师" style="width: 180px">
        <el-option v-for="t in teachers" :key="t.id" :label="t.displayName" :value="t.id" />
      </el-select>
      <el-input v-model="filter.from" placeholder="from: YYYY-MM-DD" style="width: 160px" />
      <el-input v-model="filter.to" placeholder="to: YYYY-MM-DD" style="width: 160px" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="openCreate">新建课表项</el-button>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe height="520">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="lessonDate" label="日期" width="120" />
      <el-table-column prop="slotName" label="节次" width="120" />
      <el-table-column prop="className" label="班级" min-width="180" />
      <el-table-column prop="teacherDisplayName" label="教师" width="140" />
      <el-table-column prop="labRoomName" label="实验室" width="140" />
      <el-table-column prop="courseName" label="课程名" min-width="160" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="editing ? '编辑课表项' : '新建课表项'" width="720">
      <el-form label-width="120">
        <el-form-item label="学期">
          <el-select v-model="form.semesterId" style="width: 100%">
            <el-option v-for="s in semesters" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="form.classId" style="width: 100%">
            <el-option v-for="c in classes" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="教师">
          <el-select v-model="form.teacherId" style="width: 100%">
            <el-option v-for="t in teachers" :key="t.id" :label="t.displayName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="实验室">
          <el-select v-model="form.labRoomId" clearable style="width: 100%">
            <el-option v-for="r in labRooms" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-input v-model="form.lessonDate" placeholder="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="节次">
          <el-select v-model="form.slotId" style="width: 100%">
            <el-option v-for="ts in timeSlots" :key="ts.id" :label="`${ts.name} (${ts.startTime}-${ts.endTime})`" :value="ts.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程名">
          <el-input v-model="form.courseName" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.toolbar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
</style>

