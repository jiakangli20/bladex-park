# BladeX 入驻管理 - 商机管理二级菜单迁移清单

本文档只细化“入驻管理”下的二级菜单“商机管理”迁移。它是招商入驻链路中的独立施工清单，迁移完成后必须按校验清单逐项核对，确认客户、标签、附件、审批等关联模块没有遗漏。

## 1. 菜单定位与迁移目标

### 1.1 菜单层级

| 层级 | 源菜单名 | 源菜单信息 | BladeX 建议 |
| --- | --- | --- | --- |
| 一级菜单 | 入驻管理 | `menu_id=2229`，`menu_type=M` | 保持为业务目录 |
| 二级菜单 | 商机管理 | 源 SQL 曾指向 `business/DeclareService` | 改挂真正商机页面 |
| 编辑页 | 新增/编辑/查看商机 | 源路由 `/business/service/declare/edit` | BladeX 中作为隐藏路由或同页弹窗 |

源项目中需要注意的菜单差异：

- `sql/business_service_submenus.sql` 和 `sql/biz_customer_menu.sql` 中，“商机管理”菜单指向 `business/DeclareService`。
- 实际商机页面文件是 `BusinessOpportunityList.vue` 和 `BusinessOpportunityEdit.vue`。
- 迁移到 BladeX 时，不建议继续挂旧占位组件，应明确挂到商机管理真实页面。

### 1.2 迁移目标

- 在 BladeX “入驻管理”下新增/修正二级菜单“商机管理”。
- 完成商机列表、统计、新增、编辑、查看、删除、跟进、附件、标签、背调、提交审核。
- 保持商机与客户、客户标签、入驻审批流程之间的数据链路可追踪。
- 每个子能力完成后均按本文档校验，不允许只验证页面能打开。

## 2. 功能模块详细清单

### 2.1 商机列表

源文件：

- `ruoyi-ui/src/views/business/BusinessOpportunityList.vue`
- `ruoyi-ui/src/api/business/opportunity.js`
- `BusinessOpportunityController.list`
- `BusinessOpportunityMapper.selectBusinessOpportunityList`

功能点：

- [ ] 商机分页列表。
- [ ] 商机统计卡片：商机总数、跟进中、已成交、已流失。
- [ ] 商机状态标签展示。
- [ ] 意向载体类型格式化展示。
- [ ] 新增、编辑、跟进、查看、删除操作入口。

查询条件需要保留：

| 查询字段 | 说明 |
| --- | --- |
| `customerId` | 关联客户 |
| `opportunityStatus` | 商机状态 |
| `submittedAuditFlag` | 是否提交审核 |
| `channel` | 招商渠道 |
| `followUser` | 跟进人 |
| `leaseTermLabel` | 租期分档 |
| `intentAreaMin`、`intentAreaMax` | 意向面积范围 |
| `beginTime`、`endTime` | 创建时间范围 |
| `keyword` | 企业名/联系人模糊搜索 |
| `contactKeyword` | 联系人/联系电话模糊搜索 |
| `carrierTypeArray` | 意向载体多选 |

### 2.2 商机新增/编辑/查看

源文件：

- `ruoyi-ui/src/views/business/BusinessOpportunityEdit.vue`
- `BusinessOpportunityController.save`
- `BusinessOpportunityController.update`
- `BusinessOpportunityServiceImpl.insertBusinessOpportunity`
- `BusinessOpportunityServiceImpl.updateBusinessOpportunity`

页面表单分区：

| 分区 | 关键字段 |
| --- | --- |
| 跟进信息 | 跟进人、商机状态、跟进内容、下次跟进时间 |
| 企业基本信息 | 企业名称、统一信用代码、成立日期、注册资本、企业类型、行业类型、经营范围、注册地址、股权结构、组织架构、客户标签 |
| 经营状况信息 | 主营业务、上年度营收、主要合作客户、违法违规、失信记录、行业处罚 |
| 入驻需求信息 | 意向载体类型、意向面积、使用用途、租赁期限、装修要求、配套需求 |
| 联系人与招商信息 | 负责人姓名、联系电话、邮箱、职务、招商渠道、第三方渠道 |

