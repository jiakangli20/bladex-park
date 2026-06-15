# BladeX 楼层管理迁移清单

本文档只覆盖“园区管理 > 楼层管理”二级菜单的迁移准备与验收，不包含园区档案、建筑管理、房源管理、楼层看板、合同、工单、审批、客户等模块的代码迁移。楼层管理迁移必须以园区档案与建筑管理模块可用为前置条件。

## 1. 迁移范围

### 1.1 目标菜单

- 一级菜单：园区管理
- 二级菜单：楼层管理
- 目标前端目录：`/Users/jiakangli/Desktop/bladex-park/saber3/src/views/ics/floor`
- 目标前端 API：`/Users/jiakangli/Desktop/bladex-park/saber3/src/api/ics/floor.js`
- 目标后端模块：`org.springblade.modules.ics`
- 目标后端路径：`/Users/jiakangli/Desktop/bladex-park/BladeX-Boot`
- 目标数据库：`bladex_boot`
- 目标业务表：`ics_floor`

### 1.2 源代码参考

后端：

- `ruoyi-business/src/main/java/com/ruoyi/business/controller/FloorController.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/domain/Floor.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/service/IFloorService.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/service/impl/FloorServiceImpl.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/mapper/FloorMapper.java`
- `ruoyi-business/src/main/resources/mapper/business/FloorMapper.xml`

前端：

- `ruoyi-ui/src/api/business/floor.js`
- `ruoyi-ui/src/views/business/FloorList.vue`

只读依赖参考：

- `ruoyi-business/src/main/java/com/ruoyi/business/domain/Building.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/mapper/BuildingMapper.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/mapper/RoomMapper.java`
- `ruoyi-ui/src/api/business/building.js`
- `ruoyi-ui/src/api/business/room.js`

### 1.3 明确不迁移范围

- 不迁移建筑管理菜单，但楼层管理依赖建筑下拉和建筑详情。
- 不迁移房源管理菜单，但楼层管理需要只读展示房间列表和房间状态统计。
- 不迁移楼层看板菜单，仅保留跳转入口或在迁移看板时再接入。
- 不迁移房间新增、编辑、删除、状态流转能力到本菜单；这些属于房源管理模块，楼层管理第一阶段只做只读展示或保留入口禁用。
- 不迁移合同、工单、审批、客户模块。
- 不重构 `ics_floor` 表结构。
- 不把园区 ID 当作 BladeX `tenant_id`。
- 不覆盖 `saber3` 现有本地配置：前端 8080 代理、SM2/crypto 配置。

## 2. 前置条件

### 2.1 环境前置

- BladeX 后端能启动。
- BladeX 前端 `saber3` 能启动。
- BladeX 登录正常。
- Redis 连通。
- `bladex_boot` 已导入 `ics_park`、`ics_building`、`ics_floor`、`ics_room`。
- 园区档案模块可用，至少能提供园区数据。
- 建筑管理模块可用，至少能提供建筑列表下拉。

### 2.2 数据前置

当前 `bladex_boot` 中楼层相关数据：

| 表 | 当前数量 | 用途 |
| --- | ---: | --- |
| `ics_park` | 2 | 楼层所属园区 |
| `ics_building` | 5 | 楼层所属建筑 |
| `ics_floor` | 16 | 楼层主数据 |
| `ics_room` | 2 | 楼层房间统计与面积校验 |

迁移前必须确认以下校验全部为 `0`：

```sql
SELECT 'floor_orphan_building' AS check_item, COUNT(*) AS invalid_count
FROM ics_floor f
LEFT JOIN ics_building b ON b.id = f.building_id
WHERE f.building_id IS NOT NULL AND b.id IS NULL
UNION ALL
SELECT 'floor_orphan_park', COUNT(*)
FROM ics_floor f
LEFT JOIN ics_park p ON p.id = f.park_id
WHERE f.park_id IS NOT NULL AND p.id IS NULL
UNION ALL
SELECT 'floor_park_mismatch', COUNT(*)
FROM ics_floor f
JOIN ics_building b ON b.id = f.building_id
WHERE f.park_id <> b.park_id
UNION ALL
SELECT 'floor_duplicate', COUNT(*)
FROM (
    SELECT building_id, floor_no
    FROM ics_floor
    GROUP BY building_id, floor_no
    HAVING COUNT(*) > 1
) d
UNION ALL
SELECT 'floor_over_building_floors', COUNT(*)
FROM ics_floor f
JOIN ics_building b ON b.id = f.building_id
WHERE f.floor_no < 1 OR f.floor_no > b.floors;
```

