<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiData, downloadBlob, uploadFormData } from '../../../api/http'
import { useAuthStore } from '../../../stores/auth'

type PageResult<T> = { page: number; size: number; total: number; items: T[] }
type Role = { id: number; code: string; name: string; userCount: number }
type Department = { id: number; name: string }
type ClassItem = { id: number; departmentId: number; departmentName: string; name: string }
type UserItem = {
  id: number
  username: string
  displayName: string
  enabled: boolean
  departmentId?: number | null
  departmentName?: string | null
  classId?: number | null
  className?: string | null
  roleCodes: string[]
  createdAt: string
  updatedAt: string
}

const auth = useAuthStore()
const token = computed(() => auth.token)

const loading = ref(false)
const metaLoading = ref(false)
const roles = ref<Role[]>([])
const departments = ref<Department[]>([])
const classes = ref<ClassItem[]>([])
const data = ref<PageResult<UserItem>>({ page: 1, size: 20, total: 0, items: [] })

const query = reactive({
  q: '',
  roleCode: '',
  enabled: '' as '' | 'true' | 'false',
  departmentId: undefined as number | undefined,
  classId: undefined as number | undefined,
  page: 1,
  size: 20,
})

const filteredClasses = computed(() => {
  if (!query.departmentId) return classes.value
  return classes.value.filter((c) => c.departmentId === query.departmentId)
})

const createDialog = ref(false)
const editDialog = ref(false)
const rolesDialog = ref(false)

const createForm = reactive({
  username: '',
  displayName: '',
  password: '',
  enabled: true,
  departmentId: undefined as number | undefined,
  classId: undefined as number | undefined,
  roleCodes: ['ROLE_STUDENT'] as string[],
})

const editTarget = ref<UserItem | null>(null)
const editForm = reactive({
  displayName: '',
  enabled: true,
  departmentId: undefined as number | undefined,
  classId: undefined as number | undefined,
})

const rolesTarget = ref<UserItem | null>(null)
const rolesForm = reactive({
  roleCodes: [] as string[],
})

const importFile = ref<File | null>(null)
const importing = ref(false)

async function loadMeta() {
  metaLoading.value = true
  try {
    const [r, d, c] = await Promise.all([
      apiData<Role[]>('/api/admin/roles', { method: 'GET' }, token.value),
      apiData<Department[]>('/api/admin/departments', { method: 'GET' }, token.value),
      apiData<ClassItem[]>('/api/admin/classes', { method: 'GET' }, token.value),
    ])
    roles.value = r
    departments.value = d
    classes.value = c
  } finally {
    metaLoading.value = false
  }
}

function buildQuery() {
  const p = new URLSearchParams()
  if (query.q) p.set('q', query.q)
  if (query.roleCode) p.set('roleCode', query.roleCode)
  if (query.enabled) p.set('enabled', query.enabled)
  if (query.departmentId) p.set('departmentId', String(query.departmentId))
  if (query.classId) p.set('classId', String(query.classId))
  p.set('page', String(query.page))
  p.set('size', String(query.size))
  return p.toString()
}

async function loadUsers() {
  loading.value = true
  try {
    data.value = await apiData<PageResult<UserItem>>(`/api/admin/users?${buildQuery()}`, { method: 'GET' }, token.value)
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载失败')
  } finally {
    loading.value = false
  }
}

async function openCreate() {
  try {
    // Ensure latest department/class options (no page refresh required after creating classes).
    await loadMeta()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载院系/班级选项失败')
    return
  }
  createForm.username = ''
  createForm.displayName = ''
  createForm.password = ''
  createForm.enabled = true
  createForm.departmentId = undefined
  createForm.classId = undefined
  createForm.roleCodes = ['ROLE_STUDENT']
  createDialog.value = true
}

async function submitCreate() {
  try {
    await apiData<UserItem>(
      '/api/admin/users',
      {
        method: 'POST',
        body: {
          username: createForm.username,
          displayName: createForm.displayName,
          password: createForm.password || undefined,
          enabled: createForm.enabled,
          departmentId: createForm.departmentId ?? null,
          classId: createForm.classId ?? null,
          roleCodes: createForm.roleCodes,
        },
      },
      token.value,
    )
    ElMessage.success('创建成功')
    createDialog.value = false
    await loadMeta()
    await loadUsers()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '创建失败')
  }
}

