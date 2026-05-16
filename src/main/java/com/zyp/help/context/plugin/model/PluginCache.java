package com.zyp.help.context.plugin.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 插件缓存数据模型
 *
 * @author zyp
 * @version 1.0.0
 */
@Data
@Accessors(chain = true)
public class PluginCache {

    /** 插件ID */
    private Long id;

    /** 产品代码 */
    private String productCode;

    /** 插件名称 */
    private String name;

    /** 定价模式 */
    private String pricingModel;

    /** 插件图标URL */
    private String icon;

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PluginCache)) {
            return false;
        }

        return id.equals(((PluginCache) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}