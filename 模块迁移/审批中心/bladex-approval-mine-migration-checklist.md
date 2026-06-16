# BladeX 审批中心-我的审批迁移清单

本文用于迁移 RuoYi 中“审批中心”下的二级菜单“我的审批”。该菜单与“待办任务 / 已办任务 / 抄送我的”共用同一页面组件，但本次只迁移 `scope=mine` 的业务边界，避免把整个审批中心一次性混进来。

## 1. 迁移边界

- 一级菜单保留为“审批中心”。
- 本次只迁移二级菜单“我的审批”。
- 页面主体以“列表 + 详情 + 审批表 + 审批动作”为主。
- “流程配置”不纳入本次主迁移，只作为依赖项保留。
- “待办任务 / 已办任务 / 抄送我的”先不单独做页面，沿用同一组件时只做路由兼容，不作为当前验收范围。

## 2. 现状清单

### 2.1 菜单与路由

| 项目 | 现状 |
| --- | --- |
| 一级菜单 | `审批中心` |
| 二级菜单 | `我的审批` |
| 路由 | `/assetManage/approval/mine` |
| 组件 | `business/ApprovalProjectList` |
| 权限 | `business:approval:view` |
| 共享入口 | `待办任务 / 已办任务 / 抄送我的 / 流程配置` |

### 2.2 前端文件

- `ruoyi-ui/src/views/business/ApprovalProjectList.vue`
- `ruoyi-ui/src/views/business/ApprovalFlowConfig.vue`
- `ruoyi-ui/src/api/business/approval.js`
- `ruoyi-ui/src/views/dashboard/Analysis.vue`

### 2.3 后端文件

- `ruoyi-business/src/main/java/com/ruoyi/business/controller/ApprovalController.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/service/impl/ApprovalProjectServiceImpl.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/service/impl/ApprovalFlowServiceImpl.java`
- `ruoyi-business/src/main/resources/mapper/business/ApprovalProjectMapper.xml`
- `ruoyi-business/src/main/java/com/ruoyi/business/mapper/ApprovalProjectMapper.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/mapper/ApprovalFlowMapper.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/mapper/ApprovalNodeMapper.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/mapper/ApprovalMaterialMapper.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/mapper/ApprovalLogMapper.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/domain/ApprovalProject.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/domain/ApprovalFlow.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/domain/ApprovalNode.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/domain/ApprovalMaterial.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/domain/ApprovalLog.java`

### 2.4 数据表

| 表名 | 作用 |
| --- | --- |
| `biz_approval_project` | 审批主表 |
| `biz_approval_flow` | 流程配置表 |
| `biz_approval_node` | 流程节点表 |
| `biz_approval_material` | 审批资料表 |
| `biz_approval_log` | 审批日志表 |

### 2.5 关联业务表

- `biz_customer`
- `biz_contract`
- `biz_termination`
- `biz_business_opportunity`
- `sys_user`
- `sys_dept`

## 3. 功能模块清单

### 3.1 我的审批列表

- [ ] 菜单打开后默认定位到“我的审批”。
- [ ] 列表只查当前登录人的 `mine` 范围。
- [ ] 列表支持项目名称、企业名称、审批类型、状态筛选。
- [ ] 列表展示汇总卡片，至少包含审批中、已通过、已驳回、已归档。
- [ ] 列表展示项目名称、企业名称、来源业务ID、当前节点、状态、资料数、发起人、发起时间。
- [ ] 列表分页、排序、重置、查询保持可用。

### 3.2 审批详情

- [ ] 点击项目名称可进入详情。
- [ ] 详情展示审批进度。
- [ ] 详情展示审批主体信息。
- [ ] 详情展示审批日志。
- [ ] 详情展示审批资料列表。
- [ ] 详情展示当前状态文本与当前节点名称。

### 3.3 审批动作

- [ ] 当前审批人可执行审批操作。
- [ ] 审批弹窗可填写审批意见。
- [ ] 审批动作需校验当前节点和当前登录人。
- [ ] 审批通过后写入审批日志。
- [ ] 审批通过后推进到下一节点或终审状态。
- [ ] 驳回、转审、重新提交能力在共享组件中保留，但本菜单首期只验收“审批通过”主流程。