async function openEdit(u: UserItem) {
  try {
    await loadMeta()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载院系/班级选项失败')
    return
  }
  editTarget.value = u
  editForm.displayName = u.displayName
  editForm.enabled = u.enabled
  editForm.departmentId = (u.departmentId ?? undefined) as any
  editForm.classId = (u.classId ?? undefined) as any
  editDialog.value = true
}

async function submitEdit() {
  if (!editTarget.value) return
  try {
    await apiData<UserItem>(
      `/api/admin/users/${editTarget.value.id}`,
      {
        method: 'PUT',
        body: {
          displayName: editForm.displayName,
          enabled: editForm.enabled,
          departmentId: editForm.departmentId ?? null,
          classId: editForm.classId ?? null,
        },
      },
      token.value,
    )
    ElMessage.success('更新成功')
    editDialog.value = false
    await loadMeta()
    await loadUsers()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '更新失败')
  }
}

function openRoles(u: UserItem) {
  rolesTarget.value = u
  rolesForm.roleCodes = [...(u.roleCodes ?? [])]
  rolesDialog.value = true
}

async function submitRoles() {
  if (!rolesTarget.value) return
  try {
    await apiData<void>(
      `/api/admin/users/${rolesTarget.value.id}/roles`,
      { method: 'PUT', body: { roleCodes: rolesForm.roleCodes } },
      token.value,
    )
    ElMessage.success('设置成功')
    rolesDialog.value = false
    await loadUsers()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '设置失败')
  }
}

async function resetPassword(u: UserItem) {
  try {
    const pw = (await ElMessageBox.prompt('输入新密码（留空则重置为 123456）', `重置密码: ${u.username}`, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '123456',
      inputType: 'password',
    })) as any
    const value = String(pw?.value ?? '').trim()
    await apiData<void>(
      `/api/admin/users/${u.id}/reset-password`,
      { method: 'POST', body: value ? { newPassword: value } : {} },
      token.value,
    )
    ElMessage.success('已重置')
  } catch (e: any) {
    if (e === 'cancel' || e === 'close') return
    ElMessage.error(e?.message ?? '重置失败')
  }
}

async function exportUsers() {
  try {
    await downloadBlob('/api/admin/users/export', { token: token.value, fallbackFilename: 'users.csv' })
    ElMessage.success('已下载 users.csv')
  } catch (e: any) {
    ElMessage.error(e?.message ?? '导出失败')
  }
}

function onPickFile(file: any) {
  importFile.value = file.raw ?? null
}

async function importUsers() {
  if (!importFile.value) {
    ElMessage.warning('先选择 CSV 文件')
    return
  }
  importing.value = true
  try {
    const fd = new FormData()
    fd.append('file', importFile.value)
    const result = await uploadFormData<any>('/api/admin/users/import', { token: token.value, formData: fd })
    ElMessage.success(`导入完成: total=${result.totalRows}, created=${result.created}, updated=${result.updated}, failed=${result.failed}`)
    importFile.value = null
    await loadMeta()
    await loadUsers()
  } catch (e: any) {
    ElMessage.error(e?.message ?? '导入失败')
  } finally {
    importing.value = false
  }
}

function onDepartmentChangeInQuery() {
  query.classId = undefined
}
function onDepartmentChangeInCreate() {
  createForm.classId = undefined
}
function onDepartmentChangeInEdit() {
  editForm.classId = undefined
}

onMounted(async () => {
  await loadMeta()
  await loadUsers()
})
</script>

