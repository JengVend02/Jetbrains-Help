package com.zyp.help.context;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.PemUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.SignUtil;
import cn.hutool.crypto.asymmetric.Sign;
import cn.hutool.json.JSONUtil;
import java.util.stream.Collectors;

import com.zyp.help.context.certificate.CertificateConfig;
import com.zyp.help.controller.LicenseCodeController;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.List;
import java.util.Set;

import static cn.hutool.crypto.asymmetric.SignAlgorithm.SHA1withRSA;

/**
 * 许可证上下文管理器
 * 
 * <p>此类为整个应用程序的核心组件之一，专门负责JetBrains产品激活码的生成。
 * 它封装了复杂的密码学操作和许可证格式化逻辑，对外提供简单易用的API。
 * 
 * <p>主要功能：
 * <ul>
 *   <li>生成JetBrains兼容的激活码</li>
 *   <li>支持多产品组合许可证</li>
 *   <li>自定义许可证有效期和授权信息</li>
 *   <li>RSA数字签名保证安全性</li>
 * </ul>
 * 
 * <p>激活码格式：
 * {@code 许可证ID-许可证内容Base64-数字签名Base64-证书Base64}
 * 
 * <p>安全特性：
 * <ul>
 *   <li>使用SHA1withRSA数字签名算法</li>
 *   <li>许可证内容使用JSON格式存储</li>
 *   <li>支持X.509证书链验证</li>
 * </ul>
 * 
 * @author zyp
 * @version 1.0.0
 * @since 1.0.0
 */

@Slf4j(topic = "授权上下文")
@NoArgsConstructor(access = AccessLevel.PRIVATE)  // 防止实例化
public class LicenseContextHolder {

    /**
     * 生成JetBrains产品激活码
     * 
     * <p>此方法是整个类的核心方法，负责生成符合JetBrains规范的激活码。
     * 生成的激活码包含了所有必要的许可证信息，并使用RSA数字签名确保安全性。
     * 
     * <p>生成过程：
     * <ol>
     *   <li>生成唯一的许可证ID</li>
     *   <li>构建包含产品信息的许可证对象</li>
     *   <li>将许可证对象转换为JSON并编码为Base64</li>
     *   <li>使用RSA私钥对许可证内容进行数字签名</li>
     *   <li>获取X.509证书并编码为Base64</li>
     *   <li>按照指定格式组装最终的激活码</li>
     * </ol>
     * 
     * <p>激活码格式：{@code 许可证ID-许可证内容Base64-数字签名Base64-证书Base64}
     * 
     * @param body 激活码生成请求体
     * @param productCodeSet 产品代码集合，包含所有需要激活的产品
     * @return 符合JetBrains规范的激活码字符串
     * @throws IllegalArgumentException 当密码学操作或证书处理失败时抛出
     */
    public static String generateLicense(LicenseCodeController.GenerateLicenseReqBody body, Set<String> productCodeSet) {
        String licensesName = body.getLicenseName();
        String assigneeName = body.getAssigneeName();
        String expiryDate = body.getExpiryDate();
        log.info("开始生成许可证 - 许可证名称: {}, 被授权人: {}, 过期日期: {}, 产品数量: {}",
                 licensesName, assigneeName, expiryDate, productCodeSet.size());
        
        // 1. 生成唯一的许可证ID
        String licenseId = IdUtil.fastSimpleUUID();
        log.debug("生成许可证ID: {}", licenseId);
        
        // 2. 构建产品列表，为每个产品设置相同的过期日期
        List<Product> products = productCodeSet.stream()
            .map(productCode -> new Product()
                .setCode(productCode)          // 产品代码
                .setFallbackDate(expiryDate)   // 备用过期日期
                .setPaidUpTo(expiryDate))      // 付费至日期
            .collect(Collectors.toList());
        
        // 3. 构建许可证主体对象
        LicensePart licensePart = new LicensePart()
            .setLicenseId(licenseId)        // 许可证ID
            .setLicenseeName(licensesName)  // 许可证名称
            .setAssigneeName(assigneeName)  // 被授权人
            .setProducts(products);         // 产品列表
        
        // 4. 将许可证对象转换为JSON并进行Base64编码
        String licensePartJson = JSONUtil.toJsonStr(licensePart);
        String licensePartBase64 = Base64.encode(licensePartJson);
        log.debug("许可证JSON内容长度: {} 字符", licensePartJson.length());
        
        // 5. 加载密码学组件：私钥、公钥和证书
        PrivateKey privateKey = PemUtil.readPemPrivateKey(IoUtil.toStream(CertificateConfig.privateKeyFile));
        PublicKey publicKey = PemUtil.readPemPublicKey(IoUtil.toStream(CertificateConfig.publicKeyFile));
        Certificate certificate = SecureUtil.readX509Certificate(IoUtil.toStream(CertificateConfig.codeCrtFile));


        // 6. 使用SHA1withRSA算法进行数字签名
        Sign sign = SignUtil.sign(SHA1withRSA, privateKey.getEncoded(), publicKey.getEncoded());
        String signatureBase64 = Base64.encode(sign.sign(licensePartJson));
        
        // 7. 将X.509证书编码为Base64
        String certBase64;
        try {
            certBase64 = Base64.encode(certificate.getEncoded());
        } catch (CertificateEncodingException e) {
            log.error("证书编码失败", e);
            throw new IllegalArgumentException("证书编码异常", e);
        }
        
        // 8. 按照JetBrains格式组装最终的激活码
        String activationCode = CharSequenceUtil.format("{}-{}-{}-{}", 
            licenseId, licensePartBase64, signatureBase64, certBase64);
        
        log.info("许可证生成成功 - 激活码长度: {} 字符", activationCode.length());
        
        return activationCode;
    }

