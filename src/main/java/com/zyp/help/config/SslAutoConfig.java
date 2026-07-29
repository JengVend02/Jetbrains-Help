package com.zyp.help.config;

import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.File;

@Slf4j(topic = "ssl证书管理器")
@Configuration
public class SslAutoConfig implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

    // 1. 指定你的 .p12 证书文件绝对路径（根据需要修改）
    private String certPath;

    // 2. 证书密码
    private String certPassword;

    // 3. 证书别名（如 cloudflare 或 myapp）
    private String certAlias;


    /**
     * 从Spring环境中加载配置
     */
    private void loadConfig() {
        try {
            Environment environment = SpringUtil.getBean(Environment.class);

            this.certPath = environment.getProperty("help.ssl.path", String.class, "");
            this.certPassword = environment.getProperty("help.ssl.password", String.class, "");
            this.certAlias = environment.getProperty("help.ssl.alias", String.class, "");
        } catch (Exception e) {
            log.warn("加载插件配置失败，使用默认值", e);
        }
    }
    

    @Override
    public void customize(ConfigurableServletWebServerFactory factory) {
        loadConfig();
        File certFile = new File(certPath);

        log.info("========================================");
        // 检查文件是否存在且是一个合法文件
        if (certFile.exists() && certFile.isFile()) {
            log.info("检测到 SSL 证书: {}，已自动开启 HTTPS 模式！", certPath);

            Ssl ssl = new Ssl();
            ssl.setEnabled(true);
            ssl.setKeyStoreType("PKCS12");
            ssl.setKeyStore("file:" + certPath); // 使用 file: 前缀表示外部绝对路径
            ssl.setKeyStorePassword(certPassword);
            ssl.setKeyAlias(certAlias);

            // 将 SSL 配置注入 Web 服务容器 (Tomcat/Jetty/Undertow)
            factory.setSsl(ssl);
        } else {
            log.info("未检测到 SSL 证书 ({})，回退到默认 HTTP 模式运行。", certPath);
        }
        log.info("========================================");
    }
}
