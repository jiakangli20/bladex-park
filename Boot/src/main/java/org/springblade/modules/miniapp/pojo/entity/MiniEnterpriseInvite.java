package org.springblade.modules.miniapp.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("biz_mini_enterprise_invite")
public class MiniEnterpriseInvite {
    private Long id; private String tenantId;
    private Long enterpriseSubjectId;
    private Long customerId;
    private Long parkId;
    private String inviteCode;
    private String codeHash;
    private Date expireTime;
    private Integer maxUses;
    private Integer usedCount;
    @com.baomidou.mybatisplus.annotation.TableField("status") private String processStatus;
    private Long createUser; private Date createTime; private Date updateTime; private Integer isDeleted;
}
