package com.zyp.help.context.plugin.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 插件更新时间缓存数据模型
 *
 */
@Data
@Accessors(chain = true)
public class PluginUpdateTimeCache {

    /** 原有数量 */
    private Integer oldNum;

    /** 新增数量 */
    private Integer addNum;

    /** 最新数量 */
    private Integer newNum;

    /** 更新时间 */
    private String updateTime;
}