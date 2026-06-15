# BladeX 园区资产模块迁移清单

本文档用于把 RuoYi 项目中的园区、楼栋、楼层、房间四个模块迁移到 BladeX。迁移目标不是一次性搬完整个项目，而是用清晰边界拆成可并行 Work Tree，每个模块独立迁移、独立验收、最后集成。

## 1. 当前状态

### 1.1 已确认的运行环境

- RuoYi 源工程：`/Users/jiakangli/桌面/ics-park`
- 当前分支：`dev`
- BladeX 工程用户口径目录：`/Users/jiakangli/Desktop/产业园文件`
- BladeX 工程终端实际目录：`/Users/jiakangli/Desktop/bladex-park`
- BladeX 后端工程：`/Users/jiakangli/Desktop/bladex-park/BladeX-Boot`
- BladeX 前端工程：`/Users/jiakangli/Desktop/bladex-park/saber3`
- BladeX 版本：`4.9.0.RELEASE`
- BladeX 后端技术基线：Java 17、BladeX-Boot 单体工程
- BladeX 前端技术基线：Vue 3、Vite、Element Plus、Avue
- RuoYi 数据库：`ry_ics`
- BladeX 数据库：`bladex_boot`
- BladeX 数据库现状：129 张表，初始化用户、客户端数据已导入
- Redis：已连通
- BladeX 后端：已启动，端口 `8080`，PID `45021`
- BladeX 前端 Vite：已启动，PID `45953`
- 本地 80 端口被 Docker 占用，已同步前端代理与 SM2/crypto 配置

### 1.2 待确认项

- 用户口径路径 `/Users/jiakangli/Desktop/产业园文件` 与终端实际可访问路径 `/Users/jiakangli/Desktop/bladex-park` 不一致，后续脚本和 Work Tree 命令统一使用终端实际路径。
- BladeX 前端 `saber3` 当前已有本地配置改动：`src/config/website.js`、`src/utils/crypto.js`、`vite.config.mjs`。迁移分支创建前先确认这些改动是否提交或保留在基线分支。
- 园区授权来源仍需确认：扩展业务关系表、使用 BladeX 数据权限，或先用服务层注入 `parkIds`。

### 1.3 当前源模块范围

后端源文件：

- `ruoyi-business/src/main/java/com/ruoyi/business/controller/ParkController.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/controller/BuildingController.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/controller/FloorController.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/controller/RoomController.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/domain/Park.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/domain/Building.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/domain/Floor.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/domain/Room.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/mapper/ParkMapper.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/mapper/BuildingMapper.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/mapper/FloorMapper.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/mapper/RoomMapper.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/service/IParkService.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/service/IBuildingService.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/service/IFloorService.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/service/IRoomService.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/service/impl/ParkServiceImpl.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/service/impl/BuildingServiceImpl.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/service/impl/FloorServiceImpl.java`
- `ruoyi-business/src/main/java/com/ruoyi/business/service/impl/RoomServiceImpl.java`
- `ruoyi-business/src/main/resources/mapper/business/ParkMapper.xml`
- `ruoyi-business/src/main/resources/mapper/business/BuildingMapper.xml`
- `ruoyi-business/src/main/resources/mapper/business/FloorMapper.xml`
- `ruoyi-business/src/main/resources/mapper/business/RoomMapper.xml`

前端源文件：

- `ruoyi-ui/src/api/business/park.js`
- `ruoyi-ui/src/api/business/building.js`
- `ruoyi-ui/src/api/business/floor.js`
- `ruoyi-ui/src/api/business/room.js`
- `ruoyi-ui/src/api/admin/park.js`
- `ruoyi-ui/src/views/business/ParkList.vue`
- `ruoyi-ui/src/views/business/ParkEdit.vue`
- `ruoyi-ui/src/views/business/BuildingList.vue`
- `ruoyi-ui/src/views/business/FloorList.vue`
- `ruoyi-ui/src/views/business/FloorBoard.vue`
- `ruoyi-ui/src/views/business/RoomList.vue`
- `ruoyi-ui/src/views/business/modules/ParkModal.vue`
- `ruoyi-ui/src/views/business/modules/BuildingModal.vue`
- `ruoyi-ui/src/views/business/modules/RoomModal.vue`

### 1.4 当前数据规模和数据质量

当前 `ry_ics` 数据：

| 表 | 记录数 | 说明 |
| --- | ---: | --- |
| `ics_park` | 2 | 园区 |
| `ics_building` | 5 | 楼栋 |
| `ics_floor` | 16 | 楼层 |
| `ics_room` | 3 | 房间 |

迁移前已发现的问题：

- `room_orphan_building = 1`：有 1 条 `ics_room.building_id` 找不到对应 `ics_building.id`。
- 这个问题必须在迁移前修复，不能带入 BladeX。

