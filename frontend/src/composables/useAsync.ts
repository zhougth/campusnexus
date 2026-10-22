import { ref, type Ref } from 'vue'

import { BackendNotImplementedError, BackendUnreachableError } from '@/utils/errors'

export interface NotReadyInfo {
  method: string
  path: string
}

export interface AsyncState<T> {
  data: Ref<T | null>
  loading: Ref<boolean>
  notReady: Ref<NotReadyInfo | null>
  unreachable: Ref<boolean>
  errorMessage: Ref<string>
  run: () => Promise<T | null>
}

export function useAsync<T>(loader: () => Promise<T>): AsyncState<T> {
  const data = ref<T | null>(null) as Ref<T | null>
  const loading = ref(false)
  const notReady = ref<NotReadyInfo | null>(null)
  const unreachable = ref(false)
  const errorMessage = ref('')

  async function run(): Promise<T | null> {
    loading.value = true
    notReady.value = null
    unreachable.value = false
    errorMessage.value = ''
    try {
      const result = await loader()
      data.value = result
      return result
    } catch (error) {
      if (error instanceof BackendNotImplementedError) {
        notReady.value = { method: error.method, path: error.path }
      } else if (error instanceof BackendUnreachableError) {
        unreachable.value = true
      } else {
        errorMessage.value = error instanceof Error ? error.message : '加载失败'
      }
      return null
    } finally {
      loading.value = false
    }
  }

  return { data, loading, notReady, unreachable, errorMessage, run }
}
