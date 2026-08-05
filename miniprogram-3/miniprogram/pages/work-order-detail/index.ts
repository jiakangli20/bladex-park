import { WorkOrder, workOrders } from '../../utils/mock'

type DisplayWorkOrder = WorkOrder & {
  tone: string
}

const getTone = (status: string) => {
  if (status === '已完成') {
    return 'green'
  }
  if (status === '待受理') {
    return 'orange'
  }
  if (status === '已驳回' || status === '已关闭') {
    return 'red'
  }
  return 'blue'
}

const toDisplayOrder = (order: WorkOrder): DisplayWorkOrder => ({
  ...order,
  tone: getTone(order.status),
})

Page({
  data: {
    role: 'user',
    isAdmin: false,
    order: toDisplayOrder(workOrders[0]),
  },

  onLoad(options: Record<string, string | undefined>) {
    const order = workOrders.find((item) => item.id === options.id)
    const role = options.role === 'admin' ? 'admin' : 'user'
    this.setData({
      role,
      isAdmin: role === 'admin',
      order: toDisplayOrder(order || workOrders[0]),
    })
  },

  goBack() {
    wx.navigateBack()
  },

  handleAction(event: WechatMiniprogram.TouchEvent) {
    const action = event.currentTarget.dataset.action
    wx.showToast({
      title: `${action}操作已记录`,
      icon: 'none',
    })
  },
})
