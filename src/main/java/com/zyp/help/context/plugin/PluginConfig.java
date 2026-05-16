package com.zyp.help.context.plugin;

import cn.hutool.extra.spring.SpringUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;

/**
 * 插件配置管理类
 *
 * <p>统一管理插件相关的所有配置项，避免配置获取逻辑散落在各处。
 * 使用单例模式确保配置的一致性。
 *
 * @author zyp
 * @version 1.0.0
 */
@Slf4j
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PluginConfig {

    // ==================== 常量定义 ====================

    /** JetBrains插件市场基础URL */
    public static final String PLUGIN_BASIC_URL = "https://plugins.jetbrains.com";

    /** 插件列表API地址模板 */
    public static final String PLUGIN_LIST_URL_TEMPLATE =
        PLUGIN_BASIC_URL + "/api/searchPlugins?max=%d&offset=%d&orderBy=name";

    /** 插件详情API地址模板 */
    public static final String PLUGIN_INFO_URL = PLUGIN_BASIC_URL + "/api/plugins/";

    /** 插件信息缓存文件路径 */
    public static final String PLUGIN_JSON_FILE_NAME = "external/data/plugin.json";

    // ==================== 配置字段 ====================

    /** 是否启用刷新功能 */
    private boolean refreshEnabled;

    /** 分页大小 */
    private int pageSize;

    /** 线程数量 */
    private int threadCount;

    /** 请求超时时间（毫秒） */
    private int timeout;

    // ==================== 单例实现 ====================

    private static volatile PluginConfig instance;

    /**
     * 获取配置实例
     *
     * @return 配置实例
     */
    public static PluginConfig getInstance() {
        if (instance == null) {
            synchronized (PluginConfig.class) {
                if (instance == null) {
                    instance = new PluginConfig();
                    instance.loadConfig();
                }
            }
        }
        return instance;
    }

    /**
     * 从Spring环境中加载配置
     */
    private void loadConfig() {
        try {
            Environment environment = SpringUtil.getBean(Environment.class);

            this.refreshEnabled = environment.getProperty("help.plugins.refresh-enabled", Boolean.class, true);
            this.pageSize = environment.getProperty("help.plugins.page-size", Integer.class, 20);
            this.threadCount = environment.getProperty("help.plugins.thread-count", Integer.class, 5);
            this.timeout = environment.getProperty("help.plugins.timeout", Integer.class, 30000);

            log.debug("插件配置加载完成 -> 刷新启用: {}, 分页大小: {}, 线程数: {}, 超时: {}ms",
                    refreshEnabled, pageSize, threadCount, timeout);

        } catch (Exception e) {
            log.warn("加载插件配置失败，使用默认值", e);
            setDefaultValues();
        }
    }

    /**
     * 设置默认配置值
     */
    private void setDefaultValues() {
        this.refreshEnabled = true;
        this.pageSize = 20;
        this.threadCount = 20;
        this.timeout = 30000;
    }

    /**
     * 重新加载配置
     */
    public void reload() {
        loadConfig();
    }
}
