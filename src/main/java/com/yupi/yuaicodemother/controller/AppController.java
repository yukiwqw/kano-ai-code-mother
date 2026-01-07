package com.yupi.yuaicodemother.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.yupi.yuaicodemother.annotation.AuthCheck;
import com.yupi.yuaicodemother.common.BaseResponse;
import com.yupi.yuaicodemother.common.DeleteRequest;
import com.yupi.yuaicodemother.common.ResultUtils;
import com.yupi.yuaicodemother.constant.AppConstant;
import com.yupi.yuaicodemother.constant.UserConstant;
import com.yupi.yuaicodemother.core.AiCodeGeneratorFacade;
import com.yupi.yuaicodemother.exception.ErrorCode;
import com.yupi.yuaicodemother.model.dto.app.*;
import com.yupi.yuaicodemother.model.entity.App;
import com.yupi.yuaicodemother.model.entity.User;
import com.yupi.yuaicodemother.model.enums.CodeGenTypeEnum;
import com.yupi.yuaicodemother.model.enums.UserRoleEnum;
import com.yupi.yuaicodemother.model.vo.AppVO;
import com.yupi.yuaicodemother.service.AppService;
import com.yupi.yuaicodemother.service.UserService;
import com.yupi.yuaicodemother.utils.ThrowUtils;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 应用 控制层。
 *
 * @author T00080239
 */
