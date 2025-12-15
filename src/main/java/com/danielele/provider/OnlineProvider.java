package com.danielele.provider;

import com.danielele.ServerOnlineFun;
import com.danielele.config.ConfigService;

public interface OnlineProvider
{
    ServerOnlineFun getServerOnline(ConfigService.ServerConfig serverConfig);
}