迁移前置校验 SQL：

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
WHERE r.building_id IS NOT NULL AND b.id IS NULL;
```

定位脏数据 SQL：

```sql
SELECT r.*
FROM ics_room r
LEFT JOIN ics_building b ON b.id = r.building_id
WHERE r.building_id IS NOT NULL AND b.id IS NULL;
```

## 2. 迁移总体原则

### 2.1 优先保留业务表

第一阶段建议把 `ics_park`、`ics_building`、`ics_floor`、`ics_room` 直接迁入 `bladex_boot`，字段尽量保持一致。原因：

- 当前业务表已经被合同、审批、物业、客户、工单等模块引用。
- 保留表结构可以减少跨模块连锁改造。
- BladeX 基础能力先接管权限、菜单、审计、分页、租户；业务数据模型第二阶段再优化。

### 2.2 统一 BladeX 编码风格

迁移后的代码不要继续依赖 RuoYi 基类和工具：

| RuoYi 依赖 | BladeX 迁移方向 |
| --- | --- |
| `BaseController` | BladeX 控制器基类或普通 `@RestController` |
| `R`、`AjaxResult` | BladeX `R` 响应对象 |
| `startPage()`、`TableDataInfo` | BladeX / MyBatis-Plus 分页 |
| `IBaseServiceImpl`、`tk.mybatis` | BladeX Service + MyBatis-Plus |
| `@Excel`、`ExcelUtil` | BladeX 导入导出能力，或先保留独立 Excel 工具 |
| `getLoginName()` | BladeX 当前用户上下文 |
| `@HasPermissions` | BladeX 权限注解/权限编码 |
| `@DataScope` | BladeX 数据权限或自定义园区数据权限 |

### 2.3 接口先稳定，页面再替换

短期目标建议保持接口语义基本不变，便于前端迁移：

- 列表接口保留查询条件语义。
- 新增/修改/删除接口保留业务校验。
- 分页返回改成 BladeX 标准，但前端统一做一层适配。
- 权限编码按 BladeX 菜单按钮规则重新定义。

### 2.4 并行迁移但串行集成

四个模块可以并行做，但集成顺序必须遵循依赖：

```text
园区 Park
  -> 楼栋 Building
      -> 楼层 Floor
          -> 房间 Room
              -> 楼层看板 FloorBoard
              -> 合同/物业/工单/审批等关联模块
```

## 3. 数据模型清单

### 3.1 `ics_park`

关键字段：

- `id`：主键，自增
- `name`：园区名称，必填
- `code`：园区编码
- `address`、`region`、`detail_address`：地址信息
- `area`：占地面积
- `total_rooms`、`parking_fee`
- `latitude`、`longitude`
- `rent_min`、`rent_max`、`rent_unit`
- `thumbnail_url`、`banner_url`
- `summary`、`supporting_services`、`traffic_info`、`content`
- `contact_name`、`contact_phone`
- `status`：`0` 启用，`1` 停用
- `memo`
- `create_by`、`create_time`、`update_by`、`update_time`

迁移决策：

- `park_id` 字段当前仍存在但注释为租户园区 ID。前期不再使用它作为主键或业务外键。
- 其他业务表统一引用 `ics_park.id`。
- 如启用 BladeX 多租户，新增 `tenant_id` 需另做脚本，不能混用 `park_id`。

### 3.2 `ics_building`

关键字段：

- `id`：主键，自增
- `park_id`：所属园区，引用 `ics_park.id`
- `name`：楼栋名称，必填
- `code`：建筑编码，当前业务校验必填
- `address`、`region`
- `real_estate_no`、`property_no`、`land_no`
- `sort_num`
- `floors`：楼层数，必须大于 0
- `area`：建筑面积
- `property_area`、`rentable_area`、`self_use_area`、`supporting_area`、`parking_area`
- `standard_floor_height`
- `building_type`
- `status`：`0` 启用，`1` 停用
- `memo`
- `create_by`、`create_time`、`update_by`、`update_time`

约束：

- 已存在唯一索引 `uk_park_building_name (park_id, name)`。
- 新增/修改楼栋后会自动同步楼层。
- 楼栋楼层数不能小于已有房间最大楼层。

### 3.3 `ics_floor`

关键字段：

- `id`：楼层 ID，自增
- `park_id`：园区 ID，引用 `ics_park.id`
- `building_id`：建筑 ID，引用 `ics_building.id`
- `floor_no`：楼层号
- `area`：楼层面积
- `status`：`0` 启用，`1` 停用
- `memo`
- `create_by`、`create_time`、`update_by`、`update_time`

约束：

- 已存在唯一索引 `uk_building_floor (building_id, floor_no)`。
- `floor_no` 必须在 `1..building.floors` 范围内。
- 楼层面积不能小于该楼层房间面积合计。
- 楼层已有房源时不能删除。

### 3.4 `ics_room`

关键字段：

- `id`：主键，自增
- `park_id`：所属园区，引用 `ics_park.id`
- `building_id`：所属楼栋，引用 `ics_building.id`
- `name`：房间名称
- `floor`：楼层号
- `area`：面积
- `rent_price`：租金
- `property_fee`：物业费
- `house_type`、`orientation`
- `address`
- `facilities`、`highlights`
- `scene_images`、`floor_plan_images`
- `status`：`0` 空闲，`1` 已租，`2` 预定，`3` 装修，`4` 停用
- `sync_status`：`0` 待同步，`1` 已同步
- `sync_time`
- `memo`
- `create_by`、`create_time`、`update_by`、`update_time`

约束：

- `building_id` 必须存在。
- `park_id` 应从楼栋反填，不能由前端随意提交。
- `floor` 必须存在于楼栋楼层范围内。
- 房间面积合计不能超过楼层面积。
- 房间状态变更后需将 `sync_status` 置为待同步。

## 4. 数据流和关联关系

### 4.1 主数据流

```text
用户登录 BladeX
  -> 获取菜单与按钮权限
  -> 进入园区资产菜单
  -> 查询园区列表
  -> 选择园区
  -> 查询楼栋列表
  -> 选择楼栋
  -> 查询楼层列表/楼层看板
  -> 查询房间列表
  -> 房间状态、图片、租金、面积等维护
