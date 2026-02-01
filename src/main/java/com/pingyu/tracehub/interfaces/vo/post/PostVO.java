package com.pingyu.tracehub.interfaces.vo.post;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 帖子 VO
 */
@Data
public class PostVO implements Serializable {
    private Long id;
    private String title;
    private String content;
    private Long userId;
    private Date createTime;
    private static final long serialVersionUID = 1L;
}
