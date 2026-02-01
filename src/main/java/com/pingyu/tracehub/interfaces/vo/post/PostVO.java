package com.pingyu.tracehub.interfaces.vo.post;

import com.pingyu.tracehub.interfaces.vo.user.UserVO;
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
    private String postImg;
    private String tags;
    private Integer thumbNum;
    private Integer favourNum;
    private Integer viewNum;
    private Integer reviewStatus;
    private Date editTime;
    private Long userId;
    private Date createTime;
    private Date updateTime;
    
    /**
     * 创建人信息
     */
    private UserVO user;

    /**
     * 是否点赞
     */
    private Boolean hasThumb;
    
    /**
     * 是否收藏
     */
    private Boolean hasFavour;
    
    private static final long serialVersionUID = 1L;
}
