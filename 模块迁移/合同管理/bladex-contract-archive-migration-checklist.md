# BladeX 合同归档二级菜单迁移清单

本文档用于细化「合同管理 -> 合同归档」二级菜单从 RuoYi 迁移到 BladeX 的执行清单。合同归档不是孤立 CRUD，它是合同主档、缴费记录、补充协议、退租记录、合同日志、审批表导出、合同打印的聚合查看页。迁移时必须先明确数据流和依赖边界，再逐项校验功能完整性。

## 1. 迁移边界

### 1.1 菜单定位

- 一级菜单：合同管理
- 二级菜单：合同归档
- RuoYi 路由：`/assetManage/contract-archive`
- RuoYi 页面：`ruoyi-ui/src/views/business/ContractArchiveList.vue`
- BladeX 建议路由：`/contract/archive`
- BladeX 建议接口前缀：`/blade-contract/archive`

### 1.2 重要结论

当前 RuoYi 的「合同归档」页面本质是档案汇总页，不是合同主表的归档动作。

- `biz_contract` 中没有独立的合同归档字段或归档接口。
- `contract_status` 当前后端枚举为：`0` 待签约、`1` 履约中、`2` 已到期、`3` 已续签、`4` 已终止。
- 审批项目 `biz_approval_project.process_status = 5` 表示审批项目已归档，这是审批中心的归档，不等同于合同主表归档。
- 合同归档页面当前复用合同列表、缴费列表、变更列表、退租列表、日志、导出和打印接口。

迁移第一版建议保留当前业务语义：合同归档只做档案查看、导出、打印，不新增“归档合同”写操作。若后续产品要增加真正的合同归档动作，需要单独设计字段、状态流和权限。

### 1.3 演示数据策略

当前项目以功能迁移为主，演示数据缺客户、缺园区、缺房间时不作为阻塞项。

- 历史数据列表和详情必须不报错。
- 缺失客户主档时优先展示 `biz_contract.customer_name`。
- 缺失房间或楼栋主档时优先展示 `biz_contract.room_name`、`biz_contract.building_name`。
- 新增和修改合同仍必须校验园区、客户、房源等主数据，不允许继续产生新的坏关联。

## 2. RuoYi 现状清单

### 2.1 前端页面

| 文件 | 作用 |
| --- | --- |
| `ruoyi-ui/src/views/business/ContractArchiveList.vue` | 合同归档页面，包含列表、抽屉详情、标签页、导出审批表、打印预览 |
| `ruoyi-ui/src/api/business/contract.js` | 合同列表、缴费、日志、导出审批表、打印接口 |
| `ruoyi-ui/src/api/business/contractChange.js` | 合同变更列表接口 |
| `ruoyi-ui/src/api/business/termination.js` | 退租列表接口 |
| `ruoyi-ui/src/views/business/ContractList.vue` | 合同详情中的“合同归档”跳转入口 |

### 2.2 后端接口

| 来源 | RuoYi 接口 | 用途 |
| --- | --- | --- |
| `ContractController` | `GET /business/contract/list` | 合同归档列表 |
| `ContractController` | `GET /business/contract/payment/{contractId}` | 档案详情中的缴费记录 |
| `ContractController` | `GET /business/contract/log/{contractId}` | 档案详情中的操作日志 |
| `ContractController` | `GET /business/contract/approval/export/{contractId}` | 导出合同会签审批表 |
| `ContractController` | `GET /business/contract/print/{contractId}` | 合同打印 HTML 预览 |
| `ContractChangeController` | `GET /business/contract/change/list` | 档案详情中的变更审批 |
| `TerminationController` | `GET /business/termination/list` | 档案详情中的退租记录 |

### 2.3 关联数据表

