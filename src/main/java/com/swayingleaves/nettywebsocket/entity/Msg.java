package com.swayingleaves.nettywebsocket.entity;

import lombok.Data;

/**
 * @author zhenglin
 * @date 2021/6/23
 */
@Data
public class Msg {
    private String toUid;
    private String token;
    private String message;
    private String msgType;

    public enum MsgType{
        USER_MSG, TO_SERVER_MSG,SERVER_REPLAY_MSG
    }
}
