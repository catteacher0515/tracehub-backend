package com.pingyu.tracehub.domain.post.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pingyu.tracehub.domain.post.entity.Post;

import java.util.Collection;
import java.util.List;

/**
 * 帖子服务接口
 */
public interface PostDomainService extends IService<Post> {
    // IService 已经包含了 listByIds
}
