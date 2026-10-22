interface ApiErrorOptions {
  code?: number
  status?: number
  fieldErrors?: Record<string, string>
}

export class ApiError extends Error {
  code?: number
  status?: number
  fieldErrors?: Record<string, string>

  constructor(message: string, options: ApiErrorOptions = {}) {
    super(message)
    this.name = 'ApiError'
    this.code = options.code
    this.status = options.status
    this.fieldErrors = options.fieldErrors
  }
}

export class BackendNotImplementedError extends ApiError {
  method: string
  path: string

  constructor(method: string, path: string) {
    super(`后端接口尚未实现：${method} ${path}`, { status: 404 })
    this.name = 'BackendNotImplementedError'
    this.method = method
    this.path = path
  }
}

export class BackendUnreachableError extends ApiError {
  constructor(message = '无法连接后端服务') {
    super(message)
    this.name = 'BackendUnreachableError'
  }
}

export function getErrorMessage(error: unknown): string {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return '请求失败，请稍后重试'
}
