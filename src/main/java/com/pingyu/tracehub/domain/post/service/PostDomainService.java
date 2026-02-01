package com.pingyu.tracehub.domain.post.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pingyu.tracehub.domain.post.entity.Post;
import com.pingyu.tracehub.interfaces.dto.post.PostAddRequest;
import com.pingyu.tracehub.domain.user.entity.User;

import java.util.Collection;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pingyu.tracehub.interfaces.dto.post.PostQueryRequest;
import com.pingyu.tracehub.interfaces.vo.post.PostVO;

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

    /**
     * 分页查询帖子 VO
     *
     * @param postQueryRequest
     * @param loginUser
     * @return
     */
    Page<PostVO> listPostVOByPage(PostQueryRequest postQueryRequest, User loginUser);

    /**
     * 删除帖子
     *
     * @param id
     * @param loginUser
     * @return
     */
    boolean deletePost(long id, User loginUser);
}
