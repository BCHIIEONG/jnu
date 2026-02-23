import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'

import LoginView from '../views/LoginView.vue'
import StudentHome from '../views/student/StudentHome.vue'
import TeacherHome from '../views/teacher/TeacherHome.vue'
import AdminHome from '../views/admin/AdminHome.vue'

type Role = 'ROLE_STUDENT' | 'ROLE_TEACHER' | 'ROLE_ADMIN'

declare module 'vue-router' {
  interface RouteMeta {
    public?: boolean
    roles?: Role[]
  }
}

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: LoginView, meta: { public: true } },
  { path: '/student', component: StudentHome, meta: { roles: ['ROLE_STUDENT'] } },
  { path: '/teacher', component: TeacherHome, meta: { roles: ['ROLE_TEACHER', 'ROLE_ADMIN'] } },
  { path: '/admin', component: AdminHome, meta: { roles: ['ROLE_ADMIN'] } },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.meta.public) return true

  if (!auth.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  try {
    await auth.ensureMe()
  } catch (e) {
    auth.logout()
    ElMessage.error('登录已失效，请重新登录')
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  const requiredRoles = to.meta.roles
  if (requiredRoles && requiredRoles.length > 0) {
    const ok = requiredRoles.some((r) => auth.hasRole(r))
    if (!ok) {
      ElMessage.error('无权限访问该页面')
      return auth.suggestHomePath()
    }
  }

  return true
})

