package com.easy.marketgo.biz.service.wecom;

import cn.hutool.crypto.SecureUtil;
import com.easy.marketgo.common.enums.UserGroupAudienceTypeEnum;
import com.easy.marketgo.common.enums.WeComMassTaskSendStatusEnum;
import com.easy.marketgo.common.enums.WeComMassTaskStatus;
import com.easy.marketgo.common.enums.WeComMassTaskTypeEnum;
import com.easy.marketgo.common.utils.JsonUtils;
import com.easy.marketgo.common.utils.UuidUtils;
import com.easy.marketgo.core.entity.customer.WeComDepartmentEntity;
import com.easy.marketgo.core.entity.customer.WeComMemberMessageEntity;
import com.easy.marketgo.core.entity.customer.WeComRelationMemberExternalUserEntity;
import com.easy.marketgo.core.entity.masstask.WeComMassTaskEntity;
import com.easy.marketgo.core.entity.masstask.WeComMassTaskSendQueueEntity;
import com.easy.marketgo.core.entity.masstask.WeComUserGroupAudienceEntity;
import com.easy.marketgo.core.model.bo.QueryMemberBuildSqlParam;
import com.easy.marketgo.core.model.bo.QueryUserGroupBuildSqlParam;
import com.easy.marketgo.core.model.bo.WeComUserGroupAudienceRule;
import com.easy.marketgo.core.repository.wecom.WeComDepartmentRepository;
import com.easy.marketgo.core.repository.wecom.masstask.WeComMassTaskSendQueueRepository;
import com.easy.marketgo.core.repository.wecom.WeComUserGroupAudienceRepository;
import com.easy.marketgo.core.repository.wecom.customer.WeComMemberMessageRepository;
import com.easy.marketgo.core.repository.wecom.customer.WeComRelationMemberExternalUserRepository;
import com.easy.marketgo.core.repository.wecom.masstask.WeComMassTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author : kevinwang
 * @version : 1.0
 * @data : 7/28/22 9:58 PM
 * Describe:
 */
@Slf4j
@Service
public class QueryUserGroupDetailService {

    private static final Integer QUERY_USER_GROUP_TIME_BEFORE = 10;
    @Autowired
    private WeComMassTaskRepository weComMassTaskRepository;

    @Autowired
    private WeComUserGroupAudienceRepository weComUserGroupAudienceRepository;

    @Autowired
    private WeComRelationMemberExternalUserRepository weComRelationMemberExternalUserRepository;

    @Autowired
    private WeComDepartmentRepository weComDepartmentRepository;

    @Autowired
    private WeComMemberMessageRepository weComMemberMessageRepository;

    @Autowired
    private WeComMassTaskSendQueueRepository weComMassTaskSendQueueRepository;

