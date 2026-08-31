package org.springblade.modules.miniapp.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 开发环境 mock 游客登录参数。 */
@Data
public class MiniMockLoginDTO {
    @NotBlank(message = "测试手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "测试手机号格式不正确")
    private String mobile;
    private String nickname;
}
