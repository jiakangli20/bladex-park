# BladeX 建筑管理迁移清单

本文档只覆盖“园区管理 > 建筑管理”二级菜单的迁移准备与验收，不包含园区档案、楼层管理、房源管理、合同、工单、审批、客户等模块的代码迁移。建筑管理迁移必须以已迁移完成的园区档案模块为前置条件。

## 1. 迁移范围

### 1.1 目标菜单

- 一级菜单：园区管理
- 二级菜单：建筑管理
- 目标前端目录：`/Users/jiakangli/Desktop/bladex-park/saber3/src/views/ics/building`
- 目标前端 API：`/Users/jiakangli/Desktop/bladex-park/saber3/src/api/ics/building.js`
- 目标后端模块：`org.springblade.modules.ics`
- 目标后端路径：`/Users/jiakangli/Desktop/bladex-park/BladeX-Boot`
- 目标数据库：`bladex_boot`
- 目标业务表：`ics_building`

### 1.2 源代码参考

后端：

- `ruoyi-business/src/main/java/com/ruoyi/business/controller/BuildingController.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/domain/Building.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/service/IBuildingService.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/service/impl/BuildingServiceImpl.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/mapper/BuildingMapper.java`
- `ruoyi-business/src/main/resources/mapper/business/BuildingMapper.xml`

前端：

- `ruoyi-ui/src/api/business/building.js`
- `ruoyi-ui/src/views/business/BuildingList.vue`
- `ruoyi-ui/src/views/business/modules/BuildingModal.vue`

只读依赖参考：

- `ruoyi-business/src/main/java/com/ruoyi/business/service/IParkService.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/service/IFloorService.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/mapper/RoomMapper.java`

### 1.3 明确不迁移范围

- 不迁移楼层管理菜单。
- 不迁移房间管理菜单。
- 不迁移楼层看板。
- 不迁移合同、工单、审批、客户模块。
- 不重构 `ics_building` 表结构。
- 不把园区 ID 当作 BladeX `tenant_id`。
- 不覆盖 `saber3` 现有本地配置：前端 8080 代理、SM2/crypto 配置。

## 2. 前置条件

### 2.1 环境前置

- BladeX 后端能启动。
- BladeX 前端 `saber3` 能启动。
- BladeX 登录正常。
- Redis 连通。
- `bladex_boot` 已导入 `ics_park`、`ics_building`、`ics_floor`、`ics_room`。
- 园区档案模块已迁移或至少提供可用园区下拉接口。

### 2.2 数据前置

当前 `bladex_boot` 中建筑相关数据：

| 表 | 当前数量 | 用途 |
| --- | ---: | --- |
| `ics_park` | 2 | 建筑所属园区 |
| `ics_building` | 5 | 建筑主数据 |
| `ics_floor` | 16 | 建筑楼层与管理面积统计 |
| `ics_room` | 2 | 建筑房间数、出租率、在租面积统计 |

迁移前必须确认以下校验全部为 `0`：

```sql
SELECT 'building_orphan_park' AS check_item, COUNT(*) AS invalid_count
FROM ics_building b
LEFT JOIN ics_park p ON p.id = b.park_id
WHERE b.park_id IS NOT NULL AND p.id IS NULL
UNION ALL
SELECT 'floor_orphan_building', COUNT(*)
FROM ics_floor f
LEFT JOIN ics_building b ON b.id = f.building_id
WHERE f.building_id IS NOT NULL AND b.id IS NULL
UNION ALL
SELECT 'room_orphan_building', COUNT(*)
FROM ics_room r
LEFT JOIN ics_building b ON b.id = r.building_id
WHERE r.building_id IS NOT NULL AND b.id IS NULL
UNION ALL
SELECT 'floor_park_mismatch', COUNT(*)
FROM ics_floor f
JOIN ics_building b ON b.id = f.building_id
WHERE f.park_id <> b.park_id
UNION ALL
SELECT 'room_park_mismatch', COUNT(*)
FROM ics_room r
JOIN ics_building b ON b.id = r.building_id
WHERE r.park_id <> b.park_id;
```

## 3. 数据模型清单

### 3.1 主表 `ics_building`

