# CLAUDE.md
本文件用于指导 Claude Code (claude.ai/code) 在 Saber3 代码仓库中工作时的行为规范。

> 本规范适用于 Saber3 前端工程的所有开发任务，为强制性条款。除非用户显式豁免，任何条目都不得忽视或删减。
>
> 作为 AI 助手参与本项目开发时，你必须：
> - 每次输出前深度理解 BladeX 微服务架构体系、Saber3 前端工程结构和 Vue 3 技术栈特征
> - 当回答依赖外部知识时，先查询 Vue 3、Element Plus、Avue、Vite 等官方文档
> - 若需求含糊，先复述已知信息并列出关键澄清问题
> - 面对复杂需求，先拆分为可管理的子任务
>
> 所有开发内容必须建立在深度思考过的基础之上，禁止机械生成与错误填充。
> 如果你已了解所有规范，请在用户第一次对话时说明："我已充分了解 BladeX 微服务平台开发规范。"

---

## 1. 项目概览

**Saber3** 是 BladeX 微服务平台的官方前端工程，基于 Vue 3 生态构建的企业级后台管理系统。

### 1.1 技术栈

| 技术               | 版本     | 用途               |
| ------------------ | -------- | ------------------ |
| Vue                | ^3.5.13  | 核心框架           |
| Element Plus       | ^2.10.1  | UI 组件库          |
| @smallwei/avue     | ^3.7.2   | 增强型 CRUD 组件库 |
| Vue Router         | ^4.3.2   | 路由管理           |
| Vuex               | ^4.1.0   | 状态管理           |
| Axios              | ^1.8.3   | HTTP 客户端        |
| Vite               | ^5.4.19  | 构建工具           |
| vue-i18n           | ^11.1.3  | 国际化             |
| Sass               | ^1.85.1  | CSS 预处理器       |
| crypto-js / sm-crypto | -     | 加密（AES / SM2）  |

---

## 2. 项目架构

```
Saber3/
├── src/
│   ├── main.js                  # 应用入口（全局组件注册、插件挂载）
│   ├── permission.js            # 路由守卫（鉴权、标签页、锁屏）
│   ├── axios.js                 # HTTP 拦截器（Token 刷新、加密、错误处理）
│   ├── api/                     # API 接口层（按业务模块组织）
│   ├── views/                   # 业务页面（按模块目录组织）
│   ├── option/                  # Avue 表格/表单配置（与 views/api 同构路径）
│   ├── const/                   # 常量定义
│   ├── config/                  # 全局配置
│   │   ├── website.js           # 核心配置（认证、菜单、水印、OAuth2、设计器）
│   │   └── env.js               # 环境变量（API 基础地址）
│   ├── store/                   # Vuex 状态管理（user/common/tags/logs/dict）
│   ├── router/                  # 路由系统
│   │   ├── avue-router.js       # 动态路由核心（菜单→路由、keep-alive 扁平化）
│   │   ├── page/                # 页面级路由（登录、锁屏、错误页）
│   │   └── views/               # 视图路由（首页、控制台等）
│   ├── components/              # 全局公共组件
│   ├── mixins/                  # 混入（crud.js 等）
│   ├── utils/                   # 工具函数（auth/crypto/validate/util...）
│   ├── lang/                    # 国际化（zh/en）
│   ├── styles/                  # 全局样式 & 多主题
│   ├── mac/                     # macOS 风格主题
│   └── page/                    # 页面布局框架（主布局、登录、锁屏）
├── vite/                        # Vite 插件配置
├── vite.config.mjs              # Vite 主配置
└── package.json
```

### 2.1 路径别名

`@` → `./src`、`~` → `./`、`components` → `./src/components`、`styles` → `./src/styles`、`utils` → `./src/utils`

---

## 3. 核心机制

### 3.1 Avue 组件代码生成（Skill 调用）

当涉及 `avue-crud`、`avue-form`、`avue-tree` 等 Avue 组件的代码生成或配置时，**优先调用 `avue-design` Skill**。该 Skill 覆盖 Avue 全部组件类型，支持 Options API 和 Composition API 两种代码风格。

**触发场景**：用户要求创建 CRUD 表格、Avue 表单、树组件、数据展示页面，或提到 avue、crud 表格、动态表单、JSON 配置表单等关键词。

### 3.2 Avue Option 配置

Avue 表格/表单配置独立存放于 `src/option/` 目录，与 `views` 和 `api` 保持同构的模块路径映射。

### 3.3 API 接口规范

- 所有 API 通过 `src/axios.js` 封装的 Axios 实例发起，按业务模块组织于 `src/api/`
- 命名约定：列表 `getList(current, size, params)`、详情 `getDetail(id)`、新增 `add(row)`、更新 `update(row)`、删除 `remove(ids)`、树形 `getXxxTree()`
- 后端微服务前缀：`/blade-system/`、`/blade-resource/`、`/blade-flow/`、`/blade-desk/`、`/blade-log/`、`/blade-develop/`

### 3.4 认证机制

- OAuth2：`Basic` 头传递 `clientId:clientSecret`（Base64 编码）
- Token：请求头 `Blade-Auth: bearer {token}`，支持 AES 加密模式
- 存储：`saber3-access-token` / `saber3-refresh-token`（通过 `utils/auth.js` 管理）
- 401 自动刷新 Token，并发请求排队等待；登录密码使用 SM2 国密加密

### 3.5 路由系统

