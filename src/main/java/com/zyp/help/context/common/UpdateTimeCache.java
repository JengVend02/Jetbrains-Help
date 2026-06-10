package com.zyp.help.context.common;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 插件更新时间缓存数据模型
 *
 */
@Data
@Accessors(chain = true)
public class UpdateTimeCache {

    /** 原有数量 */
    private Integer oldNum;

    /** 新增数量 */
    private Integer addNum;

    /** 最新数量 */
    private Integer newNum;

    /** 更新时间 */
    private String updateTime;

    public UpdateTimeCache() {
    }

    public UpdateTimeCache(Integer oldNum, Integer addNum) {
        this.oldNum = oldNum;
        this.addNum = addNum;
        this.newNum = oldNum + addNum;
        this.updateTime = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}