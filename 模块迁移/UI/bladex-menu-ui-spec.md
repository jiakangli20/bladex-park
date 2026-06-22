# BladeX 菜单栏 UI 规范

本文档用于固定园区项目在 BladeX `saber3` Vue3 架构下的菜单布局和菜单样式规范。

目标只有一个：

- 一级菜单固定放在顶部横向显示
- 二级菜单固定放在左侧纵向显示
- 页面内容固定放在右侧主工作区

这份文档既参考了当前 BladeX 现有实现，也把你迁移园区项目时需要锁死的 UI 规则补全了，包含布局、字体大小、颜色、间距、状态样式、数据流和校验清单。

## 1. 参考实现

当前 BladeX `saber3` 已经具备“顶部一级菜单 + 左侧二级菜单”的基础能力，主要参考这些文件：

- 外层布局：
  - [index.vue](/Users/jiakangli/Desktop/bladex-park/saber3/src/page/index/index.vue)
- 顶部一级菜单：
  - [top-menu.vue](/Users/jiakangli/Desktop/bladex-park/saber3/src/page/index/top/top-menu.vue)
  - [top/index.vue](/Users/jiakangli/Desktop/bladex-park/saber3/src/page/index/top/index.vue)
- 左侧二级菜单：
  - [sidebar/index.vue](/Users/jiakangli/Desktop/bladex-park/saber3/src/page/index/sidebar/index.vue)
  - [sidebarItem.vue](/Users/jiakangli/Desktop/bladex-park/saber3/src/page/index/sidebar/sidebarItem.vue)
- 菜单数据流：
  - [user.js](/Users/jiakangli/Desktop/bladex-park/saber3/src/store/modules/user.js)
  - [getters.js](/Users/jiakangli/Desktop/bladex-park/saber3/src/store/getters.js)
- 样式来源：
  - [website.js](/Users/jiakangli/Desktop/bladex-park/saber3/src/config/website.js)
  - [variables.scss](/Users/jiakangli/Desktop/bladex-park/saber3/src/styles/variables.scss)
  - [top.scss](/Users/jiakangli/Desktop/bladex-park/saber3/src/styles/top.scss)
  - [sidebar.scss](/Users/jiakangli/Desktop/bladex-park/saber3/src/styles/sidebar.scss)
  - [go.scss](/Users/jiakangli/Desktop/bladex-park/saber3/src/styles/theme/go.scss)

## 2. 菜单布局结论

园区项目迁移时，菜单布局要明确固化成下面这套结构：

```text
页面框架
  -> 顶部导航栏
     -> 一级菜单（横向）
     -> 右侧用户区 / 语言 / 退出
  -> 主体区域
     -> 左侧导航栏
        -> 二级菜单（纵向）
        -> 三级菜单（若存在，继续在左侧展开）
     -> 右侧内容工作区
        -> 标签栏
        -> 页面内容
```

固定规则：

- [ ] 一级菜单只放在顶部横向导航。
- [ ] 二级菜单只放在左侧纵向导航。
- [ ] 不允许把一级菜单和二级菜单都塞进左侧树菜单。
- [ ] 不允许为了省事把顶部一级菜单取消掉。
- [ ] 页面内部不再重复造一套“伪一级菜单”。
- [ ] 三级菜单如果存在，仍然在左侧树菜单中展开，不上移到顶部。

## 3. BladeX 当前菜单机制

### 3.1 一级菜单

BladeX 当前顶部一级菜单来自：

- `GetTopMenu`

实现位置：

- [top-menu.vue](/Users/jiakangli/Desktop/bladex-park/saber3/src/page/index/top/top-menu.vue)
- [user.js](/Users/jiakangli/Desktop/bladex-park/saber3/src/store/modules/user.js)

当前逻辑：

- 首页是一个单独的固定入口
- 其余一级菜单由 `GetTopMenu` 返回
- 点击一级菜单后，调用 `index.openMenu(item)`

迁移要求：

- [ ] 一级菜单承载主业务域，不承载碎片功能。
- [ ] 一级菜单数量控制在用户一眼可扫完的范围内。
- [ ] 一级菜单名称使用业务名词，不要太长。
- [ ] 一级菜单图标风格统一，不要混杂不同风格图标。

推荐一级菜单范围：

- 首页
- 园区管理
- 入驻管理
- 合同管理
- 审批中心
- 企业服务
- 财务管理

### 3.2 二级菜单

BladeX 当前左侧菜单来自：

- `GetMenu(topMenuId)`

实现位置：

- [index.vue](/Users/jiakangli/Desktop/bladex-park/saber3/src/page/index/index.vue)
- [sidebar/index.vue](/Users/jiakangli/Desktop/bladex-park/saber3/src/page/index/sidebar/index.vue)
- [sidebarItem.vue](/Users/jiakangli/Desktop/bladex-park/saber3/src/page/index/sidebar/sidebarItem.vue)

