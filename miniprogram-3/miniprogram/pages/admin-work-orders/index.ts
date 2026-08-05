import { WorkOrder, workOrders } from '../../utils/mock'

type AdminOrder = WorkOrder & {
  tone: string
}

const typeTabs = [
  { key: 'property', label: '物业工单' },
  { key: 'value', label: '增值服务' },
]

const statusTabs = ['全部', '待受理', '处理中', '沟通中', '已完成']

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

const buildOrders = (type: string, status: string): AdminOrder[] =>
  workOrders
    .filter((item) => item.kind === type)
    .filter((item) => status === '全部' || item.status === status)
    .map((item) => ({
      ...item,
      tone: getTone(item.status),
    }))

Page({
  data: {
    typeTabs,
    statusTabs,
    activeType: 'property',
    activeStatus: '全部',
    pageTitle: '物业工单处理',
    orders: buildOrders('property', '全部'),
  },

  onLoad(options: Record<string, string | undefined>) {
    const activeType = options.type === 'value' ? 'value' : 'property'
    this.refresh(activeType, '全部')
  },

  refresh(activeType: string, activeStatus: string) {
    this.setData({
      activeType,
      activeStatus,
      pageTitle: activeType === 'value' ? '增值服务工单处理' : '物业工单处理',
      orders: buildOrders(activeType, activeStatus),
    })
  },

  goBack() {
    wx.navigateBack()
  },

  switchType(event: WechatMiniprogram.TouchEvent) {
    const type = event.currentTarget.dataset.type as string | undefined
    if (!type || type === this.data.activeType) {
      return
    }
    this.refresh(type, '全部')
  },

  switchStatus(event: WechatMiniprogram.TouchEvent) {
    const status = event.currentTarget.dataset.status as string | undefined
    if (!status || status === this.data.activeStatus) {
      return
    }
    this.refresh(this.data.activeType, status)
  },

  openDetail(event: WechatMiniprogram.TouchEvent) {
    const id = event.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/work-order-detail/index?id=${id}&role=admin`,
    })
  },
})
