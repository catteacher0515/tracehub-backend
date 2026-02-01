package com.pingyu.tracehub.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pingyu.tracehub.application.service.PostFavourApplicationService;
import com.pingyu.tracehub.domain.postfavour.entity.PostFavour;
import com.pingyu.tracehub.domain.user.entity.User;
import com.pingyu.tracehub.infrastructure.exception.BusinessException;
import com.pingyu.tracehub.infrastructure.exception.ErrorCode;
import com.pingyu.tracehub.infrastructure.mapper.PostFavourMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * 帖子收藏服务实现
 * 已修正：将 QueryWrapper 替换为 LambdaQueryWrapper，解决数据库字段映射导致的 BadSqlGrammarException
 */
@Service
public class PostFavourApplicationServiceImpl extends ServiceImpl<PostFavourMapper, PostFavour>
        implements PostFavourApplicationService {

    @Resource
    private PostFavourMapper postFavourMapper;

    /**
     * 帖子收藏 / 取消收藏 (Toggle 模式)
     *
     * @param postId    帖子 ID
     * @param loginUser 当前登录用户
     * @return 1 - 收藏成功; -1 - 取消收藏成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int doPostFavour(long postId, User loginUser) {
        // 1. 校验参数
        if (postId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "帖子 ID 非法");
        }

        // 2. 检查帖子是否存在 (后续完善 Post 模块后开启)
        // 此处应注入 PostDomainService 进行校验

        // 3. 获取登录用户 ID
        long userId = loginUser.getId();

        // 4. 判断是否已收藏 (使用 LambdaQueryWrapper 确保字段映射准确)
        LambdaQueryWrapper<PostFavour> postFavourQueryWrapper = new LambdaQueryWrapper<>();
        postFavourQueryWrapper.eq(PostFavour::getPostId, postId);
        postFavourQueryWrapper.eq(PostFavour::getUserId, userId);

        PostFavour oldPostFavour = this.getOne(postFavourQueryWrapper);

        // 5. 状态切换逻辑
        if (oldPostFavour != null) {
            // 已收藏 -> 执行取消收藏 (物理删除)
            boolean result = this.removeById(oldPostFavour.getId());
            if (result) {
                return -1;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "取消收藏失败");
            }
        } else {
            // 未收藏 -> 执行添加收藏
            PostFavour postFavour = new PostFavour();
            postFavour.setPostId(postId);
            postFavour.setUserId(userId);
            boolean result = this.save(postFavour);
            if (result) {
                return 1;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "收藏操作失败");
            }
        }
    }
}