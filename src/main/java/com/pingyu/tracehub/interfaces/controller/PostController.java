package com.pingyu.tracehub.interfaces.controller;

import com.pingyu.tracehub.application.service.UserApplicationService;
import com.pingyu.tracehub.domain.post.service.PostDomainService;
import com.pingyu.tracehub.domain.user.entity.User;
import com.pingyu.tracehub.infrastructure.common.BaseResponse;
import com.pingyu.tracehub.infrastructure.common.ResultUtils;
import com.pingyu.tracehub.infrastructure.exception.BusinessException;
import com.pingyu.tracehub.infrastructure.exception.ErrorCode;
import com.pingyu.tracehub.interfaces.dto.post.PostAddRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 帖子接口
 */
@RestController
@RequestMapping("/post")
public class PostController {

    @Resource
    private PostDomainService postDomainService;

    @Resource
    private UserApplicationService userApplicationService;

    /**
     * 创建帖子
     *
     * @param postAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addPost(@RequestBody PostAddRequest postAddRequest, HttpServletRequest request) {
        if (postAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userApplicationService.getLoginUser(request);
        long newPostId = postDomainService.addPost(postAddRequest, loginUser);
        return ResultUtils.success(newPostId);
    }
}
