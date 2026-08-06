import { adminApi } from '../../services/miniapp'
import { hasCapability, requireLogin } from '../../utils/session'

const tone = (type: string) => type.includes('OVERDUE') ? 'red' : type.includes('VALUE') ? 'orange' : type.includes('SETTLEMENT') ? 'green' : 'blue'

Page({
  data: { notifications: [] as Record<string, any>[] },
  onShow() {
    if (!requireLogin('/pages/notifications/index') || !hasCapability('admin.notification.view')) return
    adminApi.notifications().then(items => this.setData({ notifications: items.map(item => ({ ...item, tone: tone(item.type), status: item.status === 'unread' ? '未读' : '已读' })) }))
  },
  goBack() { wx.navigateBack() },
  async openNotification(event: WechatMiniprogram.TouchEvent) {
    const notice = this.data.notifications.find(item => String(item.id) === String(event.currentTarget.dataset.id))
    if (!notice) return
    await adminApi.readNotification(String(notice.id))
    wx.showModal({
      title: notice.title, content: `${notice.content || ''}\n${notice.time || ''}`,
      confirmText: '去处理', cancelText: '关闭',
      success: result => {
        if (result.confirm && ['property', 'value', 'appointment', 'settlement'].includes(notice.target)) {
          wx.navigateTo({ url: `/pages/work-order-detail/index?id=${notice.targetId}&type=${notice.target}&role=admin` })
        }
      },
    })
    this.onShow()
  },
})
