import {
  overviewMetrics,
  overviewProgress,
  overviewTenants,
  overviewTodos,
} from '../../utils/mock'

Page({
  data: {
    metrics: overviewMetrics,
    progress: overviewProgress,
    todos: overviewTodos,
    tenants: overviewTenants,
  },

  goBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) {
      wx.navigateBack()
      return
    }
    wx.redirectTo({ url: '/pages/index/index' })
  },

  openTenant(event: WechatMiniprogram.TouchEvent) {
    const id = event.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/tenant-detail/index?id=${id}`,
    })
  },

  openTodo(event: WechatMiniprogram.TouchEvent) {
    const label = event.currentTarget.dataset.label
    if (label === '物业申请') {
      wx.navigateTo({ url: '/pages/admin-work-orders/index?type=property' })
      return
    }
    if (label === '增值服务') {
      wx.navigateTo({ url: '/pages/admin-work-orders/index?type=value' })
      return
    }
    wx.navigateTo({ url: '/pages/notifications/index' })
  },
})
