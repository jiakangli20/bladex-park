package org.springblade.modules.miniapp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.tool.api.R;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.modules.miniapp.pojo.dto.MiniEnterpriseAuthDTO;
import org.springblade.modules.miniapp.service.IMiniEnterpriseAuthService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name="小程序企业认证")
public class MiniEnterpriseAuthController extends BladeController {
    private final IMiniEnterpriseAuthService service;
    @GetMapping("/blade-miniapp/enterprise/context") @Operation(summary="企业及园区上下文") public R<Map<String,Object>> context(){return R.data(service.context());}
    @PostMapping("/blade-miniapp/enterprise/certifications") @Operation(summary="提交企业认证") public R<Map<String,Object>> certify(@Valid @RequestBody MiniEnterpriseAuthDTO.Certification req){return R.data(service.submitCertification(req));}
    @GetMapping("/blade-miniapp/enterprise/certifications") public R<List<Map<String,Object>>> myCertifications(){return R.data(service.myCertifications());}
    @PostMapping("/blade-miniapp/enterprise/park-applications") @Operation(summary="申请新增企业园区") public R<Map<String,Object>> applyPark(@Valid @RequestBody MiniEnterpriseAuthDTO.ParkApplication req){return R.data(service.submitParkApplication(req));}
    @PostMapping("/blade-miniapp/enterprise/joins") @Operation(summary="提交员工加入申请") public R<Map<String,Object>> join(@Valid @RequestBody MiniEnterpriseAuthDTO.Join req){return R.data(service.submitJoin(req));}
    @GetMapping("/blade-miniapp/enterprise/joins") public R<List<Map<String,Object>>> myJoins(){return R.data(service.myJoins());}
    @PostMapping("/blade-miniapp/enterprise/switch") public R<Void> switchContext(@Valid @RequestBody MiniEnterpriseAuthDTO.SwitchContext req){service.switchContext(req);return R.success("切换成功");}
    @GetMapping("/blade-miniapp/enterprise/invites/current") public R<Map<String,Object>> currentInvite(){return R.data(service.currentInvite());}
    @PostMapping("/blade-miniapp/enterprise/invites") public R<Map<String,Object>> invite(@Valid @RequestBody MiniEnterpriseAuthDTO.InviteSetting req){return R.data(service.createInvite(req));}
    @GetMapping("/blade-miniapp/enterprise/invites/resolve") public R<Map<String,Object>> resolveInvite(@RequestParam String code){return R.data(service.resolveInvite(code));}
    @GetMapping("/blade-miniapp/enterprise/owner-joins") public R<List<Map<String,Object>>> ownerJoins(@RequestParam(required=false) String status){return R.data(service.ownerJoins(status));}
    @GetMapping("/blade-miniapp/enterprise/pending-joins") public R<List<Map<String,Object>>> pendingJoins(){return R.data(service.ownerJoins("PENDING"));}
    @PostMapping("/blade-miniapp/enterprise/joins/{id}/review") public R<Void> reviewJoin(@PathVariable Long id,@Valid @RequestBody MiniEnterpriseAuthDTO.Review req){service.reviewJoin(id,req);return R.success("处理成功");}
    @GetMapping("/blade-park/enterprise-certification/list") public R<List<Map<String,Object>>> adminList(@RequestParam(required=false) String status){requireWebAdmin();return R.data(service.adminCertifications(status));}
    @PostMapping("/blade-park/enterprise-certification/{id}/review") public R<Void> adminReview(@PathVariable Long id,@Valid @RequestBody MiniEnterpriseAuthDTO.Review req){requireWebAdmin();service.reviewCertification(id,req);return R.success("审核成功");}
    private void requireWebAdmin(){if(!AuthUtil.isAdmin()&&!AuthUtil.isAdministrator()) throw new ServiceException("仅平台管理员可处理企业认证");}
}