| 字段 | 类型/含义 | 迁移说明 |
| --- | --- | --- |
| `id` | 主键 | 保留原 ID，第一阶段不改雪花 ID |
| `park_id` | 所属园区 ID | 必须引用 `ics_park.id` |
| `name` | 建筑名称 | 必填，同园区唯一 |
| `code` | 建筑编码 | 必填 |
| `address` | 建筑地址 | 可选 |
| `region` | 所属地区 | 必填 |
| `real_estate_no` | 不动产编号 | 可选 |
| `property_no` | 产权编号 | 可选 |
| `land_no` | 土地编号 | 可选 |
| `sort_num` | 排序值 | 必填，列表排序使用 |
| `floors` | 楼层数 | 必填，必须大于 0 |
| `area` | 建筑面积 | 必填，必须大于 0 |
| `property_area` | 产权面积 | 必填，必须大于 0 |
| `rentable_area` | 可租面积 | 可选 |
| `self_use_area` | 自用面积 | 可选 |
| `supporting_area` | 配套面积 | 可选 |
| `parking_area` | 车位面积 | 可选 |
| `standard_floor_height` | 标准层高 | 可选 |
| `building_type` | 产权性质 | 必填，当前值包括自持、租赁、合作、厂房等 |
| `status` | 状态 | `0` 启用，`1` 停用 |
| `memo` | 备注 | 可选，用于建筑标签拼接 |
| `create_by/create_time` | 创建信息 | 改用 BladeX 用户上下文 |
| `update_by/update_time` | 更新信息 | 改用 BladeX 用户上下文 |

### 3.2 索引与约束

当前目标表已有：

- 主键：`PRIMARY KEY (id)`
- 唯一索引：`uk_park_building_name (park_id, name)`

迁移时必须保留：

- 同一园区下建筑名称唯一。
- 不允许新增无园区建筑。
- 不允许建筑楼层数小于已有房源最大楼层。

### 3.3 列表派生字段

建筑列表除主表字段外，还需要返回以下派生字段：

| 字段 | 来源 | 说明 |
| --- | --- | --- |
| `parkName` | `ics_park.name` | 所属园区名称 |
| `managementArea` | `SUM(ics_floor.area)` | 建筑管理面积 |
| `roomCount` | `COUNT(ics_room.id)` | 建筑房间总数 |
| `rentRate` | 已租房间数 / 房间总数 | 出租率百分比 |
| `rentedArea` | 已租房间面积合计 | 仅统计 `ics_room.status = '1'` |
| `activeContractCount` | `biz_contract` | 在租合同数，第一阶段可保留 SQL 兼容，也可先返回 0 并标记待合同模块迁移 |

## 4. 功能模块详细清单

### 4.1 建筑列表

功能项：

- [ ] 分页查询建筑列表。
- [ ] 按建筑名称模糊查询。
- [ ] 按所属园区筛选。
- [ ] 按建筑编码筛选，后端接口保留。
- [ ] 按状态筛选，后端接口保留。
- [ ] 列表展示建筑名称、所属园区、建筑编号、建筑标签。
- [ ] 列表展示建筑面积、可租面积、管理面积。
- [ ] 列表展示房间总数、出租率、在租面积、在租合同数。
- [ ] 按园区名称、园区 ID、排序值、建筑名称、ID 排序。
- [ ] 复选多选。
- [ ] 新增、编辑、删除按钮按权限显示。
- [ ] 导入、导出按钮按权限显示。

数据流：

```text
saber3 建筑管理页面
  -> GET /blade-ics/building/list
  -> BuildingController.page/list
  -> BuildingService.page/list
  -> BuildingMapper.selectBuildingPage
  -> ics_building
  -> LEFT JOIN ics_park
  -> LEFT JOIN ics_floor 汇总管理面积
  -> LEFT JOIN ics_room 汇总房间与出租率
  -> 可选 LEFT JOIN biz_contract 汇总在租合同数
  -> BladeX R.data(IPage<BuildingVO>)
```

### 4.2 建筑详情

功能项：

- [ ] 根据 `id` 查询建筑详情。
- [ ] 返回主表字段。
- [ ] 返回 `parkName`。
- [ ] 返回当前建筑已有楼层面积配置，供编辑弹窗回填。
- [ ] 无数据时返回 BladeX 标准错误或空对象，不返回 RuoYi 风格 `null`。

数据流：

```text
点击编辑
  -> GET /blade-ics/building/detail?id=xxx
  -> 查询 ics_building
  -> 校验所属园区存在
  -> 查询 ics_floor where building_id = xxx
  -> 返回建筑详情 + floorAreas
```

### 4.3 新增建筑

功能项：

