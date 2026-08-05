import { bills, contracts, overviewTenants } from '../../utils/mock'

Page({
  data: {
    tenant: overviewTenants[0],
    contracts,
    bills,
  },

  onLoad(options: Record<string, string | undefined>) {
    const tenant = overviewTenants.find((item) => item.id === options.id)
    if (tenant) {
      this.setData({ tenant })
    }
  },

  goBack() {
    wx.navigateBack()
  },
})
