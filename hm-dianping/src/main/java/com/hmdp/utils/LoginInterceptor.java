package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import io.netty.util.internal.StringUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LoginInterceptor implements HandlerInterceptor {

    // LoginInterceptor 不是 Spring 容器直接管理的普通 Bean，而 StringRedisTemplate 是 Spring 容器中的 Bean
    // 所以该属性不能直接注入，只能通过构造函数手动传入 StringRedisTemplate，实现依赖注入
    // 但在配置类中可以注册拦截器并自动注入 StringRedisTemplate
    /*private StringRedisTemplate stringRedisTemplate;

    public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }*/

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        /*// 1. 获取 session
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
        return true;*/

        /*// 1. 获取请求头中的 token
        String token = request.getHeader("authorization");
        if (StringUtil.isNullOrEmpty(token)) {
            // 不存在就拦截，返回 401 状态码
            response.setStatus(401);
            return false;
        }
        // 2. 基于 token 获取 redis 中的用户
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(RedisConstants.LOGIN_USER_KEY + token); // 当数据为空则返回 null
        // 3. 判断用户是否存在
        if (userMap.isEmpty()) {
            // 不存在就拦截，返回 401 状态码
            response.setStatus(401);
            return false;
        }
        // 4. 将查询到的 Hash 数据转换成 UserDTO 对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);
        // 6. 存在就保存用户信息到 ThreadLocal
        UserHolder.saveUser(userDTO);
        // 7. 刷新 token 有效期
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token, RedisConstants.LOGIN_USER_TTL, TimeUnit.SECONDS);
        // 8. 放行
        return true;*/

        // 现在由 RefreshTokenInterceptor 拦截器拦截所有路径，判断是否登录成功，该拦截器只需判断是否有有互信息即可
        // 1. 判断是否需要拦截（ThreadLocal 中是否有用户信息）
        if (UserHolder.getUser() == null) {
            // 没有，需要拦截
            response.setStatus(401);
            return false;
        }
        // 2. 有用户信息，放行
        return true;
    }


}