核心业务规则：

- [ ] 新增商机时服务端生成 `opportunity_no`，格式为 `SJ-yyyyMMdd-0001`。
- [ ] 新增默认 `del_flag='0'`。
- [ ] 新增默认 `submitted_audit_flag='0'`。
- [ ] `DRAFT`、`AUDIT` 状态会规范化为 `INITIAL`。
- [ ] `carrierTypeArray` 要转换为逗号分隔的 `carrier_types`。
- [ ] `leaseTermYears` 要自动计算 `leaseTermLabel`：`1年以内`、`1-3年`、`3-5年`、`5年以上`。
- [ ] 保存时优先按 `customerId` 关联客户，其次按 `creditCode` 匹配已有客户。
- [ ] 已提交审核的商机不可编辑。
- [ ] 保存后如填写了跟进内容，需要新增跟进记录。

### 2.3 商机跟进

源文件：

- `BusinessOpportunityController.followList`
- `BusinessOpportunityController.addFollow`
- `BusinessOpportunityServiceImpl.addFollowRecord`
- `biz_business_opportunity_follow`

功能点：

- [ ] 查询商机跟进记录。
- [ ] 新增跟进记录。
- [ ] 新增跟进时更新商机主表 `opportunity_status`。
- [ ] 新增跟进时更新 `last_follow_time`。
- [ ] 新增跟进时更新 `next_follow_time`。
- [ ] 状态为 `DEAL` 时，`next_follow_time` 应清空。

### 2.4 商机附件

源文件：

- `BusinessOpportunityController.fileList`
- `BusinessOpportunityController.uploadFile`
- `BusinessOpportunityServiceImpl.addFile`
- `biz_business_opportunity_file`

功能点：

- [ ] 查询商机附件列表。
- [ ] 上传商机附件。
- [ ] 保存文件名、URL、后缀、大小、上传人、上传时间。

迁移决策：

- RuoYi 当前使用 `DfsConfig` 和 `FileUploadUtils`。
- BladeX 中建议改为 BladeX resource/OSS/attach 能力。
- 历史文件 URL 先保留兼容，新上传文件走 BladeX 资源。

### 2.5 商机标签

源文件：

- `CustomerTagSelector.vue`
- `TagMapper.selectTagsByOpportunityId`
- `TagMapper.batchInsertOpportunityTag`
- `TagMapper.deleteOpportunityTags`
- `biz_business_opportunity_tag`

功能点：

- [ ] 商机编辑页可选择客户标签。
- [ ] 商机详情可展示已选标签。
- [ ] 商机标签保存到 `biz_business_opportunity_tag`。
- [ ] 商机无标签但有关联客户时，兜底展示客户标签。
- [ ] 商机关联客户后，设置商机标签会同步覆盖客户标签。

迁移决策：

- 商机标签依赖客户标签模块先可用。
- `CustomerTagSelector` 应迁为 Saber3 可复用组件，客户管理和商机管理共同使用。
- 同步客户标签是覆盖式同步：先删除客户原标签，再插入商机标签。

### 2.6 背景调查与行业准入

源文件：

- `BusinessOpportunityController.background`
- `BusinessOpportunityController.backgroundByName`
- `BusinessOpportunityServiceImpl.queryBackgroundInvestigationByName`
- `CustomerController` 中行业准入相关接口
- `BusinessOpportunityEdit.vue`

功能点：

- [ ] 按商机 ID 查询背景调查。
- [ ] 按企业名称查询背景调查。
- [ ] 页面展示涉诉、被执行人、处罚、股东/高管关联风险。
- [ ] 行业类型输入后可检测行业准入。

迁移决策：

- 当前背景调查是本地空结果/模拟结构，不是外部征信接口。
- 迁移时必须保持返回结构稳定：`found`、`litigationList`、`executorList`、`penaltyList`、`relatedRiskList`。
- 行业准入依赖客户模块的 `biz_industry_access_rule`。

