package com.zyp.help.context.product.model;

import com.zyp.help.context.common.UpdateTimeCache;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class Product {
    private List<ProductCache> product;
    private List<UpdateTimeCache> updateTime;
}
