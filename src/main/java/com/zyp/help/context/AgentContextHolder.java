package com.zyp.help.context;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.PemUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.zyp.help.context.certificate.CertificateConfig;
import com.zyp.help.util.FileTools;
import java.io.File;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.CompletableFuture;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.util.encoders.Hex;

/**
 * ja-netfilter代理上下文管理器
 * 
 * <p>此类负责管理ja-netfilter代理工具的初始化、配置和打包。
 * ja-netfilter是一个基于Java Instrumentation API实现的动态字节码修改工具，
 * 主要用于在应用程序运行时动态修改字节码，实现特定的功能。
 * 
 * <p>主要功能：
 * <ul>
 *   <li>自动解压ja-netfilter工具包</li>
 *   <li>生成并配置power.conf配置文件</li>
 *   <li>生成适用于JetBrains产品的代理规则</li>
 *   <li>重新打包配置好的ja-netfilter工具</li>
 * </ul>
 * 
 * <p>配置规则生成：
 * <ul>
 *   <li>激活码规则：基于授权码证书和密钥生成</li>
 *   <li>许可证服务器规则：基于服务器证书和密钥生成</li>
 *   <li>使用RSA密码学算法和ASN.1编码</li>
 * </ul>
 * 
 * <p>文件结构：
 * <pre>
 * external/agent/
 * ├── ja-netfilter.zip     // 原始工具包
 * └── ja-netfilter/        // 解压后的目录
 *     ├── ja-netfilter.jar // 主程序
 *     ├── plugins/         // 插件目录
 *     └── config/          // 配置目录
 *         └── power.conf   // 主配置文件
 * </pre>
 * 
 * <p>使用方法：
 * <ol>
 *   <li>下载并解压ja-netfilter工具包</li>
 *   <li>在JVM启动参数中添加：{@code -javaagent:path/to/ja-netfilter.jar}</li>
 *   <li>重新启动目标应用程序</li>
 * </ol>
 * 
 * @author zyp
 * @version 1.0.0
 * @since 1.0.0
 */

