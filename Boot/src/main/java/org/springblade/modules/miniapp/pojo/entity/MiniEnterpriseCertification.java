package org.springblade.modules.miniapp.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("biz_mini_enterprise_certification")
public class MiniEnterpriseCertification {
    private Long id; private String tenantId;
    private Long applicantUserId;
    private Long enterpriseSubjectId;
    private String applicationType;
    private String subjectType;
    private String enterpriseName;
    private String creditCode;
    private String legalRepresentative;
    private BigDecimal registeredCapital;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    @com.baomidou.mybatisplus.annotation.TableField("status") private String processStatus;
    private Long reviewUserId;
    private Date reviewTime;
    private String reviewRemark;
    private Date createTime; private Date updateTime; private Integer isDeleted;
}