- [ ] 选择所属园区。
- [ ] 输入建筑编码。
- [ ] 输入建筑名称。
- [ ] 输入所属地区。
- [ ] 输入建筑地址。
- [ ] 选择产权性质。
- [ ] 输入不动产编号、产权编号、土地编号。
- [ ] 输入排序值。
- [ ] 输入楼层数。
- [ ] 选择状态。
- [ ] 输入建筑面积、产权面积、可租面积、自用面积、配套面积、车位面积、标准层高。
- [ ] 输入备注。
- [ ] 配置每层楼层面积 `floorAreas`。
- [ ] 保存时校验同园区建筑名称唯一。
- [ ] 保存后自动生成 `1..floors` 的 `ics_floor` 记录。
- [ ] 保存后应用 `floorAreas` 到对应楼层。

数据流：

```text
提交新增建筑
  -> POST /blade-ics/building/submit
  -> 校验 park_id 是否存在
  -> 校验 code/name/region/building_type/sort_num/floors/area/property_area
  -> 校验 uk_park_building_name
  -> insert ics_building
  -> syncBuildingFloors(buildingId)
  -> updateBuildingFloorAreas(buildingId, floorAreas)
  -> 返回保存成功
```

### 4.4 修改建筑

功能项：

- [ ] 编辑时回显所有建筑字段。
- [ ] 编辑时回显楼层面积配置。
- [ ] 修改所属园区时重新校验园区存在。
- [ ] 修改建筑名称时重新校验同园区唯一。
- [ ] 修改楼层数时校验已有房间最大楼层。
- [ ] 修改楼层数变大时自动补齐楼层。
- [ ] 修改楼层数变小时只删除超出范围且无房间的楼层。
- [ ] 修改楼层面积时不能小于该层房间面积合计。
- [ ] 修改后更新 `update_user/update_time` 或项目采用的审计字段。

数据流：

```text
提交建筑修改
  -> POST /blade-ics/building/submit 或 PUT /blade-ics/building/update
  -> 查询旧建筑
  -> 校验 park_id、必填项、唯一性
  -> 查询 max(ics_room.floor) 校验楼层数不能小于已有房源最高楼层
  -> update ics_building
  -> syncBuildingFloors(buildingId)
  -> updateBuildingFloorAreas(buildingId, floorAreas)
  -> 返回保存成功
```

### 4.5 删除建筑

功能项：

- [ ] 支持单条删除。
- [ ] 支持批量删除。
- [ ] 删除前校验建筑存在。
- [ ] 删除前校验建筑下是否有楼层。
- [ ] 删除前校验建筑下是否有房间。
- [ ] 删除前校验建筑是否被合同引用。
- [ ] 删除失败时返回明确原因。
- [ ] 删除成功后刷新列表。

重要说明：

RuoYi 当前 `deleteBuildingByIds` 直接删除建筑，风险较高。迁移到 BladeX 时必须加依赖校验，避免破坏 `ics_floor`、`ics_room`、`biz_contract`。

删除依赖校验 SQL：

```sql
SELECT COUNT(*) FROM ics_floor WHERE building_id = #{buildingId};
SELECT COUNT(*) FROM ics_room WHERE building_id = #{buildingId};
SELECT COUNT(*) FROM biz_contract
WHERE del_flag = '0'
  AND (
    building_id = #{buildingId}
    OR FIND_IN_SET(#{buildingId}, REPLACE(building_ids, 'building_', ''))
  );
```

### 4.6 导入建筑

功能项：

- [ ] 下载导入模板。
- [ ] 上传 `.xls/.xlsx` 文件。
- [ ] Excel 字段映射与 RuoYi 保持一致。
- [ ] 通过园区名称匹配 `park_id`。
- [ ] 园区不存在时返回具体行/建筑名称错误。
- [ ] 导入时复用新增建筑校验。
- [ ] 导入成功后返回成功数量。
- [ ] 导入失败时不产生半截脏数据，建议事务控制。

导入字段：

- 所属园区
- 建筑名称
- 建筑编码
- 建筑地址
- 所属地区
- 不动产编号
- 产权编号
- 土地编号
- 排序值
- 楼层数
- 建筑面积
- 产权面积
- 可租面积
- 自用面积
- 配套面积
- 车位面积
- 标准层高
- 产权性质
- 状态
- 备注

### 4.7 导出建筑

功能项：

- [ ] 按当前筛选条件导出。
- [ ] 导出字段与导入模板一致。
- [ ] 导出时包含所属园区名称。
- [ ] 导出权限单独控制。
- [ ] 导出文件名清晰，例如 `建筑信息.xlsx`。

## 5. 与其他模块的关联关系

