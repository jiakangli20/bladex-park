# BladeX 合同管理模块迁移清单

本文档用于把 RuoYi 项目中的合同管理域迁移到 BladeX。范围以当前页面截图中的合同管理菜单为边界：费项配置、合同管理、退租管理、合同到期提醒、合同变更审批、合同创建模板、合同打印模板、合同归档。目标是先制定可执行清单，再按子模块并行迁移，最后用校验清单逐项验收。

## 1. 当前状态

### 1.1 项目路径

- RuoYi 源工程：`/Users/jiakangli/桌面/ics-park`
- 当前工作区：`/Users/jiakangli/.codex/worktrees/8615/ics-park`
- BladeX 目标整体仓库：`/Users/jiakangli/Desktop/bladex-park`
- BladeX 数据库：`bladex_boot`
- RuoYi 数据库：`ry_ics`
- 已完成前置迁移：`ics_park`、`ics_building`、`ics_floor`、`ics_room` 已导入 `bladex_boot`

### 1.2 合同域当前数据规模

当前 `ry_ics` 数据：

| 表 | 记录数 | 说明 |
| --- | ---: | --- |
| `ics_expense_settings` | 1 | 费项配置 |
| `biz_contract` | 15 | 合同主档 |
| `biz_contract_payment` | 2 | 合同缴费计划/账单 |
| `biz_contract_log` | 34 | 合同操作日志 |
| `biz_termination` | 3 | 退租管理 |
| `biz_contract_change` | 0 | 合同变更审批 |
| `biz_document_template` | 2 | 合同/退租导出模板配置 |
| `biz_approval_flow` | 4 | 审批流程配置 |
| `biz_approval_node` | 3 | 审批节点 |
| `biz_approval_project` | 15 | 审批项目 |
| `biz_approval_log` | 26 | 审批日志 |
| `biz_approval_material` | 0 | 审批材料 |

### 1.3 演示数据备注

以下问题属于当前演示数据的不完整关联，不作为合同管理功能迁移的阻塞项。迁移优先级调整为：先保证合同管理菜单、接口、页面、权限按钮、核心业务动作全部迁到 BladeX；演示数据只要求列表和详情页面不报错，缺失关联可按空值、占位文本或历史名称字段降级展示。

| 问题 | 数量 | 处理策略 |
| --- | ---: | --- |
| `contract_orphan_park` | 1 | 不清洗数据；页面展示时允许园区为空，筛选逻辑不能报错 |
| `contract_orphan_customer` | 8 | 不清洗数据；优先展示合同表冗余的 `customer_name` |
| `termination_orphan_customer` | 3 | 不清洗数据；退租历史记录允许客户主档缺失 |

异常数据定位 SQL：

```sql
SELECT c.contract_id, c.contract_no, c.contract_name, c.park_id,
       c.customer_id, c.customer_name, c.room_id, c.building_id,
       c.contract_status, c.del_flag
FROM biz_contract c
LEFT JOIN ics_park p ON p.id = c.park_id
WHERE c.del_flag = '0'
  AND c.park_id IS NOT NULL
  AND p.id IS NULL;

SELECT c.contract_id, c.contract_no, c.contract_name, c.park_id,
       c.customer_id, c.customer_name, c.contract_status, c.del_flag
FROM biz_contract c
LEFT JOIN biz_customer cu ON cu.customer_id = c.customer_id
WHERE c.del_flag = '0'
  AND c.customer_id IS NOT NULL
  AND cu.customer_id IS NULL
ORDER BY c.contract_id;

SELECT t.termination_id, t.termination_no, t.contract_id, t.customer_id,
       t.customer_name, t.room_id, t.park_id, t.status,
       t.approval_status, t.del_flag
FROM biz_termination t
LEFT JOIN biz_customer cu ON cu.customer_id = t.customer_id
WHERE t.del_flag = '0'
  AND t.customer_id IS NOT NULL
  AND cu.customer_id IS NULL
ORDER BY t.termination_id;
```

当前异常记录摘要：

- 合同 `contract_id=3`，`contract_no=HT202606023213`，`park_id=2` 不存在。
- 合同 `contract_id=1,4,5,7,8,9,10,11` 的 `customer_id` 在 `biz_customer` 中不存在。
- 退租 `termination_id=1,2,3` 的 `customer_id` 在 `biz_customer` 中不存在。

## 2. 迁移总体原则

### 2.1 分阶段迁移

合同管理域不要一次性和审批中心、财务管理、企业服务一起迁。建议分三阶段：

