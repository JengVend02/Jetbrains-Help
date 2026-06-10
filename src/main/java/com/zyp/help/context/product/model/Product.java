package com.zyp.help.context.product.model;

import com.zyp.help.context.common.UpdateTimeCacheList;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class Product extends UpdateTimeCacheList {
    private List<ProductCache> product;

    public void addProductCache(List<ProductCache> product) {
        if (this.product == null) {
            this.product = new ArrayList<>();
        }
        this.product.addAll(product);
    }
}
