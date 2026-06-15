# BladeX 入驻、商机、客户与标签迁移清单

本文档用于把 RuoYi 项目中的入驻管理、商机管理、商机审核、客户管理、客户标签迁移到 BladeX。目标不是一次性搬完整个业务系统，而是先把这条招商入驻主链路拆成可并行迁移、可独立校验、可逐步集成的模块清单。

## 1. 迁移范围

### 1.1 本次选择的模块

| 模块 | 源业务含义 | 建议迁移优先级 | 是否可并行 |
| --- | --- | ---: | --- |
| 客户标签 | 标签类型、标签字典、客户标签关系 | 1 | 可单独开 worktree |
| 客户管理 | 客户档案、附件、导入、核验、风险排查 | 2 | 依赖客户标签底座 |
| 入驻管理 | 入驻需求、需求跟进、状态流转 | 3 | 依赖客户、园区 |
| 商机管理 | 商机档案、跟进、附件、标签、背调、提交审核 | 4 | 依赖客户、标签、园区 |
| 商机审核 | 商机提交入驻审批、审批流、审批日志、审批材料 | 5 | 可先并行适配审批底座，最终与商机集成 |

### 1.2 不在本轮直接迁移的内容

- 合同管理、退租审批、物业工单、费用账单等后续业务模块。
- Flowable 引擎替换。当前源项目审批为自定义轻量审批流，本轮先按业务表和服务逻辑迁移。
- 数据模型大重构。本轮优先保证业务链路可跑通，再考虑统一字段和租户模型优化。
- 前端视觉重做。本轮目标是 Saber3 可用页面和功能对齐，不做额外 UI 改版。

## 2. 当前源模块清单

### 2.1 后端 Controller

| 模块 | Controller | 源接口前缀 |
| --- | --- | --- |
| 入驻管理 | `SettlementDemandController.java` | `/business/settlement` |
| 商机管理 | `BusinessOpportunityController.java` | `/business/opportunity` |
| 客户管理 | `CustomerController.java` | `/business/customer` |
| 客户标签 | `TagController.java` | `/business/tag` |
| 标签类型 | `TagTypeController.java` | `/business/tagType` |
| 商机审核 | `ApprovalController.java` | `/business/approval` |

### 2.2 后端领域文件

| 模块 | Domain |
| --- | --- |
| 入驻管理 | `SettlementDemand.java` |
| 商机管理 | `BusinessOpportunity.java`、`BusinessOpportunityFollow.java`、`BusinessOpportunityFile.java` |
| 客户管理 | `Customer.java`、`CustomerAttachment.java`、`CustomerLog.java`、`CustomerImportRow.java`、`CustomerImportResult.java` |
| 客户标签 | `Tag.java`、`TagType.java` |
| 商机审核 | `ApprovalFlow.java`、`ApprovalNode.java`、`ApprovalProject.java`、`ApprovalMaterial.java`、`ApprovalLog.java` |

### 2.3 后端 Service 与 Mapper

| 模块 | Service | Mapper/XML |
| --- | --- | --- |
| 入驻管理 | `ISettlementDemandService`、`SettlementDemandServiceImpl` | `SettlementDemandMapper.java/xml` |
| 商机管理 | `IBusinessOpportunityService`、`BusinessOpportunityServiceImpl` | `BusinessOpportunityMapper.java/xml` |
| 客户管理 | `ICustomerService`、`CustomerServiceImpl` | `CustomerMapper.java/xml`、`CustomerAttachmentMapper.java/xml`、`CustomerLogMapper.java` |
| 客户标签 | `ITagService`、`ITagTypeService`、`TagServiceImpl`、`TagTypeServiceImpl` | `TagMapper.java/xml`、`TagTypeMapper.java/xml` |
| 商机审核 | `IApprovalProjectService`、`IApprovalFlowService`、`IApprovalMaterialService`、`IApprovalLogService` | `ApprovalProjectMapper.java/xml`、`ApprovalFlowMapper.java/xml`、`ApprovalNodeMapper.java/xml`、`ApprovalMaterialMapper.java/xml`、`ApprovalLogMapper.java/xml` |

### 2.4 前端文件

