# BladeX 租控管理迁移清单

本文档用于迁移 RuoYi 中的“租控管理”二级菜单到 BladeX。租控管理不是单纯的园区档案，也不是单一 CRUD 页面，它由楼层看板、房态管理、房源维护、合同占用、工单入口和租客分析组成。

## 1. 迁移边界

### 1.1 菜单定位

RuoYi 当前入口：

- 工作台快捷入口：`ruoyi-ui/src/views/dashboard/Analysis.vue`
  - 标题：`租控管理`
  - 路由：`/parkManage/floorBoard`
- 菜单 SQL：
  - `sql/park_floor_board_menu.sql`
  - `sql/park_management.sql`
  - `sql/biz_floor_menu.sql`
- 页面：
  - `ruoyi-ui/src/views/business/FloorBoard.vue`

BladeX 目标建议：

```text
园区资产
  -> 园区档案
  -> 楼栋管理
  -> 楼层管理
  -> 租控管理
```

如果“楼层管理”和“房源管理”尚未迁完，租控管理先迁只读看板；新增房间、编辑房间、状态流转、工单上报等操作后置。

### 1.2 本次迁移包含

- 租控看板页面
- 园区/楼栋树筛选
- 房号/建筑搜索
- 项目概况指标
- 房态管理视图
- 房源详情弹窗
- 房间新增、编辑、删除
- 房间状态流转
- 房间小程序同步标记
- 租客行业/标签分析
- 租赁面积排行
- 租金单价排行
- 工单记录页签的入口和占位

### 1.3 本次迁移不直接完成

这些功能只做接口预留或弱依赖，不在租控第一阶段深迁：

- 合同全量迁移
- 退租管理
- 客户管理
- 工单管理全流程
- 小程序真实同步
- 智能硬件
- 资产记录
- 租控数据大屏

## 2. 前置条件

租控管理依赖资产底座，建议至少完成以下模块后再迁移完整租控：

- 园区档案：`ics_park`
- 楼栋管理：`ics_building`
- 楼层管理：`ics_floor`
- 房源管理：`ics_room`

当前已完成数据库导入：

| 表 | `bladex_boot` 记录数 |
| --- | ---: |
| `ics_park` | 2 |
| `ics_building` | 5 |
| `ics_floor` | 16 |
| `ics_room` | 2 |

前置数据校验必须全部为 0：

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
SELECT 'room_orphan_park', COUNT(*)
FROM ics_room r
LEFT JOIN ics_park p ON p.id = r.park_id
WHERE r.park_id IS NOT NULL AND p.id IS NULL
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

## 3. 源代码清单

### 3.1 后端源文件

- `ruoyi-business/src/main/java/com/ruoyi/business/controller/RoomController.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/domain/Room.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/domain/Floor.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/domain/Building.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/domain/Park.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/service/IRoomService.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/service/impl/RoomServiceImpl.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/service/impl/FloorServiceImpl.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/mapper/RoomMapper.java`
- `ruoyi-business/src/main/resources/mapper/business/RoomMapper.xml`
- `ruoyi-business/src/main/resources/mapper/business/FloorMapper.xml`
- `ruoyi-business/src/main/resources/mapper/business/BuildingMapper.xml`
- `ruoyi-business/src/main/resources/mapper/business/ParkMapper.xml`

### 3.2 前端源文件

- `ruoyi-ui/src/views/business/FloorBoard.vue`
- `ruoyi-ui/src/views/business/RoomList.vue`
- `ruoyi-ui/src/views/business/modules/RoomModal.vue`
- `ruoyi-ui/src/api/business/room.js`
- `ruoyi-ui/src/api/business/building.js`
- `ruoyi-ui/src/api/business/floor.js`
- `ruoyi-ui/src/api/business/park.js`
- `ruoyi-ui/src/views/dashboard/Analysis.vue`

### 3.3 SQL 源文件

- `sql/park_floor_board_menu.sql`
- `sql/park_management.sql`
- `sql/biz_floor_menu.sql`
- `sql/hide_room_management_menu_incremental.sql`
- `sql/room_occupancy_incremental.sql`
- `sql/contract_management.sql`
- `sql/enterprise_service_backend_incremental.sql`

## 4. 目标落点

BladeX 整体仓库：

- `/Users/jiakangli/Desktop/bladex-park`

后端建议落点：

