import { customerApi } from '../../services/miniapp'
import { navigateBackOr } from '../../utils/navigation'
import { requireLogin } from '../../utils/session'

const tabs = [
  { key: 'all', label: '全部' },
  { key: 'unpaid', label: '待缴' },
  { key: 'paid', label: '已结清' },
]

const statusMeta = (bill: Record<string, any>) => {
  if (String(bill.status) === '1') return { text: '已结清', tone: 'green' }
  if (String(bill.status) === '3') return { text: '部分缴费', tone: 'orange' }
  if (bill.dueDate && new Date(`${bill.dueDate}T23:59:59`).getTime() < Date.now()) return { text: '已逾期', tone: 'red' }
  return { text: '待缴费', tone: 'blue' }
}

Page({
  data: {
    tabs,
    activeTab: 'all',
    loading: true,
    error: '',
    allBills: [] as Record<string, any>[],
    bills: [] as Record<string, any>[],
  },

  onLoad() {
    requireLogin('/pages/utility-bills/index')
  },

  onShow() {
    if (requireLogin('/pages/utility-bills/index')) this.loadBills()
  },

  async loadBills() {
    this.setData({ loading: true, error: '' })
    try {
      const allBills = (await customerApi.utilityBills()).map(item => {
        const status = statusMeta(item)
        return {
          ...item,
          typeText: item.type === 'water' ? '水费' : '电费',
          statusText: status.text,
          tone: status.tone,
          periodText: `${item.periodStart || '-'} 至 ${item.periodEnd || '-'}`,
          amountText: Number(item.amount || 0).toFixed(2),
          paidText: Number(item.paidAmount || 0).toFixed(2),
          remainingText: Number(item.remainingAmount || 0).toFixed(2),
        }
      })
      this.setData({ allBills, bills: this.filterBills(allBills, this.data.activeTab) })
    } catch (error) {
      this.setData({ error: '水电账单加载失败，请稍后重试' })
    } finally {
      this.setData({ loading: false })
    }
  },

  filterBills(bills: Record<string, any>[], tab: string) {
    if (tab === 'paid') return bills.filter(item => String(item.status) === '1')
    if (tab === 'unpaid') return bills.filter(item => String(item.status) !== '1')
    return bills
  },

  switchTab(event: WechatMiniprogram.TouchEvent) {
    const activeTab = String(event.currentTarget.dataset.tab || 'all')
    this.setData({ activeTab, bills: this.filterBills(this.data.allBills, activeTab) })
  },

  openDetail(event: WechatMiniprogram.TouchEvent) {
    wx.navigateTo({ url: `/pages/utility-bill-detail/index?id=${event.currentTarget.dataset.id}` })
  },

  retry() { this.loadBills() },
  goBack() { navigateBackOr('/pages/index/index') },
})