```

### 4.2 新增楼栋数据流

```text
前端提交楼栋
  -> 校验园区存在
  -> 校验建筑编码、名称、地区、产权性质、排序值、楼层数、面积
  -> 校验同园区楼栋名称唯一
  -> 写入 ics_building
  -> 根据 floors 自动生成 ics_floor
  -> 应用前端传入的 floorAreas 到楼层面积
  -> 返回 BladeX 标准响应
```

### 4.3 修改楼栋数据流

```text
前端提交楼栋修改
  -> 校验楼栋存在
  -> 校验园区存在
  -> 校验已有房间最大楼层 <= 新楼层数
  -> 更新 ics_building
  -> 同步楼层：补齐缺失楼层，删除超过楼层数且无房间的楼层
  -> 更新楼层面积
```

### 4.4 新增/修改房间数据流

```text
前端提交房间
  -> 校验楼栋存在
  -> 根据楼栋反填 park_id
  -> 校验楼层存在或在楼栋范围内
  -> 校验楼层面积容量
  -> 写入/更新 ics_room
  -> sync_status 置为 0
```

### 4.5 楼层看板数据流

```text
前端请求 floorBoard
  -> 查询园区列表
  -> 查询楼栋列表
  -> 查询楼层列表
  -> 查询房间列表
  -> 按楼栋 + 楼层聚合房间
  -> 统计空闲/已租/预定/装修/停用
  -> 聚合面积、租金、出租率、行业/标签分析
  -> 返回看板结构
