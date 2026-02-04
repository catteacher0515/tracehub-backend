package com.pingyu.tracehub.domain.space.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pingyu.tracehub.domain.space.entity.SpaceUser;
import com.pingyu.tracehub.domain.space.service.SpaceUserDomainService;
import com.pingyu.tracehub.infrastructure.mapper.SpaceUserMapper; // 🌟 必须注入 Mapper
import com.pingyu.tracehub.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import cn.hutool.core.util.ObjUtil;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author 花萍雨
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
 */
@Service
public class SpaceUserDomainServiceImpl implements SpaceUserDomainService {

    @Resource
    private SpaceUserMapper spaceUserMapper; // 🌟 注入底层 Mapper

    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryRequest == null) {
            return queryWrapper;
        }
        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceRole), "spaceRole", spaceRole);
        return queryWrapper;
    }

    /**
     * 【新增实现】调用 Mapper 的 delete 方法执行级联删除
     */
    @Override
    public boolean remove(QueryWrapper<SpaceUser> queryWrapper) {
        // 返回影响的行数，若大于等于 0 则视为成功
        return spaceUserMapper.delete(queryWrapper) >= 0;
    }
}