package com.hdh.lifeup.dto;

import com.hdh.lifeup.util.JsonUtil;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * RecordDTO class<br/>
 *
 * @author hdonghong
 * @since 2018/09/16
 */
@Data
@Accessors(chain = true)
public class RecordDTO implements Serializable {

    private static final long serialVersionUID = 3280320349458708093L;

    private Long memberRecordId;

    private Long teamRecordId;

    private Long teamId;

    private String teamTitle;

    private Long userId;

    private String nickname;

    private String userHead;

    private String userActivity;

    private List<String> activityImages;

    private Integer activityIcon;

    private LocalDateTime createTime;

    public void setActivityImages(String activityImages) {
        this.activityImages = JsonUtil.jsonToList(activityImages, String.class);
    }
}
