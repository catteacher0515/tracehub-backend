package com.pingyu.tracehub.application.service;

import com.pingyu.tracehub.domain.user.entity.User;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pingyu.tracehub.domain.post.entity.Post;
import com.pingyu.tracehub.interfaces.dto.postthumb.PostThumbQueryRequest;

/**
 * 帖子点赞服务
 */
public interface PostThumbApplicationService {

    /**
     * 帖子点赞
     *
     * @param postId
     * @param loginUser
     * @return 1 - 点赞成功, -1 - 取消点赞
     */
    int doPostThumb(long postId, User loginUser);

    /**
     * 分页查询用户点赞的帖子列表
     *
     * @param pageRequest
     * @param userId
     * @return
     */
    Page<Post> listMyThumbPostByPage(PostThumbQueryRequest pageRequest, long userId);
}
