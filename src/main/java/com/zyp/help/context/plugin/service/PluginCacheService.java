package com.zyp.help.context.plugin.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.json.JSONUtil;
import com.zyp.help.context.plugin.PluginConfig;
import com.zyp.help.context.plugin.model.Plugin;
import com.zyp.help.util.FileTools;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;

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
@Slf4j(topic = "插件缓存")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class PluginCacheService {

    private static File cacheFile;
    private static File cacheUpdateTimeFile;

    /**
     * 初始化缓存服务
     *
     * @return 缓存文件对象
     */
    public static void initCacheFile() {
        if (cacheFile == null) {
            cacheFile = FileTools.getFileOrCreat(PluginConfig.PLUGIN_JSON_FILE_NAME);
            log.debug("插件缓存文件路径: {}", cacheFile.getAbsolutePath());
        }
    }

    /**
     * 从缓存文件加载插件数据
     *
     * @return 插件缓存列表
     * @throws IllegalArgumentException 当文件读取失败时
     */
    public static Plugin loadFromPluginCache() {
        initCacheFile();
        try {
            String jsonContent = IoUtil.readUtf8(FileUtil.getInputStream(cacheFile));

            if (CharSequenceUtil.isBlank(jsonContent) || !JSONUtil.isTypeJSON(jsonContent)) {
                log.warn("插件缓存文件为空或格式错误，返回空列表");
                return new Plugin();
            }

            Plugin cache = JSONUtil.toBean(jsonContent, Plugin.class);
            log.info("从缓存加载插件数据成功，插件数量: {}", cache.getPlugin().size());
            return cache;

        } catch (IORuntimeException e) {
            throw new IllegalArgumentException(
                CharSequenceUtil.format("{} 文件读取失败!", PluginConfig.PLUGIN_JSON_FILE_NAME), e);
        }
    }

    /**
     * 保存插件数据到缓存文件
     *
     * @param pluginCache 要保存的插件数据列表
     * @throws IllegalArgumentException 当文件写入失败时
     */
    public static void saveToCache(Plugin pluginCache) {
        initCacheFile();

        try {
            String jsonStr = JSONUtil.toJsonStr(pluginCache);
            String formattedJson = JSONUtil.formatJsonStr(jsonStr);

            FileUtil.writeString(formattedJson, cacheFile, StandardCharsets.UTF_8);

        } catch (IORuntimeException e) {
            throw new IllegalArgumentException(
                CharSequenceUtil.format("{} 文件写入失败!", PluginConfig.PLUGIN_JSON_FILE_NAME), e);
        }
    }
}