<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useUiStore } from '../../stores/ui'

const ui = useUiStore()

onMounted(() => {
  ui.init()
})

const compact = ref(false)
let media: MediaQueryList | null = null
function updateCompact() {
  compact.value = media ? media.matches : false
}

onMounted(() => {
  media = window.matchMedia('(max-width: 768px)')
  updateCompact()
  if (typeof media.addEventListener === 'function') {
    media.addEventListener('change', updateCompact)
  } else {
    // Safari old versions
    ;(media as any).addListener(updateCompact)
  }
})

onBeforeUnmount(() => {
  if (!media) return
  if (typeof media.removeEventListener === 'function') {
    media.removeEventListener('change', updateCompact)
  } else {
    ;(media as any).removeListener(updateCompact)
  }
})

const value = computed({
  get: () => ui.mode,
  set: (v) => ui.setMode(v),
})
</script>

<template>
  <el-dropdown v-if="compact" trigger="click">
    <el-button size="small">
      模式：{{ value === 'auto' ? '自动' : value === 'desktop' ? '桌面' : '手机' }}
    </el-button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item @click="ui.setMode('auto')">自动</el-dropdown-item>
        <el-dropdown-item @click="ui.setMode('desktop')">桌面版</el-dropdown-item>
        <el-dropdown-item @click="ui.setMode('mobile')">手机版</el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
  <el-radio-group v-else v-model="value" size="small">
    <el-radio-button label="auto">自动</el-radio-button>
    <el-radio-button label="desktop">桌面版</el-radio-button>
    <el-radio-button label="mobile">手机版</el-radio-button>
  </el-radio-group>
</template>
