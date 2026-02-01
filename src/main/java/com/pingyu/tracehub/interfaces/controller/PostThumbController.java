package com.pingyu.tracehub.interfaces.controller;

import com.pingyu.tracehub.application.service.PostThumbApplicationService;
import com.pingyu.tracehub.application.service.UserApplicationService;
import com.pingyu.tracehub.domain.user.entity.User;
import com.pingyu.tracehub.infrastructure.common.BaseResponse;
import com.pingyu.tracehub.infrastructure.common.ResultUtils;
import com.pingyu.tracehub.infrastructure.exception.BusinessException;
import com.pingyu.tracehub.infrastructure.exception.ErrorCode;
import com.pingyu.tracehub.interfaces.dto.postthumb.PostThumbAddRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 帖子点赞接口
 */
@RestController
@RequestMapping("/post_thumb")
public class PostThumbController {

    @Resource
    private PostThumbApplicationService postThumbApplicationService;

    @Resource
    private UserApplicationService userApplicationService;

    /**
     * 点赞 / 取消点赞
     *
     * @param postThumbAddRequest
     * @param request
     * @return resultNum 点赞变化数
     */
    @PostMapping("/")
    public BaseResponse<Integer> doPostThumb(@RequestBody PostThumbAddRequest postThumbAddRequest,
                                             HttpServletRequest request) {
        if (postThumbAddRequest == null || postThumbAddRequest.getPostId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 登录才能操作
        User loginUser = userApplicationService.getLoginUser(request);
        long postId = postThumbAddRequest.getPostId();
        int result = postThumbApplicationService.doPostThumb(postId, loginUser);
        return ResultUtils.success(result);
    }
}
