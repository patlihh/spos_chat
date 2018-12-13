/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uni.cloud.chat.speech;

import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.google.auth.Credentials;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.protobuf.ByteString;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.util.ResourceUtil;

import com.uni.cloud.chat.ChatApplication;
import com.uni.cloud.chat.R;
import com.uni.cloud.chat.audio.AudioDataUtil;
import com.uni.cloud.chat.misc.ByteUtil;
import com.uni.cloud.chat.misc.Option;
import com.uni.cloud.chat.xunfei_tts.TtsDemo;
import com.uni.cloud.chat.xunfei_tts.TtsSettings;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.ClosedChannelException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.internal.DnsNameResolverProvider;
import io.grpc.okhttp.OkHttpChannelBuilder;
import io.grpc.okhttp.OkHttpChannelProvider;
import io.grpc.stub.StreamObserver;
import io.grpc.uni.spos.ChatGrpc;
import io.grpc.uni.spos.ClientHeartReq;
import io.grpc.uni.spos.ClientHeartRes;
import io.grpc.uni.spos.ClientLoginReq;
import io.grpc.uni.spos.ClientLoginRes;
import io.grpc.uni.spos.ClientLogoutReq;
import io.grpc.uni.spos.ClientLogoutRes;
import io.grpc.uni.spos.RecvMsgReq;
import io.grpc.uni.spos.RecvMsgRes;
import io.grpc.uni.spos.SendMsgReq;
import io.grpc.uni.spos.SendMsgRes;

public class SpeechService extends Service {

    public interface HeartListener {
        void onHeart(boolean ret);
    }

    public interface LoginListener {
        void onLogin(boolean ret);
    }

    public interface RecMsgListener {
        void onRecMsg(String voice_id);
    }

    public interface SendMsgListener {
        void onSendMsg(int res, String msg);
    }

//    public interface Listener {
//        /**
//         * Called when a new piece of text was recognized by the Speech API.
//         *
//         * @param text    The text.
//         * @param isFinal {@code true} when the API finished processing audio.
//         */
//        void onSpeechRecognized(String text, boolean isFinal);
//
//        void onTransText(String text);
//
//        void onTtsAudioData(byte[] audio_data);
//
//    }

    private static final String TAG = "SpeechService";

    private ChatApplication mApp;

    private static final String PREFS = "SpeechService";
    private static final String PREF_ACCESS_TOKEN_VALUE = "access_token_value";
    private static final String PREF_ACCESS_TOKEN_EXPIRATION_TIME = "access_token_expiration_time";

    private static Timer mHeartTimer = null;
    public TimerTask tHeartTask;

  //  static { Security.insertProviderAt(Conscrypt.newProvider("GmsCore_OpenSSL"), 1); }

    /**
     * We reuse an access token if its expiration time is longer than this.
     */
    private static final int ACCESS_TOKEN_EXPIRATION_TOLERANCE = 30 * 60 * 1000; // thirty minutes
    /**
     * We refresh the current access token before it expires.
     */
    private static final int ACCESS_TOKEN_FETCH_MARGIN = 60 * 1000; // one minute

    public static final List<String> SCOPE =
            Collections.singletonList("https://www.googleapis.com/auth/cloud-platform");

    private final SpeechBinder mBinder = new SpeechBinder();
 //   private final ArrayList<Listener> mListeners = new ArrayList<>();
    private volatile AccessTokenTask mAccessTokenTask;
    private ChatGrpc.ChatStub mApi;

   private static Handler mRecvMsgErrorHandler;

    private LoginListener mLoginListener;

    private HeartListener mHeartListener;

    private RecMsgListener mRecvMsgListener;

    private SendMsgListener mSendMsgListener;

    private String voice_id;

    private boolean send_isOk = true;

    private boolean recv_isOk = true;

    private boolean login_ret = false;

    private boolean heart_ret = false;
    private int heart_fail_num = 0;
    // 语音合成对象
    private SpeechSynthesizer mTts;


    // 默认本地发音人
    public static String voicerLocal = "xiaoyan";

    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;

    //缓冲进度
    private int mPercentForBuffering = 0;
    //播放进度
    private int mPercentForPlaying = 0;

    private SharedPreferences mSharedPreferences;

    private String frameId;
    private int frameNum;
    private int frameStatus = 2;    //0 ---start, 1----Continue, 2-----end

    private byte[] recv_buf;

