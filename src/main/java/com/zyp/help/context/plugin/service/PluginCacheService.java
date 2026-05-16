package com.zyp.help.context.plugin.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.json.JSONUtil;
import com.zyp.help.context.plugin.PluginConfig;
import com.zyp.help.context.plugin.model.PluginCache;
import com.zyp.help.util.FileTools;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 插件缓存服务类
 *
 * <p>负责插件数据的本地缓存管理，包括：
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
public class PluginCacheService {

    private static File cacheFile;

    /**
     * 初始化缓存服务
     *
     * @return 缓存文件对象
     */
    public static File initCacheFile() {
        if (cacheFile == null) {
            cacheFile = FileTools.getFileOrCreat(PluginConfig.PLUGIN_JSON_FILE_NAME);
            log.debug("插件缓存文件路径: {}", cacheFile.getAbsolutePath());
        }
        return cacheFile;
    }

    /**
     * 从缓存文件加载插件数据
     *
     * @return 插件缓存列表
     * @throws IllegalArgumentException 当文件读取失败时
     */
    public static List<PluginCache> loadFromCache() {
        File file = initCacheFile();

        try {
            String jsonContent = IoUtil.readUtf8(FileUtil.getInputStream(file));

            if (CharSequenceUtil.isBlank(jsonContent) || !JSONUtil.isTypeJSON(jsonContent)) {
                log.warn("插件缓存文件为空或格式错误，返回空列表");
                return new ArrayList<>();
            }

            List<PluginCache> cacheList = JSONUtil.toList(jsonContent, PluginCache.class);
            log.info("从缓存加载插件数据成功，插件数量: {}", cacheList.size());
            return cacheList;

        } catch (IORuntimeException e) {
            throw new IllegalArgumentException(
                CharSequenceUtil.format("{} 文件读取失败!", PluginConfig.PLUGIN_JSON_FILE_NAME), e);
        }
    }

    /**
     * 保存插件数据到缓存文件
     *
     * @param pluginCaches 要保存的插件数据列表
     * @throws IllegalArgumentException 当文件写入失败时
     */
    public static void saveToCache(List<PluginCache> pluginCaches) {
        File file = initCacheFile();

        try {
            String jsonStr = JSONUtil.toJsonStr(pluginCaches);
            String formattedJson = JSONUtil.formatJsonStr(jsonStr);

            FileUtil.writeString(formattedJson, file, StandardCharsets.UTF_8);
            log.info("插件数据保存到缓存成功，插件数量: {}", pluginCaches.size());

        } catch (IORuntimeException e) {
            throw new IllegalArgumentException(
                CharSequenceUtil.format("{} 文件写入失败!", PluginConfig.PLUGIN_JSON_FILE_NAME), e);
        }
    }

    /**
     * 合并新数据到现有缓存
     *
     * @param existingCache 现有缓存数据
     * @param newData 新的插件数据
     * @return 合并后的数据列表
     */
    public static List<PluginCache> mergeCache(List<PluginCache> existingCache, List<PluginCache> newData) {
        if (existingCache == null) {
            existingCache = new ArrayList<>();
        }

        log.info("合并缓存数据 -> 原有数量: {}, 新增数量: {}", existingCache.size(), newData.size());

        existingCache.addAll(newData);
        return existingCache;
    }

    /**
     * 获取缓存文件对象
     *
     * @return 缓存文件对象
     */
    public static File getCacheFile() {
        return initCacheFile();
    }
}