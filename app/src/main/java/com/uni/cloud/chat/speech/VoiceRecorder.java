/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uni.cloud.chat.speech;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.annotation.NonNull;
import android.util.Log;

import com.uni.cloud.chat.misc.Option;

import static com.uni.cloud.chat.misc.Option.Opus.FRAME_SIZE;
import static com.uni.cloud.chat.misc.Option.Opus.NUM_CHANNELS;
import static com.uni.cloud.chat.misc.Option.Opus.SAMPLE_RATE;


/**
 * Continuously records audio and notifies the {@link VoiceRecorder.Callback} when voice (or any
 * sound) is heard.
 * <p>
 * <p>The recorded audio format is always {@link AudioFormat#ENCODING_PCM_16BIT} and
 * {@link AudioFormat#CHANNEL_IN_MONO}. This class will automatically pick the right sample rate
 * for the device. Use {@link #getSampleRate()} to get the selected value.</p>
 */
public class VoiceRecorder {

    //  private static final int[] SAMPLE_RATE_CANDIDATES = new int[]{16000, 11025, 22050, 44100};
    // private static final int[] SAMPLE_RATE_CANDIDATES = new int[]{16000};
    private static final int[] SAMPLE_RATE_CANDIDATES = new int[]{8000};
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private static final int AMPLITUDE_THRESHOLD = 1500;
    private static final int SPEECH_TIMEOUT_MILLIS = 2000;
    private static final int MAX_SPEECH_LENGTH_MILLIS = 30 * 1000;

    public static abstract class Callback {
        /**
         * Called when the recorder starts hearing voice.
         */
        public void onVoiceStart() {
        }

        /**
         * Called when the recorder is hearing voice.
         *
         * @param data The audio data in {@link AudioFormat#ENCODING_PCM_16BIT}.
         * @param size The size of the actual data in {@code data}.
         */
        public void onVoice(byte[] data, int size) {
        }

        public void onVoice(short[] data, int size) {
        }

        /**
         * Called when the recorder stops hearing voice.
         */
        public void onVoiceEnd() {
        }
    }

    private final Callback mCallback;

    private AudioRecord mAudioRecord;

    private Thread mThread;

 //   private byte[] mBuffer;

    private short[] smBuffer;

    private byte[] inBuf = new byte[FRAME_SIZE * NUM_CHANNELS * 2];

    private final Object mLock = new Object();

    /**
     * The timestamp of the last time that voice is heard.
     */
    private long mLastVoiceHeardMillis = Long.MAX_VALUE;

    /**
     * The timestamp when the current voice is started.
     */
    private long mVoiceStartedMillis;

    public VoiceRecorder(@NonNull Callback callback) {
        mCallback = callback;
    }

    /**
     * Starts recording audio.
     * <p>
     * <p>The caller is responsible for calling {@link #stop()} later.</p>
     */
    public void start() {
        // Stop recording if it is currently ongoing.
        stop();
        // Try to create a new recording session.
        mAudioRecord = createAudioRecord();
        if (mAudioRecord == null) {
            throw new RuntimeException("Cannot instantiate VoiceRecorder");
        }
        // Start recording.
        //patli 20180608    mAudioRecord.startRecording();
        // Start processing the captured audio.
        mThread = new Thread(new ProcessVoice());
        mThread.start();
    }

    public void starting() {
        if (mAudioRecord != null) {
            mAudioRecord.startRecording();
            Log.d("test", "starting ---> ok");
        }
    }

    /**
     * Stops recording audio.
     */
    public void stop() {
        synchronized (mLock) {
            dismiss();
            if (mThread != null) {
                mThread.interrupt();
                mThread = null;
            }
            if (mAudioRecord != null) {
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;
            }
   //         inBuf = null;
        }
    }

    public void stoping() {
        if (mAudioRecord != null) {
            mCallback.onVoiceEnd();
            mAudioRecord.stop();
            Log.d("test", "stoping ---> ok");
        }
    }

    /**
     * Dismisses the currently ongoing utterance.
     */
    public void dismiss() {
        if (mLastVoiceHeardMillis != Long.MAX_VALUE) {
            mLastVoiceHeardMillis = Long.MAX_VALUE;
            mCallback.onVoiceEnd();
        }
    }

    /**
     * Retrieves the sample rate currently used to record audio.
     *
     * @return The sample rate of recorded audio.
     */
    public int getSampleRate() {
        if (mAudioRecord != null) {
            return mAudioRecord.getSampleRate();
        }
        return 0;
    }