| 模块 | API | 页面/组件 |
| --- | --- | --- |
| 入驻管理 | `ruoyi-ui/src/api/business/settlement.js` | `SettlementDrawer.vue` |
| 商机管理 | `ruoyi-ui/src/api/business/opportunity.js` | `BusinessOpportunityList.vue`、`BusinessOpportunityEdit.vue` |
| 客户管理 | `ruoyi-ui/src/api/business/customer.js` | `CustomerList.vue`、`CustomerEdit.vue`、`CustomerDetailDrawer.vue`、`CustomerModal.vue` |
| 客户标签 | `customer.js` 内标签 API | `TagList.vue`、`CustomerTagSelector.vue`、`TagModal.vue` |
| 商机审核 | `ruoyi-ui/src/api/business/approval.js` | `ApprovalProjectList.vue`、`ApprovalFlowConfig.vue` |

## 3. 数据表清单

### 3.1 必迁业务表

| 表 | 模块 | 说明 |
| --- | --- | --- |
| `biz_tag_type` | 客户标签 | 标签类型 |
| `biz_tag` | 客户标签 | 标签字典，含 `park_id` |
| `biz_customer_tag` | 客户标签 | 客户标签关系 |
| `biz_customer` | 客户管理 | 客户主表 |
| `biz_customer_attachment` | 客户管理 | 客户附件 |
| `biz_customer_log` | 客户管理 | 客户操作日志 |
| `biz_industry_access_rule` | 客户管理 | 行业准入规则 |
| `biz_settlement_demand` | 入驻管理 | 入驻需求 |
| `biz_business_opportunity` | 商机管理 | 商机主表 |
| `biz_business_opportunity_follow` | 商机管理 | 商机跟进记录 |
| `biz_business_opportunity_file` | 商机管理 | 商机附件 |
| `biz_business_opportunity_tag` | 商机管理 | 商机标签关系 |
| `biz_approval_flow` | 商机审核 | 审批流程配置 |
| `biz_approval_node` | 商机审核 | 结构化审批节点 |
| `biz_approval_project` | 商机审核 | 审批项目 |
| `biz_approval_material` | 商机审核 | 审批材料 |
| `biz_approval_log` | 商机审核 | 审批日志 |

### 3.2 已迁移/前置依赖表

| 表 | 依赖原因 | 当前处理建议 |
| --- | --- | --- |
| `ics_park` | 标签、客户、入驻、商机、审批均按园区过滤 | 已先迁移，后续只做外键/数据权限引用 |
| `ics_building`、`ics_floor`、`ics_room` | 入驻意向和审批表单可能引用楼层、房间信息 | 本轮不改资产模块，只读依赖 |
| `blade_user` / BladeX 用户体系 | 审批人、跟进人、创建人 | 迁移时做账号映射，不继续依赖 RuoYi `sys_user` |
| `blade_dept` / BladeX 部门体系 | 发起部门、数据权限 | 迁移时做部门映射或先置空 |
| `blade_attach` / OSS 资源表 | 附件上传 | 建议改用 BladeX 资源能力，保留旧 URL 兼容 |

## 4. 数据流与模块关联

### 4.1 客户与标签底座

```text
标签类型 biz_tag_type
  -> 标签字典 biz_tag
      -> 客户标签 biz_customer_tag
          -> 客户详情/客户列表展示
          -> 商机标签兜底展示
          -> 商机审核通过后同步客户标签
```

关键规则：

- 同一标签类型下标签名称不可重复。
- 标签删除默认不允许删除已关联客户的标签，源项目支持 `force=true` 强制删除。
- 非管理员查询标签时，会按 `park_id` 或用户维度过滤。
- 商机标签为空但商机关联客户时，商机会兜底读取客户标签。

### 4.2 客户管理主链路

```text
客户新建/导入
  -> 保存客户基础信息 biz_customer
  -> 保存客户标签 biz_customer_tag
  -> 上传附件 biz_customer_attachment
  -> 执行本地核验/行业准入/风险排查
  -> 被入驻需求、商机、审批项目、合同等模块引用
```

关键规则：

- `credit_code` 全局唯一，源项目不按园区重复。
- 删除客户前必须检查关联业务数据，存在关联时禁止删除。
- 客户详情和列表会在核验字段为空或默认值时触发本地合规核验。
- 客户主体目前不强绑定园区，`park_id` 更多用于过滤和兼容。

### 4.3 入驻管理主链路

```text
客户 biz_customer
  -> 入驻需求 biz_settlement_demand
      -> 跟进记录 follow_record
      -> 状态流转：待跟进/跟进中/已签约/已入驻/已关闭
```

