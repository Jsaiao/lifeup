package com.hdh.lifeup.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.hdh.lifeup.model.constant.AuthTypeConst;
import com.hdh.lifeup.model.domain.UserAuthDO;
import com.hdh.lifeup.model.dto.UserAuthDTO;
import com.hdh.lifeup.model.dto.UserInfoDTO;
import com.hdh.lifeup.model.enums.CodeMsgEnum;
import com.hdh.lifeup.exception.GlobalException;
import com.hdh.lifeup.dao.UserAuthMapper;
import com.hdh.lifeup.redis.RedisOperator;
import com.hdh.lifeup.redis.UserKey;
import com.hdh.lifeup.service.UserAuthService;
import com.hdh.lifeup.service.UserInfoService;
import com.hdh.lifeup.util.PasswordUtil;
import com.hdh.lifeup.util.TokenUtil;
import com.hdh.lifeup.model.vo.UserAuthVO;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * UserAuthServiceImpl class<br/>
 *
 * @author hdonghong
 * @since 2018/08/22
 */
@Slf4j
@Service
public class UserAuthServiceImpl implements UserAuthService {

    private RedisOperator redisOperator;
    private UserAuthMapper userAuthMapper;
    private UserInfoService userInfoService;

    @Autowired
    public UserAuthServiceImpl(RedisOperator redisOperator,
                               UserAuthMapper userAuthMapper,
                               UserInfoService userInfoService) {
        this.redisOperator = redisOperator;
        this.userAuthMapper = userAuthMapper;
        this.userInfoService = userInfoService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserAuthDTO insert(UserAuthDTO userAuthDTO) {
        Preconditions.checkNotNull(userAuthDTO, "【新增用户鉴权记录】UserAuthDTO类不能为空");
        UserAuthDO userAuthDO = userAuthDTO.toDO(UserAuthDO.class);

        // 新增前检查是否已经注册过
        UserAuthDO queryResult = userAuthMapper.selectOne(
                new QueryWrapper<UserAuthDO>().eq("auth_type", userAuthDO.getAuthType())
                        .eq("auth_identifier", userAuthDO.getAuthIdentifier())
        );
        if (queryResult != null) {
            log.error("【新增用户授权】该授权类型对应的唯一标识已经被注册过，UserAuthDO = [{}]", queryResult);
            throw new GlobalException(CodeMsgEnum.ACCOUNT_ALREADY_EXISTED);
        }

        Integer result = userAuthMapper.insert(userAuthDO);
        if (!Objects.equals(1, result)) {
            log.error("【新增用户授权】插入记录数量 = [{}], UserAuthDTO = [{}]", result, userAuthDTO);
            throw new GlobalException(CodeMsgEnum.DATABASE_EXCEPTION);
        }
        userAuthDTO.setAuthId(userAuthDO.getAuthId());
        return userAuthDTO;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public String oauthLogin(UserAuthDTO userAuthDTO, UserInfoDTO userInfoDTO) {
        Preconditions.checkNotNull(userAuthDTO, "【授权登陆】传入的UserAuthDTO为空");
        Preconditions.checkNotNull(userInfoDTO, "【授权登陆】传入的UserInfoDTO为空");

        // authType和authIdentifier查user_auth，取userId
        UserAuthDO userAuthDO = userAuthMapper.selectOne(
            new QueryWrapper<UserAuthDO>().eq("auth_type", userAuthDTO.getAuthType())
                    .eq("auth_identifier", userAuthDTO.getAuthIdentifier())
        );

        // userId 非空 就查user_info，取user信息，userInfoDTO放到缓存，返回token
        UserInfoDTO userInfoResult;
        if (userAuthDO != null) {
            userInfoResult = userInfoService.getOne(userAuthDO.getUserId());
        } else {
            // userId 为空 插入userInfoDTO到user_info
            userInfoDTO.setAuthTypes(Lists.newArrayList(userAuthDTO.getAuthType()));
            userInfoResult = userInfoService.insert(userInfoDTO);

            // 取生成的userInfoDTO.getUserId，set到userAuthDTO并存到user_auth
            userAuthDTO.setUserId(userInfoResult.getUserId());
            insert(userAuthDTO);
        }
        // 返回token
        return this.generateToken(userInfoResult);
    }

    @Override
    public String appLogin(@NonNull UserAuthDTO userAuthDTO) {
        // authType和authIdentifier查user_auth，取userId
        UserAuthDO userAuthDO = userAuthMapper.selectOne(
                new QueryWrapper<UserAuthDO>().eq("auth_type", userAuthDTO.getAuthType())
                        .eq("auth_identifier", userAuthDTO.getAuthIdentifier())
        );
        if (userAuthDO == null) {
            throw new GlobalException(CodeMsgEnum.USER_NOT_EXIST);
        }
        UserInfoDTO userInfoResult = userInfoService.getOne(userAuthDO.getUserId());
        boolean match = PasswordUtil.checkPwd(
                    userAuthDTO.getAccessToken(), userInfoResult.getPwdSalt(), userAuthDO.getAccessToken());
        if (!match) {
            log.error("【APP账号登录】密码错误");
            throw new GlobalException(CodeMsgEnum.PASSWORD_ERROR);
        }
        // 返回token
        return this.generateToken(userInfoResult);
    }

    @Override
    public String codeLogin(@NonNull UserAuthDTO userAuthDTO) {
        // authType和authIdentifier查user_auth，取userId
        UserAuthDO userAuthDO = userAuthMapper.selectOne(
                new QueryWrapper<UserAuthDO>().eq("auth_type", userAuthDTO.getAuthType())
                        .eq("auth_identifier", userAuthDTO.getAuthIdentifier())
        );
        if (userAuthDO == null) {
            throw new GlobalException(CodeMsgEnum.USER_NOT_EXIST);
        }
        UserInfoDTO userInfoResult = userInfoService.getOne(userAuthDO.getUserId());
        // 返回token
        return this.generateToken(userInfoResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String register(@NonNull UserAuthVO userAuthVO) {
        UserInfoDTO userInfoDTO = new UserInfoDTO();
        BeanUtils.copyProperties(userAuthVO, userInfoDTO);
        // 注册类型：目前有手机号、QQ
        userInfoDTO.setAuthTypes(Lists.newArrayList(userAuthVO.getAuthType()));
        UserInfoDTO userInfoResult = userInfoService.insert(userInfoDTO);

        log.info("【注册账号】clientPwd = [{}]", userAuthVO.getAccessToken());

        UserAuthDTO userAuthDTO = new UserAuthDTO();
        // 取生成的userInfoDTO.getUserId，set到userAuthDTO并存到user_auth
        userAuthDTO.setAuthType(userAuthVO.getAuthType())
                   .setUserId(userInfoResult.getUserId())
                   .setAuthIdentifier(userAuthVO.getAuthIdentifier());
        if (AuthTypeConst.PHONE.equals(userAuthVO.getAuthType())) {
            userAuthDTO.setAccessToken(PasswordUtil.convertClientPwdToDbPwd(userAuthVO.getAccessToken(), userInfoResult.getPwdSalt()));
        }
        insert(userAuthDTO);
        // 返回token
        return this.generateToken(userInfoResult);
    }

    /**
     * 用户信息userInfoDTO放到缓存，返回token
     * @param userInfoDTO 用户信息
     * @return token
     */
    private String generateToken(UserInfoDTO userInfoDTO) {
        String token = TokenUtil.get();
        redisOperator.setex(UserKey.TOKEN, token, userInfoDTO);
        return token;
    }
}
