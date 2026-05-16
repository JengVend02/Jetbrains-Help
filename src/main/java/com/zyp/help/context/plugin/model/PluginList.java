package com.zyp.help.context.plugin.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 插件列表数据模型
 *
 * @author zyp
 * @version 1.0.0
 */
@Data
@Accessors(chain = true)
public class PluginList {

    /** 插件列表 */
    private List<Plugin> plugins;

    /** 插件总数 */
    private Long total;

    /**
     * 插件基本信息
     */
    @Data
    @Accessors(chain = true)
    public static class Plugin {

        /** 插件ID */
        private Long id;

        /** 插件名称 */
        private String name;

        /** 插件预览描述 */
        private String preview;

        /** 下载次数 */
        private Integer downloads;

        /** 定价模式（FREE/FREEMIUM/PAID） */
        private String pricingModel;

        /** 组织名称 */
        private String organization;

        /** 插件图标路径 */
        private String icon;

        /** 预览图片路径 */
        private String previewImage;

        /** 评分 */
        private Double rating;

        /** 开发商信息 */
        private VendorInfo vendorInfo;
    }

    /**
     * 开发商信息
     */
    @Data
    @Accessors(chain = true)
    public static class VendorInfo {

        /** 开发商名称 */
        private String name;

        /** 是否为认证开发商 */
        private Boolean isVerified;
    }
}