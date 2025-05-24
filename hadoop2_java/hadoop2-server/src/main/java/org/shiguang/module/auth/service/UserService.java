package org.shiguang.module.auth.service;

import org.shiguang.entity.User;
import java.util.List;

/**
 * 用户管理服务接口
 */
public interface UserService {

    /**
     * 获取所有用户
     * @return 用户列表
     */
    List<User> getAllUsers();

    /**
     * 获取单个用户
     * @param userId 用户ID
     * @return 用户对象，如果不存在则返回null
     */
    User getUserById(Long userId);

    /**
     * 创建用户
     * @param user 用户对象
     * @return 创建后的用户对象
     */
    User createUser(User user);

    /**
     * 更新用户
     * @param user 用户对象
     * @return 更新后的用户对象，如果不存在则返回null
     */
    User updateUser(User user);

    /**
     * 删除用户
     * @param userId 用户ID
     * @return 是否删除成功
     */
    boolean deleteUser(Long userId);

    /**
     * 更改用户密码
     * @param userId 用户ID
     * @param newPassword 新密码
     * @return 是否修改成功
     */
    boolean changePassword(Long userId, String newPassword);

    /**
     * 更改用户状态
     * @param userId 用户ID
     * @param status 新状态
     * @return 更新后的用户对象，如果不存在则返回null
     */
    User changeUserStatus(Long userId, String status);

    /**
     * 更改用户角色
     * @param userId 用户ID
     * @param role 新角色
     * @return 更新后的用户对象，如果不存在则返回null
     */
    User changeUserRole(Long userId, String role);
} 