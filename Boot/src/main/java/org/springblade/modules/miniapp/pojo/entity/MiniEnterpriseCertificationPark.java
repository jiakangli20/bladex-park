package org.springblade.modules.miniapp.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("biz_mini_enterprise_certification_park")
public class MiniEnterpriseCertificationPark {
    private Long id; private String tenantId;
    private Long certificationId;
    private Long parkId;
    @com.baomidou.mybatisplus.annotation.TableField("status") private String processStatus;
    private Integer isDeleted;
}
