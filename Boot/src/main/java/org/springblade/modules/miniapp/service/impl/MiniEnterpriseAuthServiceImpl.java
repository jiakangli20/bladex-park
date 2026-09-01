package org.springblade.modules.miniapp.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springblade.core.log.exception.ServiceException;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.support.Kv;
import org.springblade.core.tool.utils.DigestUtil;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.business.pojo.entity.Customer;
import org.springblade.modules.business.service.ICustomerService;
import org.springblade.modules.miniapp.constant.MiniAppConstant;
import org.springblade.modules.miniapp.mapper.EnterpriseSubjectMapper;
import org.springblade.modules.miniapp.mapper.MiniCustomerMemberMapper;
import org.springblade.modules.miniapp.mapper.MiniEnterpriseCertificationMapper;
import org.springblade.modules.miniapp.mapper.MiniEnterpriseCertificationParkMapper;
import org.springblade.modules.miniapp.mapper.MiniEnterpriseInviteMapper;
import org.springblade.modules.miniapp.mapper.MiniEnterpriseJoinApplicationMapper;
import org.springblade.modules.miniapp.mapper.MiniMemberMapper;
import org.springblade.modules.miniapp.pojo.dto.MiniEnterpriseAuthDTO;
import org.springblade.modules.miniapp.pojo.entity.EnterpriseSubject;
import org.springblade.modules.miniapp.pojo.entity.MiniCustomerMember;
import org.springblade.modules.miniapp.pojo.entity.MiniEnterpriseCertification;
import org.springblade.modules.miniapp.pojo.entity.MiniEnterpriseCertificationPark;
import org.springblade.modules.miniapp.pojo.entity.MiniEnterpriseInvite;
import org.springblade.modules.miniapp.pojo.entity.MiniEnterpriseJoinApplication;
import org.springblade.modules.miniapp.pojo.entity.MiniMember;
import org.springblade.modules.miniapp.service.IMiniAuthService;
import org.springblade.modules.miniapp.service.IMiniEnterpriseAuthService;
import org.springblade.modules.park.pojo.entity.Park;
import org.springblade.modules.park.service.IParkService;
import org.springblade.modules.system.pojo.entity.Dept;
import org.springblade.modules.system.pojo.entity.User;
import org.springblade.modules.system.pojo.entity.UserDept;
import org.springblade.modules.system.service.IDeptService;
import org.springblade.modules.system.service.IUserDeptService;
import org.springblade.modules.system.service.IUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MiniEnterpriseAuthServiceImpl implements IMiniEnterpriseAuthService {

    private static final String OWNER = "OWNER";
    private static final String MEMBER = "MEMBER";
    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";
    private static final String ACTIVE = "ACTIVE";
    private static final String ADD_PARK = "ADD_PARK";

    private final IMiniAuthService authService;
    private final EnterpriseSubjectMapper subjectMapper;
    private final MiniEnterpriseCertificationMapper certificationMapper;
    private final MiniEnterpriseCertificationParkMapper certificationParkMapper;
    private final MiniEnterpriseInviteMapper inviteMapper;
    private final MiniEnterpriseJoinApplicationMapper joinMapper;
    private final MiniCustomerMemberMapper memberRelationMapper;
    private final MiniMemberMapper memberMapper;
    private final ICustomerService customerService;
    private final IParkService parkService;
    private final IDeptService deptService;
    private final IUserDeptService userDeptService;
    private final IUserService userService;

    private String tenant() {
        return AuthUtil.getTenantId() == null ? "000000" : AuthUtil.getTenantId();
    }

    private Long userId() {
        Long id = AuthUtil.getUserId();
        if (id == null || id <= 0) {
            throw new ServiceException("登录状态无效");
        }
        return id;
    }

    @Override
    public Map<String, Object> context() {
        MiniMember member = authService.currentMember();
        List<MiniCustomerMember> relations = activeRelations(userId());
        List<Map<String, Object>> enterprises = new ArrayList<>();
        for (MiniCustomerMember relation : relations) {
            EnterpriseSubject subject = subjectMapper.selectById(relation.getEnterpriseSubjectId());
            if (subject == null) {
                continue;
            }
            Park park = parkService.getById(relation.getParkId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("enterpriseSubjectId", idValue(subject.getId()));
            item.put("enterpriseName", subject.getEnterpriseName());
            item.put("roleCode", relation.getRoleCode());
            item.put("parkId", idValue(relation.getParkId()));
            item.put("parkName", park == null ? null : park.getName());
            item.put("customerId", idValue(relation.getCustomerId()));
            enterprises.add(item);
        }
        List<Map<String, Object>> parks = parkService.list(Wrappers.<Park>lambdaQuery().eq(Park::getStatus, "0"))
            .stream()
            .map(park -> Kv.create().set("id", idValue(park.getId())).set("name", park.getName()))
            .collect(Collectors.toList());
        Long currentSubjectId = relations.stream()
            .filter(relation -> Objects.equals(relation.getCustomerId(), member.getCustomerId())
                && Objects.equals(relation.getParkId(), member.getParkId()))
            .map(MiniCustomerMember::getEnterpriseSubjectId)
            .findFirst()
            .orElse(null);
        return Kv.create()
            .set("enterprises", enterprises)
            .set("parks", parks)
            .set("currentEnterpriseSubjectId", idValue(currentSubjectId))
            .set("currentParkId", idValue(member.getParkId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitCertification(MiniEnterpriseAuthDTO.Certification request) {
        Long applicantUserId = userId();
        String enterpriseName = request.getEnterpriseName().trim();
        String normalizedName = enterpriseName.toLowerCase(Locale.ROOT);
        boolean enterprise = "ENTERPRISE".equals(request.getSubjectType());
        if (enterprise && (StringUtil.isBlank(request.getCreditCode())
            || StringUtil.isBlank(request.getLegalRepresentative())
            || request.getRegisteredCapital() == null)) {
            throw new ServiceException("请填写完整工商信息");
        }
        boolean exists = subjectMapper.selectCount(Wrappers.<EnterpriseSubject>lambdaQuery()
            .eq(EnterpriseSubject::getTenantId, tenant())
            .eq(EnterpriseSubject::getEnterpriseNameNorm, normalizedName)
            .eq(EnterpriseSubject::getIsDeleted, 0)) > 0;
        boolean pending = certificationMapper.selectCount(Wrappers.<MiniEnterpriseCertification>lambdaQuery()
            .eq(MiniEnterpriseCertification::getTenantId, tenant())
            .eq(MiniEnterpriseCertification::getEnterpriseName, enterpriseName)
            .eq(MiniEnterpriseCertification::getProcessStatus, PENDING)
            .eq(MiniEnterpriseCertification::getIsDeleted, 0)) > 0;
        if (exists || pending) {
            throw new ServiceException("企业名称已存在或正在审核中");
        }
        validateParks(request.getParkIds());

        MiniEnterpriseCertification certification = new MiniEnterpriseCertification();
        certification.setId(IdWorker.getId());
        certification.setTenantId(tenant());
        certification.setApplicantUserId(applicantUserId);
        certification.setApplicationType("CERTIFICATION");
        certification.setSubjectType(request.getSubjectType());
        certification.setEnterpriseName(enterpriseName);
        certification.setCreditCode(enterprise ? request.getCreditCode().trim() : null);
        certification.setLegalRepresentative(enterprise ? request.getLegalRepresentative().trim() : null);
        certification.setRegisteredCapital(enterprise ? request.getRegisteredCapital() : null);
        certification.setContactName(request.getContactName().trim());
        certification.setContactPhone(request.getContactPhone().trim());
        certification.setContactEmail(request.getContactEmail().trim());
        certification.setProcessStatus(PENDING);
        certification.setCreateTime(new Date());
        certification.setIsDeleted(0);
        certificationMapper.insert(certification);
        for (Long parkId : request.getParkIds()) {
            MiniEnterpriseCertificationPark relation = new MiniEnterpriseCertificationPark();
            relation.setId(IdWorker.getId());
            relation.setTenantId(tenant());
            relation.setCertificationId(certification.getId());
            relation.setParkId(parkId);
            relation.setProcessStatus(PENDING);
            relation.setIsDeleted(0);
            certificationParkMapper.insert(relation);
        }
        return Kv.create().set("id", idValue(certification.getId())).set("status", PENDING);
    }

    @Override
    public List<Map<String, Object>> myCertifications() {
        return certificationMapper.selectList(Wrappers.<MiniEnterpriseCertification>lambdaQuery()
                .eq(MiniEnterpriseCertification::getTenantId, tenant())
                .eq(MiniEnterpriseCertification::getApplicantUserId, userId())
                .eq(MiniEnterpriseCertification::getIsDeleted, 0)
                .orderByDesc(MiniEnterpriseCertification::getCreateTime))
            .stream()
            .map(certification -> Kv.create()
                .set("id", idValue(certification.getId()))
                .set("enterpriseSubjectId", idValue(certification.getEnterpriseSubjectId()))
                .set("enterpriseName", certification.getEnterpriseName())
                .set("applicationType", certification.getApplicationType())
                .set("parkNames", certificationParkNames(certification.getId()))
                .set("status", certification.getProcessStatus())
                .set("reviewRemark", certification.getReviewRemark()))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitParkApplication(MiniEnterpriseAuthDTO.ParkApplication request) {
        MiniCustomerMember owner = currentOwnerRelation(authService.currentMember());
        validateParks(request.getParkIds());
        EnterpriseSubject subject = subjectMapper.selectById(owner.getEnterpriseSubjectId());
        if (subject == null || !ACTIVE.equals(subject.getProcessStatus())) {
            throw new ServiceException("当前企业主体不存在或已停用");
        }
        List<Long> requestedParkIds = request.getParkIds().stream().distinct().toList();
        List<Long> existingParkIds = memberRelationMapper.selectList(Wrappers.<MiniCustomerMember>lambdaQuery()
                .eq(MiniCustomerMember::getTenantId, tenant())
                .eq(MiniCustomerMember::getEnterpriseSubjectId, subject.getId())
                .eq(MiniCustomerMember::getStatus, 1)
                .eq(MiniCustomerMember::getIsDeleted, 0))
            .stream().map(MiniCustomerMember::getParkId).filter(Objects::nonNull).distinct().toList();
        if (requestedParkIds.stream().anyMatch(existingParkIds::contains)) {
            throw new ServiceException("所选园区中包含企业已入驻园区");
        }
        List<Long> pendingIds = certificationMapper.selectList(Wrappers.<MiniEnterpriseCertification>lambdaQuery()
                .eq(MiniEnterpriseCertification::getTenantId, tenant())
                .eq(MiniEnterpriseCertification::getEnterpriseSubjectId, subject.getId())
                .eq(MiniEnterpriseCertification::getApplicationType, ADD_PARK)
                .eq(MiniEnterpriseCertification::getProcessStatus, PENDING)
                .eq(MiniEnterpriseCertification::getIsDeleted, 0))
            .stream().map(MiniEnterpriseCertification::getId).toList();
        if (!pendingIds.isEmpty() && certificationParkMapper.selectCount(Wrappers.<MiniEnterpriseCertificationPark>lambdaQuery()
            .in(MiniEnterpriseCertificationPark::getCertificationId, pendingIds)
            .in(MiniEnterpriseCertificationPark::getParkId, requestedParkIds)
            .eq(MiniEnterpriseCertificationPark::getIsDeleted, 0)) > 0) {
            throw new ServiceException("所选园区已有待审核申请");
        }

        MiniEnterpriseCertification certification = new MiniEnterpriseCertification();
        certification.setId(IdWorker.getId());
        certification.setTenantId(tenant());
        certification.setApplicantUserId(userId());
        certification.setEnterpriseSubjectId(subject.getId());
        certification.setApplicationType(ADD_PARK);
        certification.setSubjectType(subject.getEnterpriseType());
        certification.setEnterpriseName(subject.getEnterpriseName());
        certification.setCreditCode(subject.getCreditCode());
        certification.setLegalRepresentative(subject.getLegalRepresentative());
        certification.setRegisteredCapital(subject.getRegisteredCapital());
        certification.setContactName(subject.getContactName());
        certification.setContactPhone(subject.getContactPhone());
        certification.setContactEmail(subject.getContactEmail());
        certification.setProcessStatus(PENDING);
        certification.setCreateTime(new Date());
        certification.setIsDeleted(0);
        certificationMapper.insert(certification);
        for (Long parkId : requestedParkIds) {
            MiniEnterpriseCertificationPark relation = new MiniEnterpriseCertificationPark();
            relation.setId(IdWorker.getId());
            relation.setTenantId(tenant());
            relation.setCertificationId(certification.getId());
            relation.setParkId(parkId);
            relation.setProcessStatus(PENDING);
            relation.setIsDeleted(0);
            certificationParkMapper.insert(relation);
        }
        return Kv.create().set("id", idValue(certification.getId())).set("status", PENDING);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> submitJoin(MiniEnterpriseAuthDTO.Join request) {
        Long applicantUserId = userId();
        MiniEnterpriseInvite invite = usableInvite(request.getInviteCode(), true);
        boolean joined = memberRelationMapper.selectCount(Wrappers.<MiniCustomerMember>lambdaQuery()
            .eq(MiniCustomerMember::getTenantId, tenant())
            .eq(MiniCustomerMember::getUserId, applicantUserId)
            .eq(MiniCustomerMember::getEnterpriseSubjectId, invite.getEnterpriseSubjectId())
            .eq(MiniCustomerMember::getParkId, invite.getParkId())
            .eq(MiniCustomerMember::getStatus, 1)
            .eq(MiniCustomerMember::getIsDeleted, 0)) > 0;
        if (joined) {
            throw new ServiceException("您已加入该企业园区");
        }
        boolean pending = joinMapper.selectCount(Wrappers.<MiniEnterpriseJoinApplication>lambdaQuery()
            .eq(MiniEnterpriseJoinApplication::getTenantId, tenant())
            .eq(MiniEnterpriseJoinApplication::getApplicantUserId, applicantUserId)
            .eq(MiniEnterpriseJoinApplication::getEnterpriseSubjectId, invite.getEnterpriseSubjectId())
            .eq(MiniEnterpriseJoinApplication::getParkId, invite.getParkId())
            .eq(MiniEnterpriseJoinApplication::getProcessStatus, PENDING)
            .eq(MiniEnterpriseJoinApplication::getIsDeleted, 0)) > 0;
        if (pending) {
            throw new ServiceException("已提交过该企业申请，请等待审核");
        }

        int usedCount = invite.getUsedCount() == null ? 0 : invite.getUsedCount();
        invite.setUsedCount(usedCount + 1);
        if (invite.getUsedCount() >= invite.getMaxUses()) {
            invite.setProcessStatus("EXHAUSTED");
        }
        invite.setUpdateTime(new Date());
        inviteMapper.updateById(invite);

        MiniEnterpriseJoinApplication application = new MiniEnterpriseJoinApplication();
        application.setId(IdWorker.getId());
        application.setTenantId(tenant());
        application.setApplicantUserId(applicantUserId);
        application.setEnterpriseSubjectId(invite.getEnterpriseSubjectId());
        application.setCustomerId(invite.getCustomerId());
        application.setParkId(invite.getParkId());
        application.setInviteId(invite.getId());
        application.setName(request.getName().trim());
        application.setMobile(request.getMobile().trim());
        application.setEmail(request.getEmail().trim());
        application.setIdType(request.getIdType().trim());
        application.setIdNo(request.getIdNo().trim());
        application.setBirthDate(request.getBirthDate());
        application.setGender(request.getGender().trim());
        application.setProcessStatus(PENDING);
        application.setCreateTime(new Date());
        application.setIsDeleted(0);
        joinMapper.insert(application);
        return Kv.create().set("id", idValue(application.getId())).set("status", PENDING);
    }

    @Override
    public List<Map<String, Object>> myJoins() {
        return joinMapper.selectList(Wrappers.<MiniEnterpriseJoinApplication>lambdaQuery()
                .eq(MiniEnterpriseJoinApplication::getTenantId, tenant())
                .eq(MiniEnterpriseJoinApplication::getApplicantUserId, userId())
                .eq(MiniEnterpriseJoinApplication::getIsDeleted, 0)
                .orderByDesc(MiniEnterpriseJoinApplication::getCreateTime))
            .stream()
            .map(this::joinView)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void switchContext(MiniEnterpriseAuthDTO.SwitchContext request) {
        MiniCustomerMember relation = memberRelationMapper.selectOne(Wrappers.<MiniCustomerMember>lambdaQuery()
            .eq(MiniCustomerMember::getTenantId, tenant())
            .eq(MiniCustomerMember::getUserId, userId())
            .eq(MiniCustomerMember::getEnterpriseSubjectId, request.getEnterpriseSubjectId())
            .eq(MiniCustomerMember::getParkId, request.getParkId())
            .eq(MiniCustomerMember::getStatus, 1)
            .eq(MiniCustomerMember::getIsDeleted, 0)
            .last("limit 1"));
        if (relation == null) {
            throw new ServiceException("无权访问该企业或园区");
        }
        MiniMember member = currentMemberByUser();
        applyMemberContext(member, relation);
    }

    @Override
    public Map<String, Object> currentInvite() {
        MiniCustomerMember owner = currentOwnerRelation(authService.currentMember());
        MiniEnterpriseInvite invite = inviteMapper.selectOne(Wrappers.<MiniEnterpriseInvite>lambdaQuery()
            .eq(MiniEnterpriseInvite::getTenantId, tenant())
            .eq(MiniEnterpriseInvite::getEnterpriseSubjectId, owner.getEnterpriseSubjectId())
            .eq(MiniEnterpriseInvite::getParkId, owner.getParkId())
            .eq(MiniEnterpriseInvite::getIsDeleted, 0)
            .last("limit 1"));
        return inviteView(invite, owner);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createInvite(MiniEnterpriseAuthDTO.InviteSetting request) {
        MiniMember member = authService.currentMember();
        MiniCustomerMember owner = currentOwnerRelation(member);
        MiniEnterpriseInvite invite = inviteMapper.selectOne(Wrappers.<MiniEnterpriseInvite>lambdaQuery()
            .eq(MiniEnterpriseInvite::getTenantId, tenant())
            .eq(MiniEnterpriseInvite::getEnterpriseSubjectId, owner.getEnterpriseSubjectId())
            .eq(MiniEnterpriseInvite::getParkId, owner.getParkId())
            .eq(MiniEnterpriseInvite::getIsDeleted, 0)
            .last("limit 1"));
        String code = newInviteCode();
        Date now = new Date();
        if (invite == null) {
            invite = new MiniEnterpriseInvite();
            invite.setId(IdWorker.getId());
            invite.setTenantId(tenant());
            invite.setEnterpriseSubjectId(owner.getEnterpriseSubjectId());
            invite.setCustomerId(owner.getCustomerId());
            invite.setParkId(owner.getParkId());
            invite.setCreateUser(userId());
            invite.setCreateTime(now);
            invite.setIsDeleted(0);
        }
        invite.setInviteCode(code);
        invite.setCodeHash(DigestUtil.sha256Hex(code));
        invite.setExpireTime(new Date(now.getTime() + request.getValidHours() * 3_600_000L));
        invite.setMaxUses(request.getMaxUses());
        invite.setUsedCount(0);
        invite.setProcessStatus(ACTIVE);
        invite.setUpdateTime(now);
        if (inviteMapper.selectById(invite.getId()) == null) {
            inviteMapper.insert(invite);
        } else {
            inviteMapper.updateById(invite);
        }
        return inviteView(invite, owner);
    }

    @Override
    public Map<String, Object> resolveInvite(String code) {
        MiniEnterpriseInvite invite = usableInvite(code, false);
        EnterpriseSubject subject = subjectMapper.selectById(invite.getEnterpriseSubjectId());
        Park park = parkService.getById(invite.getParkId());
        return Kv.create()
            .set("enterpriseSubjectId", idValue(invite.getEnterpriseSubjectId()))
            .set("enterpriseName", subject == null ? null : subject.getEnterpriseName())
            .set("parkId", idValue(invite.getParkId()))
            .set("parkName", park == null ? null : park.getName())
            .set("expireTime", invite.getExpireTime())
            .set("remainingUses", Math.max(0, invite.getMaxUses() - invite.getUsedCount()));
    }

    @Override
    public List<Map<String, Object>> ownerJoins(String status) {
        MiniCustomerMember owner = currentOwnerRelation(authService.currentMember());
        var query = Wrappers.<MiniEnterpriseJoinApplication>lambdaQuery()
            .eq(MiniEnterpriseJoinApplication::getTenantId, tenant())
            .eq(MiniEnterpriseJoinApplication::getEnterpriseSubjectId, owner.getEnterpriseSubjectId())
            .eq(MiniEnterpriseJoinApplication::getParkId, owner.getParkId())
            .eq(MiniEnterpriseJoinApplication::getIsDeleted, 0)
            .orderByDesc(MiniEnterpriseJoinApplication::getCreateTime);
        if (StringUtil.isNotBlank(status) && !"ALL".equalsIgnoreCase(status)) {
            String normalized = status.trim().toUpperCase(Locale.ROOT);
            if (!List.of(PENDING, APPROVED, REJECTED).contains(normalized)) {
                throw new ServiceException("申请状态不正确");
            }
            query.eq(MiniEnterpriseJoinApplication::getProcessStatus, normalized);
        }
        return joinMapper.selectList(query).stream().map(this::joinView).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewJoin(Long id, MiniEnterpriseAuthDTO.Review request) {
        String resultStatus = reviewStatus(request.getAction());
        MiniCustomerMember owner = currentOwnerRelation(authService.currentMember());
        MiniEnterpriseJoinApplication application = joinMapper.selectById(id);
        if (application == null || !PENDING.equals(application.getProcessStatus())) {
            throw new ServiceException("申请不存在或已处理");
        }
        if (!Objects.equals(application.getEnterpriseSubjectId(), owner.getEnterpriseSubjectId())
            || !Objects.equals(application.getParkId(), owner.getParkId())) {
            throw new ServiceException("无权审核其他企业或园区的申请");
        }
        application.setProcessStatus(resultStatus);
        application.setReviewUserId(userId());
        application.setReviewTime(new Date());
        application.setReviewRemark(request.getRemark());
        joinMapper.updateById(application);
        if (!APPROVED.equals(resultStatus)) {
            return;
        }
        MiniMember applicant = memberMapper.selectOne(Wrappers.<MiniMember>lambdaQuery()
            .eq(MiniMember::getTenantId, tenant())
            .eq(MiniMember::getUserId, application.getApplicantUserId())
            .eq(MiniMember::getIsDeleted, 0)
            .last("limit 1"));
        if (applicant == null) {
            throw new ServiceException("申请人小程序账号不存在");
        }
        boolean exists = memberRelationMapper.selectCount(Wrappers.<MiniCustomerMember>lambdaQuery()
            .eq(MiniCustomerMember::getTenantId, tenant())
            .eq(MiniCustomerMember::getUserId, application.getApplicantUserId())
            .eq(MiniCustomerMember::getEnterpriseSubjectId, application.getEnterpriseSubjectId())
            .eq(MiniCustomerMember::getParkId, application.getParkId())
            .eq(MiniCustomerMember::getStatus, 1)
            .eq(MiniCustomerMember::getIsDeleted, 0)) > 0;
        if (exists) {
            throw new ServiceException("申请人已加入当前企业园区");
        }
        MiniCustomerMember relation = new MiniCustomerMember();
        relation.setId(IdWorker.getId());
        relation.setTenantId(tenant());
        relation.setMemberId(applicant.getId());
        relation.setUserId(application.getApplicantUserId());
        relation.setEnterpriseSubjectId(application.getEnterpriseSubjectId());
        relation.setCustomerId(application.getCustomerId());
        relation.setParkId(application.getParkId());
        relation.setRoleCode(MEMBER);
        relation.setMobile(application.getMobile());
        relation.setJoinSource("INVITE");
        relation.setInviteId(application.getInviteId());
        relation.setJoinTime(new Date());
        relation.setStatus(1);
        relation.setIsDeleted(0);
        memberRelationMapper.insert(relation);
        if (applicant.getCustomerId() == null) {
            applyMemberContext(applicant, relation);
        }
        markParkEnterprise(application.getApplicantUserId());
    }

    @Override
    public List<Map<String, Object>> adminCertifications(String status) {
        String normalized = StringUtil.isBlank(status) ? PENDING : status.trim().toUpperCase(Locale.ROOT);
        return certificationMapper.selectList(Wrappers.<MiniEnterpriseCertification>lambdaQuery()
                .eq(MiniEnterpriseCertification::getTenantId, tenant())
                .eq(MiniEnterpriseCertification::getProcessStatus, normalized)
                .eq(MiniEnterpriseCertification::getIsDeleted, 0)
                .orderByAsc(MiniEnterpriseCertification::getCreateTime))
            .stream()
            .map(certification -> Kv.create()
                .set("id", idValue(certification.getId()))
                .set("applicationType", certification.getApplicationType())
                .set("subjectType", certification.getSubjectType())
                .set("enterpriseName", certification.getEnterpriseName())
                .set("creditCode", certification.getCreditCode())
                .set("legalRepresentative", certification.getLegalRepresentative())
                .set("registeredCapital", certification.getRegisteredCapital())
                .set("contactName", certification.getContactName())
                .set("contactPhone", certification.getContactPhone())
                .set("contactEmail", certification.getContactEmail())
                .set("parkNames", certificationParkNames(certification.getId()))
                .set("status", certification.getProcessStatus()))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewCertification(Long id, MiniEnterpriseAuthDTO.Review request) {
        String resultStatus = reviewStatus(request.getAction());
        MiniEnterpriseCertification certification = certificationMapper.selectById(id);
        if (certification == null || !PENDING.equals(certification.getProcessStatus())) {
            throw new ServiceException("认证申请不存在或已处理");
        }
        certification.setProcessStatus(resultStatus);
        certification.setReviewUserId(userId());
        certification.setReviewTime(new Date());
        certification.setReviewRemark(request.getRemark());
        certificationMapper.updateById(certification);
        List<MiniEnterpriseCertificationPark> parks = certificationParkMapper.selectList(
            Wrappers.<MiniEnterpriseCertificationPark>lambdaQuery()
                .eq(MiniEnterpriseCertificationPark::getCertificationId, certification.getId())
                .eq(MiniEnterpriseCertificationPark::getIsDeleted, 0));
        for (MiniEnterpriseCertificationPark park : parks) {
            park.setProcessStatus(resultStatus);
            certificationParkMapper.updateById(park);
        }
        if (!APPROVED.equals(resultStatus)) {
            return;
        }

        MiniMember member = memberMapper.selectOne(Wrappers.<MiniMember>lambdaQuery()
            .eq(MiniMember::getTenantId, tenant())
            .eq(MiniMember::getUserId, certification.getApplicantUserId())
            .eq(MiniMember::getIsDeleted, 0)
            .last("limit 1"));
        if (member == null) {
            throw new ServiceException("认证申请人的小程序账号不存在");
        }
        boolean addPark = ADD_PARK.equals(certification.getApplicationType());
        EnterpriseSubject subject = addPark ? subjectMapper.selectById(certification.getEnterpriseSubjectId()) : new EnterpriseSubject();
        if (addPark && (subject == null || !Objects.equals(subject.getOwnerUserId(), certification.getApplicantUserId()))) {
            throw new ServiceException("新增园区申请的企业主体不存在或申请人不是企业管理员");
        }
        if (!addPark) {
            subject.setId(IdWorker.getId());
            subject.setTenantId(tenant());
            subject.setEnterpriseName(certification.getEnterpriseName());
            subject.setEnterpriseNameNorm(certification.getEnterpriseName().trim().toLowerCase(Locale.ROOT));
            subject.setCreditCode(certification.getCreditCode());
            subject.setEnterpriseType(certification.getSubjectType());
            subject.setLegalRepresentative(certification.getLegalRepresentative());
            subject.setRegisteredCapital(certification.getRegisteredCapital());
            subject.setContactName(certification.getContactName());
            subject.setContactPhone(certification.getContactPhone());
            subject.setContactEmail(certification.getContactEmail());
            subject.setOwnerUserId(certification.getApplicantUserId());
            subject.setProcessStatus(ACTIVE);
            subject.setCreateTime(new Date());
            subject.setIsDeleted(0);
            subjectMapper.insert(subject);
            certification.setEnterpriseSubjectId(subject.getId());
            certificationMapper.updateById(certification);
        }

        MiniCustomerMember firstRelation = null;
        Long firstCustomerId = null;
        for (MiniEnterpriseCertificationPark park : parks) {
            boolean exists = memberRelationMapper.selectCount(Wrappers.<MiniCustomerMember>lambdaQuery()
                .eq(MiniCustomerMember::getTenantId, tenant())
                .eq(MiniCustomerMember::getEnterpriseSubjectId, subject.getId())
                .eq(MiniCustomerMember::getParkId, park.getParkId())
                .eq(MiniCustomerMember::getStatus, 1)
                .eq(MiniCustomerMember::getIsDeleted, 0)) > 0;
            if (exists) {
                throw new ServiceException("企业已拥有所申请园区权限");
            }
            Customer customer = new Customer();
            customer.setEnterpriseName(certification.getEnterpriseName());
            customer.setCreditCode(certification.getCreditCode());
            customer.setLegalRepresentative(certification.getLegalRepresentative());
            customer.setRegisteredCapital(certification.getRegisteredCapital());
            customer.setEnterpriseType(certification.getSubjectType());
            customer.setContactName(certification.getContactName());
            customer.setContactPhone(certification.getContactPhone());
            customer.setContactEmail(certification.getContactEmail());
            customer.setParkId(park.getParkId());
            customer.setStatus("0");
            customer.setDelFlag("0");
            Customer saved = customerService.insertCertifiedCustomer(customer);

            MiniCustomerMember relation = new MiniCustomerMember();
            relation.setId(IdWorker.getId());
            relation.setTenantId(tenant());
            relation.setMemberId(member.getId());
            relation.setUserId(certification.getApplicantUserId());
            relation.setEnterpriseSubjectId(subject.getId());
            relation.setCustomerId(saved.getCustomerId());
            relation.setParkId(park.getParkId());
            relation.setRoleCode(OWNER);
            relation.setMobile(certification.getContactPhone());
            relation.setJoinSource(addPark ? ADD_PARK : "CERTIFICATION");
            relation.setCertificationId(certification.getId());
            relation.setJoinTime(new Date());
            relation.setStatus(1);
            relation.setIsDeleted(0);
            memberRelationMapper.insert(relation);
            if (firstRelation == null) {
                firstRelation = relation;
                firstCustomerId = saved.getCustomerId();
            }
        }
        if (!addPark && firstCustomerId != null) {
            subject.setCustomerId(firstCustomerId);
            subjectMapper.updateById(subject);
        }
        if (member.getCustomerId() == null && firstRelation != null) {
            applyMemberContext(member, firstRelation);
        }
        markParkEnterprise(certification.getApplicantUserId());
    }

    private List<MiniCustomerMember> activeRelations(Long uid) {
        return memberRelationMapper.selectList(Wrappers.<MiniCustomerMember>lambdaQuery()
            .eq(MiniCustomerMember::getTenantId, tenant())
            .eq(MiniCustomerMember::getUserId, uid)
            .eq(MiniCustomerMember::getStatus, 1)
            .eq(MiniCustomerMember::getIsDeleted, 0)
            .orderByAsc(MiniCustomerMember::getJoinTime));
    }

    private MiniMember currentMemberByUser() {
        MiniMember member = memberMapper.selectOne(Wrappers.<MiniMember>lambdaQuery()
            .eq(MiniMember::getTenantId, tenant())
            .eq(MiniMember::getUserId, userId())
            .eq(MiniMember::getIsDeleted, 0)
            .last("limit 1"));
        if (member == null) {
            throw new ServiceException("小程序账号不存在");
        }
        return member;
    }

    private MiniCustomerMember currentOwnerRelation(MiniMember member) {
        if (member == null || member.getCustomerId() == null || member.getParkId() == null) {
            throw new ServiceException("请先切换到需要管理的企业和园区");
        }
        MiniCustomerMember relation = memberRelationMapper.selectOne(Wrappers.<MiniCustomerMember>lambdaQuery()
            .eq(MiniCustomerMember::getTenantId, tenant())
            .eq(MiniCustomerMember::getMemberId, member.getId())
            .eq(MiniCustomerMember::getCustomerId, member.getCustomerId())
            .eq(MiniCustomerMember::getParkId, member.getParkId())
            .eq(MiniCustomerMember::getRoleCode, OWNER)
            .eq(MiniCustomerMember::getStatus, 1)
            .eq(MiniCustomerMember::getIsDeleted, 0)
            .last("limit 1"));
        if (relation == null) {
            throw new ServiceException("仅当前企业园区的管理员可操作");
        }
        return relation;
    }

    private void applyMemberContext(MiniMember member, MiniCustomerMember relation) {
        member.setCustomerId(relation.getCustomerId());
        member.setParkId(relation.getParkId());
        member.setRoleCode(OWNER.equals(relation.getRoleCode())
            ? MiniAppConstant.ROLE_CUSTOMER_ADMIN : MiniAppConstant.ROLE_CUSTOMER_MEMBER);
        memberMapper.updateById(member);
    }

    private MiniEnterpriseInvite usableInvite(String code, boolean lock) {
        if (StringUtil.isBlank(code)) {
            throw new ServiceException("请输入邀请码");
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        String suffix = lock ? "limit 1 FOR UPDATE" : "limit 1";
        MiniEnterpriseInvite invite = inviteMapper.selectOne(Wrappers.<MiniEnterpriseInvite>lambdaQuery()
            .eq(MiniEnterpriseInvite::getTenantId, tenant())
            .eq(MiniEnterpriseInvite::getCodeHash, DigestUtil.sha256Hex(normalized))
            .eq(MiniEnterpriseInvite::getIsDeleted, 0)
            .last(suffix));
        if (invite == null || !ACTIVE.equals(invite.getProcessStatus())
            || invite.getExpireTime() == null || !invite.getExpireTime().after(new Date())
            || invite.getMaxUses() == null || invite.getUsedCount() == null
            || invite.getUsedCount() >= invite.getMaxUses()) {
            throw new ServiceException("邀请码无效、已过期或已用尽");
        }
        return invite;
    }

    private Map<String, Object> inviteView(MiniEnterpriseInvite invite, MiniCustomerMember owner) {
        EnterpriseSubject subject = subjectMapper.selectById(owner.getEnterpriseSubjectId());
        Park park = parkService.getById(owner.getParkId());
        int maxUses = invite == null || invite.getMaxUses() == null ? 0 : invite.getMaxUses();
        int usedCount = invite == null || invite.getUsedCount() == null ? 0 : invite.getUsedCount();
        return Kv.create()
            .set("enterpriseSubjectId", idValue(owner.getEnterpriseSubjectId()))
            .set("enterpriseName", subject == null ? null : subject.getEnterpriseName())
            .set("parkId", idValue(owner.getParkId()))
            .set("parkName", park == null ? null : park.getName())
            .set("code", invite == null ? null : invite.getInviteCode())
            .set("expireTime", invite == null ? null : invite.getExpireTime())
            .set("maxUses", maxUses)
            .set("usedCount", usedCount)
            .set("remainingUses", Math.max(0, maxUses - usedCount))
            .set("status", inviteStatus(invite));
    }

    private String inviteStatus(MiniEnterpriseInvite invite) {
        if (invite == null) {
            return "NONE";
        }
        if (invite.getExpireTime() == null || !invite.getExpireTime().after(new Date())) {
            return "EXPIRED";
        }
        if (invite.getMaxUses() == null || invite.getUsedCount() == null
            || invite.getUsedCount() >= invite.getMaxUses()) {
            return "EXHAUSTED";
        }
        return invite.getProcessStatus();
    }

    private Map<String, Object> joinView(MiniEnterpriseJoinApplication application) {
        EnterpriseSubject subject = subjectMapper.selectById(application.getEnterpriseSubjectId());
        Park park = parkService.getById(application.getParkId());
        return Kv.create()
            .set("id", idValue(application.getId()))
            .set("name", application.getName())
            .set("mobile", application.getMobile())
            .set("email", application.getEmail())
            .set("idType", application.getIdType())
            .set("idNo", application.getIdNo())
            .set("birthDate", application.getBirthDate())
            .set("gender", application.getGender())
            .set("enterpriseSubjectId", idValue(application.getEnterpriseSubjectId()))
            .set("enterpriseName", subject == null ? null : subject.getEnterpriseName())
            .set("parkId", idValue(application.getParkId()))
            .set("parkName", park == null ? null : park.getName())
            .set("status", application.getProcessStatus())
            .set("reviewRemark", application.getReviewRemark())
            .set("reviewTime", application.getReviewTime())
            .set("createTime", application.getCreateTime());
    }

    private List<String> certificationParkNames(Long certificationId) {
        return certificationParkMapper.selectList(Wrappers.<MiniEnterpriseCertificationPark>lambdaQuery()
                .eq(MiniEnterpriseCertificationPark::getCertificationId, certificationId)
                .eq(MiniEnterpriseCertificationPark::getIsDeleted, 0))
            .stream()
            .map(relation -> parkService.getById(relation.getParkId()))
            .filter(Objects::nonNull)
            .map(Park::getName)
            .collect(Collectors.toList());
    }

    private String newInviteCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String idValue(Long id) {
        return id == null ? null : String.valueOf(id);
    }

    private String reviewStatus(String action) {
        if ("APPROVE".equalsIgnoreCase(action)) {
            return APPROVED;
        }
        if ("REJECT".equalsIgnoreCase(action)) {
            return REJECTED;
        }
        throw new ServiceException("审核结果不正确");
    }

    private void markParkEnterprise(Long uid) {
        Dept dept = deptService.getOne(Wrappers.<Dept>lambdaQuery()
            .eq(Dept::getTenantId, tenant())
            .eq(Dept::getDeptName, "园区企业")
            .eq(Dept::getStatus, 1)
            .eq(Dept::getIsDeleted, 0)
            .last("limit 1"));
        if (dept != null && userService.getById(uid) != null) {
            User update = new User();
            update.setId(uid);
            update.setDeptId(String.valueOf(dept.getId()));
            userService.updateById(update);

            boolean hasParkEnterpriseDept = userDeptService.count(Wrappers.<UserDept>lambdaQuery()
                .eq(UserDept::getUserId, uid)
                .eq(UserDept::getDeptId, dept.getId())
                .eq(UserDept::getStatus, 1)
                .eq(UserDept::getIsDeleted, 0)) > 0;
            boolean hasOtherDept = userDeptService.count(Wrappers.<UserDept>lambdaQuery()
                .eq(UserDept::getUserId, uid)
                .ne(UserDept::getDeptId, dept.getId())
                .eq(UserDept::getIsDeleted, 0)) > 0;
            if (!hasParkEnterpriseDept || hasOtherDept) {
                userDeptService.remove(Wrappers.<UserDept>lambdaQuery().eq(UserDept::getUserId, uid));
                UserDept userDept = new UserDept();
                userDept.setUserId(uid);
                userDept.setDeptId(dept.getId());
                userDept.setStatus(1);
                userDept.setIsDeleted(0);
                userDeptService.save(userDept);
            }
        }
    }

    private void validateParks(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("至少选择一个园区");
        }
        for (Long id : ids) {
            Park park = parkService.getById(id);
            if (park == null || !"0".equals(park.getStatus())) {
                throw new ServiceException("园区不存在或已停用");
            }
        }
    }
}
