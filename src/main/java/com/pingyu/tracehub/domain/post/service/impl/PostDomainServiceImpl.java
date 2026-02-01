package com.pingyu.tracehub.domain.post.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pingyu.tracehub.domain.post.entity.Post;
import com.pingyu.tracehub.domain.post.service.PostDomainService;
import com.pingyu.tracehub.infrastructure.mapper.PostMapper;
import org.springframework.stereotype.Service;

/**
 * 帖子服务实现
 */
@Service
public class PostDomainServiceImpl extends ServiceImpl<PostMapper, Post> implements PostDomainService {

}