- 静态路由：`router/page/` + `router/views/`
- 动态路由：`avue-router.js` 将后端菜单数据转换为 Vue Router 路由
- 多级路由自动扁平化为二级，支持 keep-alive 跨层级缓存
- 外部链接自动转换为 iframe 路由，支持 Token 透传

### 3.6 权限控制

- 路由守卫：`permission.js` 控制登录态、锁屏、标签页
- 按钮权限：`store.getters.permission`，格式 `{module}_{action}`（如 `dict_add`）
- 管理员判断：`userInfo.authority.includes('admin')`

### 3.7 多租户

通过 `website.tenantMode` 控制开关，管理组租户编号 `000000`，后端自动通过请求头传递租户信息。

---

## 4. 开发规范

### 4.1 双 API 风格共存

项目同时支持 **Options API**（主流，绝大多数现有页面）和 **Composition API**（新组件可选）。

**选择原则**：
- 修改现有页面：保持该页面原有风格，不混用
- 新建 CRUD 页面：调用 `avue-design` Skill 生成，或参考现有页面手动编写
- 新建复杂交互页面：可使用 Composition API + `<script setup>`
- 同一文件中禁止混用两种风格

### 4.2 命名规范

| 类型           | 命名方式                   | 示例                          |
| -------------- | -------------------------- | ----------------------------- |
| 页面文件       | kebab-case                 | `notice.vue`                  |
| 组件文件       | kebab-case 目录 + main.vue | `basic-container/main.vue`    |
| API / Option / 工具文件 | camelCase         | `dict.js`、`dictbiz.js`       |
| 变量 / 函数    | camelCase                  | `dictValue`、`handleDelete`   |
| Vuex mutations | UPPER_SNAKE                | `SET_IS_MENU`、`ADD_TAG`      |
| Vuex actions   | PascalCase                 | `FedLogOut`、`RefreshToken`   |

### 4.3 代码格式（Prettier）

`printWidth: 100` / `tabWidth: 2` / `semi: true` / `singleQuote: true` / `arrowParens: "avoid"`

### 4.4 样式规范

- 全局 SCSS 变量通过 `styles/variables.scss` 定义，Vite 自动注入所有组件
- 编写样式优先使用已有变量和 mixin（`styles/mixin.scss`），而非硬编码值

### 4.5 全局注册组件

在 `main.js` 中全局注册，可直接在模板中使用：`<basic-container>`、`<basic-block>`、`<highlight>`、`<code-editor>`、`<cron-editor>`、`<flow-design>`、`<flow-design-step>`、`<third-register>`、`<code-setting>`、`<form-setting>`、`<tenant-package>`、`<tenant-datasource>`

### 4.6 全局属性（Options API 中通过 `this` 访问）

`this.website`（全局配置）、`this.$dayjs`（日期库）、`this.getScreen`（屏幕尺寸）、`this.findColumn`（Avue 列配置查找）

---

## 5. 新功能开发流程

### 5.1 新增标准 CRUD 页面

1. **调用 `avue-design` Skill** 生成标准 CRUD 页面代码（推荐）
2. 或手动创建：API 文件 `src/api/{module}/{name}.js` → Option 配置 `src/option/{module}/{name}.js` → Vue 页面 `src/views/{module}/{name}.vue`
3. 后端配置菜单后，动态路由自动生效

### 5.2 开发前必做

1. 先看同模块已有实现，模仿其结构与风格
2. 优先使用 `src/components/`、`src/utils/` 中的现有实现，禁止重复造轮子
3. 若需引入新包，先确认不与已有依赖冲突

### 5.3 开发后验证

1. 若引入新依赖：`pnpm install` → `pnpm run build` → 确认构建通过
2. 构建通过后：`pnpm run dev` → 确认开发服务器正常启动
3. 功能测试交由用户执行，除非用户明确要求，不撰写示例代码或额外文档

---

## 6. 常用命令

```bash
pnpm run dev          # 启动开发服务器（端口 2888）
pnpm run build        # 构建（开发环境）
pnpm run build:prod   # 构建（生产环境，Terser 压缩、删除 console/debugger）
pnpm run serve        # 预览构建产物
```

---

## 7. 自主学习与风格一致性

1. 风格不确定时，主动查阅当前模块现有代码并模仿，避免破坏一致性
2. 新模块参考：`src/views/system/dict.vue`（Options API）、`src/views/desk/notice-composition.vue`（Composition API）
3. 若现有模块已满足需求，禁止自写替代实现
4. 若确需新实现，须在 commit 信息中说明已检索过的相关组件、现有实现不足的原因、新实现的范围

---

## 8. Git 提交规范

当需要提交代码时，优先使用 **`/blade-commit`** skill，它会自动分析变更内容并生成符合项目规范的 Gitmoji 提交信息。

项目采用 **Gitmoji** 风格，中文描述：

| Emoji      | 代码         | 场景             |
| ---------- | ------------ | ---------------- |
| :sparkles: | `:sparkles:` | 新增功能、优化增强 |
| :bug:      | `:bug:`      | 修复 Bug         |
| :zap:      | `:zap:`      | 性能优化、问题修复 |
| :tada:     | `:tada:`     | 重大版本发布     |
| :lipstick: | `:lipstick:` | 样式调整         |
| :recycle:  | `:recycle:`  | 代码重构         |
| :wrench:   | `:wrench:`   | 配置修改         |
| :memo:     | `:memo:`     | 文档更新         |
| :fire:     | `:fire:`     | 删除代码/文件    |

---

## 9. 交流语言

与用户交互时全程使用中文。若需临时切换语言，须明确告知。
