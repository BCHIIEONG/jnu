<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { apiData } from '../../api/http'
import { useAuthStore } from '../../stores/auth'

type CheckinResponse = {
  recordId: number
  alreadyCheckedIn: boolean
  checkedInAt: string
}

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const status = ref<'idle' | 'loading' | 'ok' | 'fail'>('idle')
const message = ref('')
const data = ref<CheckinResponse | null>(null)

async function run() {
  const token = (route.query.t as string | undefined) ?? ''
  if (!token) {
    status.value = 'fail'
    message.value = '缺少签到 token（t）'
    return
  }

  status.value = 'loading'
  message.value = '正在签到...'
  try {
    data.value = await apiData<CheckinResponse>('/api/attendance/checkin', { method: 'POST', body: { token } }, auth.token)
    status.value = 'ok'
    message.value = data.value.alreadyCheckedIn ? '已签到（重复扫码）' : '签到成功'
  } catch (e: any) {
    status.value = 'fail'
    message.value = e?.message ?? '签到失败'
  }
}

function backToStudent() {
  router.replace('/student')
}

onMounted(async () => {
  // Router guard already ensures login; this is a double-check for robustness.
  if (!auth.token) {
    ElMessage.error('请先登录')
    await router.replace({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  await run()
})
</script>

<template>
  <div class="page">
    <div class="card">
      <h2>实验签到</h2>
      <p class="meta">当前用户：{{ auth.displayName }}</p>

      <div v-if="status === 'loading'" class="box">正在提交签到，请稍等...</div>
      <div v-else-if="status === 'ok'" class="box ok">
        <div class="title">{{ message }}</div>
        <div class="meta">时间：{{ data?.checkedInAt }}</div>
      </div>
      <div v-else-if="status === 'fail'" class="box fail">
        <div class="title">签到失败</div>
        <div class="meta">{{ message }}</div>
      </div>
      <div v-else class="box">准备签到</div>

      <div class="actions">
        <el-button size="small" @click="run">重试</el-button>
        <el-button size="small" type="primary" @click="backToStudent">返回学生端</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 16px;
  background: #f7f7f7;
}
.card {
  width: 100%;
  max-width: 520px;
  background: #fff;
  border: 1px solid #e6e6e6;
  border-radius: 14px;
  padding: 16px;
}
h2 {
  margin: 0 0 6px;
  font-size: 18px;
}
.meta {
  margin: 0;
  font-size: 12px;
  color: #666;
}
.box {
  margin-top: 12px;
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 12px;
  background: #fafafa;
}
.box.ok {
  border-color: #c6f6d5;
  background: #f0fff4;
}
.box.fail {
  border-color: #fed7d7;
  background: #fff5f5;
}
.title {
  font-weight: 700;
  margin-bottom: 6px;
}
.actions {
  display: flex;
  gap: 10px;
  margin-top: 12px;
  justify-content: flex-end;
}
</style>

