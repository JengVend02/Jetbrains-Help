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

        /** 插件图标 */
        private String icon;

        /** 评分 */
        private Double rating;

        /** 定价模式（FREE/FREEMIUM/PAID） */
        private String pricingModel;
    }
}