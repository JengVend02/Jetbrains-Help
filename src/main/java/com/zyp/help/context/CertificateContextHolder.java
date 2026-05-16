package com.zyp.help.context;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.PemUtil;
import cn.hutool.crypto.SecureUtil;
import com.zyp.help.util.FileTools;
import com.zyp.help.util.LicenseServerUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.GeneralNamesBuilder;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.File;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * 证书上下文持有者 - 负责管理RSA密钥对和X.509证书的生成、存储和访问
 *
 * <p>此类作为证书管理的核心工具，提供以下主要功能：
 * <ul>
 *   <li>自动生成RSA密钥对（支持2048位和4096位）</li>
 *   <li>生成自签名的X.509证书</li>
 *   <li>管理证书文件的存储和访问</li>
 *   <li>支持CA证书链的构建</li>
 * </ul>
 *
 * <p>使用场景：
 * 主要用于JetBrains产品的许可证服务器搭建，生成必要的证书文件用于许可证验证。
 *
 * <p>证书体系结构：
 * <ul>
 *   <li>授权码签名CA证书：用于授权码签名验证</li>
 *   <li>服务器CA证书：用于HTTPS通信</li>
 *   <li>服务器子证书：具体域名的服务器证书</li>
 * </ul>
 *
 * @author zyp
 * @version 1.0
 * @since 1.0
 */
