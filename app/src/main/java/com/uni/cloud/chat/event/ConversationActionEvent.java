package com.uni.cloud.chat.event;

import com.uni.cloud.chat.event.base.BaseEvent;

/**
 * 作者：叶应是叶
 * 时间：2018/10/1 22:35
 * 描述：
 */
public class ConversationActionEvent extends BaseEvent {

    public static final int UPDATE_FRIENDSHIP_MESSAGE = 10;

    public ConversationActionEvent(int action) {
        super(action);
    }

}
