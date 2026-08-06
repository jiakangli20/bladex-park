export type MiniEnv = {
  baseUrl: string
  mockEnabled: boolean
}

const configs: Record<'develop' | 'trial' | 'release', MiniEnv> = {
  develop: {
    baseUrl: 'http://127.0.0.1:8080',
    // AppSecret 和真实测试账号就绪前保持开发 Mock，避免本机接口未启动造成 15 秒超时。
    mockEnabled: true,
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
