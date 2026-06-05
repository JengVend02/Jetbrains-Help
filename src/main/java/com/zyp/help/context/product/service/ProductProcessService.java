package com.zyp.help.context.product.service;

import com.zyp.help.context.product.ProductConfig;
import com.zyp.help.context.product.model.ProductCache;
import com.zyp.help.context.product.model.ProductInfo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 产品处理服务类
 *
 * <p>负责产品数据的业务逻辑处理，包括：
 * <ul>
 *   <li>产品数据的过滤和转换</li>
 *   <li>去重和数据清洗</li>
 *   <li>业务规则应用</li>
 * </ul>
 *
 * @author zyp
 * @version 1.0.0
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductProcessService {

    /**
     * 过滤产品列表
     *
     * <p>过滤条件：
     * <ul>
     *   <li>排除已存在于缓存中的产品</li>
     * </ul>
     *
     * @param productList 原始产品列表
     * @return 过滤后的产品列表
     */
    public static List<ProductInfo> filterProducts(List<ProductInfo> productList) {
        if (productList == null || productList.isEmpty()) {
            log.warn("产品列表为空，返回空结果");
            return Collections.emptyList();
        }

        // 缓存列表：以逗号分割的字符串
        String existingCacheLinks = ","+ ProductConfig.productCacheList.stream()
        .map(productCache -> productCache.getLink().endsWith("/") ? productCache.getLink().substring(0, productCache.getLink().length() - 1) : productCache.getLink())
        .collect(Collectors.joining(","));

        Map<String,List<ProductInfo>> map = productList.stream()
        // 获取需要激活的产品
        .filter(ProductInfo::getForSale)
        // 去除已缓存的产品
        .filter(product -> !isExists(product, existingCacheLinks))
        // 按link分组,如果link以斜杠"/"结尾,则去除掉斜杠"/"后再分组
        .collect(Collectors.groupingBy(productInfo -> productInfo.getLink().endsWith("/") ? productInfo.getLink().substring(0, productInfo.getLink().length() - 1) : productInfo.getLink()));

        List<ProductInfo> filtered = new ArrayList<>();
        // 使用英文逗号拼接同短名的code
        for (Map.Entry<String, List<ProductInfo>> entry : map.entrySet()) {
            String code = entry.getValue().stream().map(ProductInfo::getCode).collect(Collectors.joining(","));
            String salesCode = entry.getValue().stream().map(ProductInfo::getSalesCode).collect(Collectors.joining(","));
            // 以换行符为分隔符
            String name = entry.getValue().stream().map(ProductInfo::getName).collect(Collectors.joining("<br>"));
            // 去除重复描述
            String description = entry.getValue().stream()
                    .map(ProductInfo::getDescription)
                    .collect(Collectors.collectingAndThen(
                            Collectors.toCollection(LinkedHashSet::new),
                            set -> String.join("<br>", set)
                    ));
            ProductInfo productInfo = new ProductInfo();
            productInfo.setLink(entry.getKey());
            productInfo.setCode(code);
            productInfo.setSalesCode(salesCode);
            productInfo.setName(name);
            productInfo.setDescription(description);
            filtered.add(productInfo);
        }

        log.info("产品过滤完成 -> 原始数量: {}, 过滤后数量: {}",productList.size(), filtered.size());
        return filtered;
    }

    /**
     * 将产品基本信息转换为缓存对象
     *
     * @param productList 产品基本信息列表
     * @return 产品缓存对象列表
     */
    public static List<ProductCache> convertToCache(List<ProductInfo> productList) {
        if (productList == null || productList.isEmpty()) {
            log.info("没有需要转换的产品数据");
            return Collections.emptyList();
        }

        List<ProductCache> cacheList = productList
            .parallelStream()
            .map(ProductProcessService::convertSingle)
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(ProductCache::getName))
            .collect(Collectors.toList());

        log.info("产品转换完成 -> 转换数量: {}", cacheList.size());
        return cacheList;
    }

    /**
     * 转换单个产品信息
     *
     * @param product 产品基本信息
     */
    private static ProductCache convertSingle(ProductInfo product) {
        try {
            return new ProductCache()
                    .setName(product.getName())
                    .setProductCode(product.getSalesCode())
                    .setDescription(product.getDescription())
                    .setLink(product.getLink())
                    .setIcon(setIcon(product.getLink()));
        } catch (Exception e) {
            log.error("转换产品信息失败: {} (CODE: {})", product.getName(),  product.getCode(), e);
            return null;
        }
    }

    /**
     * 检查产品是否已存在于缓存中
     *
     * @param plugin 产品基本信息
     * @param existingCache 现有缓存
     * @return 如果存在返回true，否则返回false
     */
    private static boolean isExists(ProductInfo plugin, String existingCache) {
        if (existingCache == null || existingCache.isEmpty()) {
            return false;
        }

        return existingCache.contains("," + (plugin.getLink().endsWith("/")?plugin.getLink().substring(0, plugin.getLink().length() - 1):plugin.getLink()));
    }

    /**
     * 检查是否为免费产品
     *
     * @param link 产品链接
     * @return 如果是免费产品返回true，否则返回false
     */
    private static String setIcon(String link) {
        return ProductConfig.PRODUCT_ICON_URL.replace("{product}", getProductShotName(link));
    }

    private static String getProductShotName(String link) {
        return link.replace("https://www.jetbrains.com/","").replace("/","");
    }
}