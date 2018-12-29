package com.uni.cloud.chat.audio;

public class OpusDecoder {
    /* Native pointer to OpusDecoder */
    private long address;

    private native int nativeInitDecoder(int samplingRate, int numberOfChannels);
    private native int nativeDecodeShorts(byte[] in, short[] out, int frames);
    private native int nativeDecodeBytes(byte[] in, byte[] out, int frames);
    private native boolean nativeReleaseDecoder();

    static {
        System.loadLibrary("newopus");
    }

    public void init(@Annotations.SamplingRate int sampleRate,
                     @Annotations.NumberOfChannels int channels) {
        OpusError.throwIfError(this.nativeInitDecoder(sampleRate, channels));
    }

    public int decode(byte[] encodedBuffer, short[] buffer, int frames) {
        return OpusError.throwIfError(this.nativeDecodeShorts(encodedBuffer, buffer, frames));
    }

    public int decode(byte[] encodedBuffer, byte[] buffer, int frames) {
        return OpusError.throwIfError(this.nativeDecodeBytes(encodedBuffer, buffer, frames));
    }

    public void close() {
        this.nativeReleaseDecoder();
    }

    public long getAddress() {
        return address;
    }

    public void setAddress(long address) {
        this.address = address;
    }
}
