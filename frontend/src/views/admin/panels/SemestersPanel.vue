<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiData, downloadBlob } from '../../../api/http'
import { useAuthStore } from '../../../stores/auth'

type Semester = { id: number; name: string; startDate?: string | null; endDate?: string | null; createdAt: string; updatedAt: string }

const auth = useAuthStore()
const token = computed(() => auth.token)

const loading = ref(false)
const rows = ref<Semester[]>([])

const dialog = ref(false)
const editing = ref<Semester | null>(null)
const form = reactive({ name: '', startDate: '', endDate: '' })

async function load() {
  loading.value = true
  try {
    rows.value = await apiData<Semester[]>('/api/admin/semesters', { method: 'GET' }, token.value)
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.name = ''
  form.startDate = ''
  form.endDate = ''
  dialog.value = true
}

function openEdit(s: Semester) {
  editing.value = s
  form.name = s.name
  form.startDate = s.startDate ?? ''
  form.endDate = s.endDate ?? ''
  dialog.value = true
}

async function submit() {
  try {
    if (!form.name.trim()) {
      ElMessage.warning('name 不能为空')
      return
    }
    const body = {
      name: form.name,
      startDate: form.startDate || null,
      endDate: form.endDate || null,
    }
    if (editing.value) {
      await apiData(`/api/admin/semesters/${editing.value.id}`, { method: 'PUT', body }, token.value)
      ElMessage.success('更新成功')
    } else {
      await apiData(`/api/admin/semesters`, { method: 'POST', body }, token.value)
      ElMessage.success('创建成功')
    }
    dialog.value = false
    await load()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '提交失败')
  }
}

async function del(s: Semester) {
  try {
    await ElMessageBox.confirm(`确认删除学期: ${s.name} ?`, '提示', { type: 'warning' })
    await apiData(`/api/admin/semesters/${s.id}`, { method: 'DELETE' }, token.value)
    ElMessage.success('已删除')
    await load()
  } catch (e: any) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.message ?? '删除失败')
  }
}

async function exportCsv() {
  try {
    await downloadBlob('/api/admin/semesters/export', { token: token.value, fallbackFilename: 'semesters.csv' })
    ElMessage.success('已下载 semesters.csv')
  } catch (e: any) {
    ElMessage.error(e?.message ?? '导出失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="panel">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate">新建学期</el-button>
      <el-button @click="exportCsv">导出 CSV</el-button>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe height="520">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="学期名称" width="200" />
      <el-table-column prop="startDate" label="开始日期" width="140" />
      <el-table-column prop="endDate" label="结束日期" width="140" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="editing ? '编辑学期' : '新建学期'" width="520">
      <el-form label-width="120">
        <el-form-item label="name">
          <el-input v-model="form.name" placeholder="2025-2026-2" />
        </el-form-item>
        <el-form-item label="startDate">
          <el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" placeholder="可选" style="width: 100%" />
        </el-form-item>
        <el-form-item label="endDate">
          <el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" placeholder="可选" style="width: 100%" />
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

