<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useUiStore } from '../../stores/ui'

const ui = useUiStore()

onMounted(() => {
  ui.init()
})

const value = computed({
  get: () => ui.mode,
  set: (v) => ui.setMode(v),
})
</script>

<template>
  <el-radio-group v-model="value" size="small">
    <el-radio-button label="auto">自动</el-radio-button>
    <el-radio-button label="desktop">桌面版</el-radio-button>
    <el-radio-button label="mobile">手机版</el-radio-button>
  </el-radio-group>
</template>

