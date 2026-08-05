import { serviceDetails, valueServices } from '../../utils/mock'

Page({
  data: {
    service: valueServices[0],
    serviceDetails,
  },

  onLoad(options: Record<string, string | undefined>) {
    const service = valueServices.find((item) => item.id === options.id)
    if (service) {
      this.setData({ service })
    }
  },

  goBack() {
    wx.navigateBack()
  },

  shareService() {
    wx.showToast({
      title: '请使用右上角菜单分享',
      icon: 'none',
    })
  },

  favoriteService() {
    wx.showToast({
      title: '已收藏',
      icon: 'success',
    })
  },

  submitIntent() {
    wx.navigateTo({
      url: `/pages/value-intent/index?id=${this.data.service.id}`,
    })
  },
})
