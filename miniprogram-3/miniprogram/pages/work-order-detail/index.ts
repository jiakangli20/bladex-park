import { adminApi, customerApi } from '../../services/miniapp'
import { requireLogin } from '../../utils/session'

const statusText = (kind: string, status: string): string => {
  const property: Record<string, string> = { '0': '待受理', '1': '处理中', '2': '待评价', '3': '已完成', '4': '已关闭' }
  const value: Record<string, string> = { '0': '待受理', '1': '沟通中', '2': '已完成', '3': '已关闭' }
  return (kind === 'property' ? property : value)[status] || status || '待受理'
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
    this.setData({ order: { ...raw, status, type: raw.kind === 'property' ? '物业服务' : '增值服务', tone: status === '已完成' ? 'green' : status === '待受理' ? 'orange' : status === '已关闭' ? 'red' : 'blue', steps } })
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
