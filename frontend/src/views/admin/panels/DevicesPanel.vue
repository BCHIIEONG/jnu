<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiData, downloadBlob } from '../../../api/http'
import { useAuthStore } from '../../../stores/auth'

type Device = {
  id: number
  code: string
  name: string
  status: string
  location?: string | null
  description?: string | null
  createdAt: string
  updatedAt: string
}

const auth = useAuthStore()
const token = computed(() => auth.token)

const loading = ref(false)
const rows = ref<Device[]>([])
const filter = reactive({ q: '', status: '' })

const dialog = ref(false)
const editing = ref<Device | null>(null)
const form = reactive({ code: '', name: '', status: 'AVAILABLE', location: '', description: '' })

function buildQuery() {
  const p = new URLSearchParams()
  if (filter.q) p.set('q', filter.q)
  if (filter.status) p.set('status', filter.status)
  return p.toString()
}

async function load() {
  loading.value = true
  try {
    rows.value = await apiData<Device[]>(`/api/admin/devices?${buildQuery()}`, { method: 'GET' }, token.value)
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
  form.status = 'AVAILABLE'
  form.location = ''
  form.description = ''
  dialog.value = true
}

function openEdit(d: Device) {
  editing.value = d
  form.code = d.code
  form.name = d.name
  form.status = d.status
  form.location = d.location ?? ''
  form.description = d.description ?? ''
  dialog.value = true
}

async function submit() {
  try {
    if (!form.code.trim() || !form.name.trim()) {
      ElMessage.warning('code/name 不能为空')
      return
    }
    const body = {
      code: form.code,
      name: form.name,
      status: form.status || null,
      location: form.location || null,
      description: form.description || null,
    }
    if (editing.value) {
      await apiData(`/api/admin/devices/${editing.value.id}`, { method: 'PUT', body }, token.value)
      ElMessage.success('更新成功')
    } else {
      await apiData(`/api/admin/devices`, { method: 'POST', body }, token.value)
      ElMessage.success('创建成功')
    }
    dialog.value = false
    await load()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '提交失败')
  }
}

async function del(d: Device) {
  try {
    await ElMessageBox.confirm(`确认删除设备: ${d.code} / ${d.name} ?`, '提示', { type: 'warning' })
    await apiData(`/api/admin/devices/${d.id}`, { method: 'DELETE' }, token.value)
    ElMessage.success('已删除')
    await load()
  } catch (e: any) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.message ?? '删除失败')
  }
}

async function exportCsv() {
  try {
    await downloadBlob('/api/admin/devices/export', { token: token.value, fallbackFilename: 'devices.csv' })
    ElMessage.success('已下载 devices.csv')
  } catch (e: any) {
    ElMessage.error(e?.message ?? '导出失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="panel">
    <div class="toolbar">
      <el-input v-model="filter.q" placeholder="搜索 code / name" clearable style="width: 260px" />
      <el-select v-model="filter.status" placeholder="状态" clearable style="width: 160px">
        <el-option label="AVAILABLE" value="AVAILABLE" />
        <el-option label="BORROWED" value="BORROWED" />
        <el-option label="REPAIR" value="REPAIR" />
        <el-option label="LOST" value="LOST" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
      <el-button @click="openCreate">新建设备</el-button>
      <el-button @click="exportCsv">导出 CSV</el-button>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe height="520">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="code" label="编码" width="150" />
      <el-table-column prop="name" label="名称" width="200" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column prop="location" label="位置" />
      <el-table-column prop="description" label="描述" width="220" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="editing ? '编辑设备' : '新建设备'" width="620">
      <el-form label-width="120">
        <el-form-item label="code">
          <el-input v-model="form.code" placeholder="DEV-001" />
        </el-form-item>
        <el-form-item label="name">
          <el-input v-model="form.name" placeholder="示波器" />
        </el-form-item>
        <el-form-item label="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="AVAILABLE" value="AVAILABLE" />
            <el-option label="BORROWED" value="BORROWED" />
            <el-option label="REPAIR" value="REPAIR" />
            <el-option label="LOST" value="LOST" />
          </el-select>
        </el-form-item>
        <el-form-item label="location">
          <el-input v-model="form.location" placeholder="实验室 A101" />
        </el-form-item>
        <el-form-item label="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="备注" />
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
  align-items: center;
}
</style>