1. 合同基础阶段：费项配置、合同主档、合同日志、合同到期提醒。
2. 履约财务阶段：缴费计划、账单列表、确认缴费、催缴。
3. 流程扩展阶段：退租、变更审批、合同/退租模板导出、归档。

### 2.2 第一阶段保留业务表结构

第一阶段建议保留以下表结构，不做大规模字段重构：

- `ics_expense_settings`
- `biz_contract`
- `biz_contract_payment`
- `biz_contract_log`
- `biz_termination`
- `biz_contract_change`
- `biz_document_template`

审批相关表 `biz_approval_*` 是否迁移，要与审批中心迁移策略统一。合同模块第一轮可以先对接现有表，后续再替换为 BladeX Flow。

### 2.3 统一 BladeX 风格

迁移后不再依赖 RuoYi：

| RuoYi 依赖 | BladeX 替换方向 |
| --- | --- |
| `BaseController` | BladeX 控制器基类或普通 Controller |
| `R`、`AjaxResult` | BladeX 标准 `R` |
| `startPage()`、`TableDataInfo` | MyBatis-Plus / BladeX 分页 |
| `getLoginName()`、`getParkId()` | BladeX 用户上下文 + 园区数据权限 |
| `User.isAdmin()` | BladeX 角色/用户权限判断 |
| RuoYi `@Excel`、`ExcelUtil` | BladeX 导入导出或独立工具 |
| 本地 `/dfs/upload` | BladeX 附件/OSS |

### 2.4 园区和租户不要混用

`park_id` 是业务园区维度，`tenant_id` 是 BladeX 租户维度。合同数据权限应基于园区授权或业务数据权限，不建议直接把 `park_id` 塞进 `tenant_id`。

## 3. 功能模块清单

### 3.1 费项配置

源代码：

- `ExpenseSettingsController.java`
- `ExpenseSettings.java`
- `IExpenseSettingsService.java`
- `ExpenseSettingsServiceImpl.java`
- `ExpenseSettingsMapper.java`
- `ExpenseSettingsMapper.xml`
- `ruoyi-ui/src/api/business/expenseSettings.js`
- `ruoyi-ui/src/views/business/ExpenseSettingsList.vue`
- `ruoyi-ui/src/views/business/modules/ExpenseSettingsModal.vue`

表：`ics_expense_settings`

功能：

- 费项列表
- 新增费项
- 修改费项
- 删除费项
- 启用/停用
- 按费项名称、类型筛选

数据流：

```text
前端费项配置
  -> expenseSettings list/save/update/enabled/remove
  -> ExpenseSettingsController
  -> ExpenseSettingsService
  -> ics_expense_settings
```

关联：

- 合同费用字段：租金、物业费、管理费、公摊费、滞纳金。
- 后续财务账单可复用费项类型、单位、税率。

### 3.2 合同主档

源代码：

- `ContractController.java`
- `Contract.java`
- `ContractService.java`
- `ContractServiceImpl.java`
- `ContractMapper.java`
- `ContractMapper.xml`
- `ContractLogMapper.java`
- `ContractLogMapper.xml`
- `ruoyi-ui/src/api/business/contract.js`
- `ruoyi-ui/src/views/business/ContractList.vue`

表：

- `biz_contract`
- `biz_contract_log`

功能：

- 合同列表
- 合同详情
- 新增合同
- 从客户创建合同草稿
- 修改合同
- 删除合同，逻辑删除
- 续签合同
- 终止合同
- 提交合同审批
- 导出合同会签审批表
- 合同打印
- 合同操作日志

数据流：

```text
合同页面
  -> 合同 CRUD / 续签 / 终止 / 提交审批
  -> ContractController
  -> ContractService
  -> biz_contract
  -> biz_contract_payment 自动生成缴费计划
  -> biz_contract_log 写操作日志
  -> biz_approval_project 提交审批
```

关联：

- `ics_park`：`biz_contract.park_id`
- `biz_customer`：`customer_id`
- `ics_room`：`room_id`、`room_ids`
- `ics_building`：`building_id`、`building_ids`
- `biz_approval_project`：合同审批
- `biz_contract_payment`：合同缴费计划
- `biz_contract_log`：操作日志
- `biz_termination`：合同退租
- `biz_contract_change`：合同变更

### 3.3 缴费计划/账单

源代码：