    //private static final boolean stt_trans_tts_flag = true;

//    private final StreamObserver<StreamingRecognizeResponse> mResponseObserver
//            = new StreamObserver<StreamingRecognizeResponse>() {
//        @Override
//        public void onNext(StreamingRecognizeResponse response) {
//            String text = null;
//            boolean isFinal = false;
//            if (response.getResultsCount() > 0) {
//                final StreamingRecognitionResult result = response.getResults(0);
//                isFinal = result.getIsFinal();
//                if (result.getAlternativesCount() > 0) {
//                    final SpeechRecognitionAlternative alternative = result.getAlternatives(0);
//                    text = alternative.getTranscript();
//                }
//            }
//            if (text != null) {
//                for (Listener listener : mListeners) {
//                    listener.onSpeechRecognized(text, isFinal);
//                }
//            }
//        }

//        @Override
//        public void onError(Throwable t) {
//            Log.e(TAG, "Error calling the API.", t);
//        }
//
//        @Override
//        public void onCompleted() {
//            Log.i(TAG, "API0 completed.");
//        }
//
//    };

    private final StreamObserver<ClientLoginRes> mLoginResponseObserver
            = new StreamObserver<ClientLoginRes>() {
        @Override
        public void onNext(ClientLoginRes response) {
            Log.d(TAG, "login response.getRet=" + response.getRet() + ";res_str=" + response.getResStr()+";res_token="+ response.getResToken());
            boolean ret = false;
            if (response.getRet()) {
                mApp.mUsers.clear();
                try {
                    JSONArray jsonArray = new JSONArray(response.getResStr());
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String id = jsonObject.getString("id");
                        String name = jsonObject.getString("name");
                        boolean islogin = jsonObject.getBoolean("islogin");
                        boolean isonline = jsonObject.getBoolean("isonline");
                        if (!id.contentEquals(mApp.getOwer_id())) {
                            mApp.mUsers.add(new User(id, name, islogin,isonline));
                        } else {
                            mApp.setOwer_name(name);
                        }
                        Log.d(TAG, "id=" + id + ";name=" + name + ";islogin=" + islogin+ ";isonline=" + isonline);
                    }

                    Log.d(TAG, "mUsers size=" +mApp.mUsers.size());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    RecvMsgReq recvReq = RecvMsgReq.newBuilder().setSid(mApp.getOwer_id())
                            .setSmac(mApp.getMac())
                            .build();
                    mApi.recvMsgStream(recvReq, mRecvMsgResObserver);
                }catch (Exception e){
                    e.printStackTrace();
                }

                login_ret = true;
            } else {
                login_ret = false;
            }


        }

        @Override
        public void onError(Throwable t) {
            Log.e(TAG, "Error calling the login API. err=", t);
        }

