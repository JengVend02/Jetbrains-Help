package com.zyp.help.context;

import com.zyp.help.context.common.UpdateTimeCache;
import com.zyp.help.context.plugin.service.PluginCacheService;
import com.zyp.help.context.product.ProductConfig;
import com.zyp.help.context.product.model.ProductCache;
import com.zyp.help.context.product.service.ProductProcessService;
import com.zyp.help.context.product.service.ProductApiService;
import com.zyp.help.context.product.service.ProductCacheService;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;


/**
 * JetBrains产品信息上下文管理器
 *
 * <p>此类负责管理JetBrains所有IDE产品的信息，包括产品名称、产品代码和图标等。
 * 产品信息从JSON配置文件中加载，用于许可证生成时的产品选择和识别。
 *
 * <p>主要功能：
 * <ul>
 *   <li>从JSON文件加载JetBrains产品信息</li>
 *   <li>提供产品信息的缓存和访问接口</li>
 *   <li>支持产品代码到产品名称的映射</li>
 *   <li>为许可证生成提供产品代码列表</li>
 * </ul>
 *
 * <p>支持的产品包括：
 * <ul>
 *   <li>IntelliJ IDEA Ultimate (II)</li>
 *   <li>PhpStorm (PS)</li>
 *   <li>WebStorm (WS)</li>
 *   <li>PyCharm Professional (PY)</li>
 *   <li>RubyMine (RM)</li>
 *   <li>CLion (CL)</li>
 *   <li>DataGrip (DB)</li>
 *   <li>GoLand (GO)</li>
 *   <li>Rider (RD)</li>
 *   <li>AppCode (AC)</li>
 *   <li>以及其他JetBrains系列产品</li>
 * </ul>
 *
 * <p>数据来源：
 * 产品信息可以通过JetBrains官方API获取：
 * {@code https://data.services.jetbrains.com/products?fields=name,salesCode}
 *
 * @author zyp
 * @version 1.0.0
 * @since 1.0.0
 */

@Slf4j(topic = "产品上下文")
@NoArgsConstructor(access = AccessLevel.PRIVATE)  // 防止实例化
public class ProductsContextHolder {
    public static void init() {
        log.info("开始初始化产品上下文...");

        try {
            // 从缓存加载产品数据
            ProductConfig.productCache = ProductCacheService.loadFromProductCache();
            log.info("产品上下文初始化成功！加载产品数量: {}", ProductConfig.productCache.getProduct().size());

            // 启动异步刷新任务获取最新数据
            // refreshJsonFile();

        } catch (Exception e) {
            log.error("产品上下文初始化失败", e);
            throw e;
        }
    }

    /**
     * 刷新产品信息文件
     */
    public static void refreshJsonFile() {
        ProductConfig config = ProductConfig.getInstance();

        // 检查是否启用刷新功能
        if (!config.isRefreshEnabled()) {
            log.info("产品刷新功能已禁用，跳过刷新任务");
            return;
        }

        log.info("开始刷新产品信息...");
        log.info("刷新配置 -> 超时时间: {}ms", config.getTimeout());

        // 1. 从API获取所有产品
        // 2. 过滤产品（排除已存在）
        // 3. 转换为缓存对象
        // 4. 保存到缓存
        CompletableFuture.supplyAsync(ProductApiService::fetchAllProducts)
                // 2. 过滤产品列表
                .thenApply(ProductProcessService::filterProducts)
                // 3. 转换为缓存对象
                .thenApply(ProductProcessService::convertToCache)
                // 4. 保存到缓存
                .thenAccept(ProductsContextHolder::saveNewProducts)
                .thenRun(() -> log.info("产品刷新成功!"))
                .exceptionally(throwable -> {
                    log.error("产品刷新失败!", throwable);
                    return null;
                });
    }

    /**
     * 保存新产品到缓存
     * @param newProducts 新获取的产品列表
     */
    private static void saveNewProducts(List<ProductCache> newProducts) {
        if (newProducts == null || newProducts.isEmpty()) {
            log.info("没有新的产品需要保存");
            return;
        }

        Integer oldNum = ProductConfig.productCache.getProduct().size();
        Integer addNum = newProducts.size();
        log.info("源大小 => [{}], 新增大小 => [{}]", oldNum, addNum);

        // 合并到内存缓存
        ProductConfig.productCache.setProduct(ProductCacheService.mergeCache(ProductConfig.productCache.getProduct(), newProducts));

        // 创建更新时间缓存
        UpdateTimeCache updateTimeCache = new UpdateTimeCache(oldNum, addNum);
        // 合并到更新时间缓存
        ProductConfig.productCache.addUpdateTime(updateTimeCache);

        // 保存到文件
        ProductCacheService.saveToCache(ProductConfig.productCache);

        log.info("产品缓存已更新，当前总数: {}, 更新时间: {}", updateTimeCache.getNewNum(), updateTimeCache.getUpdateTime());
    }
}