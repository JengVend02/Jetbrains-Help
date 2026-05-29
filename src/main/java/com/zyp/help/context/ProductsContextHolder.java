package com.zyp.help.context;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.json.JSONUtil;
import com.zyp.help.util.FileTools;
import java.util.ArrayList;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;

/**
 * JetBrains产品信息上下文管理器
 *
 * <p>此类负责管理JetBrains所有IDE产品的信息，包括产品名称、产品代码和图标等。
 * 产品信息从JSON配置文件中加载，用于许可证生成时的产品选择和识别。
 *
 * <p>主要功能：
 * <ul>
 *   <li>从JSON文件加载JetBrains产品信息</li>
 *   <li>提供产品信息的缓存和访问接口</li>
 *   <li>支持产品代码到产品名称的映射</li>
 *   <li>为许可证生成提供产品代码列表</li>
 * </ul>
 *
 * <p>支持的产品包括：
 * <ul>
 *   <li>IntelliJ IDEA Ultimate (II)</li>
 *   <li>PhpStorm (PS)</li>
 *   <li>WebStorm (WS)</li>
 *   <li>PyCharm Professional (PY)</li>
 *   <li>RubyMine (RM)</li>
 *   <li>CLion (CL)</li>
 *   <li>DataGrip (DB)</li>
 *   <li>GoLand (GO)</li>
 *   <li>Rider (RD)</li>
 *   <li>AppCode (AC)</li>
 *   <li>以及其他JetBrains系列产品</li>
 * </ul>
 *
 * <p>数据来源：
 * 产品信息可以通过JetBrains官方API获取：
 * {@code https://data.services.jetbrains.com/products?fields=name,salesCode}
 *
 * @author zyp
 * @version 1.0.0
 * @since 1.0.0
 */

@Slf4j(topic = "产品上下文")
@NoArgsConstructor(access = AccessLevel.PRIVATE)  // 防止实例化
public class ProductsContextHolder {

    // ==================== 常量定义 ====================

    /** 产品信息配置文件路径 */
    private static final String PRODUCT_JSON_FILE_NAME = "external/data/product.json";

    // ==================== 静态字段 ====================

    /** 产品信息缓存列表，存储所有加载的产品信息 */
    private static List<ProductCache> productCacheList;

    // TODO 通过该接口可以获取付费IDE的CODE
    // TODO https://data.services.jetbrains.com/products?fields=name,salesCode

    public static void init() {
        log.info("开始初始化产品上下文...");

        // 获取或创建产品配置文件
        File productJsonFile = FileTools.getFileOrCreat(PRODUCT_JSON_FILE_NAME);
        log.debug("产品配置文件路径: {}", productJsonFile.getAbsolutePath());

        // 读取JSON文件内容
        String productJsonArray;
        try {
            productJsonArray = IoUtil.readUtf8(FileUtil.getInputStream(productJsonFile));
            log.debug("成功读取产品配置文件，内容长度: {} 字符", productJsonArray.length());
        } catch (IORuntimeException e) {
            throw new IllegalArgumentException(
                CharSequenceUtil.format("{} 文件读取失败!", PRODUCT_JSON_FILE_NAME), e);
        }

        // 验证JSON文件内容并解析
        if (CharSequenceUtil.isBlank(productJsonArray) || !JSONUtil.isTypeJSON(productJsonArray)) {
            log.error("产品数据不存在或格式错误！配置文件: {}", PRODUCT_JSON_FILE_NAME);
            // 初始化为空列表，避免空指针异常
            productCacheList = new ArrayList<>();
        } else {
            // 解析JSON数据为产品对象列表
            productCacheList = JSONUtil.toList(productJsonArray, ProductCache.class);
            log.info("产品上下文初始化成功！加载产品数量: {}", productCacheList.size());

            // 输出加载的产品信息（调试级别）
            if (log.isDebugEnabled()) {
                productCacheList.forEach(product ->
                    log.debug("加载产品: {} ({})", product.getName(), product.getProductCode()));
            }
        }
    }

    /**
     * 获取产品信息缓存列表
     *
     * <p>返回已加载的所有JetBrains产品信息列表。
     * 该列表包含产品名称、产品代码和图标等信息。
     *
     * <p>使用场景：
     * <ul>
     *   <li>许可证生成时选择产品代码</li>
     *   <li>前端界面显示产品列表</li>
     *   <li>产品代码到产品名称的映射</li>
     * </ul>
     *
     * <p>注意事项：
     * <ul>
     *   <li>返回的是不可变列表，不能直接修改</li>
     *   <li>如果初始化失败，可能返回空列表</li>
     *   <li>需要在 {@link #init()} 方法调用后使用</li>
     * </ul>
     *
     * @return 产品信息缓存列表，不为null
     */
    public static List<ProductCache> productCacheList() {
        return ProductsContextHolder.productCacheList;
    }

    // ==================== 内部数据类 ====================

    /**
     * 产品信息缓存实体类
     *
     * <p>封装了单个JetBrains产品的基本信息，包括产品名称、产品代码和图标类名。
     * 该类的实例从 JSON 配置文件中反序列化得到。
     *
     * <p>字段说明：
     * <ul>
     *   <li>name - 产品显示名称，如 "IntelliJ IDEA Ultimate"</li>
     *   <li>productCode - 产品代码，如 "II" 或用逗号分隔的多个代码</li>
     *   <li>iconClass - CSS图标类名，用于前端界面显示</li>
     * </ul>
     *
     * <p>产品代码示例：
     * <ul>
     *   <li>"II" - IntelliJ IDEA Ultimate</li>
     *   <li>"PS" - PhpStorm</li>
     *   <li>"WS" - WebStorm</li>
     *   <li>"PY" - PyCharm Professional</li>
     *   <li>"RM" - RubyMine</li>
     *   <li>"CL" - CLion</li>
     *   <li>"DB" - DataGrip</li>
     *   <li>"GO" - GoLand</li>
     *   <li>"RD" - Rider</li>
     *   <li>"AC" - AppCode</li>
     * </ul>
     *
     * <p>数据来源：
     * 可以通过JetBrains官方API获取最新产品信息：
     * {@code https://data.services.jetbrains.com/products?fields=name,salesCode}
     */
    @Data
    public static class ProductCache {

        /** 产品显示名称，如 "IntelliJ IDEA Ultimate" */
        private String name;

        /** 产品简称，如 "ii" */
        private String shortName;

        /** 产品代码，单个或多个用逗号分隔，如 "II" 或 "II,IC" */
        private String productCode;

        /** CSS图标类名，用于前端界面显示产品图标 */
        private String iconClass;

        /** 产品图标 */
        private String icon;

        /** 产品描述 */
        private String description;
    }
}
