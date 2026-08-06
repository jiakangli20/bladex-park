export const navigateBackOr = (fallbackUrl: string): void => {
  const fallback = () => {
    wx.reLaunch({ url: fallbackUrl })
  }
  if (getCurrentPages().length <= 1) {
    fallback()
    return
  }
  wx.navigateBack({
    delta: 1,
    fail: fallback,
  })
}
