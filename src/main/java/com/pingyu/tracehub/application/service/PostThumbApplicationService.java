package com.pingyu.tracehub.application.service;

import com.pingyu.tracehub.domain.user.entity.User;

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
}
