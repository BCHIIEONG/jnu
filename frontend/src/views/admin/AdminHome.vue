<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import RbacSelfCheck from '../common/RbacSelfCheck.vue'
import UiModeToggle from '../common/UiModeToggle.vue'
import UsersPanel from './panels/UsersPanel.vue'
import DepartmentsPanel from './panels/DepartmentsPanel.vue'
import ClassesPanel from './panels/ClassesPanel.vue'
import TimeSlotsPanel from './panels/TimeSlotsPanel.vue'
import CourseSchedulesPanel from './panels/CourseSchedulesPanel.vue'
import LabRoomsPanel from './panels/LabRoomsPanel.vue'
import DevicesPanel from './panels/DevicesPanel.vue'
import SemestersPanel from './panels/SemestersPanel.vue'
import AuditLogsPanel from './panels/AuditLogsPanel.vue'
import BackupPanel from './panels/BackupPanel.vue'

const auth = useAuthStore()
const router = useRouter()

function logout() {
  auth.logout()
  router.replace('/login')
}
</script>

<template>
  <el-container class="layout">
    <el-header class="header">
      <div class="brand">管理员端</div>
      <div class="right">
        <span class="user">{{ auth.displayName }}</span>
        <UiModeToggle />
        <el-button size="small" @click="logout">退出</el-button>
      </div>
    </el-header>
    <el-main class="main">
      <el-card shadow="never" class="block">
        <el-tabs tab-position="top" class="tabs">
          <el-tab-pane label="用户管理">
            <UsersPanel />
          </el-tab-pane>
          <el-tab-pane label="院系管理">
            <DepartmentsPanel />
          </el-tab-pane>
          <el-tab-pane label="班级管理">
            <ClassesPanel />
          </el-tab-pane>
          <el-tab-pane label="节次管理">
            <TimeSlotsPanel />
          </el-tab-pane>
          <el-tab-pane label="课表管理">
            <CourseSchedulesPanel />
          </el-tab-pane>
          <el-tab-pane label="实验室信息">
            <LabRoomsPanel />
          </el-tab-pane>
          <el-tab-pane label="设备台账">
            <DevicesPanel />
          </el-tab-pane>
          <el-tab-pane label="学期设置">
            <SemestersPanel />
          </el-tab-pane>
          <el-tab-pane label="操作日志">
            <AuditLogsPanel />
          </el-tab-pane>
          <el-tab-pane label="备份与导出">
            <BackupPanel />
          </el-tab-pane>
          <el-tab-pane label="RBAC 自检">
            <RbacSelfCheck />
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </el-main>
  </el-container>
</template>

<style scoped>
.layout {
  min-height: 100vh;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #eee;
  background: #fff;
}
.brand {
  font-weight: 700;
}
.right {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
.main {
  padding: 14px;
}
.block {
  margin-bottom: 14px;
}
.tabs :deep(.el-tabs__content) {
  padding-top: 8px;
}
</style>