## 3. 数据模型清单

### 3.1 主表 `ics_floor`

| 字段 | 类型/含义 | 迁移说明 |
| --- | --- | --- |
| `id` | 楼层 ID | 保留原 ID，第一阶段不改雪花 ID |
| `park_id` | 园区 ID | 必须与所属建筑 `park_id` 一致 |
| `building_id` | 建筑 ID | 必须引用 `ics_building.id` |
| `floor_no` | 楼层号 | 必填，必须在 `1..building.floors` 范围内 |
| `area` | 楼层面积 | 可为空，但保存时不能小于该层房间面积合计 |
| `status` | 状态 | `0` 启用，`1` 停用 |
| `memo` | 备注 | 可选 |
| `create_by/create_time` | 创建信息 | 改用 BladeX 用户上下文 |
| `update_by/update_time` | 更新信息 | 改用 BladeX 用户上下文 |

### 3.2 索引与约束

当前目标表已有：

- 主键：`PRIMARY KEY (id)`
- 唯一索引：`uk_building_floor (building_id, floor_no)`
- 普通索引：`idx_park_id (park_id)`

迁移时必须保留：

- 同一建筑下楼层号唯一。
- 楼层园区必须与建筑园区一致。
- 楼层号不能超出建筑楼层数。
- 楼层已有房间时不允许删除。
- 楼层面积不能小于该楼层房间面积合计。

### 3.3 列表派生字段

楼层列表除主表字段外，还需要返回以下派生字段：

| 字段 | 来源 | 说明 |
| --- | --- | --- |
| `parkName` | `ics_park.name` | 所属园区名称 |
| `buildingName` | `ics_building.name` | 所属建筑名称 |
| `rooms` | `ics_room` | 展开行房间列表，第一阶段可只读展示 |
| `totalCount` | `COUNT(ics_room.id)` | 楼层房间总数 |
| `rentedCount` | `ics_room.status = '1'` | 已出租房间数 |
| `vacantCount` | `ics_room.status = '0'` | 空闲房间数 |
| `reservedCount` | `ics_room.status = '2'` | 已预定房间数 |
| `renovatingCount` | `ics_room.status = '3'` | 装修中房间数 |
| `disabledCount` | `ics_room.status = '4'` | 停用房间数 |
| `usedArea` | `SUM(ics_room.area)` | 已录入房间面积合计 |
| `occupancyRate` | `rentedCount / totalCount` | 前端或后端计算均可 |

## 4. 功能模块详细清单

### 4.1 楼层列表

功能项：

- [ ] 查询楼层列表。
- [ ] 按所属建筑筛选。
- [ ] 按楼层号筛选。
- [ ] 按楼层状态筛选，后端保留。
- [ ] 按房间状态筛选展开房间，第一阶段可只影响前端展示。
- [ ] 展示园区 / 建筑。
- [ ] 展示楼层号。
- [ ] 展示房间总数。
- [ ] 展示楼层面积。
- [ ] 展示已用面积。
- [ ] 展示出租率。
- [ ] 展示房间状态统计。
- [ ] 支持展开查看房间列表。
- [ ] 支持前端分页或后端分页，建议后端 MyBatis-Plus 分页。
- [ ] 按园区、建筑、楼层号倒序排序。

数据流：

```text
saber3 楼层管理页面
  -> GET /blade-ics/floor/list
  -> FloorController.list/page
  -> FloorService.list/page
  -> FloorMapper.selectFloorList
  -> ics_floor
  -> LEFT JOIN ics_building
  -> LEFT JOIN ics_park
  -> 查询 ics_room 按 building_id + floor 分组
  -> 组装楼层统计 totalCount/rentedCount/vacantCount/usedArea/rooms
  -> BladeX R.data(IPage<FloorVO> 或 List<FloorVO>)
```

### 4.2 楼层详情

功能项：

- [ ] 根据 `id` 查询楼层详情。
- [ ] 返回主表字段。
- [ ] 返回 `parkName`、`buildingName`。
- [ ] 返回房间统计。
- [ ] 无数据时返回 BladeX 标准错误，不返回 RuoYi 风格 `null`。

数据流：

```text
点击编辑楼层
  -> GET /blade-ics/floor/detail?id=xxx
  -> 查询 ics_floor
  -> 校验 building_id 存在
  -> 聚合该楼层房间面积与状态统计
  -> 返回 FloorVO
```

