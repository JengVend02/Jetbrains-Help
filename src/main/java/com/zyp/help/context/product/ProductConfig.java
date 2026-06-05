package com.zyp.help.context.product;

import cn.hutool.extra.spring.SpringUtil;
import com.zyp.help.context.product.model.ProductCache;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;

import java.util.List;

/**
 * 产品配置管理类
 *
 * <p>统一管理产品相关的所有配置项，避免配置获取逻辑散落在各处。
 * 使用单例模式确保配置的一致性。
 *
 * @author zyp
 * @version 1.0.0
 */
@Slf4j
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductConfig {

    // 此方法获取产品信息(产品编码,名称,链接,描述)
    // https://data.services.jetbrains.com/products?fields=code,name,link,description

    // ==================== 常量定义 ====================

    /** 产品信息配置文件路径 */
    public static final String PRODUCT_JSON_FILE_NAME = "external/data/product.json";

    /** JetBrains产票基础URL */
    public static final String PRODUCT_BASIC_URL = "https://data.services.jetbrains.com";

    /** 产品列表API地址模板 */
    public static final String PRODUCT_LIST_URL_TEMPLATE = PRODUCT_BASIC_URL + "/products?fields=code,name,link,description,forSale";

    /** 产品icon地址模板 */
    public static final String PRODUCT_ICON_URL = "https://resources.jetbrains.com/storage/logos/web/{product}/{product}.svg";

    // ==================== 静态字段 ====================

    /** 产品信息缓存列表，存储所有加载的产品信息 */
    public static List<ProductCache> productCacheList;

    // ==================== 配置字段 ====================

    /** 是否启用刷新功能 */
    private boolean refreshEnabled;

    /** 请求超时时间（毫秒） */
    private int timeout;

    // ==================== 单例实现 ====================

    private static volatile ProductConfig instance;

    /**
     * 获取配置实例
     *
     * @return 配置实例
     */
    public static ProductConfig getInstance() {
        if (instance == null) {
            synchronized (ProductConfig.class) {
                if (instance == null) {
                    instance = new ProductConfig();
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

            this.refreshEnabled = environment.getProperty("help.product.refresh-enabled", Boolean.class, true);
            this.timeout = environment.getProperty("help.product.timeout", Integer.class, 30000);

            log.debug("插件配置加载完成 -> 刷新启用: {}, 超时: {}ms", refreshEnabled, timeout);

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
        this.timeout = 30000;
    }
}
