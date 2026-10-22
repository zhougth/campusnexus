import type { ChatEvent } from '@/types/api'
import { clearToken, getToken } from './authStorage'
import { ApiError, BackendNotImplementedError, BackendUnreachableError } from './errors'
import type { Result } from '@/types/api'

export interface ChatStreamHandlers {
  onEvent: (event: ChatEvent) => void
}

function isResult(body: unknown): body is Result<unknown> {
  return (
    typeof body === 'object' &&
    body !== null &&
    typeof (body as Result<unknown>).code === 'number'
  )
}

async function buildHttpError(response: Response): Promise<ApiError> {
  let body: unknown = null
  try {
    body = await response.json()
  } catch {
    body = null
  }

  if (response.status === 401) {
    clearToken()
    window.dispatchEvent(new Event('campusnexus:unauthorized'))
    return new ApiError(isResult(body) ? body.message : '登录已过期，请重新登录', { status: 401 })
  }
  if (response.status === 403) {
    return new ApiError(isResult(body) ? body.message : '无权限访问', { status: 403 })
  }
  if (response.status === 404) {
    if (isResult(body) && body.code === 404 && body.message === '资源不存在') {
      return new BackendNotImplementedError('GET', '/api/chat/stream')
    }
    return new ApiError(isResult(body) ? body.message : '会话不存在', { status: 404 })
  }
  if (response.status === 429) {
    return new ApiError(isResult(body) ? body.message : '提问过于频繁，请稍后再试', {
      status: 429,
      code: 429,
    })
  }
  if (response.status >= 500) {
    return new BackendUnreachableError()
  }
  return new ApiError(isResult(body) ? body.message : `请求失败（${response.status}）`, {
    status: response.status,
  })
}

function parseEventChunk(chunk: string): ChatEvent | null {
  const dataLines: string[] = []
  for (const line of chunk.split(/\r?\n/)) {
    if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trimStart())
    }
  }
  if (dataLines.length === 0) {
    return null
  }
  try {
    return JSON.parse(dataLines.join('\n')) as ChatEvent
  } catch {
    return null
  }
}

export async function streamChat(
  question: string,
  sessionId: string | null,
  handlers: ChatStreamHandlers,
  signal: AbortSignal,
): Promise<void> {
  const params = new URLSearchParams({ question })
  if (sessionId) {
    params.set('sessionId', sessionId)
  }

  const token = getToken()
  const headers: Record<string, string> = { Accept: 'text/event-stream' }
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  let response: Response
  try {
    response = await fetch(`/api/chat/stream?${params.toString()}`, { headers, signal })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') {
      throw error
    }
    throw new BackendUnreachableError()
  }

  const contentType = response.headers.get('content-type') ?? ''
  if (!response.ok || !contentType.includes('text/event-stream')) {
    throw await buildHttpError(response)
  }
  if (!response.body) {
    throw new BackendUnreachableError('后端未返回流式响应')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }
    buffer += decoder.decode(value, { stream: true })
    const chunks = buffer.split(/\r?\n\r?\n/)
    buffer = chunks.pop() ?? ''
    for (const chunk of chunks) {
      const event = parseEventChunk(chunk)
      if (event) {
        handlers.onEvent(event)
      }
    }
  }

  const tail = parseEventChunk(buffer)
  if (tail) {
    handlers.onEvent(tail)
  }
}
