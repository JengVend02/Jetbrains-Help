package com.zyp.help.context.product.service;

import cn.hutool.core.io.IoUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.zyp.help.context.product.ProductConfig;
import com.zyp.help.context.product.model.ProductInfo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 产品API服务类
 *
 * <p>负责与JetBrains产品市场API的所有网络交互，包括：
 * <ul>
 *   <li>获取产品列表</li>
 *   <li>并发请求管理</li>
 * </ul>
 *
 * @author zyp
 * @version 1.0.0
 */
@Slf4j(topic = "产品API")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductApiService {

    private static final ProductConfig config = ProductConfig.getInstance();

    /**
     * 获取所有产品信息
     *
     * @return 包含所有产品的对象
     * @throws RuntimeException 当无法获取产品数据时
     */
    public static List<ProductInfo> fetchAllProducts() {
        log.info("开始多线程获取产品列表");

        // 首先获取第一页，确定总数
        List<ProductInfo> products = fetchProduct();
        if (products == null || products.isEmpty()) {
            throw new RuntimeException("无法获取产品");
        }
        return products;
    }

    /**
     * 获取产品信息
     * @return 产品列表页面数据
     */
    public static List<ProductInfo> fetchProduct() {
        String url = String.format(ProductConfig.PRODUCT_LIST_URL_TEMPLATE);
        log.debug("请求产品页面: url={}", url);

        try {
            return HttpUtil.createGet(url)
                .timeout(config.getTimeout())
                .thenFunction(response -> {
                    try (InputStream is = response.bodyStream()) {
                        if (!response.isOk()) {
                            throw new IllegalArgumentException(
                                String.format("请求失败! URL: %s, Response: %s", url, response));
                        }

                        List<ProductInfo> products = JSONUtil.toList(IoUtil.readUtf8(is), ProductInfo.class);
                        log.debug("成功获取页面 获取产品数: {}",products != null ? products.size() : 0);
                        return products;

                    } catch (IOException e) {
                        throw new IllegalArgumentException(
                            String.format("请求IO读取失败! URL: %s", url), e);
                    }
                });
        } catch (Exception e) {
            log.error("获取产品页面失败: ", e);
            return null;
        }
    }
}