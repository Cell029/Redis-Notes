package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取 session
        HttpSession session = request.getSession();
        // 2. 获取 session 中的用户
        Object user = session.getAttribute(DetailConstants.USER_BEAN);
        // 3. 判断用户是否存在
        if (user == null) {
            // 4. 不存在就拦截，返回 401 状态码
            response.setStatus(401);
            return false;
        }
        // 5. 存在就保存用户信息到 ThreadLocal
        UserHolder.saveUser((UserDTO) user);
        // 6. 放行
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除用户
        UserHolder.removeUser();
    }
}