```

### 4.6 关联模块清单

四个模块不是孤立模块，迁移时必须记录这些外部依赖：

| 关联模块 | 关联字段/接口 | 影响 |
| --- | --- | --- |
| 合同管理 | `biz_contract.park_id`、`building_id`、`building_ids`、`room_id`、`room_ids` | 房间删除、楼栋统计、楼层看板租户分析 |
| 物业/工单 | `biz_service_workorder.park_id`、`room_ids` | 楼层看板发起报修、房源选择树 |
| 客户管理 | `biz_customer`、`biz_customer_tag`、`biz_tag` | 楼层看板租户/行业分析 |
| 审批管理 | `biz_approval_* .park_id` | 园区维度审批流 |
| 政策/党建/banner | `ics_policy.park_id`、`ics_dj.park_id` 等 | 园区下内容展示 |
| 用户/部门数据权限 | RuoYi `sys_user.park_id`、`sys_role.park_id` 或 BladeX 数据权限 | 非管理员数据隔离 |
| 文件上传 | 园区图、房源实景图、平面图 | 需从 `/dfs/upload` 迁到 BladeX 附件/OSS |

## 5. BladeX 落地设计

### 5.1 模块命名建议

当前 BladeX 后端是 `BladeX-Boot` 单体工程，建议在现有 `org.springblade.modules` 下新增业务模块：

- 模块名：`ics`
- 后端包名：`org.springblade.modules.ics`
- 后端目录：`/Users/jiakangli/Desktop/bladex-park/BladeX-Boot/src/main/java/org/springblade/modules/ics`
- 子包：
  - `controller`
  - `mapper`
  - `pojo/entity`
  - `pojo/dto`
  - `pojo/vo`
  - `service`
  - `service.impl`
  - `wrapper`
- Mapper XML 目录：`/Users/jiakangli/Desktop/bladex-park/BladeX-Boot/src/main/resources/org/springblade/modules/ics/mapper` 或按现有 MyBatis 扫描配置放置

前端建议在 `saber3` 下新增园区资产目录：

- API 目录：`/Users/jiakangli/Desktop/bladex-park/saber3/src/api/ics`
- 页面目录：`/Users/jiakangli/Desktop/bladex-park/saber3/src/views/ics`
- 配置目录：按 BladeX/Saber 现有菜单动态路由机制写入 `blade_menu`，尽量不写死本地路由。

四个模块仍保持 `park/building/floor/room` 子包或文件前缀边界，避免并行分支互相改同一个大文件。

### 5.2 API 路径建议

建议迁移后使用 BladeX 风格路径：

| 模块 | 新路径建议 | 兼容旧路径 |
| --- | --- | --- |
| 园区 | `/blade-ics/park` | `/business/park` 可临时转发 |
| 楼栋 | `/blade-ics/building` | `/business/building` 可临时转发 |
| 楼层 | `/blade-ics/floor` | `/business/floor` 可临时转发 |
| 房间 | `/blade-ics/room` | `/business/room` 可临时转发 |

当前目标前端是 Vue 3 + Vite 的 `saber3`，不建议直接复用 RuoYi Vue2 页面。应迁移页面交互和字段逻辑，组件实现改为 Element Plus / Avue 风格。

### 5.3 权限编码建议

| 功能 | 权限编码 |
| --- | --- |
| 园区列表 | `park:park:list` |
| 园区详情 | `park:park:detail` |
| 园区新增 | `park:park:add` |
| 园区修改 | `park:park:edit` |
| 园区删除 | `park:park:delete` |
| 园区统计 | `park:park:statistics` |
| 楼栋列表 | `park:building:list` |
| 楼栋详情 | `park:building:detail` |
| 楼栋新增 | `park:building:add` |
| 楼栋修改 | `park:building:edit` |
| 楼栋删除 | `park:building:delete` |
| 楼栋导入 | `park:building:import` |
| 楼栋导出 | `park:building:export` |
| 楼层列表 | `park:floor:list` |
| 楼层详情 | `park:floor:detail` |
| 楼层新增 | `park:floor:add` |
| 楼层修改 | `park:floor:edit` |
| 楼层删除 | `park:floor:delete` |
| 楼层同步 | `park:floor:sync` |
| 房间列表 | `park:room:list` |
| 房间详情 | `park:room:detail` |
| 房间新增 | `park:room:add` |
| 房间修改 | `park:room:edit` |
| 房间删除 | `park:room:delete` |
| 房间状态 | `park:room:status` |
| 房间同步小程序 | `park:room:sync-mini` |
| 楼层看板 | `park:room:floor-board` |

### 5.4 菜单建议

BladeX `blade_menu` 推荐结构：

```text
园区资产
  -> 园区管理
  -> 楼栋管理
  -> 楼层管理
  -> 房间管理
  -> 楼层看板
