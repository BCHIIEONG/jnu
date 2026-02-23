<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiData, downloadBlob } from '../../../api/http'
import { useAuthStore } from '../../../stores/auth'

type Department = { id: number; name: string; createdAt: string; updatedAt: string }

const auth = useAuthStore()
const token = computed(() => auth.token)

const loading = ref(false)
const rows = ref<Department[]>([])
const dialog = ref(false)
const editing = ref<Department | null>(null)
const form = reactive({ name: '' })

async function load() {
  loading.value = true
  try {
    rows.value = await apiData<Department[]>('/api/admin/departments', { method: 'GET' }, token.value)
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.name = ''
  dialog.value = true
}

function openEdit(d: Department) {
  editing.value = d
  form.name = d.name
  dialog.value = true
}

async function submit() {
  try {
    if (!form.name.trim()) {
      ElMessage.warning('name 不能为空')
      return
    }
    if (editing.value) {
      await apiData(`/api/admin/departments/${editing.value.id}`, { method: 'PUT', body: { name: form.name } }, token.value)
      ElMessage.success('更新成功')
    } else {
      await apiData(`/api/admin/departments`, { method: 'POST', body: { name: form.name } }, token.value)
      ElMessage.success('创建成功')
    }
    dialog.value = false
    await load()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '提交失败')
  }
}

async function del(d: Department) {
  try {
    await ElMessageBox.confirm(`确认删除院系: ${d.name} ?`, '提示', { type: 'warning' })
    await apiData(`/api/admin/departments/${d.id}`, { method: 'DELETE' } as any, token.value)
    ElMessage.success('已删除')
    await load()
  } catch (e: any) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.message ?? '删除失败')
  }
}

async function exportCsv() {
  try {
    await downloadBlob('/api/admin/departments/export', { token: token.value, fallbackFilename: 'departments.csv' })
    ElMessage.success('已下载 departments.csv')
  } catch (e: any) {
    ElMessage.error(e?.message ?? '导出失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="panel">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate">新建院系</el-button>
      <el-button @click="exportCsv">导出 CSV</el-button>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe height="520">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="院系名称" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="editing ? '编辑院系' : '新建院系'" width="480">
      <el-form label-width="100">
        <el-form-item label="name">
          <el-input v-model="form.name" />
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