关键规则：

- 入驻需求必须关联客户。
- 入驻需求必须带园区上下文。
- 源项目跟进记录为文本追加模式，不是独立明细表。
- 入驻需求和商机有业务相似性，但当前是两套表，需要后续产品口径确认是否合并。

### 4.4 商机管理主链路

```text
商机录入
  -> 自动生成 opportunity_no
  -> 按 customer_id 或 enterprise_name/credit_code 关联已有客户
  -> 保存商机标签 biz_business_opportunity_tag
  -> 如果已关联客户，同步客户标签 biz_customer_tag
  -> 记录跟进/附件/背调信息
  -> 提交入驻审核
```

关键规则：

- 商机编号由服务端生成，迁移后应改为 BladeX 统一编号策略或保留源逻辑。
- 商机状态包括草稿、跟进、提交审核、审核通过/驳回等源业务状态。
- 商机附件上传当前依赖 RuoYi `DfsConfig` 和 `FileUploadUtils`，迁移后要改为 BladeX 资源/OSS。
- 商机背调当前是本地模拟规则，不是外部征信接口。

### 4.5 商机审核链路

```text
商机 submitAudit
  -> 查找 tenant_entry 审批流 biz_approval_flow
  -> 创建审批项目 biz_approval_project
      business_type = tenant_entry
      business_id = opportunity_id
  -> 写入提交日志 biz_approval_log
  -> 推进当前审批节点
  -> 审批通过/驳回
      -> 回写商机审核状态
      -> 通过时创建或更新客户
      -> 同步商机 customer_id
      -> 同步商机标签到客户标签
```

关键规则：

- 商机审核不是独立孤岛，它依赖商机、客户、标签、园区、审批流程配置。
- 审批项目通过 `business_type + business_id` 关联业务对象。
- `business_type='tenant_entry'` 是本轮核心审批类型。
- 审批通过后会尝试创建或更新客户，并把商机客户关系补齐。

## 5. BladeX 迁移口径

### 5.1 后端技术映射

| RuoYi 写法 | BladeX 迁移方向 |
| --- | --- |
| `BaseController`、自定义 `R` | `BladeController`、`org.springblade.core.tool.api.R` |
| `IBaseServiceImpl` | `BaseServiceImpl` / `BladeServiceImpl` |
| 手写 mapper CRUD | MyBatis-Plus `BaseMapper` + 必要 XML 自定义查询 |
| `startPage()` | `Condition.getPage(query)` + `IPage` |
| `@HasPermissions` | BladeX `@PreAuth` / 菜单按钮权限 |
| `getLoginName()` | `AuthUtil.getUser()` / `BladeUser` |
| `create_by` 字符串 | BladeX `create_user` 或保留兼容字段，需统一决策 |
| `del_flag` | BladeX `is_deleted` 或保留 `del_flag`，需逐表决策 |
| RuoYi 文件工具 | BladeX resource/OSS/attach 能力 |

### 5.2 表结构口径

第一阶段建议采用“业务字段保留 + BladeX 审计字段补齐”的稳妥策略：

- 主键继续使用源业务主键名，如 `customer_id`、`opportunity_id`、`project_id`，避免前端和关联 SQL 大面积改动。
- 新增 BladeX 标准字段时不要破坏旧字段语义，优先兼容 `create_by/update_by/del_flag/status`。
- 如果启用 BladeX 租户拦截，必须明确哪些业务表加入 `tenant_id`，并配置租户忽略表，避免 SQL 被自动追加错误租户条件。
- 园区权限暂时继续走 `park_id`，后续再统一为 BladeX 数据权限。

### 5.3 前端迁移口径

- RuoYi Vue2 页面迁移到 BladeX Saber3/Vue3。
- API 文件迁移到 Saber3 约定目录，保持接口语义稳定。
- 分页参数改为 BladeX `current/size`，返回值适配 `IPage.records/total`。
- 上传组件改为 BladeX 请求封装和资源上传能力。
- 标签选择器、客户弹窗、商机编辑表单优先迁移为可复用业务组件。

## 6. 并行 Worktree 策略

BladeX 现在是整体仓库，worktree 应从总仓库根目录打开：

