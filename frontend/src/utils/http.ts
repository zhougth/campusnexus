import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

import type { Result } from '@/types/api'
import { clearToken, getToken } from './authStorage'
import { ApiError, BackendNotImplementedError, BackendUnreachableError } from './errors'

declare module 'axios' {
  export interface AxiosRequestConfig {
    silent?: boolean
  }
}

const http = axios.create({
  baseURL: '/api',
  timeout: 20000,
  validateStatus: () => true,
})

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

function isResult(body: unknown): body is Result<unknown> {
  return (
    typeof body === 'object' &&
    body !== null &&
    typeof (body as Result<unknown>).code === 'number' &&
    'message' in body
  )
}

function notifyUnauthorized(): void {
  clearToken()
  window.dispatchEvent(new Event('campusnexus:unauthorized'))
}

http.interceptors.response.use(
  // 拦截器统一把 Result.data 解包成返回值
  (response): any => {
    const config = response.config
    const silent = config.silent === true
    const status = response.status
    const body: unknown = response.data

    if (isResult(body) && (body.code === 200 || body.code === 202)) {
      return body.data
    }

    if (status === 401 || (isResult(body) && (body.code === 10004 || body.code === 10005))) {
      notifyUnauthorized()
      if (!silent) {
        ElMessage.warning('登录已过期，请重新登录')
      }
      return Promise.reject(
        new ApiError(isResult(body) ? body.message : '未授权，请先登录', { code: 401, status: 401 }),
      )
    }

    if (status === 403) {
      if (!silent) {
        ElMessage.error(isResult(body) ? body.message : '无权限访问')
      }
      return Promise.reject(new ApiError(isResult(body) ? body.message : '无权限访问', { code: 403, status }))
    }

    if (status === 422 || (isResult(body) && body.code === 422)) {
      const fieldErrors =
        isResult(body) && body.data && typeof body.data === 'object'
          ? (body.data as Record<string, string>)
          : {}
      if (!silent) {
        ElMessage.error(isResult(body) ? body.message : '参数校验失败')
      }
      return Promise.reject(
        new ApiError(isResult(body) ? body.message : '参数校验失败', { code: 422, status, fieldErrors }),
      )
    }

    if (
      (status === 404 &&
        isResult(body) &&
        body.code === 404 &&
        (body.message === '资源不存在' || body.message === 'Not Found')) ||
      status === 501
    ) {
      const url = config.url ?? ''
      return Promise.reject(
        new BackendNotImplementedError(
          (config.method ?? 'GET').toUpperCase(),
          url.startsWith('/api') ? url : `/api${url}`,
        ),
      )
    }

    if (status === 429) {
      if (!silent) {
        ElMessage.warning(isResult(body) ? body.message : '请求过于频繁，请稍后再试')
      }
      return Promise.reject(new ApiError(isResult(body) ? body.message : '请求过于频繁', { status, code: 429 }))
    }

    if (!isResult(body) && status >= 500) {
      return Promise.reject(new BackendUnreachableError())
    }

    if (status >= 500) {
      if (!silent) {
        ElMessage.error(isResult(body) ? body.message : '服务器内部错误，请稍后重试')
      }
      return Promise.reject(
        new ApiError(isResult(body) ? body.message : '服务器内部错误', { code: 500, status }),
      )
    }

    if (!silent) {
      ElMessage.error(isResult(body) ? body.message : '请求失败，请稍后重试')
    }
    return Promise.reject(
      new ApiError(isResult(body) ? body.message : '请求失败', {
        code: isResult(body) ? body.code : status,
        status,
      }),
    )
  },
  (error: unknown) => {
    if (axios.isCancel(error)) {
      return Promise.reject(error)
    }
    return Promise.reject(new BackendUnreachableError())
  },
)

export function request<T>(config: AxiosRequestConfig): Promise<T> {
  return http.request(config) as unknown as Promise<T>
}
