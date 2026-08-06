import { authApi } from './services/miniapp'
import { clearSession, getSession, saveSession } from './utils/session'

App<IAppOption>({
  globalData: {
    session: getSession() || undefined,
  },
  onLaunch() {
    if (!getSession()?.accessToken) return
    authApi.session().then(saveSession).catch(() => clearSession())
  },
})
