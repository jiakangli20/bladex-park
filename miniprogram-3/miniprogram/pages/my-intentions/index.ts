import { customerApi } from '../../services/miniapp'
import { navigateBackOr } from '../../utils/navigation'
import { requireLogin } from '../../utils/session'

const appointmentStatus: Record<string, string> = { PENDING: '待受理', ACCEPTED: '已受理', COMPLETED: '已完成', REJECTED: '已驳回', CANCELLED: '已取消' }
const settlementStatus: Record<string, string> = { '0': '待跟进', '1': '已受理', '2': '跟进中', '3': '已成交', '4': '已驳回' }

Page({
  data: { activeTab: 'appointment', loading: true, appointments: [] as Record<string, any>[], settlements: [] as Record<string, any>[], rows: [] as Record<string, any>[] },
  onLoad() { if (requireLogin('/pages/my-intentions/index')) this.loadData() },
  async loadData() {
    this.setData({ loading: true })
    try {
      const [appointments, settlements] = await Promise.all([customerApi.appointments(), customerApi.settlements()])
      const appointmentRows = appointments.map(item => ({ ...item, title: item.title || item.companyName || '看房预约', statusText: appointmentStatus[String(item.status)] || item.status, canCancel: item.status === 'PENDING', kind: 'appointment' }))
      const settlementRows = settlements.map(item => ({ ...item, statusText: settlementStatus[String(item.status)] || item.status, canCancel: false, kind: 'settlement' }))
      this.setData({ appointments: appointmentRows, settlements: settlementRows, rows: this.data.activeTab === 'appointment' ? appointmentRows : settlementRows })
    } finally { this.setData({ loading: false }) }
  },
  switchTab(event: WechatMiniprogram.TouchEvent) {
    const activeTab = String(event.currentTarget.dataset.tab || 'appointment')
    this.setData({ activeTab, rows: activeTab === 'appointment' ? this.data.appointments : this.data.settlements })
  },
  async cancelAppointment(event: WechatMiniprogram.TouchEvent) {
    const modal = await wx.showModal({ title: '取消预约', content: '确定取消这条看房预约吗？' })
    if (!modal.confirm) return
    await customerApi.cancelAppointment(String(event.currentTarget.dataset.id), '用户主动取消')
    wx.showToast({ title: '预约已取消', icon: 'success' })
    await this.loadData()
  },
  goBack() { navigateBackOr('/pages/mine/index') },
})
