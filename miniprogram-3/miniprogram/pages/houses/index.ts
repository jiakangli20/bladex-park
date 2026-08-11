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
    allHouses: [] as Record<string, any>[],
    searchKeyword: '',
    selectedPark: '全部园区',
    filters: ['园区', '租金', '楼层', '更多'],
    parkOptions: [] as string[],
    loading: true,
    error: '',
  },

  onLoad() {
    this.loadHouses()
  },

  async loadHouses() {
    this.setData({ loading: true, error: '' })
    try {
      const houses = this.sortHouses(await publicApi.houses())
      const parkOptions = Array.from(new Set(houses.map(item => item.parkName).filter(Boolean)))
      this.setData({ houses, allHouses: houses, parkOptions })
    } catch (error) {
      this.setData({ error: '房源加载失败，请稍后重试' })
    } finally {
      this.setData({ loading: false })
    }
  },

  sortHouses(houses: Record<string, any>[]) {
    return [...houses].sort((left, right) => {
      const leftFloor = Number(left.floor)
      const rightFloor = Number(right.floor)
      if (Number.isFinite(leftFloor) && Number.isFinite(rightFloor) && leftFloor !== rightFloor) {
        return rightFloor - leftFloor
      }
      if (Number.isFinite(leftFloor) !== Number.isFinite(rightFloor)) return Number.isFinite(rightFloor) ? 1 : -1
      const leftRoom = Number(String(left.room || left.name || '').replace(/[^0-9]/g, ''))
      const rightRoom = Number(String(right.room || right.name || '').replace(/[^0-9]/g, ''))
      if (Number.isFinite(leftRoom) && Number.isFinite(rightRoom) && leftRoom !== rightRoom) return leftRoom - rightRoom
      return String(left.title || '').localeCompare(String(right.title || ''), 'zh-CN')
    })
  },

  applySearch() {
    const keyword = String(this.data.searchKeyword || '').trim().toLowerCase()
    const selectedPark = this.data.selectedPark
    const houses = this.data.allHouses.filter(item => {
      const parkMatched = selectedPark === '全部园区' || item.parkName === selectedPark
      if (!parkMatched) return false
      if (!keyword) return true
      return [item.title, item.building, item.room, item.area, item.parkName]
        .some(value => String(value || '').toLowerCase().includes(keyword))
    })
    this.setData({ houses: this.sortHouses(houses) })
  },

  handleSearchInput(event: WechatMiniprogram.Input) {
    this.setData({ searchKeyword: event.detail.value || '' }, () => this.applySearch())
  },

  clearSearch() {
    this.setData({ searchKeyword: '' }, () => this.applySearch())
  },

  goBack() {
    wx.redirectTo({ url: '/pages/index/index' })
  },

  showFilter(event: WechatMiniprogram.TouchEvent) {
    const name = String(event.currentTarget.dataset.name || '')
    const options = name === '园区' && this.data.parkOptions.length
      ? ['全部园区', ...this.data.parkOptions]
      : filterOptions[name] || ['全部']
    wx.showActionSheet({
      itemList: options,
      success: result => {
        if (name === '园区') {
          const selected = options[result.tapIndex]
          this.setData({ selectedPark: selected }, () => this.applySearch())
          wx.showToast({ title: selected, icon: 'none' })
          return
        }
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
