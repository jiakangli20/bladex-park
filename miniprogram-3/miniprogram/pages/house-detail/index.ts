import { publicApi } from '../../services/miniapp'
import { requireLogin } from '../../utils/session'

Page({
  data: {
    house: {} as Record<string, any>,
    loading: true,
  },

  async onLoad(options: Record<string, string | undefined>) {
    if (!options.id) return
    try {
      this.setData({ house: await publicApi.house(options.id) })
    } finally {
      this.setData({ loading: false })
    }
  },

  goBack() {
    wx.navigateBack()
  },

  bookHouse() {
    if (!requireLogin(`/pages/house-intent/index?mode=appointment&id=${this.data.house.id}`)) return
    wx.navigateTo({
      url: `/pages/house-intent/index?mode=appointment&id=${this.data.house.id}`,
    })
  },

  applySettle() {
    wx.navigateTo({
      url: `/pages/house-intent/index?mode=settlement&public=1&id=${this.data.house.id}`,
    })
  },
})
