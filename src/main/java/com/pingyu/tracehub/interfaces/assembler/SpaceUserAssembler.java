package com.pingyu.tracehub.interfaces.assembler;

import com.pingyu.tracehub.domain.space.entity.SpaceUser;
import com.pingyu.tracehub.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.pingyu.tracehub.interfaces.dto.spaceuser.SpaceUserEditRequest;
import org.springframework.beans.BeanUtils;

/**
 * 空间用户对象转换
 */
public class SpaceUserAssembler {

    public static SpaceUser toSpaceUserEntity(SpaceUserAddRequest request) {
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(request, spaceUser);
        return spaceUser;
    }

    public static SpaceUser toSpaceUserEntity(SpaceUserEditRequest request) {
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(request, spaceUser);
        return spaceUser;
    }
}