- `Boot` 或 `BladeX-Boot` 中现有启动工程，按当前项目实际启用目录确认。
- 包名建议：`org.springblade.modules.ics`
- 控制器：`org.springblade.modules.ics.controller.RentControlController`
- 房源控制器：可复用 `RoomController`，也可把租控聚合接口放到 `RentControlController`
- 服务：
  - `IRentControlService`
  - `RentControlServiceImpl`
  - `IRoomService`
  - `IFloorService`
- Mapper：
  - `RoomMapper`
  - `FloorMapper`
  - `BuildingMapper`
  - `ParkMapper`

前端建议落点：

- API：`saber3/src/api/ics/rent-control.js`
- 页面：`saber3/src/views/ics/rent-control/index.vue`
- 房源弹窗组件：`saber3/src/views/ics/rent-control/components/RoomForm.vue`
- 房源详情组件：`saber3/src/views/ics/rent-control/components/RoomDetail.vue`
- 工单占位组件：`saber3/src/views/ics/rent-control/components/WorkorderPanel.vue`

## 5. API 清单

### 5.1 租控看板

RuoYi 源接口：

```text
GET /business/room/floorBoard
```

参数：

| 参数 | 类型 | 说明 |
| --- | --- | --- |
| `parkId` | Long | 园区 ID |
| `buildingId` | Long | 楼栋 ID |
| `keyword` | String | 房号或建筑名称 |
| `searchType` | String | `room` 或 `building` |
| `status` | String | 房源状态 |
| `orientation` | String | 朝向 |

BladeX 建议接口：

```text
GET /blade-ics/rent-control/board
```

返回结构建议：

```json
{
  "currentPark": {},
  "currentBuilding": {},
  "parks": [],
  "buildings": [],
  "overview": {},
  "analysis": {},
  "floors": [],
  "summary": {}
}
```

### 5.2 房源列表

```text
GET /blade-ics/room/page
GET /blade-ics/room/detail
POST /blade-ics/room/submit
POST /blade-ics/room/remove
POST /blade-ics/room/change-status
POST /blade-ics/room/sync-mini
```

### 5.3 依赖接口

```text
GET /blade-ics/park/list
GET /blade-ics/building/list
GET /blade-ics/floor/list
```

工单接口第一阶段可占位：

```text
GET /blade-ics/rent-control/workorders
POST /blade-ics/rent-control/workorders/report
```

如果物业工单还未迁移，这两个接口可以返回空列表和明确提示，不阻塞租控主链路。

## 6. 数据流

### 6.1 看板加载

```text
进入租控管理
  -> 查询园区列表
  -> 查询楼栋列表
  -> 查询楼层列表
  -> 查询房源列表
  -> 按 building_id + floor 分组
  -> 汇总概况指标
  -> 汇总状态数量
  -> 补充租客行业/标签分析
  -> 返回页面渲染
```

### 6.2 树筛选

```text
点击园区节点
  -> 设置 parkId
  -> 清空 buildingId
  -> 重新加载租控看板

点击楼栋节点
  -> 设置 parkId
  -> 设置 buildingId
  -> 重新加载租控看板

点击楼层节点
  -> 设置 buildingId
  -> 设置 floorNo
  -> 激活房态管理页签
```

### 6.3 新增房间

```text
选择楼栋
  -> 查询该楼栋楼层
  -> 选择楼层
  -> 校验楼层范围
  -> 校验房源面积 <= 楼层剩余面积
  -> 根据楼栋反填 park_id
  -> 保存 ics_room
  -> sync_status = 0
  -> 刷新租控看板
```

### 6.4 状态流转

```text
点击房间状态流转
  -> 更新 ics_room.status
  -> sync_status = 0
  -> 记录 update_user/update_time
  -> 刷新看板 summary 和楼层房态
```

## 7. 指标口径

### 7.1 概况指标

| 指标 | 来源 | 口径 |
| --- | --- | --- |
| 在租房间数 | `ics_room` | `status = '1'` |
| 房源数量 | `ics_room` | 当前筛选范围总数 |
| 管理面积 | `ics_floor.area` | 当前筛选范围楼层面积合计 |
| 楼层面积 | `ics_floor.area` | 同管理面积 |
| 在租实时均价 | `ics_room.rent_price` | 当前实现为房源租金平均值，后续应接合同单价 |
| 在租面积 | `ics_room.area` | `status = '1'` 的房源面积 |
| 出租率 | `ics_room` | `已租房间数 / 房源总数 * 100` |
| 待租面积 | `ics_room.area` | `status = '0'` 的房源面积 |

### 7.2 房源状态