### 4.3 新增楼层

功能项：

- [ ] 选择所属建筑。
- [ ] 输入楼层号。
- [ ] 输入楼层面积。
- [ ] 选择状态。
- [ ] 输入备注。
- [ ] 保存时根据建筑反填 `park_id`。
- [ ] 保存时校验建筑存在。
- [ ] 保存时校验楼层号在建筑楼层范围内。
- [ ] 保存时校验同建筑楼层号唯一。
- [ ] 保存时校验楼层面积不能小于该楼层已有房间面积合计。

数据流：

```text
提交新增楼层
  -> POST /blade-ics/floor/submit
  -> 查询 ics_building
  -> floor.park_id = building.park_id
  -> 校验 floor_no >= 1 且 floor_no <= building.floors
  -> 校验 uk_building_floor
  -> 查询 SUM(ics_room.area) 校验面积
  -> insert ics_floor
  -> 返回保存成功
```

### 4.4 修改楼层

功能项：

- [ ] 编辑时回显楼层字段。
- [ ] 修改建筑时重新根据建筑反填 `park_id`。
- [ ] 修改楼层号时重新校验范围和唯一性。
- [ ] 修改楼层面积时重新校验房间面积合计。
- [ ] 修改状态和备注。
- [ ] 保存后刷新列表和统计。

数据流：

```text
提交楼层修改
  -> POST /blade-ics/floor/submit 或 PUT /blade-ics/floor/update
  -> 查询旧楼层
  -> 查询目标建筑
  -> 校验 park_id 与 building.park_id 一致
  -> 校验 floor_no 范围
  -> 校验同建筑楼层号唯一
  -> 查询 SUM(ics_room.area) 校验面积
  -> update ics_floor
  -> 返回保存成功
```

### 4.5 删除楼层

功能项：

- [ ] 支持单条删除。
- [ ] 支持批量删除。
- [ ] 删除前校验楼层存在。
- [ ] 删除前校验楼层所属园区/建筑在当前用户授权范围内。
- [ ] 删除前校验该楼层无房间。
- [ ] 删除失败时返回明确原因。
- [ ] 删除成功后刷新列表。

删除依赖校验 SQL：

```sql
SELECT COUNT(*)
FROM ics_room
WHERE building_id = #{buildingId}
  AND floor = #{floorNo};
```

重要说明：

- 房间表没有 `floor_id`，楼层与房间通过 `building_id + floor` 关联。
- 删除楼层不能只看 `ics_floor.id`，必须按 `building_id + floor_no` 查房间。

### 4.6 同步建筑楼层

功能项：

- [ ] 支持同步单个建筑楼层。
- [ ] 支持同步全部建筑楼层。
- [ ] 同步前校验建筑存在。
- [ ] 删除重复楼层，只保留最早记录。
- [ ] 根据 `ics_building.floors` 补齐缺失楼层。
- [ ] 默认楼层面积 = 建筑面积 / 楼层数，保留两位小数。
- [ ] 对超出建筑楼层数且无房间的楼层进行清理。
- [ ] 对超出建筑楼层数但已有房间的楼层不删除，返回提示或保留并记录风险。

数据流：

```text
点击同步建筑楼层
  -> POST /blade-ics/floor/sync/{buildingId}
  -> 查询 ics_building
  -> 删除 building_id + floor_no 重复楼层
  -> 遍历 1..building.floors
  -> 缺失则 insert ics_floor
  -> 查询 floor_no > building.floors 的楼层
  -> 无房间则删除，有房间则保留
  -> 返回同步成功
```

### 4.7 楼层房间展开展示

功能项：

- [ ] 展开楼层行展示该楼层房间。
- [ ] 房间字段只读展示：房间名称、面积、月租金、物业费、朝向、状态、小程序同步状态。
- [ ] 房间状态统计来自后端或前端统一口径。
- [ ] 第一阶段不在楼层管理里实现房间新增、编辑、删除、状态流转；这些入口可隐藏、禁用或跳转到房源管理模块。

数据流：

```text
楼层列表查询
  -> 后端按 floor 查询 rooms
  -> rooms 按 building_id + floor 分组
  -> 每个 FloorVO 附带 rooms
  -> 前端展开行渲染只读房间列表
```

## 5. 与其他模块的关联关系

