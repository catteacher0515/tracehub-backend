package com.pingyu.tracehub.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pingyu.tracehub.application.service.PostThumbApplicationService;
import com.pingyu.tracehub.domain.post.entity.Post;
import com.pingyu.tracehub.domain.post.service.PostDomainService;
import com.pingyu.tracehub.domain.postthumb.entity.PostThumb;
import com.pingyu.tracehub.domain.user.entity.User;
import com.pingyu.tracehub.infrastructure.exception.BusinessException;
import com.pingyu.tracehub.infrastructure.exception.ErrorCode;
import com.pingyu.tracehub.infrastructure.mapper.PostThumbMapper;
import com.pingyu.tracehub.interfaces.dto.postthumb.PostThumbQueryRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 帖子点赞服务实现
 */
@Service
public class PostThumbApplicationServiceImpl extends ServiceImpl<PostThumbMapper, PostThumb>
        implements PostThumbApplicationService {

    @Resource
    private PostThumbMapper postThumbMapper;

    @Resource
    private PostDomainService postDomainService;

    /**
     * 帖子点赞
     *
     * @param postId
     * @param loginUser
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int doPostThumb(long postId, User loginUser) {
        // 1. 校验参数
        if (postId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 2. 登录用户
        long userId = loginUser.getId();

        // 3. 判断是否已点赞
        LambdaQueryWrapper<PostThumb> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PostThumb::getPostId, postId);
        queryWrapper.eq(PostThumb::getUserId, userId);
        PostThumb oldPostThumb = this.getOne(queryWrapper);

        // 4. 已点赞 -> 取消点赞
        if (oldPostThumb != null) {
            boolean result = this.removeById(oldPostThumb.getId());
            if (result) {
                return -1;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
        } else {
            // 5. 未点赞 -> 点赞
            PostThumb postThumb = new PostThumb();
            postThumb.setPostId(postId);
            postThumb.setUserId(userId);
            boolean result = this.save(postThumb);
            if (result) {
                return 1;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
        }
    }

    /**
     * 分页查询用户点赞的帖子列表
     *
     * @param pageRequest
     * @param userId
     * @return
     */
    @Override
    public Page<Post> listMyThumbPostByPage(PostThumbQueryRequest pageRequest, long userId) {
        if (userId <= 0) {
            return new Page<>();
        }
        int current = pageRequest.getCurrent();
        int pageSize = pageRequest.getPageSize();

        // 1. 第一步：查询点赞关系（中间表）
        Page<PostThumb> postThumbPage = this.page(new Page<>(current, pageSize),
                new LambdaQueryWrapper<PostThumb>()
                        .eq(PostThumb::getUserId, userId)
                        .orderByDesc(PostThumb::getCreateTime)); // 最近点赞的排前面

        List<PostThumb> postThumbList = postThumbPage.getRecords();
        if (CollectionUtils.isEmpty(postThumbList)) {
            return new Page<>(current, pageSize, 0);
        }

        // 2. 第二步：提取 postId 列表
        List<Long> postIdList = postThumbList.stream()
                .map(PostThumb::getPostId)
                .collect(Collectors.toList());

        // 3. 第三步：查询帖子详情（主表）
        List<Post> postList = postDomainService.listByIds(postIdList);

        // 4. 第四步：组装返回值
        Page<Post> postPage = new Page<>(postThumbPage.getCurrent(), postThumbPage.getSize(), postThumbPage.getTotal());
        postPage.setRecords(postList);

        return postPage;
    }
}
