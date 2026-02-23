<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiData, downloadBlob } from '../../../api/http'
import { useAuthStore } from '../../../stores/auth'

type LabRoom = { id: number; name: string; location?: string | null; openHours?: string | null; createdAt: string; updatedAt: string }

const auth = useAuthStore()
const token = computed(() => auth.token)

const loading = ref(false)
const rows = ref<LabRoom[]>([])
const dialog = ref(false)
const editing = ref<LabRoom | null>(null)
const form = reactive({ name: '', location: '', openHours: '' })

async function load() {
  loading.value = true
  try {
    rows.value = await apiData<LabRoom[]>('/api/admin/lab-rooms', { method: 'GET' }, token.value)
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.name = ''
  form.location = ''
  form.openHours = ''
  dialog.value = true
}

function openEdit(r: LabRoom) {
  editing.value = r
  form.name = r.name
  form.location = r.location ?? ''
  form.openHours = r.openHours ?? ''
  dialog.value = true
}

async function submit() {
  try {
    if (!form.name.trim()) {
      ElMessage.warning('name 不能为空')
      return
    }
    const body = { name: form.name, location: form.location || null, openHours: form.openHours || null }
    if (editing.value) {
      await apiData(`/api/admin/lab-rooms/${editing.value.id}`, { method: 'PUT', body }, token.value)
      ElMessage.success('更新成功')
    } else {
      await apiData(`/api/admin/lab-rooms`, { method: 'POST', body }, token.value)
      ElMessage.success('创建成功')
    }
    dialog.value = false
    await load()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '提交失败')
  }
}

async function del(r: LabRoom) {
  try {
    await ElMessageBox.confirm(`确认删除实验室: ${r.name} ?`, '提示', { type: 'warning' })
    await apiData(`/api/admin/lab-rooms/${r.id}`, { method: 'DELETE' }, token.value)
    ElMessage.success('已删除')
    await load()
  } catch (e: any) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.message ?? '删除失败')
  }
}

async function exportCsv() {
  try {
    await downloadBlob('/api/admin/lab-rooms/export', { token: token.value, fallbackFilename: 'lab-rooms.csv' })
    ElMessage.success('已下载 lab-rooms.csv')
  } catch (e: any) {
    ElMessage.error(e?.message ?? '导出失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="panel">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate">新建实验室</el-button>
      <el-button @click="exportCsv">导出 CSV</el-button>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe height="520">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="名称" width="220" />
      <el-table-column prop="location" label="位置" />
      <el-table-column prop="openHours" label="开放时间" width="240" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="editing ? '编辑实验室' : '新建实验室'" width="560">
      <el-form label-width="120">
        <el-form-item label="name">
          <el-input v-model="form.name" placeholder="实验室 A101" />
        </el-form-item>
        <el-form-item label="location">
          <el-input v-model="form.location" placeholder="教学楼A区 1层" />
        </el-form-item>
        <el-form-item label="openHours">
          <el-input v-model="form.openHours" placeholder="周一至周五 08:00-18:00" />
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

