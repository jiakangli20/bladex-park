# CLAUDE.md
本文件用于指导 Claude Code (claude.ai/code) 在此代码仓库中工作时的行为规范。

> 本规范适用于 BladeX-Boot 单体应用平台的所有开发任务，为强制性条款。除非用户显式豁免，任何条目都不得忽视或删减。
>
> 作为 AI 助手参与本项目开发时，你必须：
> - 每次输出前深度理解 BladeX-Boot 架构、业务逻辑和 Spring Boot 技术栈特征
> - 当回答依赖外部知识时，先查询 Spring Boot 3.x、MyBatis-Plus、Flowable 等官方文档
> - 若需求含糊，先复述已知信息并列出关键澄清问题
> - 面对复杂需求，先拆分为可管理的子任务
>
> 所有开发内容必须建立在深度思考过的基础之上，禁止机械生成与错误填充。
> 如果你已了解所有规范，请在用户第一次对话时说明："我已充分了解 BladeX 微服务平台开发规范。"

---

## 1. 项目概述

BladeX-Boot 是 BladeX 平台的**单体应用版本**，基于 Spring Boot 3.x 构建，排除 Spring Cloud 组件，所有模块合并在一个 JAR 中运行。

---

## 2. 项目架构

```
BladeX-Boot/
├── src/main/java/org/springblade/
│   ├── Application.java                  # 主启动类
│   ├── common/                           # 通用模块（cache, config, constant, enums, event, handler, utils）
│   ├── modules/                          # 业务模块
│   │   ├── auth/                         # 认证授权（OAuth2, 令牌, 锁定机制）
│   │   ├── desk/                         # 工作台（通知公告）
│   │   ├── develop/                      # 开发工具（代码生成, 数据模型）
│   │   ├── resource/                     # 资源管理（OSS, SMS, 附件）
│   │   └── system/                       # 系统管理（用户, 角色, 菜单, 字典, 租户, 部门, 岗位, 日志）
│   ├── flow/                             # 工作流模块（Flowable）
│   └── job/                              # 任务调度模块（PowerJob）
├── src/main/resources/                   # 配置文件（application.yml + dev/test/prod 多环境）
├── doc/                                  # SQL脚本 + 部署脚本
├── pom.xml
└── Dockerfile
```

### 2.1 分层架构

每个业务模块遵循标准分层，所有层次在**同一模块**内：

| 层次 | 包路径 |
| --- | --- |
| Controller | `controller/` |
| Service / ServiceImpl | `service/` + `service/impl/` |
| Mapper（接口 + XML 同包） | `mapper/` |
| Entity / VO / DTO | `pojo/entity/` `pojo/vo/` `pojo/dto/` |
| Wrapper | `wrapper/`（Entity → VO 转换） |
| 可选 | `enums/` `excel/` `rule/` |

> **与 Cloud 版的关键区别**：Boot 为单体架构，无 API/Service 模块分离，无 Feign Client，模块间直接依赖。

---

## 3. 技术栈

| 类别 | 技术 |
| --- | --- |
| 基础框架 | Spring Boot 3.2.x / Maven |
| ORM | MyBatis-Plus（禁止 JDBC 直连） |
| Web 服务器 | Undertow |
| 安全 | OAuth2 / JWT（自研 Secure 框架）、数据权限、接口权限 |
| 缓存 | Redis（Protostuff 序列化） |
| 工作流 / 任务调度 | Flowable / PowerJob |
| 数据库连接池 | Druid |
| 文档 | Knife4j（OpenAPI 3.0） |
| 数据库 | MySQL / PostgreSQL / Oracle / SQL Server / 达梦 / 人大金仓 / 崖山 |

---

## 4. 开发规范

### 4.1 编写新功能前

1. 先阅读目标模块中已有的类，理解其结构、命名和风格
2. 标准参考模块：`modules/desk`（通知公告）、`modules/system`（系统管理）
3. 主动模仿现有代码风格，包括缩进（Java 用 Tab，YAML/JSON 用 2 空格）、注解顺序、Javadoc 格式
4. 新建类必须包含 BladeX 商业许可证头部和 Javadoc 类注释（`@author Chill`）

### 4.2 编写完成后

1. 使用 `mvn clean compile -DskipTests` 编译验证，编译不通过必须修复
2. 引入模块依赖前先检查循环依赖，若存在则采用更优方案
3. 编译通过后将测试交由用户执行，**不得自行执行任何测试**
4. 除非用户明确要求，不应撰写示例或额外文档

### 4.3 代码生成

当需要生成 CRUD 全套代码（Entity、VO、Service、Controller、Wrapper、Mapper、建表语句等）时，优先使用 **`/blade-design`** skill。该 skill 可根据模块名、实体名和字段列表，自动生成符合 BladeX 框架规范的后端代码和多数据库建表语句，确保生成结果与项目风格完全一致。

---

## 5. 命名规范

### 5.1 包名