- `ContractController.paymentList`
- `ContractController.paymentByContract`
- `ContractController.confirmPayment`
- `ContractController.remind`
- `ContractPayment.java`
- `ContractPaymentMapper.java`
- `ContractPaymentMapper.xml`
- `ruoyi-ui/src/views/business/PaymentList.vue`

表：`biz_contract_payment`

功能：

- 全局缴费列表
- 按合同查看缴费计划
- 新增合同时自动生成租金、物业费、管理费、公摊费账期
- 确认缴费
- 催缴提醒
- 逾期账单筛选
- 收款通知、收支流水页面复用列表能力

数据流：

```text
合同保存/续签
  -> generatePaymentPlan
  -> biz_contract_payment

财务/缴费页面
  -> payment list/confirm/remind
  -> ContractService
  -> biz_contract_payment
  -> biz_contract_log
```

关联：

- `biz_contract.contract_id`
- `ics_park.park_id`
- 合同状态、账单状态、催缴状态。

### 3.4 退租管理

源代码：

- `TerminationController.java`
- `Termination.java`
- `ITerminationService.java`
- `TerminationServiceImpl.java`
- `TerminationMapper.java`
- `TerminationMapper.xml`
- `ruoyi-ui/src/api/business/termination.js`
- `ruoyi-ui/src/views/business/TerminationList.vue`
- `ruoyi-ui/src/views/business/ContractTerminationAgreementList.vue`

表：`biz_termination`

功能：

- 退租列表
- 退租详情
- 新增退租
- 修改退租
- 删除退租
- 提交退租审批
- 退租审批通过/驳回
- 审批通过后确认退租
- 确认退租后终止合同并释放房源
- 导出退租审批表

数据流：

```text
合同/退租页面
  -> 新增退租
  -> 校验合同、客户、入驻状态
  -> biz_termination
  -> 提交审批
  -> biz_approval_project
  -> 审批通过
  -> confirmTermination
  -> biz_contract.contract_status = 4
  -> ics_room.status = 0
```

关联：

- `biz_contract`
- `biz_customer`
- `ics_room`
- `ics_park`
- `biz_approval_project`
- `biz_document_template`

### 3.5 合同变更审批

源代码：

- `ContractChangeController.java`
- `ContractChange.java`
- `IContractChangeService.java`
- `ContractChangeServiceImpl.java`
- `ContractChangeMapper.java`
- `ContractChangeMapper.xml`
- `ruoyi-ui/src/api/business/contractChange.js`
- `ruoyi-ui/src/views/business/ContractChangeList.vue`

表：`biz_contract_change`

功能：

- 变更申请列表
- 新增变更申请
- 修改变更申请
- 删除变更申请
- 提交变更
- 审批通过/驳回
- 审批通过后回写合同租金、月租、结束日期

数据流：

```text
合同变更页面
  -> 选择合同
  -> 创建变更单
  -> biz_contract_change 草稿
  -> 提交
  -> approval_status = 1
  -> 审批通过
  -> 回写 biz_contract
  -> approval_status = 2
```

关联：

- `biz_contract`
- `biz_customer`
- `ics_park`
- 后续若接 BladeX Flow，需要与审批中心统一。

### 3.6 合同模板与打印

源代码：

- `DocumentTemplate.java`
- `DocumentTemplateMapper.java`
- `DocumentTemplateMapper.xml`
- `DocumentTemplateExportService.java`
- `ruoyi-ui/src/views/business/ContractCreateTemplateList.vue`
- `ruoyi-ui/src/views/business/ContractPrintTemplateList.vue`

表：`biz_document_template`

现有模板配置：

- `contract-approval`：合同审批表
- `termination-approval`：退租审批表

功能：

- 模板配置维护
- 合同审批表导出
- 退租审批表导出
- 合同打印 HTML 预览
- 输出目录与附件存储适配

关联：

- `uploadPath/contract-template`
- RuoYi 本地文件路径
- BladeX 附件/OSS

### 3.7 合同归档与到期提醒

源页面：

- `ContractExpiringList.vue`
- `ContractArchiveList.vue`
- `ContractShareReminderList.vue`

功能：

- 到期合同列表
- 按 `renewal_remind_days` 提醒
- 合同归档列表
- 分成合同提醒，当前代码需进一步确认真实业务表

数据流：

```text
合同到期提醒
  -> selectExpiringContracts
  -> biz_contract
  -> end_date <= today + renewal_remind_days
  -> contract_status = 1
```

## 4. 数据表迁移清单

### 4.1 必迁表

