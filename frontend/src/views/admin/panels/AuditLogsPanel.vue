<script setup lang="ts">
import { onMounted, reactive, ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { apiData, downloadBlob } from '../../../api/http'
import { useAuthStore } from '../../../stores/auth'

type PageResult<T> = { page: number; size: number; total: number; items: T[] }
type AuditLog = {
  id: number
  actorId: number
  actorUsername: string
  action: string
  targetType?: string | null
  targetId?: number | null
  detailJson?: string | null
  createdAt: string
}

const auth = useAuthStore()
const token = computed(() => auth.token)

const loading = ref(false)
const data = ref<PageResult<AuditLog>>({ page: 1, size: 20, total: 0, items: [] })

const query = reactive({
  action: '',
  actorUsername: '',
  targetType: '',
  from: '',
  to: '',
  page: 1,
  size: 20,
})

function buildQuery(includePaging: boolean) {
  const p = new URLSearchParams()
  if (query.action) p.set('action', query.action)
  if (query.actorUsername) p.set('actorUsername', query.actorUsername)
  if (query.targetType) p.set('targetType', query.targetType)
  if (query.from) p.set('from', query.from)
  if (query.to) p.set('to', query.to)
  if (includePaging) {
    p.set('page', String(query.page))
    p.set('size', String(query.size))
  }
  return p.toString()
}

async function load() {
  loading.value = true
  try {
    data.value = await apiData<PageResult<AuditLog>>(`/api/admin/audit-logs?${buildQuery(true)}`, { method: 'GET' }, token.value)
  } catch (e: any) {
    ElMessage.error(e?.message ?? '加载失败')
  } finally {
    loading.value = false
  }
}

async function exportCsv() {
  try {
    await downloadBlob(`/api/admin/audit-logs/export?${buildQuery(false)}`, {
      token: token.value,
      fallbackFilename: 'audit-logs.csv',
    })
    ElMessage.success('已下载 audit-logs.csv')
  } catch (e: any) {
    ElMessage.error(e?.message ?? '导出失败')
  }
}

onMounted(load)
</script>

<template>
  <div class="panel">
    <div class="toolbar">
      <el-input v-model="query.action" placeholder="action (例如 USER_CREATE)" clearable style="width: 220px" />
      <el-input v-model="query.actorUsername" placeholder="actorUsername" clearable style="width: 180px" />
      <el-input v-model="query.targetType" placeholder="targetType (例如 sys_user)" clearable style="width: 180px" />
      <el-date-picker
        v-model="query.from"
        type="datetime"
        value-format="YYYY-MM-DDTHH:mm:ss"
        placeholder="from"
        style="width: 210px"
        clearable
      />
      <el-date-picker
        v-model="query.to"
        type="datetime"
        value-format="YYYY-MM-DDTHH:mm:ss"
        placeholder="to"
        style="width: 210px"
        clearable
      />
      <el-button type="primary" :loading="loading" @click="() => { query.page = 1; load() }">查询</el-button>
      <el-button @click="exportCsv">导出 CSV</el-button>
    </div>

    <el-table :data="data.items" v-loading="loading" stripe height="520">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="createdAt" label="时间" width="180" />
      <el-table-column prop="actorUsername" label="操作者" width="140" />
      <el-table-column prop="action" label="动作" width="200" />
      <el-table-column prop="targetType" label="目标类型" width="140" />
      <el-table-column prop="targetId" label="目标ID" width="100" />
      <el-table-column label="detailJson">
        <template #default="{ row }">
          <el-tooltip v-if="row.detailJson" :content="row.detailJson" placement="top">
            <span class="json">{{ row.detailJson }}</span>
          </el-tooltip>
          <span v-else class="muted">-</span>
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
        @update:current-page="(p:number) => { query.page = p; load() }"
        @update:page-size="(s:number) => { query.size = s; query.page = 1; load() }"
      />
    </div>
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
.pager {
  display: flex;
  justify-content: flex-end;
}
.json {
  display: inline-block;
  max-width: 520px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.muted {
  color: #888;
}
</style>

