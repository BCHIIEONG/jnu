<script setup lang="ts">
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { downloadBlob } from '../../../api/http'
import { useAuthStore } from '../../../stores/auth'

const auth = useAuthStore()
const token = computed(() => auth.token)

async function dl(path: string, filename: string) {
  try {
    await downloadBlob(path, { token: token.value, fallbackFilename: filename })
    ElMessage.success(`已下载 ${filename}`)
  } catch (e: any) {
    ElMessage.error(e?.message ?? '下载失败')
  }
}
</script>

<template>
  <div class="panel">
    <div class="grid">
      <el-card shadow="never">
        <template #header>数据导出（CSV / Excel）</template>
        <div class="btns">
          <el-button @click="dl('/api/admin/users/export', 'users.csv')">导出用户</el-button>
          <el-button type="primary" plain @click="dl('/api/admin/users/export/excel', 'users.xlsx')">用户 Excel</el-button>
          <el-button @click="dl('/api/admin/devices/export', 'devices.csv')">导出设备</el-button>
          <el-button type="primary" plain @click="dl('/api/admin/devices/export/excel', 'devices.xlsx')">设备 Excel</el-button>
          <el-button @click="dl('/api/admin/departments/export', 'departments.csv')">导出院系</el-button>
          <el-button type="primary" plain @click="dl('/api/admin/departments/export/excel', 'departments.xlsx')">院系 Excel</el-button>
          <el-button @click="dl('/api/admin/classes/export', 'classes.csv')">导出班级</el-button>
          <el-button type="primary" plain @click="dl('/api/admin/classes/export/excel', 'classes.xlsx')">班级 Excel</el-button>
          <el-button @click="dl('/api/admin/lab-rooms/export', 'lab-rooms.csv')">导出实验室</el-button>
          <el-button type="primary" plain @click="dl('/api/admin/lab-rooms/export/excel', 'lab-rooms.xlsx')">实验室 Excel</el-button>
          <el-button @click="dl('/api/admin/semesters/export', 'semesters.csv')">导出学期</el-button>
          <el-button type="primary" plain @click="dl('/api/admin/semesters/export/excel', 'semesters.xlsx')">学期 Excel</el-button>
          <el-button @click="dl('/api/admin/audit-logs/export', 'audit-logs.csv')">导出操作日志</el-button>
          <el-button type="primary" plain @click="dl('/api/admin/audit-logs/export/excel', 'audit-logs.xlsx')">日志 Excel</el-button>
        </div>
      </el-card>

      <el-card shadow="never">
        <template #header>备份/恢复流程</template>
        <div class="text">
          <div>1) 备份数据库（示例命令，按你机器实际路径调整）</div>
          <pre class="pre">C:\xampp\mysql\bin\mysqldump.exe -u root -p --databases lab_flow_report &gt; lab_flow_report_backup.sql</pre>
          <div>2) 恢复数据库（示例）</div>
          <pre class="pre">C:\xampp\mysql\bin\mysql.exe -u root -p &lt; lab_flow_report_backup.sql</pre>
          <div>3) 说明：应用启动会通过 Flyway 自动建表/补迁移；CSV 导出用于“可读的数据交付/证据”。</div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}
.btns {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.text {
  color: #333;
  line-height: 1.7;
}
.pre {
  margin: 6px 0 10px;
  padding: 10px;
  background: #f6f8fa;
  border: 1px solid #eee;
  border-radius: 6px;
  white-space: pre-wrap;
}
</style>
