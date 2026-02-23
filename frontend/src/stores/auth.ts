import { defineStore } from 'pinia'
import { apiData } from '../api/http'

export type UserProfile = {
  id: number
  username: string
  displayName: string
  roles: string[]
}

type LoginResponse = {
  token: string
  tokenType: string
  expiresAt: string
  user: UserProfile
}

type CurrentUserResponse = {
  user: UserProfile
}

const LS_TOKEN = 'labflow_token'
const LS_USER = 'labflow_user'

function readJson<T>(key: string): T | null {
  try {
    const raw = localStorage.getItem(key)
    return raw ? (JSON.parse(raw) as T) : null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(LS_TOKEN) ?? '',
    user: readJson<UserProfile>(LS_USER) as UserProfile | null,
    loading: false,
  }),
  getters: {
    isAuthed: (s) => Boolean(s.token),
    roles: (s) => s.user?.roles ?? [],
    displayName: (s) => s.user?.displayName ?? s.user?.username ?? '',
  },
  actions: {
    hasRole(role: string): boolean {
      return this.roles.includes(role)
    },
    suggestHomePath(): string {
      if (this.hasRole('ROLE_ADMIN')) return '/admin'
      if (this.hasRole('ROLE_TEACHER')) return '/teacher'
      return '/student'
    },
    async login(username: string, password: string): Promise<void> {
      this.loading = true
      try {
        const data = await apiData<LoginResponse>('/api/auth/login', { method: 'POST', body: { username, password } })
        this.token = data.token
        localStorage.setItem(LS_TOKEN, this.token)
        await this.fetchMe()
      } finally {
        this.loading = false
      }
    },
    async fetchMe(): Promise<void> {
      if (!this.token) throw new Error('No token')
      const data = await apiData<CurrentUserResponse>('/api/auth/me', { method: 'GET' }, this.token)
      this.user = data.user
      localStorage.setItem(LS_USER, JSON.stringify(this.user))
    },
    async ensureMe(): Promise<void> {
      if (!this.token) return
      if (this.user) return
      await this.fetchMe()
    },
    logout(): void {
      this.token = ''
      this.user = null
      localStorage.removeItem(LS_TOKEN)
      localStorage.removeItem(LS_USER)
    },
  },
})

