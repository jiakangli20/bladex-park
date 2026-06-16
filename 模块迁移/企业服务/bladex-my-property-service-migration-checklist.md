# BladeX 企业服务-我的物业服务迁移清单

本文用于迁移 RuoYi 中“企业服务”下的物业服务能力到 BladeX。目标菜单按“企业服务 -> 我的物业服务”收口，源端实际由“物业服务配置”和“工单处理”共同支撑。

## 1. 迁移边界

- 一级菜单：`企业服务`。
- 本次二级菜单：`我的物业服务`。
- 主流程：服务项查看、工单创建、工单处理、详情跟踪、评价与日志。
- 前置依赖：物业服务项配置必须先可用，否则无法发起工单。
- 不纳入本次：商户管理、政策服务、广告管理、在园企业数据。

## 2. 现状清单

### 2.1 菜单与路由

| 层级 | 菜单 | 现状组件 | 现状路径 | 权限 |
| --- | --- | --- | --- | --- |
| 一级 | 企业服务 | `PageView` | `/business/service` | 无 |
| 二级 | 物业服务 | `PageView` | `/business/service/property` | 无 |
| 三级 | 物业服务配置 | `business/PropertyServiceList` | `/business/service/property/config` | `business:propertyService:list` |
| 三级 | 工单处理 | `business/WorkorderList` | `/business/service/property/workorder` | `business:workorder:list` |

BladeX 目标建议先合并为一个二级菜单：

```text
企业服务
  -> 我的物业服务
```

页面内部用页签承载：

- 服务目录
- 我的工单
- 工单处理
- 工单详情

### 2.2 前端文件

- `ruoyi-ui/src/views/business/PropertyServiceList.vue`
- `ruoyi-ui/src/views/business/WorkorderList.vue`
- `ruoyi-ui/src/views/business/PropertyServiceEmpty.vue`
- `ruoyi-ui/src/api/business/propertyService.js`
- `ruoyi-ui/src/api/business/customer.js`
- `ruoyi-ui/src/api/business/contract.js`
- `ruoyi-ui/src/api/business/park.js`
- `ruoyi-ui/src/api/business/building.js`
- `ruoyi-ui/src/api/business/floor.js`
- `ruoyi-ui/src/api/business/room.js`

### 2.3 后端文件

- `ruoyi-business/src/main/java/com/ruoyi/business/controller/PropertyServiceController.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/service/PropertyServiceService.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/service/impl/PropertyServiceServiceImpl.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/mapper/PropertyServiceMapper.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/mapper/ServiceWorkorderMapper.java`
- `ruoyi-business/src/main/resources/mapper/business/PropertyServiceMapper.xml`
- `ruoyi-business/src/main/resources/mapper/business/ServiceWorkorderMapper.xml`
- `ruoyi-business/src/main/java/com/ruoyi/business/domain/PropertyService.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/domain/ServiceWorkorder.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/domain/WorkorderLog.java`

### 2.4 SQL 文件

- `sql/property_service.sql`
- `sql/enterprise_service_backend_incremental.sql`
- `sql/update_enterprise_service_submenus.sql`
- `sql/fix_enterprise_service_menu_cleanup.sql`
- `sql/menu_layout_consolidated.sql`

### 2.5 数据表

| 表名 | 作用 |
| --- | --- |
| `biz_property_service` | 物业服务项配置 |
| `biz_service_workorder` | 物业服务工单 |
| `biz_workorder_log` | 工单操作日志 |

## 3. 功能模块清单

### 3.1 服务目录

- [ ] 展示当前园区启用的物业服务项。
- [ ] 支持服务名称、服务类型、状态筛选。
- [ ] 展示服务名称、服务类型、收费标准、服务时限、状态。
- [ ] 停用服务项不允许用户发起新工单。
- [ ] 服务项必须按排序号展示。

### 3.2 我的工单列表

- [ ] 展示当前用户或当前客户关联的工单。
- [ ] 支持工单号、客户名称、状态、优先级筛选。
- [ ] 展示工单编号、服务名称、客户、园区、房源、优先级、状态、指派人、创建时间。
- [ ] 展示待受理、处理中、已完成、已评价、已关闭统计卡片。
- [ ] 列表按创建时间倒序。

### 3.3 创建工单

- [ ] 选择物业服务项。
- [ ] 选择或带出客户。
- [ ] 自动带出联系人和联系电话。
- [ ] 优先从客户生效合同带出房源。
- [ ] 无合同房源时允许手动选择园区 / 楼栋 / 楼层 / 房间。
- [ ] 填写需求描述。
- [ ] 设置优先级。
- [ ] 提交后生成工单编号。
- [ ] 提交后写入创建日志。

### 3.4 工单处理

- [ ] 可指派处理人。
- [ ] 可变更工单状态。
- [ ] 处理中或已完成状态必须填写处理人和处置内容。
- [ ] 处置后写入操作日志。
- [ ] 已完成后记录完成时间。
- [ ] 已关闭后不再允许继续处置。

### 3.5 工单详情