| 表 | 归档页用途 | 迁移要求 |
| --- | --- | --- |
| `biz_contract` | 合同档案主数据、列表数据 | 必迁 |
| `biz_contract_payment` | 缴费记录标签页 | 依赖缴费模块 |
| `biz_contract_supplement_agreement` | 补充协议标签页 | 合同归档内上传维护 |
| `biz_termination` | 退租记录标签页 | 依赖退租模块 |
| `biz_contract_log` | 操作日志时间线 | 依赖合同基础模块 |
| `biz_document_template` | 合同审批表导出模板 | 依赖模板模块 |
| `biz_approval_project` | 合同审批状态展示 | 依赖审批中心或兼容旧审批表 |
| `ics_park` | 园区名称、园区数据权限 | 已完成前置迁移 |
| `biz_customer` | 客户主档关联 | 客户模块依赖 |
| `ics_room` | 房源展示 | 已完成前置迁移 |
| `ics_building` | 楼栋展示 | 已完成前置迁移 |

## 3. 功能模块详细清单

### 3.1 合同归档列表

功能范围：

- 按合同编号查询。
- 按客户名称查询。
- 支持从合同管理页带 `contractId` 跳转并自动定位。
- 列表展示合同编号、合同名称、客户名称、房源、结束日期。
- 操作列包含查看档案、导出审批表、返回合同。

当前数据流：

```text
合同归档页面
  -> getContractList(queryParam)
  -> ContractController.list
  -> ContractService.selectContractList
  -> ContractMapper.selectContractList
  -> biz_contract
  -> left join ics_park
  -> left join biz_approval_project
```

BladeX 建议数据流：

```text
Saber3 合同归档页面
  -> GET /blade-contract/archive/page
  -> ContractArchiveController.page
  -> IContractArchiveService.selectArchivePage
  -> biz_contract
  -> left join ics_park / biz_approval_project
  -> 返回 BladeX 分页 R<IPage<ContractArchiveVO>>
```

迁移任务：

- [ ] 新增或复用合同分页查询接口，确保归档菜单有独立权限编码。
- [ ] 保留 `contractNo`、`customerName`、`contractId` 查询条件。
- [ ] 列表接口需要兼容历史演示数据，关联表缺失时不能 500。
- [ ] 普通用户按园区授权过滤，管理员可跨园区查询。
- [ ] 前端页面使用 Saber3 / Element-Plus 表格风格重写。
- [ ] 从合同管理页跳转时携带 `contractId`，归档页能过滤或打开对应档案。

### 3.2 合同档案详情

功能范围：

- 点击合同名称或“查看档案”打开详情抽屉。
- 展示合同编号、合同名称、客户名称、房源、租期、月租金。
- 展示档案进度：合同签订、租金收缴、变更/退租、档案归档。
- 详情内提供导出合同审批表、预览打印合同按钮。

当前数据流：

```text
openArchive(record)
  -> current = record
  -> 打开抽屉
  -> 并发加载缴费、变更、退租、日志
```

BladeX 建议数据流：

```text
Saber3 合同归档详情抽屉
  -> GET /blade-contract/archive/detail/{contractId}
  -> ContractArchiveController.detail
  -> IContractArchiveService.getArchiveDetail
  -> 聚合 contract / payments / changes / terminations / logs
  -> 返回 ContractArchiveDetailVO
```

建议返回结构：

```text
ContractArchiveDetailVO
  contract: ContractArchiveVO
  payments: List<ContractPaymentVO>
  changes: List<ContractChangeVO>
  terminations: List<TerminationVO>
  logs: List<ContractLogVO>
  archiveStep: Integer
```

迁移任务：

- [ ] 建议新增聚合详情接口，减少前端同时调用 4 到 5 个接口。
- [ ] 如果缴费、变更、退租模块尚未迁完，详情接口先返回空数组，不能阻塞合同档案打开。
- [ ] 详情接口必须复用合同园区数据权限。
- [ ] 合同不存在或无权限访问时返回 BladeX 标准失败响应。
- [ ] 归档进度 `archiveStep` 在后端或前端统一计算，避免多处散落逻辑。

归档进度建议规则：

| step | 规则 |
| ---: | --- |
| 0 | 只有合同主档，尚无已缴费记录 |
| 1 | 存在 `pay_status = 1` 的缴费记录 |
| 2 | 存在补充协议或退租记录 |
| 3 | 存在已结算退租，或产品后续新增合同归档标记 |

