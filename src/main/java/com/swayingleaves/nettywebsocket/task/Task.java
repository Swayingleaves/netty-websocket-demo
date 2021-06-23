package com.swayingleaves.nettywebsocket.task;

import com.alibaba.fastjson.JSONObject;
import com.swayingleaves.nettywebsocket.entity.Msg;
import com.swayingleaves.nettywebsocket.service.NettyGroupCache;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * @author zhenglin
 * @date 2021/6/23
 */
@Slf4j
@Component
public class Task {

    @Scheduled(cron = "*/2 * * * * ?")
    public void run() {
        if (NettyGroupCache.group.size() != 0) {
            LocalDateTime dateTime = LocalDateTime.now();
            String dateStr = dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            Msg msg = new Msg();
            msg.setMsgType(Msg.MsgType.SERVER_REPLAY_MSG.name());
            msg.setMessage(dateStr);
            TextWebSocketFrame textWebSocketFrame = new TextWebSocketFrame(JSONObject.toJSONString(msg));
            NettyGroupCache.group.writeAndFlush(textWebSocketFrame);
        }
    }

}