| 关联模块 | 关联点 | 建筑管理迁移处理方式 |
| --- | --- | --- |
| 园区档案 | `ics_building.park_id -> ics_park.id` | 强依赖。建筑新增/编辑必须校验园区存在，列表需要园区下拉 |
| 楼层管理 | `ics_floor.building_id` | 只做数据维护和同步，不迁移楼层菜单 |
| 房间管理 | `ics_room.building_id`、`ics_room.floor` | 只读依赖。用于楼层数校验、统计、删除保护 |
| 合同管理 | `biz_contract.building_id/building_ids` | 只读依赖。用于在租合同统计、删除保护 |
| 园区权限 | 用户授权园区列表 | 建筑列表、新增、修改、删除都应受园区范围限制 |
| 字典管理 | 产权性质、状态 | 第一阶段可用固定选项，后续迁入 BladeX 字典 |
| 操作日志 | 新增/修改/删除/导入/导出 | 使用 BladeX 操作日志能力 |

## 6. 权限与菜单清单

### 6.1 菜单

- 菜单名称：建筑管理
- 上级菜单：园区管理
- 前端组件建议：`/ics/building/index`
- 后端接口前缀建议：`/blade-ics/building`
- 菜单类型：二级菜单
- 是否缓存：按 Saber 现有规则处理

### 6.2 按钮权限

| 功能 | 权限编码建议 |
| --- | --- |
| 建筑列表 | `park:building:list` |
| 建筑详情 | `park:building:detail` |
| 建筑新增 | `park:building:add` |
| 建筑修改 | `park:building:edit` |
| 建筑删除 | `park:building:delete` |
| 建筑导入 | `park:building:import` |
| 建筑导出 | `park:building:export` |
| 建筑模板下载 | `park:building:template` |

权限验收：

- [ ] 无新增权限时不显示新增按钮。
- [ ] 无编辑权限时不显示编辑按钮。
- [ ] 无删除权限时不显示删除按钮。
- [ ] 无导入权限时不显示导入按钮。
- [ ] 无导出权限时不显示导出按钮。
- [ ] 直接调用无权限接口时后端拒绝。

## 7. 后端迁移任务清单

目标包建议：

```text
org.springblade.modules.ics.controller
org.springblade.modules.ics.mapper
org.springblade.modules.ics.pojo.entity
org.springblade.modules.ics.pojo.dto
org.springblade.modules.ics.pojo.vo
org.springblade.modules.ics.service
org.springblade.modules.ics.service.impl
org.springblade.modules.ics.wrapper
```

任务：

- [ ] 新建 `Building` 实体，对应 `ics_building`。
- [ ] 新建 `BuildingDTO`，承接新增/修改和 `floorAreas`。
- [ ] 新建 `BuildingVO`，承接列表派生字段。
- [ ] 新建 `BuildingFloorAreaDTO`，包含 `floorNo`、`area`。
- [ ] 新建 `BuildingMapper`。
- [ ] 新建 `BuildingMapper.xml`。
- [ ] 新建 `IBuildingService`。
- [ ] 新建 `BuildingServiceImpl`。
- [ ] 新建 `BuildingController`。
- [ ] 实现分页列表。
- [ ] 实现详情。
- [ ] 实现新增。
- [ ] 实现修改。
- [ ] 实现删除。
- [ ] 实现导入。
- [ ] 实现导出。
- [ ] 实现模板下载。
- [ ] 接入 BladeX 当前用户上下文。
- [ ] 接入 BladeX 标准 `R` 响应。
- [ ] 使用 MyBatis-Plus 分页。
- [ ] 不依赖 RuoYi `BaseController/startPage/TableDataInfo/AjaxResult`。

## 8. 前端迁移任务清单

目标目录建议：

```text
saber3/src/api/ics/building.js
saber3/src/views/ics/building/index.vue
saber3/src/views/ics/building/components/building-form.vue
```

任务：

- [ ] 新建建筑 API 文件。
- [ ] 实现建筑列表页面。
- [ ] 实现建筑新增/编辑抽屉或弹窗。
- [ ] 实现园区下拉。
- [ ] 实现查询条件。
- [ ] 实现分页表格。
- [ ] 实现新增、编辑、删除按钮。
- [ ] 实现批量删除。
- [ ] 实现导入弹窗。
- [ ] 实现模板下载。
- [ ] 实现导出。
- [ ] 实现楼层面积配置。
- [ ] 实现权限按钮控制。
- [ ] 适配 BladeX/Saber 的请求封装和响应结构。
- [ ] 不复用 Vue2 Ant Design 组件代码，按 Vue3 + Element Plus / Avue 重写。

## 9. 校验清单

### 9.1 数据完整性校验

迁移完成后执行：

