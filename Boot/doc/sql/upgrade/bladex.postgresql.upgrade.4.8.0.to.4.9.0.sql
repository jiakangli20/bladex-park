-- ----------------------------
-- Table structure for blade_auth_lock
-- ----------------------------
CREATE TABLE "blade_auth_lock" (
    "id" int8 NOT NULL,
    "user_id" int8,
    "tenant_id" varchar(12) COLLATE "pg_catalog"."default" DEFAULT '000000',
    "lock_type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
    "lock_status" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
    "lock_target" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
    "remote_ip" varchar(50) COLLATE "pg_catalog"."default",
    "user_agent" varchar(500) COLLATE "pg_catalog"."default",
    "fail_count" int4 DEFAULT 0,
    "lock_begin_time" timestamp(6) NOT NULL,
    "lock_end_time" timestamp(6),
    "lock_reason" varchar(500) COLLATE "pg_catalog"."default",
    "unlock_reason" varchar(500) COLLATE "pg_catalog"."default",
    "status" int4 DEFAULT 1,
    "is_deleted" int4 DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON COLUMN "blade_auth_lock"."id" IS '主键';

COMMENT ON COLUMN "blade_auth_lock"."user_id" IS '关联用户ID';

COMMENT ON COLUMN "blade_auth_lock"."tenant_id" IS '租户ID';

COMMENT ON COLUMN "blade_auth_lock"."lock_type" IS '锁定类型（SYSTEM-系统自动/MANUAL-人工手动）';

COMMENT ON COLUMN "blade_auth_lock"."lock_status" IS '锁定状态（PRE_LOCKED-待锁定/LOCKED-锁定中/UN_LOCKED-已解锁）';

COMMENT ON COLUMN "blade_auth_lock"."lock_target" IS '锁定目标（账号名或IP地址）';

COMMENT ON COLUMN "blade_auth_lock"."remote_ip" IS '请求IP';

COMMENT ON COLUMN "blade_auth_lock"."user_agent" IS '用户代理';

COMMENT ON COLUMN "blade_auth_lock"."fail_count" IS '认证失败次数';

COMMENT ON COLUMN "blade_auth_lock"."lock_begin_time" IS '锁定开始时间';

COMMENT ON COLUMN "blade_auth_lock"."lock_end_time" IS '锁定结束时间（MANUAL类型为空表示永久锁定，SYSTEM-LOCKED状态为锁定到期时间）';

COMMENT ON COLUMN "blade_auth_lock"."lock_reason" IS '锁定原因';

COMMENT ON COLUMN "blade_auth_lock"."unlock_reason" IS '解锁原因';

COMMENT ON COLUMN "blade_auth_lock"."status" IS '状态';

COMMENT ON COLUMN "blade_auth_lock"."is_deleted" IS '是否已删除';

COMMENT ON TABLE "blade_auth_lock" IS '认证锁定记录表';

-- ----------------------------
-- Table structure for blade_auth_log
-- ----------------------------
CREATE TABLE "blade_auth_log" (
    "id" int8 NOT NULL,
    "user_id" int8,
    "tenant_id" varchar(12) COLLATE "pg_catalog"."default" DEFAULT '000000',
    "service_id" varchar(32) COLLATE "pg_catalog"."default",
    "server_ip" varchar(255) COLLATE "pg_catalog"."default",
    "server_host" varchar(255) COLLATE "pg_catalog"."default",
    "env" varchar(255) COLLATE "pg_catalog"."default",
    "account" varchar(45) COLLATE "pg_catalog"."default",
    "real_name" varchar(20) COLLATE "pg_catalog"."default",
    "grant_type" varchar(30) COLLATE "pg_catalog"."default",
    "remote_ip" varchar(255) COLLATE "pg_catalog"."default",
    "user_agent" varchar(1000) COLLATE "pg_catalog"."default",
    "login_time" timestamp,
    "status" int4 DEFAULT 1,
    "is_deleted" int4 DEFAULT 0,
    PRIMARY KEY ("id")
);

COMMENT ON COLUMN "blade_auth_log"."id" IS '主键';

COMMENT ON COLUMN "blade_auth_log"."user_id" IS '用户ID';

COMMENT ON COLUMN "blade_auth_log"."tenant_id" IS '租户ID';

COMMENT ON COLUMN "blade_auth_log"."service_id" IS '服务ID';

COMMENT ON COLUMN "blade_auth_log"."server_ip" IS '服务器IP地址';

COMMENT ON COLUMN "blade_auth_log"."server_host" IS '服务器名';

COMMENT ON COLUMN "blade_auth_log"."env" IS '服务器环境';

COMMENT ON COLUMN "blade_auth_log"."account" IS '授权账号';

COMMENT ON COLUMN "blade_auth_log"."real_name" IS '用户姓名';

COMMENT ON COLUMN "blade_auth_log"."grant_type" IS '授权类型';

COMMENT ON COLUMN "blade_auth_log"."remote_ip" IS '请求IP';

COMMENT ON COLUMN "blade_auth_log"."user_agent" IS '用户代理';

COMMENT ON COLUMN "blade_auth_log"."login_time" IS '登录时间';

COMMENT ON COLUMN "blade_auth_log"."status" IS '状态';

COMMENT ON COLUMN "blade_auth_log"."is_deleted" IS '是否已删除';

COMMENT ON TABLE "blade_auth_log" IS '认证日志表';

-- ----------------------------
-- Table structure for blade_api_key_log
-- ----------------------------
CREATE TABLE "blade_api_key_log" (
    "id" int8 NOT NULL,
    "tenant_id" varchar(12) COLLATE "pg_catalog"."default" DEFAULT '000000',
    "service_id" varchar(32) COLLATE "pg_catalog"."default",
    "server_host" varchar(255) COLLATE "pg_catalog"."default",
    "server_ip" varchar(255) COLLATE "pg_catalog"."default",
    "env" varchar(255) COLLATE "pg_catalog"."default",
    "api_key_id" int8,
    "method" varchar(10) COLLATE "pg_catalog"."default",
    "request_uri" varchar(255) COLLATE "pg_catalog"."default",
    "user_agent" varchar(1000) COLLATE "pg_catalog"."default",
    "remote_ip" varchar(255) COLLATE "pg_catalog"."default",
    "params" text COLLATE "pg_catalog"."default",
    "time" varchar(64) COLLATE "pg_catalog"."default",
    "create_by" varchar(64) COLLATE "pg_catalog"."default",
    "create_time" timestamp(6),
    PRIMARY KEY ("id")
);

COMMENT ON COLUMN "blade_api_key_log"."id" IS '编号';

COMMENT ON COLUMN "blade_api_key_log"."tenant_id" IS '租户ID';

COMMENT ON COLUMN "blade_api_key_log"."service_id" IS '服务ID';

COMMENT ON COLUMN "blade_api_key_log"."server_host" IS '服务器名';

COMMENT ON COLUMN "blade_api_key_log"."server_ip" IS '服务器IP地址';

COMMENT ON COLUMN "blade_api_key_log"."env" IS '服务器环境';

COMMENT ON COLUMN "blade_api_key_log"."api_key_id" IS 'API Key主键ID';

COMMENT ON COLUMN "blade_api_key_log"."method" IS '操作方式';

COMMENT ON COLUMN "blade_api_key_log"."request_uri" IS '请求URI';

COMMENT ON COLUMN "blade_api_key_log"."user_agent" IS '用户代理';

COMMENT ON COLUMN "blade_api_key_log"."remote_ip" IS '操作IP地址';

COMMENT ON COLUMN "blade_api_key_log"."params" IS '操作提交的数据';

COMMENT ON COLUMN "blade_api_key_log"."time" IS '执行时间';

COMMENT ON COLUMN "blade_api_key_log"."create_by" IS '创建者';

COMMENT ON COLUMN "blade_api_key_log"."create_time" IS '创建时间';

COMMENT ON TABLE "blade_api_key_log" IS '令牌调用日志表';

-- -----------------------------------
-- 新增是否主页字段
-- -----------------------------------
ALTER TABLE "blade_top_menu"
    ADD COLUMN "is_main" int4 DEFAULT 0;

COMMENT ON COLUMN "blade_top_menu"."is_main" IS '是否主页';
