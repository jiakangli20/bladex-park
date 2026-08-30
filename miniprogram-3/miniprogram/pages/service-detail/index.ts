import { publicApi } from '../../services/miniapp'
import { navigateBackOr } from '../../utils/navigation'
import { hasCapability, requireLogin } from '../../utils/session'

Page({
  data: {
    service: {} as Record<string, any>,
    serviceDetails: [] as Array<{ title: string; content: string }>,
  },

  async onLoad(options: Record<string, string | undefined>) {
    if (!requireLogin(`/pages/service-detail/index?id=${options.id || ''}`)) return
    if (!hasCapability('customer.profile.view')) {
      wx.showToast({ title: '当前账号暂无增值服务权限', icon: 'none' })
      navigateBackOr('/pages/index/index')
      return
    }
    if (!options.id) return
    const service = await publicApi.valueService(options.id)
    this.setData({
      service: { ...service, image: '/assets/images/service-business.jpg', rating: '-', applied: '-', reviews: '-' },
      serviceDetails: [
        { title: '服务内容', content: service.desc || '园区服务人员将根据企业需求提供办理方案。' },
        { title: '服务区域', content: service.serviceArea || '当前园区' },
        { title: '办理说明', content: '提交意向后进入待受理状态，可在我的工单查看处理进度。' },
      ],
    })
  },

  goBack() { navigateBackOr('/pages/services/index?tab=value') },
  shareService() { wx.showToast({ title: '请使用右上角菜单分享', icon: 'none' }) },
  favoriteService() { wx.showToast({ title: '已收藏', icon: 'success' }) },
  submitIntent() {
    if (!requireLogin(`/pages/value-intent/index?id=${this.data.service.id}`)) return
    wx.navigateTo({ url: `/pages/value-intent/index?id=${this.data.service.id}` })
  },
})
