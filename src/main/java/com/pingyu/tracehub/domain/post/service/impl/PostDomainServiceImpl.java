package com.pingyu.tracehub.domain.post.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.core.lang.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.pingyu.tracehub.application.service.UserApplicationService;
import com.pingyu.tracehub.domain.post.entity.Post;
import com.pingyu.tracehub.domain.post.service.PostDomainService;
import com.pingyu.tracehub.domain.postfavour.entity.PostFavour;
import com.pingyu.tracehub.domain.postthumb.entity.PostThumb;
import com.pingyu.tracehub.domain.user.entity.User;
import com.pingyu.tracehub.domain.user.valueobject.UserRoleEnum;
import com.pingyu.tracehub.infrastructure.exception.BusinessException;
import com.pingyu.tracehub.infrastructure.exception.ErrorCode;
import com.pingyu.tracehub.infrastructure.manager.AiManager;
import com.pingyu.tracehub.infrastructure.mapper.PostFavourMapper;
import com.pingyu.tracehub.infrastructure.mapper.PostMapper;
import com.pingyu.tracehub.infrastructure.mapper.PostThumbMapper;
import com.pingyu.tracehub.interfaces.dto.post.PostAddRequest;
import com.pingyu.tracehub.interfaces.dto.post.PostQueryRequest;
import com.pingyu.tracehub.interfaces.vo.post.PostVO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 帖子服务实现
 */
@Service
@Slf4j
public class PostDomainServiceImpl extends ServiceImpl<PostMapper, Post> implements PostDomainService {

    @Resource
    private PostThumbMapper postThumbMapper;

    @Resource
    private PostFavourMapper postFavourMapper;

    @Resource
    private UserApplicationService userApplicationService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private AiManager aiManager;

    // -------------------------------------------------------------
    // 【L1 缓存】Caffeine 本地缓存定义
    // 容量 100，写入 1 分钟后过期
    // -------------------------------------------------------------
    private final Cache<String, Page<PostVO>> caffeineCache = Caffeine.newBuilder()
            .initialCapacity(10)
            .maximumSize(100)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();

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
        post.setReviewStatus(0); // 默认待审核
        post.setIsDelete(0);

