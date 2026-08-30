import { authApi } from '../../services/miniapp'
import { MiniSession, saveSession } from '../../utils/session'

const wechatCode = (): Promise<string> => new Promise((resolve, reject) => {
  wx.login({
    success: result => result.code ? resolve(result.code) : reject(new Error('微信登录失败')),
    fail: reject,
  })
})

Page({
  data: {
    agreed: false,
    loading: false,
    redirect: '/pages/index/index',
  },

  onLoad(options: Record<string, string>) {
    if (options.redirect) {
      this.setData({ redirect: decodeURIComponent(options.redirect) })
    }
  },

  toggleAgreement() {
    this.setData({ agreed: !this.data.agreed })
  },

  async quickLogin(event: WechatMiniprogram.CustomEvent<{ code?: string; errMsg?: string }>) {
    const phoneCode = event.detail.code
    if (!phoneCode) {
      wx.showToast({ title: '未获取到手机号，请允许授权后重试', icon: 'none' })
      return
    }
    this.setData({ loading: true })
    try {
      // 手机号授权和 wx.login 必须在用户点击回调中串联，未绑定用户再换取一次性绑定票据。
      const code = await wechatCode()
      const login = await authApi.wechatLogin(code)
      let session = login
      if (login.needBind) {
        if (!login.bindTicket) throw new Error('登录票据无效，请重试')
        session = await authApi.bind(login.bindTicket, phoneCode)
      }
      saveSession(session)
      await this.requestSubscriptions(session)
      await this.finish()
    } finally {
      this.setData({ loading: false })
    }
  },

  browseFirst() {
    wx.redirectTo({ url: '/pages/index/index' })
  },

  async requestSubscriptions(session: MiniSession) {
    const templateIds = (session.subscribeTemplateIds || []).filter(Boolean).slice(0, 3)
    if (!templateIds.length) return
    await wx.requestSubscribeMessage({ tmplIds: templateIds }).catch(() => undefined)
  },

  async finish() {
    wx.showToast({ title: '登录成功', icon: 'success' })
    await wx.reLaunch({ url: this.data.redirect })
  },
})
