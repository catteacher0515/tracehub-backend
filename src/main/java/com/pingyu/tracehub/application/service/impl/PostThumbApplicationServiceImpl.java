package com.pingyu.tracehub.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pingyu.tracehub.application.service.PostThumbApplicationService;
import com.pingyu.tracehub.domain.postthumb.entity.PostThumb;
import com.pingyu.tracehub.domain.user.entity.User;
import com.pingyu.tracehub.infrastructure.exception.BusinessException;
import com.pingyu.tracehub.infrastructure.exception.ErrorCode;
import com.pingyu.tracehub.infrastructure.mapper.PostThumbMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 帖子点赞服务实现
 */
@Service
public class PostThumbApplicationServiceImpl extends ServiceImpl<PostThumbMapper, PostThumb>
        implements PostThumbApplicationService {

    @Resource
    private PostThumbMapper postThumbMapper;

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
}
