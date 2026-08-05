import { profileMenus } from '../../utils/mock'

Page({
  data: {
    businessMenus: profileMenus.slice(0, 4),
    accountMenus: profileMenus.slice(4),
  },

  openMenu(event: WechatMiniprogram.TouchEvent) {
    const key = event.currentTarget.dataset.key
    if (key === 'orders') {
      wx.navigateTo({ url: '/pages/work-orders/index' })
      return
    }
    wx.navigateTo({
      url: `/pages/profile-section/index?type=${key}`,
    })
  },
})