<template>
  <div class="panel">
    <div class="toolbar">
      <el-input v-model="query.q" placeholder="搜索 username / displayName" clearable style="width: 260px" />
      <el-select v-model="query.roleCode" placeholder="角色" clearable style="width: 160px">
        <el-option v-for="r in roles" :key="r.code" :label="`${r.name} (${r.code})`" :value="r.code" />
      </el-select>
      <el-select v-model="query.enabled" placeholder="启用状态" clearable style="width: 120px">
        <el-option label="启用" value="true" />
        <el-option label="禁用" value="false" />
      </el-select>
      <el-select v-model="query.departmentId" placeholder="院系" clearable style="width: 160px" @change="onDepartmentChangeInQuery">
        <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" />
      </el-select>
      <el-select v-model="query.classId" placeholder="班级" clearable style="width: 200px">
        <el-option v-for="c in filteredClasses" :key="c.id" :label="`${c.departmentName} / ${c.name}`" :value="c.id" />
      </el-select>
      <el-button type="primary" :loading="loading" @click="() => { query.page = 1; loadUsers() }">查询</el-button>
      <el-button :loading="metaLoading" @click="openCreate">新建</el-button>
      <el-button @click="exportUsers">导出 CSV</el-button>
    </div>

    <div class="importRow">
      <el-upload :auto-upload="false" :show-file-list="true" :limit="1" accept=".csv" @change="onPickFile">
        <el-button>选择 CSV</el-button>
      </el-upload>
      <el-button type="success" :loading="importing" @click="importUsers">导入</el-button>
      <div class="hint">
        CSV 列: <code>username</code>, <code>displayName</code> 必填; 可选 <code>password</code>, <code>enabled</code>,
        <code>roleCodes</code>, <code>departmentName</code>, <code>className</code>
      </div>
    </div>

    <el-table v-loading="loading" :data="data.items" stripe height="520">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" width="160" />
      <el-table-column prop="displayName" label="姓名" width="160" />
      <el-table-column label="启用" width="90">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="角色" min-width="240">
        <template #default="{ row }">
          <el-tag v-for="r in row.roleCodes" :key="r" style="margin-right: 6px" size="small">{{ r }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="departmentName" label="院系" width="160" />
      <el-table-column prop="className" label="班级" width="180" />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" @click="openRoles(row)">角色</el-button>
          <el-button size="small" type="warning" @click="resetPassword(row)">重置密码</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        layout="prev, pager, next, sizes, total"
        :total="data.total"
        :page-size="query.size"
        :current-page="query.page"
        :page-sizes="[10, 20, 50, 100]"
        @update:current-page="(p:number) => { query.page = p; loadUsers() }"
        @update:page-size="(s:number) => { query.size = s; query.page = 1; loadUsers() }"
      />
    </div>

    <el-dialog v-model="createDialog" title="新建用户" width="560">
      <el-form label-width="120">
        <el-form-item label="username">
          <el-input v-model="createForm.username" placeholder="例如: s20221601" />
        </el-form-item>
        <el-form-item label="displayName">
          <el-input v-model="createForm.displayName" placeholder="例如: 张三" />
        </el-form-item>
        <el-form-item label="password">
          <el-input v-model="createForm.password" placeholder="留空默认 123456" show-password />
        </el-form-item>
        <el-form-item label="enabled">
          <el-switch v-model="createForm.enabled" />
        </el-form-item>
        <el-form-item label="院系">
          <el-select v-model="createForm.departmentId" :loading="metaLoading" clearable style="width: 100%" @change="onDepartmentChangeInCreate">
            <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="createForm.classId" :loading="metaLoading" clearable style="width: 100%">
            <el-option v-for="c in filteredClasses" :key="c.id" :label="`${c.departmentName} / ${c.name}`" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="roleCodes">
          <el-select v-model="createForm.roleCodes" :loading="metaLoading" multiple style="width: 100%">
            <el-option v-for="r in roles" :key="r.code" :label="`${r.name} (${r.code})`" :value="r.code" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialog = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editDialog" title="编辑用户" width="520">
      <div class="subtle" v-if="editTarget">
        user: <code>{{ editTarget.username }}</code> (id={{ editTarget.id }})
      </div>
      <el-form label-width="120">
        <el-form-item label="displayName">
          <el-input v-model="editForm.displayName" />
        </el-form-item>
        <el-form-item label="enabled">
          <el-switch v-model="editForm.enabled" />
        </el-form-item>
        <el-form-item label="院系">
          <el-select v-model="editForm.departmentId" :loading="metaLoading" clearable style="width: 100%" @change="onDepartmentChangeInEdit">
            <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="editForm.classId" :loading="metaLoading" clearable style="width: 100%">
            <el-option v-for="c in filteredClasses" :key="c.id" :label="`${c.departmentName} / ${c.name}`" :value="c.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialog = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rolesDialog" title="设置角色" width="520">
      <div class="subtle" v-if="rolesTarget">
        user: <code>{{ rolesTarget.username }}</code>
      </div>
      <el-form label-width="120">
        <el-form-item label="roleCodes">
          <el-select v-model="rolesForm.roleCodes" :loading="metaLoading" multiple style="width: 100%">
            <el-option v-for="r in roles" :key="r.code" :label="`${r.name} (${r.code})`" :value="r.code" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rolesDialog = false">取消</el-button>
        <el-button type="primary" @click="submitRoles">保存</el-button>
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
.importRow {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.hint {
  color: #666;
  font-size: 12px;
}
.pager {
  display: flex;
  justify-content: flex-end;
}
.subtle {
  margin-bottom: 10px;
  color: #666;
  font-size: 12px;
}
</style>
