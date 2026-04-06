<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiData, downloadBlob } from '../../api/http'
import { useAuthStore } from '../../stores/auth'
import { useUiStore } from '../../stores/ui'

type OpenSlot = {
  id?: number
  weekday: number
  startTime: string
  endTime: string
}

type LabRoom = {
  id: number
  name: string
  location?: string | null
  openHours?: string | null
  openSlots: OpenSlot[]
  createdAt: string
  updatedAt: string
}

const props = defineProps<{
  apiBase: string
  exportPath?: string
  exportFilename?: string
  exportExcelPath?: string
  exportExcelFilename?: string
}>()

const auth = useAuthStore()
const ui = useUiStore()
const token = computed(() => auth.token)
const isMobile = computed(() => ui.effectiveMode === 'mobile')

const loading = ref(false)
const rows = ref<LabRoom[]>([])
const dialog = ref(false)
const editing = ref<LabRoom | null>(null)
const form = reactive({
  name: '',
  location: '',
  openSlots: [] as OpenSlot[],
})

const weekdayOptions = [
  { value: 1, label: '周一' },
  { value: 2, label: '周二' },
  { value: 3, label: '周三' },
  { value: 4, label: '周四' },
  { value: 5, label: '周五' },
  { value: 6, label: '周六' },
  { value: 7, label: '周日' },
]

async function load() {
  loading.value = true
  try {
    rows.value = await apiData<LabRoom[]>(props.apiBase, { method: 'GET' }, token.value)
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载实验室失败')
  } finally {
    loading.value = false
  }
}

function resetForm() {
  form.name = ''
  form.location = ''
  form.openSlots = []
}

function openCreate() {
  editing.value = null
  resetForm()
  dialog.value = true
}

function openEdit(row: LabRoom) {
  editing.value = row
  form.name = row.name
  form.location = row.location ?? ''
  form.openSlots = (row.openSlots || []).map((slot) => ({
    id: slot.id,
    weekday: slot.weekday,
    startTime: slot.startTime,
    endTime: slot.endTime,
  }))
  dialog.value = true
}

function addOpenSlot() {
  form.openSlots.push({
    weekday: 1,
    startTime: '08:00:00',
    endTime: '18:00:00',
  })
}

function removeOpenSlot(index: number) {
  form.openSlots.splice(index, 1)
}

function normalizeOpenSlots() {
  return form.openSlots
    .filter((slot) => slot && slot.startTime && slot.endTime)
    .map((slot) => ({
      weekday: slot.weekday,
      startTime: slot.startTime,
      endTime: slot.endTime,
    }))
}

async function submit() {
  try {
    if (!form.name.trim()) {
      ElMessage.warning('请填写实验室名称')
      return
    }
    const body = {
      name: form.name.trim(),
      location: form.location.trim() || null,
      openSlots: normalizeOpenSlots(),
    }
    if (editing.value) {
      await apiData(`${props.apiBase}/${editing.value.id}`, { method: 'PUT', body }, token.value)
      ElMessage.success('实验室已更新')
    } else {
      await apiData(props.apiBase, { method: 'POST', body }, token.value)
      ElMessage.success('实验室已创建')
    }
    dialog.value = false
    await load()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '保存实验室失败')
  }
}

async function removeRoom(row: LabRoom) {
  try {
    await ElMessageBox.confirm(`确认删除实验室：${row.name}？`, '提示', { type: 'warning' })
    await apiData(`${props.apiBase}/${row.id}`, { method: 'DELETE' }, token.value)
    ElMessage.success('实验室已删除')
    await load()
  } catch (e: any) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.message ?? '删除实验室失败')
  }
}

async function exportCsv() {
  if (!props.exportPath) return
  try {
    await downloadBlob(props.exportPath, {
      token: token.value,
      fallbackFilename: props.exportFilename || 'lab-rooms.csv',
    })
    ElMessage.success('实验室 CSV 已下载')
  } catch (e: any) {
    ElMessage.error(e?.message ?? '导出失败')
  }
}

async function exportExcel() {
  if (!props.exportExcelPath) return
  try {
    await downloadBlob(props.exportExcelPath, {
      token: token.value,
      fallbackFilename: props.exportExcelFilename || 'lab-rooms.xlsx',
    })
    ElMessage.success('实验室 Excel 已下载')
  } catch (e: any) {
    ElMessage.error(e?.message ?? '导出失败')
  }
}

