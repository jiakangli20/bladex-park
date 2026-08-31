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
  subscribeTemplateIds?: string[]
  profile?: MiniProfile
  currentEnterpriseSubjectId?: string
  currentParkId?: string
  enterprises?: Record<string, any>[]
}

const SESSION_KEY = 'park-miniapp-session'

let currentSession: MiniSession | null = null

const normalizeSession = (session: MiniSession): MiniSession => ({
  ...session,
  needBind: Boolean(session.needBind),
  roleCodes: Array.isArray(session.roleCodes) ? session.roleCodes : [],
  capabilities: Array.isArray(session.capabilities) ? session.capabilities : [],
  subscribeTemplateIds: Array.isArray(session.subscribeTemplateIds) ? session.subscribeTemplateIds : [],
})

export const getSession = (): MiniSession | null => {
  if (currentSession) return currentSession
  const stored = wx.getStorageSync(SESSION_KEY) as MiniSession | undefined
  currentSession = stored && stored.accessToken ? normalizeSession(stored) : null
  return currentSession
}

export const saveSession = (session: MiniSession): void => {
	const stored = currentSession || wx.getStorageSync(SESSION_KEY) as MiniSession | undefined
	currentSession = normalizeSession({
		...stored,
		...session,
		accessToken: session.accessToken || stored?.accessToken,
		refreshToken: session.refreshToken || stored?.refreshToken,
		expiresIn: session.expiresIn || stored?.expiresIn,
	})
  wx.setStorageSync(SESSION_KEY, currentSession)
  const app = getApp<IAppOption>()
  app.globalData.session = currentSession
}

export const clearSession = (): void => {
  currentSession = null
  wx.removeStorageSync(SESSION_KEY)
  const app = getApp<IAppOption>()
  app.globalData.session = undefined
}

export const hasCapability = (capability: string): boolean => {
  return getSession()?.capabilities?.includes(capability) === true
}

export const isLoggedIn = (): boolean => Boolean(getSession()?.accessToken)

export const requireLogin = (redirect?: string): boolean => {
  if (isLoggedIn()) return true
  const target = redirect ? `?redirect=${encodeURIComponent(redirect)}` : ''
  wx.navigateTo({ url: `/pages/login/index${target}` })
  return false
}
