package com.pingyu.tracehub.interfaces.controller;

import cn.hutool.core.util.ObjectUtil;
import com.pingyu.tracehub.domain.space.entity.Space;
import com.pingyu.tracehub.domain.space.valueobject.SpaceRoleEnum;
import com.pingyu.tracehub.infrastructure.common.BaseResponse;
import com.pingyu.tracehub.infrastructure.common.DeleteRequest;
import com.pingyu.tracehub.infrastructure.common.ResultUtils;
import com.pingyu.tracehub.infrastructure.exception.BusinessException;
import com.pingyu.tracehub.infrastructure.exception.ErrorCode;
import com.pingyu.tracehub.infrastructure.exception.ThrowUtils;
import com.pingyu.tracehub.interfaces.assembler.SpaceUserAssembler;
import com.pingyu.tracehub.shared.auth.annotation.SaSpaceCheckPermission;
import com.pingyu.tracehub.shared.auth.model.SpaceUserPermissionConstant;
import com.pingyu.tracehub.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.pingyu.tracehub.interfaces.dto.spaceuser.SpaceUserEditRequest;
import com.pingyu.tracehub.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.pingyu.tracehub.domain.space.entity.SpaceUser;
import com.pingyu.tracehub.domain.user.entity.User;
import com.pingyu.tracehub.interfaces.vo.space.SpaceUserVO;
import com.pingyu.tracehub.application.service.SpaceUserApplicationService;
import com.pingyu.tracehub.application.service.UserApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.pingyu.tracehub.application.service.SpaceApplicationService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 空间成员管理
 */
@RestController
@RequestMapping("/spaceUser")
@Slf4j
public class SpaceUserController {

    @Resource
    private SpaceUserApplicationService spaceUserApplicationService;

    @Resource
    private UserApplicationService userApplicationService;

    // 【核心修复 1】变量名改为小写开头，符合 Java 规范，解决找不到符号的报错
    @Resource
    private SpaceApplicationService spaceApplicationService;