```bash
cd /Users/jiakangli/Desktop/bladex-park
git status
git worktree add ../bladex-park-migrate-customer-tags -b codex/migrate-customer-tags
git worktree add ../bladex-park-migrate-customer -b codex/migrate-customer
git worktree add ../bladex-park-migrate-settlement -b codex/migrate-settlement
git worktree add ../bladex-park-migrate-opportunity -b codex/migrate-opportunity
git worktree add ../bladex-park-migrate-opportunity-audit -b codex/migrate-opportunity-audit
```

建议分工：

| Worktree | 迁移内容 | 主要依赖 | 集成顺序 |
| --- | --- | --- | ---: |
| `bladex-park-migrate-customer-tags` | 标签类型、标签字典、客户标签关系、标签前端组件 | 园区 | 1 |
| `bladex-park-migrate-customer` | 客户 CRUD、附件、导入、核验、行业准入 | 客户标签 | 2 |
| `bladex-park-migrate-settlement` | 入驻需求 CRUD、状态、跟进 | 客户、园区 | 3 |
| `bladex-park-migrate-opportunity` | 商机 CRUD、跟进、附件、标签、背调 | 客户、标签、园区 | 4 |
| `bladex-park-migrate-opportunity-audit` | tenant_entry 审批流、审批项目、材料、日志、回写业务 | 商机、客户、标签、用户 | 5 |

并行原则：

- 各 worktree 可以并行开发，但数据库脚本命名必须带模块前缀和执行顺序。
- 公共字段、枚举、接口路径、权限编码必须先约定，避免后续 merge 冲突。
- 每个模块完成后先独立验收，再合入集成分支。
- 商机审核可以先做审批底座，但最终必须等商机和客户模块合并后做端到端联调。

## 7. 模块迁移任务清单

### 7.1 客户标签模块

准备清单：

- [ ] 确认 `biz_tag_type`、`biz_tag`、`biz_customer_tag` 在 `bladex_boot` 是否已存在。
- [ ] 确认标签是否保留 `park_id` 过滤。
- [ ] 确认标签类型初始化数据：行业、规模、客户等级、客户来源。
- [ ] 确认删除策略：有关联客户时禁止删除，还是保留 `force` 强制删除。

后端任务：

- [ ] 创建/适配 Entity：`TagType`、`Tag`。
- [ ] 创建 Mapper 和 XML：标签列表、按类型查询、客户标签查询、重复名校验。
- [ ] 创建 Service：标签校验、类型校验、删除前关联检查。
- [ ] 创建 Controller：`/blade-park/tag`、`/blade-park/tag-type` 或按项目现有业务前缀统一。
- [ ] 增加权限编码：查询、新增、修改、删除、强制删除。

前端任务：

- [ ] 迁移 `TagList.vue` 到 Saber3。
- [ ] 迁移 `TagModal.vue`。
- [ ] 迁移 `CustomerTagSelector.vue`，供客户和商机共用。
- [ ] 迁移标签 API。

数据任务：

- [ ] 导入标签类型。
- [ ] 导入标签字典。
- [ ] 导入客户标签关系前，先保证客户主表已迁移；如先迁标签模块，只迁字典和类型。

校验清单：

- [ ] 标签类型列表可查询、可新增、可修改、可删除。
- [ ] 有标签引用的标签类型不能删除。
- [ ] 同类型标签重名会被拦截。
- [ ] 已关联客户的标签默认不能删除。
- [ ] 标签按类型查询能被客户选择器和商机选择器复用。

### 7.2 客户管理模块

准备清单：

- [ ] 确认 `biz_customer` 字段是否需要补 `tenant_id/is_deleted/create_user/update_user/create_dept`。
- [ ] 确认 `credit_code` 唯一口径：全局唯一或园区内唯一。
- [ ] 确认客户附件迁移策略：旧 URL 兼容或统一迁入 BladeX 资源表。
- [ ] 确认客户导入是否继续保留内存 token 缓存，还是改 Redis 临时缓存。
- [ ] 确认行业准入规则是否按园区隔离。

后端任务：

- [ ] 创建/适配 Entity：`Customer`、`CustomerAttachment`、`CustomerLog`、`IndustryAccessRule`。
- [ ] 创建 VO/DTO：客户详情、客户列表、客户导入预览、客户核验结果。
- [ ] 创建 Mapper/XML：列表条件、详情、统计、关联业务数量检查、核验结果更新。
- [ ] 创建 Service：新增、修改、删除保护、状态变更、标签设置、合规核验、导入确认。
- [ ] 创建附件服务：上传、列表、删除。
- [ ] 创建 Controller：客户 CRUD、统计、标签设置、附件、导入、核验。
- [ ] 将 RuoYi 异常改为 BladeX 业务异常体系。

