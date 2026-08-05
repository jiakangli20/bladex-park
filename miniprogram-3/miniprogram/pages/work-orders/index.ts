import { WorkOrder, workOrders } from '../../utils/mock'

type DisplayWorkOrder = WorkOrder & {
  tone: string
}

const tabs = [
  { key: 'all', label: '全部' },
  { key: 'property', label: '物业' },
  { key: 'value', label: '增值' },
]

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

const buildOrders = (tab: string): DisplayWorkOrder[] =>
  workOrders
    .filter((item) => tab === 'all' || item.kind === tab)
    .map((item) => ({
      ...item,
      tone: getTone(item.status),
    }))

Page({
  data: {
    tabs,
    activeTab: 'all',
    orders: buildOrders('all'),
  },

  onLoad(options: Record<string, string | undefined>) {
    const tab = options.tab === 'property' || options.tab === 'value' ? options.tab : 'all'
    this.setData({
      activeTab: tab,
      orders: buildOrders(tab),
    })
  },

  goBack() {
    wx.navigateBack()
  },

  switchTab(event: WechatMiniprogram.TouchEvent) {
    const tab = event.currentTarget.dataset.tab as string | undefined
    if (!tab || tab === this.data.activeTab) {
      return
    }
    this.setData({
      activeTab: tab,
      orders: buildOrders(tab),
    })
  },

  openDetail(event: WechatMiniprogram.TouchEvent) {
    const id = event.currentTarget.dataset.id
    wx.navigateTo({
      url: `/pages/work-order-detail/index?id=${id}`,
    })
  },
})
