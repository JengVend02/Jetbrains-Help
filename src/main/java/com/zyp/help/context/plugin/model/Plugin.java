package com.zyp.help.context.plugin.model;

import com.zyp.help.context.common.UpdateTimeCache;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class Plugin {
    private List<PluginCache> plugin;
    private List<UpdateTimeCache> updateTime;
}
