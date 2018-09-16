package com.hdh.lifeup.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * MembersVO class<br/>
 * 团队成员VO类
 * @author hdonghong
 * @since 2018/09/16
 */
@ApiModel("团队成员VO类")
@Data
@Accessors(chain = true)
public class MembersVO {

    /** '用户昵称' */
    private String nickName;

    /**'用户地区'  */
    private String userAddress;

    /** 用户头像 */
    private String userHead;

    @ApiModelProperty("1已关注，0未关注")
    private Integer isFollow;
}