- [ ] 展示基础信息。
- [ ] 展示处置信息。
- [ ] 展示评价信息。
- [ ] 展示操作日志时间线。
- [ ] 展示房源信息和园区信息。

### 3.6 工单评价

- [ ] 已完成工单可评价。
- [ ] 评价包含评分和评价内容。
- [ ] 评价后状态变为已评价。
- [ ] 评价后写入日志。

### 3.7 服务项配置

- [ ] 管理员可新增服务项。
- [ ] 管理员可编辑服务项。
- [ ] 管理员可启用 / 停用服务项。
- [ ] 管理员可删除服务项。
- [ ] 服务项必须按园区隔离。

## 4. API 清单

### 4.1 源端 RuoYi 接口

```text
GET    /business/propertyService/service/list
GET    /business/propertyService/service/get/{serviceId}
POST   /business/propertyService/service/save
PUT    /business/propertyService/service/update
DELETE /business/propertyService/service/remove/{ids}

GET    /business/propertyService/workorder/list
GET    /business/propertyService/workorder/get/{orderId}
POST   /business/propertyService/workorder/save
PUT    /business/propertyService/workorder/update
DELETE /business/propertyService/workorder/remove/{ids}
PUT    /business/propertyService/workorder/assign/{orderId}
PUT    /business/propertyService/workorder/finish/{orderId}
PUT    /business/propertyService/workorder/close/{orderId}
PUT    /business/propertyService/workorder/rate/{orderId}
GET    /business/propertyService/workorder/log/{orderId}
```

### 4.2 BladeX 建议接口

```text
GET    /blade-ics/property-service/page
GET    /blade-ics/property-service/detail
POST   /blade-ics/property-service/submit
POST   /blade-ics/property-service/remove

GET    /blade-ics/property-workorder/page
GET    /blade-ics/property-workorder/detail
POST   /blade-ics/property-workorder/submit
POST   /blade-ics/property-workorder/remove
POST   /blade-ics/property-workorder/assign
POST   /blade-ics/property-workorder/finish
POST   /blade-ics/property-workorder/close
POST   /blade-ics/property-workorder/rate
GET    /blade-ics/property-workorder/log-list
```

## 5. 数据流走向

```mermaid
flowchart LR
  A["进入我的物业服务"] --> B["加载启用服务项"]
  B --> C["选择服务项"]
  C --> D["选择客户"]
  D --> E["读取客户生效合同"]
  E --> F["带出合同房源"]
  E --> G["无合同则手选园区/楼栋/楼层/房间"]
  F --> H["提交工单"]
  G --> H
  H --> I["生成工单编号"]
  I --> J["写入 biz_service_workorder"]
  J --> K["写入 biz_workorder_log"]
  K --> L["工单列表/详情展示"]
  L --> M["指派/处理/完成/评价/关闭"]
  M --> K
```

### 5.1 服务目录数据流

- 页面加载服务项列表。
- 后端按当前园区过滤。
- 只展示 `status = '0'` 且 `del_flag = '0'` 的服务项。
- 用户选择服务项后进入工单创建。

### 5.2 工单创建数据流

- 用户选择客户。
- 前端拉取客户资料并回填联系人、联系电话。
- 前端拉取客户生效合同。
- 若合同存在 `room_ids`，自动绑定房源。
- 若无合同房源，手动选择园区、楼栋、楼层、房间。
- 后端校验服务项与工单园区一致。
- 后端生成 `WOyyyyMMddHHmmssXXXX` 工单编号。
- 写入工单主表与创建日志。

### 5.3 工单处理数据流

- 管理员或处理人打开工单。
- 选择状态、优先级、处理人、处置内容。
- 后端更新工单状态。
- 若状态为处理中，写入指派时间。
- 若状态为已完成，写入完成时间。
- 写入处理日志。

### 5.4 工单评价数据流

- 用户对已完成工单评分。
- 后端写入评分、评价内容、评价时间。
- 工单状态改为已评价。
- 写入评价日志。

## 6. 关联模块

| 模块 | 关联方式 | 迁移要求 |
| --- | --- | --- |
| 园区档案 | 工单与服务项按 `park_id` 隔离 | 园区数据必须先完成迁移 |
| 楼栋 / 楼层 / 房间 | 手动选择房源时使用 | 房源树接口必须可用 |
| 客户管理 | 工单客户、联系人、联系电话来源 | 客户 ID 和企业名称要可追溯 |
| 合同管理 | 优先从生效合同带出房源 | 合同状态和 `room_ids` 必须正确 |
| 用户管理 | 指派人、处理人来源 | 用户账号和姓名要可选 |
| 附件 / 文件 | 工单图片、服务图标 | 首期可只迁字段，文件上传后置 |
| 企业服务菜单 | 菜单入口与权限 | 角色授权必须同步 |

## 7. 迁移顺序

### 7.1 第一阶段：菜单和服务目录

- [ ] 建立“企业服务”一级菜单。
- [ ] 建立“我的物业服务”二级菜单。
- [ ] 迁移服务项列表。
- [ ] 迁移服务项查询。
- [ ] 迁移服务项启停状态。