@RestController
@RequestMapping("/app")
public class AppController {

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    /**
     * 创建应用
     *
     * @param appAddRequest 应用创建请求
     * @param request       HTTP 请求
     * @return 应用 id
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddRequest appAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appAddRequest == null, ErrorCode.PARAMS_ERROR);
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(initPrompt == null || initPrompt.trim().isEmpty(), ErrorCode.PARAMS_ERROR, "initPrompt 不能为空");
        
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 创建应用
        App app = new App();
        app.setAppName(initPrompt.substring(0, Math.min(initPrompt.length(), 15)));
        app.setInitPrompt(initPrompt);
        app.setUserId(loginUser.getId());
        app.setEditTime(LocalDateTime.now());
        app.setCodeGenType(CodeGenTypeEnum.MULTI_FILE.getValue());
        
        boolean result = appService.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(app.getId());
    }

    /**
     * 修改自己的应用（仅支持修改应用名称）
     *
     * @param appEditRequest 应用编辑请求
     * @param request        HTTP 请求
     * @return 是否成功
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editApp(@RequestBody AppEditRequest appEditRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appEditRequest == null || appEditRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        
        // 查询应用
        App app = appService.getById(appEditRequest.getId());
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        
        // 验证所有权
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无权限修改此应用");
        
        // 更新应用名称
        App updateApp = new App();
        updateApp.setId(appEditRequest.getId());
        updateApp.setAppName(appEditRequest.getAppName());
        updateApp.setEditTime(LocalDateTime.now());
        
        boolean result = appService.updateById(updateApp);
        return ResultUtils.success(result);
    }

    /**
     * 删除自己的应用
     *
     * @param deleteRequest 删除请求
     * @param request       HTTP 请求
     * @return 是否成功
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        
        // 查询应用
        App app = appService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);

        // 验证所有权
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()) && !UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole()), ErrorCode.NO_AUTH_ERROR, "无权限删除此应用");
        
        boolean result = appService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 查看应用详情
     *
     * @param id      应用 id
     * @param request HTTP 请求
     * @return 应用详情
     */
    @GetMapping("/get/{id}")
    public BaseResponse<AppVO> getAppById(@PathVariable Long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        
        // 需要登录
        userService.getLoginUser(request);
        
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 分页查询自己的应用列表
     *
     * @param appQueryRequest 查询请求
     * @param request            HTTP 请求
     * @return 分页结果
     */
    @PostMapping("/list/my/page/vo")
    public BaseResponse<Page<AppVO>> listMyAppVOByPage(@RequestBody AppQueryRequest appQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        
        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        
        // 限制每页最多 20 个
        int pageSize = appQueryRequest.getPageSize();
        if (pageSize > 20) {
            appQueryRequest.setPageSize(20);
        }
        
        long pageNum = appQueryRequest.getPageNum();
        long pageSizeFinal = appQueryRequest.getPageSize();
        
        // 构造查询条件：只查询当前用户的应用
        appQueryRequest.setUserId(loginUser.getId());
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        
        Page<App> appPage = appService.page(Page.of(pageNum, pageSizeFinal), queryWrapper);
        
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSizeFinal, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        
        return ResultUtils.success(appVOPage);
    }

    /**
     * 分页查询精选的应用列表（按 priority 排序）
     *
     * @param appQueryRequest 查询请求
     * @param request            HTTP 请求
     * @return 分页结果
     */
    @PostMapping("/list/featured/page/vo")
    public BaseResponse<Page<AppVO>> listFeaturedAppByPage(@RequestBody AppQueryRequest appQueryRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        
        // 需要登录
        userService.getLoginUser(request);
        
        // 限制每页最多 20 个
        int pageSize = appQueryRequest.getPageSize();
        if (pageSize > 20) {
            appQueryRequest.setPageSize(20);
        }
        
        long pageNum = appQueryRequest.getPageNum();
        long pageSizeFinal = appQueryRequest.getPageSize();
        
        // 构造查询条件：只查精选的应用
        appQueryRequest.setPriority(AppConstant.GOOD_APP_PRIORITY);
        QueryWrapper queryWrapper = appService.getQueryWrapper(appQueryRequest);
        
        Page<App> appPage = appService.page(Page.of(pageNum, pageSizeFinal), queryWrapper);
        
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSizeFinal, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        
        return ResultUtils.success(appVOPage);
    }

    /**
     * 管理员：删除任意应用
     *
     * @param deleteRequest 删除请求
     * @return 是否成功
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> adminDeleteApp(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        
        App app = appService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        
        boolean result = appService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 管理员：更新任意应用（支持更新应用名称、应用封面、优先级）
     *
     * @param appUpdateRequest 更新请求
     * @return 是否成功
     */
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> adminUpdateApp(@RequestBody AppUpdateRequest appUpdateRequest) {
        ThrowUtils.throwIf(appUpdateRequest == null || appUpdateRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        
        App app = appService.getById(appUpdateRequest.getId());
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        
        App updateApp = new App();
        BeanUtil.copyProperties(appUpdateRequest, updateApp);
        updateApp.setEditTime(LocalDateTime.now());
        
        boolean result = appService.updateById(updateApp);
        return ResultUtils.success(result);
    }

    /**
     * 管理员：分页查询应用列表（支持根据除时间外的任何字段查询，每页数量不限）
     *
     * @param appQueryRequest 查询请求
     * @return 分页结果
     */
    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> adminListAppByPage(@RequestBody AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);

        long pageNum = appQueryRequest.getPageNum();
        long pageSize = appQueryRequest.getPageSize();
        
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize), appService.getQueryWrapper(appQueryRequest));
        
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        
        return ResultUtils.success(appVOPage);
    }

    /**
     * 管理员：根据 id 查看应用详情
     *
     * @param id 应用 id
     * @return 应用详情
     */
    @GetMapping("/admin/get/{id}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AppVO> adminGetAppById(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        
        return ResultUtils.success(appService.getAppVO(app));
    }

    /**
     * 用户对话生成并保存代码（流式）
     *
     * @param appId
     * @param userMessage
     * @param request
     * @return
     */
    @GetMapping(value = "/chat/gen/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chatToGenCode(@RequestParam Long appId, @RequestParam String userMessage, HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用ID不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(userMessage), ErrorCode.PARAMS_ERROR, "用户消息不能为空");
        User loginUser = userService.getLoginUser(request);
        Flux<String> contentFlux = appService.chatToGenCode(appId, userMessage, loginUser);
        return contentFlux.map(chunk -> {
            // 将内容包装成JSON对象
            Map<String, String> wrapper = Map.of("d", chunk);
            String jsonData = JSONUtil.toJsonStr(wrapper);
            return ServerSentEvent.<String>builder()
                    .data(jsonData)
                    .build();
        }).concatWith(Mono.just(
                ServerSentEvent.<String>builder()
                        .event("done")
                        .data("")
                        .build()
        ));
    }


    /**
     * 部署应用
     *
     * @param appDeployRequest
     * @param request
     * @return
     */
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(@RequestBody AppDeployRequest appDeployRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(appDeployRequest == null || appDeployRequest.getAppId() == null, ErrorCode.PARAMS_ERROR);

        App app = appService.getById(appDeployRequest.getAppId());
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);

        User loginUser = userService.getLoginUser(request);
        String deployedUrl = appService.deployApp(app.getId(), loginUser);
        return ResultUtils.success(deployedUrl);
    }


}
