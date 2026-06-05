package com.zyp.help.context.product.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ProductCache {

    /** 产品显示名称，如 "IntelliJ IDEA Ultimate" */
    private String name;

    /** 产品代码，单个或多个用逗号分隔，如 "II" 或 "II,IC" */
    private String productCode;

    /** 产品链接 */
    private String link;

    /** 产品描述 */
    private String description;

    /** 产品图标 */
    private String icon;
}