前端任务：

- [ ] 迁移 `CustomerList.vue`。
- [ ] 迁移 `CustomerEdit.vue`。
- [ ] 迁移 `CustomerModal.vue`。
- [ ] 迁移 `CustomerDetailDrawer.vue`。
- [ ] 接入 `CustomerTagSelector.vue`。
- [ ] 迁移附件上传和导入弹窗。

数据任务：

- [ ] 导入 `biz_customer`。
- [ ] 导入 `biz_customer_attachment`。
- [ ] 导入 `biz_customer_log`。
- [ ] 导入 `biz_industry_access_rule`。
- [ ] 导入或重建 `biz_customer_tag`。

校验清单：

- [ ] 客户列表分页、筛选、标签展示正常。
- [ ] 客户新增后可保存标签。
- [ ] 客户编辑后标签不会丢失。
- [ ] 重复 `credit_code` 会被拦截。
- [ ] 有商机、入驻需求、审批项目、合同等关联数据的客户不能删除。
- [ ] 客户核验能更新 `verify_status`、`industry_access_result`、`risk_level`。
- [ ] 附件能上传、展示、删除。
- [ ] 导入模板下载、导入预校验、确认导入链路可用。

### 7.3 入驻管理模块

准备清单：

- [ ] 确认入驻需求是否继续作为独立模块保留。
- [ ] 确认入驻需求与商机的边界：线索阶段用商机，明确需求阶段用入驻需求，或二者并存。
- [ ] 确认入驻需求 `customer_id` 在目标库中均能找到客户。
- [ ] 确认 `park_id` 均能找到园区。

后端任务：

- [ ] 创建/适配 Entity：`SettlementDemand`。
- [ ] 创建 Mapper/XML：详情、列表、状态修改、追加跟进记录。
- [ ] 创建 Service：新增默认状态、默认来源、状态流转、跟进记录。
- [ ] 创建 Controller：详情、列表、新增、修改、删除、状态变更、跟进。
- [ ] 补权限编码：查询、新增、修改、删除、状态变更、跟进。

前端任务：

- [ ] 迁移 `SettlementDrawer.vue`。
- [ ] 在客户详情或业务入口接入入驻需求。
- [ ] 迁移入驻需求 API。

数据任务：

- [ ] 导入 `biz_settlement_demand`。
- [ ] 校验 `customer_id` 关联客户。
- [ ] 校验 `park_id` 关联园区。

校验清单：

- [ ] 入驻需求列表能按客户、状态、跟进人、园区筛选。
- [ ] 新增入驻需求时必须绑定客户。
- [ ] 修改需求不会丢失跟进记录。
- [ ] 状态变更符合源枚举。
- [ ] 跟进记录追加后可在详情展示。

### 7.4 商机管理模块

准备清单：

- [ ] 确认商机状态枚举和页面展示文案。
- [ ] 确认商机编号生成规则。
- [ ] 确认商机与客户匹配规则：`customer_id` 优先，其次统一社会信用代码或企业名称。
- [ ] 确认商机附件存储策略。
- [ ] 确认背调接口本轮是否仍用本地模拟规则。

后端任务：

- [ ] 创建/适配 Entity：`BusinessOpportunity`、`BusinessOpportunityFollow`、`BusinessOpportunityFile`。
- [ ] 创建 Mapper/XML：商机列表、详情、统计、跟进、附件、标签。
- [ ] 创建 Service：编号生成、状态规范化、客户关联、标签同步、跟进、附件、背调。
- [ ] 创建 Controller：CRUD、统计、跟进、附件、标签、背调、提交审核。
- [ ] 与客户标签模块对接 `biz_business_opportunity_tag`。
- [ ] 与商机审核模块约定 `submitAudit` 入参和返回。

前端任务：

- [ ] 迁移 `BusinessOpportunityList.vue`。
- [ ] 迁移 `BusinessOpportunityEdit.vue`。
- [ ] 接入客户选择器、标签选择器、附件上传。
- [ ] 接入提交审核按钮和审批流选择。
- [ ] 接入背调展示。

