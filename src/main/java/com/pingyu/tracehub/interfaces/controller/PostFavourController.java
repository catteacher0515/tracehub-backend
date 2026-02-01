package com.pingyu.tracehub.interfaces.controller;

import com.pingyu.tracehub.application.service.PostFavourApplicationService;
import com.pingyu.tracehub.application.service.UserApplicationService;
import com.pingyu.tracehub.domain.user.entity.User;
import com.pingyu.tracehub.infrastructure.common.BaseResponse;
import com.pingyu.tracehub.infrastructure.common.ResultUtils;
import com.pingyu.tracehub.infrastructure.exception.BusinessException;
import com.pingyu.tracehub.infrastructure.exception.ErrorCode;
import com.pingyu.tracehub.interfaces.dto.postfavour.PostFavourAddRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 帖子收藏接口
 */
@RestController
@RequestMapping("/post_favour")
public class PostFavourController {

    @Resource
    private PostFavourApplicationService postFavourApplicationService;

    @Resource
    private UserApplicationService userApplicationService;

    /**
     * 收藏 / 取消收藏
     *
     * @param postFavourAddRequest
     * @param request
     * @return resultNum 收藏变化数
     */
    @PostMapping("/")
    public BaseResponse<Integer> doPostFavour(@RequestBody PostFavourAddRequest postFavourAddRequest,
                                              HttpServletRequest request) {
        if (postFavourAddRequest == null || postFavourAddRequest.getPostId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 登录才能操作
        User loginUser = userApplicationService.getLoginUser(request);
        long postId = postFavourAddRequest.getPostId();
        int result = postFavourApplicationService.doPostFavour(postId, loginUser);
        return ResultUtils.success(result);
    }
}
