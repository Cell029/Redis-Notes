package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IFollowService followService;

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户与点赞
        records.forEach(blog ->{
            queryBlogUser(blog);
            isBlogLiked(blog);
        });
        return Result.ok(records);
    }

    @Override
    public Result queryById(Long id) {
        // 1. 查询 blog
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("笔记不存在！");
        }
        // 2. 查询 blog 相关用户
        queryBlogUser(blog);
        // 3. 查询 blog 是否已点赞
        isBlogLiked(blog);
        return Result.ok(blog);
    }

    private void isBlogLiked(Blog blog) {
        // 1. 获取登录用户
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            // 用户未登录，无需查询是否点赞
            return;
        }
        // 2. 判断当前登录用户是否已经点赞
        String key = RedisConstants.BLOG_LIKED_KEY + blog.getId();
        // Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
        Double score = stringRedisTemplate.opsForZSet().score(key, user.getId().toString());
        // 为 true 证明已经点过赞了
        // blog.setIsLike(BooleanUtil.isTrue(isMember));
        blog.setIsLike(score != null);
    }

    @Override
    public Result likeBlog(Long id) {
        // 1. 获取登录用户
        Long userId = UserHolder.getUser().getId();
        // 2. 判断当前登录用户是否已经点赞
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        // Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        /*if (BooleanUtil.isFalse(isMember)) {
            // 3. 如果未点赞，则该用户可以点赞
            // 数据库点赞数+1
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            // 保存用户信息到 Redis 的 set 集合，以此作为已点赞
            if (isSuccess) {
                stringRedisTemplate.opsForSet().add(key, userId.toString());
            }
        }*/
        if (score == null) {
            boolean isSuccess = update().setSql("liked = liked + 1").eq("id", id).update();
            // 保存用户到 Redis 的 sortedset 集合，用当前时间作为排序标识  zadd key value score
            if (isSuccess) {
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        } else {
            // 4. 如果已点赞，取消点赞
            // 数据库点赞数-1
            boolean isSuccess = update().setSql("liked = liked - 1").eq("id", id).update();
            // 把用户从 Redis 的 set 集合移除
            if (isSuccess) {
                // stringRedisTemplate.opsForSet().remove(key, userId.toString());
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result queryBlogLikes(Long id) {
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        // 1. 查询 top5 的点赞用户 zrange key 0 4
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);
        if (top5 == null || top5.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        // 2. 解析出其中的用户 id
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        String idStr = StrUtil.join(",", ids); // 在 id 后面拼接 ','
        // 因为查询语句用的是 in，所以最终查出的数据可能与真实的插入顺序无关，所以需要手动设置最后的排序，让 sorted 前面的数据做头
        // 3. 根据用户 id 查询用户 WHERE id IN ( 5 , 1 ) ORDER BY FIELD(id, 5, 1)
        List<UserDTO> userDTOS = userService.query()
                .in("id", ids)
                .last("ORDER BY FIELD(id," + idStr + ")").list() // 在后面拼接，手动设置排序
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        // 4.返回
        return Result.ok(userDTOS);
    }

    @Override
    public Result saveBlog(Blog blog) {
        // 1. 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 2. 保存探店笔记
        boolean isSuccess = save(blog);
        if(!isSuccess){
            return Result.fail("新增笔记失败!");
        }
        // 3. 查询笔记作者的所有粉丝 select * from tb_follow where follow_user_id = ?
        List<Follow> follows = followService.query().eq("follow_user_id", user.getId()).list();
        // 4. 推送笔记 id 给所有粉丝
        for (Follow follow : follows) {
            // 获取粉丝 id
            Long userId = follow.getUserId();
            // 推送
            String key = RedisConstants.FEED_KEY + userId;
            stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), System.currentTimeMillis());
        }
        // 5.返回id
        return Result.ok(blog.getId());
    }

    @Override
    public Result queryBlogOfFollow(Long max, Integer offset) {
        // 1. 获取当前用户
        Long userId = UserHolder.getUser().getId();
        // 2. 查询收件箱 ZREVRANGEBYSCORE key Max Min LIMIT offset count
        String key = RedisConstants.FEED_KEY + userId;
        Set<ZSetOperations.TypedTuple<String>> typedTuples =
                stringRedisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, 0, max, offset, 2);
        // 3. 非空判断
        if (typedTuples == null || typedTuples.isEmpty()) {
            return Result.ok();
        }
        // 4. 解析数据：blogId、minTime（时间戳）、offset
        List<Long> ids = new ArrayList<>(typedTuples.size());
        long minTime = 0;
        int os = 1; // 计算本次获取的数据中有几个相同的最小值
        for (ZSetOperations.TypedTuple<String> tuple : typedTuples) {
            // 获取 id 并保存在 List 集合中，因为取出的东西是自带顺序的，所以添加进 List 的最后一个元素就是本次查询的最小值
            ids.add(Long.valueOf(tuple.getValue()));
            // 获取 score(时间戳）
            long time = tuple.getScore().longValue();
            // 判断当前遍历到的 score（时间戳）是否与最小 score 一致
            if(time == minTime){
                // 一致
                os++;
            }else{
                // 不一致，跟新最小 score（时间戳）
                minTime = time;
                // 防止最小值前面有非最小的相同 score，所以出现不一致时重置偏移量为 1
                os = 1;
            }
        }
        // 判断当前的最小 score 是否等于最大的 score，如果相等那直接让 os 作为下一次查询的偏移量，
        // 如果不相等，证明此时已经走完了上一次传来的
        if (offset > 1 && minTime == max) {
            os = os + offset;
        }
        // os = minTime == max ? os + offset : 1;
        // 5. 根据 id 查询 blog
        String idStr = StrUtil.join(",", ids); // 与之前的内容同理，mybatisplus 的查询用到了 in，所以要在后面拼接 order by 手动设置查询顺序
        List<Blog> blogs = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
        // 接收到推送后应该更新它的点赞与
        for (Blog blog : blogs) {
            // 查询 blog 有关的用户信息
            queryBlogUser(blog);
            // 查询 blog 是否被点赞
            isBlogLiked(blog);
        }
        // 6. 封装并返回
        ScrollResult r = new ScrollResult();
        r.setList(blogs);
        r.setOffset(os);
        r.setMinTime(minTime);
        return Result.ok(r);
    }

    private void queryBlogUser(Blog blog) {
        Long userId = blog.getUserId();
        User user = userService.getById(userId);
        blog.setName(user.getNickName());
        blog.setIcon(user.getIcon());
    }
}
