import { env } from '../config/env'
import { mockRequest } from '../services/mock-adapter'
import { clearSession, getSession, MiniSession, saveSession } from './session'

type ApiEnvelope<T> = {
  code: number
  success?: boolean
  data: T
  msg?: string
  message?: string
}

export type RequestOptions = {
  method?: WechatMiniprogram.RequestOption['method']
  data?: WechatMiniprogram.RequestOption['data']
  auth?: boolean
  requestId?: string
  retry?: boolean
  silent?: boolean
}

let refreshPromise: Promise<MiniSession> | null = null

const showError = (message: string): void => {
  wx.showToast({ title: message || '服务暂时不可用', icon: 'none' })
}

const rawRequest = <T>(path: string, options: RequestOptions): Promise<T> => {
  const session = getSession()
  const header: Record<string, string> = {
    'content-type': 'application/json',
    'Blade-Requested-With': 'BladeHttpRequest',
    'Tenant-Id': session?.tenantId || '000000',
  }
  if (options.auth !== false && session?.accessToken) {
    header['Blade-Auth'] = `bearer ${session.accessToken}`
  }
  if (options.requestId) header['X-Request-Id'] = options.requestId

  return new Promise((resolve, reject) => {
    wx.request<ApiEnvelope<T>>({
      url: `${env().baseUrl}${path}`,
      method: options.method || 'GET',
      data: options.data,
      timeout: 15000,
      header,
      success(result) {
        const body = result.data
        if (result.statusCode === 401) {
          reject({ statusCode: 401, message: body?.msg || '登录已过期' })
          return
        }
        if (result.statusCode < 200 || result.statusCode >= 300 || body?.success === false || (body?.code && body.code !== 200)) {
          reject({ statusCode: result.statusCode, message: body?.msg || body?.message || '请求失败' })
          return
        }
        resolve(body.data)
      },
      fail(error) {
        reject({ statusCode: 0, message: error.errMsg.includes('timeout') ? '请求超时，请重试' : '网络连接失败' })
      },
    })
  })
}

const refreshSession = (): Promise<MiniSession> => {
  if (refreshPromise) return refreshPromise
  const refreshToken = getSession()?.refreshToken
  if (!refreshToken) return Promise.reject(new Error('缺少刷新令牌'))
  refreshPromise = rawRequest<MiniSession>('/blade-miniapp/auth/refresh', {
    method: 'POST',
    auth: false,
    data: { refreshToken },
  }).then(session => {
    saveSession(session)
    return session
  }).finally(() => {
    refreshPromise = null
  })
  return refreshPromise
}

export const request = async <T>(path: string, options: RequestOptions = {}): Promise<T> => {
  if (env().mockEnabled) return mockRequest<T>(path, options)
  try {
    return await rawRequest<T>(path, options)
  } catch (error) {
    const failure = error as { statusCode?: number; message?: string }
    if (failure.statusCode === 401 && options.auth !== false && options.retry !== false) {
      try {
        await refreshSession()
        return await rawRequest<T>(path, { ...options, retry: false })
      } catch (refreshError) {
        clearSession()
        wx.navigateTo({ url: '/pages/login/index' })
        if (!options.silent) showError('登录已过期，请重新登录')
        throw refreshError
      }
    }
    if (!options.silent) showError(failure.message || '请求失败')
    throw error
  }
}

export const createRequestId = (): string => {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 12)}`
}

export const uploadFile = (filePath: string, name = 'file'): Promise<{ name: string; url: string }> => {
  if (env().mockEnabled) {
    return Promise.resolve({ name: filePath.split('/').pop() || '开发环境文件', url: filePath })
  }
  const session = getSession()
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: `${env().baseUrl}/blade-resource/oss/endpoint/put-file-attach`,
      filePath,
      name,
      timeout: 30000,
      header: {
        'Blade-Requested-With': 'BladeHttpRequest',
        'Tenant-Id': session?.tenantId || '000000',
        'Blade-Auth': session?.accessToken ? `bearer ${session.accessToken}` : '',
      },
      success(result) {
        try {
          const body = JSON.parse(result.data) as ApiEnvelope<{ link?: string; url?: string; originalName?: string; name?: string }>
          const url = body.data?.link || body.data?.url || ''
          if (result.statusCode < 200 || result.statusCode >= 300 || body.success === false || !url) {
            throw new Error(body.msg || body.message || '上传失败')
          }
          resolve({ name: body.data?.originalName || body.data?.name || '上传文件', url })
        } catch (error) {
          showError(error instanceof Error ? error.message : '上传失败')
          reject(error)
        }
      },
      fail(error) {
        const message = error.errMsg.includes('timeout') ? '上传超时，请重试' : '文件上传失败'
        showError(message)
        reject(new Error(message))
      },
    })
  })
}
