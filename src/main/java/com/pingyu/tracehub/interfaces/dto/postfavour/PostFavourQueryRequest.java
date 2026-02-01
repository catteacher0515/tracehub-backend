package com.pingyu.tracehub.interfaces.dto.postfavour;

import com.pingyu.tracehub.infrastructure.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;

/**
 * 帖子收藏查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PostFavourQueryRequest extends PageRequest implements Serializable {

    /**
     * 帖子查询请求
     * 可选，用于筛选特定条件的帖子，暂不使用
     */
    // private PostQueryRequest postQueryRequest;

    private static final long serialVersionUID = 1L;
}
