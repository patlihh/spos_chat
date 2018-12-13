package com.uni.cloud.chat.event;

import com.uni.cloud.chat.event.base.BaseEvent;

/**
 * 作者：叶应是叶
 * 时间：2018/10/1 21:42
 * 描述：
 */
public class ChatActionEvent extends BaseEvent {

    public static final int SEND_MESSAGE_FAIL = 10;

    public static final int CLEAN_MESSAGE = 20;

    public ChatActionEvent(int action) {
        super(action);
    }

}
