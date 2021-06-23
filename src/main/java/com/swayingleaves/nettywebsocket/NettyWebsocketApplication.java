package com.swayingleaves.nettywebsocket;

import com.swayingleaves.nettywebsocket.service.WsServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author zhenglin
 */
@SpringBootApplication
@EnableScheduling
public class NettyWebsocketApplication {

    public static void main(String[] args) {
        SpringApplication.run(NettyWebsocketApplication.class, args);
        //因为netty开启服务器时会阻塞，需要在spring启动后加载
        WsServer wsServer = new WsServer();
        wsServer.startNetty();
    }

}
