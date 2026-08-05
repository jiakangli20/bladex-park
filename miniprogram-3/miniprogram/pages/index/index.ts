import { homeActivities, homePolicies, homeServiceCards, quickActions } from '../../utils/mock'

Page({
  data: {
    quickActions,
    homeServiceCards,
    homePolicies,
    homeActivities,
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
      wx.navigateTo({ url: '/pages/overview/index' })
      return
    }
    if (key === 'orders') {
      wx.navigateTo({ url: '/pages/work-orders/index' })
      return
    }
    if (key === 'settle') {
      wx.navigateTo({ url: '/pages/house-intent/index?mode=settlement' })
      return
    }
    if (key === 'parking-pay') {
      wx.navigateTo({ url: '/pages/property-form/index?type=parking-pay' })
      return
    }
    if (key === 'more') {
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
    wx.navigateTo({ url: '/pages/notifications/index' })
  },

  goOverview() {
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
