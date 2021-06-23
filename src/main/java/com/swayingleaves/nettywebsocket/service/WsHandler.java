package com.swayingleaves.nettywebsocket.service;

import com.alibaba.fastjson.JSONObject;
import com.swayingleaves.nettywebsocket.entity.Msg;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.AttributeKey;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * 接受/处理/响应客户端websocket请求的核心业务处理类
 *
 * @author zhenglin
 */
@Slf4j
public class WsHandler extends SimpleChannelInboundHandler<Object> {

    private WebSocketServerHandshaker webSocketServerHandshaker;
    private static final String WEB_SOCKET_URL = "ws://localhost:8888/webSocket/{uid}";

    /**
     * 客户端与服务端创建链接的时候调用
     */
    @Override
    public void channelActive(ChannelHandlerContext context) throws Exception {
        Channel channel = context.channel();
        NettyGroupCache.group.add(channel);
        log.info("用户接入");
    }

    /**
     * 客户端与服务端断开连接的时候调用
     */
    @Override
    public void channelInactive(ChannelHandlerContext context) throws Exception {
        Channel channel = context.channel();
        NettyGroupCache.group.remove(channel);
        String uid = getUid(channel);
        if (uid != null) {
            NettyGroupCache.userMap.remove(uid);
        }
        log.info("用户:{}断开", uid);
    }

    /**
     * 服务端接收客户端发送过来的数据结束之后调用
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext context) throws Exception {
        context.flush();
    }

    public String getUid(Channel channel) {
        AttributeKey<String> key = AttributeKey.valueOf("user");
        if (channel.attr(key) != null && channel.attr(key).get() != null) {
            return channel.attr(key).get();
        }
        return null;
    }


    /**
     * 工程出现异常的时候调用
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext context, Throwable throwable) throws Exception {
        throwable.printStackTrace();
        context.close();
    }

    /**
     * 服务端处理客户端websocket请求的核心方法
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        //处理客户端向服务端发起的http握手请求
        if (o instanceof FullHttpRequest) {
            handHttpRequest(channelHandlerContext, (FullHttpRequest) o);
        } else if (o instanceof WebSocketFrame) {
            //处理websocket链接业务
            handWebSocketFrame(channelHandlerContext, (WebSocketFrame) o);
        }
    }

    /**
     * 处理客户端与服务端之间的websocket业务
     */
    private void handWebSocketFrame(ChannelHandlerContext context, WebSocketFrame webSocketFrame) {
        //判断是否是关闭websocket的指令
        Channel mainChannel = context.channel();
        if (webSocketFrame instanceof CloseWebSocketFrame) {
            webSocketServerHandshaker.close(mainChannel, (CloseWebSocketFrame) webSocketFrame.retain());
            return;
        }
        //判断是否是ping消息
        if (webSocketFrame instanceof PingWebSocketFrame) {
            mainChannel.write(new PongWebSocketFrame(webSocketFrame.content().retain()));
            return;
        }
        //判断是否是二进制消息
        if (!(webSocketFrame instanceof TextWebSocketFrame)) {
            log.info("不支持二进制消息");
            throw new RuntimeException(this.getClass().getName());
        }
        //返回应答消息
        //获取客户端向服务端发送的消息
        String request = ((TextWebSocketFrame) webSocketFrame).text();

        JSONObject jsonMsg = JSONObject.parseObject(request);
        Msg msg = JSONObject.toJavaObject(jsonMsg, Msg.class);
        String msgType = msg.getMsgType();
        if (Msg.MsgType.USER_MSG.name().equals(msgType)) {
            String toUid = msg.getToUid();
            if (NettyGroupCache.userMap.containsKey(toUid)) {
                Channel channel = NettyGroupCache.userMap.get(toUid);
                Msg newMsg = new Msg();
                newMsg.setMessage(msg.getMessage());
                newMsg.setMsgType(Msg.MsgType.USER_MSG.name());
                TextWebSocketFrame textWebSocketFrame = new TextWebSocketFrame(JSONObject.toJSONString(newMsg));
                channel.writeAndFlush(textWebSocketFrame);
            } else {
                log.info("要发送消息的用户:{}未登录", toUid);
            }
        }
        if (Msg.MsgType.TO_SERVER_MSG.name().equals(msgType)) {
            String uid = getUid(mainChannel);
            log.info("服务器接受到来自客户端用户:{}的消息:{}", uid, request);
        }
    }


    /**
     * 处理客户端向服务端发起http握手请求业务
     */
    private void handHttpRequest(ChannelHandlerContext context, FullHttpRequest fullHttpRequest) throws UnsupportedEncodingException {
        //判断是否http握手请求
        if (!fullHttpRequest.decoderResult().isSuccess() || !("websocket".equals(fullHttpRequest.headers().get("Upgrade")))) {
            sendHttpResponse(context, fullHttpRequest, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        Channel channel = context.channel();
        WebSocketServerHandshakerFactory webSocketServerHandshakerFactory = new WebSocketServerHandshakerFactory(WEB_SOCKET_URL, null, false);
        webSocketServerHandshaker = webSocketServerHandshakerFactory.newHandshaker(fullHttpRequest);
        if (webSocketServerHandshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(channel);
        } else {
            String[] split = fullHttpRequest.uri().split("/");
            String uid = split[split.length - 1];

            if (StringUtils.isBlank(uid)) {
                log.info("用户ID为空");
                return;
            }
            uid = URLDecoder.decode(uid, StandardCharsets.UTF_8);
            if (NettyGroupCache.userMap.containsKey(uid)) {
                throw new RuntimeException("用户:" + uid + "已登录");
            }
            webSocketServerHandshaker.handshake(channel, fullHttpRequest);

            AttributeKey<String> key = AttributeKey.valueOf("user");
            channel.attr(key).set(uid);
            NettyGroupCache.userMap.put(uid, channel);
        }
    }

    /**
     * 服务端向客户端发送响应消息
     */
    private void sendHttpResponse(ChannelHandlerContext context, FullHttpRequest fullHttpRequest, DefaultFullHttpResponse defaultFullHttpResponse) {
        if (defaultFullHttpResponse.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(defaultFullHttpResponse.status().toString(), CharsetUtil.UTF_8);
            defaultFullHttpResponse.content().writeBytes(buf);
            buf.release();
        }
        //服务端向客户端发送数据
        ChannelFuture future = context.channel().writeAndFlush(defaultFullHttpResponse);
        if (defaultFullHttpResponse.status().code() != 200) {
            future.addListener(ChannelFutureListener.CLOSE);
        }

    }

}