@Slf4j(topic = "代理上下文")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AgentContextHolder {

    // ==================== 常量定义 ====================
    
    /** ja-netfilter工具文件夹路径 */
    private static final String JA_NETFILTER_FILE_PATH = "external/agent/ja-netfilter";

    /** POWER配置文件路径 */
    private static final String POWER_CONF_FILE_NAME = JA_NETFILTER_FILE_PATH + "/config/power.conf";

    // ==================== 静态字段 ====================
    
    /** ja-netfilter工具文件夹对象 */
    private static File jaNetfilterFile;

    /** ja-netfilter ZIP包文件对象 */
    private static File jaNetfilterZipFile;

    /** POWER配置文件内容 */
    private static volatile String POWER_CONF_STR = null;
    // ==================== 核心方法 ====================

    public static void init() {
        log.info("配置初始化中...");
        jaNetfilterZipFile = FileTools.getFileOrCreat(JA_NETFILTER_FILE_PATH + ".zip");
        unzipJaNetfilter();
        loadPowerConf();
        zipJaNetfilter();
        log.info("配置初始化成功!");
    }

    public static File jaNetfilterZipFile() {
        return AgentContextHolder.jaNetfilterZipFile;
    }

    /**
     * 获取 power.conf 文件内容
     *
     * @return power.conf 文件内容
     */
    public static String getPowerConfContent() {
        // 第一重检查：如果不为空，直接返回，不走锁，保证性能
        if (CharSequenceUtil.isBlank(POWER_CONF_STR)) {
            synchronized (AgentContextHolder.class) {
                // 第二重检查：进入锁后再次判空，防止排队等待的线程再次读取 IO
                if (CharSequenceUtil.isBlank(POWER_CONF_STR)) {
                    File powerConfFile = FileTools.getFileOrCreat(POWER_CONF_FILE_NAME);
                    try {
                        log.info("从文件加载配置缓存...");
                        POWER_CONF_STR = IoUtil.readUtf8(FileUtil.getInputStream(powerConfFile));
                    } catch (IORuntimeException e) {
                        throw new IllegalArgumentException(CharSequenceUtil.format("{} 文件读取失败!", POWER_CONF_FILE_NAME), e);
                    }
                }
            }
        }
        return POWER_CONF_STR;
    }

    private static void unzipJaNetfilter() {
        jaNetfilterFile = ZipUtil.unzip(jaNetfilterZipFile);
    }

    private static void zipJaNetfilter() {
        jaNetfilterZipFile = ZipUtil.zip(jaNetfilterFile);
    }

    private static void loadPowerConf() {
        CompletableFuture
            .supplyAsync(AgentContextHolder::generateCodePowerConfigRule)
            .thenApply(AgentContextHolder::generateServerPowerConfigRule)
            .thenApply(AgentContextHolder::generatePowerConfigStr)
            .thenAccept(AgentContextHolder::overridePowerConfFileContent)
            .exceptionally(throwable -> {
                log.error("配置初始化失败!", throwable);
                return null;
            }).join();
    }

    @SneakyThrows
    private static String generateCodePowerConfigRule() {
        X509Certificate crt = (X509Certificate) KeyUtil.readX509Certificate(
            IoUtil.toStream(CertificateConfig.codeCrtFile));
        RSAPublicKey publicKey = (RSAPublicKey) PemUtil.readPemPublicKey(
            IoUtil.toStream(CertificateConfig.publicKeyFile));
        RSAPublicKey rootPublicKey = (RSAPublicKey) PemUtil.readPemPublicKey(
            IoUtil.toStream(CertificateConfig.codeRootKeyFile));
        BigInteger x = new BigInteger(1, crt.getSignature());
        BigInteger y = BigInteger.valueOf(65537L);
        BigInteger z = rootPublicKey.getModulus();
        BigInteger r = x.modPow(publicKey.getPublicExponent(), publicKey.getModulus());
        return CharSequenceUtil.format("; Activation Code \nEQUAL,{},{},{}->{}", x, y, z, r);
    }

    @SneakyThrows
    private static String generateServerPowerConfigRule(String ruleValue) {
        X509Certificate crt = (X509Certificate) KeyUtil.readX509Certificate(
            IoUtil.toStream(CertificateConfig.serverChildCrtFile));
        RSAPublicKey rootPublicKey = (RSAPublicKey) PemUtil.readPemPublicKey(
            IoUtil.toStream(CertificateConfig.serverRootKeyFile));
        BigInteger x = new BigInteger(1, crt.getSignature());
        BigInteger y = BigInteger.valueOf(65537L);
        BigInteger z = rootPublicKey.getModulus();
        byte[] tbsCertificate = crt.getTBSCertificate();
        // 1、证书sha256摘要结果
        byte[] bytes = DigestUtil.sha256(tbsCertificate);
        String sha256Str = HexUtil.encodeHexStr(bytes);
        // 2、计算的结果转换为ASN1格式数据
        String transit = convertDataToASN1Format(sha256Str);
        // 3、ASN1格式数据再进行填充
        String fillingStr = filling512(transit);
        // 4、填充后的数据转换为BigInteger数据，BigInteger输出的结果就是规则中替换的结果。
        BigInteger r = new BigInteger(HexUtil.decodeHex(fillingStr));
        return CharSequenceUtil.format("{}\n; License Server \nEQUAL,{},{},{}->{}", ruleValue,x, y, z, r);
    }


    private static String generatePowerConfigStr(String ruleValue) {
        return CharSequenceUtil.builder("[Result]", "\n", ruleValue).toString();
    }

    private static void overridePowerConfFileContent(String configStr) {
        File powerConfFile = FileTools.getFileOrCreat(POWER_CONF_FILE_NAME);
        try {
            POWER_CONF_STR = configStr;
            FileUtil.writeString(configStr, powerConfFile, StandardCharsets.UTF_8);
        } catch (IORuntimeException e) {
            throw new IllegalArgumentException(CharSequenceUtil.format("{} 文件写入失败!", POWER_CONF_FILE_NAME), e);
        }
    }

    /**
     * 证书sha256摘要结果转换为ASN1格式数据
     *
     * @param sha256Data 证书sha256摘要结果
     * @return
     */
    public static String convertDataToASN1Format(String sha256Data) throws Exception {
        // 构建内层 SEQUENCE
        ASN1ObjectIdentifier algorithmOid = new ASN1ObjectIdentifier("2.16.840.1.101.3.4.2.1"); // sha-256
        ASN1Encodable[] innerSequenceElements = {algorithmOid, DERNull.INSTANCE};
        DERSequence innerSequence = new DERSequence(innerSequenceElements);

        // 构建外层 SEQUENCE
        byte[] octetStringBytes = Hex.decode(sha256Data);
        ASN1Encodable octetString = new DEROctetString(octetStringBytes);
        ASN1Encodable[] outerSequenceElements = {innerSequence, octetString};
        DERSequence outerSequence = new DERSequence(outerSequenceElements);

        // 将ASN.1结构编码为DER格式
        byte[] encodedData = outerSequence.getEncoded();

        // 将字节数组转换为十六进制字符串
        String hexString = Hex.toHexString(encodedData);
        return hexString;
    }

    private static String filling512(String target) {
        return filling(target, 512);
    }

    private static String filling256(String target) {
        return filling(target, 256);
    }

    private static String filling(String target, int length) {
        int count = length - target.length() / 2 - 3;
        StringBuilder strBuilder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            strBuilder.append("ff");
        }
        return ("01" + strBuilder + "00" + target).toUpperCase();
    }

}
