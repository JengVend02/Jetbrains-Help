package com.zyp.help.context.product.model;

import com.zyp.help.context.common.UpdateTimeCacheList;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class Product extends UpdateTimeCacheList {
    private List<ProductCache> product;
}
