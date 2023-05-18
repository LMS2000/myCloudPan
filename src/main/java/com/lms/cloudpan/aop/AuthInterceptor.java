package com.lms.cloudpan.aop;


import com.lms.cloudpan.annotation.AuthCheck;
import com.lms.cloudpan.constants.HttpCode;
import com.lms.cloudpan.entity.dao.User;
import com.lms.cloudpan.exception.BusinessException;
import com.lms.cloudpan.service.IUserService;
import org.apache.commons.lang3.StringUtils;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 鉴权拦截器
 */
@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private IUserService userService;

    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        List<String> anyRolesList = Arrays.stream(authCheck.anyRole()).filter(StringUtils::isNotBlank).collect(Collectors.toList());

        String mustRole = authCheck.mustRole();

        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

        User loginUser = userService.getLoginUser(request);
        //如果没有包含角色就返回无权限
        if (anyRolesList!=null&&anyRolesList.size()>0) {
            String userRole = loginUser.getUserRole();
            if (!anyRolesList.contains(userRole)) {
                throw new BusinessException(HttpCode.NO_AUTH_ERROR);
            }
        }
        //如果没有包含必须的角色就报错无权限
        if (StringUtils.isNotBlank(mustRole)) {
            String userRole = loginUser.getUserRole();
            if (!mustRole.equals(userRole)) {
                throw new BusinessException(HttpCode.NO_AUTH_ERROR);
            }
        }

        return joinPoint.proceed();
    }
}
