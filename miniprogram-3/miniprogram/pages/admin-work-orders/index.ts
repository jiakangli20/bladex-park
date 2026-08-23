import { adminApi } from '../../services/miniapp'
import { hasCapability, requireLogin } from '../../utils/session'

const typeTabs = [
  { key: 'property', label: '物业工单' },
  { key: 'value', label: '增值服务' },
  { key: 'appointment', label: '看房预约' },
  { key: 'settlement', label: '入驻商机' },
]
const statusTabsByType: Record<string, string[]> = {
  property: ['全部', '待受理', '处理中', '待评价', '已完成', '已关闭'],
  value: ['全部', '待受理', '处理中', '已完成', '已关闭'],
  appointment: ['全部', '待受理', '已受理', '已完成', '已驳回', '已取消'],
  settlement: ['全部', '待跟进', '已受理', '跟进中', '已成交', '已驳回'],
}
const statusText = (kind: string, value: string) => {
  const maps: Record<string, Record<string, string>> = {
    property: { '0': '待受理', '1': '处理中', '2': '待评价', '3': '已完成', '4': '已关闭' },
    value: { '0': '待受理', '1': '处理中', '2': '已完成', '3': '已关闭' },
    appointment: { PENDING: '待受理', ACCEPTED: '已受理', COMPLETED: '已完成', REJECTED: '已驳回', CANCELLED: '已取消' },
    settlement: { '0': '待跟进', '1': '已受理', '2': '跟进中', '3': '已成交', '4': '已驳回' },
  }
  return maps[kind]?.[value] || value
}

Page({
  data: { typeTabs, statusTabs: statusTabsByType.property, activeType: 'property', activeStatus: '全部', pageTitle: '物业工单处理', allOrders: [] as Record<string, any>[], orders: [] as Record<string, any>[] },
  onLoad(options: Record<string, string | undefined>) {
    if (!requireLogin('/pages/admin-work-orders/index') || !hasCapability('admin.work-order.view')) {
      if (!hasCapability('admin.work-order.view')) wx.showToast({ title: '没有园区管理权限', icon: 'none' })
      return
    }
    const activeType = typeTabs.some(item => item.key === options.type) ? String(options.type) : 'property'
    this.setData({ activeType, statusTabs: statusTabsByType[activeType] })
    this.loadOrders()
  },
  async loadOrders() {
    const allOrders = (await adminApi.workOrders(this.data.activeType)).map(item => {
      const status = statusText(item.kind, item.status)
      const labels: Record<string, string> = { property: '物业服务', value: '增值服务', appointment: '看房预约', settlement: '入驻商机' }
      return { ...item, status, tone: ['已完成', '已成交'].includes(status) ? 'green' : ['待受理', '待跟进'].includes(status) ? 'orange' : ['已关闭', '已驳回', '已取消'].includes(status) ? 'red' : 'blue', type: labels[item.kind] || item.kind, applyTime: item.createTime, urgency: item.urgency || '普通' }
    })
    const titles: Record<string, string> = { property: '物业工单处理', value: '增值服务工单处理', appointment: '看房预约处理', settlement: '入驻商机跟进' }
    this.setData({ allOrders, orders: this.filter(allOrders, this.data.activeStatus), pageTitle: titles[this.data.activeType] })
  },
  filter(orders: Record<string, any>[], status: string) { return orders.filter(item => status === '全部' || item.status === status) },
  goBack() { wx.navigateBack() },
  switchType(event: WechatMiniprogram.TouchEvent) {
    const type = String(event.currentTarget.dataset.type)
    this.setData({ activeType: type, activeStatus: '全部', statusTabs: statusTabsByType[type] })
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