        // 5. 保存
        boolean result = this.save(post);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }

        // 【日志埋点】主流程结束，记录帖子 ID
        log.info("帖子创建成功，初始状态: 0 (待审核), postId: {}", post.getId());

        // 6. 异步调用 AI 进行审核
        CompletableFuture.runAsync(() -> {
            // 【日志埋点】异步任务开始
            log.info("[Async] 开始 AI 审核，postId: {}", post.getId());
            try {
                // 构造 Prompt
                String systemMessage = "你是一个严谨的社区内容风控专家。请对用户输入的内容进行安全检测和信息增强。\n" +
                        "严格按照以下 JSON 格式返回结果，不要包含任何多余的 Markdown 标记或解释：\n" +
                        "{\n" +
                        "    \"isSafe\": boolean, // 是否安全（不包含涉黄、涉政、暴力、辱骂等）\n" +
                        "    \"reason\": \"string\", // 审核结果说明\n" +
                        "    \"summary\": \"string\", // 50字以内的精简摘要\n" +
                        "    \"tags\": [\"string\", \"string\"] // 3-5个相关的技术或内容标签\n" +
                        "}";
                String userMessage = "标题：" + title + "\n内容：" + content;

                // 调用 AI
                String aiResult = aiManager.doChat(systemMessage, userMessage);

                // 【日志埋点】AI 原始响应
                log.info("[AI Response] postId: {}, Result: {}", post.getId(), aiResult);

                // 清洗 AI 结果 (去掉可能的 Markdown 代码块标记)
                String jsonStr = aiResult;
                int start = jsonStr.indexOf("{");
                int end = jsonStr.lastIndexOf("}");
                if (start != -1 && end != -1) {
                    jsonStr = jsonStr.substring(start, end + 1);
                }

                // 解析 JSON
                AiAuditResult auditResult = JSONUtil.toBean(jsonStr, AiAuditResult.class);

                // 更新数据库
                Post updatePost = new Post();
                updatePost.setId(post.getId());
                if (auditResult.getIsSafe()) {
                    updatePost.setReviewStatus(1); // 通过
                    // 补充标签 (如果用户没填，或者做增强)
                    if (CollUtil.isNotEmpty(auditResult.getTags())) {
                        updatePost.setTags(JSONUtil.toJsonStr(auditResult.getTags()));
                    }

                    log.info("[Async] AI 审核通过，已更新数据库, postId: {}", post.getId());
                    // 注意：这里我们暂不更新 content，摘要也暂无字段存储
                } else {
                    updatePost.setReviewStatus(2); // 拒绝
                    updatePost.setReviewMessage(auditResult.getReason());
                    log.info("[Async] AI 审核拒绝，原因: {}, postId: {}", auditResult.getReason(), post.getId());
                }
                boolean updateResult = this.updateById(updatePost);
                if (!updateResult) {
                    log.error("AI 审核完成后更新数据库失败, postId={}", post.getId());
                }

            } catch (Exception e) {
                log.error("AI 审核异步任务执行失败, postId=" + post.getId(), e);
                // 也可以在这里将 reviewStatus 设为 2 或人工审核状态
            }
        });

        return post.getId();
    }

    @Data
    private static class AiAuditResult {
        private Boolean isSafe;
        private String reason;
        private String summary;
        private List<String> tags;
    }

    @Override
    public PostVO getPostVOById(long id, User loginUser) {
        Post post = this.getById(id);
        if (post == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // 校验审核状态：非管理员且非作者，只能看审核通过的
        boolean isAdmin = loginUser != null && UserRoleEnum.ADMIN.getValue().equals(loginUser.getUserRole());
        boolean isAuthor = loginUser != null && post.getUserId().equals(loginUser.getId());
        if (!isAdmin && !isAuthor && post.getReviewStatus() != 1) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权查看或帖子未审核通过");
        }

        return getPostVO(post, loginUser);
    }

    @Override
    public Page<PostVO> listPostVOByPage(PostQueryRequest postQueryRequest, User loginUser) {
        // 【StopWatch】启动秒表，开始计时
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        long current = postQueryRequest.getCurrent();
        long size = postQueryRequest.getPageSize();

        // -------------------------------------------------------------
        // 【多级缓存逻辑开始】
        // 策略：只缓存"无条件查询"且"前3页"的热点数据
        // -------------------------------------------------------------
        boolean isCacheable = StringUtils.isBlank(postQueryRequest.getSearchText())
                && StringUtils.isBlank(postQueryRequest.getTitle())
                && StringUtils.isBlank(postQueryRequest.getContent())
                && postQueryRequest.getUserId() == null
                && (postQueryRequest.getTags() == null || postQueryRequest.getTags().isEmpty())
                && current <= 3;

        String cacheKey = "tracehub:post:square:page:" + current;

        Page<PostVO> postVOPage = null;

        // 用于记录本次查询命中了哪里
        String hitSource = "DB (Database)";

        if (isCacheable) {
            // 1. 查询 L1 (Caffeine)
            postVOPage = caffeineCache.getIfPresent(cacheKey);
            if (postVOPage != null) {
                hitSource = "L1 (Caffeine)";
            } else {
                // 2. 查询 L2 (Redis)
                ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
                String redisData = ops.get(cacheKey);
                if (StringUtils.isNotBlank(redisData)) {
                    hitSource = "L2 (Redis)";
                    // 反序列化
                    postVOPage = JSONUtil.toBean(redisData, new TypeReference<Page<PostVO>>() {}, false);
                    // 回填 L1
                    caffeineCache.put(cacheKey, postVOPage);
                }
            }
        }

        // 3. 查询数据库 (DB) - 兜底逻辑
        if (postVOPage == null) {

            // 如果本来应该走缓存但没命中，打印一条日志方便调试
            if (isCacheable) {
                log.info("【缓存未命中】查询数据库，并准备回填缓存: {}", cacheKey);
            }

            Page<Post> postPage = this.page(new Page<>(current, size),
                    this.getQueryWrapper(postQueryRequest));
            // 获取 VO 对象 (传入 null 用户以获取纯净数据用于缓存)
            postVOPage = this.getPostVOPage(postPage, isCacheable ? null : loginUser);

            // -------------------------------------------------------------
            // 4. 回填缓存
            // -------------------------------------------------------------
            if (isCacheable) {
                try {
                    // 序列化
                    String jsonString = JSONUtil.toJsonStr(postVOPage);

                    // 写入 L2 (Redis): 5分钟 + 随机抖动 (防止雪崩)
                    int jitter = (int) (Math.random() * 60);
                    stringRedisTemplate.opsForValue().set(cacheKey, jsonString, 300 + jitter, TimeUnit.SECONDS);

                    // 写入 L1 (Caffeine)
                    caffeineCache.put(cacheKey, postVOPage);

                } catch (Exception e) {
                    log.error("【缓存写入失败】", e);
                }
            }
        }

        // 5. 如果是从缓存取出的数据，需要针对当前用户填充点赞/收藏状态
        if (isCacheable && loginUser != null && CollUtil.isNotEmpty(postVOPage.getRecords())) {
            fillPostVOState(postVOPage.getRecords(), loginUser);
        }

        // 【StopWatch】停止计时
        stopWatch.stop();
        long costTime = stopWatch.getTotalTimeMillis();

        // 打印带耗时的性能监控日志
        if (isCacheable) {
            log.info("【性能监控】查询来源: {}, 耗时: {} ms, Key: {}", hitSource, costTime, cacheKey);
        } else {
            log.info("【普通查询】耗时: {} ms", costTime);
        }

        return postVOPage;
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
        queryWrapper.orderBy(StringUtils.isNotBlank(sortField), "ascend".equals(sortOrder), sortField);
        if (StringUtils.isBlank(sortField)) {
            queryWrapper.orderByDesc("createTime");
        }

        return queryWrapper;
    }

    /**
     * 获取 PostVO 分页
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
            // 必须在这里也注入用户信息，否则列表页的头像会丢失
            Long userId = post.getUserId();

            // 【关键修复】增加 try-catch 容错，防止脏数据（用户不存在）导致抛出 BusinessException 从而中断整个列表加载
            User user = null;
            try {
                user = userApplicationService.getUserById(userId);
            } catch (Exception e) {
                // 捕获异常，视为用户不存在，不中断程序
            }

            if (user != null) {
                postVO.setUser(userApplicationService.getUserVO(user));
            } else {
                // 兜底方案：如果找不到用户，设置一个临时的默认信息，避免前端报错
                // 也可以直接留空，看前端 UserVO 的处理逻辑
                postVO.setUser(null);
            }
            return postVO;
        }).collect(Collectors.toList());

        // 2. 若用户已登录，填充点赞和收藏状态
        if (loginUser != null) {
            fillPostVOState(postVOList, loginUser);
        }
        postVOPage.setRecords(postVOList);
        return postVOPage;
    }

    /**
     * 填充点赞和收藏状态
     */
    private void fillPostVOState(List<PostVO> postVOList, User loginUser) {
        if (CollUtil.isEmpty(postVOList) || loginUser == null) {
            return;
        }
        Set<Long> postIdSet = postVOList.stream().map(PostVO::getId).collect(Collectors.toSet());
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

    /**
     * 获取单个 PostVO (私有辅助方法)
     *
     * @param post
     * @param loginUser
     * @return
     */
    private PostVO getPostVO(Post post, User loginUser) {
        PostVO postVO = new PostVO();
        BeanUtils.copyProperties(post, postVO);

        // 1. 关联用户信息
        Long userId = post.getUserId();
        // 【修复点 2】修改 getById 为 getUserById
        User user = userApplicationService.getUserById(userId);
        if (user != null) {
            postVO.setUser(userApplicationService.getUserVO(user));
        }

        // 2. 填充点赞和收藏状态
        if (loginUser != null) {
            Long loginUserId = loginUser.getId();
            Long postId = post.getId();

            QueryWrapper<PostThumb> thumbQueryWrapper = new QueryWrapper<>();
            thumbQueryWrapper.eq("postId", postId);
            thumbQueryWrapper.eq("userId", loginUserId);
            postVO.setHasThumb(postThumbMapper.selectCount(thumbQueryWrapper) > 0);

            QueryWrapper<PostFavour> favourQueryWrapper = new QueryWrapper<>();
            favourQueryWrapper.eq("postId", postId);
            favourQueryWrapper.eq("userId", loginUserId);
            postVO.setHasFavour(postFavourMapper.selectCount(favourQueryWrapper) > 0);
        }
        return postVO;
    }
}