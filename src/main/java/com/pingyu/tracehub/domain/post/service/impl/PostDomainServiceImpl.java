package com.pingyu.tracehub.domain.post.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pingyu.tracehub.domain.post.entity.Post;
import com.pingyu.tracehub.domain.post.service.PostDomainService;
import com.pingyu.tracehub.domain.user.entity.User;
import com.pingyu.tracehub.infrastructure.exception.BusinessException;
import com.pingyu.tracehub.infrastructure.exception.ErrorCode;
import com.pingyu.tracehub.infrastructure.mapper.PostMapper;
import com.pingyu.tracehub.interfaces.dto.post.PostAddRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 帖子服务实现
 */
@Service
public class PostDomainServiceImpl extends ServiceImpl<PostMapper, Post> implements PostDomainService {

    @Override
    public long addPost(PostAddRequest postAddRequest, User loginUser) {
        // 1. 校验参数
        if (postAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String title = postAddRequest.getTitle();
        String content = postAddRequest.getContent();
        if (StringUtils.isAnyBlank(title, content)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "标题和内容不能为空");
        }
        
        // 2. 填充实体
        Post post = new Post();
        BeanUtils.copyProperties(postAddRequest, post);
        
        // 3. 处理标签
        List<String> tags = postAddRequest.getTags();
        if (CollUtil.isNotEmpty(tags)) {
            post.setTags(JSONUtil.toJsonStr(tags));
        }
        
        // 4. 设置用户
        post.setUserId(loginUser.getId());
        post.setFavourNum(0);
        post.setThumbNum(0);
        
        // 5. 保存
        boolean result = this.save(post);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return post.getId();
    }
}