数据任务：

- [ ] 导入 `biz_business_opportunity`。
- [ ] 导入 `biz_business_opportunity_follow`。
- [ ] 导入 `biz_business_opportunity_file`。
- [ ] 导入 `biz_business_opportunity_tag`。
- [ ] 校验商机关联客户、园区、审批项目。

校验清单：

- [ ] 新增商机会自动生成唯一编号。
- [ ] 有 `customer_id` 的商机能展示客户信息。
- [ ] 无 `customer_id` 但企业名称/信用代码匹配时能关联已有客户。
- [ ] 商机设置标签后，已关联客户的客户标签同步更新。
- [ ] 商机跟进会更新最后跟进时间和下次跟进时间。
- [ ] 商机附件能上传和展示。
- [ ] 背调结果能按商机和企业名称查询。
- [ ] 提交审核后生成审批项目，并回写 `approval_project_id/submitted_audit_flag`。

### 7.5 商机审核模块

准备清单：

- [ ] 确认本轮只迁 `tenant_entry` 入驻审核，还是迁整个审批中心。
- [ ] 确认审批人账号从 RuoYi 登录名映射到 BladeX 用户账号。
- [ ] 确认审批流配置是否使用结构化节点 `biz_approval_node`。
- [ ] 确认审批材料文件存储。
- [ ] 确认审批通过后客户创建/更新策略。

后端任务：

- [ ] 创建/适配 Entity：`ApprovalFlow`、`ApprovalNode`、`ApprovalProject`、`ApprovalMaterial`、`ApprovalLog`。
- [ ] 创建 Mapper/XML：流程列表、节点列表、项目列表、材料、日志、状态更新。
- [ ] 创建 Service：流程发布、节点保存、项目提交、撤回、通过、驳回、转审、重提、归档。
- [ ] 创建商机审核集成服务：通过 `business_type='tenant_entry'` 和 `business_id=opportunity_id` 关联商机。
- [ ] 审批通过后回写商机、客户、客户标签。
- [ ] 创建 Controller：流程、项目、材料、日志、打印/导出审批表。

前端任务：

- [ ] 迁移 `ApprovalProjectList.vue`。
- [ ] 迁移 `ApprovalFlowConfig.vue`。
- [ ] 接入审批项目详情、审批动作、材料上传、审批日志。
- [ ] 在商机页面接入提交审核入口。

数据任务：

- [ ] 导入 `biz_approval_flow`。
- [ ] 导入 `biz_approval_node`。
- [ ] 导入 `biz_approval_project`。
- [ ] 导入 `biz_approval_material`。
- [ ] 导入 `biz_approval_log`。
- [ ] 初始化或校验 `tenant_entry` 已发布审批流。

校验清单：

- [ ] 审批流程可以新建、编辑、发布、复制。
- [ ] 审批节点可以保存并按顺序读取。
- [ ] 商机提交审核能创建 `tenant_entry` 审批项目。
- [ ] 审批项目能提交、撤回、通过、驳回、转审、重提、归档。
- [ ] 当前审批人校验有效，非当前审批人不能审批。
- [ ] 审批通过会回写商机状态。
- [ ] 审批通过会创建或更新客户。
- [ ] 审批通过会同步商机标签到客户标签。
- [ ] 审批驳回会回写商机驳回状态。
- [ ] 审批材料能上传、列表展示。
- [ ] 审批日志完整记录动作、节点、操作人、意见。

## 8. 数据迁移执行顺序

建议脚本顺序：

```text
01_customer_tag_type.sql
02_customer_tag.sql
03_customer.sql
04_customer_attachment.sql
05_customer_log.sql
06_industry_access_rule.sql
07_customer_tag_relation.sql
08_settlement_demand.sql
09_business_opportunity.sql
10_business_opportunity_follow.sql
11_business_opportunity_file.sql
12_business_opportunity_tag.sql
13_approval_flow.sql
14_approval_node.sql
15_approval_project.sql
16_approval_material.sql
17_approval_log.sql
18_menu_permission.sql
19_role_menu_binding.sql
```

执行规则：

- 每个脚本必须可重复执行或在执行前有清晰备份。
- 关联表必须在主表导入后再导入。
- 审批项目导入前必须保证客户、商机、审批流已存在。
- 文件类表导入后先保留旧 URL，等资源模块统一后再做二次迁移。

