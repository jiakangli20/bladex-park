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
    needBind: false,
    bindTicket: '',
    inviteCode: '',
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

  onInviteInput(event: WechatMiniprogram.Input) {
    this.setData({ inviteCode: event.detail.value.trim() })
  },

  async startLogin() {
    if (!this.data.agreed) {
      wx.showToast({ title: '请先阅读并同意隐私协议', icon: 'none' })
      return
    }
    this.setData({ loading: true })
    try {
      const code = await wechatCode()
      const session = await authApi.wechatLogin(code)
      if (session.needBind) {
        this.setData({ needBind: true, bindTicket: session.bindTicket || '' })
        wx.showToast({ title: '请授权手机号完成绑定', icon: 'none' })
        return
      }
      saveSession(session)
      await this.requestSubscriptions(session)
      this.finish()
    } finally {
      this.setData({ loading: false })
    }
  },

  async getPhoneNumber(event: WechatMiniprogram.CustomEvent<{ code?: string; errMsg?: string }>) {
    const phoneCode = event.detail.code
    if (!phoneCode) {
      wx.showToast({ title: '需要手机号授权才能完成身份绑定', icon: 'none' })
      return
    }
    this.setData({ loading: true })
    try {
      const session = await authApi.bind(
        this.data.bindTicket,
        phoneCode,
        this.data.inviteCode,
      )
      saveSession(session)
      await this.requestSubscriptions(session)
      this.finish()
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

  finish() {
    wx.showToast({ title: '登录成功', icon: 'success' })
    setTimeout(() => wx.redirectTo({ url: this.data.redirect }), 300)
  },
})
