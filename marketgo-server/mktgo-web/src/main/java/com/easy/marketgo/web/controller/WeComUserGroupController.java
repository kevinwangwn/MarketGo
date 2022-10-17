package com.easy.marketgo.web.controller;

import com.easy.marketgo.web.model.request.UserGroupAudienceRules;
import com.easy.marketgo.web.model.response.UserGroupEstimateResponse;
import com.easy.marketgo.web.service.wecom.WeComUserGroupService;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author : kevinwang
 * @version : 1.0
 * @data : 6/21/22 3:01 PM
 * Describe:
 */
@Api(value = "人群管理", tags = "人群管理")
@RestController
@RequestMapping(value = "/mktgo/wecom/user_group")
class WeComUserGroupController {

    @Autowired
    private WeComUserGroupService weComUserGroupService;

    @ApiResponses({
            @ApiResponse(code = 0, message = "ok", response = UserGroupEstimateResponse.class)
    })
    @ApiOperation(value = "预估人群的数量", nickname = "userGroupEstimate", notes = "", response =
            UserGroupEstimateResponse.class)
    @RequestMapping(value = {"/estimate"}, produces = {"application/json"}, method = RequestMethod.POST)
    public ResponseEntity userGroupEstimate(@ApiParam(value = "人群计算的条件", required = true) @RequestBody @NotNull @Valid UserGroupAudienceRules audienceRules,
                                            @ApiParam(value = "企微项目id", required = true) @RequestParam(value =
                                                    "project_id", required = true) @NotBlank @Valid String projectId,
                                            @ApiParam(value = "请求的requestID", required = true) @RequestParam(value =
                                                    "request_id", required = true) @NotBlank @Valid String requestId,
                                            @ApiParam(value = "企业id", required = true) @RequestParam(value = "corp_id",
                                                    required = true) @NotBlank @Valid String corpId,
                                            @ApiParam(value = "群发类型 SINGLE 群发好友, GROUP 群发客户群, MOMENT 群发朋友圈",
                                                    required = true) @RequestParam(value = "task_type",
                                                    required = true) @NotBlank @Valid String taskType) {

        return ResponseEntity.ok(weComUserGroupService.estimate(projectId, audienceRules, requestId, corpId, taskType));
    }

    @ApiResponses({
            @ApiResponse(code = 0, message = "ok", response = UserGroupEstimateResponse.class)
    })
    @ApiOperation(value = "获取人群信息", nickname = "userGroupMessage", notes = "", response =
            UserGroupAudienceRules.class)
    @RequestMapping(value = {"/query"}, produces = {"application/json"}, method = RequestMethod.GET)
    public ResponseEntity queryUserGroup(@ApiParam(value = "企微项目id", required = true) @RequestParam(value =
            "project_id", required = true) @NotBlank @Valid String projectId,
                                         @ApiParam(value = "人群的uuid", required = true) @RequestParam(value =
                                                 "group_uuid", required = true) @NotBlank @Valid String groupUuid,
                                         @ApiParam(value = "企业id", required = true) @RequestParam(value = "corp_id",
                                                 required = true) @NotBlank @Valid String corpId,
                                         @ApiParam(value = "群发类型 SINGLE 群发好友, GROUP 群发客户群, MOMENT 群发朋友圈",
                                                 required = true) @RequestParam(value = "task_type",
                                                 required = true) @NotBlank @Valid String taskType) {

        return ResponseEntity.ok(weComUserGroupService.queryUserGroup(projectId, corpId, taskType, groupUuid));
    }
}