### 7.2 第二阶段：工单列表和详情

- [ ] 迁移工单列表。
- [ ] 迁移状态统计卡片。
- [ ] 迁移工单详情。
- [ ] 迁移工单日志。

### 7.3 第三阶段：创建工单

- [ ] 迁移服务项选择。
- [ ] 迁移客户选择。
- [ ] 迁移合同房源自动带出。
- [ ] 迁移房源树手动选择。
- [ ] 迁移工单保存。
- [ ] 迁移工单编号生成。

### 7.4 第四阶段：处理和评价

- [ ] 迁移指派。
- [ ] 迁移处置。
- [ ] 迁移完成。
- [ ] 迁移关闭。
- [ ] 迁移评价。
- [ ] 迁移日志写入。

## 8. 并行 Work Tree 切片

- WT-A：菜单、服务项、权限、基础 API。
- WT-B：工单列表、详情、日志、状态统计。
- WT-C：客户、合同、房源树、创建工单。
- WT-D：指派、处置、完成、关闭、评价。

## 9. 校验清单

### 9.1 菜单校验

- [ ] “企业服务”一级菜单可见。
- [ ] “我的物业服务”二级菜单可见。
- [ ] 角色授权后可进入。
- [ ] 未授权用户无法进入。

### 9.2 服务项校验

- [ ] 只显示当前园区服务项。
- [ ] 停用服务项不允许发起工单。
- [ ] 删除服务项后列表不可见。
- [ ] 服务项排序正确。

### 9.3 工单列表校验

- [ ] 工单只显示当前园区或当前客户范围。
- [ ] 工单号筛选正确。
- [ ] 客户筛选正确。
- [ ] 状态筛选正确。
- [ ] 优先级筛选正确。
- [ ] 状态统计与列表一致。

### 9.4 工单创建校验

- [ ] 服务项必填。
- [ ] 客户必填。
- [ ] 联系人和联系电话必填。
- [ ] 房源必填。
- [ ] 需求描述必填。
- [ ] 服务项园区与工单园区一致。
- [ ] 提交后生成唯一工单编号。
- [ ] 提交后写入创建日志。

### 9.5 工单处理校验

- [ ] 指派后状态为处理中。
- [ ] 指派后写入指派时间。
- [ ] 完成后状态为已完成。
- [ ] 完成后写入完成时间。
- [ ] 关闭后状态为已关闭。
- [ ] 处理、完成、关闭均写入日志。

### 9.6 评价校验

- [ ] 已完成工单可评价。
- [ ] 评价后状态为已评价。
- [ ] 评价分数和内容正确落库。
- [ ] 评价后写入日志。

## 10. 建议核对 SQL

```sql
-- 1. 工单是否存在无效服务项
SELECT COUNT(*) AS invalid_count
FROM biz_service_workorder w
LEFT JOIN biz_property_service s ON s.service_id = w.service_id
WHERE w.del_flag = '0'
  AND w.service_id IS NOT NULL
  AND s.service_id IS NULL;

-- 2. 工单是否存在无效园区
SELECT COUNT(*) AS invalid_count
FROM biz_service_workorder w
LEFT JOIN ics_park p ON p.id = w.park_id
WHERE w.del_flag = '0'
  AND w.park_id IS NOT NULL
  AND p.id IS NULL;

-- 3. 工单日志是否存在无效工单
SELECT COUNT(*) AS invalid_count
FROM biz_workorder_log l
LEFT JOIN biz_service_workorder w ON w.order_id = l.order_id
WHERE w.order_id IS NULL;

-- 4. 状态值是否合法
SELECT order_status, COUNT(*) AS cnt
FROM biz_service_workorder
WHERE del_flag = '0'
GROUP BY order_status;

-- 5. 服务项与工单园区是否一致
SELECT COUNT(*) AS invalid_count
FROM biz_service_workorder w
JOIN biz_property_service s ON s.service_id = w.service_id
WHERE w.del_flag = '0'
  AND s.del_flag = '0'
  AND w.park_id <> s.park_id;
```

## 11. 交付标准

- [ ] 能从“企业服务 -> 我的物业服务”进入。
- [ ] 能看到当前园区启用的服务项。
- [ ] 能创建物业服务工单。
- [ ] 能查看我的工单列表和详情。
- [ ] 能完成指派、处理、完成、关闭、评价闭环。
- [ ] 工单日志完整可追溯。
- [ ] 与客户、合同、房源、园区、用户模块没有断链。
- [ ] 校验清单全部通过后，再迁移企业服务下一个二级菜单。

## 12. 风险点

- 当前源端“物业服务”是目录，目标若合并成“我的物业服务”，要处理原三级页面的入口归并。
- 工单创建依赖客户、合同、房源三条线，任一模块未迁完都可能影响创建。
- `room_ids` 是逗号分隔字段，迁移到 BladeX 时建议保留兼容，后续再考虑工单房源关联表。
- 服务图标和工单图片字段存在，但前端当前只做了弱处理，文件上传可后置。
- 园区隔离必须先做，否则服务项和工单会跨园区串用。

