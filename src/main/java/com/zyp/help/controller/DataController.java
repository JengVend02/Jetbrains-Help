package com.zyp.help.controller;

import com.zyp.help.context.plugin.PluginConfig;
import com.zyp.help.context.plugin.model.PluginCache;
import com.zyp.help.context.common.UpdateTimeCache;
import com.zyp.help.context.product.ProductConfig;
import com.zyp.help.context.product.model.ProductCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据接口控制器
 *
 * <p>此控制器提供前端所需的基础数据API，包括产品列表、插件列表等。
 * 这些数据用于前端界面的动态展示和用户选择。
 *
 * <p>主要功能：
 * <ul>
 *   <li>提供JetBrains产品列表数据</li>
 *   <li>提供付费插件列表数据</li>
 *   <li>支持前端动态加载数据</li>
 *   <li>返回JSON格式的结构化数据</li>
 * </ul>
 *
 * @author zyp
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j(topic = "数据接口")
@RestController
@RequestMapping("/api")
public class DataController {

    /**
     * 获取JetBrains产品列表
     *
     * <p>返回所有支持的JetBrains IDE产品信息，包括产品名称、产品代码和描述等。
     * 前端可以使用这些数据构建产品选择下拉框。
     *
     * <p>返回的产品信息包含：
     * <ul>
     *   <li>name - 产品显示名称</li>
     *   <li>productCode - 产品代码（用于许可证生成）</li>
     * </ul>
     *
     * <p>请求示例：
     * <pre>
     * GET /api/products
     * </pre>
     *
     * <p>响应示例：
     * <pre>
     * [
     *   {
     *     "name": "IntelliJ IDEA Ultimate",
     *     "productCode": "II"
     *   },
     *   {
     *     "name": "PhpStorm",
     *     "productCode": "PS"
     *   }
     * ]
     * </pre>
     *
     * @return JetBrains产品信息列表
     */
    @GetMapping("/products")
    public List<ProductCache> getProducts() {
        log.debug("获取产品列表，产品数量: {}", ProductConfig.productCache.getProduct().size());
        return ProductConfig.productCache.getProduct();
    }

    /**
     * 获取JetBrains付费插件列表
     *
     * <p>返回所有支持的JetBrains付费插件信息，包括插件名称、ID和产品代码等。
     * 前端可以使用这些数据构建插件选择下拉框。
     *
     * <p>返回的插件信息包含：
     * <ul>
     *   <li>id - 插件唯一标识符</li>
     *   <li>name - 插件显示名称</li>
     *   <li>productCode - 产品代码（用于许可证生成）</li>
     *   <li>pricingModel - 定价模式</li>
     *   <li>icon - 插件图标URL</li>
     * </ul>
     *
     * <p>请求示例：
     * <pre>
     * GET /api/plugins
     * </pre>
     *
     * <p>响应示例：
     * <pre>
     * [
     *   {
     *     "id": 7973,
     *     "name": "SonarLint",
     *     "productCode": "SONAR_LINT",
     *     "pricingModel": "PAID",
     *     "icon": "https://plugins.jetbrains.com/files/7973/icon.svg"
     *   }
     * ]
     * </pre>
     *
     * @return JetBrains付费插件信息列表
     */
    @GetMapping("/plugins")
    public List<PluginCache> getPlugins() {
        log.debug("获取插件列表，插件数量: {}", PluginConfig.pluginCache.getPlugin().size());
        return PluginConfig.pluginCache.getPlugin();
    }

    /**
     * 获取插件列表最后更新时间
     *
     * <p>返回插件信息缓存的最后一次更新时间，
     * 用于前端展示数据的新鲜度。
     *
     * <p>请求示例：
     * <pre>
     * GET /api/plugins/lastUpdateTime
     * </pre>
     *
     * <p>响应示例：
     * <pre>
     * {
     *   "lastUpdateTime": "2024-01-15 12:00:00"
     * }
     * </pre>
     *
     * @return 包含更新时间的JSON对象
     */
    @GetMapping("/plugins/lastUpdateTime")
    public List<UpdateTimeCache> getPluginLastUpdateTime() {
        return PluginConfig.pluginCache.getUpdateTime();
    }

    @GetMapping("/products/lastUpdateTime")
    public List<UpdateTimeCache> getProductLastUpdateTime() {
        return ProductConfig.productCache.getUpdateTime();
    }
}
