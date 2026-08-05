import { houses } from '../../utils/mock'

Page({
  data: {
    house: houses[0],
  },

  onLoad(options: Record<string, string | undefined>) {
    const house = houses.find((item) => item.id === options.id)
    if (house) {
      this.setData({ house })
    }
  },

  goBack() {
    wx.navigateBack()
  },

  openVr() {
    wx.showModal({
      title: '在线看房',
      content: `后续接入三方 VR / 视频链接：${this.data.house.vrUrl}`,
      confirmText: '知道了',
      showCancel: false,
    })
  },

  bookHouse() {
    wx.navigateTo({
      url: `/pages/house-intent/index?mode=appointment&id=${this.data.house.id}`,
    })
  },

  applySettle() {
    wx.navigateTo({
      url: `/pages/house-intent/index?mode=settlement&id=${this.data.house.id}`,
    })
  },
})