    /**
     * Creates a new {@link AudioRecord}.
     *
     * @return A newly created {@link AudioRecord}, or null if it cannot be created (missing
     * permissions?).
     */
    private AudioRecord createAudioRecord() {

        int minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                NUM_CHANNELS == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);

        // initialize audio recorder
//        AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
//                SAMPLE_RATE,
//                NUM_CHANNELS == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO,
//                AudioFormat.ENCODING_PCM_16BIT,
//                minBufSize);

        AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                NUM_CHANNELS == 1 ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBufSize);

        if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
   //         mBuffer = new byte[minBufSize];
            smBuffer = new short[minBufSize];
            return recorder;
        } else {
            recorder.release();
        }
//
//        for (int sampleRate : SAMPLE_RATE_CANDIDATES) {
//            final int sizeInBytes = AudioRecord.getMinBufferSize(sampleRate, CHANNEL, ENCODING);
//            Log.d("test", "createAudioRecord sampleRate=" + sampleRate + ";sizeInBytes=" + sizeInBytes);
//            if (sizeInBytes == AudioRecord.ERROR_BAD_VALUE) {
//                continue;
//            }
//            final AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
//                    sampleRate, CHANNEL, ENCODING, sizeInBytes);
//
//            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
//                mBuffer = new byte[sizeInBytes];
//                smBuffer = new short[sizeInBytes];
//                return audioRecord;
//            } else {
//                audioRecord.release();
//            }
//        }
        return null;
    }

    /**
     * Continuously processes the captured audio and notifies {@link #mCallback} of corresponding
     * events.
     */
    private class ProcessVoice implements Runnable {

        @Override
        public void run() {
            while (true) {
                synchronized (mLock) {
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    if (Option.Speex_IsOpen) {

                        final int ssize = mAudioRecord.read(smBuffer, 0, smBuffer.length);

                        final long now = System.currentTimeMillis();
                        if (isHearingVoice(smBuffer, ssize)) {
                            if (mLastVoiceHeardMillis == Long.MAX_VALUE) {
                                mVoiceStartedMillis = now;
                                mCallback.onVoiceStart();
                            }
                            mCallback.onVoice(smBuffer, ssize);
                            mLastVoiceHeardMillis = now;
                            if (now - mVoiceStartedMillis > MAX_SPEECH_LENGTH_MILLIS) {
                                end();
                            }
                        } else if (mLastVoiceHeardMillis != Long.MAX_VALUE) {
                            mCallback.onVoice(smBuffer, ssize);
                            if (now - mLastVoiceHeardMillis > SPEECH_TIMEOUT_MILLIS) {
                                end();
                            }
                        }

                    } else {
                        final int size = mAudioRecord.read(inBuf, 0, inBuf.length);

                        final long now = System.currentTimeMillis();
                        if (isHearingVoice(inBuf, size)) {
                            if (mLastVoiceHeardMillis == Long.MAX_VALUE) {
                                mVoiceStartedMillis = now;
                                mCallback.onVoiceStart();
                            }
                            mCallback.onVoice(inBuf, size);
                            mLastVoiceHeardMillis = now;
                            if (now - mVoiceStartedMillis > MAX_SPEECH_LENGTH_MILLIS) {
                                end();
                            }
                        } else if (mLastVoiceHeardMillis != Long.MAX_VALUE) {
                            mCallback.onVoice(inBuf, size);
                            if (now - mLastVoiceHeardMillis > SPEECH_TIMEOUT_MILLIS) {
                                end();
                            }
                        }
                    }
                }
            }
        }

        private void end() {
            mLastVoiceHeardMillis = Long.MAX_VALUE;
            mCallback.onVoiceEnd();
        }

        private boolean isHearingVoice(byte[] buffer, int size) {
            for (int i = 0; i < size - 1; i += 2) {
                // The buffer has LINEAR16 in little endian.
                int s = buffer[i + 1];
                if (s < 0) s *= -1;
                s <<= 8;
                s += Math.abs(buffer[i]);
                if (s > AMPLITUDE_THRESHOLD) {
                    return true;
                }
            }
            return false;
        }

        private boolean isHearingVoice(short[] buffer, int size) {
            for (int i = 0; i < size - 1; i += 2) {
                // The buffer has LINEAR16 in little endian.
                int s = buffer[i + 1];
                if (s < 0) s *= -1;
                s <<= 16;
                s += Math.abs(buffer[i]);
                if (s > AMPLITUDE_THRESHOLD) {
                    return true;
                }
            }
            return false;
        }

    }
}
