package com.zyp.help.context.certificate;

import cn.hutool.extra.spring.SpringUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;

import java.io.File;

@Slf4j
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CertificateConfig {
    // ==================== 常量定义 ====================

    /** 授权码根密钥文件路径 */
    public static final String CODE_ROOT_KEY_FILE_NAME = "external/certificate/code-root.key";

    /** 服务器根密钥文件路径 */
    public static final String SERVER_ROOT_KEY_FILE_NAME = "external/certificate/server-root.key";

    /** 4096位私钥文件路径 */
    public static final String PRIVATE_KEY_FILE_NAME = "external/certificate/customization/private.key";

    /** 4096位公钥文件路径 */
    public static final String PUBLIC_KEY_FILE_NAME = "external/certificate/customization/public.key";

    /** 授权码证书文件路径 */
    public static final String CODE_CET_FILE_NAME = "external/certificate/customization/code-ca.crt";

    /** 服务器证书文件路径 */
    public static final String SERVER_CET_FILE_NAME = "external/certificate/customization/server-ca.crt";

    /** 服务器子证书文件路径 */
    public static final String SERVER_CHILD_CET_FILE_NAME = "external/certificate/customization/server-child-ca.crt";

    /** 通用4096位私钥文件路径 */
    public static final String COMMON_PRIVATE_KEY_FILE_NAME = "external/certificate/common/private.key";

    /** 通用4096位公钥文件路径 */
    public static final String COMMON_PUBLIC_KEY_FILE_NAME = "external/certificate/common/public.key";

    /** 通用授权码证书文件路径 */
    public static final String COMMON_CODE_CET_FILE_NAME = "external/certificate/common/code-ca.crt";

    /** 通用服务器证书文件路径 */
    public static final String COMMON_SERVER_CET_FILE_NAME = "external/certificate/common/server-ca.crt";

    /** 通用服务器子证书文件路径 */
    public static final String COMMON_SERVER_CHILD_CET_FILE_NAME = "external/certificate/common/server-child-ca.crt";

    /** 证书有效期（年） */
    public static final int CERTIFICATE_VALIDITY_YEARS = 100;

    /** RSA签名算法 */
    public static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    /** BouncyCastle提供者标识 */
    public static final String BC_PROVIDER = "BC";

    /** RSA加密算法标识 */
    public static final String RSA_ALGORITHM = "RSA";

    /** 授权码签名CA的DN */
    public static final String CODE_CA_DN = "CN=JetProfile CA";

    /** 许可证服务器CA的DN */
    public static final String LICENSE_SERVER_CA_DN = "CN=License Servers CA";

    /** 应用程序主体DN */
    public static final String APP_SUBJECT_DN = "CN=Jetbrains-Help";

    // ==================== 静态文件对象 ====================

    /** 授权码根密钥文件对象 */
    public static File codeRootKeyFile;

    /** 服务器根密钥文件对象 */
    public static File serverRootKeyFile;

    /** 4096位私钥文件对象 */
    public static File privateKeyFile;

    /** 4096位公钥文件对象 */
    public static File publicKeyFile;

    /** 授权码证书文件对象 */
    public static File codeCrtFile;

    /** 服务器证书文件对象 */
    public static File serverCrtFile;

    /** 服务器子证书文件对象 */
    public static File serverChildCrtFile;

    // ==================== 配置字段 ====================

    /** 是否启用通用许可证 */
    public boolean common;


    // ==================== 单例实现 ====================

    private static volatile CertificateConfig instance;

    /**
     * 获取配置实例
     *
     * @return 配置实例
     */
    public static CertificateConfig getInstance() {
        if (instance == null) {
            synchronized (CertificateConfig.class) {
                if (instance == null) {
                    instance = new CertificateConfig();
                    instance.loadConfig();
                }
            }
        }
        return instance;
    }

    /**
     * 从Spring环境中加载配置
     */
    private void loadConfig() {
        try {
            Environment environment = SpringUtil.getBean(Environment.class);

            this.common = environment.getProperty("help.certificate.common", Boolean.class, true);

            log.debug("证书配置加载完成 -> 通用证书启用: {}",common);

        } catch (Exception e) {
            log.warn("加载插件配置失败，使用默认值", e);
        }
    }
}
