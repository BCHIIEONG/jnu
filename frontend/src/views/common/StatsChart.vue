<script setup lang="ts">
import * as echarts from 'echarts'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'

type ChartSeries = {
  name: string
  type: string
  data: number[]
}

type ChartData = {
  categories: string[]
  series: ChartSeries[]
}

const props = defineProps<{
  title: string
  chart: ChartData
}>()

const elRef = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null
let resizeObserver: ResizeObserver | null = null

const option = computed<echarts.EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  legend: { top: 0 },
  grid: { left: 36, right: 18, top: 48, bottom: 28 },
  xAxis: {
    type: 'category',
    data: props.chart.categories,
    axisLabel: { hideOverlap: true },
  },
  yAxis: { type: 'value' },
  series: props.chart.series.map((item) => ({
    name: item.name,
    type: item.type === 'bar' ? 'bar' : 'line',
    data: item.data,
    smooth: item.type !== 'bar',
  })),
}))

function renderChart() {
  if (!elRef.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(elRef.value)
  }
  chartInstance.setOption(option.value)
  chartInstance.resize()
}

onMounted(() => {
  renderChart()
  if (typeof ResizeObserver !== 'undefined' && elRef.value) {
    resizeObserver = new ResizeObserver(() => {
      chartInstance?.resize()
    })
    resizeObserver.observe(elRef.value)
  } else {
    window.addEventListener('resize', renderChart)
  }
})

watch(option, () => {
  renderChart()
}, { deep: true })

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  resizeObserver = null
  window.removeEventListener('resize', renderChart)
  chartInstance?.dispose()
  chartInstance = null
})
</script>

<template>
  <el-card shadow="never" class="chartCard">
    <template #header>
      <div class="chartTitle">{{ title }}</div>
    </template>
    <div ref="elRef" class="chartCanvas" />
  </el-card>
</template>

<style scoped>
.chartCard {
  min-height: 360px;
}

.chartTitle {
  font-weight: 600;
}

.chartCanvas {
  width: 100%;
  height: 300px;
}
</style>
