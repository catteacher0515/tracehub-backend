package com.pingyu.tracehub.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pingyu.tracehub.infrastructure.common.DeleteRequest;
import com.pingyu.tracehub.interfaces.dto.user.UserLoginRequest;
import com.pingyu.tracehub.interfaces.dto.user.UserQueryRequest;
import com.pingyu.tracehub.domain.user.entity.User;
import com.pingyu.tracehub.interfaces.dto.user.UserRegisterRequest;
import com.pingyu.tracehub.interfaces.dto.user.UserUpdateMyRequest;
import com.pingyu.tracehub.interfaces.vo.user.LoginUserVO;
import com.pingyu.tracehub.interfaces.vo.user.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
 * @author 花萍雨
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2024-12-09 20:03:03
 */
public interface UserApplicationService {

    /**
     * 用户注册
     *
     * @param userRegisterRequest
     * @return 新用户 id
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 用户登录
     *
     * @param userLoginRequest
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);

    /**
     * 获取加密后的密码
     *
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获得脱敏后的登录用户信息
     *
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获得脱敏后的用户信息
     *
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获得脱敏后的用户信息列表
     *
     * @param userList
     * @return 脱敏后的用户列表
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取查询条件
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    User getUserById(long id);

    UserVO getUserVOById(long id);

    boolean deleteUser(DeleteRequest deleteRequest);

    void updateUser(User user);

    /**
     * 更新个人信息
     *
     * @param userUpdateMyRequest
     * @param request
     */
    void updateMyUser(UserUpdateMyRequest userUpdateMyRequest, HttpServletRequest request);

    Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest);

    List<User> listByIds(Set<Long> userIdSet);

    long saveUser(User userEntity);
}