| 状态值 | RuoYi 文案 | 租控文案建议 |
| --- | --- | --- |
| `0` | 空闲 | 空置中/招商中 |
| `1` | 已租 | 已出租 |
| `2` | 已预定 | 已锁定/已预定 |
| `3` | 装修中 | 装修中 |
| `4` | 停用 | 停用 |

注意：RuoYi 页面图例中有 `90日内到期`、`30日内到期`、`已到期`，这依赖合同到期数据。合同模块未迁移前，不做真实到期状态覆盖。

### 7.3 租客行业/标签

RuoYi 源逻辑依赖：

- `biz_contract`
- `biz_customer`
- `biz_customer_tag`
- `biz_tag`
- `biz_tag_type`

第一阶段处理建议：

- 如果合同/客户/标签表未迁入 BladeX，则返回空数组。
- 页面显示“暂无行业标签数据”“暂无客户标签数据”。
- 不阻塞房态主功能。

## 8. 数据关联

| 模块 | 表/字段 | 依赖程度 | 处理策略 |
| --- | --- | --- | --- |
| 园区档案 | `ics_park.id` | 强依赖 | 必须已迁 |
| 楼栋管理 | `ics_building.id/park_id/floors` | 强依赖 | 必须已迁 |
| 楼层管理 | `ics_floor.building_id/floor_no/area` | 强依赖 | 必须已迁 |
| 房源管理 | `ics_room` | 强依赖 | 本模块核心 |
| 合同管理 | `biz_contract.room_id/room_ids/building_id/building_ids` | 中依赖 | 第一阶段只读统计或空数据 |
| 客户管理 | `biz_customer`、标签表 | 中依赖 | 第一阶段可空 |
| 工单管理 | `biz_service_workorder.room_ids` | 中依赖 | 第一阶段占位 |
| 小程序同步 | `sync_status/sync_time` | 弱依赖 | 先只做标记，不做外部推送 |
| 智能硬件 | 未明确表 | 弱依赖 | 占位，不迁 |

## 9. 权限和菜单

### 9.1 菜单建议

```text
园区资产
  -> 租控管理
```

BladeX 菜单字段建议：

| 字段 | 建议 |
| --- | --- |
| 菜单名称 | 租控管理 |
| 路由 | `/ics/rent-control` |
| 组件 | `ics/rent-control/index` |
| 权限编码 | `park:rent-control:view` |
| 图标 | `House`、`OfficeBuilding` 或项目现有资源图标 |

### 9.2 按钮权限

| 功能 | 权限编码 |
| --- | --- |
| 看板查询 | `park:rent-control:list` |
| 房源详情 | `park:rent-control:detail` |
| 新增房间 | `park:rent-control:room-add` |
| 编辑房间 | `park:rent-control:room-edit` |
| 删除房间 | `park:rent-control:room-delete` |
| 状态流转 | `park:rent-control:room-status` |
| 同步小程序 | `park:rent-control:room-sync-mini` |
| 工单刷新 | `park:rent-control:workorder-list` |
| 上报工单 | `park:rent-control:workorder-report` |

如果房源管理已经有独立权限，也可以复用：

- `park:room:add`
- `park:room:edit`
- `park:room:delete`
- `park:room:status`

## 10. 分阶段迁移计划

### 阶段 A：只读租控看板

目标：页面能打开，能展示园区、楼栋、楼层、房源状态。

后端任务：

- [ ] 新增 `RentControlController`
- [ ] 新增 `RentControlService`
- [ ] 实现 `/blade-ics/rent-control/board`
- [ ] 查询园区、楼栋、楼层、房源
- [ ] 组装 `parks/buildings/floors/overview/summary`
- [ ] 合同/客户分析先返回空数组

前端任务：

- [ ] 新增 `saber3/src/api/ics/rent-control.js`
- [ ] 新增 `saber3/src/views/ics/rent-control/index.vue`
- [ ] 实现左侧园区楼栋树
- [ ] 实现项目概况卡片
- [ ] 实现房态网格
- [ ] 实现状态筛选和关键字搜索
- [ ] 接入 BladeX 菜单

验收：

- [ ] 租控管理菜单可见。
- [ ] 页面可打开。
- [ ] 能看到 `bladex_boot.ics_park` 的 2 个园区。
- [ ] 能看到楼栋和楼层。
- [ ] 能看到 `ics_room` 的 2 条房源。
- [ ] 切换园区/楼栋后数据正确过滤。

### 阶段 B：房态管理操作

目标：能在租控页面维护房源。

后端任务：