当前逻辑：

- 点击顶部一级菜单
- `index.openMenu(item)` 触发 `GetMenu(topMenuId)`
- 左侧 `sidebar` 根据当前一级菜单加载对应 children
- 当前路由高亮对应左侧菜单项

迁移要求：

- [ ] 左侧只展示当前一级菜单下的业务菜单。
- [ ] 菜单层级以二级为主，三级按需展开。
- [ ] 左侧菜单展开逻辑统一，不同模块不要各写一套。
- [ ] 左侧菜单名称优先 4 到 8 个字，避免过长换行。

## 4. 布局尺寸规范

基于当前 BladeX 现有样式，园区项目建议固定使用下面这些尺寸：

### 4.1 顶部导航

- 顶部导航高度：`70px`
- 一级菜单单项高度：`70px`
- 一级菜单单项左右内边距：`24px`
- 右侧用户操作区预留宽度：建议 `180px` 到 `220px`

### 4.2 左侧菜单

- 左侧菜单宽度：`260px`
- 折叠宽度：`60px`
- 二级菜单单项高度：`54px`
- 二级菜单首层左内边距：`24px`
- 三级菜单左内边距：`48px`
- 左侧菜单顶部内边距：`12px`

### 4.3 内容区

- 主内容区背景：浅灰工作区背景
- 页面内容内边距：`22px`
- 标签栏高度：`50px`

## 5. 字体规范

菜单栏字体不要做得太大，也不要像演示后台那样忽大忽小。建议固定如下：

### 5.1 顶部一级菜单字体

- 顶栏默认字号：`16px`
- 一级菜单文字字号：建议固定 `15px`
- 一级菜单图标字号：建议固定 `15px`
- 一级菜单字重：`500`

说明：

- `top.scss` 中一级菜单文字存在 `14px`
- `theme/go.scss` 中又覆盖成了 `15px`
- 园区项目建议最终以 `15px` 为准，更稳，也更接近正式业务系统

### 5.2 左侧二级菜单字体

- 二级菜单文字字号：`14px`
- 二级菜单图标字号：`14px`
- 二级菜单默认字重：`500`
- 激活态字重：`700`
- 三级菜单文字字号：`14px`
- 三级菜单默认字重：`400`

### 5.3 顶部右侧用户区字体

- 用户名字号：`14px`
- 下拉菜单字号：`14px`
- 顶部图标字号：`16px`

## 6. 颜色规范

菜单颜色必须和园区项目整体蓝白灰工作台风格一致，不能漂成紫色、黑金色或营销风。

### 6.1 顶部一级菜单颜色

当前代码表现大致如下：

- 顶栏背景：`#1059C6`
- 顶栏默认文字：`rgba(255, 255, 255, 0.92)`
- 顶栏 hover 背景：`rgba(255, 255, 255, 0.12)`
- 顶栏 hover 文字：`#FFFFFF`
- 顶栏激活背景：`#FFFFFF`
- 顶栏激活文字：`#1059C6`

迁移时建议固定为：

```text
--park-menu-top-bg: #1059C6
--park-menu-top-text: rgba(255, 255, 255, 0.92)
--park-menu-top-text-hover: #FFFFFF
--park-menu-top-hover-bg: rgba(255, 255, 255, 0.12)
--park-menu-top-active-bg: #FFFFFF
--park-menu-top-active-text: #1059C6
```

### 6.2 左侧二级菜单颜色

当前代码表现大致如下：

- 左侧背景：`#FFFFFF`
- 左侧默认文字：`#3D4F67`
- 左侧默认图标：`#7A8798`
- 左侧 hover 背景：`#F4F8FF`
- 左侧 hover 文字：`#1059C6`
- 左侧激活背景：`#E8F1FF`
- 左侧激活文字：`#1059C6`
- 左侧激活标识条：`#1059C6`
- 左侧分隔阴影：`rgba(16, 89, 198, 0.08)`

迁移时建议固定为：

```text
--park-menu-side-bg: #FFFFFF
--park-menu-side-text: #3D4F67
--park-menu-side-icon: #7A8798
--park-menu-side-hover-bg: #F4F8FF
--park-menu-side-hover-text: #1059C6
--park-menu-side-active-bg: #E8F1FF
--park-menu-side-active-text: #1059C6
--park-menu-side-active-bar: #1059C6
--park-menu-side-shadow: rgba(16, 89, 198, 0.08)
```

### 6.3 内容区配套颜色

```text
--park-layout-page-bg: #F4F4F6
--park-layout-content-bg: #F4F4F6
--park-layout-tag-bg: #EAECF2
--park-layout-tag-text: #667085
--park-layout-tag-border: #D9DEEA
```

## 7. 交互状态规范

### 7.1 顶部一级菜单状态

