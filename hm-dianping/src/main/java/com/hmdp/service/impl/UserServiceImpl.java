package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.DetailConstants;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.sql.Time;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 发送验证码（基于 session）
    /*@Override
    public Result sendCode(String phone, HttpSession session) {
        // 1. 校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2. 如果不符合，返回错误信息
            return Result.fail("手机号格式错误！");
        }
        // 3. 符合，则生成随机六位数的验证码
        String code = RandomUtil.randomNumbers(6);

        // 4. 保存验证码到 session
        session.setAttribute(DetailConstants.PHONE_CODE,code);
        // 5. 发送验证码，通过日志模拟
        log.debug("发送短信验证码成功，验证码：{}", code);
        // 返回ok
        return Result.ok();
    }*/

    // 发送验证码（基于 redis）
    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1. 校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2. 如果不符合，返回错误信息
            return Result.fail("手机号格式错误！");
        }
        // 3. 符合，则生成随机六位数的验证码
        String code = RandomUtil.randomNumbers(6);
        // 4. 保存验证码到 redis
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY + phone, code, RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);
        // 5. 发送验证码，通过日志模拟
        log.debug("发送短信验证码成功，验证码：{}", code);
        // 返回ok
        return Result.ok();
    }

    // 实现登录（基于 session）
    /*@Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1. 再次校验手机号（防止发送验证码后修改手机号，导致登录出现问题）
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2.如果不符合，返回错误信息
            return Result.fail("手机号格式错误！");
        }
        // 3.校验验证码
        Object cacheCode = session.getAttribute(DetailConstants.PHONE_CODE);
        String code = loginForm.getCode();
        if(cacheCode == null || !cacheCode.toString().equals(code)){
            // 4.不一致，报错
            return Result.fail("验证码错误");
        }
        // 一致，则根据手机号查询用户
        User user = query().eq(DetailConstants.PHONE, phone).one();

        // 5.判断用户是否存在
        if(user == null){
            // 6. 不存在，则创建
            user =  createUserWithPhone(phone);
        }

        // 7.保存用户信息到 session 中，使用 BeanUtil 工具类，把 User 中的信息拷贝到 UserDTO 中
        session.setAttribute(DetailConstants.USER_BEAN, BeanUtil.copyProperties(user, UserDTO.class));
        // 每一个 session 都自带一个 SessionId，它会存放在 cookie 中，只要获取到 session 就能获取到 cookie
        // 而手机号是唯一的，所以只要有手机号就能获取到 session，从而获取到唯一凭证 cookie
        return Result.ok();
    }*/

    // 实现验证码登录（基于 redis）
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1. 再次校验手机号（防止发送验证码后修改手机号，导致登录出现问题）
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 2. 如果不符合，返回错误信息
            return Result.fail("手机号格式错误！");
        }
        // 3. 从 redis 中获取验证码并校验验证码
        String cacheCode = stringRedisTemplate.opsForValue().get(RedisConstants.LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        if (cacheCode == null || !cacheCode.toString().equals(code)) {
            // 4. 不一致，报错
            return Result.fail("验证码错误");
        }
        // 一致，则根据手机号查询用户
        User user = query().eq(DetailConstants.PHONE, phone).one();

        // 5. 判断用户是否存在
        if (user == null) {
            // 6. 不存在，则创建
            user = createUserWithPhone(phone);
        }

        // 7.保存用户信息到 redis 中
        // 随机生成 token 作为登录令牌
        String token = UUID.randomUUID().toString(true); // 使用 UUID 工具类生成简单字符串类型的 token
        // 将 User 对象转为 HashMap 存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        // UserDTO userDTO = new UserDTO(user.getId(), user.getNickName(), user.getIcon());
        // 因为 StringRedisTemplate 要求 Hash 的字段和值都必须是 String，所以要把 UserDTO 中的属性全部转换成 String 类型
        Map<String, Object> userMap = BeanUtil.beanToMap(
                userDTO,
                new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true) // 忽略 null 字段
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue == null ? null : fieldValue.toString())
        );

        /*Map<String,Object> userMap = new HashMap<>();
        userMap.put("icon", userDTO.getIcon());
        userMap.put("nickName", userDTO.getNickName());
        userMap.put("id", userDTO.getId().toString());*/

        stringRedisTemplate.opsForHash().putAll(RedisConstants.LOGIN_USER_KEY + token, userMap);
        // 设置 token 有效期(60 min)
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token, RedisConstants.LOGIN_USER_TTL, TimeUnit.SECONDS);
        // 返回 token 到客户端，即存到请求头中
        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        // 保存用户，使用 mybatisplus 的 save 方法
        save(user);
        return user;
    }
}
