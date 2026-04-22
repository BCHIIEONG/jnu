<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { apiData } from '../api/http'
import { useAuthStore } from '../stores/auth'
import UiModeToggle from './common/UiModeToggle.vue'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const LS_REMEMBER = 'labflow_login_remember'
const LS_USERNAME = 'labflow_login_username'
const LS_PASSWORD = 'labflow_login_password'

const form = reactive({
  username: '',
  password: '',
})

const remember = ref(false)
const forceDialogVisible = ref(false)
const forceChanging = ref(false)

const forceForm = reactive({
  username: '',
  displayName: '',
  newPassword: '',
  confirmPassword: '',
})

onMounted(async () => {
  try {
    remember.value = localStorage.getItem(LS_REMEMBER) === '1'
    if (remember.value) {
      form.username = localStorage.getItem(LS_USERNAME) ?? ''
      form.password = localStorage.getItem(LS_PASSWORD) ?? ''
    }
  } catch {
    // ignore
  }
  await openForceChangeIfNeeded()
})

function clearRememberedLogin() {
  try {
    localStorage.removeItem(LS_REMEMBER)
    localStorage.removeItem(LS_USERNAME)
    localStorage.removeItem(LS_PASSWORD)
  } catch {
    // ignore
  }
  remember.value = false
}

function prepareForceForm() {
  forceForm.username = auth.user?.username ?? form.username
  forceForm.displayName = ''
  forceForm.newPassword = ''
  forceForm.confirmPassword = ''
}

async function openForceChangeIfNeeded() {
  if (!auth.token) return
  try {
    await auth.ensureMe()
    if (auth.user?.mustChangePassword) {
      prepareForceForm()
      clearRememberedLogin()
      forceDialogVisible.value = true
    }
  } catch {
    auth.logout()
  }
}

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  try {
    await auth.login(form.username, form.password)
    if (auth.user?.mustChangePassword) {
      prepareForceForm()
      clearRememberedLogin()
      ElMessage.warning('首次登录请先修改默认密码')
      forceDialogVisible.value = true
      return
    }
    try {
      if (remember.value) {
        localStorage.setItem(LS_REMEMBER, '1')
        localStorage.setItem(LS_USERNAME, form.username)
        localStorage.setItem(LS_PASSWORD, form.password)
      } else {
        localStorage.removeItem(LS_REMEMBER)
        localStorage.removeItem(LS_USERNAME)
        localStorage.removeItem(LS_PASSWORD)
      }
    } catch {
      // ignore
    }
    ElMessage.success(`欢迎，${auth.displayName}`)
    const redirect = (route.query.redirect as string | undefined) ?? auth.suggestHomePath()
    await router.replace(redirect)
  } catch (e: any) {
    ElMessage.error(e?.message ?? '登录失败')
  }
}

async function submitForceChange() {
  if (!forceForm.username || !forceForm.displayName || !forceForm.newPassword || !forceForm.confirmPassword) {
    ElMessage.warning('请填写学号、姓名和新密码')
    return
  }
  if (forceForm.newPassword !== forceForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  if (forceForm.newPassword.length < 6) {
    ElMessage.warning('新密码长度至少 6 位')
    return
  }
  if (forceForm.newPassword === '123456') {
    ElMessage.warning('新密码不能使用默认密码')
    return
  }
  if (!auth.token) {
    ElMessage.error('登录状态已失效，请重新登录')
    forceDialogVisible.value = false
    return
  }
  forceChanging.value = true
  try {
    await apiData<void>('/api/auth/force-change-password', {
      method: 'POST',
      body: {
        username: forceForm.username,
        displayName: forceForm.displayName,
        newPassword: forceForm.newPassword,
        confirmPassword: forceForm.confirmPassword,
      },
    }, auth.token)
    ElMessage.success('密码修改成功，请重新登录')
    auth.logout()
    clearRememberedLogin()
    form.password = ''
    forceDialogVisible.value = false
    await router.replace('/login')
  } catch (e: any) {
    ElMessage.error(e?.message ?? '密码修改失败')
  } finally {
    forceChanging.value = false
  }
}
</script>

<template>
  <div class="page">
    <div class="card">
      <h1>实验流程与实验报告管理系统</h1>
      <p class="sub">登录后将按角色进入学生/教师/管理员页面</p>
      <el-form @submit.prevent="submit" label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="form.username" @keyup.enter="submit" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            show-password
            @keyup.enter="submit"
          />
        </el-form-item>
        <div class="modeRow">
          <span class="modeLabel">界面模式</span>
          <UiModeToggle />
        </div>
        <div class="rememberRow">
          <el-checkbox v-model="remember">记住账号密码</el-checkbox>
        </div>
        <el-button type="primary" native-type="submit" :loading="auth.loading" style="width: 100%">登录</el-button>
      </el-form>
    </div>

    <el-dialog
      v-model="forceDialogVisible"
      title="首次登录请修改默认密码"
      width="460px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
    >
      <p class="forceTip">请填写当前登录账号对应的学号和姓名。系统只会修改当前登录账号的密码。</p>
      <el-form label-position="top" @submit.prevent="submitForceChange">
        <el-form-item label="学号">
          <el-input v-model="forceForm.username" autocomplete="off" />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="forceForm.displayName" autocomplete="off" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="forceForm.newPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input
            v-model="forceForm.confirmPassword"
            type="password"
            show-password
            autocomplete="new-password"
            @keyup.enter="submitForceChange"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" :loading="forceChanging" @click="submitForceChange">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  min-height: 100dvh;
  display: grid;
  place-items: center;
  padding: 24px;
}
.card {
  width: 100%;
  max-width: 520px;
  background: #fff;
  border: 1px solid #e6e6e6;
  border-radius: 14px;
  padding: 22px;
}
h1 {
  font-size: 20px;
  margin: 0 0 6px;
}
.sub {
  margin: 0 0 16px;
  color: #666;
  font-size: 13px;
}
.modeRow {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
  margin: 4px 0 12px;
}
.modeLabel {
  color: #666;
  font-size: 13px;
  white-space: nowrap;
}
.rememberRow {
  display: flex;
  align-items: center;
  margin: 0 0 12px;
}
.forceTip {
  margin: 0 0 12px;
  color: #666;
  font-size: 13px;
  line-height: 1.6;
}
</style>