- [ ] 实现房源详情接口
- [ ] 实现房源新增接口
- [ ] 实现房源编辑接口
- [ ] 实现房源删除接口
- [ ] 实现状态流转接口
- [ ] 实现同步小程序标记接口
- [ ] 校验楼栋存在
- [ ] 根据楼栋反填园区
- [ ] 校验楼层范围
- [ ] 校验楼层面积容量
- [ ] 删除前校验合同/工单引用

前端任务：

- [ ] 新增/编辑房源弹窗
- [ ] 房源详情弹窗
- [ ] 状态流转菜单
- [ ] 删除确认
- [ ] 小程序同步按钮
- [ ] 权限按钮控制

验收：

- [ ] 新增房间后看板刷新。
- [ ] 编辑房间后字段回显正确。
- [ ] 状态流转后颜色和统计同步变化。
- [ ] 房间面积超楼层剩余面积时被拦截。
- [ ] 删除被合同/工单引用的房源时被拦截。

### 阶段 C：合同/客户/工单联动

目标：补齐租控管理里的经营分析和服务入口。

任务：

- [ ] 合同到期状态覆盖房态图例。
- [ ] 租客行业分析接入真实合同和客户标签。
- [ ] 租客标签分析接入真实客户标签。
- [ ] 租赁面积排行接入真实合同面积。
- [ ] 租金单价排行接入合同租金口径。
- [ ] 工单记录接入物业工单。
- [ ] 上报工单接入工单模块。

验收：

- [ ] 90 日内到期、30 日内到期、已到期能正确展示。
- [ ] 已租房源能显示租客相关分析。
- [ ] 工单记录按当前园区/楼栋/房源过滤。

## 11. 后端迁移细节

### 11.1 查询组装规则

租控看板建议后端一次聚合返回，不要前端并发打多个基础接口后自己拼。

原因：

- 数据权限应在后端统一处理。
- 房态统计口径应统一。
- 合同/客户/标签分析后续会越来越复杂。

### 11.2 房源与楼层关联

当前 `ics_room` 不存 `floor_id`，只存：

- `building_id`
- `floor`

所以所有楼层关联必须使用：

```sql
r.building_id = f.building_id
AND r.floor = f.floor_no
```

不要在迁移中新增 `floor_id` 并强行改模型，除非后续单独做数据模型升级。

### 11.3 删除房源校验

删除房源前至少检查：

```sql
SELECT COUNT(*)
FROM biz_contract c
WHERE c.del_flag = '0'
  AND (
    c.room_id = #{roomId}
    OR FIND_IN_SET(#{roomId}, REPLACE(c.room_ids, 'room_', ''))
  );
```

如果工单表已迁移，再检查：

```sql
SELECT COUNT(*)
FROM biz_service_workorder w
WHERE FIND_IN_SET(#{roomId}, REPLACE(w.room_ids, 'room_', ''));
```

### 11.4 面积容量校验

新增或编辑房间时：

```text
目标楼层面积 >= 当前楼层其他房间面积合计 + 本次房间面积
```

编辑时要排除当前房间原面积。

## 12. 前端迁移细节

### 12.1 页面形态

BladeX/Saber 前端是 Vue 3 + Element Plus + Avue，不能直接复制 RuoYi Vue2 + Ant Design Vue 页面。

迁移方式：

- 保留页面信息架构。
- 保留数据字段和交互逻辑。
- 用 Element Plus 重写布局和组件。
- 图标使用 Element Plus 图标或项目已有图标系统。
- 不要把 RuoYi 的 `a-*` 组件直接搬过来。

### 12.2 页面结构建议

```text
rent-control/index.vue
  左侧：园区/楼栋树 + 搜索
  顶部：当前园区/楼栋标题
  Tabs：
    项目概况
    房态管理
    工单记录
    资产记录
    智能硬件
    楼宇信息
```

第一阶段可只实现：

- 项目概况
- 房态管理

其他 Tab 保留轻量空状态，不接入复杂模块。

### 12.3 房态颜色

建议建立统一状态映射：

```js
const roomStatusMap = {
  "0": { label: "空置中", type: "success" },
  "1": { label: "已出租", type: "primary" },
  "2": { label: "已预定", type: "warning" },
  "3": { label: "装修中", type: "info" },
  "4": { label: "停用", type: "danger" }
}
```

合同到期类状态后续作为覆盖状态：

- `expiring90`
- `expiring30`
- `expired`

## 13. 数据校验清单

### 13.1 租控基础数据

