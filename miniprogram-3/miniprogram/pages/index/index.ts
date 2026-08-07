import { publicApi } from '../../services/miniapp'
import { hasCapability, requireLogin } from '../../utils/session'

const baseActions = [
  { key: 'house', label: '我要看房', tone: 'blue' },
  { key: 'property', label: '物业服务', tone: 'green' },
  { key: 'value', label: '增值服务', tone: 'orange' },
  { key: 'orders', label: '我的工单', tone: 'red' },
  { key: 'settle', label: '入驻申请', tone: 'sky' },
  { key: 'parking-pay', label: '停车缴费', tone: 'amber' },
  { key: 'service-desk', label: '服务台', tone: 'cyan', icon: 'value' },
  { key: 'ad-push', label: '广告推送', tone: 'purple' },
]

const serviceCards = [
  { key: 'repair', title: '在线报修', desc: '报修便捷高效', tone: 'purple' },
  { key: 'venue', title: '场地预约', desc: '合理规划使用', tone: 'orange' },
  { key: 'declare', title: '申报服务', desc: '业务快捷申报', tone: 'cyan' },
  { key: 'ip', title: '知产服务', desc: '权益保障服务', tone: 'pink' },
]

Page({
  data: {
    quickActions: baseActions,
    homeServiceCards: serviceCards,
    homePolicies: [] as Record<string, any>[],
    homeActivities: [] as Record<string, any>[],
    noticeCount: 0,
  },

  onShow() {
    const adminActions = hasCapability('admin.overview.view')
      ? [{ key: 'overview', label: '园区概览', tone: 'blue' }, { key: 'more', label: '管理', tone: 'gray' }]
      : []
    this.setData({ quickActions: [...baseActions, ...adminActions] })
    publicApi.home().then(data => this.setData({
      homePolicies: data.policies || [],
      homeActivities: data.activities || [],
    })).catch(() => undefined)
  },

  handleQuickAction(event: WechatMiniprogram.TouchEvent) {
    const key = event.currentTarget.dataset.key
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
      wx.redirectTo({ url: '/pages/services/index?tab=property' })
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
        itemList: ['通知中心', '物业工单处理', '增值服务工单处理'],
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
        },
      })
    }
  },

  showNotice() {
    if (!requireLogin('/pages/notifications/index')) return
    wx.navigateTo({ url: '/pages/notifications/index' })
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
      wx.navigateTo({ url: '/pages/service-detail/index?id=register' })
      return
    }
    if (key === 'ip') {
      wx.navigateTo({ url: '/pages/service-detail/index?id=trademark' })
      return
    }
    wx.redirectTo({ url: '/pages/services/index?tab=value' })
  },
})
