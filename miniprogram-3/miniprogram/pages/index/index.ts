import { adminApi, publicApi } from '../../services/miniapp'
import { hasCapability, requireLogin } from '../../utils/session'

const baseActions = [
  { key: 'notice', label: '公告', tone: 'blue', icon: 'notice' },
  { key: 'house', label: '我要看房', tone: 'blue' },
  { key: 'property', label: '物业服务', tone: 'green' },
  { key: 'value', label: '增值服务', tone: 'orange' },
  { key: 'orders', label: '我的工单', tone: 'red' },
  { key: 'settle', label: '入驻申请', tone: 'sky' },
  { key: 'parking-pay', label: '停车缴费', tone: 'amber' },
  { key: 'service-desk', label: '园区商场', tone: 'cyan', icon: 'mall' },
  { key: 'ad-push', label: '广告推送', tone: 'purple' },
]

const complaintAction = { key: 'complaint', label: '投诉建议', tone: 'purple', icon: 'complaint' }

const splitActionRows = (actions: Record<string, any>[]) => {
  const rows: Record<string, any>[][] = []
  for (let index = 0; index < actions.length; index += 5) {
    rows.push(actions.slice(index, index + 5))
  }
  return rows
}

const serviceCards = [
  { key: 'repair', title: '在线报修', desc: '报修便捷高效', tone: 'purple' },
  { key: 'venue', title: '场地预约', desc: '合理规划使用', tone: 'orange' },
  { key: 'declare', title: '申报服务', desc: '业务快捷申报', tone: 'cyan' },
  { key: 'ip', title: '知产服务', desc: '权益保障服务', tone: 'pink' },
]

Page({
  data: {
    quickActions: [...baseActions, complaintAction],
    quickActionRows: splitActionRows([...baseActions, complaintAction]),
    homeServiceCards: serviceCards,
    homePolicies: [] as Record<string, any>[],
    homeActivities: [] as Record<string, any>[],
    homeBanners: [] as Record<string, any>[],
    homeNotices: [] as Record<string, any>[],
    noticeTitle: '暂无最新公告',
    noticeCount: 0,
  },

  onShow() {
    const adminActions = hasCapability('admin.overview.view')
      ? [{ key: 'overview', label: '园区概览', tone: 'blue' }, { key: 'more', label: '管理', tone: 'gray' }]
      : []
    const quickActions = [...baseActions, ...adminActions, complaintAction]
    this.setData({ quickActions, quickActionRows: splitActionRows(quickActions) })
    publicApi.home().then(data => this.setData({
      homePolicies: data.policies || [],
      homeActivities: data.activities || [],
      homeBanners: data.banners || [],
      homeNotices: data.notices || [],
      noticeTitle: data.notices?.[0]?.title || '暂无最新公告',
    })).catch(() => undefined)
    if (hasCapability('admin.notification.view')) {
      adminApi.notifications().then(items => this.setData({ noticeCount: items.filter(item => item.status === 'unread').length })).catch(() => undefined)
    } else {
      this.setData({ noticeCount: 0 })
    }
  },

  handleQuickAction(event: WechatMiniprogram.TouchEvent) {
    const key = event.currentTarget.dataset.key
    if (key === 'notice') {
      this.showNotice()
      return
    }
    if (key === 'house') {
      wx.redirectTo({ url: '/pages/houses/index' })
      return
    }
    if (key === 'property') {
      wx.redirectTo({ url: '/pages/services/index?tab=property' })
      return
    }
    if (key === 'value') {
      wx.redirectTo({ url: '/pages/services/index?tab=value' })
      return
    }
    if (key === 'overview') {
      if (!requireLogin('/pages/overview/index')) return
      wx.navigateTo({ url: '/pages/overview/index' })
      return
    }
    if (key === 'orders') {
      if (!requireLogin('/pages/work-orders/index')) return
      wx.navigateTo({ url: '/pages/work-orders/index' })
      return
    }
    if (key === 'settle') {
      if (!requireLogin('/pages/house-intent/index?mode=settlement')) return
      wx.navigateTo({ url: '/pages/house-intent/index?mode=settlement' })
      return
    }
    if (key === 'parking-pay') {
      if (!requireLogin('/pages/property-form/index?type=parking-pay')) return
      wx.navigateTo({ url: '/pages/property-form/index?type=parking-pay' })
      return
    }
    if (key === 'service-desk') {
      wx.redirectTo({ url: '/pages/park-mall/index' })
      return
    }
    if (key === 'complaint') {
      if (!requireLogin('/pages/property-form/index?type=complaint')) return
      wx.navigateTo({ url: '/pages/property-form/index?type=complaint' })
      return
    }
    if (key === 'ad-push') {
      if (!requireLogin('/pages/customer-ads/index')) return
      wx.navigateTo({ url: '/pages/customer-ads/index' })
      return
    }
    if (key === 'more') {
      if (!requireLogin('/pages/notifications/index')) return
      wx.showActionSheet({
        itemList: ['通知中心', '物业工单处理', '增值服务工单处理', '看房预约处理', '入驻商机跟进'],
        success(result) {
          if (result.tapIndex === 0) {
            wx.navigateTo({ url: '/pages/notifications/index' })
          }
          if (result.tapIndex === 1) {
            wx.navigateTo({ url: '/pages/admin-work-orders/index?type=property' })
          }
          if (result.tapIndex === 2) {
            wx.navigateTo({ url: '/pages/admin-work-orders/index?type=value' })
          }
          if (result.tapIndex === 3) wx.navigateTo({ url: '/pages/admin-work-orders/index?type=appointment' })
          if (result.tapIndex === 4) wx.navigateTo({ url: '/pages/admin-work-orders/index?type=settlement' })
        },
      })
    }
  },

  showNotice() {
    wx.navigateTo({ url: '/pages/notices/index' })
  },

  showAdminNotifications() {
    if (hasCapability('admin.notification.view')) wx.navigateTo({ url: '/pages/notifications/index' })
    else this.showNotice()
  },

  goOverview() {
    if (!requireLogin('/pages/overview/index')) return
    wx.navigateTo({ url: '/pages/overview/index' })
  },

  openServiceCard(event: WechatMiniprogram.TouchEvent) {
    const key = event.currentTarget.dataset.key
    if (key === 'repair') {
      wx.navigateTo({ url: '/pages/property-form/index?type=repair' })
      return
    }
    if (key === 'venue') {
      wx.navigateTo({ url: '/pages/property-form/index?type=meeting' })
      return
    }
    if (key === 'declare') {
      wx.redirectTo({ url: '/pages/services/index?tab=value&keyword=申报' })
      return
    }
    if (key === 'ip') {
      wx.redirectTo({ url: '/pages/services/index?tab=value&keyword=知识产权' })
      return
    }
    wx.redirectTo({ url: '/pages/services/index?tab=value' })
  },
  openBanner(event: WechatMiniprogram.TouchEvent) {
    const banner = this.data.homeBanners.find(item => String(item.id) === String(event.currentTarget.dataset.id))
    const link = String(banner?.linkUrl || '')
    if (!link) return
    if (link.startsWith('/pages/')) wx.navigateTo({ url: link })
    else wx.showToast({ title: '该广告暂未配置小程序跳转页面', icon: 'none' })
  },
})
