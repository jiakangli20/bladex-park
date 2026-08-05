Page({
  data: {
    tenantName: '租客11',
    tenantRole: '租客11',
    expireTime: '2026-07-02 17:22:23',
    qrImage: '/assets/images/pass-code-qr.png',
    avatar: '/assets/images/pass-avatar.png',
  },

  refreshCode() {
    wx.showToast({
      title: '通行码已刷新',
      icon: 'success',
    })
  },
})