- [ ] `ics_expense_settings`
- [ ] `biz_contract`
- [ ] `biz_contract_payment`
- [ ] `biz_contract_log`
- [ ] `biz_termination`
- [ ] `biz_contract_change`
- [ ] `biz_document_template`

### 4.2 条件迁移表

审批中心若暂未迁移，合同模块可先保留以下 RuoYi 审批表并继续使用：

- [ ] `biz_approval_flow`
- [ ] `biz_approval_node`
- [ ] `biz_approval_project`
- [ ] `biz_approval_material`
- [ ] `biz_approval_log`

如果决定直接接入 BladeX Flow，则这些表只作为历史数据来源，不应继续扩展。

### 4.3 依赖已完成表

- `ics_park`
- `ics_building`
- `ics_floor`
- `ics_room`

## 5. BladeX 落地建议

当前 BladeX 是整体仓库 `/Users/jiakangli/Desktop/bladex-park`，后端与前端都在同一个 Git 仓库内。建议在整体仓库根目录开 Work Tree。

后端建议落点：

- 后端模块包：`org.springblade.modules.contract` 或并入 `org.springblade.modules.ics.contract`
- 推荐第一阶段用 `org.springblade.modules.contract`
- Controller：`BladeX-Boot` 或 `Boot` 对应源码目录下的 `controller`
- Entity/DTO/VO：按 BladeX 现有 `pojo/entity`、`pojo/dto`、`pojo/vo` 风格
- Mapper：MyBatis-Plus Mapper + 必要 XML
- Service：按 BladeX `IService` / `ServiceImpl` 风格

前端建议落点：

- API：`saber3/src/api/contract`
- 页面：`saber3/src/views/contract`
- 菜单：使用 `blade_menu` 动态路由，不建议写死路由

建议 API 前缀：

| 模块 | BladeX 路径 |
| --- | --- |
| 费项配置 | `/blade-contract/expense` |
| 合同主档 | `/blade-contract/contract` |
| 缴费计划 | `/blade-contract/payment` |
| 退租管理 | `/blade-contract/termination` |
| 合同变更 | `/blade-contract/change` |
| 模板管理 | `/blade-contract/template` |
| 合同归档 | `/blade-contract/archive` |

## 6. 并行迁移 Work Tree 方案

### 6.1 建议先开合同基础 Work Tree

```bash
cd /Users/jiakangli/Desktop/bladex-park
git worktree add ../bladex-park-migrate-contract-base -b codex/migrate-contract-base master
```

第一轮只迁：

- 费项配置
- 合同主档
- 合同日志
- 合同到期提醒

### 6.2 后续并行 Work Tree

```bash
cd /Users/jiakangli/Desktop/bladex-park
git worktree add ../bladex-park-migrate-contract-payment -b codex/migrate-contract-payment master
git worktree add ../bladex-park-migrate-contract-termination -b codex/migrate-contract-termination master
git worktree add ../bladex-park-migrate-contract-change -b codex/migrate-contract-change master
git worktree add ../bladex-park-migrate-contract-template -b codex/migrate-contract-template master
```

### 6.3 并行边界

| Work Tree | 目标 | 可独立完成 | 依赖 |
| --- | --- | --- | --- |
| `migrate-contract-base` | 费项、合同主档、日志、到期提醒 | 合同 CRUD、列表详情、续签、终止、日志 | 园区资产、客户数据 |
| `migrate-contract-payment` | 缴费计划、账单、催缴 | 自动生成缴费计划、确认缴费、催缴 | 合同主档 |
| `migrate-contract-termination` | 退租管理 | 退租 CRUD、提交审批、确认退租 | 合同主档、房源、客户、审批 |
| `migrate-contract-change` | 合同变更审批 | 变更 CRUD、提交、审批通过回写合同 | 合同主档、审批 |
| `migrate-contract-template` | 模板与导出打印 | 模板配置、合同审批表、退租审批表、打印 | 合同主档、退租、附件/OSS |

原则：DDL、菜单、通用字典、API 契约由集成分支统一维护，子 Work Tree 不互相改对方模块。

## 7. 菜单与权限清单

建议 BladeX 菜单结构：

```text
合同管理
  -> 费项配置
  -> 合同管理
  -> 退租管理
  -> 合同到期提醒
  -> 合同变更审批
  -> 合同创建模板
  -> 合同打印模板
  -> 合同归档
```

建议权限编码：

