package com.zyp.help.context.license;

import cn.hutool.extra.spring.SpringUtil;
import com.zyp.help.controller.LicenseCodeController;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;

import java.util.*;

@Slf4j(topic = "授权配置")
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LicenseConfig {
    // ==================== 常量定义 ====================

    /** 授权历史信息配置文件路径 */
    public static final String LICENSE_JSON_FILE_NAME = "external/data/licenseHistory.json";


    // ==================== 静态字段 ====================

    /** 授权历史信息缓存列表，存储所有授权历史记录 */
    public static Map<String, List<Map<String,String>>> licenseHistory;

    public static void addLicenseHistoryCache(LicenseCodeController.GenerateLicenseRespBody body){
        String licensesName = body.getLicenseName();
        String assigneeName = body.getAssigneeName();
        String key = licensesName +"," + assigneeName;
        List<Map<String, String>> list = licenseHistory.get(key);
        if(list == null){
            list = new ArrayList<>();
        }
        Map<String, String> map = new LinkedHashMap<>();
        map.put("请求时间",body.getGenerationTime());
        map.put("许可证名称",licensesName);
        map.put("被授权人",assigneeName);
        map.put("过期日期",body.getExpiryDate());
        map.put("激活内容",body.getActivationProduct());
        list.add(map);
        licenseHistory.put(key, list);
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
