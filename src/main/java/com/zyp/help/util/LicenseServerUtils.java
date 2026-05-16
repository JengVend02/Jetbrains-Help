package com.zyp.help.util;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.IoUtil;
import cn.hutool.crypto.PemUtil;
import com.zyp.help.context.CertificateContextHolder;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * 许可证服务器工具类
 * 
 * <p>此工具类为许可证服务器提供各种工具方法，主要包括：
 * <ul>
 *   <li>RSA数字签名功能</li>
 *   <li>确认时间戳生成</li>
 *   <li>租约签名生成</li>
 *   <li>XML数据签名和序列化</li>
 * </ul>
 * 
 * <p>此类的所有方法都是静态的，不需要实例化。
 * 所有的密码学操作都基于CertificateContextHolder中管理的证书和密钥。
 * 
 * <p>安全性说明：
 * <ul>
 *   <li>使用RSA非对称加密算法</li>
 *   <li>支持SHA1withRSA和SHA512withRSA签名算法</li>
 *   <li>所有签名都包含相应的X.509证书信息</li>
 * </ul>
 * 
 * @author zyp
 * @version 1.0.0
 * @since 1.0.0
 */
@Slf4j(topic = "许可证服务器工具")
@NoArgsConstructor(access = AccessLevel.PRIVATE)  // 私有构造函数，防止实例化

public class LicenseServerUtils {

    // ==================== 常量定义 ====================
    
    /** 服务器唯一标识符，用于标识许可证服务器实例 */
    public static final String SERVER_UID = "zyp";

    /** 许可证服务器域名：必须以.lsrv.jetbrains.com结尾 */
    private static final String LICENSE_SERVER_DOMAIN = SERVER_UID + ".lsrv.jetbrains.com";

    /** 许可证服务器主体DN */
    public static final String LICENSE_SERVER_SUBJECT_DN = "CN=" + LICENSE_SERVER_DOMAIN;

    /**
     * 服务器租约内容，格式为：过期时间戳:服务器UID
     * 4102415999000 对应的日期为 2099-12-31 23:59:59，表示许可证长期有效
     */
    public static final String LEASES_CONTENT = "4102415999000:" + SERVER_UID;

    // ==================== 核心方法 ====================

    /**
     * 使用指定的私钥和算法对内容进行数字签名
     * 
     * <p>此方法是许可证服务器的核心签名功能，用于确保数据的完整性和可靠性。
     * 签名过程采用标准的RSA数字签名算法，结果使用Base64编码。
     * 
     * <p>支持的签名算法：
     * <ul>
     *   <li>SHA1withRSA - 适用于一般安全级别</li>
     *   <li>SHA256withRSA - 适用于高安全级别</li>
     *   <li>SHA512withRSA - 适用于最高安全级别</li>
     * </ul>
     * 
     * @param content 需要签名的内容字符串
     * @param privateKey RSA私钥，用于生成数字签名
     * @param signAlgorithm 签名算法，如 "SHA1withRSA"、"SHA256withRSA" 等
     * @return Base64编码的数字签名字符串
     * @throws RuntimeException 当签名过程发生错误时抛出
     */
    @SneakyThrows
    public static String signContent(String content, PrivateKey privateKey, String signAlgorithm) {
        log.debug("开始数字签名 - 算法: {}, 内容长度: {} 字符", signAlgorithm, content.length());
        
        // 初始化签名器
        Signature signature = Signature.getInstance(signAlgorithm);
        signature.initSign(privateKey);
        
        // 对内容进行签名（使用UTF-8编码）
        signature.update(content.getBytes(StandardCharsets.UTF_8));
        
        // 生成签名并转换为Base64格式
        String signedContent = Base64.encode(signature.sign());
        
        log.debug("数字签名完成 - 签名长度: {} 字符", signedContent.length());
        
        return signedContent;
    }

    /**
     * 生成机器确认时间戳
     * 
     * <p>确认时间戳用于验证客户端请求的有效性和及时性。
     * 它结合了当前时间戳、机器ID和数字签名，确保请求的安全性。
     * 
     * <p>确认时间戳格式：
     * {@code 时间戳:机器ID:签名算法:签名内容:证书Base64}
     * 
     * <p>生成过程：
     * <ol>
     *   <li>获取当前系统时间戳</li>
     *   <li>构建签名内容：时间戳 + ":" + 机器ID</li>
     *   <li>使用SHA1withRSA算法进行数字签名</li>
     *   <li>获取服务器子证书并转换为Base64</li>
     *   <li>组装最终的确认时间戳字符串</li>
     * </ol>
     * 
     * @param machineId 客户端机器的唯一标识符
     * @return 组装好的确认时间戳字符串
     * @throws RuntimeException 当签名或证书处理发生错误时抛出
     */
    public static String getConfirmationStamp(String machineId) {
        log.debug("开始生成确认时间戳 - 机器ID: {}", machineId);
        
        // 获取当前时间戳
        long timeStamp = System.currentTimeMillis();
        
        // 构建需要签名的内容：时间戳 + ":" + 机器ID
        String contentToSign = timeStamp + ":" + machineId;
        
        // 获取私钥并进行数字签名
        PrivateKey privateKey = PemUtil.readPemPrivateKey(IoUtil.toStream(CertificateContextHolder.privateKeyFile()));
        String signature = signContent(contentToSign, privateKey, "SHA1withRSA");
        
        // 获取服务器子证书并转换为Base64
        String certificateBase64 = Base64.encode(PemUtil.readPem(IoUtil.toStream(CertificateContextHolder.serverChildCrtFile())));
        
        // 组装最终的确认时间戳
        String confirmationStamp = timeStamp + ":" + machineId + ":" + "SHA1withRSA" + ":" + signature + ":" + certificateBase64;
        
        log.debug("确认时间戳生成完成 - 时间戳: {}", timeStamp);
        
        return confirmationStamp;
    }