| 功能 | 权限编码 |
| --- | --- |
| 费项列表 | `contract:expense:list` |
| 费项新增 | `contract:expense:add` |
| 费项修改 | `contract:expense:edit` |
| 费项删除 | `contract:expense:delete` |
| 费项启停 | `contract:expense:status` |
| 合同列表 | `contract:contract:list` |
| 合同详情 | `contract:contract:detail` |
| 合同新增 | `contract:contract:add` |
| 合同修改 | `contract:contract:edit` |
| 合同删除 | `contract:contract:delete` |
| 合同续签 | `contract:contract:renew` |
| 合同终止 | `contract:contract:terminate` |
| 合同提交审批 | `contract:contract:submit` |
| 合同打印 | `contract:contract:print` |
| 合同审批表导出 | `contract:contract:export-approval` |
| 缴费列表 | `contract:payment:list` |
| 确认缴费 | `contract:payment:confirm` |
| 催缴 | `contract:payment:remind` |
| 退租列表 | `contract:termination:list` |
| 退租新增 | `contract:termination:add` |
| 退租修改 | `contract:termination:edit` |
| 退租删除 | `contract:termination:delete` |
| 退租提交审批 | `contract:termination:submit` |
| 退租确认 | `contract:termination:confirm` |
| 变更列表 | `contract:change:list` |
| 变更新增 | `contract:change:add` |
| 变更提交 | `contract:change:submit` |
| 变更审批 | `contract:change:approve` |
| 模板管理 | `contract:template:list` |
| 合同归档 | `contract:archive:list` |

## 8. 演示数据兼容校验清单

当前项目以演示功能迁移为主，以下 SQL 用于发现页面可能遇到的空关联，不要求全部清零。迁移实现时要保证这些异常数据不会导致列表、详情、提交、导出接口 500。

```sql
SELECT 'contract_orphan_park' AS check_item, COUNT(*) invalid_count
FROM biz_contract c
LEFT JOIN ics_park p ON p.id = c.park_id
WHERE c.del_flag = '0'
  AND c.park_id IS NOT NULL
  AND p.id IS NULL
UNION ALL
SELECT 'contract_orphan_customer', COUNT(*)
FROM biz_contract c
LEFT JOIN biz_customer cu ON cu.customer_id = c.customer_id
WHERE c.del_flag = '0'
  AND c.customer_id IS NOT NULL
  AND cu.customer_id IS NULL
UNION ALL
SELECT 'contract_orphan_room', COUNT(*)
FROM biz_contract c
LEFT JOIN ics_room r ON r.id = c.room_id
WHERE c.del_flag = '0'
  AND c.room_id IS NOT NULL
  AND r.id IS NULL
UNION ALL
SELECT 'contract_orphan_building', COUNT(*)
FROM biz_contract c
LEFT JOIN ics_building b ON b.id = c.building_id
WHERE c.del_flag = '0'
  AND c.building_id IS NOT NULL
  AND b.id IS NULL
UNION ALL
SELECT 'payment_orphan_contract', COUNT(*)
FROM biz_contract_payment p
LEFT JOIN biz_contract c ON c.contract_id = p.contract_id
WHERE c.contract_id IS NULL
UNION ALL
SELECT 'log_orphan_contract', COUNT(*)
FROM biz_contract_log l
LEFT JOIN biz_contract c ON c.contract_id = l.contract_id
WHERE c.contract_id IS NULL
UNION ALL
SELECT 'termination_orphan_contract', COUNT(*)
FROM biz_termination t
LEFT JOIN biz_contract c ON c.contract_id = t.contract_id
WHERE t.del_flag = '0'
  AND t.contract_id IS NOT NULL
  AND c.contract_id IS NULL
UNION ALL
SELECT 'termination_orphan_customer', COUNT(*)
FROM biz_termination t
LEFT JOIN biz_customer cu ON cu.customer_id = t.customer_id
WHERE t.del_flag = '0'
  AND t.customer_id IS NOT NULL
  AND cu.customer_id IS NULL
UNION ALL
SELECT 'termination_orphan_room', COUNT(*)
FROM biz_termination t
LEFT JOIN ics_room r ON r.id = t.room_id
WHERE t.del_flag = '0'
  AND t.room_id IS NOT NULL
  AND r.id IS NULL
UNION ALL
SELECT 'contract_change_orphan_contract', COUNT(*)
FROM biz_contract_change ch
LEFT JOIN biz_contract c ON c.contract_id = ch.contract_id
WHERE ch.del_flag = '0'
  AND c.contract_id IS NULL;
```