    // ==================== 内部数据类 ====================

    /**
     * 许可证主体信息实体类
     * 
     * <p>封装了许可证的所有基本信息，包括许可证ID、名称、被授权人和产品列表。
     * 此类的实例将被序列化为JSON格式，作为激活码的一部分。
     * 
     * <p>字段说明：
     * <ul>
     *   <li>licenseId - 许可证的唯一标识符</li>
     *   <li>licenseeName - 许可证名称，通常为公司或组织名称</li>
     *   <li>assigneeName - 被授权人名称，即许可证的使用者</li>
     *   <li>products - 包含在许可证中的产品列表</li>
     *   <li>metadata - 元数据信息，包含版本和类型信息</li>
     * </ul>
     */
    @Data
    @Accessors(chain = true)  // 支持链式调用
    public static class LicensePart {

        /** 许可证唯一标识符，由UUID生成 */
        private String licenseId;
        
        /** 许可证名称，通常为公司或组织名称 */
        private String licenseeName;
        
        /** 被授权人名称，即许可证的使用者 */
        private String assigneeName;
        
        /** 包含在许可证中的产品列表 */
        private List<Product> products;
        
        /** 
         * 元数据信息，默认值说明：
         * - 01: 版本标识
         * - 20230914: 日期标识
         * - PSAX000005: 产品类型和序列号
         */
        private String metadata = "0120230914PSAX000005";
    }

    /**
     * 产品信息实体类
     * 
     * <p>封装了单个产品在许可证中的所有相关信息。
     * 每个产品都包含产品代码、备用过期日期和付费截止日期。
     * 
     * <p>字段说明：
     * <ul>
     *   <li>code - 产品代码，如 "II"(代表IntelliJ IDEA)、"PS"(代表PhpStorm)等</li>
     *   <li>fallbackDate - 备用过期日期，通常与 paidUpTo 相同</li>
     *   <li>paidUpTo - 付费截止日期，表示许可证的有效期</li>
     * </ul>
     * 
     * <p>常见产品代码：
     * <ul>
     *   <li>II - IntelliJ IDEA Ultimate</li>
     *   <li>PS - PhpStorm</li>
     *   <li>WS - WebStorm</li>
     *   <li>PY - PyCharm Professional</li>
     *   <li>PC - PyCharm Community</li>
     *   <li>RM - RubyMine</li>
     *   <li>CL - CLion</li>
     *   <li>DB - DataGrip</li>
     * </ul>
     */
    @Data
    @Accessors(chain = true)  // 支持链式调用
    public static class Product {

        /** 产品代码，用于标识不同的JetBrains产品 */
        private String code;
        
        /** 备用过期日期，格式为 yyyy-MM-dd */
        private String fallbackDate;
        
        /** 付费截止日期，格式为 yyyy-MM-dd */
        private String paidUpTo;
    }

}