    public void queryWeComMassTaskUserGroup(String taskType) {
//        Date date = DateUtil.date();
//        Date newDate = DateUtil.offset(date, DateField.MINUTE, 2);
//        LocalDateTime now = LocalDateTime.now();
//        String nowTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
//        weComMassTaskRepository.querytByStatusAndScheduleTime(WeComMassTaskTypeEnum.SINGLE.name(),
//                WeComMassTaskStatus.UNSTART.getValue(), nowTime);
        log.info("start query user group send mass task. taskType={}", taskType);
        List<WeComMassTaskEntity> entities =
                weComMassTaskRepository.getWeComMassTaskByScheduleTime(QUERY_USER_GROUP_TIME_BEFORE,
                        taskType, WeComMassTaskStatus.UNSTART.getValue());
        log.info("query send mass task. entities={}", entities);
        if (CollectionUtils.isEmpty(entities)) {
            log.info("query mass task is empty. taskType={}", taskType);
            return;
        }
        for (WeComMassTaskEntity entity : entities) {
            if (!entity.getTaskStatus().equals(WeComMassTaskStatus.UNSTART.getValue())) {
                log.info("task type is error for mass task. entity={}", entity);
                continue;
            }
            weComMassTaskRepository.updateTaskStatusByUUID(entity.getUuid(),
                    WeComMassTaskStatus.COMPUTING.getValue());
            WeComUserGroupAudienceEntity weComUserGroupAudienceEntity =
                    weComUserGroupAudienceRepository.queryWeComUserGroupAudienceEntityByUuid(entity.getUserGroupUuid());
            if (weComUserGroupAudienceEntity == null) {
                log.info("failed to query user group audience. userGroupUuid={}", entity.getUserGroupUuid());
                continue;
            }

            if (!weComUserGroupAudienceEntity.getUserGroupType().equals(UserGroupAudienceTypeEnum.WECOM_USER_GROUP.getValue())) {
                log.info("query user group not audience. weComUserGroupAudienceEntity={}",
                        weComUserGroupAudienceEntity);
                continue;
            }

            String conditions = weComUserGroupAudienceEntity.getWecomConditions();
            if (StringUtils.isBlank(conditions)) {
                log.error("query user group conditions is empty. weComUserGroupAudienceEntity={}",
                        weComUserGroupAudienceEntity);
                continue;
            }

            WeComUserGroupAudienceRule weComUserGroupAudienceRule = JsonUtils.toObject(conditions,
                    WeComUserGroupAudienceRule.class);
            log.info("query user group for mass task. weComUserGroupAudienceRule={}", weComUserGroupAudienceRule);
            try {
                if (entity.getTaskType().equals(WeComMassTaskTypeEnum.GROUP.name())) {
                    queryGroupMassTaskUserGroup(entity.getCorpId(), entity.getUuid(), weComUserGroupAudienceRule);
                } else if (entity.getTaskType().equals(WeComMassTaskTypeEnum.MOMENT.name())) {
                    queryMomentMassTaskUserGroup(entity.getCorpId(), entity.getUuid(), weComUserGroupAudienceRule);
                } else {
                    List<String> memberList = getMemberList(entity.getCorpId(), weComUserGroupAudienceRule);
                    log.info("query user group for member. memberList={}", memberList);
                    memberList = memberList.stream().distinct().collect(Collectors.toList());
                    log.info("query user group for member remove duplicate. memberList={}", memberList);
                    for (String member : memberList) {
                        List<WeComRelationMemberExternalUserEntity> externalUserEntities =
                                getExternalUsers(entity.getCorpId(), Arrays.asList(member), weComUserGroupAudienceRule);
                        log.info("user group for external user estimate total result. externalUserCount={}",
                                externalUserEntities.size());

                        Map<String, List<String>> memberRelationExternalUsers =
                                externalUserEntities.stream().collect(Collectors.groupingBy(WeComRelationMemberExternalUserEntity::getMemberId,
                                        Collectors.mapping(WeComRelationMemberExternalUserEntity::getExternalUserId,
                                                Collectors.toList())));
                        log.info("memberRelationExternalUsers result. memberRelationExternalUsers={}",
                                memberRelationExternalUsers.size());
                        List<WeComMassTaskSendQueueEntity> weComMassTaskSendQueueEntities = new ArrayList<>();
                        Iterator<Map.Entry<String, List<String>>> iterator =
                                memberRelationExternalUsers.entrySet().iterator();
                        while (iterator.hasNext()) {
                            WeComMassTaskSendQueueEntity weComMassTaskSendQueueEntity =
                                    new WeComMassTaskSendQueueEntity();
                            Map.Entry<String, List<String>> item = iterator.next();
                            weComMassTaskSendQueueEntity.setMemberId(item.getKey());
                            weComMassTaskSendQueueEntity.setUuid(UuidUtils.generateUuid());
                            weComMassTaskSendQueueEntity.setMemberMd5(SecureUtil.md5(item.getKey()));
                            weComMassTaskSendQueueEntity.setTaskUuid(entity.getUuid());
                            weComMassTaskSendQueueEntity.setExternalUserIds(item.getValue().stream().collect(Collectors.joining(",")));
                            weComMassTaskSendQueueEntity.setStatus(WeComMassTaskSendStatusEnum.UNSEND.name());
                            log.info("save mass task send queue. weComMassTaskSendQueueEntity={}",
                                    weComMassTaskSendQueueEntity);
                            weComMassTaskSendQueueEntities.add(weComMassTaskSendQueueEntity);
                        }

                        weComMassTaskSendQueueRepository.saveAll(weComMassTaskSendQueueEntities);
                    }
                }
                weComMassTaskRepository.updateTaskStatusByUUID(entity.getUuid(),
                        WeComMassTaskStatus.COMPUTED.getValue());
            } catch (Exception e) {
                log.info("failed to save mass task send queue. entity={}", entity, e);
            }
        }
    }

