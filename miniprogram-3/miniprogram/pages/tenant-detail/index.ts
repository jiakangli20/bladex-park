import { adminApi } from '../../services/miniapp'
import { hasCapability, requireLogin } from '../../utils/session'

Page({
  data: { tenant: {} as Record<string, any>, contracts: [] as Record<string, any>[], bills: [] as Record<string, any>[] },
  async onLoad(options: Record<string, string | undefined>) {
    if (!requireLogin('/pages/overview/index') || !hasCapability('admin.tenant.view') || !options.id) return
    const tenant = await adminApi.tenant(options.id)
    this.setData({ tenant: { ...tenant, industry: tenant.industry || '-', room: '-', area: '-', leasePeriod: '-', rentStatus: tenant.status || '正常', contractStatus: '-' }, contracts: (tenant.contracts || []).map((item: Record<string, any>) => ({ ...item, period: `${item.periodStart || '-'} 至 ${item.periodEnd || '-'}` })) })
  },
  goBack() { wx.navigateBack() },
})