### 2.7 提交审核

源文件：

- `BusinessOpportunityController.submitAudit`
- `BusinessOpportunityServiceImpl.createApprovalProjectFromOpportunity`
- `ApprovalProjectMapper`
- `ApprovalFlowMapper`
- `ApprovalNodeMapper`
- `ApprovalLogMapper`

功能点：

- [ ] 商机可提交入驻审核。
- [ ] 可选传入 `flowId`。
- [ ] 未传 `flowId` 时自动选择当前园区或全局的 `tenant_entry` 启用流程。
- [ ] 创建或复用审批项目 `biz_approval_project`。
- [ ] 通过 `business_type='tenant_entry'`、`business_id=opportunity_id` 关联商机。
- [ ] 写入提交日志 `biz_approval_log`。
- [ ] 更新审批项目当前节点。
- [ ] 回写商机审核相关字段。

迁移注意：

- 审批流不是商机管理内部表，但商机提交审核强依赖它。
- 商机管理验收时至少要有一条启用的 `tenant_entry` 审批流程。
- 审批通过/驳回的完整流转属于“项目审核/商机审核”菜单，但商机提交入口必须在本菜单可用。

## 3. 数据表与字段清单

### 3.1 主表 `biz_business_opportunity`

| 字段 | 说明 | 迁移注意 |
| --- | --- | --- |
| `opportunity_id` | 商机 ID | 源库自增，BladeX 可保留业务主键名 |
| `park_id` | 园区 ID | 依赖 `ics_park` |
| `customer_id` | 客户 ID | 可为空，但有关联时必须存在于 `biz_customer` |
| `opportunity_no` | 商机编号 | 唯一 |
| `enterprise_name` | 企业名称 | 必填 |
| `credit_code` | 统一社会信用代码 | 必填，用于关联客户 |
| `carrier_types` | 意向载体类型 | 逗号分隔 |
| `intent_area` | 意向面积 | 数值 |
| `lease_term_years` | 租赁期限 | 数值 |
| `lease_term_label` | 租期分档 | 服务端自动补 |
| `follow_user` | 跟进人 | 需映射 BladeX 用户账号 |
| `opportunity_status` | 商机状态 | 见状态枚举 |
| `approval_project_id` | 审批项目 ID | 提交审核后回写 |
| `submitted_audit_flag` | 是否提交审核 | `0/1` |
| `last_follow_time` | 最后跟进时间 | 跟进时更新 |
| `next_follow_time` | 下次跟进时间 | 成交时清空 |
| `del_flag` | 删除标志 | 第一阶段建议保留 |

### 3.2 子表与关系表

| 表 | 说明 | 主关联 |
| --- | --- | --- |
| `biz_business_opportunity_follow` | 商机跟进记录 | `opportunity_id` |
| `biz_business_opportunity_file` | 商机附件 | `opportunity_id` |
| `biz_business_opportunity_tag` | 商机标签关系 | `opportunity_id + tag_id` |
| `biz_customer` | 客户主表 | `customer_id`、`credit_code` |
| `biz_tag` | 标签字典 | `tag_id` |
| `biz_customer_tag` | 客户标签关系 | 标签同步依赖 |
| `biz_approval_project` | 审批项目 | `business_type + business_id` |
| `biz_approval_flow` | 审批流程 | `flow_id`、`business_type` |
| `biz_approval_node` | 审批节点 | `flow_id` |
| `biz_approval_log` | 审批日志 | `project_id` |

### 3.3 商机状态枚举

| 状态值 | 页面文案 | 说明 |
| --- | --- | --- |
| `LEAD` | 潜在线索 | 初始线索 |
| `INITIAL` | 初步沟通 | 常用默认状态 |
| `DEEP` | 深入洽谈 | 深度跟进 |
| `DEAL` | 成交 | 成交后清空下次跟进时间 |
| `LOST` | 流失 | 商机流失 |
| `DRAFT` | 初步沟通 | 旧状态，服务端迁移为 `INITIAL` |
| `AUDIT` | 初步沟通 | 旧状态，服务端迁移为 `INITIAL` |