统一前缀 `org.springblade`，按模块划分：`modules.system`、`modules.desk`、`modules.auth`、`modules.resource`、`modules.develop`、`flow`、`job`、`common` 等

### 5.2 类名

| 类型 | 规则 | 示例 |
| --- | --- | --- |
| Entity | 直接类名 | `Notice`、`Tenant` |
| VO / DTO | `XxxVO` / `XxxDTO` | `NoticeVO`、`DeptDTO` |
| Service 接口 | `IXxxService` | `INoticeService` |
| Service 实现 | `XxxServiceImpl` | `NoticeServiceImpl` |
| Controller | `XxxController` | `NoticeController` |
| Mapper | `XxxMapper` | `NoticeMapper` |
| Wrapper | `XxxWrapper` | `NoticeWrapper` |
| 表名 | `blade_` + 下划线 | `blade_notice` |

### 5.3 变量命名

- 必须具有明确语义：`Exception exception`（✅）`Exception e`（❌）；`List<Notice> noticeList`（✅）`List<Notice> list`（❌）
- 冲突时提升语义：`Cache cache; Cache noticeCache`（✅）`Cache cache1; Cache cache2`（❌）

---

## 6. 编码规范

### 6.1 注解顺序

- **Controller 类**：租户注解(`@TenantDS`/`@NonDS`) → `@RestController` → Lombok(`@AllArgsConstructor`) → 安全注解(`@PreAuth`) → `@RequestMapping` → `@Tag`
- **Controller 方法**：HTTP 方法注解 → `@ApiOperationSupport(order=N)` → `@Operation`
- **Entity 类**：`@Data` → `@EqualsAndHashCode` → `@TableName` → `@Schema`
- **VO 类**：`@Data` → `@EqualsAndHashCode(callSuper = true)` → `@Schema`

### 6.2 Entity-Service 继承体系（核心）

Entity 基类的选择**直接决定** Service 和 ServiceImpl 的继承方式，三者必须配套：

| 场景 | Entity | Service 接口 | ServiceImpl | 示例 |
| --- | --- | --- | --- | --- |
| **多租户业务表** | `extends TenantEntity` | `extends BaseService<T>` | `extends BaseServiceImpl<M, T>` | Notice, Post, Dept |
| **非租户业务表** | `extends BaseEntity` | `extends BaseService<T>` | `extends BaseServiceImpl<M, T>` | Tenant |
| **轻量级/关系表** | `implements Serializable` | `extends IService<T>` | `extends ServiceImpl<M, T>` | RoleMenu, Dict, Role |

- `TenantEntity` 继承自 `BaseEntity` 并额外包含 `tenantId`
- `BaseEntity` 包含：id, createUser, createDept, createTime, updateUser, updateTime, status, isDeleted
- `BaseService`/`BaseServiceImpl` 是 BladeX 增强版，提供 `deleteLogic()` 等方法，**要求 Entity 继承 BaseEntity 或 TenantEntity**
- `IService`/`ServiceImpl` 是 MyBatis-Plus 原生版，用于 `implements Serializable` 的轻量实体，删除用 `removeByIds()`

### 6.3 Controller 约定

- 继承 `BladeController`，使用 `@AllArgsConstructor` 注入
- **路由前缀使用 `AppConstant.APPLICATION_XXX_NAME` 常量**（Boot 单体特有，Cloud 版无需前缀）
- 统一返回 `R<T>` 响应体
- Entity → VO 转换通过 `XxxWrapper.build().entityVO()` / `.pageVO()`

### 6.4 依赖注入

- Controller 使用 `@AllArgsConstructor` + `private final` 字段
- Service 需要额外依赖时使用 `@RequiredArgsConstructor` + `private final` 字段

### 6.5 Lombok

禁止手写 getter/setter，统一使用 `@Data`、`@EqualsAndHashCode(callSuper = true)`、`@AllArgsConstructor`、`@RequiredArgsConstructor`、`@Slf4j` 等

### 6.6 Java 17 特性

- 可使用增强 switch、Text Blocks、Pattern Matching for instanceof、`@Serial`
- **禁止** `var` / `val` 类型推断，所有变量显式声明类型
- 优先使用 Stream API，Lambda 保持简洁

### 6.7 MyBatis-Plus

- 简单查询使用 `LambdaQueryWrapper`，复杂查询写在 Mapper XML 中
- Mapper XML 与接口**同包**放置（`src/main/java` 下）
- 分页统一使用 `Condition.getPage(query)` + `Condition.getQueryWrapper()`
- 禁止 JDBC 直连查询

### 6.8 日志

- 使用 `@Slf4j`，占位符传参（禁止字符串拼接）
- 包含关键业务标识，异常必须携带堆栈，禁止打印敏感信息

---

## 7. 数据库规范