兼容要求：

- [ ] 合同列表遇到缺失园区、客户、房间、楼栋时不报错
- [ ] 合同详情遇到缺失客户主档时优先展示合同表冗余字段
- [ ] 缴费列表遇到缺失合同时不报错，并能定位异常账单
- [ ] 合同日志遇到缺失合同时不报错，并能按已有合同正常查询
- [ ] 退租列表遇到缺失客户或房间时不报错
- [ ] 合同变更遇到缺失合同时不报错，并能排除无效记录
- [ ] 新增、修改、提交审批等新功能仍需按正常主数据校验，不允许继续制造新的脏关联

## 9. 模块验收清单

### 9.1 费项配置验收

- [ ] 列表显示 `ics_expense_settings` 数据
- [ ] 新增费项成功
- [ ] 修改费项成功
- [ ] 删除费项成功
- [ ] 启用/停用成功
- [ ] 权限按钮生效
- [ ] 税率、单位、类型字段显示正确

### 9.2 合同主档验收

- [ ] 合同列表显示 `biz_contract` 数据
- [ ] 园区筛选可用
- [ ] 客户、楼栋、房间字段显示正确
- [ ] 合同详情可打开
- [ ] 新增合同自动生成合同编号
- [ ] 新增合同自动生成缴费计划
- [ ] 修改合同写入日志
- [ ] 删除合同为逻辑删除
- [ ] 续签合同会将原合同置为已续签并生成新合同
- [ ] 终止合同会将合同状态置为已终止
- [ ] 到期提醒只显示履约中且到期日期满足提醒规则的合同
- [ ] 非授权园区数据不可见

### 9.3 缴费/账单验收

- [ ] 缴费列表显示账单
- [ ] 按缴费状态筛选可用
- [ ] 按园区筛选可用
- [ ] 确认缴费后 `pay_status = 1`
- [ ] 确认缴费后写入 `pay_time`
- [ ] 催缴后 `remind_status = 1`
- [ ] 催缴后写入 `remind_time`
- [ ] 确认缴费和催缴都写入合同日志

### 9.4 退租验收

- [ ] 退租列表显示
- [ ] 退租详情显示
- [ ] 新增退租必须关联合同
- [ ] 未完成入驻审批的客户不能发起退租
- [ ] 提交审批后状态变为待审批
- [ ] 审批通过后允许确认退租
- [ ] 确认退租后合同状态变为已终止
- [ ] 确认退租后房源状态释放为空闲
- [ ] 导出退租审批表可用

### 9.5 合同变更验收

- [ ] 变更列表显示
- [ ] 新增变更会带出合同原租金、原月租、原结束日期
- [ ] 草稿或驳回状态可修改
- [ ] 待审批状态不可修改
- [ ] 审批通过后回写合同新租金、新月租、新结束日期
- [ ] 审批驳回后允许再次修改提交

### 9.6 模板/归档验收

- [ ] 合同审批模板配置存在
- [ ] 退租审批模板配置存在
- [ ] 合同审批表导出可用
- [ ] 退租审批表导出可用
- [ ] 合同打印 HTML 预览可用
- [ ] 输出文件进入 BladeX 附件/OSS 或兼容本地路径
- [ ] 合同归档列表不会显示已删除合同

## 10. 集成回归清单

- [ ] BladeX 后端启动成功
- [ ] BladeX 前端启动成功
- [ ] 登录正常
- [ ] 合同管理菜单可见
- [ ] 左侧子菜单顺序与截图一致
- [ ] 园区资产数据仍可被合同选择
- [ ] 客户数据仍可被合同选择
- [ ] 合同提交审批不影响审批中心其他业务
- [ ] 退租确认不会破坏房源数据
- [ ] 缴费确认不会破坏财务账单
- [ ] 权限按钮按角色生效
- [ ] 普通用户不能查看未授权园区合同

## 11. 推荐下一步

1. 在 `bladex-park` 根目录创建 `codex/migrate-contract-base` Work Tree。
2. 先迁 `ics_expense_settings`、`biz_contract`、`biz_contract_log`，同时实现合同列表/详情/新增/修改/删除/到期提醒。
3. 基础链路实现时对历史演示数据做空关联兼容，但新建/修改数据必须继续校验园区、客户、房间等主数据。
4. 合同基础链路跑通后，再开并行 Work Tree 迁缴费、退租、变更、模板。
5. 每个子模块完成后必须按第 9 节验收清单逐项核对。