## 4. 数据流走向

### 4.1 新增商机数据流

```text
商机编辑页
  -> 保存表单
  -> BusinessOpportunityController.save
  -> 生成 opportunity_no
  -> 规范化状态/载体类型/租期分档
  -> 按 customer_id 或 credit_code 关联客户
  -> 插入 biz_business_opportunity
  -> 保存 biz_business_opportunity_tag
  -> 如有关联客户，同步 biz_customer_tag
  -> 如填写跟进内容，插入 biz_business_opportunity_follow
```

关联模块：

- 客户管理：用于关联客户和信用代码匹配。
- 客户标签：用于标签选择和标签同步。
- 园区管理：用于 `park_id` 数据过滤。

### 4.2 编辑商机数据流

```text
商机列表/详情进入编辑
  -> 查询商机详情
  -> 加载跟进记录、附件、标签
  -> 提交修改
  -> 判断 submitted_audit_flag
  -> 未提交审核允许更新
  -> 更新主表
  -> 需要时重置商机标签并同步客户标签
```

关键边界：

- 已提交审核的商机不可编辑。
- 如果只修改跟进，建议走跟进接口，不直接改主表。

### 4.3 跟进数据流

```text
商机列表点击跟进
  -> 新增 biz_business_opportunity_follow
  -> 更新 biz_business_opportunity.opportunity_status
  -> 更新 last_follow_time
  -> 更新 next_follow_time
  -> 刷新统计和列表
```

关联模块：

- 用户体系：跟进人应映射 BladeX 当前用户或可选用户。
- 消息/提醒：本轮不迁，但 `next_follow_time` 后续可接提醒。

### 4.4 提交审核数据流

```text
商机点击提交审核
  -> 校验商机存在
  -> 查询 tenant_entry 审批流
  -> 读取 biz_approval_node 或 flow.node_config
  -> 创建/复用 biz_approval_project
  -> 写入 biz_approval_log
  -> 更新审批项目为审批中
  -> 回写商机 approval_project_id/submitted_audit_flag/opportunity_status
```

关联模块：

- 项目审核/商机审核：承接审批动作。
- 客户管理：审批通过后可能创建或更新客户。
- 客户标签：审批通过后可能同步商机标签到客户标签。

## 5. BladeX 迁移设计口径

### 5.1 后端包与类建议

按 BladeX-Boot 当前项目结构落位，命名可跟随现有业务模块：

| 层 | 建议文件 |
| --- | --- |
| Entity | `BusinessOpportunity`、`BusinessOpportunityFollow`、`BusinessOpportunityFile` |
| VO | `BusinessOpportunityVO`、`BusinessOpportunityDetailVO` |
| DTO | `BusinessOpportunityDTO`、`BusinessOpportunityFollowDTO` |
| Wrapper | `BusinessOpportunityWrapper` |
| Mapper | `BusinessOpportunityMapper` + XML |
| Service | `IBusinessOpportunityService`、`BusinessOpportunityServiceImpl` |
| Controller | `BusinessOpportunityController` |

BladeX 改造点：

- Controller 使用 `BladeController`、`R`、OpenAPI3 注解。
- 分页使用 `Query` + `Condition.getPage(query)` + `IPage`。
- 业务异常改为 BladeX 统一异常。
- 获取当前用户改为 `AuthUtil.getUser()`。
- 权限使用 `@PreAuth` 或接口权限编码。
- 如果接入数据权限，列表接口按 `park_id` 或 BladeX 数据权限过滤。

### 5.2 接口建议

