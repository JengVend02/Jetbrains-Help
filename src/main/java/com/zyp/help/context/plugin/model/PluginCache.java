package com.zyp.help.context.plugin.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PluginCache {

    /** 插件ID */
    private Long id;

    /** 产品代码 */
    private String productCode;

    /** 插件链接 */
    private String link;

    /** 插件名称 */
    private String name;

    /** 定价模式（FREE/FREEMIUM/PAID）*/
    private String pricingModel;

    /** 插件图标URL */
    private String icon;

    /** 评分 */
    private Double rating;

    /** 开发商信息 */
    private VendorInfo vendor;

    /**
     * 开发商信息
     */
    @Data
    @Accessors(chain = true)
    public static class VendorInfo {
        /** 开发商ID */
        private Long id;

        /** 开发商名称 */
        private String name;

        /** 是否已认证 */
        private Boolean isVerified;
    }

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

    public String getIdStr() {
        return String.valueOf(id);
    }
}