| 关联模块 | 关联点 | 楼层管理迁移处理方式 |
| --- | --- | --- |
| 园区档案 | `ics_floor.park_id -> ics_park.id` | 强依赖。列表展示园区名称，权限按园区过滤 |
| 建筑管理 | `ics_floor.building_id -> ics_building.id` | 强依赖。楼层新增/修改/同步必须校验建筑 |
| 房源管理 | `ics_room.building_id + ics_room.floor` | 只读依赖。用于统计、面积校验、删除保护 |
| 楼层看板 | 使用楼层、房间聚合数据 | 本次不迁移，只保留后续接入点 |
| 合同管理 | 间接依赖房间 | 本次不直接查询合同 |
| 园区权限 | 用户授权园区列表 | 楼层列表、新增、修改、删除都应受园区范围限制 |
| 字典管理 | 楼层状态、房间状态 | 第一阶段可固定选项，后续迁入 BladeX 字典 |
| 操作日志 | 新增/修改/删除/同步 | 使用 BladeX 操作日志能力 |

## 6. 权限与菜单清单

### 6.1 菜单

- 菜单名称：楼层管理
- 上级菜单：园区管理
- 前端组件建议：`/ics/floor/index`
- 后端接口前缀建议：`/blade-ics/floor`
- 菜单类型：二级菜单

### 6.2 按钮权限

| 功能 | 权限编码建议 |
| --- | --- |
| 楼层列表 | `park:floor:list` |
| 楼层详情 | `park:floor:detail` |
| 楼层新增 | `park:floor:add` |
| 楼层修改 | `park:floor:edit` |
| 楼层删除 | `park:floor:delete` |
| 楼层同步 | `park:floor:sync` |
| 查看房间 | `park:floor:room-view` |

权限验收：

- [ ] 无新增权限时不显示新增楼层按钮。
- [ ] 无编辑权限时不显示编辑楼层按钮。
- [ ] 无删除权限时不显示删除楼层按钮。
- [ ] 无同步权限时不显示同步按钮。
- [ ] 无房间查看权限时不允许展开房间列表。
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

- [ ] 新建 `Floor` 实体，对应 `ics_floor`。
- [ ] 新建 `FloorDTO`，承接新增/修改。
- [ ] 新建 `FloorVO`，承接列表派生字段与房间列表。
- [ ] 新建 `FloorMapper`。
- [ ] 新建 `FloorMapper.xml`。
- [ ] 新建 `IFloorService`。
- [ ] 新建 `FloorServiceImpl`。
- [ ] 新建 `FloorController`。
- [ ] 实现列表查询。
- [ ] 实现详情查询。
- [ ] 实现新增楼层。
- [ ] 实现修改楼层。
- [ ] 实现删除楼层。
- [ ] 实现同步单建筑楼层。
- [ ] 可选实现同步全部建筑楼层。
- [ ] 实现楼层房间统计。
- [ ] 实现楼层面积校验。
- [ ] 实现楼层删除保护。
- [ ] 接入 BladeX 当前用户上下文。
- [ ] 接入 BladeX 标准 `R` 响应。
- [ ] 不依赖 RuoYi `BaseController/startPage/TableDataInfo/AjaxResult`。

## 8. 前端迁移任务清单

目标目录建议：

```text
saber3/src/api/ics/floor.js
saber3/src/views/ics/floor/index.vue
saber3/src/views/ics/floor/components/floor-form.vue
```

任务：

- [ ] 新建楼层 API 文件。
- [ ] 实现楼层管理页面。
- [ ] 实现建筑下拉。
- [ ] 实现查询条件：建筑、楼层号、房间状态。
- [ ] 实现汇总卡片：总楼层数、总面积、已出租房间、空闲房间。
- [ ] 实现楼层表格。
- [ ] 实现展开房间列表。
- [ ] 实现新增楼层表单。
- [ ] 实现编辑楼层表单。
- [ ] 实现删除楼层确认。
- [ ] 实现同步建筑楼层。
- [ ] 实现权限按钮控制。
- [ ] 适配 BladeX/Saber 的请求封装和响应结构。
- [ ] 不复用 Vue2 Ant Design 组件代码，按 Vue3 + Element Plus / Avue 重写。

## 9. 校验清单

### 9.1 数据完整性校验

迁移完成后执行：