```

菜单 SQL 需要单独生成，并写入 `blade_menu`、`blade_role_menu`。菜单 ID 不能直接沿用 RuoYi `sys_menu.menu_id`，应使用 BladeX ID 规则。

### 5.5 数据权限建议

第一阶段先实现两级权限：

- 超级管理员：可看全部园区。
- 普通用户：只能看授权园区。

授权园区来源待确认：

- 方案 A：扩展 BladeX 用户/部门与园区关系表，例如 `ics_user_park`、`ics_dept_park`。
- 方案 B：用 BladeX 数据权限 `blade_role_scope` 表达园区范围。
- 方案 C：短期在业务表查询中传入 `parkIds`，由统一拦截器或服务方法注入。

不建议把园区 ID 塞进 `tenant_id`。租户是平台隔离维度，园区是业务资源维度，二者不要混用。

## 6. 分模块并行 Work Tree 方案

### 6.1 分支和目录规划

迁移涉及三个代码仓库/工程角色：

- RuoYi 源工程：`/Users/jiakangli/桌面/ics-park`，只作为迁移参考和比对来源。
- BladeX 后端工程：`/Users/jiakangli/Desktop/bladex-park/BladeX-Boot`，当前分支 `master`。
- BladeX 前端工程：`/Users/jiakangli/Desktop/bladex-park/saber3`，当前分支 `master`。

RuoYi 源工程可选建立只读参考 Work Tree：

```bash
git worktree add ../ics-park-migrate-park -b codex/migrate-park dev
git worktree add ../ics-park-migrate-building -b codex/migrate-building dev
git worktree add ../ics-park-migrate-floor -b codex/migrate-floor dev
git worktree add ../ics-park-migrate-room -b codex/migrate-room dev
```

BladeX 后端建议每个模块独立 Work Tree：

```bash
cd /Users/jiakangli/Desktop/bladex-park/BladeX-Boot
git worktree add ../BladeX-Boot-migrate-park -b codex/migrate-park master
git worktree add ../BladeX-Boot-migrate-building -b codex/migrate-building master
git worktree add ../BladeX-Boot-migrate-floor -b codex/migrate-floor master
git worktree add ../BladeX-Boot-migrate-room -b codex/migrate-room master
```

BladeX 前端也建议每个模块独立 Work Tree，但创建前要先处理当前 `saber3` 的本地配置改动：

```bash
cd /Users/jiakangli/Desktop/bladex-park/saber3
git worktree add ../saber3-migrate-park -b codex/migrate-park master
git worktree add ../saber3-migrate-building -b codex/migrate-building master
git worktree add ../saber3-migrate-floor -b codex/migrate-floor master
git worktree add ../saber3-migrate-room -b codex/migrate-room master
```

如果某个模块只做后端或只做前端，可只创建对应工程的 Work Tree。

### 6.2 并行边界

| Work Tree | 负责人目标 | 可独立完成 | 需要依赖 |
| --- | --- | --- | --- |
| `BladeX-Boot-migrate-park` / `saber3-migrate-park` | 园区主数据 | `ics_park`、园区 CRUD、园区统计、图片上传适配 | BladeX 附件/OSS、菜单 |
| `BladeX-Boot-migrate-building` / `saber3-migrate-building` | 楼栋主数据 | `ics_building`、楼栋 CRUD、导入导出、楼层自动同步契约 | 园区接口契约、楼层服务接口 |
| `BladeX-Boot-migrate-floor` / `saber3-migrate-floor` | 楼层主数据 | `ics_floor`、楼层 CRUD、楼层同步、楼层面积校验 | 楼栋接口契约、房间面积统计接口 |
| `BladeX-Boot-migrate-room` / `saber3-migrate-room` | 房间与看板 | `ics_room`、房间 CRUD、状态切换、楼层看板 | 园区/楼栋/楼层查询契约、合同/客户/工单依赖 |

### 6.3 并行时的契约文件

为避免并行分支冲突，建议先在主分支确定契约：

- 数据库 DDL：`sql/blade_ics_asset_tables.sql`
- 菜单权限 SQL：`sql/blade_ics_asset_menu.sql`
- API 契约：`docs/bladex-park-api-contract.md`
- 通用字典：房间状态、楼栋状态、楼层状态、产权性质、朝向
- 通用 VO：`ParkVO`、`BuildingVO`、`FloorVO`、`RoomVO`

每个 Work Tree 只实现自己的模块，不随意修改其他模块的表结构和接口。

## 7. 模块迁移清单

### 7.1 园区模块 Park

迁移范围：

- 表：`ics_park`
- 后端：园区 CRUD、园区统计
- 前端：园区列表、园区编辑、园区图片上传
- 附件：`thumbnail_url`、`banner_url`

功能清单：

- [ ] 查询园区详情
- [ ] 分页查询园区列表
- [ ] 园区名称、编码、状态筛选
- [ ] 新增园区
- [ ] 修改园区
- [ ] 删除园区
- [ ] 园区统计：管理面积、可租面积、房间总数、可租房间数
- [ ] 园区小图上传
- [ ] 园区 banner 上传
- [ ] 富文本内容保存
- [ ] 状态字典适配
- [ ] 操作日志适配
- [ ] 权限按钮适配

数据流：

```text
BladeX 前端
  -> park list/detail/save/update/remove/statistics
  -> ParkController
  -> ParkService
  -> ParkMapper
  -> ics_park + ics_floor + ics_room 统计
```

关联关系：

- 被 `ics_building.park_id` 引用。
- 被 `ics_floor.park_id` 引用。
- 被 `ics_room.park_id` 引用。
- 被合同、审批、客户、政策、党建、物业服务引用。

迁移验收：

- [ ] `ics_park` 记录数迁移前后一致。
- [ ] 园区列表统计字段与 RuoYi 原接口一致。
- [ ] 新增园区后可被楼栋新增页面选择。
- [ ] 删除园区前必须校验是否存在楼栋、楼层、房间、合同等引用。
- [ ] 普通用户只能看到授权园区。
- [ ] 图片上传后 URL 可访问。

验收 SQL：

```sql
SELECT COUNT(*) FROM ics_park;

SELECT p.id, p.name,
       COALESCE(fs.management_area, 0) AS management_area,
       COALESCE(rs.rentable_area, 0) AS rentable_area,
       COALESCE(rs.total_room_count, 0) AS total_room_count,
       COALESCE(rs.rentable_room_count, 0) AS rentable_room_count
