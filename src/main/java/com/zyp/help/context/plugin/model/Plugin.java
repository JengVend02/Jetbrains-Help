package com.zyp.help.context.plugin.model;

import com.zyp.help.context.common.UpdateTimeCacheList;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class Plugin extends UpdateTimeCacheList {
    private List<PluginCache> plugin;
}
