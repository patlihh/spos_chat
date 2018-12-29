package com.uni.cloud.chat.audio;

import android.support.annotation.IntRange;

public class Opus {

    private native int open( int compression);
    private native int getFrameSize();
    private native int encode(short[] in, int offset, byte[] out, int size);
    private native int decode(byte[] encoded, short[] lin, int size);
    private native void close();

    static {
        System.loadLibrary("newopus");
    }


}