    /**
     * 生成服务器租约签名
     * 
     * <p>租约签名用于验证服务器的合法性和权威性。
     * 它对预定义的租约内容进行数字签名，并附带相应的证书信息。
     * 
     * <p>租约签名格式：
     * {@code 签名算法-签名内容-证书Base64}
     * 
     * <p>生成过程：
     * <ol>
     *   <li>使用SHA512withRSA算法对租约内容进行数字签名</li>
     *   <li>获取授权码证书并转换为Base64</li>
     *   <li>按照指定格式组装签名结果</li>
     * </ol>
     * 
     * <p>使用SHA512withRSA算法提供更高的安全性，适合于重要的服务器认证场景。
     * 
     * @return 组装好的租约签名字符串
     * @throws RuntimeException 当签名或证书处理发生错误时抛出
     */
    public static String getLeaseSignature() {
        log.debug("开始生成租约签名 - 租约内容: {}", LEASES_CONTENT);
        
        // 获取私钥并使用SHA512withRSA算法进行签名
        PrivateKey privateKey = PemUtil.readPemPrivateKey(IoUtil.toStream(CertificateContextHolder.privateKeyFile()));
        String signature = signContent(LEASES_CONTENT, privateKey, "SHA512withRSA");
        
        // 获取授权码证书并转换为Base64
        String certificateBase64 = Base64.encode(PemUtil.readPem(IoUtil.toStream(CertificateContextHolder.codeCrtFile())));
        
        // 按照格式组装租约签名
        String leaseSignature = "SHA512withRSA-" + signature + "-" + certificateBase64;
        
        log.debug("租约签名生成完成");
        
        return leaseSignature;
    }

    /**
     * 对XML数据进行数字签名并生成完整的XML响应
     * 
     * <p>此方法是许可证服务器响应生成的核心方法。
     * 它将Java对象序列化为XML，然后对XML内容进行数字签名，
     * 最后在XML头部添加签名注释。
     * 
     * <p>XML签名格式：
     * <pre>
     * &lt;!-- SHA1withRSA-签名内容-证书Base64 --&gt;
     * &lt;ResponseObject&gt;
     *   ...实际XML内容...
     * &lt;/ResponseObject&gt;
     * </pre>
     * 
     * <p>序列化过程：
     * <ol>
     *   <li>使用JAXB将Java对象转换为XML字符串</li>
     *   <li>移除XML头部声明（设置JAXB_FRAGMENT为true）</li>
     *   <li>对XML内容使用SHA1withRSA进行数字签名</li>
     *   <li>获取服务器子证书并转换为Base64</li>
     *   <li>在XML内容前添加包含签名信息的注释</li>
     * </ol>
     * 
     * <p>此方法保证了XML响应的完整性和可验证性，
     * JetBrains客户端可以通过验证签名来确认响应的真实性。
     * 
     * @param xmlObject 需要签名的Java对象，必须具有JAXB注解
     * @return 带有数字签名的完整XML字符串
     * @throws RuntimeException 当XML序列化或签名过程发生错误时抛出
     */
    @SneakyThrows
    public static String getSignXml(Object xmlObject) {
        log.debug("开始生成签名XML - 对象类型: {}", xmlObject.getClass().getSimpleName());
        
        // 初始化JAXB上下文
        JAXBContext jaxbContext = JAXBContext.newInstance(xmlObject.getClass());
        
        // 创建序列化器
        Marshaller marshaller = jaxbContext.createMarshaller();
        
        // 设置序列化参数：去除XML头部声明
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        
        // 将Java对象序列化为XML字符串
        StringWriter writer = new StringWriter();
        marshaller.marshal(xmlObject, writer);
        String xmlContent = writer.toString();
        
        log.debug("XML序列化完成 - XML长度: {} 字符", xmlContent.length());
        
        // 获取私钥并对XML内容进行数字签名
        PrivateKey privateKey = PemUtil.readPemPrivateKey(IoUtil.toStream(CertificateContextHolder.privateKeyFile()));
        String signature = signContent(xmlContent, privateKey, "SHA1withRSA");
        
        // 获取服务器子证书并转换为Base64
        String certificateBase64 = Base64.encode(PemUtil.readPem(IoUtil.toStream(CertificateContextHolder.serverChildCrtFile())));
        
        // 构建带有签名注释的完整XML
        String signedXml = "<!-- SHA1withRSA-" + signature + "-" + certificateBase64 + " -->\n" + xmlContent;
        
        log.debug("签名XML生成完成 - 最终长度: {} 字符", signedXml.length());
        
        return signedXml;
    }

}
