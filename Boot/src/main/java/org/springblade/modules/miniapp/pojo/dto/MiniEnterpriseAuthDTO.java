package org.springblade.modules.miniapp.pojo.dto;

import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface MiniEnterpriseAuthDTO {
    @Data class Certification {
        @NotBlank(message="认证类型不能为空") @Pattern(regexp="ENTERPRISE|PERSONAL", message="认证类型不正确") private String subjectType;
        @NotBlank(message="企业名称不能为空") @Size(max=200) private String enterpriseName;
        @Size(max=32) private String creditCode;
        @Size(max=100) private String legalRepresentative;
        @DecimalMin(value="0", message="注册资本不能小于0") @Digits(integer=10, fraction=2, message="注册资本最多10位整数、2位小数") private BigDecimal registeredCapital;
        @NotBlank(message="联系人不能为空") private String contactName;
        @NotBlank(message="认证手机号不能为空") @Pattern(regexp="^1\\d{10}$", message="手机号格式不正确") private String contactPhone;
        @NotBlank(message="认证邮箱不能为空") @Email(message="邮箱格式不正确") private String contactEmail;
        @NotEmpty(message="至少选择一个园区") private List<Long> parkIds;
    }
    @Data class Join {
        @NotBlank(message="邀请码不能为空") private String inviteCode;
        @NotBlank(message="姓名不能为空") private String name;
        @NotBlank(message="手机号不能为空") @Pattern(regexp="^1\\d{10}$", message="手机号格式不正确") private String mobile;
        @NotBlank(message="邮箱不能为空") @Email(message="邮箱格式不正确") private String email;
        @NotBlank(message="证件类型不能为空") private String idType;
        @NotBlank(message="证件号不能为空") private String idNo;
        @NotNull(message="出生日期不能为空") @JsonFormat(pattern="yyyy-MM-dd") private Date birthDate;
        @NotBlank(message="性别不能为空") private String gender;
    }
    @Data class Review { @NotBlank(message="审核结果不能为空") private String action; private String remark; }
    @Data class ParkApplication { @NotEmpty(message="至少选择一个园区") private List<Long> parkIds; }
    @Data class SwitchContext { @NotNull private Long enterpriseSubjectId; @NotNull private Long parkId; }
    @Data class InviteSetting { @Min(1) @Max(720) private Integer validHours=72; @Min(1) @Max(10000) private Integer maxUses=1; }
}
