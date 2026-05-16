package com.zyp.help.context;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring 上下文工具类
 * 用于在任意代码处获取 Spring 配置和 Bean
 */
@Component
public class SpringContextHolder implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) {
        applicationContext = context;
    }

    /**
     * 获取配置属性
     *
     * @param key 配置键（支持 application.yml 中的配置和环境变量）
     * @return 配置值，不存在返回 null
     */
    public static String getProperty(String key) {
        return applicationContext != null ? applicationContext.getEnvironment().getProperty(key) : null;
    }

    /**
     * 获取配置属性，带默认值
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值，不存在返回默认值
     */
    public static String getProperty(String key, String defaultValue) {
        return applicationContext != null ? applicationContext.getEnvironment().getProperty(key, defaultValue) : defaultValue;
    }
}