### 3.3 缴费记录标签页

功能范围：

- 展示费用名称、账期、应收、实收、缴费状态。
- 只展示当前合同的缴费记录。
- 无数据时展示空状态。

当前数据流：

```text
归档详情
  -> getPaymentByContract(contractId)
  -> ContractController.paymentByContract
  -> ContractService.selectPaymentByContractId
  -> biz_contract_payment
```

迁移任务：

- [ ] 缴费记录按 `contract_id` 查询。
- [ ] 字段映射：`fee_name`、`period_start`、`period_end`、`amount_due`、`amount_paid`、`pay_status`。
- [ ] 状态字典迁移：`0` 未缴、`1` 已缴、`2` 逾期、`3` 部分缴纳。
- [ ] 账期展示格式统一为 `periodStart ~ periodEnd`。
- [ ] 缴费模块未迁完时，归档详情仍可打开并展示空缴费记录。

### 3.4 补充协议标签页

功能范围：

- 展示协议名称、变更事项、文件名称、归档人、归档时间。
- 支持上传、下载、删除当前合同的补充协议。

当前数据流：

```text
归档详情
  -> ContractArchiveController.supplementList
  -> IContractArchiveService.listSupplementAgreements
  -> biz_contract_supplement_agreement
```

迁移任务：

- [ ] 补充协议按 `contract_id` 查询。
- [ ] 字段映射：`agreement_name`、`change_item`、`file_name`、`file_url`、`create_by`、`create_time`。
- [ ] 上传文件复用 OSS 上传接口，归档表只保存文件元数据。
- [ ] 补充协议表未建时，归档详情返回空数组，不影响合同档案。

### 3.5 退租记录标签页

功能范围：

- 展示退租单号、退租时间、审批状态、结算状态。
- 只展示当前合同的退租记录。

当前数据流：

```text
归档详情
  -> listTermination({ contractId, pageNum: 1, pageSize: 100 })
  -> TerminationController.list
  -> ITerminationService.selectTerminationList
  -> biz_termination
```

迁移任务：

- [ ] 退租记录按 `contract_id` 查询。
- [ ] 字段映射：`termination_no`、`termination_time`、`approval_status`、`status`。
- [ ] 审批状态字典迁移：`0` 草稿、`1` 待审批、`2` 已批准、`3` 已驳回。
- [ ] 结算状态字典迁移：`0` 待结算、`1` 已完成或已结算、`2` 已取消。
- [ ] 退租模块未迁完时，归档详情返回空数组，不影响合同档案。
- [ ] 已结算退租应推动档案进度到 step 3。

### 3.6 操作日志标签页

功能范围：

- 按时间线展示合同操作日志。
- 展示操作说明、操作人、操作时间。
- 无日志时展示空状态。

当前数据流：

```text
归档详情
  -> getContractLogs(contractId)
  -> ContractController.logs
  -> ContractService.selectLogByContractId
  -> biz_contract_log
```

迁移任务：

- [ ] 日志按 `contract_id` 查询并按 `operate_time` 倒序或正序统一。
- [ ] 字段映射：`action`、`action_desc`、`operator`、`operate_time`。
- [ ] 合同新增、修改、续签、终止、缴费确认、催缴等动作迁移后仍写入日志。
- [ ] 历史日志缺操作人时使用 `-` 或系统默认展示，不能报错。

### 3.7 导出合同审批表

功能范围：

- 在列表操作列导出合同审批表。
- 在详情合同档案标签页导出合同审批表。
- 成功后打开返回文件地址。

当前数据流：

```text
导出合同审批表
  -> exportContractApproval(contractId)
  -> ContractController.exportApproval
  -> ContractService.exportApprovalForm
  -> DocumentTemplateExportService
  -> biz_document_template
  -> uploadPath/contract-template
```

迁移任务：

