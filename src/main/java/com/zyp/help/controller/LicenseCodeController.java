package com.zyp.help.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.zyp.help.context.LicenseContextHolder;
import com.zyp.help.context.PluginsContextHolder;
import com.zyp.help.context.ProductsContextHolder;
import com.zyp.help.context.plugin.model.PluginCache;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 许可证代码生成控制器
 *
 * <p>此控制器专门用于生成JetBrains产品的激活码（许可证代码）。
 * 它能够根据用户的输入参数生成适用于不同产品的激活码。
 *
 * <p>主要功能：
 * <ul>
 *   <li>生成个人或企业版许可证</li>
 *   <li>支持指定产品代码或自动包含所有产品</li>
 *   <li>自定义许可证名称、被授权人和过期日期</li>
 *   <li>支持JetBrains所有付费IDE和插件</li>
 * </ul>
 *
 * <p>生成的激活码格式为：
 * {@code 许可证ID-许可证内容Base64-签名Base64-证书Base64}
 *
 * @author zyp
 * @version 1.0.0
 * @since 1.0.0
 */

@RestController
@RequestMapping("/license-code")
public class LicenseCodeController {

    /**
     * 生成许可证请求参数实体类
     *
     * <p>封装了生成许可证所需的全部参数，包括许可证基本信息和产品代码。
     *
     * <p>参数说明：
     * <ul>
     *   <li>licenseName - 许可证名称，用于标识许可证的来源（如公司名称或个人名称）</li>
     *   <li>assigneeName - 被授权人名称，即许可证的使用者</li>
     *   <li>expiryDate - 过期日期，格式为 yyyy-MM-dd（如：2025-12-31）</li>
     *   <li>productCode - 产品代码，多个代码用逗号分隔，为空时包含所有产品</li>
     * </ul>
     *
     * <p>使用示例：
     * <pre>
     * {
     *   "licenseName": "Technology",
     *   "assigneeName": "张三",
     *   "expiryDate": "2025-12-31",
     *   "productCode": "II,PS,WS,RM,PCC,PC,CLN"
     * }
     * </pre>
     */
    @Data
    public static class GenerateLicenseReqBody {

        /** 许可证名称（公司或组织名称） */
        private String licenseName;

        /** 被授权人名称（使用者名称） */
        private String assigneeName;

        /** 过期日期（格式：yyyy-MM-dd） */
        private String expiryDate;

        /** 产品代码（多个代码用逗号分隔，为空时包含所有产品） */
        private String productCode;
    }

    /**
     * 生成JetBrains产品激活码接口（GET方式）
     *
     * <p>此接口提供GET方式访问，用于前端页面直接调用生成激活码。
     *
     * @param productCode 产品代码
     * @param licenseeName 许可证名称
     * @param assigneeName 被授权人名称
     * @param expiryDate 过期日期
     * @return JetBrains产品激活码字符串
     */
    @GetMapping("/generate")
    public String generateLicenseByGet(
            @RequestParam(required = false) String productCode,
            @RequestParam String licenseeName,
            @RequestParam String assigneeName,
            @RequestParam String expiryDate) {

        GenerateLicenseReqBody body = new GenerateLicenseReqBody();
        body.setProductCode(productCode);
        body.setLicenseName(licenseeName);
        body.setAssigneeName(assigneeName);
        body.setExpiryDate(expiryDate);

        return generateLicense(body);
    }


    /**
     * 生成JetBrains产品激活码接口
     *
     * <p>此接口用于生成JetBrains系列产品的激活码（许可证代码）。
     * 生成的激活码可以用于激活各种JetBrains IDE和插件。
     *
     * <p>功能特点：
     * <ul>
     *   <li>智能产品选择：如果未指定产品代码，自动包含所有可用产品</li>
     *   <li>灵活的过期设置：支持自定义过期日期</li>
     *   <li>完整产品支持：包括IDE和付费插件</li>
     *   <li>RSA数字签名：确保激活码的安全性和完整性</li>
     * </ul>
     *
     * <p>生成过程：
     * <ol>
     *   <li>根据请求参数解析产品代码集合</li>
     *   <li>构建许可证对象，包含产品信息、有效期等</li>
     *   <li>使用RSA私钥对许可证内容进行数字签名</li>
     *   <li>组装最终的激活码字符串</li>
     * </ol>
     *
     * <p>请求示例：
     * <pre>
     * POST /generateLicense
     * Content-Type: application/json
     *
     * {
     *   "licenseName": "Technology",
     *   "assigneeName": "张三",
     *   "expiryDate": "2025-12-31",
     *   "productCode": "II,PS,WS"
     * }
     * </pre>
     *
     * <p>响应示例：
     * <pre>
     * K9V7I1-FLS6QH-eyJsaWNlbnNlSWQiOi...（完整激活码）
     * </pre>
     *
     * @param body 生成许可证的请求参数
     * @return JetBrains产品激活码字符串
     */
    @PostMapping("/generate")
    public String generateLicense(@RequestBody GenerateLicenseReqBody body) {
        // 定义产品代码集合，用于存储所有需要包含在许可证中的产品代码
        Set<String> productCodeSet;

        // 判断是否指定了产品代码
        if (CharSequenceUtil.isBlank(body.getProductCode())) {
            // 未指定产品代码，自动包含所有可用产品

            // 获取所有JetBrains IDE产品代码
            List<String> productCodeList = ProductsContextHolder.productCacheList()
                .stream()
                .map(ProductsContextHolder.ProductCache::getProductCode)  // 提取产品代码
                .filter(StrUtil::isNotBlank)  // 过滤空值
                .map(productCode -> CharSequenceUtil.splitTrim(productCode, ","))  // 按逗号分割
                .flatMap(Collection::stream)  // 展平成一维数据流
                .collect(Collectors.toList());

            // 获取所有付费插件代码
            List<String> pluginCodeList = PluginsContextHolder.pluginCacheList()
                .stream()
                .map(PluginCache::getProductCode)  // 提取插件产品代码
                .filter(StrUtil::isNotBlank)  // 过滤空值
                .collect(Collectors.toList());

            // 合并IDE产品代码和插件代码，去除重复
            productCodeSet = CollUtil.newHashSet(productCodeList);
            productCodeSet.addAll(pluginCodeList);

        } else {
            // 已指定产品代码，解析用户输入的产品代码列表
            productCodeSet = CollUtil.newHashSet(CharSequenceUtil.splitTrim(body.getProductCode(), ','));
        }

        // 调用许可证生成服务，生成最终的激活码
        return LicenseContextHolder.generateLicense(
            body.getLicenseName(),     // 许可证名称
            body.getAssigneeName(),    // 被授权人名称
            body.getExpiryDate(),      // 过期日期
            productCodeSet             // 产品代码集合
        );
    }

    /**
     * 插件激活码生成请求体类
     *
     * <p>此内部类用于封装生成JetBrains插件激活码时的请求参数。
     * 专门针对付费插件的激活需求设计。
     *
     * @since 1.0.0
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneratePluginLicenseReqBody {
        /** 许可证名称（组织名称或公司名称） */
        private String licenseeName;

        /** 被授权人名称（使用者名称） */
        private String assigneeName;

        /** 过期日期（格式：yyyy-MM-dd） */
        private String expiryDate;

        /** 插件ID */
        private String pluginId;
    }

}
