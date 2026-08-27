package com.zyp.help.controller;

import com.zyp.help.context.CommonContextHolder;
import com.zyp.help.context.license.LicenseConfig;
import com.zyp.help.context.license.model.GenerateLicenseBody;
import com.zyp.help.context.plugin.PluginConfig;
import com.zyp.help.context.plugin.model.PluginCache;
import com.zyp.help.context.common.UpdateTimeCache;
import com.zyp.help.context.product.ProductConfig;
import com.zyp.help.context.product.model.ProductCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
     */
    @GetMapping("/products")
    public List<ProductCache> getProducts() {
        log.debug("获取产品列表，产品数量: {}", ProductConfig.productCache.getProduct().size());
        return ProductConfig.productCache.getProduct();
    }

    /**
     * 获取JetBrains付费插件列表
     */
    @GetMapping("/plugins")
    public List<PluginCache> getPlugins() {
        log.debug("获取插件列表，插件数量: {}", PluginConfig.pluginCache.getPlugin().size());
        return PluginConfig.pluginCache.getPlugin();
    }

    /**
     * 获取插件列表最后更新时间
     */
    @GetMapping("/plugins/lastUpdateTime")
    public List<UpdateTimeCache> getPluginLastUpdateTime() {
        return PluginConfig.pluginCache.getUpdateTime();
    }

    @GetMapping("/products/lastUpdateTime")
    public List<UpdateTimeCache> getProductLastUpdateTime() {
        return ProductConfig.productCache.getUpdateTime();
    }


    @GetMapping("/common/version")
    public String getCommonVersion() {
        return CommonContextHolder.version;
    }

    @GetMapping("/license/history")
    public Map<String, List<GenerateLicenseBody>> getLicenseHistory() {
        return LicenseConfig.licenseCache;
    }

    @GetMapping("/license/history/configKey")
    public List<GenerateLicenseBody> getLicenseHistoryConfigKey(@RequestParam String configKey) {
        return LicenseConfig.getLicenseCache(configKey);
    }

    @GetMapping("/license/history/del")
    public void delLicenseHistory(@RequestParam String configKey,@RequestParam String delKey) {
        LicenseConfig.delLicenseCache(configKey,delKey);
    }
}