- [ ] 迁移合同审批表导出接口。
- [ ] 确认 `biz_document_template` 中存在 `contract-approval` 模板配置。
- [ ] 文件输出适配 BladeX 附件、本地静态资源或 OSS。
- [ ] 返回结构统一为 BladeX `R.data({ url, fileName })` 或目标项目现有文件响应规范。
- [ ] 模板缺失时返回明确错误，不影响归档列表和详情。

### 3.8 合同打印预览

功能范围：

- 在合同档案详情中预览合同 HTML。
- 支持浏览器打印。

当前数据流：

```text
预览打印合同
  -> printContract(contractId)
  -> ContractController.print
  -> ContractService.printContract
  -> 返回 html
  -> 前端弹窗 v-html 预览
```

迁移任务：

- [ ] 迁移合同打印预览接口。
- [ ] 返回 HTML 前确认 XSS 风险，尽量使用后端模板或白名单字段渲染。
- [ ] 前端弹窗展示 HTML，并保留打印按钮。
- [ ] 合同不存在、无权限、模板缺失时返回明确错误。

## 4. BladeX 目标设计

### 4.1 后端建议落点

以目标仓库实际结构为准，建议归档聚合能力放在合同模块内：

```text
org.springblade.modules.contract
  controller/ContractArchiveController.java
  service/IContractArchiveService.java
  service/impl/ContractArchiveServiceImpl.java
  vo/ContractArchiveVO.java
  vo/ContractArchiveDetailVO.java
```

如项目已经按 `org.springblade.modules.ics.contract` 组织业务包，则归档模块跟随现有合同包，不单独开新业务根包。

### 4.2 接口映射

| 功能 | RuoYi 接口 | BladeX 建议接口 |
| --- | --- | --- |
| 归档列表 | `GET /business/contract/list` | `GET /blade-contract/archive/page` |
| 归档详情聚合 | 多接口并发 | `GET /blade-contract/archive/detail/{contractId}` |
| 缴费记录 | `GET /business/contract/payment/{contractId}` | 可聚合到 `archive/detail`，也可保留 `/blade-contract/payment/contract/{contractId}` |
| 变更记录 | `GET /business/contract/change/list` | 可聚合到 `archive/detail`，也可保留 `/blade-contract/change/page` |
| 退租记录 | `GET /business/termination/list` | 可聚合到 `archive/detail`，也可保留 `/blade-contract/termination/page` |
| 操作日志 | `GET /business/contract/log/{contractId}` | 可聚合到 `archive/detail`，也可保留 `/blade-contract/contract/log/{contractId}` |
| 导出审批表 | `GET /business/contract/approval/export/{contractId}` | `GET /blade-contract/archive/export-approval/{contractId}` |
| 打印预览 | `GET /business/contract/print/{contractId}` | `GET /blade-contract/archive/print/{contractId}` |

### 4.3 前端建议落点

```text
saber3/src/api/contract/archive.js
saber3/src/views/contract/archive/index.vue
```

页面结构建议：

- 顶部查询区：合同编号、客户名称、合同状态、园区。
- 表格：合同编号、合同名称、客户名称、园区、房源、开始日期、结束日期、合同状态、操作。
- 详情抽屉：合同档案、缴费记录、变更审批、退租记录、操作日志。
- 操作按钮：查看档案、导出审批表、返回合同、预览打印合同。

### 4.4 菜单和权限

建议菜单：

```text
合同管理
  -> 合同归档
```

建议权限编码：

| 功能 | 权限编码 |
| --- | --- |
| 合同归档菜单 | `contract:archive` |
| 归档列表 | `contract:archive:list` |
| 查看档案 | `contract:archive:detail` |
| 导出审批表 | `contract:archive:export-approval` |
| 打印预览 | `contract:archive:print` |
| 返回合同 | 复用 `contract:contract:list` 或仅前端跳转 |

后端接口按 BladeX Secure 规范加权限控制，普通用户还要叠加园区数据权限。

## 5. 与其他模块的关联关系

