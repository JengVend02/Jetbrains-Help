package com.zyp.help.context.plugin.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 插件详细信息数据模型
 *
 * @author zyp
 * @version 1.0.0
 */
@Data
@Accessors(chain = true)
public class PluginInfo {

    /** 插件ID */
    private Long id;

    /** 购买信息 */
    private PurchaseInfo purchaseInfo;

    /**
     * 购买信息
     */
    @Data
    @Accessors(chain = true)
    public static class PurchaseInfo {

        /** 产品代码 */
        private String productCode;
    }
}