| 功能 | 源接口 | BladeX 建议 |
| --- | --- | --- |
| 详情 | `GET /business/opportunity/get/{id}` | `GET /business/opportunity/detail?id=` 或保留兼容 |
| 分页 | `GET /business/opportunity/list` | `GET /business/opportunity/page` |
| 统计 | `GET /business/opportunity/statistics` | 保留 |
| 新增 | `POST /business/opportunity/save` | 保留 |
| 修改 | `POST /business/opportunity/update` | 保留 |
| 删除 | `POST /business/opportunity/remove?ids=` | 保留 |
| 跟进列表 | `GET /business/opportunity/follow/list/{id}` | 保留 |
| 新增跟进 | `POST /business/opportunity/follow/add/{id}` | 保留 |
| 附件列表 | `GET /business/opportunity/file/list/{id}` | 保留 |
| 附件上传 | `POST /business/opportunity/file/upload` | 改 BladeX 上传实现 |
| 标签列表 | `GET /business/opportunity/tag/list/{id}` | 保留 |
| 设置标签 | `POST /business/opportunity/tag/set/{id}` | 保留 |
| 背调 | `GET /business/opportunity/background/{id}` | 保留 |
| 按名称背调 | `GET /business/opportunity/background/byName` | 保留 |
| 提交审核 | `POST /business/opportunity/submitAudit/{id}` | 保留 |

建议第一阶段保留源接口路径，降低前端改造成本；待页面稳定后再统一 BladeX REST 风格。

### 5.3 前端页面建议

| 页面 | 迁移目标 |
| --- | --- |
| `BusinessOpportunityList.vue` | Saber3 商机列表页，作为“商机管理”菜单组件 |
| `BusinessOpportunityEdit.vue` | Saber3 新增/编辑/查看页，建议隐藏路由 |
| `CustomerTagSelector.vue` | 改为 Saber3 复用组件 |
| `opportunity.js` | 改为 Saber3 API 模块 |

菜单建议：

| 菜单 | 建议路径 | 建议组件 | 权限编码 |
| --- | --- | --- | --- |
| 商机管理 | `/settlementManage/opportunity` | `business/BusinessOpportunityList` | `business:opportunity:list` |
| 商机新增 | 隐藏路由或页面动作 | `business/BusinessOpportunityEdit` | `business:opportunity:add` |
| 商机编辑 | 隐藏路由或页面动作 | `business/BusinessOpportunityEdit` | `business:opportunity:edit` |
| 商机查看 | 隐藏路由或详情抽屉 | `business/BusinessOpportunityEdit` 或详情抽屉 | `business:opportunity:view` |
| 商机删除 | 按钮 | 无组件 | `business:opportunity:remove` |
| 商机跟进 | 按钮/弹窗 | 列表页内弹窗 | `business:opportunity:follow` |
| 商机提交审核 | 按钮 | 列表/详情动作 | `business:opportunity:audit` |
| 商机附件上传 | 按钮 | 详情/编辑页 | `business:opportunity:file:upload` |

## 6. 迁移前准备清单

### 6.1 数据库准备

- [ ] 确认 `bladex_boot` 中是否已有 `biz_business_opportunity`。
- [ ] 确认 `bladex_boot` 中是否已有 `biz_business_opportunity_follow`。
- [ ] 确认 `bladex_boot` 中是否已有 `biz_business_opportunity_file`。
- [ ] 确认 `bladex_boot` 中是否已有 `biz_business_opportunity_tag`。
- [ ] 确认 `biz_customer` 已迁移或至少有可关联测试数据。
- [ ] 确认 `biz_tag`、`biz_tag_type`、`biz_customer_tag` 已迁移。
- [ ] 确认 `ics_park` 已迁移。
- [ ] 确认至少存在一个启用的 `tenant_entry` 审批流程。

### 6.2 数据质量检查

