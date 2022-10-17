package com.easy.marketgo.web.service.wecom.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import com.easy.marketgo.api.model.request.WeComCheckAgentMessageRequest;
import com.easy.marketgo.api.model.request.customer.WeComQueryExternalUserDetailClientRequest;
import com.easy.marketgo.api.model.response.RpcResponse;
import com.easy.marketgo.api.model.response.customer.WeComQueryExternalUserDetailClientResponse;
import com.easy.marketgo.api.service.WeComAgentRpcService;
import com.easy.marketgo.api.service.WeComExternalUserRpcService;
import com.easy.marketgo.api.service.WeComMemberRpcService;
import com.easy.marketgo.biz.service.XxlJobManualTriggerService;
import com.easy.marketgo.common.constants.Constants;
import com.easy.marketgo.common.enums.ErrorCodeEnum;
import com.easy.marketgo.common.enums.WeComCorpConfigStepEnum;
import com.easy.marketgo.common.utils.RandomUtils;
import com.easy.marketgo.core.entity.WeComAgentMessageEntity;
import com.easy.marketgo.core.entity.WeComCorpMessageEntity;
import com.easy.marketgo.core.repository.wecom.WeComAgentMessageRepository;
import com.easy.marketgo.core.repository.wecom.WeComCorpMessageRepository;
import com.easy.marketgo.web.model.request.WeComAgentMessageRequest;
import com.easy.marketgo.web.model.request.WeComCorpMessageRequest;
import com.easy.marketgo.web.model.response.BaseResponse;
import com.easy.marketgo.web.model.response.corp.WeComCorpCallbackResponse;
import com.easy.marketgo.web.model.response.corp.WeComCorpConfigResponse;
import com.easy.marketgo.web.service.wecom.CorpMessageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author : kevinwang
 * @version : 1.0
 * @data : 6/22/22 10:49 PM
 * Describe:
 */
@Slf4j
@Service
public class CorpMessageServiceImpl implements CorpMessageService {
    @Resource
    private WeComAgentRpcService weComAgentRpcService;

    @Resource
    private WeComMemberRpcService weComMemberRpcService;

    @Resource
    private WeComExternalUserRpcService weComExternalUserRpcService;

    @Resource
    private WeComAgentMessageRepository weComAgentMessageRepository;

    @Resource
    private WeComCorpMessageRepository weComCorpMessageRepository;

    @Autowired
    private XxlJobManualTriggerService xxlJobManualTriggerService;

    @Override
    public BaseResponse checkAgentParams(String projectId, String corpId, String agentId, String secret) {
        WeComCheckAgentMessageRequest request = new WeComCheckAgentMessageRequest();
        request.setCorpId(corpId);
        request.setAgentId(agentId);
        request.setSecret(secret);
        RpcResponse response = weComAgentRpcService.checkAgentParams(request);
        log.info("check agent params. response={}", response);
        return BaseResponse.builder().code(response.getCode()).message(ErrorCodeEnum.getMessage(response.getCode(),
                response.getMessage())).build();
    }

