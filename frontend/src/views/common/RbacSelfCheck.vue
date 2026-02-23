<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ApiError, apiData } from '../../api/http'
import { useAuthStore } from '../../stores/auth'

type CheckItem = {
  name: string
  expected: string
  result: string
  ok: boolean | null
}

const auth = useAuthStore()

const checks = ref<CheckItem[]>([
  { name: '未登录访问 /api/tasks', expected: '401 未登录', result: '-', ok: null },
  { name: '管理员创建任务', expected: '200 成功', result: '-', ok: null },
  { name: '管理员提交报告（应禁止）', expected: '403 无权限', result: '-', ok: null },
])

const anyRunning = ref(false)
const token = computed(() => auth.token)

function setResult(idx: number, ok: boolean, result: string) {
  const item = checks.value[idx]
  if (!item) return
  item.ok = ok
  item.result = result
}

async function run() {
  if (!token.value) {
    ElMessage.error('请先登录管理员账号')
    return
  }
  anyRunning.value = true
  try {
    // 1) 401 without token
    try {
      await apiData('/api/tasks', { method: 'GET' }, undefined)
      setResult(0, false, '意外成功')
    } catch (e: any) {
      const ok = e instanceof ApiError && e.status === 401
      setResult(0, ok, `${e?.status ?? '?'} ${e?.message ?? ''}`.trim())
    }

    // 2) admin can create task
    try {
      const t = await apiData<any>(
        '/api/tasks',
        { method: 'POST', body: { title: 'RBAC自检任务', description: '用于验证管理员权限' } },
        token.value,
      )
      setResult(1, true, `OK taskId=${t?.id ?? '?'}`)
    } catch (e: any) {
      setResult(1, false, `${e?.status ?? '?'} ${e?.message ?? ''}`.trim())
    }

    // 3) admin cannot submit
    try {
      await apiData<any>(
        '/api/tasks/1/submissions',
        { method: 'POST', body: { contentMd: '# admin forbidden' } },
        token.value,
      )
      setResult(2, false, '意外成功')
    } catch (e: any) {
      const ok = e instanceof ApiError && e.status === 403
      setResult(2, ok, `${e?.status ?? '?'} ${e?.message ?? ''}`.trim())
    }
  } finally {
    anyRunning.value = false
  }
}
</script>

<template>
  <el-card>
    <template #header>
      <div style="display: flex; align-items: center; justify-content: space-between">
        <div>权限自检（RBAC）</div>
        <el-button type="primary" :loading="anyRunning" @click="run">运行检查</el-button>
      </div>
    </template>

    <el-table :data="checks" size="small" style="width: 100%">
      <el-table-column prop="name" label="检查项" min-width="240" />
      <el-table-column prop="expected" label="期望" min-width="140" />
      <el-table-column prop="result" label="实际结果" min-width="220" />
      <el-table-column label="通过" width="90">
        <template #default="{ row }">
          <span v-if="row.ok === null">-</span>
          <span v-else-if="row.ok" style="color: #0a7b3b">PASS</span>
          <span v-else style="color: #b42318">FAIL</span>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
