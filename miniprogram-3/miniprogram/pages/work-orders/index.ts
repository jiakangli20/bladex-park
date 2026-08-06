import { customerApi } from '../../services/miniapp'
import { requireLogin } from '../../utils/session'

const tabs = [
  { key: 'all', label: '全部' },
  { key: 'property', label: '物业' },
  { key: 'value', label: '增值' },
]

const statusText = (kind: string, status: string): string => {
  const property: Record<string, string> = { '0': '待受理', '1': '处理中', '2': '待评价', '3': '已完成', '4': '已关闭' }
  const value: Record<string, string> = { '0': '待受理', '1': '沟通中', '2': '已完成', '3': '已关闭' }
  return (kind === 'property' ? property : value)[status] || status || '待受理'
}

const getTone = (status: string) => status === '已完成' ? 'green' : status === '待受理' ? 'orange' : status === '已关闭' ? 'red' : 'blue'

Page({
  data: { tabs, activeTab: 'all', allOrders: [] as Record<string, any>[], orders: [] as Record<string, any>[], loading: true },

  onLoad(options: Record<string, string | undefined>) {
    if (!requireLogin('/pages/work-orders/index')) return
    this.setData({ activeTab: options.tab === 'property' || options.tab === 'value' ? options.tab : 'all' })
  },

  onShow() { this.loadOrders() },

  async loadOrders() {
    if (!requireLogin('/pages/work-orders/index')) return
    this.setData({ loading: true })
    try {
      const allOrders = (await customerApi.workOrders()).map(item => {
        const status = statusText(item.kind, item.status)
        return { ...item, status, tone: getTone(status), type: item.kind === 'property' ? '物业服务' : '增值服务', applyTime: item.createTime }
      })
      this.setData({ allOrders, orders: this.filter(allOrders, this.data.activeTab) })
    } finally { this.setData({ loading: false }) }
  },

  filter(orders: Record<string, any>[], tab: string) { return orders.filter(item => tab === 'all' || item.kind === tab) },
  goBack() { wx.navigateBack() },
  switchTab(event: WechatMiniprogram.TouchEvent) {
    const tab = String(event.currentTarget.dataset.tab || 'all')
    this.setData({ activeTab: tab, orders: this.filter(this.data.allOrders, tab) })
  },
  openDetail(event: WechatMiniprogram.TouchEvent) {
    wx.navigateTo({ url: `/pages/work-order-detail/index?id=${event.currentTarget.dataset.id}&type=${event.currentTarget.dataset.type}` })
  },
})