### 3.4 审批表

- [ ] 可以查看审批表内容。
- [ ] 可以导出审批表。
- [ ] 可以打印 / 另存为 PDF。
- [ ] 审批表内容应包含流程、节点、材料、日志信息。

### 3.5 审批资料

- [ ] 可以查看当前项目资料。
- [ ] 可以预览资料。
- [ ] 可以下载资料。
- [ ] 资料列表需与项目一一对应。

### 3.6 审批日志

- [ ] 显示提交、审批、驳回、转审、归档、重新提交等动作。
- [ ] 显示操作人、操作时间、审批意见。
- [ ] 日志顺序按时间线展示。

## 4. 数据流走向

```mermaid
flowchart LR
  A["进入菜单：我的审批"] --> B["前端注入 scope=mine"]
  B --> C["ApprovalController.projectList"]
  C --> D["设置 parkId / currentUser / admin"]
  D --> E["ApprovalProjectMapper.selectApprovalProjectList"]
  E --> F["按 applicant 或 create_by 过滤"]
  F --> G["返回 project + materialCount + logCount"]
  G --> H["前端表格 / 汇总卡片"]
  H --> I["查看详情 / 审批表 / 审批动作"]
  I --> J["project/form/{id} / log/list / material/list"]
  J --> K["审批日志、资料、流程节点回填"]
```

### 4.1 列表数据流

- 菜单进入后，前端路由带 `scope=mine`。
- 前端请求 `project/list` 时附带当前查询条件。
- 后端注入当前用户账号、园区 ID、管理员标识。
- Mapper 只返回 `applicant = 当前用户` 或 `create_by = 当前用户` 的记录。
- 结果集同时带出资料数和日志数。

### 4.2 详情数据流

- 点击记录后，请求项目详情。
- 后端先做园区可访问性校验。
- 再组合流程、节点、资料、日志，生成审批表数据。
- 前端在详情弹窗中渲染步骤条、描述区和时间线。

### 4.3 审批动作流

- 当前用户点击审批。
- 后端校验是否为当前节点审批人。
- 写入审批日志。
- 更新项目状态和当前节点。
- 若为终审，回写关联业务状态。

## 5. 关联模块

| 模块 | 关联方式 | 迁移要求 |
| --- | --- | --- |
| 客户管理 | 审批项目来源、客户信息回填、审批通过后客户状态同步 | 先保证客户ID、企业名称、信用代码可追溯 |
| 合同管理 | 合同续签审批通过后回写合同状态 | 保留合同审批结果同步逻辑 |
| 退租管理 | 退租审批通过 / 驳回回写退租状态 | 先保留审批结果接口，再决定是否前端接入 |
| 商机管理 | 入驻审批常从商机创建 | 需保留商机到审批项目的绑定关系 |
| 流程配置 | 决定当前项目走哪个审批流 | 流程必须先发布再允许发起 |
| 用户 / 部门 | 决定发起人、审批人、发起部门 | loginName、deptName 需可用 |
| 园区数据 | 决定数据隔离范围 | 非管理员只能看当前园区数据 |
| 附件存储 | 审批资料、审批表导出文件 | 路径、域名、下载链路要通 |

## 6. BladeX 迁移顺序

### 6.1 第一阶段：只读骨架

- [ ] 建立“审批中心”一级菜单。
- [ ] 建立“我的审批”二级菜单。
- [ ] 迁移列表页和路由壳。
- [ ] 迁移列表查询、分页、汇总卡片。
- [ ] 迁移详情弹窗和审批表查看。

### 6.2 第二阶段：审批动作

- [ ] 迁移审批通过接口。
- [ ] 迁移审批校验逻辑。
- [ ] 迁移审批日志写入。
- [ ] 迁移节点流转和状态推进。

### 6.3 第三阶段：资料与导出

- [ ] 迁移资料列表。
- [ ] 迁移资料上传 / 预览 / 下载。
- [ ] 迁移审批表导出。
- [ ] 迁移打印 / PDF 链路。

### 6.4 第四阶段：关联业务回写

