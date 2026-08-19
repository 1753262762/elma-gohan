import { getAnonymousUserId } from '@/services/anonymous-user'

import { ApiError, isErrorResponse } from './errors'

type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'

export interface ApiRequestOptions {
  path: string
  method?: HttpMethod
  data?: UniApp.RequestOptions['data']
  headers?: Record<string, string>
}

function getApiBaseUrl(): string {
  return import.meta.env.VITE_API_BASE_URL.replace(/\/$/, '')
}

export function apiRequest<T>(options: ApiRequestOptions): Promise<T> {
  return new Promise((resolve, reject) => {
    uni.request({
      url: `${getApiBaseUrl()}${options.path.startsWith('/') ? options.path : `/${options.path}`}`,
      method: options.method ?? 'GET',
      data: options.data,
      header: {
        'Content-Type': 'application/json',
        ...options.headers,
        'X-Anonymous-User-Id': getAnonymousUserId(),
      },
      success(response) {
        if (response.statusCode >= 200 && response.statusCode < 300) {
          resolve(response.data as T)
          return
        }

        if (isErrorResponse(response.data)) {
          reject(
            new ApiError(response.data.message, {
              kind: 'BACKEND',
              statusCode: response.statusCode,
              response: response.data,
            }),
          )
          return
        }

        reject(
          new ApiError('服务暂时不可用，请稍后再试', {
            kind: 'UNKNOWN',
            statusCode: response.statusCode,
          }),
        )
      },
      fail(error) {
        reject(
          new ApiError('网络连接失败，请检查网络后重试', {
            kind: 'NETWORK',
            cause: error,
          }),
        )
      },
    })
  })
}