```sql
SELECT COUNT(*) AS floor_count FROM ics_floor;

SELECT f.*
FROM ics_floor f
LEFT JOIN ics_building b ON b.id = f.building_id
WHERE b.id IS NULL;

SELECT f.*
FROM ics_floor f
LEFT JOIN ics_park p ON p.id = f.park_id
WHERE p.id IS NULL;

SELECT building_id, floor_no, COUNT(*) AS duplicate_count
FROM ics_floor
GROUP BY building_id, floor_no
HAVING COUNT(*) > 1;

SELECT f.id, f.park_id AS floor_park_id, b.park_id AS building_park_id
FROM ics_floor f
JOIN ics_building b ON b.id = f.building_id
WHERE f.park_id <> b.park_id;

SELECT f.id, f.building_id, f.floor_no, b.floors
FROM ics_floor f
JOIN ics_building b ON b.id = f.building_id
WHERE f.floor_no < 1 OR f.floor_no > b.floors;

SELECT f.id, f.building_id, f.floor_no, f.area, COALESCE(SUM(r.area), 0) AS room_area
FROM ics_floor f
LEFT JOIN ics_room r ON r.building_id = f.building_id AND r.floor = f.floor_no
GROUP BY f.id, f.building_id, f.floor_no, f.area
HAVING f.area IS NOT NULL AND f.area < room_area;
```

要求：

- [ ] `ics_floor` 数量仍为 16，除非本次验收中明确新增/删除了测试数据。
- [ ] 孤儿建筑为 0。
- [ ] 孤儿园区为 0。
- [ ] 同建筑重复楼层为 0。
- [ ] 楼层与建筑园区不一致为 0。
- [ ] 楼层号超出建筑层数为 0。
- [ ] 楼层面积小于房间面积合计为 0。

### 9.2 功能验收清单

- [ ] 楼层管理菜单可见。
- [ ] 楼层列表能显示 `bladex_boot.ics_floor` 中的 16 条数据。
- [ ] 列表能显示园区名称和建筑名称。
- [ ] 建筑筛选可用。
- [ ] 楼层号筛选可用。
- [ ] 房间状态筛选可用或明确延后。
- [ ] 汇总卡片数据正确。
- [ ] 展开楼层能看到该楼层房间。
- [ ] 新增楼层可用。
- [ ] 新增重复楼层会被拦截。
- [ ] 新增超过建筑楼层数会被拦截。
- [ ] 编辑楼层可用。
- [ ] 楼层面积小于房间面积合计会被拦截。
- [ ] 删除无房间楼层可用。
- [ ] 删除有房间楼层会被拦截。
- [ ] 同步单建筑楼层可用。
- [ ] 同步全部建筑楼层可用或明确延后。
- [ ] 权限按钮生效。
- [ ] 操作日志可记录新增、修改、删除、同步。

### 9.3 关联模块回归清单

- [ ] 园区档案列表和详情不受影响。
- [ ] 建筑管理列表和详情不受影响。
- [ ] 建筑新增/修改触发的楼层同步仍可用。
- [ ] 房源数据不被楼层删除误删。
- [ ] 房源列表仍能根据建筑和楼层查询。
- [ ] 楼层看板后续接入所需字段仍可从楼层/房间数据获得。
- [ ] 合同中已有房间关联不受楼层操作影响。

## 10. 风险与处理

| 风险 | 影响 | 处理方式 |
| --- | --- | --- |
| 房间表没有 `floor_id` | 删除/统计容易漏判断 | 所有校验统一使用 `building_id + floor_no` |
| 楼层同步误删有房间楼层 | 房间数据失去楼层承载 | 同步删除前必须查 `ics_room` 数量 |
| 楼层面积低于房间面积 | 面积统计异常 | 保存楼层前查 `SUM(ics_room.area)` |
| 建筑楼层数与楼层记录不一致 | 列表/看板异常 | 提供同步建筑楼层能力 |
| 楼层与建筑园区不一致 | 数据权限和统计错误 | 保存时由建筑反填 `park_id` |
| 房间状态操作混入楼层管理 | 迁移边界扩大 | 第一阶段只读展示房间，状态流转放到房源管理 |
| Vue2 页面直接复制 | Saber/Vue3 无法运行 | 只迁移交互与字段，按 Vue3 + Element Plus / Avue 重写 |

## 11. 建议执行顺序

1. 确认园区档案和建筑管理接口可用。
2. 建立楼层管理菜单和按钮权限。
3. 实现后端 `Floor` 实体、Mapper、Service、Controller。
4. 先完成列表和详情。
5. 实现新增、修改和面积校验。
6. 实现删除保护。
7. 实现同步建筑楼层。
8. 实现前端列表、查询、汇总卡片和展开房间只读展示。
9. 跑 SQL 数据校验。
10. 跑页面功能验收。
11. 跑关联模块回归清单。

