package com.pingyu.tracehub.interfaces.controller;

import com.pingyu.tracehub.application.service.UserApplicationService;
import com.pingyu.tracehub.domain.post.service.PostDomainService;
import com.pingyu.tracehub.domain.user.entity.User;
import com.pingyu.tracehub.infrastructure.common.BaseResponse;
import com.pingyu.tracehub.infrastructure.common.ResultUtils;
import com.pingyu.tracehub.infrastructure.exception.BusinessException;
import com.pingyu.tracehub.infrastructure.exception.ErrorCode;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pingyu.tracehub.interfaces.dto.post.PostAddRequest;
import com.pingyu.tracehub.interfaces.dto.post.PostQueryRequest;
import com.pingyu.tracehub.interfaces.vo.post.PostVO;
import org.springframework.web.bind.annotation.*;

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

    /**
     * 删除帖子
     *
     * @param idRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePost(@RequestBody PostQueryRequest idRequest, HttpServletRequest request) {
        if (idRequest == null || idRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userApplicationService.getLoginUser(request);
        boolean result = postDomainService.deletePost(idRequest.getId(), loginUser);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取帖子（封装类）
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<PostVO> getPostVOById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userApplicationService.getLoginUser(request);
        PostVO postVO = postDomainService.getPostVOById(id, loginUser);
        return ResultUtils.success(postVO);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param postQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PostVO>> listPostVOByPage(@RequestBody PostQueryRequest postQueryRequest,
                                                       HttpServletRequest request) {
        if (postQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = postQueryRequest.getCurrent();
        long size = postQueryRequest.getPageSize();
        // 限制爬虫
        if (size > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 默认只查已审核通过的
        postQueryRequest.setReviewStatus(1);
        User loginUser = userApplicationService.getLoginUser(request);
        Page<PostVO> postVOPage = postDomainService.listPostVOByPage(postQueryRequest, loginUser);
        return ResultUtils.success(postVOPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param postQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page/vo")
    public BaseResponse<Page<PostVO>> listMyPostVOByPage(@RequestBody PostQueryRequest postQueryRequest,
                                                         HttpServletRequest request) {
        if (postQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userApplicationService.getLoginUser(request);
        postQueryRequest.setUserId(loginUser.getId());
        long current = postQueryRequest.getCurrent();
        long size = postQueryRequest.getPageSize();
        // 限制爬虫
        if (size > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<PostVO> postVOPage = postDomainService.listPostVOByPage(postQueryRequest, loginUser);
        return ResultUtils.success(postVOPage);
    }
}
