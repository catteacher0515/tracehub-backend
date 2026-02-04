package com.pingyu.tracehub.domain.space.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pingyu.tracehub.domain.space.entity.SpaceUser;
import com.pingyu.tracehub.interfaces.dto.spaceuser.SpaceUserQueryRequest;

/**
 * @author 花萍雨
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service
 */
public interface SpaceUserDomainService {

    /**
     * 获取查询对象
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 【新增】根据条件删除记录
     * @param queryWrapper 删除条件
     * @return 是否成功
     */
    boolean remove(QueryWrapper<SpaceUser> queryWrapper);
}