| 关联模块 | 依赖方向 | 归档页需要什么 | 未迁完时的降级策略 |
| --- | --- | --- | --- |
| 合同主档 | 强依赖 | 列表、详情、状态、租期、客户、房源 | 不可降级，合同主档必须先迁 |
| 园区资产 | 强依赖 | 园区、楼栋、房间展示和数据权限 | 已迁表可直接复用，缺关联时空值展示 |
| 客户管理 | 中依赖 | 客户名称和客户主档校验 | 历史数据优先展示合同冗余客户名 |
| 缴费管理 | 中依赖 | 缴费记录标签页、档案进度 | 未迁完返回空数组 |
| 合同变更 | 中依赖 | 变更审批标签页、档案进度 | 未迁完返回空数组 |
| 退租管理 | 中依赖 | 退租记录标签页、档案进度 | 未迁完返回空数组 |
| 合同日志 | 强依赖 | 操作日志时间线 | 必须随合同基础模块迁移 |
| 模板导出 | 弱依赖 | 合同审批表导出 | 未迁完只禁用导出按钮或返回明确错误 |
| 合同打印 | 弱依赖 | HTML 预览和打印 | 未迁完只禁用打印按钮或返回明确错误 |
| 审批中心 | 中依赖 | 审批状态、审批项目归档状态 | 第一版可兼容旧 `biz_approval_project` |

## 6. 迁移执行步骤

### 6.1 准备阶段

- [ ] 确认合同基础模块已迁移或正在迁移。
- [ ] 确认 `biz_contract`、`biz_contract_log` 已导入 BladeX 数据库。
- [ ] 确认园区、楼栋、楼层、房间基础表可查询。
- [ ] 确认菜单规划中存在「合同管理 -> 合同归档」。
- [ ] 确认合同归档不新增合同状态写操作。

### 6.2 后端迁移

- [ ] 创建 `ContractArchiveController` 或在合同控制器中新增归档分组接口。
- [ ] 实现归档分页查询。
- [ ] 实现归档详情聚合查询。
- [ ] 接入缴费记录查询。
- [ ] 接入变更记录查询。
- [ ] 接入退租记录查询。
- [ ] 接入操作日志查询。
- [ ] 接入合同审批表导出。
- [ ] 接入合同打印预览。
- [ ] 接入 BladeX 权限注解和园区数据权限。
- [ ] 返回结构统一使用 BladeX `R`。
- [ ] 分页统一使用 BladeX / MyBatis-Plus 分页对象。

### 6.3 前端迁移

- [ ] 创建 `saber3/src/api/contract/archive.js`。
- [ ] 创建 `saber3/src/views/contract/archive/index.vue`。
- [ ] 实现查询表单。
- [ ] 实现归档列表。
- [ ] 实现查看档案抽屉。
- [ ] 实现合同档案标签页。
- [ ] 实现缴费记录标签页。
- [ ] 实现变更审批标签页。
- [ ] 实现退租记录标签页。
- [ ] 实现操作日志标签页。
- [ ] 实现导出合同审批表。
- [ ] 实现合同打印预览弹窗。
- [ ] 从合同管理页面补充跳转合同归档入口。
- [ ] 空数据、加载中、接口错误状态处理完整。

### 6.4 菜单权限迁移

- [ ] `blade_menu` 新增合同归档菜单。
- [ ] 菜单路由指向 Saber3 合同归档页面。
- [ ] 添加归档列表、查看、导出、打印按钮权限。
- [ ] 管理员角色拥有全部权限。
- [ ] 普通业务角色按实际需要分配查看和导出权限。
- [ ] 无权限用户看不到菜单或按钮。

## 7. 数据流总览

### 7.1 列表数据流

```text
用户进入合同归档菜单
  -> 前端读取查询条件 contractNo/customerName/contractId/parkId
  -> /blade-contract/archive/page
  -> 校验菜单和接口权限
  -> 注入园区数据权限
  -> 查询 biz_contract
  -> left join ics_park / biz_approval_project
  -> 返回分页列表
```

### 7.2 详情数据流

