package com.pingyu.tracehub.domain.postfavour.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField; // 务必导入这个
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 帖子收藏
 */
@TableName(value ="post_favour")
@Data
public class PostFavour implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 帖子 id
     * 修正点：显式指定数据库列名为 post_id
     */
    @TableField(value = "post_id")
    private Long postId;

    /**
     * 创建用户 id
     * 修正点：显式指定数据库列名为 user_id
     */
    @TableField(value = "user_id")
    private Long userId;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}