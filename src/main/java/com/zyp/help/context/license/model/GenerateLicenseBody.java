package com.zyp.help.context.license.model;

import lombok.Data;

@Data
public class GenerateLicenseBody {

    /** 原始标识,用来标识用户 */
    private String configKey;
    /** 激活码生成时间 */
    private String generationTime;

    /** 许可证名称（公司或组织名称） */
    private String licenseName;

    /** 被授权人名称（使用者名称） */
    private String assigneeName;

    /** 过期日期（格式：yyyy-MM-dd） */
    private String expiryDate;

    /** 许可证类型（PERPETUAL:永久许可证,ANNUAL:年度许可证,MONTHLY:月度许可证） */
    private String licenseType;

    /** 并发用户数 1-1000 */
    private Integer userCount;

    /** 产品代码（多个代码用逗号分隔，为空时包含所有产品） */
    private String productCode;

    /** 激活产品列表 */
    private String activationProduct;

    /** 激活码 */
    private String activationCode;

    /** power */
    private String powerConf;
}
