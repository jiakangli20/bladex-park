package org.springblade.modules.miniapp.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("biz_mini_enterprise_join_application")
public class MiniEnterpriseJoinApplication {
    private Long id; private String tenantId;
    private Long applicantUserId;
    private Long enterpriseSubjectId;
    private Long customerId;
    private Long parkId;
    private Long inviteId;
    private String name;
    private String mobile;
    private String email;
    private String idType;
    private String idNo;
    private Date birthDate;
    private String gender;
    @com.baomidou.mybatisplus.annotation.TableField("status") private String processStatus;
    private Long reviewUserId;
    private Date reviewTime;
    private String reviewRemark;
    private Date createTime; private Date updateTime; private Integer isDeleted;
}
