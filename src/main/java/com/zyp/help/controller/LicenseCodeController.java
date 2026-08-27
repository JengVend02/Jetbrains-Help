package com.zyp.help.controller;

import com.zyp.help.context.LicenseContextHolder;


import com.zyp.help.context.license.model.GenerateLicenseBody;
import org.springframework.web.bind.annotation.GetMapping;
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
    public GenerateLicenseBody generateLicenseByGet(
            @RequestParam(required = false) String productCode,
            @RequestParam String configKey,
            @RequestParam String licenseeName,
            @RequestParam String assigneeName,
            @RequestParam String expiryDate,
            @RequestParam String licenseType,
            @RequestParam Integer userCount,
            @RequestParam String activationProduct) {

        GenerateLicenseBody body = new GenerateLicenseBody();
        body.setConfigKey(configKey);
        body.setProductCode(productCode);
        body.setLicenseName(licenseeName);
        body.setAssigneeName(assigneeName);
        body.setExpiryDate(expiryDate);
        body.setLicenseType(licenseType);
        body.setUserCount(userCount);
        body.setActivationProduct(activationProduct);

        return LicenseContextHolder.generateLicense(body);
    }
}
