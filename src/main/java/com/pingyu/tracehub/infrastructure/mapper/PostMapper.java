package com.pingyu.tracehub.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pingyu.tracehub.domain.post.entity.Post;
import org.apache.ibatis.annotations.Mapper;

/**
 * 帖子数据库操作
 */
@Mapper
public interface PostMapper extends BaseMapper<Post> {

}
