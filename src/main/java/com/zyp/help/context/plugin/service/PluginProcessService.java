package com.zyp.help.context.plugin.service;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.zyp.help.context.plugin.PluginConfig;
import com.zyp.help.context.plugin.model.PluginCache;
import com.zyp.help.context.plugin.model.PluginInfo;
import com.zyp.help.context.plugin.model.PluginList;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 插件处理服务类
 *
 * <p>负责插件数据的业务逻辑处理，包括：
 * <ul>
 *   <li>插件数据的过滤和转换</li>
 *   <li>去重和数据清洗</li>
 *   <li>业务规则应用</li>
 * </ul>
 *
 * @author zyp
 * @version 1.0.0
 */
@Slf4j(topic = "插件处理")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PluginProcessService {

    /**
     * 过滤插件列表
     *
     * <p>过滤条件：
     * <ul>
     *   <li>排除已存在于缓存中的插件</li>
     *   <li>更新插件评分</li>
     * </ul>
     *
     * @param pluginList 原始插件列表
     * @return 过滤后的插件列表
     */
    public static List<PluginList.Plugin> filterPlugins(PluginList pluginList) {
        if (pluginList == null || pluginList.getPlugins() == null) {
            log.warn("插件列表为空，返回空结果");
            return Collections.emptyList();
        }

        // Map<id,index> id(插件的唯一标识) index(插件在缓存中的索引)
        Map<Long, Integer> existingCache  = new HashMap<>();
        for (int i = 0; i < PluginConfig.pluginCache.getPlugin().size(); i++) {
            PluginCache pluginCache = PluginConfig.pluginCache.getPlugin().get(i);
            existingCache.put(pluginCache.getId(),i);
        }

        List<PluginList.Plugin> filteredPlugins = pluginList.getPlugins()
            .stream()
            .filter(plugin -> !isPluginExists(plugin, existingCache))
            .collect(Collectors.toList());

        log.info("插件过滤完成 -> 原始数量: {}, 过滤后数量: {}",
                pluginList.getPlugins().size(), filteredPlugins.size());

        return filteredPlugins;
    }

    /**
     * 将插件基本信息转换为缓存对象
     *
     * @param pluginList 插件基本信息列表
     * @return 插件缓存对象列表
     */
    public static List<PluginCache> convertToCache(List<PluginList.Plugin> pluginList) {
        if (pluginList == null || pluginList.isEmpty()) {
            log.info("没有需要转换的插件数据");
            return Collections.emptyList();
        }

        List<PluginCache> cacheList = pluginList
            .parallelStream()
            .map(PluginProcessService::convertSinglePlugin)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        log.info("插件转换完成 -> 转换数量: {}", cacheList.size());
        return cacheList;
    }

    /**
     * 转换单个插件信息
     *
     * @param plugin 插件基本信息
     * @return 插件缓存对象，如果转换失败返回null
     */
    private static PluginCache convertSinglePlugin(PluginList.Plugin plugin) {
        try {
            PluginInfo pluginInfo = PluginApiService.fetchPluginInfo(plugin);
            if (pluginInfo == null || pluginInfo.getPurchaseInfo() == null) {
                log.warn("插件详情获取失败，跳过插件: {}", plugin.getName());
                return null;
            }

            String productCode = pluginInfo.getPurchaseInfo().getProductCode();
            if (CharSequenceUtil.isBlank(productCode)) {
                log.warn("插件产品代码为空，跳过插件: {}", plugin.getName());
                return null;
            }
            return new PluginCache()
                    .setId(plugin.getId())
                    .setProductCode(productCode)
                    .setLink(buildUrl(pluginInfo.getLink()))
                    .setName(plugin.getName())
                    .setPricingModel(plugin.getPricingModel())
                    .setIcon(buildUrl(pluginInfo.getIcon()))
                    .setRating(plugin.getRating())
                    .setVendor(
                        new PluginCache.VendorInfo()
                        .setId(pluginInfo.getVendor().getId())
                        .setName(pluginInfo.getVendor().getName())
                        .setIsVerified(pluginInfo.getVendor().getIsVerified())
                    );


        } catch (Exception e) {
            log.error("转换插件信息失败: {} (ID: {})", plugin.getName(), plugin.getId(), e);
            return null;
        }
    }

    /**
     * 构建插件完整URL
     *
     * @param path 路径
     * @return 完整的URL，如果路径为空则返回null
     */
    private static String buildUrl(String path) {
        if (StrUtil.isBlank(path)) {
            return null;
        }
        return PluginConfig.PLUGIN_BASIC_URL + path;
    }

    /**
     * 检查插件是否已存在于缓存中,并更新已存在插件的评分
     *
     * @param plugin 插件基本信息
     * @param existingCache 现有缓存
     * @return 如果存在返回true，否则返回false
     */
    private static boolean isPluginExists(PluginList.Plugin plugin, Map<Long, Integer> existingCache) {
        if (existingCache == null || existingCache.isEmpty()) {
            return false;
        }
        // 获取缓存中该插件的索引
        Integer index = existingCache.get(plugin.getId());
        // 缓存中存在插件索引
        if (!ObjectUtils.isEmpty(index)) {
            // 更新插件评分
            PluginConfig.pluginCache.getPlugin().get(index).setRating(plugin.getRating());
            return true;
        }
        return false;
    }
}