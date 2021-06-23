package com.swayingleaves.nettywebsocket.service;


import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhenglin
 */
public class NettyGroupCache {
    /**
     * 存储每一个客户端接入进来的对象
     */
    public static ChannelGroup group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public static Map<String, Channel> userMap = new ConcurrentHashMap<>();
}