@Slf4j(topic = "证书上下文")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CertificateContextHolder {

    // ==================== 常量定义 ====================

    /** 授权码根密钥文件路径 */
    private static final String CODE_ROOT_KEY_FILE_NAME = "external/certificate/code-root.key";

    /** 服务器根密钥文件路径 */
    private static final String SERVER_ROOT_KEY_FILE_NAME = "external/certificate/server-root.key";

    /** 4096位私钥文件路径 */
    private static final String PRIVATE_KEY_FILE_NAME = "external/certificate/private.key";

    /** 4096位公钥文件路径 */
    private static final String PUBLIC_KEY_FILE_NAME = "external/certificate/public.key";

    /** 授权码证书文件路径 */
    private static final String CODE_CET_FILE_NAME = "external/certificate/code-ca.crt";

    /** 服务器证书文件路径 */
    private static final String SERVER_CET_FILE_NAME = "external/certificate/server-ca.crt";

    /** 服务器子证书文件路径 */
    private static final String SERVER_CHILD_CET_FILE_NAME = "external/certificate/server-child-ca.crt";

    /** 证书有效期（年） */
    private static final int CERTIFICATE_VALIDITY_YEARS = 100;

    /** RSA签名算法 */
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    /** BouncyCastle提供者标识 */
    private static final String BC_PROVIDER = "BC";

    /** RSA加密算法标识 */
    private static final String RSA_ALGORITHM = "RSA";

    /** 授权码签名CA的DN */
    private static final String CODE_CA_DN = "CN=JetProfile CA";

    /** 许可证服务器CA的DN */
    private static final String LICENSE_SERVER_CA_DN = "CN=License Servers CA";

    /** 应用程序主体DN */
    private static final String APP_SUBJECT_DN = "CN=Jetbrains-Help";

    // ==================== 静态文件对象 ====================

    /** 授权码根密钥文件对象 */
    private static File codeRootKeyFile;

    /** 服务器根密钥文件对象 */
    private static File serverRootKeyFile;

    /** 4096位私钥文件对象 */
    private static File privateKeyFile;

    /** 4096位公钥文件对象 */
    private static File publicKeyFile;

    /** 授权码证书文件对象 */
    private static File codeCrtFile;

    /** 服务器证书文件对象 */
    private static File serverCrtFile;

    /** 服务器子证书文件对象 */
    private static File serverChildCrtFile;

    // ==================== 初始化方法 ====================

    /**
     * 初始化证书上下文
     *
     * <p>此方法执行以下操作：
     * <ol>
     *   <li>创建根密钥文件（如果不存在）</li>
     *   <li>检查所需的密钥和证书文件是否存在</li>
     *   <li>如果文件缺失，自动生成完整的证书体系</li>
     *   <li>初始化所有文件对象引用</li>
     * </ol>
     *
     * @throws RuntimeException 当文件操作或证书生成失败时抛出
     */
    public static void init() {
        log.info("证书上下文初始化开始...");

        try {
            // 初始化根密钥文件
            initializeRootKeyFiles();

            // 检查并生成必要的证书文件
            if (isCertificateGenerationRequired()) {
                log.info("检测到证书文件缺失，开始生成证书...");
                generateCertificate();
                log.info("证书生成完成!");
            } else {
                log.info("证书文件已存在，直接加载...");
                initializeExistingFiles();
            }

            log.info("证书上下文初始化成功!");

        } catch (Exception e) {
            log.error("证书上下文初始化失败", e);
            throw new RuntimeException("证书初始化异常", e);
        }
    }

    /**
     * 初始化根密钥文件
     * 创建授权码根密钥和服务器根密钥文件（如果不存在）
     */
    private static void initializeRootKeyFiles() {
        codeRootKeyFile = FileTools.getFileOrCreat(CODE_ROOT_KEY_FILE_NAME);
        serverRootKeyFile = FileTools.getFileOrCreat(SERVER_ROOT_KEY_FILE_NAME);
        log.debug("根密钥文件初始化完成");
    }

    /**
     * 检查是否需要生成证书
     * 如果任意一个必需文件不存在，则需要重新生成所有证书
     *
     * @return 如果任意一个必需文件不存在则返回true
     */
    private static boolean isCertificateGenerationRequired() {
        return !FileTools.fileExists(PRIVATE_KEY_FILE_NAME)
            || !FileTools.fileExists(PUBLIC_KEY_FILE_NAME)
            || !FileTools.fileExists(CODE_CET_FILE_NAME)
            || !FileTools.fileExists(SERVER_CET_FILE_NAME)
            || !FileTools.fileExists(SERVER_CHILD_CET_FILE_NAME);
    }

    /**
     * 初始化已存在的文件对象
     * 当所有证书文件都存在时，直接创建文件对象引用
     */
    private static void initializeExistingFiles() {
        privateKeyFile = FileTools.getFileOrCreat(PRIVATE_KEY_FILE_NAME);
        publicKeyFile = FileTools.getFileOrCreat(PUBLIC_KEY_FILE_NAME);
        codeCrtFile = FileTools.getFileOrCreat(CODE_CET_FILE_NAME);
        serverCrtFile = FileTools.getFileOrCreat(SERVER_CET_FILE_NAME);
        serverChildCrtFile = FileTools.getFileOrCreat(SERVER_CHILD_CET_FILE_NAME);
        log.debug("已存在文件对象初始化完成");
    }

    // ==================== 文件访问方法 ====================

    /**
     * 获取授权码根密钥文件对象
     *
     * @return 授权码根密钥文件
     */
    public static File codeRootKeyFile() {
        return codeRootKeyFile;
    }

    /**
     * 获取服务器根密钥文件对象
     *
     * @return 服务器根密钥文件
     */
    public static File serverRootKeyFile() {
        return serverRootKeyFile;
    }

    /**
     * 获取4096位私钥文件对象
     *
     * @return 4096位私钥文件
     */
    public static File privateKeyFile() {
        return privateKeyFile;
    }

    /**
     * 获取4096位公钥文件对象
     *
     * @return 4096位公钥文件
     */
    public static File publicKeyFile() {
        return publicKeyFile;
    }

    /**
     * 获取授权码证书文件对象
     *
     * @return 授权码证书文件
     */
    public static File codeCrtFile() {
        return codeCrtFile;
    }

    /**
     * 获取服务器证书文件对象
     *
     * @return 服务器证书文件
     */
    public static File serverCrtFile() {
        return serverCrtFile;
    }

    /**
     * 获取服务器子证书文件对象
     *
     * @return 服务器子证书文件
     */
    public static File serverChildCrtFile() {
        return serverChildCrtFile;
    }

    // ==================== 证书生成方法 ====================

    /**
     * 生成完整的证书体系
     *
     * <p>此方法将生成以下内容：
     * <ul>
     *   <li>4096位RSA密钥对（用于CA证书）</li>
     *   <li>授权码签名CA证书</li>
     *   <li>许可证服务器CA证书</li>
     *   <li>具体域名的服务器证书（包含扩展属性）</li>
     * </ul>
     *
     * @throws RuntimeException 当密钥生成或证书创建失败时抛出
     */
    public static void generateCertificate() {
        log.info("开始生成证书体系...");

        try {
            // 生成密钥对
            log.debug("正在生成RSA密钥对...");
            KeyPair keyPair4096 = generateRSAKeyPair(4096);

            // 保存密钥对到文件
            saveKeyPairToFiles(keyPair4096, PRIVATE_KEY_FILE_NAME, PUBLIC_KEY_FILE_NAME);

            // 生成证书
            log.debug("正在生成X.509证书...");
            generateCertificates(keyPair4096);

            log.info("证书体系生成完成");

        } catch (Exception e) {
            log.error("证书生成过程中发生错误", e);
            throw new RuntimeException("证书生成失败", e);
        }
    }

    /**
     * 生成指定长度的RSA密钥对
     *
     * @param keySize RSA密钥长度（位）
     * @return 生成的RSA密钥对
     * @throws RuntimeException 当密钥生成失败时抛出
     */
    private static KeyPair generateRSAKeyPair(int keySize) {
        try {
            log.debug("生成{}位RSA密钥对", keySize);
            return SecureUtil.generateKeyPair(RSA_ALGORITHM, keySize);
        } catch (Exception e) {
            throw new RuntimeException("RSA密钥对生成失败: " + keySize + "位", e);
        }
    }

    /**
     * 将密钥对保存到指定的文件中
     *
     * @param keyPair     要保存的密钥对
     * @param privateKeyPath 私钥文件路径
     * @param publicKeyPath  公钥文件路径
     */
    private static void saveKeyPairToFiles(KeyPair keyPair, String privateKeyPath, String publicKeyPath) {
        try {
            // 保存私钥
            File privateKeyFileObj = FileTools.getFileOrCreat(privateKeyPath);
            PemUtil.writePemObject("PRIVATE KEY", keyPair.getPrivate().getEncoded(),
                FileUtil.getWriter(privateKeyFileObj, StandardCharsets.UTF_8, false));

            // 保存公钥
            File publicKeyFileObj = FileTools.getFileOrCreat(publicKeyPath);
            PemUtil.writePemObject("PUBLIC KEY", keyPair.getPublic().getEncoded(),
                FileUtil.getWriter(publicKeyFileObj, StandardCharsets.UTF_8, false));

            publicKeyFile = publicKeyFileObj;
            privateKeyFile = privateKeyFileObj;

            log.debug("密钥对已保存: {} 和 {}", privateKeyPath, publicKeyPath);

        } catch (Exception e) {
            throw new RuntimeException("密钥对文件保存失败", e);
        }
    }

    /**
     * 生成所有类型的X.509证书
     *
     * @param keyPair 4096位密钥对
     */
    private static void generateCertificates(KeyPair keyPair) {
        try {
            PrivateKey privateKey4096 = keyPair.getPrivate();
            PublicKey publicKey4096 = keyPair.getPublic();

            // 创建内容签名器
            ContentSigner signer = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).build(privateKey4096);
            JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter().setProvider(BC_PROVIDER);

            // 生成授权码签名CA证书
            Certificate codeCertificate = generateCodeCACertificate(publicKey4096, signer, certConverter);

            // 生成许可证服务器CA证书
            Certificate serverCertificate = generateServerCACertificate(publicKey4096, signer, certConverter);

            // 生成服务器子证书（具有特殊扩展）
            Certificate childCertificate = generateServerChildCertificate(publicKey4096, signer, certConverter);

            // 保存所有证书到文件
            saveCertificatesToFiles(codeCertificate, serverCertificate, childCertificate);

        } catch (Exception e) {
            throw new RuntimeException("X.509证书生成失败", e);
        }
    }

    /**
     * 生成授权码签名CA证书
     *
     * @param publicKey      公钥
     * @param signer        内容签名器
     * @param certConverter 证书转换器
     * @return 生成的授权码签名CA证书
     */
    private static Certificate generateCodeCACertificate(PublicKey publicKey, ContentSigner signer,
        JcaX509CertificateConverter certConverter) throws Exception {
        JcaX509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(
            new X500Name(CODE_CA_DN),                                      // 颁发者DN
            BigInteger.valueOf(System.currentTimeMillis()),                // 序列号
            DateUtil.yesterday().toJdkDate(),                                          // 生效日期（昨天，避免时钟偏差）
            DateUtil.date().offset(DateField.YEAR, CERTIFICATE_VALIDITY_YEARS).toJdkDate(), // 过期日期
            new X500Name(APP_SUBJECT_DN),                                   // 主体DN
            SubjectPublicKeyInfo.getInstance(publicKey.getEncoded()));      // 公钥信息

        return certConverter.getCertificate(certificateBuilder.build(signer));
    }

    /**
     * 生成许可证服务器CA证书
     *
     * @param publicKey      公钥
     * @param signer        内容签名器
     * @param certConverter 证书转换器
     * @return 生成的许可证服务器CA证书
     */
    private static Certificate generateServerCACertificate(PublicKey publicKey, ContentSigner signer,
        JcaX509CertificateConverter certConverter) throws Exception {
        JcaX509v3CertificateBuilder serversCertBuilder = new JcaX509v3CertificateBuilder(
            new X500Name(LICENSE_SERVER_CA_DN),                            // 颁发者DN
            BigInteger.valueOf(System.currentTimeMillis()),                // 序列号
            DateUtil.yesterday().toJdkDate(),                                          // 生效日期
            DateUtil.date().offset(DateField.YEAR, CERTIFICATE_VALIDITY_YEARS).toJdkDate(), // 过期日期
            new X500Name(APP_SUBJECT_DN),                                   // 主体DN
            SubjectPublicKeyInfo.getInstance(publicKey.getEncoded()));      // 公钥信息

        return certConverter.getCertificate(serversCertBuilder.build(signer));
    }

    /**
     * 生成服务器子证书（包含扩展属性）
     *
     * <p>此证书包含以下扩展属性：
     * <ul>
     *   <li>扩展密钥用途：服务器认证和客户端认证</li>
     *   <li>主体备用名称：DNS域名</li>
     *   <li>密钥用途：数字签名、密钥加密、密钥协商</li>
     *   <li>权威密钥标识符</li>
     *   <li>主体密钥标识符</li>
     * </ul>
     *
     * @param publicKey4096  4096位公钥（用于权威密钥标识符）
     * @param signer        内容签名器
     * @param certConverter 证书转换器
     * @return 生成的服务器子证书
     */
    private static Certificate generateServerChildCertificate(PublicKey publicKey4096,
        ContentSigner signer, JcaX509CertificateConverter certConverter) throws Exception {
        JcaX509v3CertificateBuilder childCertBuilder = new JcaX509v3CertificateBuilder(
            new X500Name(LICENSE_SERVER_CA_DN),                            // 颁发者DN
            BigInteger.valueOf(System.currentTimeMillis()),                // 序列号
            DateUtil.yesterday().toJdkDate(),                                          // 生效日期
            DateUtil.date().offset(DateField.YEAR, CERTIFICATE_VALIDITY_YEARS).toJdkDate(), // 过期日期
            new X500Name(LicenseServerUtils.LICENSE_SERVER_SUBJECT_DN),                        // 主体DN（具体域名）
            SubjectPublicKeyInfo.getInstance(publicKey4096.getEncoded()));   // 公钥信息

        // 添加证书扩展
        addCertificateExtensions(childCertBuilder, publicKey4096);

        return certConverter.getCertificate(childCertBuilder.build(signer));
    }

    /**
     * 为服务器子证书添加扩展属性
     *
     * @param certBuilder    证书构建器
     * @param publicKey4096  4096位公钥
     */
    private static void addCertificateExtensions(JcaX509v3CertificateBuilder certBuilder,PublicKey publicKey4096) throws Exception {
        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();

        // 1. 扩展密钥用途：服务器认证和客户端认证
        certBuilder.addExtension(Extension.extendedKeyUsage, false,
            new ExtendedKeyUsage(new KeyPurposeId[]{
                KeyPurposeId.id_kp_serverAuth,  // 服务器认证
                KeyPurposeId.id_kp_clientAuth   // 客户端认证
            }));

        // 2. 主体备用名称：DNS域名
        GeneralNamesBuilder generalNamesBuilder = new GeneralNamesBuilder();
        GeneralName dnsName = new GeneralName(GeneralName.dNSName, LicenseServerUtils.LICENSE_SERVER_SUBJECT_DN);
        generalNamesBuilder.addName(dnsName);
        GeneralNames generalNames = generalNamesBuilder.build();
        certBuilder.addExtension(Extension.subjectAlternativeName, false, generalNames);

        // 3. 密钥用途：数字签名、密钥加密、密钥协商
        certBuilder.addExtension(Extension.keyUsage, true,
            new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment | KeyUsage.keyAgreement));

        // 4. 权威密钥标识符（基于服务器根证书）
        X509Certificate templateCert = (X509Certificate) KeyUtil.readX509Certificate(
            IoUtil.toStream(serverRootKeyFile));
        certBuilder.addExtension(Extension.authorityKeyIdentifier, false,
            extUtils.createAuthorityKeyIdentifier(
                SubjectPublicKeyInfo.getInstance(templateCert.getPublicKey().getEncoded())));

        // 5. 主体密钥标识符（基于2048位公钥）
        certBuilder.addExtension(Extension.subjectKeyIdentifier, false,
            extUtils.createSubjectKeyIdentifier(SubjectPublicKeyInfo.getInstance(publicKey4096.getEncoded())));

        log.debug("服务器子证书扩展属性添加完成");
    }

    /**
     * 将生成的证书保存到对应的文件中
     *
     * @param codeCertificate   授权码签名CA证书
     * @param serverCertificate 许可证服务器CA证书
     * @param childCertificate  服务器子证书
     */
    private static void saveCertificatesToFiles(Certificate codeCertificate, Certificate serverCertificate,
        Certificate childCertificate) throws Exception {
        // 创建证书文件对象
        codeCrtFile = FileTools.getFileOrCreat(CODE_CET_FILE_NAME);
        serverCrtFile = FileTools.getFileOrCreat(SERVER_CET_FILE_NAME);
        serverChildCrtFile = FileTools.getFileOrCreat(SERVER_CHILD_CET_FILE_NAME);

        // 保存授权码签名CA证书
        PemUtil.writePemObject("CERTIFICATE", codeCertificate.getEncoded(),
            FileUtil.getWriter(codeCrtFile, StandardCharsets.UTF_8, false));

        // 保存许可证服务器CA证书
        PemUtil.writePemObject("CERTIFICATE", serverCertificate.getEncoded(),
            FileUtil.getWriter(serverCrtFile, StandardCharsets.UTF_8, false));

        // 保存服务器子证书
        PemUtil.writePemObject("CERTIFICATE", childCertificate.getEncoded(),
            FileUtil.getWriter(serverChildCrtFile, StandardCharsets.UTF_8, false));

        log.debug("所有证书文件保存完成");
    }

}
