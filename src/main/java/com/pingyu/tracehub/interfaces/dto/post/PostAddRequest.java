package com.pingyu.tracehub.interfaces.dto.post;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 创建帖子请求
 */
@Data
public class PostAddRequest implements Serializable {

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 帖子图片
     */
    private String postImg;

    private static final long serialVersionUID = 1L;
}
