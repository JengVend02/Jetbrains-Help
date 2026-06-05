package com.zyp.help.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.zyp.help.context.AgentContextHolder;
import com.zyp.help.context.LicenseContextHolder;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.zyp.help.context.plugin.PluginConfig;
import com.zyp.help.context.plugin.model.PluginCache;
import com.zyp.help.context.product.ProductConfig;
import com.zyp.help.context.product.model.ProductCache;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
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
     *   "licenseName": "zyp Technology",
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

        /** 许可证类型（PERPETUAL:永久许可证,ANNUAL:年度许可证,MONTHLY:月度许可证） */
        private String licenseType;

        /** 并发用户数 1-1000 */
        private Integer userCount;

        /** 激活产品列表 */
        private String activationProduct;
    }

    @Data
    public static class GenerateLicenseRespBody {

        /** 激活码生成时间 */
        private String generationTime;

        /** 许可证名称（公司或组织名称） */
        private String licenseName;

        /** 被授权人名称（使用者名称） */
        private String assigneeName;

        /** 过期日期（格式：yyyy-MM-dd） */
        private String expiryDate;

        /** 许可证类型（PERPETUAL:永久许可证,ANNUAL:年度许可证,MONTHLY:月度许可证） */
        private String licenseType;

        /** 并发用户数 1-1000 */
        private Integer userCount;

        /** 激活产品列表 */
        private String activationProduct;

        /** 激活码 */
        private String activationCode;

        /** power */
        private String powerConf;
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
    public GenerateLicenseRespBody generateLicenseByGet(
            @RequestParam(required = false) String productCode,
            @RequestParam String licenseeName,
            @RequestParam String assigneeName,
            @RequestParam String expiryDate,
            @RequestParam String licenseType,
            @RequestParam Integer userCount,
            @RequestParam String activationProduct) {

        GenerateLicenseReqBody body = new GenerateLicenseReqBody();
        body.setProductCode(productCode);
        body.setLicenseName(licenseeName);
        body.setAssigneeName(assigneeName);
        body.setExpiryDate(expiryDate);
        body.setLicenseType(licenseType);
        body.setUserCount(userCount);
        body.setActivationProduct(activationProduct);

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
     *   "licenseName": "zyp Technology",
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
    private GenerateLicenseRespBody generateLicense(@RequestBody GenerateLicenseReqBody body) {
        // 定义产品代码集合，用于存储所有需要包含在许可证中的产品代码
        Set<String> productCodeSet;

        // 定义返回值
        GenerateLicenseRespBody respBody = new GenerateLicenseRespBody();
        BeanUtils.copyProperties(body, respBody);
        respBody.setGenerationTime(DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss"));
        respBody.setPowerConf(AgentContextHolder.getPowerConfContent());

        // 判断是否指定了产品代码
        if (CharSequenceUtil.isBlank(body.getProductCode())) {
            // 未指定产品代码，自动包含所有可用产品

            // 获取所有JetBrains IDE产品代码
            List<String> productCodeList = ProductConfig.productCacheList
                .stream()
                .map(ProductCache::getProductCode)  // 提取产品代码
                .filter(StrUtil::isNotBlank)  // 过滤空值
                .map(productCode -> CharSequenceUtil.splitTrim(productCode, ","))  // 按逗号分割
                .flatMap(Collection::stream)  // 展平成一维数据流
                .collect(Collectors.toList());

            // 获取所有付费插件代码
            List<String> pluginCodeList = PluginConfig.pluginCacheList
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

        // 最终的激活码
        respBody.setActivationCode(LicenseContextHolder.generateLicense(body,productCodeSet));
        return respBody;
    }
}
