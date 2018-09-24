package com.hdh.lifeup.redis;

import com.hdh.lifeup.dto.UserInfoDTO;
import com.hdh.lifeup.util.TokenUtil;

/**
 * @author hdonghong
 * @since 2018/09/24
 */
public class UserKey<T> extends BasePrefix<T> {

	private UserKey(int expireSeconds, String prefix, Class<T> valueClass) {
		super(expireSeconds, prefix, valueClass);
	}

	public static final UserKey<UserInfoDTO> TOKEN = new UserKey<>(
			TokenUtil.EXPIRE_SECONDS, "token", UserInfoDTO.class
	);

}
