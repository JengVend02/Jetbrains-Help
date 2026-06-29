package com.zyp.help.context;

import com.zyp.help.context.plugin.PluginConfig;
import com.zyp.help.context.plugin.model.PluginCache;
import com.zyp.help.context.common.UpdateTimeCache;
import com.zyp.help.context.plugin.service.PluginApiService;
import com.zyp.help.context.plugin.service.PluginCacheService;
import com.zyp.help.context.plugin.service.PluginProcessService;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * JetBrains插件信息上下文管理器
 *
 * <p>此类负责管理JetBrains插件市场中付费插件的信息，包括插件ID、名称、产品代码、定价模式等。
 * 插件信息通过JetBrains官方API实时获取，并缓存到本地JSON文件中，用于许可证生成时的插件选择。
 *
 * <p>主要功能：
 * <ul>
 *   <li>从JetBrains插件市场API获取插件信息</li>
 *   <li>过滤和缓存付费插件信息</li>
 *   <li>定期刷新插件信息以保持最新状态</li>
 *   <li>为许可证生成提供插件产品代码</li>
 * </ul>
 *
 * <p>架构优化：
 * <ul>
 *   <li>配置管理 - {@link PluginConfig}: 统一管理所有配置参数</li>
 *   <li>网络服务 - {@link PluginApiService}: 处理所有API请求</li>
 *   <li>缓存服务 - {@link PluginCacheService}: 管理本地文件缓存</li>
 *   <li>业务处理 - {@link PluginProcessService}: 处理数据转换和过滤</li>
 * </ul>
 *
 * @author zyp
 * @version 2.0.0
 * @since 1.0.0
 */
@Slf4j(topic = "插件上下文")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PluginsContextHolder {

    // ==================== 核心方法 ====================

    /**
     * 初始化插件上下文
     *
     * <p>此方法负责初始化插件信息管理系统，包括加载本地缓存和启动刷新任务。
     * 如果本地缓存文件不存在或格式错误，会自动从网络获取最新数据。
     *
     * @throws IllegalArgumentException 当文件读取失败时抛出
     */
    public static void init() {
        log.info("开始初始化插件上下文...");

        try {
            // 从缓存加载插件数据,插件更新时间数据
            PluginConfig.pluginCache = PluginCacheService.loadFromPluginCache();
            log.info("插件上下文初始化成功！加载插件数量: {}", PluginConfig.pluginCache.getPlugin().size());

            // 启动异步刷新任务获取最新数据
            // refreshJsonFile();

        } catch (Exception e) {
            log.error("插件上下文初始化失败", e);
            throw e;
        }
    }


    /**
     * 刷新插件信息文件
     *
     * <p>使用多线程分页方式从JetBrains插件市场获取所有插件信息。
     * 该方法会根据配置文件中的参数来控制并发数量和分页大小。
     */
    public static void refreshJsonFile() {
        PluginConfig config = PluginConfig.getInstance();

        // 检查是否启用刷新功能
        if (!config.isRefreshEnabled()) {
            log.info("插件刷新功能已禁用，跳过刷新任务");
            return;
        }

        log.info("开始多线程分页刷新插件信息...");
        log.info("刷新配置 -> 分页大小: {}, 并发线程数: {}, 超时时间: {}ms",
                config.getPageSize(), config.getThreadCount(), config.getTimeout());

        // 初始化线程池
        initExecutorService(config.getThreadCount());

        // 启动异步刷新任务
        CompletableFuture
                // 1. 从API获取所有插件
            .supplyAsync(PluginApiService::fetchAllPlugins, PluginConfig.executorService)
                // 2. 过滤并更新插件(评分)列表
            .thenApply(PluginProcessService::filterPlugins)
                // 3. 转换为缓存对象
            .thenApply(PluginProcessService::convertToCache)
                // 4. 保存到缓存
            .thenAccept(PluginsContextHolder::saveNewPlugins)
            .thenRun(() -> log.info("多线程刷新成功!"))
            .exceptionally(throwable -> {
                log.error("多线程刷新失败!", throwable);
                return null;
            });
    }

    /**
     * 保存新插件到缓存
     *
     * @param newPlugins 新获取的插件列表
     */
    private static void saveNewPlugins(List<PluginCache> newPlugins) {
        if (newPlugins == null || newPlugins.isEmpty()) {
            log.info("没有新的插件需要保存");
            return;
        }

        Integer oldNum = PluginConfig.pluginCache.getPlugin().size();
        Integer addNum = newPlugins.size();
        log.info("源大小 => [{}], 新增大小 => [{}]", oldNum, addNum);

        // 新增插件合并到内存缓存
        PluginConfig.pluginCache.addPluginCache(newPlugins);

        // 创建更新时间缓存
        UpdateTimeCache updateTimeCache = new UpdateTimeCache(oldNum, addNum);
        // 更新时间合并到内存缓存
        PluginConfig.pluginCache.addUpdateTime(updateTimeCache);

        // 保存到文件
        PluginCacheService.saveToCache(PluginConfig.pluginCache);

        log.info("插件缓存已更新，当前总数: {}, 更新时间: {}", updateTimeCache.getNewNum(),updateTimeCache.getUpdateTime());
    }

    /**
     * 初始化线程池
     *
     * @param threadCount 线程数量
     */
    private static void initExecutorService(int threadCount) {
        if (PluginConfig.executorService == null || PluginConfig.executorService.isShutdown()) {
            PluginConfig.executorService = Executors.newFixedThreadPool(threadCount, r -> {
                Thread thread = new Thread(r, "PluginRefresh-");
                thread.setDaemon(true);
                return thread;
            });
            log.debug("线程池已创建，线程数: {}", threadCount);
        }
    }

    /**
     * 清理资源
     */
    public static void shutdown() {
        if (PluginConfig.executorService != null && !PluginConfig.executorService.isShutdown()) {
            log.info("正在关闭插件刷新线程池...");
            PluginConfig.executorService.shutdown();
            try {
                if (!PluginConfig.executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    PluginConfig.executorService.shutdownNow();
                }
                log.info("插件刷新线程池已关闭");
            } catch (InterruptedException e) {
                PluginConfig.executorService.shutdownNow();
                Thread.currentThread().interrupt();
                log.warn("线程池关闭被中断");
            }
        }
    }
}