    private List<String> getMemberList(String corpId, WeComUserGroupAudienceRule weComUserGroupAudienceRule) {
        List<Long> departmentList = new ArrayList<>();
        List<String> memberList = new ArrayList<>();
        if (!weComUserGroupAudienceRule.getMembers().getIsAll()) {
            if (CollectionUtils.isNotEmpty(weComUserGroupAudienceRule.getMembers().getUsers())) {
                weComUserGroupAudienceRule.getMembers().getUsers().forEach(user -> {
                    memberList.add(user.getMemberId());
                });
            }

            if (CollectionUtils.isNotEmpty(weComUserGroupAudienceRule.getMembers().getDepartments())) {
                weComUserGroupAudienceRule.getMembers().getDepartments().forEach(department -> {
                    departmentList.add(department.getId());
                });

                List<WeComDepartmentEntity> departmentEntities =
                        weComDepartmentRepository.findByParentIdIn(departmentList);
                while (CollectionUtils.isNotEmpty(departmentEntities)) {
                    List<Long> tempDepartmentList = new ArrayList<>();
                    departmentEntities.forEach(departmentEntity -> {
                        tempDepartmentList.add(departmentEntity.getDepartmentId());
                    });
                    log.info("find department list. tempDepartmentList={}", tempDepartmentList);
                    departmentList.addAll(tempDepartmentList);
                    departmentEntities =
                            weComDepartmentRepository.findByParentIdIn(tempDepartmentList);
                }

                QueryMemberBuildSqlParam queryMemberBuildSqlParam =
                        QueryMemberBuildSqlParam.builder().corpId(corpId).departments(departmentList).build();
                List<WeComMemberMessageEntity> memberTmp =
                        weComMemberMessageRepository.listByParam(queryMemberBuildSqlParam);
                log.info("query user group for member from db count. memberTmp={}", memberTmp.size());
                if (CollectionUtils.isNotEmpty(memberTmp)) {
                    log.info("user group for member estimate result. memberCount={}", memberTmp.size());
                    memberTmp.forEach(item -> {
                        memberList.add(item.getMemberId());
                    });
                }
            }
        }
        return memberList.stream().distinct().collect(Collectors.toList());
    }

    private List<WeComRelationMemberExternalUserEntity> getExternalUsers(String corpId, List<String> memberList,
                                                                         WeComUserGroupAudienceRule weComUserGroupAudienceRule) {
        int offset = 0;
        int limitSize = 20000;
        List<WeComRelationMemberExternalUserEntity> entities = new ArrayList<>();
        do {
            QueryUserGroupBuildSqlParam paramExternalUser =
                    QueryUserGroupBuildSqlParam.builder().corpId(corpId).pageNum(offset).pageSize(limitSize).build();
            paramExternalUser.setMemberIds(memberList);
            WeComUserGroupAudienceRule.ExternalUserMessage externalUserMessage =
                    weComUserGroupAudienceRule.getExternalUsers();
            if (!externalUserMessage.getIsAll()) {

                //添加时间的条件
                if (externalUserMessage.isAddTimeSwitch()) {
                    paramExternalUser.setStartTime(externalUserMessage.getStartTime());
                    paramExternalUser.setEndTime(externalUserMessage.getEndTime());
                }

                //标签条件
                if (externalUserMessage.isCorpTagSwitch()
                        && externalUserMessage.getCorpTags() != null
                        && CollectionUtils.isNotEmpty(externalUserMessage.getCorpTags().getTags())) {
                    paramExternalUser.setTagRelation(externalUserMessage.getCorpTags().getRelation());
                    List<String> tags = new ArrayList<>();
                    externalUserMessage.getCorpTags().getTags().forEach(weComCorpTag -> {
                        tags.add(weComCorpTag.getId());
                    });

                    paramExternalUser.setTags(tags);
                }

                //性别条件
                if (externalUserMessage.isGenderSwitch() &&
                        CollectionUtils.isNotEmpty(externalUserMessage.getGenders())) {
                    paramExternalUser.setGenders(externalUserMessage.getGenders());
                }

                //客户群条件
                if (externalUserMessage.isGroupChatsSwitch() &&
                        CollectionUtils.isNotEmpty(externalUserMessage.getGroupChats())) {
                    List<String> groups = new ArrayList<>();
                    externalUserMessage.getGroupChats().forEach(group -> {
                        groups.add(group.getChatId());
                    });

                    paramExternalUser.setGroupChats(groups);
                }
            }

            List<WeComRelationMemberExternalUserEntity> externalUserEntities =
                    weComRelationMemberExternalUserRepository.listByUserGroupCnd(paramExternalUser);
            log.info("user group for external user estimate result. externalUserCount={}",
                    externalUserEntities.size());
            entities.addAll(externalUserEntities);
            offset = (externalUserEntities.size() < limitSize) ? 0 : (offset + limitSize);
        } while (offset > 0);
        return entities;
    }

