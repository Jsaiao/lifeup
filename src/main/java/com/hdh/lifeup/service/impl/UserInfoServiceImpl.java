package com.hdh.lifeup.service.impl;

import com.google.common.base.Preconditions;
import com.hdh.lifeup.auth.TokenContext;
import com.hdh.lifeup.auth.UserContext;
import com.hdh.lifeup.domain.UserInfoDO;
import com.hdh.lifeup.dto.AttributeDTO;
import com.hdh.lifeup.dto.UserInfoDTO;
import com.hdh.lifeup.enums.CodeMsgEnum;
import com.hdh.lifeup.exception.GlobalException;
import com.hdh.lifeup.mapper.UserInfoMapper;
import com.hdh.lifeup.redis.RedisOperator;
import com.hdh.lifeup.redis.UserKey;
import com.hdh.lifeup.service.AttributeService;
import com.hdh.lifeup.service.TeamMemberService;
import com.hdh.lifeup.service.UserInfoService;
import com.hdh.lifeup.util.PasswordUtil;
import com.hdh.lifeup.util.TokenUtil;
import com.hdh.lifeup.vo.UserDetailVO;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * UserInfoServiceImpl class<br/>
 *
 * @author hdonghong
 * @since 2018/08/14
 */
@Slf4j
@Service
public class UserInfoServiceImpl implements UserInfoService {

    private RedisOperator redisOperator;
    private UserInfoMapper userInfoMapper;
    private AttributeService attributeService;
    private TeamMemberService memberService;

    @Autowired
    public UserInfoServiceImpl(RedisOperator redisOperator,
                               UserInfoMapper userInfoMapper,
                               AttributeService attributeService,
                               TeamMemberService memberService) {
        this.redisOperator = redisOperator;
        this.userInfoMapper = userInfoMapper;
        this.attributeService = attributeService;
        this.memberService = memberService;
    }

    @Override
    public UserInfoDTO getOne(@NonNull Long userId) {
        UserInfoDO userInfoDO = userInfoMapper.selectById(userId);
        if (userInfoDO == null) {
            log.error("【获取用户】不存在的用户，userId = [{}]", userId);
            throw new GlobalException(CodeMsgEnum.USER_NOT_EXIST);
        }
        return UserInfoDTO.from(userInfoDO, UserInfoDTO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoDTO insert(@NonNull UserInfoDTO userInfoDTO) {
        userInfoDTO.setPwdSalt(PasswordUtil.getSalt());
        UserInfoDO userInfoDO = userInfoDTO.toDO(UserInfoDO.class);
        Integer result = userInfoMapper.insert(userInfoDO);
        if (!Objects.equals(1, result)) {
            log.error("【新增用户信息】插入记录数量 = [{}], UserInfoDTO = [{}]", result, userInfoDTO);
            throw new GlobalException(CodeMsgEnum.DATABASE_EXCEPTION);
        }
        // 新建账号的时候需要顺便新建人物的属性表
        AttributeDTO attributeDTO = new AttributeDTO()
                                        .setUserId(userInfoDO.getUserId());
        attributeService.insert(attributeDTO);
        return userInfoDTO.setUserId(userInfoDO.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoDTO update(@NonNull UserInfoDTO userInfoDTO) {
        UserInfoDTO cachedUserInfoDTO = UserContext.get();
        BeanUtils.copyProperties(userInfoDTO, cachedUserInfoDTO, "userId", "createTime");
        Integer result = userInfoMapper.updateById(cachedUserInfoDTO.toDO(UserInfoDO.class));
        if (!Objects.equals(1, result)) {
            log.error("【修改用户信息】插入记录数量 = [{}], UserInfoDTO = [{}]", result, userInfoDTO);
            throw new GlobalException(CodeMsgEnum.DATABASE_EXCEPTION);
        }
        redisOperator.setex(UserKey.TOKEN, TokenContext.get(), cachedUserInfoDTO);
        return cachedUserInfoDTO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoDTO deleteLogically(
            @NotNull(message = "【删除用户】用户id不能为空") Long userId) {
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoDTO delete(
            @NotNull(message = "【删除用户】用户id不能为空") Long userId) {
        return null;
    }

    @Override
    public UserInfoDTO getByToken(String authenticityToken) {
        Preconditions.checkNotNull(authenticityToken, "【通过token获取用户】传入的Token为空");

        UserInfoDTO userInfoDTO = redisOperator.get(UserKey.TOKEN, authenticityToken);
        if (userInfoDTO == null) {
            log.info("【通过token获取用户】无效的TOKEN");
            return null;
        }

        long expire = redisOperator.ttl(UserKey.TOKEN, authenticityToken);
        if (expire < TokenUtil.MIN_EXPIRED) {
            log.info("【通过token获取用户】当前用户Token有效时长expire = [{}], 重设", expire);
            redisOperator.expire(UserKey.TOKEN, TokenContext.get());
        }
        return userInfoDTO;
    }

    @Override
    public UserDetailVO getDetailById(Long userId) {
        // 当传入的用户id为空时，返回当前登录的用户信息
        UserInfoDTO userInfoDTO = (userId == null) ?
                UserContext.get() : this.getOne(userId);

        UserDetailVO userDetailVO = new UserDetailVO();
        BeanUtils.copyProperties(userInfoDTO, userDetailVO);
        userDetailVO.setTeamAmount(memberService.countUserTeams(userInfoDTO.getUserId()));
        return userDetailVO;
    }

}
