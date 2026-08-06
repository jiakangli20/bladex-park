import { publicApi } from '../../services/miniapp'
import { requireLogin } from '../../utils/session'

const tones = ['green', 'orange', 'blue', 'cyan', 'purple']

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
    serviceCategories: ['全部'] as string[],
    allValueCards: [] as Record<string, any>[],
    valueCards: [] as Record<string, any>[],
  },

  onLoad(options: Record<string, string | undefined>) {
    if (options.tab === 'value') this.setData({ activeTab: 'value' })
    this.loadServices()
  },

  async loadServices() {
    const [properties, values] = await Promise.all([publicApi.propertyServices(), publicApi.valueServices()])
    const propertyCards = properties.map((item, index) => ({
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
      tags: item.serviceArea ? [item.serviceArea] : [],
      rating: '-',
      applied: '-',
    }))
    const categories = ['全部', ...Array.from(new Set(valueCards.map(item => item.category).filter(Boolean)))]
    this.setData({
      propertyCards,
      allValueCards: valueCards,
      valueCards,
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
      valueCards: category === '全部' ? this.data.allValueCards : this.data.allValueCards.filter(item => item.category === category),
    })
  },

  openPropertyService(event: WechatMiniprogram.TouchEvent) {
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