## 9. 迁移前数据校验 SQL

### 9.1 表记录统计

```sql
SELECT 'biz_tag_type' table_name, COUNT(*) count_num FROM ry_ics.biz_tag_type
UNION ALL SELECT 'biz_tag', COUNT(*) FROM ry_ics.biz_tag
UNION ALL SELECT 'biz_customer', COUNT(*) FROM ry_ics.biz_customer
UNION ALL SELECT 'biz_customer_tag', COUNT(*) FROM ry_ics.biz_customer_tag
UNION ALL SELECT 'biz_settlement_demand', COUNT(*) FROM ry_ics.biz_settlement_demand
UNION ALL SELECT 'biz_business_opportunity', COUNT(*) FROM ry_ics.biz_business_opportunity
UNION ALL SELECT 'biz_business_opportunity_tag', COUNT(*) FROM ry_ics.biz_business_opportunity_tag
UNION ALL SELECT 'biz_approval_flow', COUNT(*) FROM ry_ics.biz_approval_flow
UNION ALL SELECT 'biz_approval_node', COUNT(*) FROM ry_ics.biz_approval_node
UNION ALL SELECT 'biz_approval_project', COUNT(*) FROM ry_ics.biz_approval_project
UNION ALL SELECT 'biz_approval_material', COUNT(*) FROM ry_ics.biz_approval_material
UNION ALL SELECT 'biz_approval_log', COUNT(*) FROM ry_ics.biz_approval_log;
```

### 9.2 关联完整性校验

```sql
SELECT 'customer_tag_missing_customer' check_item, COUNT(*) invalid_count
FROM ry_ics.biz_customer_tag ct
LEFT JOIN ry_ics.biz_customer c ON c.customer_id = ct.customer_id
WHERE c.customer_id IS NULL
UNION ALL
SELECT 'customer_tag_missing_tag', COUNT(*)
FROM ry_ics.biz_customer_tag ct
LEFT JOIN ry_ics.biz_tag t ON t.tag_id = ct.tag_id
WHERE t.tag_id IS NULL
UNION ALL
SELECT 'settlement_missing_customer', COUNT(*)
FROM ry_ics.biz_settlement_demand d
LEFT JOIN ry_ics.biz_customer c ON c.customer_id = d.customer_id
WHERE c.customer_id IS NULL
UNION ALL
SELECT 'opportunity_missing_customer', COUNT(*)
FROM ry_ics.biz_business_opportunity o
LEFT JOIN ry_ics.biz_customer c ON c.customer_id = o.customer_id
WHERE o.customer_id IS NOT NULL AND c.customer_id IS NULL
UNION ALL
SELECT 'opportunity_tag_missing_opportunity', COUNT(*)
FROM ry_ics.biz_business_opportunity_tag ot
LEFT JOIN ry_ics.biz_business_opportunity o ON o.opportunity_id = ot.opportunity_id
WHERE o.opportunity_id IS NULL
UNION ALL
SELECT 'opportunity_tag_missing_tag', COUNT(*)
FROM ry_ics.biz_business_opportunity_tag ot
LEFT JOIN ry_ics.biz_tag t ON t.tag_id = ot.tag_id
WHERE t.tag_id IS NULL
UNION ALL
SELECT 'approval_missing_opportunity', COUNT(*)
FROM ry_ics.biz_approval_project p
LEFT JOIN ry_ics.biz_business_opportunity o ON o.opportunity_id = p.business_id
WHERE p.business_type = 'tenant_entry'
  AND p.business_id IS NOT NULL
  AND o.opportunity_id IS NULL
UNION ALL
SELECT 'approval_missing_customer', COUNT(*)
FROM ry_ics.biz_approval_project p
LEFT JOIN ry_ics.biz_customer c ON c.customer_id = p.customer_id
WHERE p.customer_id IS NOT NULL
  AND c.customer_id IS NULL
UNION ALL
SELECT 'approval_missing_flow', COUNT(*)
FROM ry_ics.biz_approval_project p
LEFT JOIN ry_ics.biz_approval_flow f ON f.flow_id = p.flow_id
WHERE p.flow_id IS NOT NULL
  AND f.flow_id IS NULL;
```

### 9.3 重复数据校验

