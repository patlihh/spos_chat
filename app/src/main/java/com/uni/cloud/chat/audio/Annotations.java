package com.uni.cloud.chat.audio;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class Annotations {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({8000, 12000, 16000, 24000, 48000})
    public @interface SamplingRate {}

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({1, 2})
    public @interface NumberOfChannels {}
}
