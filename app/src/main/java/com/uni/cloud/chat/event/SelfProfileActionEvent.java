package com.uni.cloud.chat.event;

import com.uni.cloud.chat.event.base.BaseCallbackEvent;

/**
 * 作者：叶应是叶
 * 时间：2018/10/1 10:00
 * 描述：
 */
public class SelfProfileActionEvent extends BaseCallbackEvent {

    public static final int LOGOUT_SUCCESS = 10;

    public SelfProfileActionEvent(int action) {
        super(action);
    }

}