    @Override
    public BaseResponse updateOrInsertCorpMessage(String projectId, WeComCorpMessageRequest weComCorpMessageRequest) {
        log.info("updateOrInsertCorpMessage params. projectId={}, weComCorpMessageRequest={}", projectId,
                weComCorpMessageRequest);
        BaseResponse response = BaseResponse.failure(ErrorCodeEnum.ERROR_WEB_AGENT_PARAM);
        try {
            if (weComCorpMessageRequest.getConfigType().equalsIgnoreCase(WeComCorpConfigStepEnum.CONTACTS_MSG.getValue())) {
                response = checkAgentParams(projectId, weComCorpMessageRequest.getCorp().getCorpId(),
                        Constants.AGENT_KEY_FOR_CONTACTS,
                        weComCorpMessageRequest.getContacts().getSecret());
                if (response.getCode().equals(ErrorCodeEnum.OK.getCode())) {
                    byte[] key = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue(), 256).getEncoded();
                    String encode = Base64.encodeWithoutPadding(key);
                    weComCorpMessageRepository.updateSecretByCorpId(weComCorpMessageRequest.getCorp().getCorpId(),
                            projectId,
                            weComCorpMessageRequest.getContacts().getSecret(),
                            RandomUtils.getRandomStr(16), encode);
                }
            } else if (weComCorpMessageRequest.getConfigType().equalsIgnoreCase(WeComCorpConfigStepEnum.EXTERNAL_USER_MSG.getValue())) {
                response = checkAgentParams(projectId, weComCorpMessageRequest.getCorp().getCorpId(),
                        Constants.AGENT_KEY_FOR_EXTERNALUSER,
                        weComCorpMessageRequest.getExternalUser().getSecret());
                if (response.getCode().equals(ErrorCodeEnum.OK.getCode())) {
                    byte[] key = SecureUtil.generateKey(SymmetricAlgorithm.AES.getValue(), 256).getEncoded();
                    String encode = Base64.encodeWithoutPadding(key);
                    weComCorpMessageRepository.updateExternalUserSecretByCorpId(weComCorpMessageRequest.getCorp().getCorpId(),
                            projectId, weComCorpMessageRequest.getExternalUser().getSecret(),
                            RandomUtils.getRandomStr(16), encode);
                    xxlJobManualTriggerService.manualTriggerHandler("syncCorpTags");
                    xxlJobManualTriggerService.manualTriggerHandler("syncMembers");
                    xxlJobManualTriggerService.manualTriggerHandler("syncGroupChats");
                }
            } else if (weComCorpMessageRequest.getConfigType().equalsIgnoreCase(WeComCorpConfigStepEnum.AGENT_MSG.getValue())) {
                response = checkAgentParams(projectId, weComCorpMessageRequest.getCorp().getCorpId(),
                        weComCorpMessageRequest.getAgent().getAgentId(),
                        weComCorpMessageRequest.getAgent().getSecret());
                if (response.getCode().equals(ErrorCodeEnum.OK.getCode())) {
                    WeComAgentMessageEntity weComAgentMessageEntity = new WeComAgentMessageEntity();
                    weComAgentMessageEntity.setProjectUuid(projectId);
                    weComAgentMessageEntity.setAgentId(weComCorpMessageRequest.getAgent().getAgentId());
                    weComAgentMessageEntity.setCorpId(weComCorpMessageRequest.getCorp().getCorpId());
                    weComAgentMessageEntity.setSecret(weComCorpMessageRequest.getAgent().getSecret());
                    weComAgentMessageEntity.setIsChief(Boolean.TRUE);
                    weComAgentMessageEntity.setName("营销助手");
                    weComAgentMessageEntity.setEnableStatus("enable");
                    weComAgentMessageEntity.setAuthStatus("auth");
                    weComAgentMessageEntity.setHomePage("/home");
                    weComAgentMessageRepository.save(weComAgentMessageEntity);

                    WeComCorpMessageEntity entity =
                            weComCorpMessageRepository.getCorpConfigByCorpId(weComCorpMessageRequest.getCorp().getCorpId());
                    if (entity == null) {
                        WeComCorpMessageEntity weComCorpMessageEntity = new WeComCorpMessageEntity();
                        weComCorpMessageEntity.setProjectUuid(projectId);
                        weComCorpMessageEntity.setCorpName(weComCorpMessageRequest.getCorp().getCorpName());
                        weComCorpMessageEntity.setCorpId(weComCorpMessageRequest.getCorp().getCorpId());
                        weComCorpMessageRepository.save(weComCorpMessageEntity);
                    }
                }
            } else {
                log.info("not support config type.projectId={}, weComCorpMessageRequest={}", projectId,
                        weComCorpMessageRequest);
            }
        } catch (Exception e) {
            log.error("failed to save config message.", e);
        }
        return response;
    }

    @Override
    public BaseResponse updateOrInsertAgentMessage(String projectId, WeComAgentMessageRequest agentMessageRequest) {
        if (StringUtils.isEmpty(projectId) || agentMessageRequest == null ||
                StringUtils.isEmpty(agentMessageRequest.getAgentId()) ||
                StringUtils.isEmpty(agentMessageRequest.getCorpId()) ||
                StringUtils.isEmpty(agentMessageRequest.getSecret())) {
            return BaseResponse.builder().code(ErrorCodeEnum.ERROR_WEB_AGENT_PARAM.getCode())
                    .message(ErrorCodeEnum.ERROR_WEB_AGENT_PARAM.getMessage()).build();
        }
        WeComAgentMessageEntity agentEntity =
                weComAgentMessageRepository.getWeComAgentByCorpAndAgent(agentMessageRequest.getCorpId(),
                        agentMessageRequest.getAgentId());
        if (agentEntity == null) {
            WeComAgentMessageEntity entity = new WeComAgentMessageEntity();
            entity.setAgentId(agentMessageRequest.getAgentId());
            entity.setCorpId(agentMessageRequest.getCorpId());
            entity.setSecret(agentMessageRequest.getSecret());
            weComAgentMessageRepository.save(entity);
        } else {
            weComAgentMessageRepository.updateAgentMessageByCorpId(agentMessageRequest.getCorpId(), projectId,
                    agentMessageRequest.getAgentId(), agentMessageRequest.getSecret());
        }
        return BaseResponse.builder().code(ErrorCodeEnum.OK.getCode()).message(ErrorCodeEnum.OK.getMessage()).build();
    }

    @Override
    public BaseResponse getCorpConfig(String projectId) {
        WeComCorpConfigResponse response = new WeComCorpConfigResponse();
        List<WeComCorpMessageEntity> entities = weComCorpMessageRepository.getCorpConfigListByProjectUuid(projectId);
        if (CollectionUtils.isEmpty(entities)) {
            return BaseResponse.success();
        }
        List<WeComCorpConfigResponse.ConfigMessage> configList = new ArrayList<>();
        for (WeComCorpMessageEntity item : entities) {
            WeComAgentMessageEntity agentEntity = weComAgentMessageRepository.getWeComAgentByCorp(projectId,
                    item.getCorpId());

            WeComCorpConfigResponse.ConfigMessage config = new WeComCorpConfigResponse.ConfigMessage();
            WeComCorpConfigResponse.CorpConfig corpConfig = new WeComCorpConfigResponse.CorpConfig();
            corpConfig.setCorpId(StringUtils.isEmpty(item.getCorpId()) ? "" : item.getCorpId());
            corpConfig.setCorpName(StringUtils.isEmpty(item.getCorpName()) ? "" : item.getCorpName());
            config.setCorp(corpConfig);

            if (agentEntity != null) {
                WeComCorpConfigResponse.AgentConfig agentConfig = new WeComCorpConfigResponse.AgentConfig();
                agentConfig.setAgentId(agentEntity.getAgentId());
                agentConfig.setSecret(agentEntity.getSecret());
                config.setAgent(agentConfig);
            }
            if (StringUtils.isNotEmpty(item.getContactsSecret())) {
                WeComCorpConfigResponse.ContactsConfig contactsConfig = new WeComCorpConfigResponse.ContactsConfig();
                contactsConfig.setEncodingAesKey(item.getContactsEncodingAesKey());
                contactsConfig.setToken(item.getContactsToken());
                contactsConfig.setUrl(Constants.WECOM_CALLBACK_CONSTACTS + item.getCorpId());
                contactsConfig.setSecret(item.getContactsSecret());
                config.setContacts(contactsConfig);
            }
            if (StringUtils.isNotEmpty(item.getExternalUserSecret())) {
                WeComCorpConfigResponse.ContactsConfig externalUserConfig =
                        new WeComCorpConfigResponse.ContactsConfig();
                externalUserConfig.setEncodingAesKey(item.getExternalUserEncodingAesKey());
                externalUserConfig.setToken(item.getExternalUserToken());
                externalUserConfig.setUrl(Constants.WECOM_CALLBACK_CUSTOMER + item.getCorpId());
                externalUserConfig.setSecret(item.getExternalUserSecret());
                config.setExternalUser(externalUserConfig);
            }
            configList.add(config);
        }
        response.setConfigs(configList);
        return BaseResponse.success(response);
    }

    @Override
    public WeComCorpCallbackResponse getCallbackConfig(String projectId, String corpId, String configType) {

        WeComCorpMessageEntity entity = weComCorpMessageRepository.getCorpConfigByCorpId(corpId);

        WeComCorpCallbackResponse response = new WeComCorpCallbackResponse();
        if (configType.equals(WeComCorpConfigStepEnum.CONTACTS_MSG.getValue())) {
            response.setToken(entity.getContactsToken());
            response.setEncodingAesKey(entity.getContactsEncodingAesKey());
            //TUDO 添加配置
            response.setCallbackUrl(Constants.WECOM_CALLBACK_CONSTACTS + corpId);
        } else if (configType.equals(WeComCorpConfigStepEnum.EXTERNAL_USER_MSG.getValue())) {
            response.setToken(entity.getExternalUserToken());
            response.setEncodingAesKey(entity.getExternalUserEncodingAesKey());
            //TUDO 添加配置
            response.setCallbackUrl(Constants.WECOM_CALLBACK_CUSTOMER + corpId);
        }
        return response;
    }

    @Override
    public void getExternalUserDetail(String corpId, String externalUserId) {
        WeComQueryExternalUserDetailClientRequest request = new WeComQueryExternalUserDetailClientRequest();
        request.setExternalUserId(externalUserId);
        request.setCorpId(corpId);
        request.setAgentId(Constants.AGENT_KEY_FOR_EXTERNALUSER);
        RpcResponse<WeComQueryExternalUserDetailClientResponse> responseRpcResponse =
                weComExternalUserRpcService.queryExternalUserDetail(request);
        System.out.println("responseRpcResponse= " + responseRpcResponse);
    }

}
