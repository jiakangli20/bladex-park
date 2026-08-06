import { adminApi } from '../../services/miniapp'
import { hasCapability, requireLogin } from '../../utils/session'

const typeTabs = [
  { key: 'property', label: '物业工单' },
  { key: 'value', label: '增值服务' },
]
const statusTabs = ['全部', '待受理', '处理中', '已完成', '已关闭']
const statusText = (kind: string, value: string) => {
  const maps: Record<string, Record<string, string>> = {
    property: { '0': '待受理', '1': '处理中', '2': '待评价', '3': '已完成', '4': '已关闭' },
    value: { '0': '待受理', '1': '处理中', '2': '已完成', '3': '已关闭' },
  }
  return maps[kind]?.[value] || value
}

Page({
  data: { typeTabs, statusTabs, activeType: 'property', activeStatus: '全部', pageTitle: '物业工单处理', allOrders: [] as Record<string, any>[], orders: [] as Record<string, any>[] },
  onLoad(options: Record<string, string | undefined>) {
    if (!requireLogin('/pages/admin-work-orders/index') || !hasCapability('admin.work-order.view')) {
      if (!hasCapability('admin.work-order.view')) wx.showToast({ title: '没有园区管理权限', icon: 'none' })
      return
    }
    this.setData({ activeType: options.type === 'value' ? 'value' : 'property' })
    this.loadOrders()
  },
  async loadOrders() {
    const allOrders = (await adminApi.workOrders(this.data.activeType)).map(item => {
      const status = statusText(item.kind, item.status)
      return { ...item, status, tone: status === '已完成' ? 'green' : status === '待受理' ? 'orange' : status === '已关闭' ? 'red' : 'blue', type: item.kind === 'property' ? '物业服务' : '增值服务', applyTime: item.createTime }
    })
    this.setData({ allOrders, orders: this.filter(allOrders, this.data.activeStatus), pageTitle: this.data.activeType === 'value' ? '增值服务工单处理' : '物业工单处理' })
  },
  filter(orders: Record<string, any>[], status: string) { return orders.filter(item => status === '全部' || item.status === status) },
  goBack() { wx.navigateBack() },
  switchType(event: WechatMiniprogram.TouchEvent) {
    const type = String(event.currentTarget.dataset.type)
    this.setData({ activeType: type, activeStatus: '全部' })
    this.loadOrders()
  },
  switchStatus(event: WechatMiniprogram.TouchEvent) {
    const activeStatus = String(event.currentTarget.dataset.status)
    this.setData({ activeStatus, orders: this.filter(this.data.allOrders, activeStatus) })
  },
  openDetail(event: WechatMiniprogram.TouchEvent) {
    wx.navigateTo({ url: `/pages/work-order-detail/index?id=${event.currentTarget.dataset.id}&type=${this.data.activeType}&role=admin` })
  },
})
