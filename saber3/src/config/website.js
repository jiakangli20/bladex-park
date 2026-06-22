/**
 * 全局配置文件
 */
export default {
  title: '吴中金控企业服务平台',
  logo: 'X',
  key: 'saber', //配置主键,目前用于存储
  indexTitle: '吴中金控企业服务平台',
  clientId: 'saber3', // 客户端id
  clientSecret: 'saber3_secret', // 客户端密钥
  tenantMode: true, // 是否开启租户模式
  tenantId: '000000', // 管理组租户编号
  captchaMode: true, // 是否开启验证码模式
  switchMode: false, // 是否开启登录切换角色部门
  lockPage: '/lock', // 锁屏页面地址
  tokenTime: 3000, // 定时刷新token间隔(单位:毫秒)
  tokenHeader: 'Blade-Auth', // 请求头中携带的token名称
  tokenKey: 'saber3-access-token', // token存储的key(多个系统部署需要修改以免冲突)
  refreshTokenKey: 'saber3-refresh-token', // 刷新token存储的key(多个系统部署需要修改以免冲突)
  //HTTP状态码白名单
  statusWhiteList: [],
  //配置首页不可关闭
  setting: {
    sidebar: 'vertical',
    tag: true,
    debug: false,
    collapse: false,
    search: false,
    color: false,
    lock: false,
    screenshot: true,
    fullscreen: false,
    theme: false,
    menu: true,
  },
  //首页配置
  fistPage: {
    name: '首页',
    path: '/home',
  },
  //配置菜单的属性
  menu: {
    iconDefault: 'icon-caidan',
    label: 'name',
    path: 'path',
    icon: 'source',
    children: 'children',
    query: 'query',
    href: 'path',
    meta: 'meta',
  },
  //水印配置
  watermark: {
    mode: false,
    text: '吴中金控企业服务平台',
  },
  //oauth2配置
  oauth2: {
    // 是否开启注册功能
    registerMode: true,
    // 使用后端工程 @org.springblade.test.Sm2KeyGenerator 获取
    publicKey:
      '047e484091da95e0109aa7436b6b33b057829367f4417770aaae1895e3e5b001e6a00157d9f6f7165c8d903273179b80991411512095fb8fb172745499ef16709d',
    // 第三方系统授权地址
    authUrl: 'http://localhost:8080/blade-auth/oauth/render',
    // 单点登录系统认证
    ssoMode: false, // 是否开启单点登录功能
    ssoBaseUrl: 'http://localhost:8080', // 单点登录系统地址(cloud端口为8100,boot端口为80)
    ssoAuthUrl: '/oauth/authorize?client_id=saber3&response_type=code&redirect_uri=', // 单点登录授权地址
    ssoLogoutUrl: '/oauth/authorize/logout?redirect_uri=', // 单点登录退出地址
    redirectUri: 'http://localhost:2888/login', // 单点登录回调地址(Saber服务的登录界面地址)
  },
  //设计器配置
  design: {
    // 流程设计器类型(true->nutflow,false->flowable)
    designMode: true,
    // 流程设计器地址(flowable模式)
    designUrl: 'http://localhost:9999',
    // 报表设计器地址(cloud端口为8108,boot端口为80)
    reportUrl: 'http://localhost:8080/ureport',
  },
};