    private void queryGroupMassTaskUserGroup(String corpId, String taskUuid,
                                             WeComUserGroupAudienceRule weComUserGroupAudienceRule) {
        List<String> memberList = getMemberList(corpId, weComUserGroupAudienceRule);
        memberList = memberList.stream().distinct().collect(Collectors.toList());
        List<WeComMassTaskSendQueueEntity> weComMassTaskSendQueueEntities = new ArrayList<>();
        memberList.forEach(member -> {
            WeComMassTaskSendQueueEntity weComMassTaskSendQueueEntity = new WeComMassTaskSendQueueEntity();
            weComMassTaskSendQueueEntity.setUuid(UuidUtils.generateUuid());
            weComMassTaskSendQueueEntity.setMemberMd5(member);
            weComMassTaskSendQueueEntity.setMemberId(member);
            weComMassTaskSendQueueEntity.setTaskUuid(taskUuid);
            weComMassTaskSendQueueEntity.setExternalUserIds("");
            weComMassTaskSendQueueEntity.setStatus(WeComMassTaskSendStatusEnum.UNSEND.name());
            weComMassTaskSendQueueEntities.add(weComMassTaskSendQueueEntity);
        });
        weComMassTaskSendQueueRepository.saveAll(weComMassTaskSendQueueEntities);
    }

    private void queryMomentMassTaskUserGroup(String corpId, String taskUuid,
                                              WeComUserGroupAudienceRule weComUserGroupAudienceRule) {
        List<String> memberList = getMemberList(corpId, weComUserGroupAudienceRule);
        WeComMassTaskSendQueueEntity weComMassTaskSendQueueEntity = new WeComMassTaskSendQueueEntity();
        if (CollectionUtils.isNotEmpty(memberList)) {
            String members = memberList.stream().collect(Collectors.joining(","));
            weComMassTaskSendQueueEntity.setMemberMd5(SecureUtil.md5(members));
            weComMassTaskSendQueueEntity.setMemberId(members);
        }

        weComMassTaskSendQueueEntity.setUuid(UuidUtils.generateUuid());

        weComMassTaskSendQueueEntity.setTaskUuid(taskUuid);

        if (!weComUserGroupAudienceRule.getExternalUsers().getIsAll() &&
                weComUserGroupAudienceRule.getExternalUsers().isCorpTagSwitch()
                && weComUserGroupAudienceRule.getExternalUsers().getCorpTags() != null &&
                CollectionUtils.isNotEmpty(weComUserGroupAudienceRule.getExternalUsers().getCorpTags().getTags())) {
            List<String> tags = new ArrayList<>();
            weComUserGroupAudienceRule.getExternalUsers().getCorpTags().getTags().forEach(tag -> {
                tags.add(tag.getId());
            });
            weComMassTaskSendQueueEntity.setExternalUserIds(tags.stream().collect(Collectors.joining(",")));
        }

        weComMassTaskSendQueueEntity.setStatus(WeComMassTaskSendStatusEnum.UNSEND.name());
        weComMassTaskSendQueueRepository.save(weComMassTaskSendQueueEntity);
    }
}
