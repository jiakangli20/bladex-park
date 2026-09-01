import { customerApi } from '../../services/miniapp'
import { navigateBackOr } from '../../utils/navigation'
import { hasCapability, requireLogin } from '../../utils/session'
import { DEFAULT_AD_IMAGE, resolveAdImage } from '../../utils/media'

const auditMeta: Record<string, { text: string; tone: string }> = {
  DRAFT: { text: '草稿', tone: 'gray' },
  PENDING: { text: '待审核', tone: 'blue' },
  APPROVED: { text: '已通过', tone: 'green' },
  REJECTED: { text: '已驳回', tone: 'red' },
}

Page({
  data: {
    loading: true,
    error: '',
    canCreate: false,
    ads: [] as Record<string, any>[],
  },

  onLoad() {
    if (!requireLogin('/pages/customer-ads/index')) return
    this.setData({ canCreate: hasCapability('customer.ad.submit') })
  },

  onShow() {
    if (requireLogin('/pages/customer-ads/index')) this.loadAds()
  },

  async loadAds() {
    this.setData({ loading: true, error: '' })
    try {
      const ads = (await customerApi.ads()).map(item => {
        const status = auditMeta[String(item.auditStatus)] || { text: item.auditStatus || '-', tone: 'gray' }
        return {
          ...item,
          ...resolveAdImage(item),
          auditStatusText: status.text,
          tone: status.tone,
          onlineText: String(item.onlineStatus) === '0' ? '已上架' : '未上架',
          rangeText: item.startTime || item.endTime ? `${item.startTime || '不限'} 至 ${item.endTime || '不限'}` : '长期展示',
        }
      })
      this.setData({ ads })
    } catch (error) {
      this.setData({ error: '企业广告加载失败，请稍后重试' })
    } finally {
      this.setData({ loading: false })
    }
  },

  openDetail(event: WechatMiniprogram.TouchEvent) {
    wx.navigateTo({ url: `/pages/customer-ad-detail/index?id=${event.currentTarget.dataset.id}` })
  },

  handleImageError(event: WechatMiniprogram.TouchEvent) {
    const id = String(event.currentTarget.dataset.id)
    const index = this.data.ads.findIndex(item => String(item.id) === id)
    if (index >= 0) this.setData({ [`ads[${index}].image`]: this.data.ads[index].fallbackImage || DEFAULT_AD_IMAGE })
  },

  createAd() { wx.navigateTo({ url: '/pages/customer-ad-form/index' }) },
  retry() { this.loadAds() },
  goBack() { navigateBackOr('/pages/index/index') },
})
