package com.lms.cloudpan.controller;


import com.lms.cloudpan.annotation.AuthCheck;
import com.lms.cloudpan.constants.HttpCode;
import com.lms.cloudpan.constants.UserConstant;
import com.lms.cloudpan.entity.dao.User;
import com.lms.cloudpan.entity.dto.UserDto;
import com.lms.cloudpan.entity.vo.UserVo;
import com.lms.cloudpan.exception.BusinessException;
import com.lms.cloudpan.service.IUserService;
import com.lms.result.EnableResponseAdvice;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/user")
@EnableResponseAdvice
public class UserController {

    @Resource
    private IUserService userService;

    /**
     * 注册
     * @param userVo
     * @return
     */

    @PostMapping( "/register")
    public Long registerUser(@RequestBody UserVo userVo){
        if (userVo == null) {
            throw new BusinessException(HttpCode.PARAMS_ERROR);
        }
        String userName = userVo.getUsername();
        String userPassword = userVo.getPassword();
        String checkPassword = userVo.getCheckPassword();
        if (StringUtils.isAnyBlank(userName, userPassword, checkPassword)) {
            throw new BusinessException(HttpCode.PARAMS_ERROR);
        }
        return userService.userRegister(userName, userPassword, checkPassword, UserConstant.DEFAULT_ROLE);
    }

    /**
     * 登录
     * @param userVo
     * @param request
     * @return
     */
    @PostMapping("/login")
    public UserDto userLogin(@RequestBody UserVo userVo, HttpServletRequest request){
        if (userVo == null) {
            throw new BusinessException(HttpCode.PARAMS_ERROR);
        }
        String username = userVo.getUsername();
        String userPassword = userVo.getPassword();
        if (StringUtils.isAnyBlank(username, userPassword)) {
            throw new BusinessException(HttpCode.PARAMS_ERROR);
        }
        User user = userService.userLogin(username, userPassword, request);
        UserDto userDto=new UserDto();
        BeanUtils.copyProperties(user,userDto);
        return userDto;
    }

    /**
     * 注销
     * @param request
     * @return
     */
   @PostMapping("/logout")
   public Boolean logout(HttpServletRequest request){
        if (request == null) {
            throw new BusinessException(HttpCode.PARAMS_ERROR);
        }
        return userService.userLogout(request);
    }


    /**
     * 获取当前用户
     * @param request
     * @return
     */
    @GetMapping(value = "/get/login")
    public UserDto getCurrentUser(HttpServletRequest request){
        User loginUser = userService.getLoginUser(request);
       UserDto userDto=new UserDto();
        BeanUtils.copyProperties(loginUser,userDto);
        return userDto;
    }

    /**
     * 添加用户
     * @param userVo
     * @param request
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public Integer add(@RequestBody(required = true)UserVo userVo,HttpServletRequest request){
        if (userVo == null) {
            throw new BusinessException(HttpCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userVo, user);
        boolean result = userService.save(user);
        if (!result) {
            throw new BusinessException(HttpCode.OPERATION_ERROR);
        }
        return user.getUserId();
    }

    /**
     * 删除用户,逻辑删除
     * @param userVo
     * @param request
     * @return
     */
   @DeleteMapping("/delete")
   @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
   public Boolean delete(@RequestBody UserVo userVo,HttpServletRequest request){
        if (userVo == null || userVo.getUserId() <= 0) {
            throw new BusinessException(HttpCode.PARAMS_ERROR);
        }
        return userService.deleteUser(userVo.getUserId());
   }

    /**
     * 上传头像
     * @param file
     * @param request
     * @return 返回头像图片地址
     */
   @PostMapping("/uploadAvatar")
   public String uploadAvatar(@RequestParam MultipartFile file ,HttpServletRequest request){
       Integer userId = userService.getLoginUser(request).getUserId();
       return userService.uploadAvatar(file,userId);
   }

//    /**
//     * 更新用户
//     *
//     * @param userUpdateRequest
//     * @param request
//     * @return
//     */
//    @PostMapping("/update")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
//    public ResultData updateUser(@RequestBody UserUpdateRequest userUpdateRequest,
//                                            HttpServletRequest request) {
//        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        User user = new User();
//        BeanUtils.copyProperties(userUpdateRequest, user);
//        boolean result = userService.updateById(user);
//        return ResultUtils.success(result);
//    }
//    /**
//     * 获取用户列表
//     *
//     * @param userQueryRequest
//     * @param request
//     * @return
//     */
//    @GetMapping("/list")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
//    public BaseResponse<List<UserVO>> listUser(UserQueryRequest userQueryRequest, HttpServletRequest request) {
//        User userQuery = new User();
//        if (userQueryRequest != null) {
//            BeanUtils.copyProperties(userQueryRequest, userQuery);
//        }
//        QueryWrapper<User> queryWrapper = new QueryWrapper<>(userQuery);
//        List<User> userList = userService.list(queryWrapper);
//        List<UserVO> userVOList = userList.stream().map(user -> {
//            UserVO userVO = new UserVO();
//            BeanUtils.copyProperties(user, userVO);
//            return userVO;
//        }).collect(Collectors.toList());
//        return ResultUtils.success(userVOList);
//    }
//    /**
//     * 根据 id 获取用户
//     *
//     * @param id
//     * @param request
//     * @return
//     */
//    @GetMapping("/get")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
//    public BaseResponse<UserVO> getUserById(int id, HttpServletRequest request) {
//        if (id <= 0) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        User user = userService.getById(id);
//        UserVO userVO = new UserVO();
//        BeanUtils.copyProperties(user, userVO);
//        return ResultUtils.success(userVO);
//    }
//
//
//
//
//
//
//
//    /**
//     * 获取用户分页列表
//     * @param userQueryRequest
//     * @param request
//     * @return
//     */
//
//    @GetMapping("/list/page")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
//    public BaseResponse<Page<UserVO>> page(UserQueryRequest userQueryRequest,HttpServletRequest request){
//        long current = 1;
//        long size = 10;
//        User userQuery = new User();
//        if (userQueryRequest != null) {
//            BeanUtils.copyProperties(userQueryRequest, userQuery);
//            current = userQueryRequest.getCurrent();
//            size = userQueryRequest.getPageSize();
//        }
//        QueryWrapper<User> wrapper=new QueryWrapper<>(userQuery);
//
//        Page<User> userPage=userService.page(new Page<>(current,size),wrapper);
//        Page<UserVO> userVOPage = new PageDTO<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
//        List<UserVO> userVOList=userPage.getRecords().stream().map(user->{
//            UserVO userVO=new UserVO();
//            BeanUtils.copyProperties(user,userVO);
//            return userVO;
//        }).collect(Collectors.toList());
//        userVOPage.setRecords(userVOList);
//        return ResultUtils.success(userVOPage);
//    }

}
