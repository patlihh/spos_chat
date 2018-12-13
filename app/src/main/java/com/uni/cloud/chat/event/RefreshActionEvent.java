package com.uni.cloud.chat.event;

import com.uni.cloud.chat.event.base.BaseEvent;

/**
 * 作者：叶应是叶
 * 时间：2018/10/1 16:57
 * 描述：
 */
public class RefreshActionEvent extends BaseEvent {

    public static final int REFRESH = 10;

    public RefreshActionEvent(int action) {
        super(action);
    }

}