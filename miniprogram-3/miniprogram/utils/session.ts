export type MiniProfile = {
  userId?: string
  nickname?: string
  mobile?: string
  avatar?: string
  enterpriseName?: string
}

export type MiniSession = {
  needBind: boolean
  bindTicket?: string
  accessToken?: string
  refreshToken?: string
  expiresIn?: number
  tenantId?: string
  parkId?: string
  customerId?: string
  roleCodes: string[]
  capabilities: string[]
  profile?: MiniProfile
}

const SESSION_KEY = 'park-miniapp-session'

let currentSession: MiniSession | null = null

export const getSession = (): MiniSession | null => {
  if (currentSession) return currentSession
  const stored = wx.getStorageSync(SESSION_KEY) as MiniSession | undefined
  currentSession = stored && stored.accessToken ? stored : null
  return currentSession
}

export const saveSession = (session: MiniSession): void => {
  currentSession = session
  wx.setStorageSync(SESSION_KEY, session)
  const app = getApp<IAppOption>()
  app.globalData.session = session
}

export const clearSession = (): void => {
  currentSession = null
  wx.removeStorageSync(SESSION_KEY)
  const app = getApp<IAppOption>()
  app.globalData.session = undefined
}

export const hasCapability = (capability: string): boolean => {
  return getSession()?.capabilities.includes(capability) === true
}

export const isLoggedIn = (): boolean => Boolean(getSession()?.accessToken)

export const requireLogin = (redirect?: string): boolean => {
  if (isLoggedIn()) return true
  const target = redirect ? `?redirect=${encodeURIComponent(redirect)}` : ''
  wx.navigateTo({ url: `/pages/login/index${target}` })
  return false
}
