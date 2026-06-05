package com.zyp.help.context.product.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.json.JSONUtil;
import com.zyp.help.context.product.ProductConfig;
import com.zyp.help.context.product.model.ProductCache;
import com.zyp.help.util.FileTools;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 产品缓存服务类
 *
 * <p>负责产品数据的本地缓存管理，包括：
 * <ul>
 *   <li>从本地文件加载缓存数据</li>
 *   <li>保存数据到本地文件</li>
 *   <li>缓存数据的合并和更新</li>
 * </ul>
 *
 * @author zyp
 * @version 1.0.0
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class ProductCacheService {

    private static File cacheFile;

    /**
     * 初始化缓存服务
     *
     * @return 缓存文件对象
     */
    public static void initCacheFile() {
        if (cacheFile == null) {
            cacheFile = FileTools.getFileOrCreat(ProductConfig.PRODUCT_JSON_FILE_NAME);
            log.debug("产品缓存文件路径: {}", cacheFile.getAbsolutePath());
        }
    }

    /**
     * 从缓存文件加载产品数据
     *
     * @return 产品缓存列表
     * @throws IllegalArgumentException 当文件读取失败时
     */
    public static List<ProductCache> loadFromProductCache() {
        initCacheFile();
        try {
            String jsonContent = IoUtil.readUtf8(FileUtil.getInputStream(cacheFile));

            if (CharSequenceUtil.isBlank(jsonContent) || !JSONUtil.isTypeJSON(jsonContent)) {
                log.warn("产品缓存文件为空或格式错误，返回空列表");
                return new ArrayList<>();
            }

            List<ProductCache> cacheList = JSONUtil.toList(jsonContent, ProductCache.class);
            log.info("从缓存加载产品数据成功，产品数量: {}", cacheList.size());
            return cacheList;

        } catch (IORuntimeException e) {
            throw new IllegalArgumentException(
                CharSequenceUtil.format("{} 文件读取失败!", ProductConfig.PRODUCT_JSON_FILE_NAME), e);
        }
    }


    /**
     * 保存产品数据到缓存文件
     *
     * @param productCaches 要保存的产品数据列表
     * @throws IllegalArgumentException 当文件写入失败时
     */
    public static void saveToCache(List<ProductCache> productCaches) {
        initCacheFile();

        try {
            String jsonStr = JSONUtil.toJsonStr(productCaches);
            String formattedJson = JSONUtil.formatJsonStr(jsonStr);

            FileUtil.writeString(formattedJson, cacheFile, StandardCharsets.UTF_8);
            log.info("产品数据保存到缓存成功，产品数量: {}", productCaches.size());

        } catch (IORuntimeException e) {
            throw new IllegalArgumentException(
                CharSequenceUtil.format("{} 文件写入失败!", ProductConfig.PRODUCT_JSON_FILE_NAME), e);
        }
    }

    /**
     * 合并新数据到现有缓存
     *
     * @param existingCache 现有缓存数据
     * @param newData 新的产品数据
     * @return 合并后的数据列表
     */
    public static <T> List<T> mergeCache(List<T> existingCache, List<T> newData) {
        if (existingCache == null) {
            existingCache = new ArrayList<>();
        }

        log.info("合并缓存数据 -> 原有数量: {}, 新增数量: {}", existingCache.size(), newData.size());

        existingCache.addAll(newData);
        return existingCache;
    }
}