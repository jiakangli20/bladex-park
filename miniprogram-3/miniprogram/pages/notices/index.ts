import { publicApi } from '../../services/miniapp'
import { navigateBackOr } from '../../utils/navigation'

Page({
  data: { loading: true, notices: [] as Record<string, any>[] },
  onLoad() { this.loadNotices() },
  async loadNotices() {
    this.setData({ loading: true })
    try { this.setData({ notices: await publicApi.notices() }) }
    finally { this.setData({ loading: false }) }
  },
  async openNotice(event: WechatMiniprogram.TouchEvent) {
    const notice = await publicApi.notice(String(event.currentTarget.dataset.id))
    const content = String(notice.content || notice.summary || '').replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
    wx.showModal({ title: notice.title || '园区公告', content: content || '暂无公告内容', showCancel: false, confirmText: '知道了' })
  },
  goBack() { navigateBackOr('/pages/index/index') },
})
