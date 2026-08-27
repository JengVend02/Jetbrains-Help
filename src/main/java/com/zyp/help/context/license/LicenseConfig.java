package com.zyp.help.context.license;

import cn.hutool.extra.spring.SpringUtil;
import com.zyp.help.context.license.model.GenerateLicenseBody;
import com.zyp.help.context.product.service.LicenseCacheService;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Slf4j(topic = "授权配置")
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LicenseConfig {
    // ==================== 常量定义 ====================

    /** 授权历史信息配置文件路径 */
    public static final String LICENSE_JSON_FILE_NAME = "external/data/licenseHistory.json";


    // ==================== 静态字段 ====================

    /** 授权历史信息缓存列表，存储所有授权历史记录 */
    public static LinkedHashMap<String, List<GenerateLicenseBody>> licenseCache;

    public static void addLicenseCache(GenerateLicenseBody body) {
        String key = body.getConfigKey();

        if (licenseCache.containsKey(key)) {
            licenseCache.get(key).add(body);
        } else {
            List<GenerateLicenseBody> list = new ArrayList<>();
            list.add(body);
            licenseCache.put(key, list);
        }
    }

    public static void delLicenseCache(String key,String delKey) {
        if (licenseCache.containsKey(key)) {
            if ("all".equals(delKey)) {
                licenseCache.get(key).clear();
            } else{
                licenseCache.get(key).removeIf(body -> delKey.equals(body.getGenerationTime()));
            }
            // 保存到文件
            LicenseCacheService.saveToCache(licenseCache);
        }
    }

    public static List<GenerateLicenseBody> getLicenseCache(String key) {
        if (licenseCache.containsKey(key)) {
            return licenseCache.get(key);
        }
        return new ArrayList<>();
    }


    // ==================== 单例实现 ====================
    private static volatile LicenseConfig instance;

    public static LicenseConfig getInstance() {
        if (instance == null) {
            synchronized (LicenseConfig.class) {
                if (instance == null) {
                    instance = new LicenseConfig();
                    // instance.loadConfig();
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

            // this.refreshEnabled = environment.getProperty("help.plugins.refresh-enabled", Boolean.class, true);

            // log.debug("插件配置加载完成 -> 刷新启用: {}",refreshEnabled);

        } catch (Exception e) {
            log.warn("加载插件配置失败，使用默认值", e);
            setDefaultValues();
        }
    }

    /**
     * 设置默认配置值
     */
    private void setDefaultValues() {
        // this.refreshEnabled = true;
    }
}
