package com.lms.cloudpan.service.impl;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lms.cloudpan.client.OssClient;
import com.lms.cloudpan.config.OssProperties;
import com.lms.cloudpan.constants.HttpCode;
import com.lms.cloudpan.constants.QuotaConstants;
import com.lms.cloudpan.constants.UserConstant;
import com.lms.cloudpan.entity.dao.Folder;
import com.lms.cloudpan.entity.dao.User;
import com.lms.cloudpan.exception.BusinessException;
import com.lms.cloudpan.mapper.FolderMapper;
import com.lms.cloudpan.mapper.UserMapper;
import com.lms.cloudpan.service.IUserService;
import com.lms.cloudpan.utis.MybatisUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.lms.cloudpan.constants.FileConstants.STATIC_REQUEST_PREFIX;
import static com.lms.cloudpan.constants.UserConstant.ADMIN_ROLE;
import static com.lms.cloudpan.constants.UserConstant.USER_LOGIN_STATE;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper,User> implements IUserService {

    @Resource
    private UserMapper userMapper;


    @Resource
    private FolderMapper folderMapper;
    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "luomosan";

    @Resource
    private OssClient ossClient;

    @Resource
    private OssProperties ossProperties;


    @Override
    public long userRegister(String userName, String userPassword, String checkPassword, String userRole) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userName, userPassword, checkPassword)) {
            throw new BusinessException(HttpCode.PARAMS_ERROR, "参数为空");
        }
        if (userName.length() > 16) {
            throw new BusinessException(HttpCode.PARAMS_ERROR, "用户名过长");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(HttpCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(HttpCode.PARAMS_ERROR, "两次密码不一致");
        }
        //使用同步块来保证在高并发的环境下,同一时间有很多人用同一个账号注册冲突
        synchronized (userName.intern()) {
            boolean existCheck = MybatisUtils.existCheck(this, Map.of("username", userName));
            if (existCheck) {
                throw new BusinessException(HttpCode.PARAMS_ERROR, "账号已存在！");
            }
            //查找用户是否存在


            // 3. 插入数据
            User user = new User();
            user.setUsername(userName);
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            user.setPassword(encryptPassword);
            //设置用户角色
            user.setUserRole(UserConstant.DEFAULT_ROLE);
            user.setUseQuota(QuotaConstants.EMTRY_QUOTA);
            user.setQuota(QuotaConstants.USER_QUOTA);
            //插入用户
            Integer integer = userMapper.insert(user);
            if (integer <= 0) {
                throw new BusinessException(HttpCode.OPERATION_ERROR, "注册失败");
            }
            //设置用户根目录

            Folder folder = new Folder();
            folder.setParentFolder(0);
            folder.setFolderName("/root");
            folder.setUserId(user.getUserId());
            folderMapper.insert(folder);
            return user.getUserId();
        }
    }

    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(HttpCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(HttpCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(HttpCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

        User user = userMapper.selectOne(new QueryWrapper<User>().eq("username",userAccount)
                .eq("password",encryptPassword));
        // 用户不存在
        if (user == null) {
//            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(HttpCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return user;

    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) attribute;
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new BusinessException(HttpCode.NOT_LOGIN_ERROR, "未登录");
        }
        Integer id = currentUser.getUserId();
//        User byId = this.getById(id);  根据用户id查找用户
        User byId = userMapper.selectById(id);
        if (byId == null) {
            throw new BusinessException(HttpCode.NOT_LOGIN_ERROR, "未登录");
        }

        return byId;
    }

    @Override
    public boolean isAdmin(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute(USER_LOGIN_STATE);

        User user = (User) attribute;

        return user != null && ADMIN_ROLE.equals(user.getUserRole());
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute(USER_LOGIN_STATE);


        if (attribute == null) {
            throw new BusinessException(HttpCode.NOT_LOGIN_ERROR, "未登录");
        }

        request.getSession().removeAttribute(USER_LOGIN_STATE);

        return true;
    }

    @Override
    public boolean save(User user) {
        return userMapper.insert(user) > 0;
    }

    @Override
    public boolean deleteUser(Integer uid) {
        //先查找用户是否存在
        boolean existCheck = MybatisUtils.existCheck(this, Map.of("user_id", uid));
        if (!existCheck) {
            throw new BusinessException(HttpCode.PARAMS_ERROR, "找不到要删除的用户");
        }
        return userMapper.updateById(User.builder().isEnable(1).userId(uid).build()) > 0;
    }

    @Override
    public String uploadAvatar(MultipartFile file, Integer uid) {
        validAvatar(file);
        String fileName;
        try {
            fileName = com.lms.cloudpan.utis.FileUtil.generatorFileName(file.getOriginalFilename() == null ? file.getName() : file.getOriginalFilename());
            ossClient.putObject("system", fileName, file.getInputStream());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return com.lms.cloudpan.utis.FileUtil.getFileUrl(ossProperties.getEndpoint(), STATIC_REQUEST_PREFIX, "system", fileName);
    }

    private void validAvatar(MultipartFile file) {
        long size = file.getSize();
        // 文件后缀
        String fileSuffix = FileUtil.getSuffix(file.getOriginalFilename());
        final long ONE_M = 1024 * 1024L;
        if (size > ONE_M) {
            throw new BusinessException(HttpCode.PARAMS_ERROR, "文件大小不能超过 10M");
        }
        if (!Arrays.asList("jpeg", "jpg", "svg", "png", "webp").contains(fileSuffix)) {
            throw new BusinessException(HttpCode.PARAMS_ERROR, "文件类型错误");
        }
    }
}
