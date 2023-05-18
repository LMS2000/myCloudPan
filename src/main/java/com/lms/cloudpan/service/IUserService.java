package com.lms.cloudpan.service;



import com.baomidou.mybatisplus.extension.service.IService;
import com.lms.cloudpan.entity.dao.File;
import com.lms.cloudpan.entity.dao.User;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
 * 用户服务
 *
 * @author https://github.com/liyupi
 */
public interface IUserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userName 用户名
     * @param userPassword 用户密码
     * @param checkPassword 校验密码
     * @param userRole 用户角色
     * @return 新用户 id
     */
    long userRegister(String userName, String userPassword, String checkPassword, String userRole);

    /**
     * 用户登录
     *
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    User userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 保存用户
     * @param user
     * @return
     */
     boolean save(User user);

    /**
     * 逻辑删除用户
     * @param uid
     * @return
     */
    boolean deleteUser(Integer uid);


    String uploadAvatar(MultipartFile file, Integer uid);
}
