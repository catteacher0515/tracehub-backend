package com.pingyu.tracehub.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pingyu.tracehub.application.service.PostFavourApplicationService;
import com.pingyu.tracehub.domain.post.entity.Post;
import com.pingyu.tracehub.domain.post.service.PostDomainService;
import com.pingyu.tracehub.domain.postfavour.entity.PostFavour;
import com.pingyu.tracehub.domain.user.entity.User;
import com.pingyu.tracehub.infrastructure.exception.BusinessException;
import com.pingyu.tracehub.infrastructure.exception.ErrorCode;
import com.pingyu.tracehub.infrastructure.mapper.PostFavourMapper;
import com.pingyu.tracehub.interfaces.dto.postfavour.PostFavourQueryRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 帖子收藏服务实现
 */
@Service
public class PostFavourApplicationServiceImpl extends ServiceImpl<PostFavourMapper, PostFavour>
        implements PostFavourApplicationService {

    @Resource
    private PostFavourMapper postFavourMapper;

    @Resource
    private PostDomainService postDomainService;

    /**
     * 帖子收藏
     *
     * @param postId
     * @param loginUser
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int doPostFavour(long postId, User loginUser) {
        // 1. 校验参数
        if (postId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 2. 检查帖子是否存在
        // 假设帖子存在，后续集成 PostService 后取消注释
        // Post post = postDomainService.getById(postId);
        // if (post == null) {
        //     throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
        // }

        // 3. 登录用户
        long userId = loginUser.getId();

        // 4. 判断是否已收藏
        LambdaQueryWrapper<PostFavour> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PostFavour::getPostId, postId);
        queryWrapper.eq(PostFavour::getUserId, userId);
        PostFavour oldPostFavour = this.getOne(queryWrapper);

        // 5. 已收藏 -> 取消收藏
        if (oldPostFavour != null) {
            boolean result = this.removeById(oldPostFavour.getId());
            if (result) {
                return -1;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
        } else {
            // 6. 未收藏 -> 添加收藏
            PostFavour postFavour = new PostFavour();
            postFavour.setPostId(postId);
            postFavour.setUserId(userId);
            boolean result = this.save(postFavour);
            if (result) {
                return 1;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR);
            }
        }
    }

    /**
     * 分页查询用户收藏的帖子列表
     *
     * @param pageRequest
     * @param userId
     * @return
     */
    @Override
    public Page<Post> listMyFavourPostByPage(PostFavourQueryRequest pageRequest, long userId) {
        if (userId <= 0) {
            return new Page<>();
        }
        int current = pageRequest.getCurrent();
        int pageSize = pageRequest.getPageSize();
        
        // 1. 第一步：查询收藏关系（中间表）
        Page<PostFavour> postFavourPage = this.page(new Page<>(current, pageSize),
                new LambdaQueryWrapper<PostFavour>()
                        .eq(PostFavour::getUserId, userId)
                        .orderByDesc(PostFavour::getCreateTime)); // 最近收藏的排前面
        
        List<PostFavour> postFavourList = postFavourPage.getRecords();
        if (CollectionUtils.isEmpty(postFavourList)) {
            return new Page<>(current, pageSize, 0);
        }

        // 2. 第二步：提取 postId 列表
        List<Long> postIdList = postFavourList.stream()
                .map(PostFavour::getPostId)
                .collect(Collectors.toList());

        // 3. 第三步：查询帖子详情（主表）
        List<Post> postList = postDomainService.listByIds(postIdList);
        
        // 4. 第四步：组装返回值
        // 注意：这里没有保证查询出的 postList 顺序与 postIdList 一致，如果需要保持顺序（如按收藏时间），
        // 可以在内存中根据 postIdList 对 postList 进行排序，或者使用 map 映射
        Page<Post> postPage = new Page<>(postFavourPage.getCurrent(), postFavourPage.getSize(), postFavourPage.getTotal());
        postPage.setRecords(postList);
        
        return postPage;
    }
}
