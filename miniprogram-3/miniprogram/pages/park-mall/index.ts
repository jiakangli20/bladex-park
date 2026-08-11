Page({
  data: {
    features: [
      { title: '餐饮配套', desc: '园区餐厅、咖啡和轻食', tone: 'orange', icon: '🍽' },
      { title: '便利零售', desc: '日常用品与便民服务', tone: 'blue', icon: '▦' },
      { title: '企业采购', desc: '园区商户与企业专属优惠', tone: 'green', icon: '◇' },
    ],
  },

  goBack() {
    wx.navigateBack({ delta: 1, fail: () => wx.reLaunch({ url: '/pages/index/index' }) })
  },
})
