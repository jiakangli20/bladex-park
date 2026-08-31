package org.springblade.modules.miniapp.service;

import org.springblade.modules.miniapp.pojo.dto.MiniEnterpriseAuthDTO;
import java.util.List;
import java.util.Map;

public interface IMiniEnterpriseAuthService {
    Map<String,Object> context();
    Map<String,Object> submitCertification(MiniEnterpriseAuthDTO.Certification request);
    List<Map<String,Object>> myCertifications();
    Map<String,Object> submitParkApplication(MiniEnterpriseAuthDTO.ParkApplication request);
    Map<String,Object> submitJoin(MiniEnterpriseAuthDTO.Join request);
    List<Map<String,Object>> myJoins();
    void switchContext(MiniEnterpriseAuthDTO.SwitchContext request);
    Map<String,Object> currentInvite();
    Map<String,Object> createInvite(MiniEnterpriseAuthDTO.InviteSetting request);
    Map<String,Object> resolveInvite(String code);
    List<Map<String,Object>> ownerJoins(String status);
    void reviewJoin(Long id, MiniEnterpriseAuthDTO.Review request);
    List<Map<String,Object>> adminCertifications(String status);
    void reviewCertification(Long id, MiniEnterpriseAuthDTO.Review request);
}
