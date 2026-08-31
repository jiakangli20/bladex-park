package org.springblade.modules.miniapp.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("biz_enterprise_subject")
public class EnterpriseSubject {
    private Long id; private String tenantId;
    private String enterpriseName;
    private String enterpriseNameNorm;
    private String creditCode;
    private String enterpriseType;
    private String legalRepresentative;
    private BigDecimal registeredCapital;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private Long customerId;
    private Long ownerUserId;
    @com.baomidou.mybatisplus.annotation.TableField("status") private String processStatus;
    private Date createTime; private Date updateTime; private Integer isDeleted;
}