FROM ics_park p
LEFT JOIN (
    SELECT park_id, SUM(COALESCE(area, 0)) AS management_area
    FROM ics_floor
    GROUP BY park_id
) fs ON fs.park_id = p.id
LEFT JOIN (
    SELECT park_id,
           COUNT(1) AS total_room_count,
           SUM(CASE WHEN status = '0' THEN 1 ELSE 0 END) AS rentable_room_count,
           SUM(CASE WHEN status = '0' THEN COALESCE(area, 0) ELSE 0 END) AS rentable_area
    FROM ics_room
    GROUP BY park_id
) rs ON rs.park_id = p.id;
```

### 7.2 楼栋模块 Building

迁移范围：

- 表：`ics_building`
- 后端：楼栋 CRUD、导入、导出、模板、楼层自动同步
- 前端：楼栋列表、楼栋弹窗、导入导出入口

功能清单：

- [ ] 查询楼栋详情
- [ ] 分页查询楼栋列表
- [ ] 按园区、名称、编码、状态筛选
- [ ] 新增楼栋
- [ ] 修改楼栋
- [ ] 删除楼栋
- [ ] 同园区楼栋名称唯一校验
- [ ] 建筑编码必填校验
- [ ] 所属地区必填校验
- [ ] 产权性质必填校验
- [ ] 排序值必填校验
- [ ] 楼层数有效性校验
- [ ] 建筑面积、产权面积校验
- [ ] 修改楼层数时校验已有房间最大楼层
- [ ] 新增/修改后自动同步楼层
- [ ] 楼层面积配置 `floorAreas` 生效
- [ ] 导入建筑
- [ ] 导出建筑
- [ ] 下载导入模板

数据流：

```text
BladeX 前端
  -> building list/detail/save/update/remove/import/export/template
  -> BuildingController
  -> BuildingService
  -> BuildingMapper
  -> ics_building
  -> FloorService.syncBuildingFloors
  -> ics_floor
```

关联关系：

- 依赖 `ics_park.id`。
- 被 `ics_floor.building_id` 引用。
- 被 `ics_room.building_id` 引用。
- 被 `biz_contract.building_id/building_ids` 引用。
- 楼栋列表统计依赖 `ics_floor`、`ics_room`、`biz_contract`。

迁移验收：

- [ ] `ics_building` 记录数迁移前后一致。
- [ ] 新增楼栋会自动生成 `1..floors` 的楼层。
- [ ] 修改楼栋楼层数会补齐缺失楼层。
- [ ] 修改楼栋楼层数不会删除已有房间所在楼层。
- [ ] 同园区重复楼栋名称被拦截。
- [ ] 楼栋列表统计字段与 RuoYi 原接口一致。
- [ ] 导入导出不丢字段。

验收 SQL：

```sql
SELECT COUNT(*) FROM ics_building;

SELECT b.id, b.park_id, b.name, b.floors,
       COUNT(f.id) AS floor_count
FROM ics_building b
LEFT JOIN ics_floor f ON f.building_id = b.id
GROUP BY b.id, b.park_id, b.name, b.floors;

SELECT park_id, name, COUNT(*) AS duplicate_count
FROM ics_building
GROUP BY park_id, name
HAVING COUNT(*) > 1;
```

### 7.3 楼层模块 Floor

迁移范围：

- 表：`ics_floor`
- 后端：楼层 CRUD、楼层同步、面积容量校验
- 前端：楼层列表

功能清单：

- [ ] 查询楼层详情
- [ ] 查询楼层列表
- [ ] 按园区、楼栋、楼层号、状态筛选
- [ ] 新增楼层
- [ ] 修改楼层
- [ ] 删除楼层
- [ ] 同楼栋楼层号唯一校验
- [ ] 楼层号范围校验
- [ ] 楼层归属与楼栋归属一致校验
- [ ] 楼层面积不能小于房间面积合计
- [ ] 有房间的楼层不能删除
- [ ] 同步楼栋楼层
- [ ] 楼层统计：房间总数、空闲、已租、预定、装修、停用、已用面积

数据流：

```text
BladeX 前端
  -> floor list/detail/save/update/remove/sync
  -> FloorController
  -> FloorService
  -> FloorMapper
  -> ics_floor
  -> RoomMapper 统计房间数量和面积
```

关联关系：

- 依赖 `ics_park.id`。
- 依赖 `ics_building.id` 和 `ics_building.floors`。
- 与 `ics_room` 通过 `building_id + floor` 关联。
- 注意当前房间表没有 `floor_id`，不能按 `ics_floor.id` 关联房间。

迁移验收：

- [ ] `ics_floor` 记录数迁移前后一致。
- [ ] `building_id + floor_no` 无重复。
- [ ] 每条楼层都有合法楼栋。
- [ ] 每条楼层 `park_id` 与楼栋 `park_id` 一致。
- [ ] 楼层面积容量校验生效。
- [ ] 有房间楼层删除会被拦截。
- [ ] 同步接口能补齐楼层。

验收 SQL：

```sql
SELECT COUNT(*) FROM ics_floor;

SELECT building_id, floor_no, COUNT(*) AS duplicate_count
FROM ics_floor
GROUP BY building_id, floor_no
HAVING COUNT(*) > 1;

SELECT f.*
FROM ics_floor f
LEFT JOIN ics_building b ON b.id = f.building_id
WHERE b.id IS NULL;

