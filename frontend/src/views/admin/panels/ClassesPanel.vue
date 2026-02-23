<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiData, downloadBlob } from '../../../api/http'
import { useAuthStore } from '../../../stores/auth'

type Department = { id: number; name: string }
type ClassItem = { id: number; departmentId: number; departmentName: string; name: string; createdAt: string; updatedAt: string }

const auth = useAuthStore()
const token = computed(() => auth.token)

const loading = ref(false)
const departments = ref<Department[]>([])
const rows = ref<ClassItem[]>([])

const filter = reactive({ departmentId: undefined as number | undefined })

const dialog = ref(false)
const editing = ref<ClassItem | null>(null)
const form = reactive({ departmentId: undefined as number | undefined, name: '' })

async function loadMeta() {
  departments.value = await apiData<Department[]>('/api/admin/departments', { method: 'GET' }, token.value)
}

async function load() {
  loading.value = true
  try {
    const q = new URLSearchParams()
    if (filter.departmentId) q.set('departmentId', String(filter.departmentId))
    rows.value = await apiData<ClassItem[]>(`/api/admin/classes?${q.toString()}`, { method: 'GET' }, token.value)
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.departmentId = filter.departmentId
  form.name = ''
  dialog.value = true
}

function openEdit(c: ClassItem) {
  editing.value = c
  form.departmentId = c.departmentId
  form.name = c.name
  dialog.value = true
}

async function submit() {
  try {
    if (!form.departmentId) {
      ElMessage.warning('departmentId 不能为空')
      return
    }
    if (!form.name.trim()) {
      ElMessage.warning('name 不能为空')
      return
    }
    const body = { departmentId: form.departmentId, name: form.name }
    if (editing.value) {
      await apiData(`/api/admin/classes/${editing.value.id}`, { method: 'PUT', body }, token.value)
      ElMessage.success('更新成功')
    } else {
      await apiData(`/api/admin/classes`, { method: 'POST', body }, token.value)
      ElMessage.success('创建成功')
    }
    dialog.value = false
    await load()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '提交失败')
  }
}

async function del(c: ClassItem) {
  try {
    await ElMessageBox.confirm(`确认删除班级: ${c.departmentName} / ${c.name} ?`, '提示', { type: 'warning' })
    await apiData(`/api/admin/classes/${c.id}`, { method: 'DELETE' }, token.value)
    ElMessage.success('已删除')
    await load()
  } catch (e: any) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.message ?? '删除失败')
  }
}

async function exportCsv() {
  try {
    await downloadBlob('/api/admin/classes/export', { token: token.value, fallbackFilename: 'classes.csv' })
    ElMessage.success('已下载 classes.csv')
  } catch (e: any) {
    ElMessage.error(e?.message ?? '导出失败')
  }
}

onMounted(async () => {
  try {
    await loadMeta()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载院系列表失败')
  }
  await load()
})
</script>

<template>
  <div class="panel">
    <div class="toolbar">
      <el-select v-model="filter.departmentId" clearable placeholder="筛选院系" style="width: 200px">
        <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" />
      </el-select>
      <el-button type="primary" @click="() => { load() }">查询</el-button>
      <el-button @click="openCreate">新建班级</el-button>
      <el-button @click="exportCsv">导出 CSV</el-button>
      <el-button :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe height="520">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="departmentName" label="院系" width="180" />
      <el-table-column prop="name" label="班级名称" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="editing ? '编辑班级' : '新建班级'" width="520">
      <el-form label-width="120">
        <el-form-item label="院系">
          <el-select v-model="form.departmentId" style="width: 100%">
            <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="name">
          <el-input v-model="form.name" placeholder="例如: 2022级软件工程1班" />
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

