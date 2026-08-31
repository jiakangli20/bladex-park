import { authApi } from '../../services/miniapp'
import { clearSession, getSession, requireLogin, saveSession } from '../../utils/session'

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
const enterpriseMenus = [
  { key: 'enterprise-certification', label: '企业认证', tone: 'blue' },
  { key: 'enterprise-join', label: '申请成为企业员工', tone: 'green' },
]
const ownerMenus = [
  { key: 'enterprise-invite', label: '员工邀请', tone: 'cyan', badge: '' },
  { key: 'enterprise-join-review', label: '加入申请审核', tone: 'orange', badge: '' },
  { key: 'enterprise-park-application', label: '申请新增园区', tone: 'purple', badge: '' },
]

Page({
  data: { loggedIn: false, companyName: '游客', companyMeta: '登录后查看企业与园区服务', businessMenus: [] as Record<string, any>[], accountMenus: [] as Record<string, any>[], enterprises: [] as Record<string, any>[] },
  async onShow() {
    let session = getSession()
    if (session?.accessToken) {
      session = await authApi.session().then(latest => {
        saveSession(latest)
        return getSession()
      }).catch(() => getSession())
    }
    const isCustomer = session?.capabilities?.includes('customer.profile.view') === true
    const adminMenus = session?.capabilities?.includes('admin.overview.view') ? [
      { key: 'overview', label: '园区概览', tone: 'blue' },
      { key: 'notifications', label: '管理通知', tone: 'orange' },
    ] : []
    const currentRelation = (session?.enterprises || []).find(item =>
      String(item.enterpriseSubjectId) === String(session?.currentEnterpriseSubjectId)
      && String(item.parkId) === String(session?.currentParkId || session?.parkId))
    const isOwner = currentRelation?.roleCode === 'OWNER'
    let ownerEntries = isOwner ? ownerMenus.map(item => ({ ...item })) : []
    if (isOwner) {
      const pending = await authApi.ownerJoins('PENDING').catch(() => [])
      ownerEntries = ownerEntries.map(item => item.key === 'enterprise-join-review'
        ? { ...item, badge: pending.length ? String(pending.length) : '' } : item)
    }
    this.setData({
      loggedIn: Boolean(session?.accessToken), companyName: session?.profile?.enterpriseName || session?.profile?.nickname || '游客',
      companyMeta: session ? `${this.roleText(session.roleCodes[0])}${currentRelation?.parkName ? ` · ${currentRelation.parkName}` : ''}` : '登录后查看企业与园区服务',
      businessMenus: session ? (adminMenus.length ? adminMenus : (isCustomer ? customerMenus : [])) : [],
      accountMenus: session ? [...enterpriseMenus, ...ownerEntries, ...(isCustomer ? accountMenus : accountMenus.filter(item => item.key === 'account'))] : [],
      enterprises: session?.enterprises || [],
    })
  },
  switchEnterprise() {
    if (!requireLogin('/pages/mine/index')) return
    const list = this.data.enterprises
    if (list.length < 2) { wx.showToast({ title: list.length ? '当前仅加入一家企业' : '请先完成企业认证或加入申请', icon: 'none' }); return }
    wx.showActionSheet({ itemList: list.map(item => `${item.enterpriseName || '未命名企业'}${item.parkName ? ` · ${item.parkName}` : ''}`), success: async result => {
      const target = list[result.tapIndex]
      await authApi.switchEnterprise({ enterpriseSubjectId: target.enterpriseSubjectId, parkId: target.parkId })
      const session = await authApi.session()
      saveSession(session)
      this.onShow()
      wx.showToast({ title: '企业已切换', icon: 'success' })
    } })
  },
  roleText(roleCode?: string) {
    return ({ mini_user: '游客', mini_customer_member: '企业员工', mini_customer_admin: '企业管理员' } as Record<string, string>)[roleCode || ''] || '游客'
  },
  openMenu(event: WechatMiniprogram.TouchEvent) {
    if (!requireLogin('/pages/mine/index')) return
    const key = event.currentTarget.dataset.key
    if (key === 'orders') return void wx.navigateTo({ url: '/pages/work-orders/index' })
    if (key === 'intentions') return void wx.navigateTo({ url: '/pages/my-intentions/index' })
    if (key === 'overview') return void wx.navigateTo({ url: '/pages/overview/index' })
    if (key === 'notifications') return void wx.navigateTo({ url: '/pages/notifications/index' })
    if (key === 'enterprise-certification') return void wx.navigateTo({ url: '/pages/enterprise-certification/index' })
    if (key === 'enterprise-join') return void wx.navigateTo({ url: '/pages/enterprise-join/index' })
    if (key === 'enterprise-invite') return void wx.navigateTo({ url: '/pages/enterprise-invite/index' })
    if (key === 'enterprise-join-review') return void wx.navigateTo({ url: '/pages/enterprise-join-review/index' })
    if (key === 'enterprise-park-application') return void wx.navigateTo({ url: '/pages/enterprise-park-application/index' })
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
