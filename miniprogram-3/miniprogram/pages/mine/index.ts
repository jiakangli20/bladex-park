import { authApi } from '../../services/miniapp'
import { clearSession, getSession, hasCapability, requireLogin } from '../../utils/session'

const customerMenus = [
  { key: 'intentions', label: '看房与入驻', tone: 'orange', badge: '' },
  { key: 'orders', label: '我的工单', tone: 'red', badge: '' },
  { key: 'contracts', label: '我的合同', tone: 'blue', badge: '' },
  { key: 'payments', label: '缴费记录', tone: 'green', badge: '' },
  { key: 'company', label: '企业信息', tone: 'purple', badge: '' },
]
const accountMenus = [
  { key: 'account', label: '账号管理', tone: 'cyan' },
  { key: 'contact', label: '联系方式', tone: 'gray' },
]

Page({
  data: { loggedIn: false, companyName: '游客', companyMeta: '登录后查看企业与园区服务', businessMenus: [] as Record<string, any>[], accountMenus: [] as Record<string, any>[] },
  onShow() {
    const session = getSession()
    const isCustomer = hasCapability('customer.profile.view')
    const adminMenus = hasCapability('admin.overview.view') ? [
      { key: 'overview', label: '园区概览', tone: 'blue' },
      { key: 'notifications', label: '管理通知', tone: 'orange' },
    ] : []
    this.setData({
      loggedIn: Boolean(session?.accessToken), companyName: session?.profile?.enterpriseName || session?.profile?.nickname || '游客',
      companyMeta: session ? this.roleText(session.roleCodes[0]) : '登录后查看企业与园区服务',
      businessMenus: adminMenus.length ? adminMenus : (isCustomer ? customerMenus : []),
      accountMenus: session ? (isCustomer ? accountMenus : accountMenus.filter(item => item.key === 'account')) : [],
    })
  },
  roleText(roleCode?: string) {
    return ({ mini_user: '微信用户', mini_customer_member: '企业成员', mini_customer_admin: '企业管理员', mini_park_admin: '园区管理员' } as Record<string, string>)[roleCode || ''] || '微信用户'
  },
  openMenu(event: WechatMiniprogram.TouchEvent) {
    if (!requireLogin('/pages/mine/index')) return
    const key = event.currentTarget.dataset.key
    if (key === 'orders') return void wx.navigateTo({ url: '/pages/work-orders/index' })
    if (key === 'intentions') return void wx.navigateTo({ url: '/pages/my-intentions/index' })
    if (key === 'overview') return void wx.navigateTo({ url: '/pages/overview/index' })
    if (key === 'notifications') return void wx.navigateTo({ url: '/pages/notifications/index' })
    wx.navigateTo({ url: `/pages/profile-section/index?type=${key}` })
  },
  login() { if (!this.data.loggedIn) wx.navigateTo({ url: '/pages/login/index?redirect=%2Fpages%2Fmine%2Findex' }) },
  logout() {
    wx.showModal({
      title: '退出登录',
      content: '退出后仍可以浏览园区公开内容，确定退出当前账号吗？',
      confirmText: '退出',
      confirmColor: '#e45161',
      success: async result => {
        if (!result.confirm) return
        await authApi.logout().catch(() => undefined)
        clearSession()
        this.onShow()
        wx.showToast({ title: '已退出登录', icon: 'none' })
      },
    })
  },
})