```text
用户点击查看档案
  -> /blade-contract/archive/detail/{contractId}
  -> 校验合同是否存在
  -> 校验合同园区是否可访问
  -> 查询合同主档 biz_contract
  -> 查询缴费 biz_contract_payment
  -> 查询变更 biz_contract_change
  -> 查询退租 biz_termination
  -> 查询日志 biz_contract_log
  -> 计算 archiveStep
  -> 返回聚合详情
```

### 7.3 导出数据流

```text
用户点击导出审批表
  -> /blade-contract/archive/export-approval/{contractId}
  -> 校验合同权限
  -> 查询合同主档和审批项目
  -> 查询 biz_document_template
  -> 生成文件
  -> 保存到 BladeX 文件体系
  -> 返回 url/fileName
```

### 7.4 打印数据流

```text
用户点击预览打印合同
  -> /blade-contract/archive/print/{contractId}
  -> 校验合同权限
  -> 查询合同主档
  -> 渲染 HTML
  -> 返回 html
  -> 前端弹窗预览
  -> 浏览器打印
```

## 8. 迁移校验清单

每完成合同归档模块迁移后，必须按以下清单逐项核对。没有通过校验前，不建议合并到合同管理主迁移分支。

### 8.1 基础可用性

- [ ] BladeX 后端启动成功。
- [ ] Saber3 前端启动成功。
- [ ] 登录后能看到「合同管理 -> 合同归档」菜单。
- [ ] 点击菜单能进入合同归档页面。
- [ ] 页面无控制台报错。
- [ ] 页面刷新后路由仍可访问。

### 8.2 列表校验

- [ ] 合同归档列表能展示 `biz_contract` 数据。
- [ ] 合同编号查询可用。
- [ ] 客户名称查询可用。
- [ ] 从合同管理页面携带 `contractId` 跳转后能定位或筛选对应合同。
- [ ] 合同名称为空时能回退展示合同编号。
- [ ] 客户主档缺失时能展示合同冗余客户名称。
- [ ] 园区、房源缺失时页面不报错。
- [ ] 已逻辑删除合同不显示。
- [ ] 普通用户只能看到授权园区合同。
- [ ] 管理员能按园区筛选。

### 8.3 档案详情校验

- [ ] 点击合同名称可打开档案详情抽屉。
- [ ] 点击“查看档案”可打开档案详情抽屉。
- [ ] 合同编号、合同名称、客户名称、房源、租期、月租金显示正确。
- [ ] 缴费记录标签页可切换。
- [ ] 变更审批标签页可切换。
- [ ] 退租记录标签页可切换。
- [ ] 操作日志标签页可切换。
- [ ] 任一子模块无数据时展示空状态，不报错。
- [ ] 无权限访问合同详情时返回明确错误，前端有提示。

### 8.4 缴费记录校验

- [ ] 只展示当前合同的缴费记录。
- [ ] 费用名称显示正确。
- [ ] 账期显示正确。
- [ ] 应收金额、实收金额显示正确。
- [ ] 缴费状态字典显示正确。
- [ ] 存在已缴记录时档案进度至少到“租金收缴”。

### 8.5 变更审批校验

- [ ] 只展示当前合同的变更记录。
- [ ] 变更单号显示正确。
- [ ] 变更类型字典显示正确。
- [ ] 审批状态字典显示正确。
- [ ] 审批意见显示正确。
- [ ] 存在变更记录时档案进度至少到“变更/退租”。

### 8.6 退租记录校验

- [ ] 只展示当前合同的退租记录。
- [ ] 退租单号显示正确。
- [ ] 退租时间显示正确。
- [ ] 退租审批状态显示正确。
- [ ] 结算状态显示正确。
- [ ] 存在退租记录时档案进度至少到“变更/退租”。
- [ ] 存在已结算退租时档案进度到“档案归档”。

### 8.7 操作日志校验

- [ ] 只展示当前合同的操作日志。
- [ ] 操作说明显示正确。
- [ ] 操作人显示正确。
- [ ] 操作时间显示正确。
- [ ] 无日志时展示空状态。
- [ ] 新增、修改、续签、终止、缴费确认、催缴等动作完成后能在日志中看到记录。

