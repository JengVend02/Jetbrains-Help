package com.zyp.help;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.zyp.help.context.AgentContextHolder;
import com.zyp.help.context.CertificateContextHolder;
import com.zyp.help.context.PluginsContextHolder;
import com.zyp.help.context.ProductsContextHolder;
import com.zyp.help.context.plugin.PluginConfig;
import java.net.InetAddress;

import com.zyp.help.context.product.ProductConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.PreDestroy;

/**
 * JetBrains Help 应用程序主入口类
 * 
 * <p>这是一个Spring Boot应用程序，用于提供JetBrains产品的许可证激活服务。
 * 该应用程序主要功能包括：
 * <ul>
 *   <li>JetBrains产品许可证生成和验证</li>
 *   <li>许可证服务器模拟（兼容JetBrains官方许可证服务器协议）</li>
 *   <li>产品和插件信息管理</li>
 *   <li>证书和密钥管理</li>
 *   <li>ja-netfilter代理工具配置</li>
 * </ul>
 * 
 * <p>应用启动后将在指定端口（默认10768）提供HTTP服务，用于处理许可证相关请求。
 * 
 * @author zyp
 * @version 1.0.0
 * @since 1.0.0
 */

@Slf4j(topic = "项目入口")
@EnableScheduling  // 启用Spring任务调度功能
@Import(SpringUtil.class)  // 导入Hutool的Spring工具类
@SpringBootApplication
public class JetbrainsHelpApplication {

    /**
     * 应用程序主入口方法
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 启动Spring Boot应用程序
        SpringApplication.run(JetbrainsHelpApplication.class, args);
    }

    /**
     * 应用程序准备就绪事件监听器
     * 
     * <p>当Spring Boot应用程序完全启动并准备好接收请求时触发此方法。
     * 该方法负责初始化所有必要的上下文组件和输出启动成功信息。
     * 
     * <p>初始化顺序说明：
     * <ol>
     *   <li>产品上下文 - 初始化JetBrains产品信息</li>
     *   <li>插件上下文 - 初始化JetBrains插件信息</li>
     *   <li>证书上下文 - 初始化RSA密钥对和X.509证书</li>
     *   <li>代理上下文 - 初始化ja-netfilter代理配置</li>
     * </ol>
     * 
     * @throws Exception 当初始化过程中发生错误时抛出异常
     */
    @SneakyThrows
    @EventListener(ApplicationReadyEvent.class)
    public void ready() {
        log.info("开始初始化应用程序组件...");
        
        // 按顺序初始化各个上下文组件
        ProductsContextHolder.init();     // 初始化产品信息上下文
        PluginsContextHolder.init();      // 初始化插件信息上下文
        CertificateContextHolder.init();  // 初始化证书和密钥上下文
        AgentContextHolder.init();        // 初始化ja-netfilter代理上下文
        
        // 获取本地IP地址和端口号，构建访问URL
        InetAddress localHost = InetAddress.getLocalHost();
        String serverPort = SpringUtil.getProperty("server.port");
        String accessUrl = CharSequenceUtil.format("http://{}:{}", localHost.getHostAddress(), serverPort);
        
        // 输出应用启动成功信息
        String startupSuccessMessage = buildStartupSuccessMessage(accessUrl);
        log.info(startupSuccessMessage);
    }

    /**
     * 定时刷新插件信息任务
     *
     * <p>每天中午12点执行一次，从JetBrains官网获取最新的插件信息。
     * 此任务采用异步执行方式，不会阻塞应用程序的正常运行。
     * 支持通过配置文件开关控制是否启用该定时任务。
     */
    @Scheduled(cron = "0 0 12 * * ?")
    public void refreshPlugin() {
        // 检查是否启用定时刷新功能
        PluginConfig config = PluginConfig.getInstance();
        if (!config.isRefreshEnabled()) {
            log.info("插件定时刷新功能已禁用，跳过本次刷新任务");
            return;
        }

        log.info("开始执行定时刷新插件信息任务...");
        // 异步执行插件信息刷新任务，避免阻塞主线程
        ThreadUtil.execute(PluginsContextHolder::refreshJsonFile);
    }

    /**
     * 定时刷新产品信息任务
     *
     * <p>每月1号中午12点执行一次，从JetBrains官网获取最新的产品信息。
     * 支持通过配置文件开关控制是否启用该定时任务。
     */
    @Scheduled(cron = "0 0 12 1 * ?")
    public void refreshProduct() {
        // 检查是否启用定时刷新功能
        ProductConfig config = ProductConfig.getInstance();
        if (!config.isRefreshEnabled()) {
            log.info("产品定时刷新功能已禁用，跳过本次刷新任务");
            return;
        }

        log.info("开始执行定时刷新产品信息任务...");
        // 异步执行插件信息刷新任务，避免阻塞主线程
        ThreadUtil.execute(ProductsContextHolder::refreshJsonFile);
    }

    /**
     * 应用关闭时的清理工作
     */
    @PreDestroy
    public void onDestroy() {
        log.info("应用程序正在关闭，清理资源...");
        PluginsContextHolder.shutdown();
    }
    
    /**
     * 构建应用启动成功消息
     * 
     * @param accessUrl 应用访问地址
     * @return 格式化的启动成功消息
     */
    private String buildStartupSuccessMessage(String accessUrl) {
        return "\n" +
               "============================================================================================\n" +
               "=                           Jetbrains-Help 启动成功! 🎉                             =\n" +
               "=                                                                                          =\n" +
               String.format("=  访问地址: %-70s =\n", accessUrl) +
               "=                                                                                          =\n" +
               "=  功能说明:                                                                                =\n" +
               "=    • 生成JetBrains产品许可证                                                              =\n" +
               "=    • 模拟许可证服务器                                                                     =\n" +
               "=    • 下载ja-netfilter代理工具                                                            =\n" +
               "=                                                                                          =\n" +
               "============================================================================================\n";
    }

    /**
     * 跨域资源共享(CORS)配置
     * 
     * <p>为了支持前端页面调用后端 API，需要配置 CORS 策略。
     * 这个配置允许来自任何域名的请求访问 API。
     * 
     * @return WebMvc配置器，用于设置跨域访问规则
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                    .allowedOrigins("*")  // 允许所有来源
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  // 允许的HTTP方法
                    .allowedHeaders("*")  // 允许所有请求头
                    .maxAge(3600);  // 预检请求的有效期（1小时）
            }
        };
    }

}
