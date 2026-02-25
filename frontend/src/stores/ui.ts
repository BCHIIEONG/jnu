import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

export type UiMode = 'auto' | 'desktop' | 'mobile'
export type EffectiveUiMode = 'desktop' | 'mobile'

const STORAGE_KEY = 'ui.mode'

function readStoredMode(): UiMode {
  try {
    const v = localStorage.getItem(STORAGE_KEY)
    if (v === 'auto' || v === 'desktop' || v === 'mobile') return v
  } catch {
    // ignore
  }
  return 'auto'
}

function writeStoredMode(mode: UiMode) {
  try {
    localStorage.setItem(STORAGE_KEY, mode)
  } catch {
    // ignore
  }
}

function isMobileNow(): boolean {
  if (typeof window === 'undefined') return false
  return window.matchMedia('(max-width: 768px)').matches
}

function applyToDom(mode: EffectiveUiMode) {
  if (typeof document === 'undefined') return
  document.documentElement.dataset.ui = mode
}

export const useUiStore = defineStore('ui', () => {
  const mode = ref<UiMode>('auto')
  const viewportMobile = ref(false)
  const initialized = ref(false)

  const effectiveMode = computed<EffectiveUiMode>(() => {
    if (mode.value === 'desktop') return 'desktop'
    if (mode.value === 'mobile') return 'mobile'
    return viewportMobile.value ? 'mobile' : 'desktop'
  })

  function setMode(next: UiMode) {
    mode.value = next
    writeStoredMode(next)
    applyToDom(effectiveMode.value)
  }

  function init() {
    if (initialized.value) return
    initialized.value = true

    mode.value = readStoredMode()
    viewportMobile.value = isMobileNow()
    applyToDom(effectiveMode.value)

    window.addEventListener('resize', () => {
      viewportMobile.value = isMobileNow()
      applyToDom(effectiveMode.value)
    })
  }

  return { mode, effectiveMode, init, setMode, initialized }
})