    /**
     * 添加成员到空间
     */
    @PostMapping("/add")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        long id = spaceUserApplicationService.addSpaceUser(spaceUserAddRequest);
        return ResultUtils.success(id);
    }

    /**
     * 从空间移除成员
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody DeleteRequest deleteRequest,
                                                 HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        // 1. 判断待删除的记录是否存在
        SpaceUser targetUser = spaceUserApplicationService.getById(id);
        ThrowUtils.throwIf(targetUser == null, ErrorCode.NOT_FOUND_ERROR);

        // 2. 获取当前关联的空间信息
        Space space = spaceApplicationService.getById(targetUser.getSpaceId());
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);

        // ================= 【权限防御体系升级】 =================

        // 3. 【防止移除创建者】(之前的逻辑)
        if (space.getUserId().equals(targetUser.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无法移除空间创建者！");
        }

        // 4. 【防止管理员内战】(新增逻辑)
        // 获取当前操作者的信息
        User loginUser = userApplicationService.getLoginUser(request);

        // 查询操作者在这个空间的角色
        SpaceUserQueryRequest queryRequest = new SpaceUserQueryRequest();
        queryRequest.setUserId(loginUser.getId());
        queryRequest.setSpaceId(space.getId());
        SpaceUser operator = spaceUserApplicationService.getOne(spaceUserApplicationService.getQueryWrapper(queryRequest));

        // 核心校验逻辑：
        // 如果操作者存在，且不是空间创建者（说明操作者只是个普通管理员）
        if (operator != null && !space.getUserId().equals(operator.getUserId())) {
            // 此时，如果目标用户也是管理员
            if (SpaceRoleEnum.ADMIN.getValue().equals(targetUser.getSpaceRole())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "管理员无法移除其他管理员！");
            }
        }
        // =======================================================

        // 操作数据库
        boolean result = spaceUserApplicationService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 查询某个成员在某个空间的信息
     */
    @PostMapping("/get")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        // 参数校验
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
        // 查询数据库
        SpaceUser spaceUser = spaceUserApplicationService.getOne(spaceUserApplicationService.getQueryWrapper(spaceUserQueryRequest));
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(spaceUser);
    }

    /**
     * 查询成员信息列表
     * 修改说明：移除了 @SaSpaceCheckPermission 注解，改为校验“是否为空间成员”。
     * 这样“浏览者”和“编辑者”也能看到列表，从而进行“退出空间”操作。
     */
    @PostMapping("/list")
    // 1. 【核心修改】移除管理员权限注解，让所有人都能调通这个接口
    // @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<List<SpaceUserVO>> listSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest,
                                                         HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        if (spaceId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 2. 【核心修改】新增鉴权逻辑：判断当前登录用户是否在该空间中
        User loginUser = userApplicationService.getLoginUser(request);

        // 构造查询条件：查一下当前用户在这个空间有没有记录
        SpaceUserQueryRequest queryRequest = new SpaceUserQueryRequest();
        queryRequest.setUserId(loginUser.getId());
        queryRequest.setSpaceId(spaceId);
        SpaceUser self = spaceUserApplicationService.getOne(spaceUserApplicationService.getQueryWrapper(queryRequest));

        // 如果查不到记录，说明不是该空间成员，拒绝访问
        if (self == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "非本空间成员，无权查看列表");
        }

        // 3. 权限校验通过，执行原有的列表查询
        List<SpaceUser> spaceUserList = spaceUserApplicationService.list(
                spaceUserApplicationService.getQueryWrapper(spaceUserQueryRequest)
        );
        return ResultUtils.success(spaceUserApplicationService.getSpaceUserVOList(spaceUserList));
    }

    /**
     * 编辑成员信息（设置权限）
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditRequest spaceUserEditRequest,
                                               HttpServletRequest request) {
        if (spaceUserEditRequest == null || spaceUserEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        SpaceUser spaceUser = SpaceUserAssembler.toSpaceUserEntity(spaceUserEditRequest);
        // 数据校验
        spaceUserApplicationService.validSpaceUser(spaceUser, false);
        // 判断目标是否存在
        long id = spaceUserEditRequest.getId();
        SpaceUser targetUser = spaceUserApplicationService.getById(id);
        ThrowUtils.throwIf(targetUser == null, ErrorCode.NOT_FOUND_ERROR);

        // 获取当前关联的空间信息
        Space space = spaceApplicationService.getById(targetUser.getSpaceId());
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);

        // ================= 【权限防御升级】 =================

        // 1. 【防止兵变】无论谁，都不能动空间创建者
        if (space.getUserId().equals(targetUser.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权修改空间创建者的权限！");
        }

        // 2. 【防止管理员内战】普通管理员不能动其他管理员
        User loginUser = userApplicationService.getLoginUser(request);
        // 查询当前操作者在这个空间的角色
        SpaceUserQueryRequest queryRequest = new SpaceUserQueryRequest();
        queryRequest.setUserId(loginUser.getId());
        queryRequest.setSpaceId(space.getId());
        SpaceUser operator = spaceUserApplicationService.getOne(spaceUserApplicationService.getQueryWrapper(queryRequest));

        // 如果操作者存在，且不是空间创建者（即只是普通管理员）
        if (operator != null && !space.getUserId().equals(operator.getUserId())) {
            // 如果目标用户也是管理员，且不是操作者自己
            if (SpaceRoleEnum.ADMIN.getValue().equals(targetUser.getSpaceRole())
                    && !targetUser.getUserId().equals(operator.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "管理员无法修改其他管理员的权限！");
            }
        }
        // ===================================================

        // 操作数据库
        boolean result = spaceUserApplicationService.updateById(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 查询我加入的团队空间列表
     */
    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMyTeamSpace(HttpServletRequest request) {
        User loginUser = userApplicationService.getLoginUser(request);
        SpaceUserQueryRequest spaceUserQueryRequest = new SpaceUserQueryRequest();
        spaceUserQueryRequest.setUserId(loginUser.getId());
        List<SpaceUser> spaceUserList = spaceUserApplicationService.list(
                spaceUserApplicationService.getQueryWrapper(spaceUserQueryRequest)
        );
        return ResultUtils.success(spaceUserApplicationService.getSpaceUserVOList(spaceUserList));
    }

    /**
     * 【新增】主动退出团队空间
     * 不需要管理员权限，但必须校验操作对象是否是本人
     */

    @PostMapping("/quit")
    public BaseResponse<Boolean> quitTeamSpace(@RequestBody DeleteRequest deleteRequest,
                                               HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long spaceUserId = deleteRequest.getId();
        SpaceUser spaceUser = spaceUserApplicationService.getById(spaceUserId);
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);

        // 获取当前登录用户
        User loginUser = userApplicationService.getLoginUser(request);

        // 1. 校验权限：只能退出自己的
        if (!spaceUser.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能退出自己加入的团队！");
        }

        // 2. 校验特殊身份：创建者不能退出，只能解散
        Space space = spaceApplicationService.getById(spaceUser.getSpaceId());
        if (space != null && space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "你是空间创建者，不能退出，只能解散空间！");
        }

        // 操作数据库
        boolean result = spaceUserApplicationService.removeById(spaceUserId);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }
}