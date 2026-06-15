-- ----------------------------
-- Table structure for blade_auth_lock
-- ----------------------------
CREATE TABLE [blade_auth_lock] (
    [id] bigint NOT NULL,
    [user_id] bigint,
    [tenant_id] nvarchar(12) DEFAULT '000000',
    [lock_type] nvarchar(20) NOT NULL,
    [lock_status] nvarchar(20) NOT NULL,
    [lock_target] nvarchar(100) NOT NULL,
    [remote_ip] nvarchar(50),
    [user_agent] nvarchar(500),
    [fail_count] int DEFAULT 0,
    [lock_begin_time] datetime NOT NULL,
    [lock_end_time] datetime,
    [lock_reason] nvarchar(500),
    [unlock_reason] nvarchar(500),
    [status] int DEFAULT 1,
    [is_deleted] int DEFAULT 0,
    PRIMARY KEY CLUSTERED ([id] ASC)
    WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
    )
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'主键',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_lock',
    'COLUMN', N'id'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'关联用户ID',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_lock',
    'COLUMN', N'user_id'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'租户ID',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_lock',
    'COLUMN', N'tenant_id'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'锁定类型（SYSTEM-系统自动/MANUAL-人工手动）',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_lock',
    'COLUMN', N'lock_type'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'锁定状态（PRE_LOCKED-待锁定/LOCKED-锁定中/UN_LOCKED-已解锁）',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_lock',
    'COLUMN', N'lock_status'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'锁定目标（账号名或IP地址）',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_lock',
    'COLUMN', N'lock_target'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'请求IP',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_lock',
    'COLUMN', N'remote_ip'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'用户代理',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_lock',
    'COLUMN', N'user_agent'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'认证失败次数',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_lock',
    'COLUMN', N'fail_count'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'锁定开始时间',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_lock',
    'COLUMN', N'lock_begin_time'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'锁定结束时间（MANUAL类型为空表示永久锁定，SYSTEM-LOCKED状态为锁定到期时间）',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_lock',
    'COLUMN', N'lock_end_time'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'锁定原因',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_lock',
    'COLUMN', N'lock_reason'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'解锁原因',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_lock',
    'COLUMN', N'unlock_reason'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'状态',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_lock',
    'COLUMN', N'status'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'是否已删除',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_lock',
    'COLUMN', N'is_deleted'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'认证锁定记录表',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_lock';

-- ----------------------------
-- Table structure for blade_auth_log
-- ----------------------------
CREATE TABLE [blade_auth_log] (
    [id] bigint NOT NULL,
    [user_id] bigint,
    [tenant_id] nvarchar(12) DEFAULT '000000',
    [service_id] nvarchar(32),
    [server_ip] nvarchar(255),
    [server_host] nvarchar(255),
    [env] nvarchar(255),
    [account] nvarchar(45),
    [real_name] nvarchar(20),
    [grant_type] nvarchar(30),
    [remote_ip] nvarchar(255),
    [user_agent] nvarchar(1000),
    [login_time] datetime2,
    [status] int DEFAULT 1,
    [is_deleted] int DEFAULT 0,
    PRIMARY KEY CLUSTERED ([id] ASC)
    WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
    )
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'主键',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_log',
    'COLUMN', N'id'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'用户ID',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_log',
    'COLUMN', N'user_id'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'租户ID',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_log',
    'COLUMN', N'tenant_id'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'服务ID',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_log',
    'COLUMN', N'service_id'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'服务器IP地址',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_log',
    'COLUMN', N'server_ip'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'服务器名',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_log',
    'COLUMN', N'server_host'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'服务器环境',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_log',
    'COLUMN', N'env'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'授权账号',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_log',
    'COLUMN', N'account'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'用户姓名',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_log',
    'COLUMN', N'real_name'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'授权类型',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_log',
    'COLUMN', N'grant_type'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'请求IP',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_log',
    'COLUMN', N'remote_ip'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'用户代理',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_log',
    'COLUMN', N'user_agent'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'登录时间',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_log',
    'COLUMN', N'login_time'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'状态',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_log',
    'COLUMN', N'status'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'是否已删除',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_log',
    'COLUMN', N'is_deleted'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'认证日志表',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_auth_log';

-- ----------------------------
-- Table structure for blade_api_key_log
-- ----------------------------
CREATE TABLE [blade_api_key_log] (
    [id] bigint NOT NULL,
    [tenant_id] nvarchar(12) DEFAULT '000000',
    [service_id] nvarchar(32),
    [server_host] nvarchar(255),
    [server_ip] nvarchar(255),
    [env] nvarchar(255),
    [api_key_id] bigint,
    [method] nvarchar(10),
    [request_uri] nvarchar(255),
    [user_agent] nvarchar(1000),
    [remote_ip] nvarchar(255),
    [params] ntext,
    [time] nvarchar(64),
    [create_by] nvarchar(64),
    [create_time] datetime,
    PRIMARY KEY CLUSTERED ([id] ASC)
    WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON)
    )
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'编号',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_api_key_log',
    'COLUMN', N'id'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'租户ID',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_api_key_log',
    'COLUMN', N'tenant_id'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'服务ID',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_api_key_log',
    'COLUMN', N'service_id'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'服务器名',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_api_key_log',
    'COLUMN', N'server_host'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'服务器IP地址',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_api_key_log',
    'COLUMN', N'server_ip'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'服务器环境',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_api_key_log',
    'COLUMN', N'env'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'API Key主键ID',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_api_key_log',
    'COLUMN', N'api_key_id'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'操作方式',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_api_key_log',
    'COLUMN', N'method'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'请求URI',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_api_key_log',
    'COLUMN', N'request_uri'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'用户代理',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_api_key_log',
    'COLUMN', N'user_agent'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'操作IP地址',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_api_key_log',
    'COLUMN', N'remote_ip'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'操作提交的数据',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_api_key_log',
    'COLUMN', N'params'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'执行时间',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_api_key_log',
    'COLUMN', N'time'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'创建者',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_api_key_log',
    'COLUMN', N'create_by'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'创建时间',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_api_key_log',
    'COLUMN', N'create_time'
    GO

    EXEC sp_addextendedproperty
    'MS_Description', N'令牌调用日志表',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_api_key_log';

-- -----------------------------------
-- 新增是否主页字段
-- -----------------------------------
ALTER TABLE [blade_top_menu] ADD [is_main] int DEFAULT 0
GO

EXEC sp_addextendedproperty
    'MS_Description', N'是否主页',
    'SCHEMA', N'dbo',
    'TABLE', N'blade_top_menu',
    'COLUMN', N'is_main';
