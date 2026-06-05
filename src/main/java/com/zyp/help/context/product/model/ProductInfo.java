package com.zyp.help.context.product.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 产品列表数据模型
 *
 * @author zyp
 * @version 1.0.0
 */
@Data
@Accessors(chain = true)
public class ProductInfo {
    /** 产品显示名称，如 "IntelliJ IDEA Ultimate" */
    private String name;

    /** 产品代码，单个或多个用逗号分隔，如 "II" 或 "II,IC" */
    private String code;

    /** 产品代码，单个或多个用逗号分隔，如 "II" 或 "II,IC" */
    private String salesCode;

    /** 产品链接 */
    private String link;

    /** 产品描述 */
    private String description;

    /** 是否收费 */
    private Boolean forSale;
}