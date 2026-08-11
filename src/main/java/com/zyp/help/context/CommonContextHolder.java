package com.zyp.help.context;

import cn.hutool.extra.spring.SpringUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;


@Slf4j(topic = "通用上下文")
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)  // 防止实例化
public class CommonContextHolder {

    // ==================== 配置字段 ====================
    /** 可用版本 */
    public static String version;

    // ==================== 单例实现 ====================
    private static volatile CommonContextHolder instance;

    public static void init() {
        log.info("开始初始化通用信息...");
        CommonContextHolder.getInstance();
    }

    /**
     * 获取配置实例
     *
     * @return 配置实例
     */
    public static CommonContextHolder getInstance() {
        if (instance == null) {
            synchronized (CommonContextHolder.class) {
                if (instance == null) {
                    instance = new CommonContextHolder();
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

            version = environment.getProperty("help.version", String.class, "2026.2.1");

            log.debug("通用配置加载完成 -> 可用版本: {}", version);

        } catch (Exception e) {
            log.warn("加载通用配置失败，使用默认值", e);
            setDefaultValues();
        }
    }

    /**
     * 设置默认配置值
     */
    private void setDefaultValues() {
        version = "2026.2.1";
    }


}