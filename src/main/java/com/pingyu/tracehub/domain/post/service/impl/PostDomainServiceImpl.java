package com.pingyu.tracehub.domain.post.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pingyu.tracehub.domain.post.entity.Post;
import com.pingyu.tracehub.domain.post.service.PostDomainService;
import com.pingyu.tracehub.domain.postfavour.entity.PostFavour;
import com.pingyu.tracehub.domain.postthumb.entity.PostThumb;
import com.pingyu.tracehub.domain.user.entity.User;
import com.pingyu.tracehub.domain.user.valueobject.UserRoleEnum;
import com.pingyu.tracehub.infrastructure.exception.BusinessException;
import com.pingyu.tracehub.infrastructure.exception.ErrorCode;
import com.pingyu.tracehub.infrastructure.mapper.PostFavourMapper;
import com.pingyu.tracehub.infrastructure.mapper.PostMapper;
import com.pingyu.tracehub.infrastructure.mapper.PostThumbMapper;
import com.pingyu.tracehub.interfaces.dto.post.PostAddRequest;
import com.pingyu.tracehub.interfaces.dto.post.PostQueryRequest;
import com.pingyu.tracehub.interfaces.vo.post.PostVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 帖子服务实现
 */
@Service
public class PostDomainServiceImpl extends ServiceImpl<PostMapper, Post> implements PostDomainService {

    @Resource
    private PostThumbMapper postThumbMapper;

    @Resource
    private PostFavourMapper postFavourMapper;

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
        post.setViewNum(0);
        post.setReviewStatus(1); // 默认通过，实际业务可改为 0 待审核
        
        // 5. 保存
        boolean result = this.save(post);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        return post.getId();
    }

    @Override
    public Page<PostVO> listPostVOByPage(PostQueryRequest postQueryRequest, User loginUser) {
        long current = postQueryRequest.getCurrent();
        long pageSize = postQueryRequest.getPageSize();
        Page<Post> postPage = this.page(new Page<>(current, pageSize),
                getQueryWrapper(postQueryRequest));
        return getPostVOPage(postPage, loginUser);
    }

    @Override
    public boolean deletePost(long id, User loginUser) {
        Post post = this.getById(id);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        // 仅本人或管理员可删除
        if (!post.getUserId().equals(loginUser.getId()) && !UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return this.removeById(id);
    }

    /**
     * 获取查询包装类
     *
     * @param postQueryRequest
     * @return
     */
    private QueryWrapper<Post> getQueryWrapper(PostQueryRequest postQueryRequest) {
        QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
        if (postQueryRequest == null) {
            return queryWrapper;
        }
        String searchText = postQueryRequest.getSearchText();
        String sortField = postQueryRequest.getSortField();
        String sortOrder = postQueryRequest.getSortOrder();
        Long id = postQueryRequest.getId();
        String title = postQueryRequest.getTitle();
        String content = postQueryRequest.getContent();
        List<String> tags = postQueryRequest.getTags();
        Long userId = postQueryRequest.getUserId();
        
        // 1. 基础查询
        queryWrapper.like(StringUtils.isNotBlank(title), "title", title);
        queryWrapper.like(StringUtils.isNotBlank(content), "content", content);
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(userId != null, "userId", userId);
        
        // 2. 广场核心：搜索词 (标题 OR 内容)
        if (StringUtils.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw.like("title", searchText).or().like("content", searchText));
        }
        
        // 3. 标签精准匹配 (JSON 数组)
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        
        // 4. 广场安全过滤：只看审核通过的 (reviewStatus == 1)
        Integer reviewStatus = postQueryRequest.getReviewStatus();
        if (reviewStatus != null) {
            queryWrapper.eq("reviewStatus", reviewStatus);
        } else if (userId == null) {
            // 如果没有指定查谁的（说明是广场），默认只查审核通过的
            queryWrapper.eq("reviewStatus", 1);
        }

        // 必须过滤逻辑删除
        queryWrapper.eq("isDelete", 0);
        
        // 5. 排序
        // 默认按创建时间倒序
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField), "ascend".equals(sortOrder), sortField);
        if (StringUtils.isBlank(sortField)) {
            queryWrapper.orderByDesc("createTime");
        }
        
        return queryWrapper;
    }

    /**
     * 获取 PostVO 分页
     *
     * @param postPage
     * @param loginUser
     * @return
     */
    private Page<PostVO> getPostVOPage(Page<Post> postPage, User loginUser) {
        List<Post> postList = postPage.getRecords();
        Page<PostVO> postVOPage = new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        if (CollUtil.isEmpty(postList)) {
            return postVOPage;
        }
        
        // 1. 转换基础属性
        List<PostVO> postVOList = postList.stream().map(post -> {
            PostVO postVO = new PostVO();
            BeanUtils.copyProperties(post, postVO);
            return postVO;
        }).collect(Collectors.toList());

        // 2. 若用户已登录，填充点赞和收藏状态
        if (loginUser != null) {
            Set<Long> postIdSet = postList.stream().map(Post::getId).collect(Collectors.toSet());
            Long userId = loginUser.getId();
            
            // 查询点赞状态
            QueryWrapper<PostThumb> thumbQueryWrapper = new QueryWrapper<>();
            thumbQueryWrapper.in("postId", postIdSet);
            thumbQueryWrapper.eq("userId", userId);
            List<PostThumb> postThumbList = postThumbMapper.selectList(thumbQueryWrapper);
            Set<Long> thumbPostIdSet = postThumbList.stream().map(PostThumb::getPostId).collect(Collectors.toSet());
            
            // 查询收藏状态
            QueryWrapper<PostFavour> favourQueryWrapper = new QueryWrapper<>();
            favourQueryWrapper.in("postId", postIdSet);
            favourQueryWrapper.eq("userId", userId);
            List<PostFavour> postFavourList = postFavourMapper.selectList(favourQueryWrapper);
            Set<Long> favourPostIdSet = postFavourList.stream().map(PostFavour::getPostId).collect(Collectors.toSet());
            
            // 填充状态
            postVOList.forEach(postVO -> {
                Long postId = postVO.getId();
                postVO.setHasThumb(thumbPostIdSet.contains(postId));
                postVO.setHasFavour(favourPostIdSet.contains(postId));
            });
        }
        postVOPage.setRecords(postVOList);
        return postVOPage;
    }
}
