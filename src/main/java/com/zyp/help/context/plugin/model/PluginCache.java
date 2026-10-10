package com.zyp.help.context.plugin.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@Accessors(chain = true)
public class PluginCache {

    /** 插件ID */
    private Long id;

    /** 产品代码 */
    private String productCode;

    /** 插件链接 */
    private String link;

    /** 插件名称 */
    private String name;

    /** 定价模式（FREE/FREEMIUM/PAID）*/
    private String pricingModel;

    /** 插件图标URL */
    private String icon;

    /** 评分 */
    private Double rating;

    /** 评分信息  */
    private RatingObject ratingObject;;

    /** 贝叶斯平滑-评分 */
    private Double bayesianRating;

    /** 开发商信息 */
    private VendorInfo vendor;


    public PluginCache setRatingObject(RatingObject ratingObject) {
        this.ratingObject = ratingObject;
        setBayesianRating();
        return this;
    }

    public void setBayesianRating() {
        // 1. 统计实际加权总分与实际总票数 v
        double weightedSum = 0.0;
        int totalVotes = 0;

        for (Map.Entry<String, Integer> entry : this.ratingObject.votes.entrySet()) {
            try {
                int score = Integer.parseInt(entry.getKey());
                int count = entry.getValue() != null ? entry.getValue() : 0;

                if (score >= 1 && score <= 5 && count > 0) {
                    weightedSum += score * count;
                    totalVotes += count;
                }
            } catch (NumberFormatException ignored) {
                // 忽略非数字的非正常 key
            }
        }

        // 2. 获取先验参数 m 和 C（带防空值兜底）
        double m = (ratingObject.meanVotes != null) ? ratingObject.meanVotes : 0.0;
        double C = (ratingObject.meanRating != null) ? ratingObject.meanRating : 0.0;

        // 3. 应用贝叶斯平滑公式：(实际加权总分 + m * C) / (实际总票数 + m)
        double bayesianScore = (weightedSum + m * C) / (totalVotes + m);

        // 4. 四舍五入保留 1 位小数（与前端页面展示对齐）
        this.bayesianRating = Math.round(bayesianScore * 10.0) / 10.0;
    }

    /**
     * 开发商信息
     */
    @Data
    @Accessors(chain = true)
    public static class VendorInfo {
        /** 开发商ID */
        private Long id;

        /** 开发商名称 */
        private String name;

        /** 是否已认证 */
        private Boolean isVerified;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PluginCache)) {
            return false;
        }

        return id.equals(((PluginCache) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }


    /**
     * 评分信息
     */
    @Data
    @Accessors(chain = true)
    public static class RatingObject {

        /**
         * 表示该插件目前只有多少用户给出了某星评价 <br>
         * "votes": { "5": 1 }：表示该插件目前只有一个用户给出了 5 星 评价。
         * */
        private Map<String,Integer> votes;

        /** 系统的基准参照票数 */
        private Integer meanVotes;

        /** 全局平均分：所有插件的平均分 */
        private Double meanRating;
    }
}