### 8.8 导出和打印校验

- [ ] 列表操作列可导出合同审批表。
- [ ] 详情页可导出合同审批表。
- [ ] 模板存在时返回可访问文件地址。
- [ ] 模板不存在时返回明确错误，不影响页面其他功能。
- [ ] 详情页可打开合同打印预览。
- [ ] 打印预览 HTML 不为空。
- [ ] 浏览器打印按钮可触发打印。
- [ ] 导出和打印接口都受合同园区权限控制。

### 8.9 菜单权限校验

- [ ] 无 `contract:archive:list` 权限时不可访问合同归档菜单。
- [ ] 无 `contract:archive:detail` 权限时不可查看档案详情。
- [ ] 无 `contract:archive:export-approval` 权限时不显示导出按钮。
- [ ] 无 `contract:archive:print` 权限时不显示打印按钮。
- [ ] 后端接口权限和前端按钮权限一致。

### 8.10 关联模块回归

- [ ] 合同管理列表中的“合同归档”跳转仍可用。
- [ ] 合同基础 CRUD 不受归档页面影响。
- [ ] 缴费管理接口不因归档聚合查询改变原有行为。
- [ ] 合同变更审批接口不因归档聚合查询改变原有行为。
- [ ] 退租管理接口不因归档聚合查询改变原有行为。
- [ ] 模板导出功能不影响合同打印预览。
- [ ] 审批中心的审批项目归档不被误认为合同主表归档。

## 9. SQL 辅助校验

以下 SQL 用于迁移后快速核对归档页数据来源，不要求演示数据全部无异常。

### 9.1 合同归档列表基数

```sql
SELECT COUNT(*) AS contract_count
FROM biz_contract
WHERE del_flag = '0';
```

### 9.2 单合同档案聚合数据

```sql
SET @contract_id = 1;

SELECT contract_id, contract_no, contract_name, customer_name, room_name,
       start_date, end_date, monthly_rent, contract_status, park_id
FROM biz_contract
WHERE contract_id = @contract_id
  AND del_flag = '0';

SELECT COUNT(*) AS payment_count
FROM biz_contract_payment
WHERE contract_id = @contract_id;

SELECT COUNT(*) AS change_count
FROM biz_contract_change
WHERE contract_id = @contract_id
  AND del_flag = '0';

SELECT COUNT(*) AS termination_count
FROM biz_termination
WHERE contract_id = @contract_id
  AND del_flag = '0';

SELECT COUNT(*) AS log_count
FROM biz_contract_log
WHERE contract_id = @contract_id;
```

### 9.3 空关联兼容检查

```sql
SELECT c.contract_id, c.contract_no, c.customer_id, c.customer_name
FROM biz_contract c
LEFT JOIN biz_customer cu ON cu.customer_id = c.customer_id
WHERE c.del_flag = '0'
  AND c.customer_id IS NOT NULL
  AND cu.customer_id IS NULL;

SELECT c.contract_id, c.contract_no, c.park_id
FROM biz_contract c
LEFT JOIN ics_park p ON p.id = c.park_id
WHERE c.del_flag = '0'
  AND c.park_id IS NOT NULL
  AND p.id IS NULL;
```

## 10. 推荐迁移顺序

1. 先完成合同基础模块：`biz_contract`、`biz_contract_log`、合同列表、合同详情。
2. 在合同基础 Work Tree 中增加合同归档菜单、列表和详情抽屉。
3. 先让归档详情只展示合同档案和日志。
4. 缴费、变更、退租模块迁移后，再把对应标签页从空数组切换为真实数据。
5. 模板导出和合同打印迁移后，再开放导出、打印按钮。
6. 最后按第 8 节校验清单逐项回归。

建议如果单独开 Work Tree：

```bash
cd /Users/jiakangli/Desktop/bladex-park
git worktree add ../bladex-park-migrate-contract-archive -b codex/migrate-contract-archive master
```

如果合同基础模块还未合并，合同归档可以先放在 `codex/migrate-contract-base` 分支内开发，避免重复改合同主档接口和菜单。