```sql
SELECT 'opportunity_missing_customer' AS check_item, COUNT(*) AS invalid_count
FROM ry_ics.biz_business_opportunity o
LEFT JOIN ry_ics.biz_customer c ON c.customer_id = o.customer_id
WHERE o.customer_id IS NOT NULL AND c.customer_id IS NULL
UNION ALL
SELECT 'opportunity_missing_park', COUNT(*)
FROM ry_ics.biz_business_opportunity o
LEFT JOIN ry_ics.ics_park p ON p.id = o.park_id
WHERE o.park_id IS NOT NULL AND p.id IS NULL
UNION ALL
SELECT 'follow_missing_opportunity', COUNT(*)
FROM ry_ics.biz_business_opportunity_follow f
LEFT JOIN ry_ics.biz_business_opportunity o ON o.opportunity_id = f.opportunity_id
WHERE o.opportunity_id IS NULL
UNION ALL
SELECT 'file_missing_opportunity', COUNT(*)
FROM ry_ics.biz_business_opportunity_file f
LEFT JOIN ry_ics.biz_business_opportunity o ON o.opportunity_id = f.opportunity_id
WHERE o.opportunity_id IS NULL
UNION ALL
SELECT 'opportunity_tag_missing_opportunity', COUNT(*)
FROM ry_ics.biz_business_opportunity_tag ot
LEFT JOIN ry_ics.biz_business_opportunity o ON o.opportunity_id = ot.opportunity_id
WHERE o.opportunity_id IS NULL
UNION ALL
SELECT 'opportunity_tag_missing_tag', COUNT(*)
FROM ry_ics.biz_business_opportunity_tag ot
LEFT JOIN ry_ics.biz_tag t ON t.tag_id = ot.tag_id
WHERE t.tag_id IS NULL;
```

重复数据检查：

```sql
SELECT opportunity_no, COUNT(*) AS count_num
FROM ry_ics.biz_business_opportunity
WHERE opportunity_no IS NOT NULL AND opportunity_no <> ''
GROUP BY opportunity_no
HAVING COUNT(*) > 1;

SELECT opportunity_id, tag_id, COUNT(*) AS count_num
FROM ry_ics.biz_business_opportunity_tag
GROUP BY opportunity_id, tag_id
HAVING COUNT(*) > 1;
```

### 6.3 代码准备

- [ ] 明确 BladeX 业务包路径。
- [ ] 明确商机表是否加入 `tenant_id`。
- [ ] 明确 `del_flag` 是否第一阶段保留。
- [ ] 明确上传文件走 BladeX resource 还是先兼容旧 URL。
- [ ] 明确跟进人、创建人从 RuoYi `loginName` 到 BladeX `account/userId` 的映射。
- [ ] 明确菜单组件路径从旧 `business/DeclareService` 改为真实商机页面。

## 7. 迁移任务清单

### 7.1 数据脚本

- [ ] 创建 `biz_business_opportunity` 表。
- [ ] 创建 `biz_business_opportunity_follow` 表。
- [ ] 创建 `biz_business_opportunity_file` 表。
- [ ] 创建 `biz_business_opportunity_tag` 表。
- [ ] 导入商机主表数据。
- [ ] 导入跟进记录。
- [ ] 导入附件记录。
- [ ] 导入商机标签关系。
- [ ] 修正 `approval_project_id` 与 `biz_approval_project` 的关联。
- [ ] 初始化菜单和按钮权限。

### 7.2 后端迁移

- [ ] 创建商机 Entity/VO/DTO/Wrapper。
- [ ] 迁移商机 Mapper 和 XML 自定义查询。
- [ ] 迁移商机 Service：新增、编辑、删除、详情、列表、统计。
- [ ] 迁移编号生成逻辑，保证并发下不重复。
- [ ] 迁移客户关联逻辑：`customerId` 优先、`creditCode` 次之。
- [ ] 迁移标签保存和同步客户标签逻辑。
- [ ] 迁移跟进记录逻辑。
- [ ] 迁移附件上传逻辑。
- [ ] 迁移背景调查返回结构。
- [ ] 迁移提交审核逻辑。
- [ ] 补充事务边界：新增/编辑/标签/审核提交必须事务包裹。
- [ ] 补充接口权限和操作日志。

### 7.3 前端迁移

