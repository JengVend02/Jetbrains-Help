package com.zyp.help.context.common;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true)
public class UpdateTimeCacheList {

    private List<UpdateTimeCache> updateTime;

    public void addUpdateTime(UpdateTimeCache updateTime) {
        if (this.updateTime == null) {
            this.updateTime = new ArrayList<>();
        }
        this.updateTime.add(updateTime);
    }
}