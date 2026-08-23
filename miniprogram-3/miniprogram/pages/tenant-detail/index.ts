import { adminApi } from '../../services/miniapp'
import { hasCapability, requireLogin } from '../../utils/session'

Page({
  data: { tenant: {} as Record<string, any>, contracts: [] as Record<string, any>[], bills: [] as Record<string, any>[] },
  async onLoad(options: Record<string, string | undefined>) {
    if (!requireLogin('/pages/overview/index') || !hasCapability('admin.tenant.view') || !options.id) return
    const tenant = await adminApi.tenant(options.id)
    const contracts = (tenant.contracts || []).map((item: Record<string, any>) => ({ ...item, period: `${item.periodStart || '-'} 至 ${item.periodEnd || '-'}` }))
    const current = contracts[0] || {}
    this.setData({
      tenant: { ...tenant, industry: tenant.industry || '-', room: current.room || '-', area: current.rentArea ? `${current.rentArea}m²` : '-', leasePeriod: current.period || '-', rentStatus: ['0', '1'].includes(String(tenant.status)) ? '正常' : '需关注', contractStatus: current.status || '-' },
      contracts,
      bills: tenant.bills || [],
    })
  },
  goBack() { wx.navigateBack() },
})