- [ ] 创建商机列表 API。
- [ ] 创建商机统计 API。
- [ ] 创建商机详情 API。
- [ ] 创建商机保存/修改/删除 API。
- [ ] 创建跟进 API。
- [ ] 创建附件 API。
- [ ] 创建标签 API。
- [ ] 创建背景调查 API。
- [ ] 创建提交审核 API。
- [ ] 迁移商机列表页。
- [ ] 迁移商机编辑页。
- [ ] 迁移客户标签选择器。
- [ ] 处理 BladeX 分页返回结构。
- [ ] 处理 BladeX 上传组件与认证头。
- [ ] 处理按钮权限控制。

### 7.4 菜单权限迁移

- [ ] 在“入驻管理”下创建“商机管理”二级菜单。
- [ ] 菜单组件指向真实商机列表页。
- [ ] 新增隐藏编辑路由或页面内编辑方案。
- [ ] 新增按钮权限：查询、新增、编辑、删除、查看、跟进、附件上传、提交审核。
- [ ] 绑定管理员角色。
- [ ] 普通角色按需要授权。

## 8. 迁移后校验清单

### 8.1 数据库校验

```sql
SELECT 'biz_business_opportunity' AS table_name, COUNT(*) AS count_num FROM bladex_boot.biz_business_opportunity
UNION ALL SELECT 'biz_business_opportunity_follow', COUNT(*) FROM bladex_boot.biz_business_opportunity_follow
UNION ALL SELECT 'biz_business_opportunity_file', COUNT(*) FROM bladex_boot.biz_business_opportunity_file
UNION ALL SELECT 'biz_business_opportunity_tag', COUNT(*) FROM bladex_boot.biz_business_opportunity_tag;
```

关联校验：

```sql
SELECT 'opportunity_missing_customer' AS check_item, COUNT(*) AS invalid_count
FROM bladex_boot.biz_business_opportunity o
LEFT JOIN bladex_boot.biz_customer c ON c.customer_id = o.customer_id
WHERE o.customer_id IS NOT NULL AND c.customer_id IS NULL
UNION ALL
SELECT 'follow_missing_opportunity', COUNT(*)
FROM bladex_boot.biz_business_opportunity_follow f
LEFT JOIN bladex_boot.biz_business_opportunity o ON o.opportunity_id = f.opportunity_id
WHERE o.opportunity_id IS NULL
UNION ALL
SELECT 'file_missing_opportunity', COUNT(*)
FROM bladex_boot.biz_business_opportunity_file f
LEFT JOIN bladex_boot.biz_business_opportunity o ON o.opportunity_id = f.opportunity_id
WHERE o.opportunity_id IS NULL
UNION ALL
SELECT 'opportunity_tag_missing_tag', COUNT(*)
FROM bladex_boot.biz_business_opportunity_tag ot
LEFT JOIN bladex_boot.biz_tag t ON t.tag_id = ot.tag_id
WHERE t.tag_id IS NULL;
```

必须满足：

- [ ] 主表、跟进、附件、标签关系数量与源库一致，排除已确认脏数据。
- [ ] 关联校验全部为 0。
- [ ] `opportunity_no` 无重复。
- [ ] `biz_business_opportunity_tag` 无重复关系。

### 8.2 后端接口校验

- [ ] `GET /business/opportunity/page` 或兼容列表接口可分页查询。
- [ ] `GET /business/opportunity/statistics` 返回统计数据。
- [ ] `GET /business/opportunity/detail` 或兼容详情接口返回跟进、附件、标签。
- [ ] `POST /business/opportunity/save` 可新增商机并生成编号。
- [ ] `POST /business/opportunity/update` 可编辑未提交审核商机。
- [ ] 已提交审核商机编辑会被拦截。
- [ ] `POST /business/opportunity/remove` 为逻辑删除。
- [ ] 跟进新增后主表状态、最后跟进时间、下次跟进时间更新。
- [ ] 附件上传后附件列表可查到。
- [ ] 设置商机标签后关系表更新。
- [ ] 有客户的商机设置标签后客户标签同步。
- [ ] 背景调查接口返回固定结构。
- [ ] 提交审核能创建审批项目和审批日志。

### 8.3 前端页面校验

