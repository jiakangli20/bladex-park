type TabItem = {
  key: string
  label: string
  url: string
  icon: string
  activeIcon: string
}

Component({
  properties: {
    active: {
      type: String,
      value: 'home',
    },
  },
  data: {
    tabs: [
      {
        key: 'home',
        label: '首页',
        url: '/pages/index/index',
        icon: '/assets/tabbar/home.png',
        activeIcon: '/assets/tabbar/home-active.png',
      },
      {
        key: 'houses',
        label: '看房',
        url: '/pages/houses/index',
        icon: '/assets/icons/house.png',
        activeIcon: '/assets/icons/house.png',
      },
      {
        key: 'scan',
        label: '',
        url: '/pages/pass-code/index',
        icon: '/assets/tabbar/scan-active.png',
        activeIcon: '/assets/tabbar/scan-active.png',
      },
      {
        key: 'discover',
        label: '发现',
        url: '/pages/overview/index',
        icon: '/assets/tabbar/discover.png',
        activeIcon: '/assets/tabbar/discover-active.png',
      },
      {
        key: 'mine',
        label: '我的',
        url: '/pages/mine/index',
        icon: '/assets/tabbar/mine.png',
        activeIcon: '/assets/tabbar/mine-active.png',
      },
    ] as TabItem[],
  },
  methods: {
    handleTap(event: WechatMiniprogram.TouchEvent) {
      const dataset = event.currentTarget.dataset as Record<string, string>
      if (!dataset.url || dataset.key === this.data.active) {
        return
      }
      wx.redirectTo({
        url: dataset.url,
      })
    },
  },
})
