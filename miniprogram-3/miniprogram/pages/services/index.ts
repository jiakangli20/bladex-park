import { publicApi } from '../../services/miniapp'
import { getSession, hasCapability, requireLogin } from '../../utils/session'

const tones = ['green', 'orange', 'blue', 'cyan', 'purple']
const categoryLabels: Record<string, string> = {
  value_added: '综合服务',
  IT: 'IT服务',
  clean: '保洁服务',
  security: '安保服务',
  catering: '餐饮服务',
  repair: '维修服务',
  green: '绿化服务',
  other: '其他服务',
}
const categoryLabel = (value: string) => categoryLabels[value] || value || '其他服务'

Page({
  data: {
    activeTab: 'property',
    activeCategory: '全部',
    serviceStats: [
      { value: '0', label: '物业事项' },
      { value: '0', label: '增值服务' },
      { value: '-', label: '申请进度' },
    ],
    processSteps: ['提交申请', '管理员受理', '进度反馈'],
    propertyCards: [] as Record<string, any>[],
    serviceCategories: [{ value: '全部', label: '全部' }] as Record<string, string>[],
    allValueCards: [] as Record<string, any>[],
    valueCards: [] as Record<string, any>[],
    searchKeyword: '',
  },

  onLoad(options: Record<string, string | undefined>) {
    if (!requireLogin(`/pages/services/index?tab=${options.tab || 'property'}`)) return
    if (!hasCapability('customer.profile.view')) {
      wx.showToast({ title: '当前账号暂无企业服务权限', icon: 'none' })
      wx.redirectTo({ url: '/pages/index/index' })
      return
    }
    if (options.tab === 'value') this.setData({ activeTab: 'value' })
    if (options.keyword) this.setData({ searchKeyword: options.keyword })
    this.loadServices()
  },

  async loadServices() {
    const [properties, values] = await Promise.all([publicApi.propertyServices(), publicApi.valueServices()])
    const parkId = getSession()?.parkId
    const visibleProperties = parkId ? properties.filter(item => String(item.parkId) === String(parkId)) : properties
    const propertyCards = visibleProperties.map((item, index) => ({
      ...item,
      key: item.type || String(item.id),
      tone: tones[index % tones.length],
      badge: item.type?.includes('停车') ? '提交停车申请' : '立即办理',
      eta: '待受理',
    }))
    const valueCards: Record<string, any>[] = values.map(item => ({
      ...item,
      image: '/assets/images/service-business.jpg',
      providerType: '园区服务商',
      providerTone: 'blue',
      categoryLabel: categoryLabel(item.category),
      tags: item.serviceArea ? [item.serviceArea] : [],
    }))
    const categoryValues = Array.from(new Set(valueCards.map(item => item.category).filter(Boolean)))
    const categories = ['全部', ...categoryValues]
      .map(value => ({ value, label: value === '全部' ? '全部' : categoryLabel(value) }))
    this.setData({
      propertyCards,
      allValueCards: valueCards,
      valueCards: this.filterValues(valueCards, this.data.activeCategory, this.data.searchKeyword),
      serviceCategories: categories,
      serviceStats: [
        { value: String(propertyCards.length), label: '物业事项' },
        { value: String(valueCards.length), label: '增值服务' },
        { value: '-', label: '申请进度' },
      ],
    })
  },

  goBack() { wx.redirectTo({ url: '/pages/index/index' }) },

  switchTab(event: WechatMiniprogram.TouchEvent) {
    const tab = event.currentTarget.dataset.tab
    if (tab !== this.data.activeTab) this.setData({ activeTab: tab })
  },

  selectCategory(event: WechatMiniprogram.TouchEvent) {
    const category = String(event.currentTarget.dataset.category || '全部')
    this.setData({
      activeCategory: category,
      valueCards: this.filterValues(this.data.allValueCards, category, this.data.searchKeyword),
    })
  },

  handleSearchInput(event: WechatMiniprogram.Input) {
    const searchKeyword = event.detail.value || ''
    this.setData({ searchKeyword, valueCards: this.filterValues(this.data.allValueCards, this.data.activeCategory, searchKeyword) })
  },

  clearSearch() {
    this.setData({ searchKeyword: '', valueCards: this.filterValues(this.data.allValueCards, this.data.activeCategory, '') })
  },

  filterValues(items: Record<string, any>[], category: string, keyword: string) {
    const normalized = String(keyword || '').trim().toLowerCase()
    return items.filter(item => (category === '全部' || item.category === category)
      && (!normalized || [item.title, item.category, item.desc, item.serviceArea]
        .some(value => String(value || '').toLowerCase().includes(normalized))))
  },

  openPropertyService(event: WechatMiniprogram.TouchEvent) {
    if (!hasCapability('customer.profile.view')) {
      wx.showToast({ title: '当前账号暂无物业服务权限', icon: 'none' })
      return
    }
    if (!requireLogin('/pages/services/index?tab=property')) return
    const id = event.currentTarget.dataset.id
    const type = event.currentTarget.dataset.key
    wx.navigateTo({ url: `/pages/property-form/index?id=${id}&type=${encodeURIComponent(type)}` })
  },

  openServiceDetail(event: WechatMiniprogram.TouchEvent) {
    wx.navigateTo({ url: `/pages/service-detail/index?id=${event.currentTarget.dataset.id}` })
  },

  openWorkOrders() {
    if (!requireLogin(`/pages/work-orders/index?tab=${this.data.activeTab}`)) return
    wx.navigateTo({ url: `/pages/work-orders/index?tab=${this.data.activeTab}` })
  },
})