- [ ] “入驻管理 > 商机管理”菜单可见且可进入。
- [ ] 菜单进入的是商机列表，不是旧 `DeclareService` 空页面。
- [ ] 统计卡片显示正确。
- [ ] 列表分页、筛选、刷新正常。
- [ ] 新增商机表单校验正常。
- [ ] 第三方渠道时第三方渠道名称必填。
- [ ] 行业准入检测按钮可用。
- [ ] 背景调查弹窗可打开，空数据也能正常展示。
- [ ] 客户标签选择器可加载标签类型和标签。
- [ ] 编辑商机能回显载体类型、标签、跟进记录。
- [ ] 查看模式不可编辑。
- [ ] 跟进弹窗可新增记录。
- [ ] 附件可上传并在详情中显示链接。
- [ ] 提交审核按钮权限和状态控制正确。

### 8.4 关联模块校验

客户管理：

- [ ] 商机按 `customer_id` 能关联到客户。
- [ ] 商机按 `credit_code` 能匹配已有客户。
- [ ] 商机标签同步后，客户详情可看到同步后的标签。

客户标签：

- [ ] 标签停用后，商机选择器不展示。
- [ ] 商机已引用的标签删除策略与客户标签一致。
- [ ] 商机标签为空且有关联客户时，详情可兜底显示客户标签。

项目审核：

- [ ] 存在启用的 `tenant_entry` 审批流。
- [ ] 商机提交审核后，项目审核列表可看到审批项目。
- [ ] 审批项目 `business_id` 等于商机 `opportunity_id`。
- [ ] 审批项目 `business_type` 等于 `tenant_entry`。
- [ ] 审批日志有提交记录。

园区权限：

- [ ] 普通用户只能看到授权园区下的商机。
- [ ] 超级管理员可看到全部或按筛选查看。
- [ ] 商机新增时 `park_id` 来源明确。

### 8.5 端到端校验

```text
进入 入驻管理 > 商机管理
  -> 新增商机
  -> 填写企业、经营、入驻需求、联系人、跟进信息
  -> 选择客户标签
  -> 保存
  -> 列表出现新商机并显示统计变化
  -> 查看详情，确认标签、跟进、附件区域正常
  -> 新增跟进记录
  -> 上传附件
  -> 执行背景调查
  -> 提交入驻审核
  -> 项目审核菜单出现对应审批项目
  -> 审批日志出现提交记录
```

## 9. 风险与决策点

| 风险/决策点 | 影响 | 建议 |
| --- | --- | --- |
| 菜单仍指向 `business/DeclareService` | 用户进入空页或旧页 | 迁移时改到 `BusinessOpportunityList` |
| 商机编号并发重复 | 新增失败或数据冲突 | BladeX 中使用更稳的序列/锁/雪花编号策略 |
| 已审核商机被编辑 | 审批数据和商机数据不一致 | 保留“提交审核后不可编辑”规则 |
| 标签同步是覆盖式 | 可能覆盖客户原有标签 | 验收时确认产品是否接受；如不接受，改为追加式 |
| 附件旧 URL 不可访问 | 历史附件失效 | 先兼容旧 URL，再做资源迁移 |
| 审批流缺失 | 提交审核失败 | 迁移前初始化 `tenant_entry` 启用流程 |
| `del_flag` 与 BladeX `is_deleted` 不一致 | 删除/查询异常 | 第一阶段保留 `del_flag` 并显式过滤 |
| `park_id` 和租户字段混用 | 数据权限异常 | 先按园区过滤，租户模型另行设计 |

## 10. 推荐执行顺序

1. 确认 BladeX 总仓库 `/Users/jiakangli/Desktop/bladex-park` 工作区干净。
2. 为商机管理单独开 worktree：`bladex-park-migrate-opportunity`。
3. 先迁数据表和菜单权限。
4. 再迁后端商机 CRUD、跟进、标签。
5. 再迁附件上传和背景调查。
6. 再迁前端列表、编辑页、标签选择器。
7. 最后接入提交审核，并与项目审核菜单做端到端联调。

