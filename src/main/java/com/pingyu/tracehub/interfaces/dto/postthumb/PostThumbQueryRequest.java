package com.pingyu.tracehub.interfaces.dto.postthumb;

import com.pingyu.tracehub.infrastructure.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;

/**
 * 帖子点赞查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PostThumbQueryRequest extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;
}
