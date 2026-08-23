import { adminApi, customerApi } from '../../services/miniapp'
import { requireLogin } from '../../utils/session'

const statusText = (kind: string, status: string): string => {
  const property: Record<string, string> = { '0': '待受理', '1': '处理中', '2': '待评价', '3': '已完成', '4': '已关闭' }
  const value: Record<string, string> = { '0': '待受理', '1': '沟通中', '2': '已完成', '3': '已关闭' }
  const appointment: Record<string, string> = { PENDING: '待受理', ACCEPTED: '已受理', COMPLETED: '已完成', REJECTED: '已驳回', CANCELLED: '已取消' }
  const settlement: Record<string, string> = { '0': '待跟进', '1': '已受理', '2': '跟进中', '3': '已成交', '4': '已驳回' }
  return ({ property, value, appointment, settlement }[kind] || {})[status] || status || '待受理'
}

Page({
  data: { role: 'user', isAdmin: false, type: 'property', id: '', order: {} as Record<string, any> },

  async onLoad(options: Record<string, string | undefined>) {
    if (!requireLogin('/pages/work-orders/index') || !options.id) return
    const isAdmin = options.role === 'admin'
    const type = options.type || 'property'
    this.setData({ role: isAdmin ? 'admin' : 'user', isAdmin, type, id: options.id })
    await this.loadOrder()
  },

  async loadOrder() {
    const raw = this.data.isAdmin
      ? await adminApi.workOrder(this.data.type, this.data.id)
      : await customerApi.workOrder(this.data.type, this.data.id)
    const status = statusText(raw.kind, raw.status)
    const steps = (raw.steps || []).map((step: Record<string, any>) => ({
      title: step.actionType || step.title || '进度更新',
      time: step.createTime || step.time,
      desc: step.actionContent || step.content || step.desc,
      done: true,
    }))
    const labels: Record<string, string> = { property: '物业服务', value: '增值服务', appointment: '看房预约', settlement: '入驻商机' }
    const normalizedSteps = steps.length ? steps : [{ title: '提交申请', time: raw.createTime, desc: raw.description || '申请已提交，等待园区处理', done: true }]
    this.setData({ order: { ...raw, status, type: labels[raw.kind] || raw.kind, tone: ['已完成', '已成交'].includes(status) ? 'green' : ['待受理', '待跟进'].includes(status) ? 'orange' : ['已关闭', '已驳回', '已取消'].includes(status) ? 'red' : 'blue', steps: normalizedSteps } })
  },

  goBack() { wx.navigateBack() },

  async handleAction(event: WechatMiniprogram.TouchEvent) {
    const label = String(event.currentTarget.dataset.action || '')
    if (this.data.isAdmin) {
      const action = label === '驳回' ? 'REJECT' : label === '完成' ? 'COMPLETE' : 'ASSIGN'
      await adminApi.action(this.data.type, this.data.id, { action, content: label === '完成' ? '管理员已完成处理' : '', reason: label === '驳回' ? '管理员驳回' : '' })
    } else {
      const action = label === '评价' ? 'rate' : 'cancel'
      await customerApi.workOrderAction(this.data.type, this.data.id, { action, rating: 5, content: '确认完成', reason: '用户取消' })
    }
    wx.showToast({ title: '操作成功', icon: 'success' })
    await this.loadOrder()
  },
})
