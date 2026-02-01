package com.pingyu.tracehub.application.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pingyu.tracehub.domain.post.entity.Post;
import com.pingyu.tracehub.domain.user.entity.User;
import com.pingyu.tracehub.interfaces.dto.postfavour.PostFavourQueryRequest;

/**
 * 帖子收藏服务
 */
public interface PostFavourApplicationService {

    /**
     * 帖子收藏
     *
     * @param postId
     * @param loginUser
     * @return 1 - 收藏成功, -1 - 取消收藏
     */
    int doPostFavour(long postId, User loginUser);

    /**
     * 分页查询用户收藏的帖子列表
     *
     * @param pageRequest
     * @param userId
     * @return
     */
    Page<Post> listMyFavourPostByPage(PostFavourQueryRequest pageRequest, long userId);
}
