package com.pingyu.tracehub.domain.space.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pingyu.tracehub.domain.space.entity.SpaceUser;
import com.pingyu.tracehub.interfaces.dto.spaceuser.SpaceUserQueryRequest;

/**
 * @author 花萍雨
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service
 * @createDate 2025-01-02 20:07:15
 */
public interface SpaceUserDomainService {

    /**
     * 获取查询对象
     *
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);
}