```sql
SELECT COUNT(*) AS building_count FROM ics_building;

SELECT b.*
FROM ics_building b
LEFT JOIN ics_park p ON p.id = b.park_id
WHERE b.park_id IS NOT NULL AND p.id IS NULL;

SELECT park_id, name, COUNT(*) AS duplicate_count
FROM ics_building
GROUP BY park_id, name
HAVING COUNT(*) > 1;

SELECT f.*
FROM ics_floor f
LEFT JOIN ics_building b ON b.id = f.building_id
WHERE b.id IS NULL;

SELECT r.*
FROM ics_room r
LEFT JOIN ics_building b ON b.id = r.building_id
WHERE b.id IS NULL;

SELECT f.id, f.park_id AS floor_park_id, b.park_id AS building_park_id
FROM ics_floor f
JOIN ics_building b ON b.id = f.building_id
WHERE f.park_id <> b.park_id;

SELECT r.id, r.park_id AS room_park_id, b.park_id AS building_park_id
FROM ics_room r
JOIN ics_building b ON b.id = r.building_id
WHERE r.park_id <> b.park_id;
```

要求：

- [ ] `ics_building` 数量仍为 5，除非本次验收中明确新增/删除了测试数据。
- [ ] 孤儿园区为 0。
- [ ] 同园区重复建筑名为 0。
- [ ] 孤儿楼层为 0。
- [ ] 孤儿房间为 0。
- [ ] 楼层园区不一致为 0。
- [ ] 房间园区不一致为 0。

### 9.2 功能验收清单

- [ ] 建筑管理菜单可见。
- [ ] 建筑列表能显示 `bladex_boot.ics_building` 中的 5 条数据。
- [ ] 列表能显示所属园区名称。
- [ ] 建筑名称查询可用。
- [ ] 园区筛选可用。
- [ ] 分页可用。
- [ ] 排序符合园区、排序值、名称逻辑。
- [ ] 新增建筑可用。
- [ ] 新增后自动生成楼层。
- [ ] 编辑建筑可用。
- [ ] 编辑楼层数变大后自动补齐楼层。
- [ ] 编辑楼层数变小时不会删除已有房源楼层。
- [ ] 楼层面积配置保存可用。
- [ ] 删除无依赖建筑可用。
- [ ] 删除有楼层/房间/合同依赖的建筑会被拦截。
- [ ] 导入模板可下载。
- [ ] 导入建筑可用。
- [ ] 导出建筑可用。
- [ ] 权限按钮生效。
- [ ] 操作日志可记录关键操作。

### 9.3 关联模块回归清单

- [ ] 园区档案列表和详情不受影响。
- [ ] 建筑新增后园区统计的管理面积、房间数逻辑不异常。
- [ ] `ics_floor` 数据不被误删。
- [ ] `ics_room` 数据不被误删。
- [ ] 合同中已有 `building_id/building_ids` 不被破坏。
- [ ] 房间列表仍能根据建筑查询。
- [ ] 楼层列表仍能根据建筑查询。

## 10. 风险与处理

| 风险 | 影响 | 处理方式 |
| --- | --- | --- |
| 建筑删除未校验依赖 | 造成楼层、房间、合同孤儿数据 | 删除前强制查 `ics_floor`、`ics_room`、`biz_contract` |
| 楼层数修改过小 | 已有房间楼层超出建筑楼层数 | 修改前查 `MAX(ics_room.floor)` |
| 楼层面积小于房间面积 | 楼层统计异常 | 保存楼层面积前查该层房间面积合计 |
| 合同模块未迁移 | 在租合同统计可能不可用 | 第一阶段保留兼容 SQL，或暂时返回 0 并标记 |
| 园区权限未建模 | 普通用户越权看建筑 | 建筑查询统一接入园区授权范围 |
| Vue2 页面直接复制 | Saber/Vue3 无法运行 | 只迁移交互与字段，不复制组件实现 |
| 导入半成功 | 产生部分建筑与楼层 | 导入使用事务，逐行校验后再写入 |

## 11. 建议执行顺序

1. 确认园区档案模块接口可用，至少能提供园区下拉。
2. 建立建筑管理菜单和按钮权限。
3. 实现后端 `Building` 实体、Mapper、Service、Controller。
4. 先完成列表和详情。
5. 再完成新增和修改，并接入楼层自动同步。
6. 补充删除保护。
7. 实现前端列表和表单。
8. 实现导入、导出、模板下载。
9. 跑 SQL 数据校验。
10. 跑页面功能验收。
11. 跑关联模块回归清单。

