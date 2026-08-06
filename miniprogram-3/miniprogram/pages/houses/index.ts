import { publicApi } from '../../services/miniapp'
import { requireLogin } from '../../utils/session'

const filterOptions: Record<string, string[]> = {
  园区: ['全部园区', '智慧园区一期', '金融科技园', '研发总部园'],
  租金: ['不限租金', '3元以下', '3-4元', '4元以上'],
  楼层: ['不限楼层', '低楼层', '中楼层', '高楼层', '整层'],
  更多: ['精装修', '近地铁', '可定制装修', '免租期'],
}

Page({
  data: {
    houses: [] as Record<string, any>[],
    filters: ['园区', '租金', '楼层', '更多'],
    loading: true,
    error: '',
  },

  onLoad() {
    this.loadHouses()
  },

  async loadHouses() {
    this.setData({ loading: true, error: '' })
    try {
      const houses = await publicApi.houses()
      this.setData({ houses })
    } catch (error) {
      this.setData({ error: '房源加载失败，请稍后重试' })
    } finally {
      this.setData({ loading: false })
    }
  },

  goBack() {
    wx.redirectTo({ url: '/pages/index/index' })
  },

  showFilter(event: WechatMiniprogram.TouchEvent) {
    const name = String(event.currentTarget.dataset.name || '')
    const options = filterOptions[name] || ['全部']
    wx.showActionSheet({
      itemList: options,
      success(result) {
        wx.showToast({
          title: `${name}: ${options[result.tapIndex]}`,
          icon: 'none',
        })
      },
    })
  },

  openHouseDetail(event: WechatMiniprogram.TouchEvent) {
    const id = event.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/house-detail/index?id=${id}`,
    })
  },

  bookHouse(event: WechatMiniprogram.TouchEvent) {
    const id = event.currentTarget.dataset.id
    if (!requireLogin(`/pages/house-intent/index?mode=appointment&id=${id}`)) return
    wx.navigateTo({
      url: `/pages/house-intent/index?mode=appointment&id=${id}`,
    })
  },
})