SELECT f.id, f.park_id AS floor_park_id, b.park_id AS building_park_id
FROM ics_floor f
JOIN ics_building b ON b.id = f.building_id
WHERE f.park_id <> b.park_id;

SELECT f.id, f.building_id, f.floor_no, f.area, COALESCE(SUM(r.area), 0) AS room_area
FROM ics_floor f
LEFT JOIN ics_room r ON r.building_id = f.building_id AND r.floor = f.floor_no
GROUP BY f.id, f.building_id, f.floor_no, f.area
HAVING f.area IS NOT NULL AND f.area < room_area;
```

### 7.4 房间模块 Room

迁移范围：

- 表：`ics_room`
- 后端：房间 CRUD、状态切换、小程序同步标记、楼层看板
- 前端：房间列表、房间弹窗、楼层看板、图片上传

功能清单：

- [ ] 查询房间详情
- [ ] 分页查询房间列表
- [ ] 按园区、楼栋、楼层、状态、朝向、面积、租金筛选
- [ ] 新增房间
- [ ] 修改房间
- [ ] 删除房间
- [ ] 房间状态切换
- [ ] 小程序同步标记
- [ ] 房间图片上传
- [ ] 实景图 `scene_images` 保存
- [ ] 平面图 `floor_plan_images` 保存
- [ ] 新增/修改时从楼栋反填 `park_id`
- [ ] 楼栋存在性校验
- [ ] 楼层范围校验
- [ ] 楼层面积容量校验
- [ ] 删除房间前检查合同/工单引用
- [ ] 楼层看板数据返回
- [ ] 合同到期提醒接口兼容或迁出

数据流：

```text
BladeX 前端
  -> room list/detail/save/update/remove/changeStatus/syncMini/floorBoard
  -> RoomController
  -> RoomService
  -> RoomMapper
  -> ics_room
  -> BuildingMapper/FloorMapper/ParkMapper
  -> 合同/客户/标签统计
```

关联关系：

- 依赖 `ics_building.id`。
- 依赖 `ics_floor` 的 `building_id + floor_no`。
- 被 `biz_contract.room_id/room_ids` 引用。
- 被 `biz_service_workorder.room_ids` 引用。
- 楼层看板依赖 `biz_contract`、`biz_customer`、`biz_customer_tag`、`biz_tag`、`biz_tag_type`。

迁移验收：

- [ ] `ics_room` 记录数迁移前后一致。
- [ ] 每条房间都有合法楼栋。
- [ ] 每条房间 `park_id` 与楼栋 `park_id` 一致。
- [ ] 房间楼层在楼栋范围内。
- [ ] 房间面积容量校验生效。
- [ ] 状态切换后 `sync_status = 0`。
- [ ] 小程序同步标记后 `sync_status = 1` 且 `sync_time` 有值。
- [ ] 楼层看板能按园区/楼栋筛选。
- [ ] 楼层看板统计数量和房间列表一致。
- [ ] 房间删除不会破坏合同/工单引用。

验收 SQL：

```sql
SELECT COUNT(*) FROM ics_room;

SELECT r.*
FROM ics_room r
LEFT JOIN ics_building b ON b.id = r.building_id
WHERE b.id IS NULL;

SELECT r.id, r.park_id AS room_park_id, b.park_id AS building_park_id
FROM ics_room r
JOIN ics_building b ON b.id = r.building_id
WHERE r.park_id <> b.park_id;

SELECT r.id, r.building_id, r.floor, b.floors
FROM ics_room r
JOIN ics_building b ON b.id = r.building_id
WHERE r.floor IS NULL OR r.floor < 1 OR r.floor > b.floors;

SELECT r.id, r.name, c.contract_id
FROM ics_room r
JOIN biz_contract c
  ON c.room_id = r.id
  OR FIND_IN_SET(r.id, REPLACE(c.room_ids, 'room_', ''))
