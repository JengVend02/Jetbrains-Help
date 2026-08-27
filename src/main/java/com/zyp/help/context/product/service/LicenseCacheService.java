package com.zyp.help.context.product.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.json.JSONUtil;
import com.zyp.help.context.license.LicenseConfig;
import com.zyp.help.util.FileTools;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j(topic = "授权缓存")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class LicenseCacheService {

    private static File cacheFile;

    /**
     * 初始化缓存服务
     *
     * @return 缓存文件对象
     */
    public static void initCacheFile() {
        if (cacheFile == null) {
            cacheFile = FileTools.getFileOrCreat(LicenseConfig.LICENSE_JSON_FILE_NAME);
            log.debug("授权历史缓存文件路径: {}", cacheFile.getAbsolutePath());
        }
    }

    /**
     * 从缓存文件加载授权历史数据
     *
     * @return 授权历史缓存列表
     * @throws IllegalArgumentException 当文件读取失败时
     */
    public static Map<String, List<Map<String,String>>> loadFromLicenseCache() {
        initCacheFile();
        try {
            String jsonContent = IoUtil.readUtf8(FileUtil.getInputStream(cacheFile));

            if (CharSequenceUtil.isBlank(jsonContent) || !JSONUtil.isTypeJSON(jsonContent)) {
                log.warn("授权缓存文件为空或格式错误，返回空列表");
                return new HashMap<>();
            }

            Map<String, List<Map<String,String>>> cache = JSONUtil.toBean(
                        jsonContent,
                        new TypeReference<LinkedHashMap<String, List<Map<String, String>>>>() {},
                        true
                );
            log.info("从缓存加载授权历史成功，授权人数: {}", cache.size());
            return cache;

        } catch (IORuntimeException e) {
            throw new IllegalArgumentException(
                CharSequenceUtil.format("{} 文件读取失败!", LicenseConfig.LICENSE_JSON_FILE_NAME), e);
        }
    }


    /**
     * 保存授权历史数据到缓存文件
     *
     * @param cache 要保存的授权历史数据列表
     * @throws IllegalArgumentException 当文件写入失败时
     */
    public static void saveToCache(Map<String, List<Map<String,String>>> cache) {
        initCacheFile();

        try {
            String jsonStr = JSONUtil.toJsonStr(cache);
            String formattedJson = JSONUtil.formatJsonStr(jsonStr);

            FileUtil.writeString(formattedJson, cacheFile, StandardCharsets.UTF_8);

        } catch (IORuntimeException e) {
            throw new IllegalArgumentException(
                CharSequenceUtil.format("{} 文件写入失败!", LicenseConfig.LICENSE_JSON_FILE_NAME), e);
        }
    }
}