        @Override
        public void onCompleted() {

            mLoginListener.onLogin(login_ret);
            startHeartTimer();
            Log.i(TAG, "login API completed.");
        }

    };

    private final StreamObserver<ClientLogoutRes> mLogoutResponseObserver
            = new StreamObserver<ClientLogoutRes>() {
        @Override
        public void onNext(ClientLogoutRes response) {
            Log.d(TAG, "logout response.getRet=" + response.getRet());
        }

        @Override
        public void onError(Throwable t) {
            Log.e(TAG, "Error calling the logout API. err=", t);
        }

        @Override
        public void onCompleted() {
            //            for (Listener listener : mListeners) {
//                listener.onLogin(ret);
//            }
//            mLoginListener.onLogin(login_ret);
            Log.i(TAG, "logout API completed.");
        }

    };

    private final Runnable mRecvMsgRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "mRecvMsgRunnable re run");
            try {
                RecvMsgReq recvReq = RecvMsgReq.newBuilder().setSid(mApp.getOwer_id())
                        .setSmac(mApp.getMac())
                        .build();
                mApi.recvMsgStream(recvReq, mRecvMsgResObserver);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    private final StreamObserver<RecvMsgRes> mRecvMsgResObserver
            = new StreamObserver<RecvMsgRes>() {
        @Override
        public void onNext(RecvMsgRes response) {
            Log.d(TAG, "recvMsgStream::response.getDid=" + response.getDid() + ";getSid=" + response.getSid() + ";sample_rate=" + response.getSampleRate());
            //        Log.d(TAG, "recvMsgStream::response.getMsg=" + response.getMsg());
//            Log.d(TAG, "recvMsgStream::response.getMtype=" + response.getMtype());
//            Log.d(TAG, "recvMsgStream::response.getMsgId=" + response.getMsgId());
//            Log.d(TAG, "recvMsgStream::response.getMsgNum=" + response.getMsgNum());
//            Log.d(TAG, "recvMsgStream::response.getMsgLen=" + response.getMsgLen());
//            Log.d(TAG, "recvMsgStream::response.getMsgStatus=" + response.getMsgStatus());

            if (response.getMtype() == RecvMsgRes.MsgType.VOICE) {

                voice_id = response.getSid();

                if ((recv_buf == null || response.getMsgNum() == 1) && response.getMsgLen() > 0) {
                    recv_buf = response.getMsg().toByteArray();
                } else {
                    if (recv_buf != null)
                        recv_buf = ByteUtil.append(recv_buf, response.getMsg().toByteArray());
                }

                if (recv_buf != null)
                    Log.d(TAG, "recvMsgStream::recv_buf.length=" + recv_buf.length);

                if (response.getMsgStatus() == 2) {

                    Log.d(TAG, "recvMsgStream::voice_id=" + voice_id);

                    if (mRecvMsgListener != null && voice_id != null && voice_id.length() > 0) mRecvMsgListener.onRecMsg(voice_id);

                    Log.d(TAG, "recvMsgStream::response play voice....");
// 获得音频缓冲区大小  
                    int bufferSize = android.media.AudioTrack.getMinBufferSize(response.getSampleRate(), AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                    Log.e(TAG, "播放缓冲区大小" + bufferSize);
// 获得音轨对象  STREAM_SYSTEM STREAM_MUSIC  STREAM_VOICE_CALL
                    AudioTrack player = new AudioTrack(AudioManager.STREAM_MUSIC, response.getSampleRate(), AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);

// 设置喇叭音量  
                    player.setStereoVolume(1.0f, 1.0f);

// 开始播放声音  
                    player.play();
                    if (recv_buf != null) {
                        if (Option.Speex_IsOpen) {

                            int raw_len = AudioDataUtil.spx2raw(recv_buf).length;
                            Log.d(TAG, "recvMsgStream::raw_len=" + raw_len);

                            player.write(AudioDataUtil.spx2raw(recv_buf), 0, raw_len);// 播放音频数据
                        } else {
                            //                      player.write(response.getMsg().toByteArray(), 0, response.getMsg().toByteArray().length);// 播放音频数据
                            player.write(recv_buf, 0, recv_buf.length);// 播放音频数据

                        }
                    }
                    player.stop();
                    player.release();
                    player = null;

                    recv_buf = null;
                }

            }
        }

        @Override
        public void onError(Throwable t) {
            Log.e(TAG, "Error calling the recvMsgStream API, err=", t);
//            System.exit(0);            // Schedule access token refresh before it expires
            if (mRecvMsgErrorHandler != null) {
                mRecvMsgErrorHandler.postDelayed(mRecvMsgRunnable, 5000);
            }

        }

        @Override
        public void onCompleted() {
            Log.i(TAG, "recvMsgStream API completed.");
        }

    };


    private final StreamObserver<SendMsgRes> mSendMsgResObserver
            = new StreamObserver<SendMsgRes>() {
        @Override
        public void onNext(SendMsgRes response) {
            Log.d(TAG, "SendMsg response Ret=" + response.getRet() + ";Msg=" + response.getMsg());
            if (mSendMsgListener != null)
                mSendMsgListener.onSendMsg(response.getRet(), response.getMsg());
            switch (response.getRet()) {
                case Option.SendMsgRes.SEND_MSG_OK:
                    break;
                case Option.SendMsgRes.SEND_MSG_USER_LOGOUT:
                    Login(mApp.getOwer_id(), mApp.getOwer_password());
                    Log.d(TAG, "NOT login, please login!");
                    send_isOk = false;
                    break;
                case Option.SendMsgRes.SEND_MSG_NO_DEST_USER:
                    Log.d(TAG, "no dest user!");
                    send_isOk = false;
                    break;
                case Option.SendMsgRes.SEND_MSG_DEST_USER_LOGOUT:
                    Log.d(TAG, "dest user not login!");
                    send_isOk = false;
                    break;
                default:
                    break;

            }

        }

        @Override
        public void onError(Throwable t) {
            Log.e(TAG, "Error calling the SendMsg API. err=", t);
        }

        @Override
        public void onCompleted() {
            Log.i(TAG, "SendMsg API completed.");
        }

    };

    private final StreamObserver<ClientHeartRes> mHeartResponseObserver
            = new StreamObserver<ClientHeartRes>() {
        @Override
        public void onNext(ClientHeartRes response) {
            Log.d(TAG, "heart response.getRet=" + response.getRet() + ";res_str=" + response.getMsg());
            boolean ret = false;
            if (response.getRet()) {
                try {
                    JSONArray jsonArray = new JSONArray(response.getMsg());
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                        String id = jsonObject.getString("id");
//                        String mac = jsonObject.getString("mac");
                        boolean islogin = jsonObject.getBoolean("islogin");
                        boolean isonline = jsonObject.getBoolean("isonline");
                        boolean isrecvok = jsonObject.getBoolean("isrecvok");

                        for (int index =0; index < mApp.mUsers.size(); index++){
                            if(id.equals(mApp.mUsers.get(index).id)){
                                mApp.mUsers.get(index).setLogin(islogin);
                                mApp.mUsers.get(index).setOnline(isonline);
                                mApp.mUsers.get(index).setRecvok(isrecvok);
                            }
                        }

                        if(id.equals(mApp.getOwer_id())&&!isrecvok)
                            recv_isOk = false;

                        Log.d(TAG, "id=" + id + ";islogin=" + islogin + ";isonline=" + isonline);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                heart_ret = true;
                heart_fail_num = 0;
            } else {
                heart_ret = false;
                heart_fail_num++;
            }


        }

        @Override
        public void onError(Throwable t) {
            Log.e(TAG, "Error calling the heart API. err=", t);
        }

        @Override
        public void onCompleted() {
            //            for (Listener listener : mListeners) {
//                listener.onLogin(ret);
//            }
            if(mHeartListener!=null) mHeartListener.onHeart(heart_ret);

            Log.i(TAG, "heart API completed.heart_ret="+ heart_ret+";heart_fail_num="+ heart_fail_num+";recv_isOk="+ recv_isOk);

            if(!heart_ret&&heart_fail_num>=Option.heart_fail_max_num) {
                Login(mApp.getOwer_id(), mApp.getOwer_password());
            }

            if(!recv_isOk){
                try {
                    RecvMsgReq recvReq = RecvMsgReq.newBuilder().setSid(mApp.getOwer_id())
                            .setSmac(mApp.getMac())
                            .build();
                    mApi.recvMsgStream(recvReq, mRecvMsgResObserver);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            recv_isOk = true;
        }

    };

//    private final StreamObserver<RecognizeResponse> mFileResponseObserver
//            = new StreamObserver<RecognizeResponse>() {
//        @Override
//        public void onNext(RecognizeResponse response) {
//            String text = null;
//            if (response.getResultsCount() > 0) {
//                final SpeechRecognitionResult result = response.getResults(0);
//                if (result.getAlternativesCount() > 0) {
//                    final SpeechRecognitionAlternative alternative = result.getAlternatives(0);
//                    text = alternative.getTranscript();
//                }
//            }
//            if (text != null) {
//                for (Listener listener : mListeners) {
//                    listener.onSpeechRecognized(text, true);
//                }
//            }
//        }

//        @Override
//        public void onError(Throwable t) {
//            Log.e(TAG, "Error calling the API.", t);
//        }
//
//        @Override
//        public void onCompleted() {
//            Log.i(TAG, "API completed.");
//        }
//
//    };


    private StreamObserver<SendMsgReq> mSendMsgReqObserver;

    public static SpeechService from(IBinder binder) {
        return ((SpeechBinder) binder).getService();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "SpeechService OnCreate...");
    //    Log.d(TAG, "mUsers size=" + mUsers.size());

        mApp = (ChatApplication) getApplication();
        mRecvMsgErrorHandler = new Handler();
        fetchAccessToken();

        //patli 20180703 for  xunfei
        if (Option.FlyTek_IsOpen) {
            StringBuffer param = new StringBuffer();
            param.append("appid=" + getString(R.string.app_id));
            param.append(",");
            // 设置使用v5+
            param.append(SpeechConstant.ENGINE_MODE + "=" + SpeechConstant.MODE_MSC);
            SpeechUtility.createUtility(SpeechService.this, param.toString());

            // 初始化合成对象
            mTts = SpeechSynthesizer.createSynthesizer(this, mTtsInitListener);

//            // 云端发音人名称列表
//            cloudVoicersEntries = getResources().getStringArray(R.array.voicer_cloud_entries);
//            cloudVoicersValue = getResources().getStringArray(R.array.voicer_cloud_values);
//
//            // 本地发音人名称列表
//            localVoicersEntries = getResources().getStringArray(R.array.voicer_local_entries);
//            localVoicersValue = getResources().getStringArray(R.array.voicer_local_values);

            mSharedPreferences = getSharedPreferences(TtsSettings.PREFER_NAME, Activity.MODE_PRIVATE);
        }

        registerBoradcastReceiver();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "SpeechService onDestroy...");

        mRecvMsgErrorHandler.removeCallbacks(mRecvMsgRunnable);
        mRecvMsgErrorHandler = null;
        // Release the gRPC channel.

        if (mApi != null) {
            final ManagedChannel channel = (ManagedChannel) mApi.getChannel();
            if (channel != null && !channel.isShutdown()) {
                try {
                    channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Error shutting down the gRPC channel.", e);
                }
            }
            mApi = null;
        }

        unregisterReceiver(mBroadcastReceiver);
        if (Option.Speex_IsOpen) {
            AudioDataUtil.free();
        }
        mAccessTokenTask = null;

        stopHeartTimer();
    }


    private void fetchAccessToken() {
        if (mAccessTokenTask != null) {
            return;
        }
        mAccessTokenTask = new AccessTokenTask();
        mAccessTokenTask.execute();
    }

    private String getDefaultLanguageCode() {
        final Locale locale = Locale.getDefault();
        final StringBuilder language = new StringBuilder(locale.getLanguage());
        final String country = locale.getCountry();
        Log.d(TAG, "country=" + country);
        if (!TextUtils.isEmpty(country)) {
            language.append("-");
            language.append(country);
        }
        Log.d(TAG, "language.toString()=" + language.toString());
        return language.toString();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind.......");
        restartHeartTimer();
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand.......");

        return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind.......");
 //       stopHeartTimer();
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "onRebind.......");
        super.onRebind(intent);
    }



    public void setLoginListener(@NonNull LoginListener listener) {
        mLoginListener = listener;
    }

    public void setHeartListener(@NonNull HeartListener listener) {
        mHeartListener = listener;
    }

    public void setRecvMsgListener(@NonNull RecMsgListener listener) {
        mRecvMsgListener = listener;
    }

    public void setSendMsgListener(@NonNull SendMsgListener listener) {
        mSendMsgListener = listener;
    }

    public void RemoveHeartListener(@NonNull HeartListener listener) {
        listener = null;
    }

    public void RemoveRecvMsgListener(@NonNull RecMsgListener listener) {
        listener = null;
    }

    public void RemoveSendMsgListener(@NonNull SendMsgListener listener) {
        listener = null;
    }

    public void RemoveLoginListener(@NonNull LoginListener listener) {
        listener = null;
    }

//    public void addListener(@NonNull Listener listener) {
//        mListeners.add(listener);
//    }
//
//    public void removeListener(@NonNull Listener listener) {
//        mListeners.remove(listener);
//    }

    public boolean Login(String id, String password) {

        if (mApi == null) {
            Log.w(TAG, "API not ready. Ignoring the request login.");
            return false;
        }

        mApp.setOwer_id(id);
        mApp.setOwer_password(password);

        ClientLoginReq loginReq = ClientLoginReq.newBuilder().setSid(id)
                .setMac(mApp.getMac())
                .setPassword(password)
                .build();

        mApi.login(loginReq, mLoginResponseObserver);

        return true;
    }

    public boolean Logout(String id) {

        if (mApi == null) {
            Log.w(TAG, "API not ready. Ignoring the request logout.");
            return false;
        }

        ClientLogoutReq logoutReq = ClientLogoutReq.newBuilder().setSid(id)
                .setMac(mApp.getMac())
                .build();

        mApi.logout(logoutReq, mLogoutResponseObserver);

//        RecvMsgReq recvReq = RecvMsgReq.newBuilder().setSid(id)
//                .setName(name)
//                .setMac(mac)
//                .build();
//
//        mApi.recvMsgStream(recvReq, mRecvMsgResObserver);

        return true;
    }

    /**
     * Starts recognizing speech audio.
     *
     * @param sampleRate The sample rate of the audio.
     */
    public void startSendVoiceing(String dest_id, int sampleRate) {


        if (mApi == null) {
            Log.w(TAG, "API not ready. Ignoring the request.");
            return;
        }
        // Configure the API
        long t = System.currentTimeMillis();
        frameId = Long.toString(t);
        frameNum = 0;
        frameStatus = 0;

        Log.w(TAG, "startVoicing src_id=" + mApp.getOwer_id() + ";dest_id=" + dest_id + ";frameid=" + frameId + ";sampleRate=" + sampleRate);
        send_isOk = true;
        try {
            mSendMsgReqObserver = mApi.sendMsgStream(mSendMsgResObserver);
            mSendMsgReqObserver.onNext(SendMsgReq.newBuilder()
                    .setSid(mApp.getOwer_id())
                    .setSmac(mApp.getMac())
                    .setDid(dest_id)
                    .setMtype(SendMsgReq.MsgType.VOICE)
                    .setSampleRate(sampleRate)
                    .setMsgId(frameId)
                    .setMsgNum(frameNum)
                    .setMsgStatus(frameStatus)
                    .build());
        } catch (Exception e) {
            Log.w(TAG, "startVoicing exception!");
            e.printStackTrace();
        }
    }

    /**
     * Recognizes the speech audio. This method should be called every time a chunk of byte buffer
     * is ready.
     *
     * @param data The audio data.
     * @param size The number of elements that are actually relevant in the {@code data}.
     */
    public void SendVoiceing(String dest_id, int sampleRate, byte[] data, int size) {

        if (!send_isOk) {
            Log.w(TAG, "net failed, voiceing the audio stop!");

            return;
        }
        frameNum++;
        frameStatus = 1;

        Log.w(TAG, "voiceing the audio. frameid=" + frameId + ";frameNum=" + frameNum + ";frameStatus=" + frameStatus + ";size=" + size);
        if (mSendMsgReqObserver == null) {
            return;
        }
        // Call the streaming recognition API
        try {
            mSendMsgReqObserver.onNext(SendMsgReq.newBuilder()
                    .setDid(dest_id)
                    .setSid(mApp.getOwer_id())
                    .setSmac(mApp.getMac())
                    .setMtype(SendMsgReq.MsgType.VOICE)
                    .setSampleRate(sampleRate)
                    .setMsgId(frameId)
                    .setMsgNum(frameNum)
                    .setMsgStatus(frameStatus)
                    .setMsgLen(size)
                    .setMsg(ByteString.copyFrom(data, 0, size))
                    .build());
        } catch (Exception e) {
            Log.w(TAG, "voiceing the audio exception!");
            e.printStackTrace();
        }
    }

    /**
     * Finishes recognizing speech audio.
     */
    public void finishSendVoiceing(String dest_id, int sampleRate) {

        Log.w(TAG, "voice the audio end.");

        if (mSendMsgReqObserver == null) {
            return;
        }

        if (frameNum > 0 && frameStatus == 1) {
            // Call the streaming recognition API
            frameNum++;
            frameStatus = 2;
            mSendMsgReqObserver.onNext(SendMsgReq.newBuilder()
                    .setDid(dest_id)
                    .setSid(mApp.getOwer_id())
                    .setSmac(mApp.getMac())
                    .setMtype(SendMsgReq.MsgType.VOICE)
                    .setSampleRate(sampleRate)
                    .setMsgId(frameId)
                    .setMsgNum(frameNum)
                    .setMsgStatus(frameStatus)
                    .build());
        }


        mSendMsgReqObserver.onCompleted();
        mSendMsgReqObserver = null;


    }

    public void restartHeartTimer() {
        stopHeartTimer();
        startHeartTimer();
    }

    public void startHeartTimer() {

        if(!mApp.isLogin) return;

        if(!Option.Heart_IsOpen) return;

        Log.w(TAG, "startHeartTimer.");

        if (mHeartTimer == null) {
            mHeartTimer = new Timer();
        }

        tHeartTask = new TimerTask() {
            @Override
            public void run() {

                Log.w(TAG, "heart req TimerTask  run.");
                ClientHeartReq heartReq = ClientHeartReq.newBuilder().setSid(mApp.getOwer_id())
                        .setMac(mApp.getMac())
                        .build();

                mApi.heartMsg(heartReq, mHeartResponseObserver);

            }
        };

        mHeartTimer.schedule(tHeartTask, 10*1000, Option.heart_time * 1000);
    }

    /*
     * public void stopHeartTimer() { if(mTimer != null) {
     * log("stop heart timer"); mTimer.cancel(); } }
     */
    public void stopHeartTimer() {

        if(!Option.Heart_IsOpen) return;

        if (tHeartTask != null) {
            tHeartTask.cancel();
            tHeartTask = null;
        }

        if (mHeartTimer != null) {
            mHeartTimer.cancel();
            mHeartTimer.purge();
            mHeartTimer = null;
        }

    }

    /**
     * Recognize all data from the specified {@link InputStream}.
     * <p>
     * //     * @param stream The audio data.
     */
//    public void recognizeInputStream(String lang, InputStream stream) {
//        try {
//            if (mApi == null) {
//                Log.w(TAG, "API not ready. Ignoring the request.");
//                return;
//            }
//            mApi.recognize(
//                    RecognizeRequest.newBuilder()
//                            .setConfig(RecognitionConfig.newBuilder()
//                                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
//                                    .setLanguageCode(lang)
//                                    .setSampleRateHertz(16000)
//                                    .build())
//                            .setAudio(RecognitionAudio.newBuilder()
//                                    .setContent(ByteString.readFrom(stream))
//                                    .build())
//                            .build(),
//                    mFileResponseObserver);
//        } catch (IOException e) {
//            Log.e(TAG, "Error loading the input", e);
//        }
//    }

    private class SpeechBinder extends Binder {

        SpeechService getService() {
            return SpeechService.this;
        }

    }

    private final Runnable mFetchAccessTokenRunnable = new Runnable() {
        @Override
        public void run() {
            fetchAccessToken();
        }
    };

    private class AccessTokenTask extends AsyncTask<Void, Void, AccessToken> {

        @Override
        protected AccessToken doInBackground(Void... voids) {
            return null;
        }

        @Override
        protected void onPostExecute(AccessToken accessToken) {
//            mAccessTokenTask = null;
            Log.d(TAG, "AccessTokenTask host=" + Option.host + ";port=" + Option.port);

            ManagedChannel channel;

            if (Option.Ssl_IsOpen) {
                try {
                    // Loading CAs from an InputStream
                    CertificateFactory cf;
                    cf = CertificateFactory.getInstance("X.509");

                    final X509Certificate server_ca;
                    InputStream cert = getResources().openRawResource(R.raw.gubstech);
                    server_ca = (X509Certificate) cf.generateCertificate(cert);

                    // Creating a KeyStore containing our trusted CAs
                    String keyStoreType = KeyStore.getDefaultType();
                    KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                    keyStore.load(null, null);
                    keyStore.setCertificateEntry("ca-gubstech", server_ca);

                    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory
                            .getDefaultAlgorithm());
                    trustManagerFactory.init(keyStore);

                    TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

                    if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                        throw new IllegalStateException("Unexpected default trust managers:"
                                + Arrays.toString(trustManagers));
                    }


                    // SSLContext sslContext = SSLContext.getInstance("TLS");
     //               TLSSocketFactory sslContext = new TLSSocketFactory();
        //            sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());

                    // 使用 X509TrustManager 初始化 SSLContext
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(null, new TrustManager[]{trustManagers[0]}, null);
                    SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

                    if(Option.IsK8s_test){
                        Log.d(TAG, "AccessTokenTask ssl k8s test host=" + Option.k8s_test_host +";port="+Option.k8s_test_port);
                        channel = OkHttpChannelBuilder.forAddress(Option.k8s_test_host, Option.k8s_test_port)
                                .overrideAuthority(Option.dns_name)
                                .sslSocketFactory(sslSocketFactory)
                                .build();
//                       channel = ZnzGrpcChannelBuilder.build(Option.k8s_test_host, Option.k8s_test_port, Option.dns_name, true, cert, null);
                    }else {
                        Log.d(TAG, "AccessTokenTask ssl host=" + Option.host + ";port=" + Option.ssl_port);
                        channel = OkHttpChannelBuilder.forAddress(Option.host, Option.ssl_port)
                                .overrideAuthority(Option.dns_name)
//                                .sslSocketFactory(sslContext.getSocketFactory())
                                .sslSocketFactory(sslSocketFactory)
                                .build();
                    }

                    mApi = ChatGrpc.newStub(channel);

                    cert.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {

                if(Option.IsK8s_test){
                    Log.d(TAG, "AccessTokenTask k8s test host=" + Option.k8s_test_host +";port="+Option.k8s_test_port);
                    channel = ManagedChannelBuilder.forAddress(Option.k8s_test_host, Option.k8s_test_port).usePlaintext(true).build();
                    mApi = ChatGrpc.newStub(channel);
                }else {

                    if (Option.test_IsOpen) {

                        channel = ManagedChannelBuilder.forAddress(Option.test_host, Option.test_port).usePlaintext(true).build();
                        mApi = ChatGrpc.newStub(channel);

                        //                Login("U1536825106", "patli", "11.22.33.44.55.66");
                        //            Login(Option.src_id, Option.src_password,"patli", "11.22.33.44.55.66");

//                    startVoiceing(Option.src_id, "0000", 16000);
                    } else {
                        channel = ManagedChannelBuilder.forAddress(Option.host, Option.port).usePlaintext(true).build();
                        mApi = ChatGrpc.newStub(channel);
                    }
                }

            }

        }
    }

    public void registerBoradcastReceiver() {
        // 注册广播
        IntentFilter myIntentFilter = new IntentFilter();

        myIntentFilter.addAction("com.ut.pos.tts");

        registerReceiver(mBroadcastReceiver, myIntentFilter);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Log.d(TAG, "onReceive action=" + intent.getAction());

            String action = intent.getAction();
            if (action.equals("com.ut.pos.tts")) {

                if (Option.FlyTek_IsOpen) {
                    String text = intent.getStringExtra("com.ut.pos.tts.str");
                    String lang = intent.getStringExtra("com.ut.pos.tts.lang");
                    Log.d(TAG, "com.ut.pos.tts.str=" + text + ";lang=" + lang);
                    // 设置参数
                    if (lang.equals("pt")) {
                        setParam("xiaoyan");
                    } else if (lang.equals("gd")) {
                        setParam("xiaomei");
                    } else {
                        setParam("xiaoyan");
                    }

                    int code = mTts.startSpeaking(text, mTtsListener);
//			/**
//			 * 只保存音频不进行播放接口,调用此接口请注释startSpeaking接口
//			 * text:要合成的文本，uri:需要保存的音频全路径，listener:回调接口
//			*/
//			String path = Environment.getExternalStorageDirectory()+"/tts.pcm";
//			int code = mTts.synthesizeToUri(text, path, mTtsListener);

                    MainActivity.getMainActivity().CleanmText();  //patli 20180709

                    if (code != ErrorCode.SUCCESS) {
                        Log.d(TAG, "语音合成失败,错误码: " + code);
                    }
                }

            }
        }
    };

    /**
     * 参数设置
     *
     * @param
     * @return
     */
    private void setParam(String lang_voice) {

        Log.d(TAG, "setParam lang_voice=" + lang_voice);

        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        //设置合成
        if (mEngineType.equals(SpeechConstant.TYPE_CLOUD)) {
            //设置使用云端引擎
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
            //设置发音人
//            mTts.setParameter(SpeechConstant.VOICE_NAME,voicerCloud);
            mTts.setParameter(SpeechConstant.VOICE_NAME, lang_voice);
        } else {
            //设置使用本地引擎
            mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
            //设置发音人资源路径
            mTts.setParameter(ResourceUtil.TTS_RES_PATH, getResourcePath());
            //设置发音人
            mTts.setParameter(SpeechConstant.VOICE_NAME, voicerLocal);
        }
        //设置合成语速
        mTts.setParameter(SpeechConstant.SPEED, mSharedPreferences.getString("speed_preference", "50"));
        //设置合成音调
        mTts.setParameter(SpeechConstant.PITCH, mSharedPreferences.getString("pitch_preference", "50"));
        //设置合成音量
        mTts.setParameter(SpeechConstant.VOLUME, mSharedPreferences.getString("volume_preference", "50"));
        //设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE, mSharedPreferences.getString("stream_preference", "3"));

        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/tts.wav");
    }

    //获取发音人资源路径
    private String getResourcePath() {
        StringBuffer tempBuffer = new StringBuffer();
        //合成通用资源
        tempBuffer.append(ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "tts/common.jet"));
        tempBuffer.append(";");
        //发音人资源
        tempBuffer.append(ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "tts/" + TtsDemo.voicerLocal + ".jet"));
        return tempBuffer.toString();
    }

    /**
     * 初始化监听。
     */
    private InitListener mTtsInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            Log.d(TAG, "InitListener init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                Log.d(TAG, "初始化失败,错误码：" + code);
            } else {
                // 初始化成功，之后可以调用startSpeaking方法
                // 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
                // 正确的做法是将onCreate中的startSpeaking调用移至这里
            }
        }
    };

    /**
     * 合成回调监听。
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
            Log.d(TAG, "开始播放");
        }

        @Override
        public void onSpeakPaused() {
            Log.d(TAG, "暂停播放");
        }

        @Override
        public void onSpeakResumed() {
            Log.d(TAG, "继续播放");
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos,
                                     String info) {
            // 合成进度
            mPercentForBuffering = percent;
            Log.d(TAG, String.format(getString(R.string.tts_toast_format),
                    mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
            // 播放进度
            mPercentForPlaying = percent;
            Log.d(TAG, String.format(getString(R.string.tts_toast_format),
                    mPercentForBuffering, mPercentForPlaying));
        }

        @Override
        public void onCompleted(SpeechError error) {
            if (error == null) {
                Log.d(TAG, "播放完成");
            } else if (error != null) {
                Log.d(TAG, error.getPlainDescription(true));
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }
    };

}