- [ ] 审批通过后回写客户。
- [ ] 审批通过后回写合同。
- [ ] 审批通过 / 驳回后回写退租。
- [ ] 验证流程发布和节点配置。

## 7. 并行 Work Tree 切片

- WT-A：菜单、路由、列表、筛选、汇总卡片。
- WT-B：详情、审批表、审批日志、资料展示。
- WT-C：审批动作、节点校验、状态流转。
- WT-D：导出、打印、附件、业务回写。

每个 Work Tree 单独完成后，都要过自己的校验清单，再合并到主迁移分支。

## 8. 校验清单

### 8.1 菜单校验

- [ ] 一级菜单“审批中心”可见。
- [ ] 二级菜单“我的审批”可点。
- [ ] 菜单路径正确。
- [ ] 权限分配正确。
- [ ] 角色授权后可正常进入。

### 8.2 列表校验

- [ ] 只返回当前用户的 `mine` 数据。
- [ ] 非管理员只看当前园区。
- [ ] 分页总数正确。
- [ ] 筛选条件生效。
- [ ] 汇总卡片数字与列表一致。

### 8.3 权限校验

- [ ] 未授权用户无法进入。
- [ ] 非当前审批人无法点击审批。
- [ ] 转审人、审批人、发起人权限边界明确。
- [ ] 管理员逻辑与普通用户逻辑区分清楚。

### 8.4 数据完整性校验

- [ ] `biz_approval_project.flow_id` 必须能在 `biz_approval_flow` 找到。
- [ ] `biz_approval_node.flow_id` 必须能在 `biz_approval_flow` 找到。
- [ ] `biz_approval_material.project_id` 必须能在 `biz_approval_project` 找到。
- [ ] `biz_approval_log.project_id` 必须能在 `biz_approval_project` 找到。
- [ ] 项目状态只允许 `0 / 1 / 2 / 3 / 4 / 5 / 9`。
- [ ] 审批表导出后 `approval_form_url` 有值。

### 8.5 业务联动校验

- [ ] 审批通过后客户状态同步正确。
- [ ] 审批通过后合同状态同步正确。
- [ ] 退租审批回写正确。
- [ ] 审批日志能追溯到操作人和时间。

## 9. 建议核对 SQL

```sql
-- 1. 审批主表与流程表是否存在断链
SELECT COUNT(*) AS invalid_count
FROM biz_approval_project p
LEFT JOIN biz_approval_flow f ON f.flow_id = p.flow_id
WHERE p.process_status <> '9'
  AND p.flow_id IS NOT NULL
  AND f.flow_id IS NULL;

-- 2. 审批资料是否挂在有效项目上
SELECT COUNT(*) AS invalid_count
FROM biz_approval_material m
LEFT JOIN biz_approval_project p ON p.project_id = m.project_id
WHERE p.project_id IS NULL;

-- 3. 审批日志是否挂在有效项目上
SELECT COUNT(*) AS invalid_count
FROM biz_approval_log l
LEFT JOIN biz_approval_project p ON p.project_id = l.project_id
WHERE p.project_id IS NULL;

-- 4. 当前用户的我的审批是否能命中
SELECT COUNT(*) AS mine_count
FROM biz_approval_project p
WHERE p.process_status <> '9'
  AND (p.applicant = '当前登录名' OR p.create_by = '当前登录名');
```

## 10. 交付标准

- [ ] 能从“审批中心 -> 我的审批”进入页面。
- [ ] 能看到自己的审批列表。
- [ ] 能查看审批表和审批日志。
- [ ] 当前审批人能完成审批动作。
- [ ] 审批结果能正确流转并落库。
- [ ] 关联模块没有断链。
- [ ] 校验清单全部通过后，才进入下一菜单迁移。

## 11. 风险点

- “我的审批”与“待办 / 已办 / 抄送”共用组件，迁移时不要提前拆成四套重复页面。
- 历史审批数据依赖 `loginName`，账号改名会影响可见性。
- `current_node` 可能在转审后被写成转审人账号，前端和后端都要按这个规则校验。
- 审批表导出依赖文件存储路径，环境不通会直接影响打印能力。

