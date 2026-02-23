<script setup lang="ts">
import { reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const form = reactive({
  username: '',
  password: '',
})

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  try {
    await auth.login(form.username, form.password)
    ElMessage.success(`欢迎，${auth.displayName}`)
    const redirect = (route.query.redirect as string | undefined) ?? auth.suggestHomePath()
    await router.replace(redirect)
  } catch (e: any) {
    ElMessage.error(e?.message ?? '登录失败')
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
          <el-input v-model="form.username" placeholder="teacher / student / admin" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" placeholder="teacher123 / student123 / admin123" show-password />
        </el-form-item>
        <el-button type="primary" :loading="auth.loading" @click="submit" style="width: 100%">登录</el-button>
      </el-form>
      <div class="hint">
        <div>演示账号：</div>
        <div><code>teacher / teacher123</code>（教师）</div>
        <div><code>student / student123</code>（学生）</div>
        <div><code>admin / admin123</code>（管理员）</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
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
.hint {
  margin-top: 16px;
  font-size: 13px;
  color: #444;
  line-height: 1.6;
}
code {
  background: #f3f3f3;
  padding: 2px 6px;
  border-radius: 6px;
}
</style>