- **表名前缀**：`blade_`（如 `blade_notice`、`blade_user`）
- **业务表必含字段**：`id`(BIGINT)、`tenant_id`(VARCHAR)、`create_user`、`create_dept`、`create_time`、`update_user`、`update_time`、`status`、`is_deleted`
- **主键策略**：雪花算法（`IdType.ASSIGN_ID`），Long 字段用 `@JsonSerialize(using = ToStringSerializer.class)` 防精度丢失
- **逻辑删除**：`is_deleted` (INT)，0 = 未删除，1 = 已删除
- **索引命名**：唯一索引 `uk_` 前缀，普通索引 `idx_` 前缀
- **多数据库**：支持 7 种数据库，修改表结构时需同步所有脚本

---

## 8. 多租户与安全

- **多租户**：默认字段隔离模式（`tenant_id`），MP 内置方法自动注入；自定义 SQL 需手动 `AuthUtil.getTenantId()`
- **租户注解**：`@TenantDS`（启用数据源切换）/ `@NonDS`（禁用切换）
- **权限注解**：`@PreAuth(menu = "xxx")`（菜单权限）、`@PreAuth(AuthConstant.PERMIT_ALL)`（公开）
- **数据权限**：DataScopeHandler 行级过滤
- **接口权限**：ApiScopeHandler 端点级控制

---

## 9. 缓存规范

- `CacheUtil` 统一操作，常量在 `CacheConstant`
- 数据变更后清除缓存：`CacheUtil.clear(CACHE_NAME, Boolean.FALSE)`
- 字典翻译：`DictCache.getValue(DictEnum.XXX, value)`

---

## 10. 商业许可头部

所有新建的 Java 源文件必须在文件顶部包含以下许可头部：
```java
/**
 * BladeX Commercial License Agreement
 * Copyright (c) 2018-2099, https://bladex.cn. All rights reserved.
 * <p>
 * Use of this software is governed by the Commercial License Agreement
 * obtained after purchasing a license from BladeX.
 * <p>
 * 1. This software is for development use only under a valid license
 * from BladeX.
 * <p>
 * 2. Redistribution of this software's source code to any third party
 * without a commercial license is strictly prohibited.
 * <p>
 * 3. Licensees may copyright their own code but cannot use segments
 * from this software for such purposes. Copyright of this software
 * remains with BladeX.
 * <p>
 * Using this software signifies agreement to this License, and the software
 * must not be used for illegal purposes.
 * <p>
 * THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY. The author is
 * not liable for any claims arising from secondary or illegal development.
 * <p>
 * Author: Chill Zhuang (bladejava@qq.com)
 */
```

---

## 11. 构建与部署

```bash
mvn clean compile -DskipTests                  # 编译验证
mvn clean package -DskipTests                  # 打包
mvn clean package docker:build -DskipTests     # Docker 镜像
```

---

## 12. Git 提交规范

当需要提交代码时，优先使用 **`/blade-commit`** skill，它会自动分析变更内容并生成符合项目规范的 Gitmoji 提交信息。

采用 **Gitmoji** 风格，中文描述。格式：`:<gitmoji>: 简要描述`

| Gitmoji | 场景 | 示例                     |
| --- | --- |------------------------|
| `:sparkles:` | 新增功能 | `:sparkles: 新增角色授权功能`  |
| `:zap:` | 优化重构 | `:zap: 优化字典查询排序逻辑`     |
| `:bug:` | 修复缺陷 | `:bug: 修复用户查询未过滤已删除数据` |
| `:tada:` | 版本发布 | `:tada: x.x.x.RELEASE` |
| `:recycle:` | 代码重构 | `:recycle: 重构租户删除逻辑`   |

---

## 13. 框架组件速查

| 组件 | 用途 |
| --- | --- |
| `R<T>` | 统一 API 响应 |
| `BladeController` | Controller 基类 |
| `TenantEntity` / `BaseEntity` | 实体基类（多租户/非租户） |
| `BaseService` / `BaseServiceImpl` | BladeX 增强 Service（含 deleteLogic） |
| `BaseEntityWrapper` | Entity→VO 转换基类 |
| `BladeUser` | 当前登录用户（含租户信息） |
| `Condition` / `Query` | 查询条件构建 / 分页参数 |
| `CacheUtil` / `DictCache` | 缓存工具 / 字典缓存 |
| `AuthUtil` | 获取当前用户/租户信息 |
| `Func` / `BeanUtil` / `StringUtil` | 通用工具类 |
| `ServiceException` | 业务异常 |
| `@PreAuth` / `@TenantDS` / `@NonDS` | 权限 / 租户数据源切换 |
| `@BladeView` / `@DataRecord` / `@XssIgnore` | 视图控制 / 数据审计 / XSS 跳过 |

---

## 14. 风格一致性

1. 风格不确定时，优先查阅现有代码并模仿当前模块的实现方式
2. 新模块参考 `modules/desk` 作为标准模板
3. 现有模块已满足需求时，禁止自写替代实现
4. 与用户交互全程使用**中文**，代码注释和 Javadoc 亦使用中文
