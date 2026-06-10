package com.zyp.help.context.plugin.service;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.zyp.help.context.plugin.PluginConfig;
import com.zyp.help.context.plugin.model.PluginInfo;
import com.zyp.help.context.plugin.model.PluginList;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 插件API服务类
 *
 * <p>负责与JetBrains插件市场API的所有网络交互，包括：
 * <ul>
 *   <li>获取插件列表（支持分页）</li>
 *   <li>获取插件详细信息</li>
 *   <li>并发请求管理</li>
 * </ul>
 *
 * @author zyp
 * @version 1.0.0
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PluginApiService {

    private static final PluginConfig config = PluginConfig.getInstance();

    /**
     * 使用多线程分页获取所有插件信息
     *
     * @return 包含所有插件的PluginList对象
     * @throws RuntimeException 当无法获取插件数据时
     */
    public static PluginList fetchAllPlugins() {
        log.info("开始多线程获取插件列表，分页大小: {}, 线程数: {}",
                config.getPageSize(), config.getThreadCount());

        // 首先获取第一页，确定总数
        PluginList firstPage = fetchPluginPage(0, config.getPageSize());
        if (firstPage == null || firstPage.getTotal() == null) {
            throw new RuntimeException("无法获取插件总数");
        }

        long totalPlugins = firstPage.getTotal();
        int totalPages = (int) ((totalPlugins + config.getPageSize() - 1) / config.getPageSize());

        log.info("插件总数: {}, 预计需要 {} 页", totalPlugins, totalPages);

        // 创建结果收集器
        List<PluginList.Plugin> allPlugins = new ArrayList<>(firstPage.getPlugins());
        List<CompletableFuture<PluginList>> futures = new ArrayList<>();

        // 创建并发任务获取剩余页面
        for (int page = 1; page < totalPages; page++) {
            final int offset = page * config.getPageSize();

            CompletableFuture<PluginList> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return fetchPluginPage(offset, config.getPageSize());
                } catch (Exception e) {
                    log.error("获取插件页面失败 (offset: {})", offset, e);
                    return null;
                }
            }, PluginConfig.executorService);

            futures.add(future);
        }

        // 等待所有页面获取完成并收集结果
        collectResults(futures, allPlugins, totalPages);

        // 返回合并结果
        PluginList result = new PluginList();
        result.setPlugins(allPlugins);
        result.setTotal((long) allPlugins.size());

        return result;
    }

    /**
     * 获取指定页面的插件信息
     *
     * @param offset 偏移量
     * @param pageSize 页面大小
     * @return 插件列表页面数据
     */
    public static PluginList fetchPluginPage(int offset, int pageSize) {
        String url = String.format(PluginConfig.PLUGIN_LIST_URL_TEMPLATE, pageSize, offset);
        log.debug("请求插件页面: offset={}, pageSize={}, url={}", offset, pageSize, url);

        try {
            return HttpUtil.createGet(url)
                .timeout(config.getTimeout())
                .thenFunction(response -> {
                    try (InputStream is = response.bodyStream()) {
                        if (!response.isOk()) {
                            throw new IllegalArgumentException(
                                String.format("请求失败! URL: %s, Response: %s", url, response));
                        }

                        PluginList pluginList = JSONUtil.toBean(IoUtil.readUtf8(is), PluginList.class);
                        log.debug("成功获取页面 offset={}, 获取插件数: {}", offset,
                            pluginList.getPlugins() != null ? pluginList.getPlugins().size() : 0);
                        return pluginList;

                    } catch (IOException e) {
                        throw new IllegalArgumentException(
                            String.format("请求IO读取失败! URL: %s", url), e);
                    }
                });
        } catch (Exception e) {
            log.error("获取插件页面失败: offset={}, pageSize={}", offset, pageSize, e);
            return null;
        }
    }

    /**
     * 获取插件详细信息
     *
     * @param plugin 插件基本信息
     * @return 插件详细信息
     * @throws IllegalArgumentException 当请求失败时
     */
    public static PluginInfo fetchPluginInfo(PluginList.Plugin plugin) {
        String url = PluginConfig.PLUGIN_INFO_URL + plugin.getId();

        return HttpUtil.createGet(url)
            .timeout(config.getTimeout())
            .thenFunction(response -> {
                try (InputStream is = response.bodyStream()) {
                    if (!response.isOk()) {
                        throw new IllegalArgumentException(
                            CharSequenceUtil.format("{} 请求失败! = {}", url, response));
                    }

                    PluginInfo pluginInfo = JSONUtil.toBean(IoUtil.readUtf8(is), PluginInfo.class);
                    log.debug("已抓取 => ID = [{}], 名称 = [{}], Code = [{}]",
                            pluginInfo.getId(), plugin.getName(),
                            pluginInfo.getPurchaseInfo().getProductCode());
                    return pluginInfo;

                } catch (IOException e) {
                    throw new IllegalArgumentException(
                        CharSequenceUtil.format("{} 请求IO读取失败!", url), e);
                }
            });
    }

    /**
     * 收集并发请求的结果
     *
     * @param futures 异步任务列表
     * @param allPlugins 结果收集器
     * @param totalPages 总页数
     */
    private static void collectResults(List<CompletableFuture<PluginList>> futures,
                                     List<PluginList.Plugin> allPlugins,
                                     int totalPages) {
        try {
            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );

            // 添加超时控制
            allOf.get(config.getTimeout() * totalPages / 1000, TimeUnit.SECONDS);

            // 收集所有结果
            AtomicInteger successCount = new AtomicInteger(1); // 包含第一页
            for (CompletableFuture<PluginList> future : futures) {
                PluginList pageResult = future.get();
                if (pageResult != null && pageResult.getPlugins() != null) {
                    allPlugins.addAll(pageResult.getPlugins());
                    successCount.incrementAndGet();
                } else {
                    log.warn("某一页插件获取失败，跳过该页");
                }
            }

            log.info("多线程获取完成，成功获取 {} 页，总插件数: {}",
                    successCount.get(), allPlugins.size());

        } catch (TimeoutException e) {
            log.error("获取插件超时，已获取部分结果，插件数: {}", allPlugins.size());
        } catch (Exception e) {
            log.error("获取插件过程中发生异常", e);
        }
    }
}