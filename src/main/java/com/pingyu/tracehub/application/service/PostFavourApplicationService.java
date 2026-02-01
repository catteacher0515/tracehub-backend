package com.pingyu.tracehub.application.service;

import com.pingyu.tracehub.domain.user.entity.User;

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
}
