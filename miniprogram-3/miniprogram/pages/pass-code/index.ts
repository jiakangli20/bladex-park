Page({
  data: {
    tenantName: '租客11',
    tenantRole: '租客11',
    avatar: '/assets/images/pass-avatar.png',
  },

  refreshCode() {
    wx.showToast({
      title: '门禁通行码暂未开通',
      icon: 'none',
    })
  },
})
