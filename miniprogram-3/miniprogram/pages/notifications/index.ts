import { AdminNotification, adminNotifications } from '../../utils/mock'

type DisplayNotification = AdminNotification & {
  tone: string
}

const getTone = (type: string) => {
  if (type === '逾期缴费' || type === '工单超时') {
    return 'red'
  }
  if (type === '增值服务申请') {
    return 'orange'
  }
  if (type === '客户入驻申请') {
    return 'green'
  }
  return 'blue'
}

const notifications: DisplayNotification[] = adminNotifications.map((item) => ({
  ...item,
  tone: getTone(item.type),
}))

Page({
  data: {
    notifications,
  },

  goBack() {
    wx.navigateBack()
  },

  openNotification(event: WechatMiniprogram.TouchEvent) {
    const id = event.currentTarget.dataset.id
    const notice = adminNotifications.find((item) => item.id === id)
    if (!notice) {
      return
    }
    wx.showModal({
      title: notice.title,
      content: `${notice.type}\n${notice.content}\n${notice.time}`,
      confirmText: '去处理',
      cancelText: '关闭',
      success(result) {
        if (!result.confirm) {
          return
        }
        if (notice.target === 'property-order' || notice.target === 'value-order') {
          wx.navigateTo({ url: `/pages/work-order-detail/index?id=${notice.targetId}&role=admin` })
          return
        }
        if (notice.target === 'settlement') {
          wx.navigateTo({ url: `/pages/tenant-detail/index?id=${notice.targetId}` })
          return
        }
        wx.navigateTo({ url: '/pages/profile-section/index?type=payments' })
      },
    })
  },
})
