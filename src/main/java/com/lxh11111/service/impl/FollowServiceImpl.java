package com.lxh11111.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lxh11111.dto.Result;
import com.lxh11111.dto.UserDTO;
import com.lxh11111.entity.Follow;
import com.lxh11111.mapper.FollowMapper;
import com.lxh11111.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lxh11111.service.IUserService;
import com.lxh11111.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;
    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        //获取用户id
        Long userId= UserHolder.getUser().getId();
        //判断取关还是关注
        String key="follows:"+userId;
        if(isFollow){
            //新增数据
            Follow follow=new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean isSuccess=save(follow);
            if(isSuccess){
                //存到redis--set取交集查看共同好友
                stringRedisTemplate.opsForSet().add(key,followUserId.toString());
            }
        }else{
            //取关
            boolean isSuccess=remove(new QueryWrapper<Follow>()
                    .eq("user_id",userId)
                    .eq("follow_user_id",followUserId)
            );
            if(isSuccess) stringRedisTemplate.opsForSet().remove(key,followUserId.toString());
        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Long followUserId) {
        //查询是否关注
        Long useId=UserHolder.getUser().getId();
        Integer count=query().eq("user_id",useId).eq("follow_user_id",followUserId).count();
        return Result.ok(count>0);
    }

    @Override
    public Result followCommons(Long id) {
        //获取当前用户
        Long userId=UserHolder.getUser().getId();
        String key1="follows:"+userId;
        //交集
        String key2="follows:"+id;
        Set<String> intersect=stringRedisTemplate.opsForSet().intersect(key1,key2);
        if(intersect==null){
            return Result.ok(Collections.emptyList());
        }
        //获取id
        List<Long> ids=intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        List<UserDTO> users=userService.listByIds(ids)
                .stream()
                .map(user-> BeanUtil.copyProperties(user,UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(users);
    }
}