WHERE c.del_flag = '0';
```

## 8. 数据迁移脚本清单

### 8.1 建表脚本

如果 `bladex_boot` 中尚无四张业务表，先执行建表脚本。建议从当前 `SHOW CREATE TABLE` 生成并清理字符集差异，文件名：

- `sql/blade_ics_asset_tables.sql`

内容包括：

- `ics_park`
- `ics_building`
- `ics_floor`
- `ics_room`
- 必要索引：
  - `uk_park_building_name (park_id, name)`
  - `uk_building_floor (building_id, floor_no)`
  - `idx_ics_floor_park_id (park_id)`
  - `idx_ics_room_park_id (park_id)`
  - `idx_ics_room_building_floor (building_id, floor)`

### 8.2 数据导入脚本

建议先用逻辑导出导入：

```bash
mysqldump -uroot -proot ry_ics ics_park ics_building ics_floor ics_room > /tmp/ics_asset_data.sql
mysql -uroot -proot bladex_boot < /tmp/ics_asset_data.sql
```

如果 `bladex_boot` 已存在同名表，导入前必须确认是否清空、合并或增量导入。

### 8.3 迁移后修正脚本

建议准备：

- 修正房间 `park_id`：

```sql
UPDATE ics_room r
JOIN ics_building b ON b.id = r.building_id
SET r.park_id = b.park_id
WHERE r.park_id IS NULL OR r.park_id <> b.park_id;
```

- 修正楼层 `park_id`：

```sql
UPDATE ics_floor f
JOIN ics_building b ON b.id = f.building_id
SET f.park_id = b.park_id
WHERE f.park_id <> b.park_id;
```

- 补齐缺失楼层：由应用服务 `syncBuildingFloors` 执行，不建议纯 SQL 生成，避免楼层面积规则不一致。

## 9. 集成校验清单

### 9.1 启动前校验

- [ ] BladeX 后端能启动。
- [ ] BladeX 前端能启动。
- [ ] Redis 可连接。
- [ ] `bladex_boot` 可连接。
- [ ] 用户能登录。
- [ ] 菜单能正常加载。
- [ ] 当前端口仍为后端 `8080`。
- [ ] 前端代理指向 `8080`。
- [ ] SM2/crypto 配置与后端一致。

### 9.2 数据校验

- [ ] 四张表记录数一致。
- [ ] 楼栋无孤儿园区。
- [ ] 楼层无孤儿楼栋。
- [ ] 房间无孤儿楼栋。
- [ ] 房间无孤儿园区。
- [ ] 楼层与楼栋园区一致。
- [ ] 房间与楼栋园区一致。
- [ ] 楼栋名称唯一索引无冲突。
- [ ] 楼层唯一索引无冲突。
- [ ] 房间楼层不超出楼栋层数。

### 9.3 接口校验

每个模块至少校验：

- [ ] `list`
- [ ] `detail`
- [ ] `submit/save`
- [ ] `update`
- [ ] `remove`
- [ ] 权限不足返回正确。
- [ ] 参数非法返回业务错误。
- [ ] 未登录返回认证错误。
- [ ] 分页字段符合 BladeX 前端期望。
- [ ] 操作日志正常记录。

### 9.4 页面校验

- [ ] 菜单可见。
- [ ] 页面可进入。
- [ ] 查询条件可用。
- [ ] 表格分页可用。
- [ ] 新增弹窗/页面可用。
- [ ] 编辑回显完整。
- [ ] 删除二次确认。
- [ ] 按钮权限生效。
- [ ] 字典显示正确。
- [ ] 图片上传和预览可用。
- [ ] 移动端或窄屏无明显布局错误。

### 9.5 回归校验

- [ ] 合同新增选择园区、楼栋、房间正常。
- [ ] 工单选择房源正常。
- [ ] 楼层看板统计正常。
- [ ] 审批按园区过滤正常。
- [ ] 普通用户数据权限正常。
- [ ] 超级管理员可查看全部数据。

## 10. 风险清单

| 风险 | 影响 | 处理方式 |
| --- | --- | --- |
| BladeX 源码目录未定位 | 无法直接落代码 | 先确认目标工程路径 |
| `ics_room` 有孤儿楼栋 | 导入后看板/列表异常 | 迁移前修复 |
| 房间不引用 `floor_id` | 楼层删除/统计容易误判 | 继续使用 `building_id + floor` 规则 |
| 合同字段有单 ID 和逗号多 ID | 统计 SQL 复杂 | 保留兼容查询，第二阶段规范 |
| RuoYi 分页与 BladeX 分页不同 | 前端列表异常 | 统一封装分页响应 |
| 图片上传路径变化 | 图片不可访问 | 迁移到 BladeX 附件/OSS 或做旧 URL 兼容 |
| 多租户与园区权限混淆 | 数据越权或查不到数据 | 租户和园区权限分开建模 |
| 并行分支修改同一菜单/DDL | 合并冲突 | 菜单和 DDL 由集成分支统一维护 |

## 11. 推荐执行顺序

1. 以 `/Users/jiakangli/Desktop/bladex-park/BladeX-Boot` 和 `/Users/jiakangli/Desktop/bladex-park/saber3` 作为 BladeX 后端/前端目标工程。
2. 先处理 `saber3` 当前本地配置改动，确认提交、暂存或作为迁移基线保留。
3. 在 BladeX 后端和前端仓库分别创建四个迁移 Work Tree。
4. 建立统一 DDL、菜单权限、API 契约。
5. 修复 `ry_ics` 中 `room_orphan_building = 1` 的脏数据。
6. 导入四张业务表到 `bladex_boot`。
7. 先合并园区模块。
8. 合并楼栋模块，并验证自动生成楼层。
9. 合并楼层模块，并验证面积/删除规则。
10. 合并房间模块，并验证楼层看板。
11. 统一跑集成验收清单。
12. 再开始合同、工单、审批等关联模块迁移。