```sql
SELECT 'park' AS item, COUNT(*) AS cnt FROM ics_park
UNION ALL SELECT 'building', COUNT(*) FROM ics_building
UNION ALL SELECT 'floor', COUNT(*) FROM ics_floor
UNION ALL SELECT 'room', COUNT(*) FROM ics_room;
```

### 13.2 房态统计

```sql
SELECT park_id, status, COUNT(*) AS room_count, COALESCE(SUM(area), 0) AS area_sum
FROM ics_room
GROUP BY park_id, status
ORDER BY park_id, status;
```

### 13.3 楼层面积容量

```sql
SELECT f.id, f.building_id, f.floor_no, f.area AS floor_area,
       COALESCE(SUM(r.area), 0) AS room_area
FROM ics_floor f
LEFT JOIN ics_room r
  ON r.building_id = f.building_id
 AND r.floor = f.floor_no
GROUP BY f.id, f.building_id, f.floor_no, f.area
HAVING f.area IS NOT NULL AND f.area < room_area;
```

### 13.4 房间楼层合法性

```sql
SELECT r.id, r.name, r.building_id, r.floor, b.floors
FROM ics_room r
JOIN ics_building b ON b.id = r.building_id
WHERE r.floor IS NULL OR r.floor < 1 OR r.floor > b.floors;
```

## 14. 功能验收清单

### 14.1 页面验收

- [ ] 租控管理菜单可见。
- [ ] 页面可正常打开。
- [ ] 左侧园区/楼栋树正常显示。
- [ ] 搜索房号可用。
- [ ] 搜索建筑可用。
- [ ] 项目概况指标显示正常。
- [ ] 房态网格显示正常。
- [ ] 状态筛选可用。
- [ ] 重置可用。
- [ ] 空状态显示自然。

### 14.2 接口验收

- [ ] 看板接口能按 `parkId` 过滤。
- [ ] 看板接口能按 `buildingId` 过滤。
- [ ] 看板接口能按房号搜索。
- [ ] 看板接口能按建筑搜索。
- [ ] 看板接口能按状态过滤。
- [ ] 房源新增接口可用。
- [ ] 房源编辑接口可用。
- [ ] 房源删除接口可用。
- [ ] 状态流转接口可用。
- [ ] 小程序同步标记接口可用。

### 14.3 权限验收

- [ ] 无权限用户看不到租控菜单。
- [ ] 只有查询权限时不能新增/编辑/删除。
- [ ] 状态流转按钮受权限控制。
- [ ] 小程序同步按钮受权限控制。
- [ ] 普通用户只能看到授权园区。

### 14.4 回归验收

- [ ] 不影响园区档案。
- [ ] 不影响楼栋管理。
- [ ] 不影响楼层管理。
- [ ] 不影响房源基础数据。
- [ ] 不破坏 `ics_room` 与 `ics_building` 的归属关系。
- [ ] 不破坏 `ics_floor` 与 `ics_building` 的归属关系。

## 15. 风险清单

| 风险 | 影响 | 处理 |
| --- | --- | --- |
| 租控页面依赖合同/客户/工单 | 迁移范围膨胀 | 第一阶段只做资产主链路，联动模块空状态 |
| RuoYi 页面是 Vue2 Ant Design | 不能直接复制到 Saber | 用 Vue3 + Element Plus/Avue 重写 |
| 房源状态和合同到期状态混用 | 房态颜色错乱 | 先用房源状态，合同到期后续覆盖 |
| 房源不存 `floor_id` | 容易关联错误 | 坚持 `building_id + floor` 关联 |
| 面积口径不一致 | 出租率/管理面积不准 | 在后端统一聚合 |
| 删除房源破坏合同/工单 | 历史业务断链 | 删除前查合同和工单引用 |
| 租客分析依赖标签表 | 看板空数据 | 第一阶段返回空数组并显示空状态 |

## 16. 推荐执行顺序

1. 确认园区档案、楼栋管理、楼层管理、房源基础接口是否已迁移。
2. 若未迁房源基础接口，先迁 `RoomController/RoomService/RoomMapper` 的基础 CRUD。
3. 新增租控看板只读接口 `/blade-ics/rent-control/board`。
4. 新增 Saber 租控管理页面，只实现项目概况和房态管理。
5. 接入新增/编辑房源弹窗。
6. 接入状态流转和删除校验。
7. 接入菜单和权限。
8. 跑数据校验 SQL。
9. 做页面验收。
10. 再计划合同/客户/工单联动阶段。

