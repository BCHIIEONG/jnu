<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiData } from '../../../api/http'
import { useAuthStore } from '../../../stores/auth'

type TimeSlot = {
  id: number
  code: string
  name: string
  startTime: string
  endTime: string
}

const auth = useAuthStore()
const token = computed(() => auth.token)

const loading = ref(false)
const rows = ref<TimeSlot[]>([])

const dialog = ref(false)
const editing = ref<TimeSlot | null>(null)
const form = reactive({ code: '', name: '', startTime: '08:00', endTime: '09:40' })

async function load() {
  loading.value = true
  try {
    rows.value = await apiData<TimeSlot[]>('/api/admin/time-slots', { method: 'GET' }, token.value)
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.code = ''
  form.name = ''
  form.startTime = '08:00'
  form.endTime = '09:40'
  dialog.value = true
}

function openEdit(row: TimeSlot) {
  editing.value = row
  form.code = row.code
  form.name = row.name
  form.startTime = row.startTime?.slice(0, 5) || '08:00'
  form.endTime = row.endTime?.slice(0, 5) || '09:40'
  dialog.value = true
}

async function submit() {
  try {
    if (!form.code.trim()) return ElMessage.warning('code 不能为空')
    if (!form.name.trim()) return ElMessage.warning('name 不能为空')
    if (!form.startTime.trim()) return ElMessage.warning('startTime 不能为空')
    if (!form.endTime.trim()) return ElMessage.warning('endTime 不能为空')

    const body = { code: form.code.trim(), name: form.name.trim(), startTime: form.startTime.trim(), endTime: form.endTime.trim() }
    if (editing.value) {
      await apiData(`/api/admin/time-slots/${editing.value.id}`, { method: 'PUT', body }, token.value)
      ElMessage.success('更新成功')
    } else {
      await apiData(`/api/admin/time-slots`, { method: 'POST', body }, token.value)
      ElMessage.success('创建成功')
    }
    dialog.value = false
    await load()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '提交失败')
  }
}

async function del(row: TimeSlot) {
  try {
    await ElMessageBox.confirm(`确认删除节次: ${row.code} / ${row.name}?`, '提示', { type: 'warning' })
    await apiData(`/api/admin/time-slots/${row.id}`, { method: 'DELETE' }, token.value)
    ElMessage.success('已删除')
    await load()
  } catch (e: any) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.message ?? '删除失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="panel">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate">新建节次</el-button>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe height="520">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="code" label="code" width="120" />
      <el-table-column prop="name" label="名称" width="180" />
      <el-table-column prop="startTime" label="开始" width="120" />
      <el-table-column prop="endTime" label="结束" width="120" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="editing ? '编辑节次' : '新建节次'" width="520">
      <el-form label-width="120">
        <el-form-item label="code">
          <el-input v-model="form.code" placeholder="例如: S1" />
        </el-form-item>
        <el-form-item label="name">
          <el-input v-model="form.name" placeholder="例如: 第1-2节" />
        </el-form-item>
        <el-form-item label="startTime">
          <el-input v-model="form.startTime" placeholder="08:00" />
        </el-form-item>
        <el-form-item label="endTime">
          <el-input v-model="form.endTime" placeholder="09:40" />
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