- [ ] 默认态：蓝底，白字，轻透明度。
- [ ] hover 态：半透明白底浮起感。
- [ ] 激活态：白底蓝字。
- [ ] 一级菜单切换后，必须同步刷新左侧菜单。
- [ ] 激活一级菜单后，用户要一眼看出当前主业务域。

### 7.2 左侧二级菜单状态

- [ ] 默认态：白底，深灰蓝文字。
- [ ] hover 态：浅蓝底，主色文字。
- [ ] 激活态：浅蓝底 + 左侧 4px 主色高亮条。
- [ ] 激活态文本字重提升。
- [ ] 有子级菜单时展开动画保持自然，不要抖动。

### 7.3 折叠状态

- [ ] 左侧菜单允许折叠，但一级菜单依然保留在顶部。
- [ ] 折叠后只收左侧菜单，不收顶部一级菜单。
- [ ] 折叠菜单时图标必须可识别，tooltip 要可用。

## 8. 数据流规范

菜单迁移不仅是 UI，还要保证数据流固定。

### 8.1 一级菜单数据流

```text
页面初始化
  -> GetTopMenu()
  -> 渲染顶部一级菜单
  -> 默认命中首页或当前路由对应主菜单
```

### 8.2 二级菜单数据流

```text
点击顶部一级菜单
  -> index.openMenu(item)
  -> GetMenu(topMenuId)
  -> 返回当前一级菜单下的 children
  -> 左侧 sidebar 渲染当前二级菜单
  -> router push 到默认业务页或当前菜单 path
```

### 8.3 迁移时的固定要求

- [ ] `setting.sidebar` 固定保持 `vertical`。
- [ ] 不要切成“horizontal sidebar”模式。
- [ ] 一级菜单使用 `GetTopMenu`。
- [ ] 二级菜单使用 `GetMenu(topMenuId)`。
- [ ] 左侧菜单高亮依赖当前路由 path 或 `meta.activeMenu`。
- [ ] 菜单切换逻辑收口到公共布局层，不要每个业务页自己处理菜单激活。

## 9. 菜单栏 UI 落地建议

### 9.1 顶部菜单组件

建议保留并轻定制：

- `top-menu.vue`

建议抽象规则：

- [ ] 首页单独固定入口保留。
- [ ] 业务一级菜单统一走接口返回。
- [ ] 图标与文字水平对齐。
- [ ] 一级菜单不换行。
- [ ] 一级菜单过多时优先做滚动或裁剪，不直接挤压右侧用户区。

### 9.2 左侧菜单组件

建议保留并轻定制：

- `sidebar/index.vue`
- `sidebarItem.vue`

建议抽象规则：

- [ ] 左侧菜单统一复用一套递归组件。
- [ ] 菜单项、子菜单标题、激活态、展开态全部共用一套样式。
- [ ] 不允许某个模块单独重写左侧菜单色值。

### 9.3 园区项目推荐主题

建议继续使用：

- `theme-go`

原因：

- 蓝白灰基调和园区业务最匹配
- 当前顶部菜单、左侧菜单、标签栏已经基本成型
- 后续你只需要做轻定制，不需要重做整套后台主题

## 10. 迁移校验清单

菜单迁移完成后，必须逐项核对：

- [ ] 一级菜单固定在顶部横向显示。
- [ ] 二级菜单固定在左侧纵向显示。
- [ ] 点击一级菜单后，左侧菜单正确切换。
- [ ] 当前一级菜单高亮清晰。
- [ ] 当前二级菜单高亮清晰。
- [ ] 左侧激活条位置正确，没有错位。
- [ ] 一级菜单字体统一为 `15px` 左右，不忽大忽小。
- [ ] 二级菜单字体统一为 `14px`。
- [ ] 顶部菜单背景和左侧菜单背景颜色统一服从蓝白灰体系。
- [ ] hover、激活、默认三种状态区分明确。
- [ ] 菜单图标大小、文字基线、左右间距统一。
- [ ] 左侧菜单宽度稳定为 `260px`，折叠后为 `60px`。
- [ ] 顶部导航高度稳定为 `70px`。
- [ ] 右侧内容区不会被菜单挤压变形。
- [ ] 不会退化成 BladeX 默认演示后台的杂色导航风格。

## 11. 最终结论

园区项目菜单栏迁移时，建议你直接把这套规则锁死：

- 一级菜单在顶部
- 二级菜单在左边
- 顶部高度 `70px`
- 左侧宽度 `260px`
- 一级菜单字号 `15px`
- 二级菜单字号 `14px`
- 顶部主色 `#1059C6`
- 左侧激活色 `#1059C6`
- 左侧 hover 背景 `#F4F8FF`
- 左侧激活背景 `#E8F1FF`

这样后面你迁“园区管理、入驻管理、审批中心、企业服务、财务管理”时，菜单壳子就不会来回改了。
