package com.zyp.help.context.plugin.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

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

    /** 插件名称 */
    private String name;

    /** 插件链接 */
    private String link;

    /** 是否已审核 */
    private Boolean approve;

    /** 插件XML标识 */
    private String xmlId;

    /** 插件描述（HTML格式） */
//    private String description;

    /** 自定义IDE列表 */
    private Boolean customIdeList;

    /** 预览描述 */
//    private String preview;

    /** 联系邮箱 */
    private String email;

    /** 创建时间戳 */
    private Long cdate;

    /** IDE家族 */
    private String family;

    /** 下载次数 */
    private Integer downloads;

    /** 购买信息 */
    private PurchaseInfo purchaseInfo;

    /** 开发商信息 */
    private VendorInfo vendor;

    /** URL信息 */
    private UrlsInfo urls;

    /** 标签列表 */
    private List<TagInfo> tags;

    /** 是否有未审核的更新 */
    private Boolean hasUnapprovedUpdate;

    /** 定价模式（FREE/FREEMIUM/PAID） */
    private String pricingModel;

    /** 截图列表 */
    private List<ScreenInfo> screens;

    /** 主题列表 */
    private List<ThemeInfo> themes;

    /** 插件图标路径 */
    private String icon;

    /** 仅支持语义化版本 */
    private Boolean semverOnly;

    /** 是否隐藏 */
    private Boolean isHidden;

    /** 是否可用货币化 */
    private Boolean isMonetizationAvailable;

    /** 是否被阻止 */
    private Boolean isBlocked;

    /** 是否允许修改 */
    private Boolean isModificationAllowed;

    /**
     * 购买信息
     */
    @Data
    @Accessors(chain = true)
    public static class PurchaseInfo {

        /** 产品代码 */
        private String productCode;

        /** 购买链接 */
        private String buyUrl;

        /** 购买条款 */
        private String purchaseTerms;

        /** 是否可选 */
        private Boolean optional;

        /** 试用期（天） */
        private Integer trialPeriod;
    }

    /**
     * 开发商信息
     */
    @Data
    @Accessors(chain = true)
    public static class VendorInfo {

        /** 类型（organization/individual） */
        private String type;

        /** 开发商ID */
        private Long id;

        /** 开发商名称 */
        private String name;

        /** 网站URL */
        private String url;

        /** 链接 */
        private String link;

        /** 公开名称 */
        private String publicName;

        /** 邮箱 */
        private String email;

        /** 是否显示邮箱 */
        private Boolean showEmail;

        /** 国家代码 */
        private String countryCode;

        /** 国家 */
        private String country;

        /** 是否已认证 */
        private Boolean isVerified;

        /** 是否为交易商 */
        private Boolean isTrader;

        /** 描述 */
        private String description;

        /** 详细信息 */
        private VendorDetails details;

        /** 市场开发商ID */
        private Long marketplaceVendorId;
    }

    /**
     * 开发商详细信息
     */
    @Data
    @Accessors(chain = true)
    public static class VendorDetails {

        /** 城市 */
        private String city;

        /** 地址 */
        private String address;

        /** 州/省 */
        private String state;

        /** 邮编 */
        private String zip;

        /** 电话 */
        private String phone;
    }

    /**
     * URL信息
     */
    @Data
    @Accessors(chain = true)
    public static class UrlsInfo {

        /** 主页URL */
        private String url;

        /** 论坛URL */
        private String forumUrl;

        /** 许可证URL */
        private String licenseUrl;

        /** 隐私政策URL */
        private String privacyPolicyUrl;

        /** Bug追踪URL */
        private String bugtrackerUrl;

        /** 文档URL */
        private String docUrl;

        /** 源代码URL */
        private String sourceCodeUrl;
    }

    /**
     * 标签信息
     */
    @Data
    @Accessors(chain = true)
    public static class TagInfo {

        /** 标签ID */
        private Long id;

        /** 标签名称 */
        private String name;

        /** 是否为特权标签 */
        private Boolean privileged;

        /** 是否可搜索 */
        private Boolean searchable;

        /** 链接 */
        private String link;
    }

    /**
     * 截图信息
     */
    @Data
    @Accessors(chain = true)
    public static class ScreenInfo {

        /** 截图URL */
        private String url;
    }

    /**
     * 主题信息
     */
    @Data
    @Accessors(chain = true)
    public static class ThemeInfo {

        /** 主题名称 */
        private String name;

        /** 是否为暗色主题 */
        private Boolean dark;
    }
}