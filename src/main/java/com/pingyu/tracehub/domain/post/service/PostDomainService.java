package com.pingyu.tracehub.domain.post.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pingyu.tracehub.domain.post.entity.Post;
import com.pingyu.tracehub.interfaces.dto.post.PostAddRequest;
import com.pingyu.tracehub.domain.user.entity.User;

import java.util.Collection;
import java.util.List;

/**
 * 帖子服务接口
 */
public interface PostDomainService extends IService<Post> {
    
    /**
     * 创建帖子
     *
     * @param postAddRequest
     * @param loginUser
     * @return
     */
    long addPost(PostAddRequest postAddRequest, User loginUser);
}
