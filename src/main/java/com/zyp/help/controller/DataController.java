package com.zyp.help.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.zyp.help.context.LicenseContextHolder;
import com.zyp.help.context.PluginsContextHolder;
import com.zyp.help.context.ProductsContextHolder;
import com.zyp.help.context.plugin.model.PluginCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
     *   <li>iconClass - 图标CSS类名</li>
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
     *     "productCode": "II",
     *     "iconClass": "icon-idea"
     *   },
     *   {
     *     "name": "PhpStorm",
     *     "productCode": "PS", 
     *     "iconClass": "icon-phpstorm"
     *   }
     * ]
     * </pre>
     *
     * @return JetBrains产品信息列表
     */
    @GetMapping("/products")
    public List<ProductsContextHolder.ProductCache> getProducts() {
        log.debug("获取产品列表，产品数量: {}", ProductsContextHolder.productCacheList().size());
        return ProductsContextHolder.productCacheList();
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
        log.debug("获取插件列表，插件数量: {}", PluginsContextHolder.pluginCacheList().size());
        return PluginsContextHolder.pluginCacheList();
    }

    /**
     * 生成JetBrains产品激活码
     *
     * @param productCode 产品代码
     * @param licenseeName 许可证名称
     * @param assigneeName 被授权人名称
     * @param expiryDate 过期日期
     * @return 激活码字符串
     */
    @GetMapping("/generateLicense")
    public String generateLicense(
            @RequestParam(required = false) String productCode,
            @RequestParam String licenseeName,
            @RequestParam String assigneeName,
            @RequestParam String expiryDate) {
        
        Set<String> productCodeSet;
        
        if (CharSequenceUtil.isBlank(productCode)) {
            // 未指定产品代码，自动包含所有可用产品
            List<String> productCodeList = ProductsContextHolder.productCacheList()
                .stream()
                .map(ProductsContextHolder.ProductCache::getProductCode)
                .filter(StrUtil::isNotBlank)
                .map(code -> CharSequenceUtil.splitTrim(code, ","))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

            List<String> pluginCodeList = PluginsContextHolder.pluginCacheList()
                .stream()
                .map(PluginCache::getProductCode)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());

            productCodeSet = CollUtil.newHashSet(productCodeList);
            productCodeSet.addAll(pluginCodeList);
        } else {
            productCodeSet = CollUtil.newHashSet(CharSequenceUtil.splitTrim(productCode, ','));
        }

        return LicenseContextHolder.generateLicense(
            licenseeName,
            assigneeName,
            expiryDate,
            productCodeSet
        );
    }

    /**
     * 生成JetBrains插件激活码
     *
     * @param pluginId 插件ID
     * @param licenseeName 许可证名称
     * @param assigneeName 被授权人名称
     * @param expiryDate 过期日期
     * @return 插件激活码字符串
     */
    @GetMapping("/generatePluginLicense")
    public String generatePluginLicense(
            @RequestParam String pluginId,
            @RequestParam String licenseeName,
            @RequestParam String assigneeName,
            @RequestParam String expiryDate) {
        
        String productCode = PluginsContextHolder.pluginCacheList()
            .stream()
            .filter(plugin -> Objects.equals(plugin.getId(), pluginId))
            .map(PluginCache::getProductCode)
            .filter(StrUtil::isNotBlank)
            .findFirst()
            .orElse("");

        if (CharSequenceUtil.isBlank(productCode)) {
            productCode = "PLUGIN_" + pluginId;
        }

        Set<String> productCodeSet = CollUtil.newHashSet(productCode);

        return LicenseContextHolder.generateLicense(
            licenseeName,
            assigneeName,
            expiryDate,
            productCodeSet
        );
    }

    /**
     * 下载ja-netfilter代理工具
     */
    @GetMapping("/downloadAgent")
    public void downloadAgent(HttpServletResponse response) throws IOException {
        response.sendRedirect("/api/downloadZip");
    }
}
