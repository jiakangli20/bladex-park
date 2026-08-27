export type MiniEnv = {
  baseUrl: string
  mockEnabled: boolean
}

const configs: Record<'develop' | 'trial' | 'release', MiniEnv> = {
  develop: {
    baseUrl: 'http://127.0.0.1:8080',
    // 开发版默认联调本地 Boot；需要 UI 演示时再显式改为 true。
    mockEnabled: false,
  },
  trial: {
    baseUrl: 'https://test-api.example.com',
    mockEnabled: false,
  },
  release: {
    baseUrl: 'https://api.example.com',
    mockEnabled: false,
  },
}

export const env = (): MiniEnv => {
  const version = wx.getAccountInfoSync().miniProgram.envVersion || 'develop'
  const current = configs[version]
  if (version === 'release' && current.mockEnabled) {
    throw new Error('生产环境禁止启用 mock')
  }
  return current
}