function weekdayLabel(weekday: number) {
  return weekdayOptions.find((item) => item.value === weekday)?.label || `周${weekday}`
}

onMounted(load)
</script>

<template>
  <div class="panel">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate">新建实验室</el-button>
      <el-button v-if="exportPath" @click="exportCsv">导出 CSV</el-button>
      <el-button v-if="exportExcelPath" type="primary" plain @click="exportExcel">导出 Excel</el-button>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <div v-if="isMobile" class="card-list" v-loading="loading">
      <el-card v-for="row in rows" :key="row.id" class="room-card" shadow="never">
        <div class="card-head">
          <div>
            <div class="title">{{ row.name }}</div>
            <div class="meta">位置：{{ row.location || '-' }}</div>
            <div class="meta">开放时间：{{ row.openHours || '未配置' }}</div>
          </div>
          <div class="actions">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="removeRoom(row)">删除</el-button>
          </div>
        </div>
        <div v-if="row.openSlots?.length" class="slot-list">
          <div v-for="slot in row.openSlots" :key="slot.id || `${slot.weekday}-${slot.startTime}`" class="slot-row">
            {{ weekdayLabel(slot.weekday) }} {{ slot.startTime }}-{{ slot.endTime }}
          </div>
        </div>
      </el-card>
      <div v-if="!rows.length && !loading" class="empty">暂无实验室</div>
    </div>

    <el-table v-else :data="rows" v-loading="loading" stripe height="520">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="名称" width="180" />
      <el-table-column prop="location" label="位置" min-width="180" />
      <el-table-column prop="openHours" label="开放时间" min-width="260" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="removeRoom(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="editing ? '编辑实验室' : '新建实验室'" :width="isMobile ? '96%' : '720px'">
      <div class="dialog-body">
        <el-form label-width="100" :label-position="isMobile ? 'top' : 'right'">
          <el-form-item label="实验室名称">
            <el-input v-model="form.name" placeholder="实验室 A101" />
          </el-form-item>
          <el-form-item label="位置">
            <el-input v-model="form.location" placeholder="教学楼 A 区 1 层" />
          </el-form-item>
        </el-form>

        <div class="section-head">
          <div class="section-title">开放时段管理</div>
          <el-button size="small" @click="addOpenSlot">新增时段</el-button>
        </div>
        <div class="slot-editor">
          <div v-for="(slot, index) in form.openSlots" :key="slot.id || index" class="slot-card">
            <el-select v-model="slot.weekday" class="slot-field">
              <el-option v-for="item in weekdayOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-time-picker
              v-model="slot.startTime"
              class="slot-field"
              value-format="HH:mm:ss"
              format="HH:mm"
              placeholder="开始时间"
            />
            <el-time-picker
              v-model="slot.endTime"
              class="slot-field"
              value-format="HH:mm:ss"
              format="HH:mm"
              placeholder="结束时间"
            />
            <el-button type="danger" plain @click="removeOpenSlot(index)">删除</el-button>
          </div>
          <div v-if="!form.openSlots.length" class="empty">未配置结构化开放时段。保存后仍可只保留基础信息。</div>
        </div>
      </div>
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
  gap: 12px;
}

.toolbar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.room-card {
  border-radius: 14px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.title {
  font-size: 18px;
  font-weight: 700;
}

.meta {
  color: #667085;
  margin-top: 6px;
}

.slot-list {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.slot-row {
  padding: 8px 10px;
  border-radius: 10px;
  background: #f8fafc;
  color: #334155;
}

.dialog-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-height: 68vh;
  overflow: auto;
  padding-right: 4px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.section-title {
  font-weight: 700;
}

.slot-editor {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.slot-card {
  display: grid;
  grid-template-columns: 140px 1fr 1fr auto;
  gap: 10px;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
}

.slot-field {
  width: 100%;
}

.empty {
  color: #94a3b8;
}

@media (max-width: 768px) {
  .card-head {
    flex-direction: column;
  }

  .actions {
    justify-content: flex-start;
  }

  .slot-card {
    grid-template-columns: 1fr;
  }
}
</style>
