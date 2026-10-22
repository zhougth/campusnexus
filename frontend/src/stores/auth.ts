import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import * as authApi from '@/api/auth'
import type { LoginDTO, RegisterDTO, UserVO } from '@/types/api'
import { clearToken, getToken, saveToken } from '@/utils/authStorage'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getToken())
  const user = ref<UserVO | null>(null)
  const loading = ref(false)

  const isLoggedIn = computed(() => Boolean(token.value))
  const isAdmin = computed(() => user.value?.role === 1)

  function setToken(value: string): void {
    token.value = value
    saveToken(value)
  }

  function clearSession(): void {
    token.value = null
    user.value = null
    clearToken()
  }

  async function login(dto: LoginDTO): Promise<void> {
    const issuedToken = await authApi.login(dto)
    setToken(issuedToken)
    await fetchCurrentUser()
  }

  async function register(dto: RegisterDTO): Promise<void> {
    await authApi.register(dto)
  }

  let pendingFetch: Promise<UserVO | null> | null = null

  async function fetchCurrentUser(): Promise<UserVO | null> {
    if (!token.value) {
      user.value = null
      return null
    }
    if (pendingFetch) {
      return pendingFetch
    }
    loading.value = true
    pendingFetch = (async () => {
      try {
        const profile = await authApi.fetchMe()
        user.value = profile
        return profile
      } catch {
        user.value = null
        return null
      } finally {
        loading.value = false
      }
    })()
    try {
      return await pendingFetch
    } finally {
      pendingFetch = null
    }
  }

  async function ensureUser(): Promise<UserVO | null> {
    if (user.value) {
      return user.value
    }
    return fetchCurrentUser()
  }

  async function logout(): Promise<void> {
    try {
      if (token.value) {
        await authApi.logout()
      }
    } finally {
      clearSession()
    }
  }

  function updateLocalUser(patch: Partial<UserVO>): void {
    if (user.value) {
      user.value = { ...user.value, ...patch }
    }
  }

  return {
    token,
    user,
    loading,
    isLoggedIn,
    isAdmin,
    login,
    register,
    fetchCurrentUser,
    ensureUser,
    logout,
    clearSession,
    updateLocalUser,
  }
})