```sql
SELECT credit_code, COUNT(*) count_num
FROM ry_ics.biz_customer
WHERE credit_code IS NOT NULL AND credit_code <> ''
GROUP BY credit_code
HAVING COUNT(*) > 1;

SELECT tag_type, tag_name, COUNT(*) count_num
FROM ry_ics.biz_tag
WHERE del_flag = '0'
GROUP BY tag_type, tag_name
HAVING COUNT(*) > 1;

SELECT customer_id, tag_id, COUNT(*) count_num
FROM ry_ics.biz_customer_tag
GROUP BY customer_id, tag_id
HAVING COUNT(*) > 1;

SELECT opportunity_id, tag_id, COUNT(*) count_num
FROM ry_ics.biz_business_opportunity_tag
GROUP BY opportunity_id, tag_id
HAVING COUNT(*) > 1;
```

## 10. 迁移后验收总清单

### 10.1 后端验收

- [ ] BladeX 后端启动无报错。
- [ ] Swagger/Knife4j 能看到新增模块接口。
- [ ] 所有接口返回 BladeX 标准 `R`。
- [ ] 分页接口返回 `IPage` 或前端已统一适配。
- [ ] 新增、修改、删除、详情、列表接口均通过。
- [ ] 上传接口走 BladeX 资源能力或旧 URL 兼容策略明确。
- [ ] 权限注解和菜单按钮权限匹配。
- [ ] 园区数据过滤有效。
- [ ] 租户拦截不会影响业务表查询。

### 10.2 前端验收

- [ ] 菜单能进入客户、标签、入驻、商机、审批页面。
- [ ] 页面刷新无空白、无控制台明显错误。
- [ ] 列表分页、筛选、重置可用。
- [ ] 表单新增、编辑、校验、回显可用。
- [ ] 标签选择器可在客户和商机复用。
- [ ] 上传、导入、审批动作按钮可用。
- [ ] 无 RuoYi 旧路由、旧 token、旧响应字段强依赖。

### 10.3 数据验收

- [ ] 源库和目标库核心表数量一致，允许明确排除的脏数据除外。
- [ ] 关联完整性 SQL 全部为 0。
- [ ] 标签、客户、商机、审批项目之间的关联能在页面展示。
- [ ] 审批通过后的客户、商机、标签回写正确。
- [ ] 删除保护逻辑有效，不会误删有关联业务的客户和标签。

### 10.4 端到端验收

```text
新建标签类型/标签
  -> 新建客户并选择标签
  -> 新建入驻需求并关联客户
  -> 新建商机并选择同一客户
  -> 商机设置标签并同步客户标签
  -> 商机提交入驻审核
  -> 审批人审批通过
  -> 商机状态更新
  -> 客户入驻/审核相关字段更新
  -> 审批日志和材料可追溯
```

## 11. 风险点与处理建议

| 风险点 | 影响 | 建议 |
| --- | --- | --- |
| `del_flag` 与 BladeX `is_deleted` 不一致 | 逻辑删除失效或数据误查 | 第一阶段保留 `del_flag`，服务层显式过滤；第二阶段再统一 |
| 审计字段 `create_by` 与 BladeX `create_user` 不一致 | 创建人显示和数据权限异常 | 做账号映射，必要时双字段兼容 |
| 文件 URL 迁移不完整 | 附件无法打开 | 先保留旧 URL，新增文件走 BladeX 资源 |
| 审批人账号不一致 | 审批按钮不可用 | 提前建立 RuoYi loginName 到 BladeX account 映射 |
| 商机审核依赖客户和标签 | 单模块验收通过但端到端失败 | 审批模块最终必须与商机、客户、标签联调 |
| 租户拦截误加 `tenant_id` | SQL 报错或查不到数据 | 明确业务表租户策略，必要时配置忽略表 |
| 客户导入用内存缓存 | 多实例或重启后 token 丢失 | BladeX 中建议改 Redis 临时缓存 |

## 12. 下一步执行建议

1. 先在 BladeX 总仓库 `/Users/jiakangli/Desktop/bladex-park` 确认当前基线已提交、工作区干净。
2. 按第 6 节从总仓库根目录创建 5 个 worktree。
3. 第一批只启动两个 worktree：客户标签、客户管理。
4. 客户标签完成并验收后，再合入客户管理。
5. 客户管理完成后，再并行推进入驻管理和商机管理。
6. 商机审核最后做端到端联调，不要孤立合并。

