package com.hdh.lifeup.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.hdh.lifeup.base.BaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * TeamTaskDO class<br/>
 *
 * @author hdonghong
 * @since 2018/09/02
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public class TeamTaskDO extends BaseDO {

    private static final long serialVersionUID = 6742400261790303176L;

    @TableId
    private Long teamId;

    private String teamTitle;

    private String teamDesc;

    private String rewardAttr;

    private Integer rewardExp;

    /** 频率，0不重复，7则是每周重复一次；服务端最大数值为65525 */
    private Integer teamFreq;

    /** 开始（截至）日期，默认为第一次开始签到时间的日期-1，比如开始签到时间，不知道怎么叙述更好，来个文案，(/▽＼) */
    private LocalDate startDate;

    /** 开始签到的时间，必须开始后才能签到，与endTime在同一天，不填写的话默认为当天0点 */
    private LocalTime startTime;

    /** 结束签到的时间，必须开始后才能签到，与startTime在同一天，不填写的话默认为当天23.59.59点 */
    private LocalTime endTime;

    private Integer teamStatus;

    private LocalDateTime completeTime;

    private Long userId;

    @TableLogic
    private Integer isDel;

    private LocalDateTime createTime;
}
