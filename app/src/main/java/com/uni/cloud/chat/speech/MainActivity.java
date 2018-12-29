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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Path;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.uni.cloud.chat.R;
import com.uni.cloud.chat.audio.AudioDataUtil;
import com.uni.cloud.chat.audio.OpusEncoder;
import com.uni.cloud.chat.log.LogcatHelper;
import com.uni.cloud.chat.log.Logger;

import com.uni.cloud.chat.misc.FileUtil;
import com.uni.cloud.chat.misc.LimitQueue;
import com.uni.cloud.chat.misc.Option;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.util.ArrayList;

import java.util.List;
import java.util.Map;

import java.util.concurrent.TimeUnit;


import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import static com.uni.cloud.chat.misc.Option.Opus.FRAME_SIZE;
import static com.uni.cloud.chat.misc.Option.Opus.NUM_CHANNELS;


public class MainActivity extends AppCompatActivity implements MessageDialogFragment.Listener, View.OnClickListener {

    private static final String FRAGMENT_MESSAGE_DIALOG = "message_dialog";

    private static final String STATE_RESULTS = "results";

    private static final String TAG = "MainActivity";

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;

    private SpeechService mSpeechService;

    private VoiceRecorder mVoiceRecorder;

    private String stt_source_lang = "en-US";
    private String trans_source_lang = "en";
    private String trans_target_lang = "en";
    private String tts_target_lang = "en-US";
    private String tts_target_voice = "en-US-Wavenet-C";
    private String tts_target_gender = "FEMALE";

    private int speech_type = 0;   // 0-------client, 1-------server

    private String tts_string;

    private Spinner SourceSpinner;

    private Spinner TargetSpinner;

    private String[] stt_ln;
    private String[] trans_ln;
    private String[] tts_ln;
    private String[] tts_voice;
    private String[] tts_gender;

    private Boolean Is_haveVoice = false;

    //private Trans_Operation trans_lp;
    //  private TTS_Operation tts_lp;

    private ManagedChannel trans_channel;
    private ManagedChannel tts_channel;

    OpusEncoder encoder;
    //  byte[] inBuf = new byte[FRAME_SIZE * NUM_CHANNELS * 2];
    byte[] encBuf = new byte[1024];
    short[] outBuf = new short[FRAME_SIZE * NUM_CHANNELS];

    private final VoiceRecorder.Callback mVoiceCallback = new VoiceRecorder.Callback() {

        @Override
        public void onVoiceStart() {
            showStatus(true);
            if (mSpeechService != null) {
                Log.d("test", "onVoiceStart");
                mSpeechService.startSendVoiceing(Option.dest_id, mVoiceRecorder.getSampleRate());
                Is_haveVoice = true;

                if(Option.Opus.Is_Opus){
                    encoder = new OpusEncoder();
                    encoder.init(mVoiceRecorder.getSampleRate(), NUM_CHANNELS, OpusEncoder.OPUS_APPLICATION_VOIP);
                }
            }
        }

        @Override
        public void onVoice(byte[] data, int size) {
            if (mSpeechService != null && Is_haveVoice) {
                Log.d("test", "onVoice data.length=" + data.length + ";size=" + size);
                if(Option.Opus.Is_Opus) {
                    int encoded = encoder.encode(data, FRAME_SIZE, encBuf);
                    Log.v(TAG, "Encoded " + data.length + " bytes of audio into " + encoded + " bytes");
                    mSpeechService.SendVoiceing(Option.dest_id, mVoiceRecorder.getSampleRate(), encBuf, encoded);
                }else {
                    Log.d("test", "Not Speex onVoice");
                    mSpeechService.SendVoiceing(Option.dest_id, mVoiceRecorder.getSampleRate(), data, size);
                }

            }
        }

        @Override
        public void onVoice(short[] data, int size) {
            if (mSpeechService != null && Is_haveVoice) {
                Log.d("test", "onVoice data.length=" + data.length + ";size=" + size);

                int spx_len = AudioDataUtil.raw2spx(data).length;
                Log.d("test", "Speex onVoice spx_len=" + spx_len);
                if(Option.Opus.Is_Opus) {
                    int encoded = encoder.encode(data, FRAME_SIZE, encBuf);
                    Log.v(TAG, "Encoded " + data.length + " bytes of audio into " + encoded + " bytes");
                    mSpeechService.SendVoiceing(Option.dest_id, mVoiceRecorder.getSampleRate(), encBuf, encBuf.length);
                }else {
                    mSpeechService.SendVoiceing(Option.dest_id, mVoiceRecorder.getSampleRate(), AudioDataUtil.raw2spx(data), spx_len);
                }
            }
        }

        @Override
        public void onVoiceEnd() {
            Log.d("test", "onVoiceEnd");
            showStatus(false);
            Is_haveVoice = false;
            if (mSpeechService != null) {
                mSpeechService.finishSendVoiceing(Option.dest_id, mVoiceRecorder.getSampleRate());

                if(Option.Opus.Is_Opus) {
                    encoder.close();
                }
            }
        }
    };

    // Resource caches
    private int mColorHearing;
    private int mColorNotHearing;

    // View references
    private TextView mStatus;
    private TextView mText;
    private ResultAdapter mAdapter;
    private RecyclerView mRecyclerView;
    //   private List<ResultItem> resultitemList = new ArrayList<>();
    private List<ResultItem> itemList = new ArrayList<>();

    //    private FloatingActionButton mbut_speak;
    private Button mbut_client_speak;
    private Button mbut_server_speak;

    private Boolean Is_pressing = false;

    private Handler TranstextViewHandler, TTStextViewHandler;
    private String tr_str;

    private LimitQueue<AudioItem> TTS_limitQ = new LimitQueue<AudioItem>(3);


//    private List<RecyclerViewItem> itemList = new ArrayList<>();
//    private RecyclerView recyclerview;
//    private RecyclerViewAdapter mAdapter;

        private final ServiceConnection mServiceConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName componentName, IBinder binder) {
                mSpeechService = SpeechService.from(binder);
//                mSpeechService.addListener(mSpeechServiceListener);
                mStatus.setVisibility(View.VISIBLE);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                mSpeechService = null;
            }

        };

    public MainActivity() {
        mainActivity = this;
    }

    public static MainActivity getMainActivity() {
        return mainActivity;
    }


    private static MainActivity mainActivity;

    public void SetRecyclerViewItemServer(int pos, String server_str) {
        mAdapter.SetResultServer(0, "S: " + server_str);
    }

    public void SetRecyclerViewItemClient(int pos, String client_str) {
        mAdapter.SetResultClient(0, "C: " + client_str);
    }

    public void CleanmText() {
        if (mText != null) {
            mText.setText(null);
        }
    }

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};

    public static void verifyStoragePermissions(Activity activity) {

        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Resources resources = getResources();
        final Resources.Theme theme = getTheme();
        mColorHearing = ResourcesCompat.getColor(resources, R.color.status_hearing, theme);
        mColorNotHearing = ResourcesCompat.getColor(resources, R.color.status_not_hearing, theme);

//        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        mStatus = (TextView) findViewById(R.id.status);
        mText = (TextView) findViewById(R.id.text);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));


        mAdapter = new ResultAdapter(itemList);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(new ResultAdapter.OnItemClickListener() {
            @Override
            public void onClick(int position) {
                //         Toast.makeText(RecyclerViewTestActivity.this,"您点击了"+position+"行",Toast.LENGTH_SHORT).show();
//                Log.d("test", "您点击了" + position + "行");
                if (TTS_limitQ.size() > 0 && position < TTS_limitQ.getLimit()) {
//                    Log.d("test", "TTS limitQ size =" + TTS_limitQ.size() + "; TTS_limitQ.get(i)=" + TTS_limitQ.get(TTS_limitQ.size() - position % TTS_limitQ.size() - 1).getLn_code());
                    if (TTS_limitQ.get(TTS_limitQ.size() - position % TTS_limitQ.size() - 1).getLn_code().contains("普通话")
                            || TTS_limitQ.get(TTS_limitQ.size() - position % TTS_limitQ.size() - 1).getLn_code().contains("廣東話")) {
                        Xunfei_TTS(TTS_limitQ.get(TTS_limitQ.size() - position % TTS_limitQ.size() - 1).getLn_code(), TTS_limitQ.get(TTS_limitQ.size() - position % TTS_limitQ.size() - 1).getAudio_str());
                    } else
                        playMp3(TTS_limitQ.get(TTS_limitQ.size() - position % TTS_limitQ.size() - 1).getAudio_data());
                }
            }

            @Override
            public void onLongClick(int position) {
//                Toast.makeText(RecyclerViewTestActivity.this,"您长按点击了"+position+"行",Toast.LENGTH_SHORT).show();
//                Log.d("test", "您长按点击了" + position + "行");
            }
        });

//        mRecyclerView.findViewHolderForAdapterPosition(0).itemView
        mbut_client_speak = (Button) findViewById(R.id.but_speak_client);
        mbut_client_speak.setOnClickListener(this);
        mbut_client_speak.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                speech_type = 0;

                int c_pos = SourceSpinner.getSelectedItemPosition();
                int s_pos = TargetSpinner.getSelectedItemPosition();
//                Log.d("test", "c_pos" + c_pos + ";s_pos=" + s_pos);

                stt_source_lang = stt_ln[c_pos];
                trans_source_lang = trans_ln[c_pos];

                trans_target_lang = trans_ln[s_pos];
                tts_target_lang = tts_ln[s_pos];
                tts_target_voice = tts_voice[s_pos];
                tts_target_gender = tts_gender[s_pos];

                if (v.getId() == R.id.but_speak_client) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        Log.d("test", "cansal button ---> cancel");
                        if (Is_pressing) {
                            Is_pressing = false;
                            mVoiceRecorder.stoping();
                            mStatus.setText(R.string.press_speak);
                        }
                    }
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        Log.d("test", "cansal button ---> down");
                        if (trans_source_lang.equals(trans_target_lang)) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setTitle("Confirm");
                            builder.setMessage(R.string.input_diff_lang);
                            builder.setPositiveButton("Ok", null);
                            builder.show();
                        } else {
                            Is_pressing = true;
                            mVoiceRecorder.starting();
                            mStatus.setText(R.string.listening);
                        }
                    }
                }
                return false;
            }
        });

        mbut_server_speak = (Button) findViewById(R.id.but_speak_server);
        mbut_server_speak.setOnClickListener(this);


        //    trans_lp = new  Trans_Operation();
        //    tts_lp = new  TTS_Operation();

        TranstextViewHandler = new Handler();
        TTStextViewHandler = new Handler();

//        mbut_trans = (Button) findViewById(R.id.but_translation);
//        mbut_trans.setOnClickListener(this);

//        mbut_trans.setOnClickListener(new View.OnClickListener(){
//            @Override
//            public void onClick(View v) {
//                Log.d("test", "trans button ---> setOnClickListener");
//
//               Trans_Operation trans_lp = new  Trans_Operation();
//                Map<String, String> params=new HashMap<String, String>();
////               params.put("q", "Hello world");
//                params.put("q", "hello world");
//
//                params.put("target","de");
//                params.put("source","en");
//
//                trans_lp.execute(params);
//            }
//        });

        Resources res = getResources();
        stt_ln = res.getStringArray(R.array.stt_ln);
        trans_ln = res.getStringArray(R.array.trans_ln);
        tts_ln = res.getStringArray(R.array.tts_ln);
        tts_voice = res.getStringArray(R.array.tts_voice);
        tts_gender = res.getStringArray(R.array.tts_Gender);

        SourceSpinner = (Spinner) findViewById(R.id.spinner_source);
        SourceSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {//选择item的选择点击监听事件
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                String disp_text = "";
                // TODO Auto-generated method stub
                // 将所选mySpinner 的值带入myTextView 中
                Log.d("test", "您选择源语言：" + arg2 + "个");
                stt_source_lang = stt_ln[arg2];
                trans_source_lang = trans_ln[arg2];
                Log.d("test", "stt_source_lang=" + stt_source_lang + ";trans_source_lang=" + trans_source_lang);
            }

            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub
//                myTextView.setText("Nothing");
            }
        });

        TargetSpinner = (Spinner) findViewById(R.id.spinner_target);
        TargetSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {//选择item的选择点击监听事件
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                String disp_text = "";
                // TODO Auto-generated method stub
                // 将所选mySpinner 的值带入myTextView 中
                Log.d("test", "您选择目标语言：" + arg2 + "个");//文本说明
                trans_target_lang = trans_ln[arg2];
                tts_target_lang = tts_ln[arg2];
                tts_target_voice = tts_voice[arg2];
                tts_target_gender = tts_gender[arg2];

                Log.d("test", "trans_target_lang=" + trans_target_lang + ";tts_target_lang=" + tts_target_lang + ";tts_target_voice=" + tts_target_voice + ";tts_target_gender=" + tts_target_gender);
            }

            public void onNothingSelected(AdapterView<?> arg0) {
                // TODO Auto-generated method stub
//                myTextView.setText("Nothing");
            }
        });

        // Initializing an ArrayAdapter
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(
                this, R.layout.spinner_item, res.getStringArray(R.array.lang_spinner)
        );
        spinnerArrayAdapter.setDropDownViewResource(R.layout.spinner_item);
        SourceSpinner.setAdapter(spinnerArrayAdapter);
        TargetSpinner.setAdapter(spinnerArrayAdapter);

        verifyStoragePermissions(this);

        if (Option.Log_IsOpen) {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                File file = new File(Environment.getExternalStorageDirectory() + "/uspeech_log/");
                if (file.exists()) {
                    Log.d("test", "LogcatHelper start!");
                    LogcatHelper.getInstance(this).start();
                } else {
                    Log.d("test", "uspeech_log dir not exits!");
                }
            }
        }
    }


    @Override
    public void onAttachedToWindow() {

        super.onAttachedToWindow();

//        if (getResources().getBoolean(R.bool.is_tablet) && mOpenAsSmallWindow) {

        final View view = getWindow().getDecorView();

        final WindowManager.LayoutParams lp = (WindowManager.LayoutParams) view.getLayoutParams();

        lp.gravity = Gravity.CENTER;

        //       lp.width = 800;    //800;   //400;   //480;

        //      lp.height = 1000;    //1000;  //600;    //640;

        DisplayMetrics metrics = new DisplayMetrics();
        Display display = getWindowManager().getDefaultDisplay();
        display.getMetrics(metrics);

        Log.d("test", "widthPixels=" + metrics.widthPixels + ";heightPixels=" + metrics.heightPixels + ";xdpi=" + metrics.xdpi + ";ydpi=" + metrics.ydpi);

        if (metrics.widthPixels <= 480) {
            lp.width = 320;    //800;   //400;   //480;
            lp.height = 500;    //1000;  //1200;    //640;
            Log.d("test", "widthPixels------------------0");
        } else if (metrics.widthPixels > 480 && metrics.widthPixels <= 720) {
            lp.width = 480;    //800;   //400;   //480;
            lp.height = 640;    //1000;  //1200;    //640;
            Log.d("test", "widthPixels------------------1");
        } else {
            lp.width = 800;    //800;   //400;   //480;
            lp.height = 1000;    //1000;  //1200;    //640;
            Log.d("test", "widthPixels------------------2");
        }

        getWindowManager().updateViewLayout(view, lp);

//        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        // Prepare Cloud Speech API
        bindService(new Intent(this, SpeechService.class), mServiceConnection, BIND_AUTO_CREATE);

        // Start listening to voices
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            startVoiceRecorder();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.RECORD_AUDIO)) {
            showPermissionMessageDialog();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }

    @Override
    protected void onStop() {
        // Stop listening to voice
        stopVoiceRecorder();

        // Stop Cloud Speech API
//        mSpeechService.removeListener(mSpeechServiceListener);
        unbindService(mServiceConnection);
        mSpeechService = null;

        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
//        if (mAdapter != null) {
//            outState.putStringArrayList(STATE_RESULTS, mAdapter.getResults());
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (Option.Log_IsOpen)
            LogcatHelper.getInstance(this).stop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        Log.d("test", "onRequestPermissionsResult requestCode=" + requestCode);//文本说明

        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (permissions.length == 1 && grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecorder();
            } else {
                showPermissionMessageDialog();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

        if (Option.Log_IsOpen) {
            switch (requestCode) {
                case 1:
                    if (permissions.length == 1 && grantResults.length == 1
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        //创建文件夹
                        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                            File file = new File(Environment.getExternalStorageDirectory() + "/uspeech_log/");
                            if (!file.exists()) {
                                Log.d("test", "path1 create:" + file.mkdirs());
                            } else {
                                Log.d("test", "LogcatHelper start!");
                                LogcatHelper.getInstance(this).start();
                            }
                        }
                        break;
                    }
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_file:
//                mSpeechService.recognizeInputStream(getResources().openRawResource(R.raw.audio));

//                Log.d("test", "onOptionsItemSelected");
//                TTS_Operation tts_lp = new TTS_Operation();
//                Map<String, String> params = new HashMap<String, String>();
//                params.put("text", "Yes, you are right");
//
//                params.put("languageCode", "en-gb");
//                params.put("name", "en-GB-Standard-A");
//                params.put("ssmlGender", "FEMALE");
//                params.put("audioEncoding", "MP3");
//
//                tts_lp.execute(params);

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
        }
        mVoiceRecorder = new VoiceRecorder(mVoiceCallback);
        mVoiceRecorder.start();
    }

    private void stopVoiceRecorder() {
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
            mVoiceRecorder = null;
        }
    }

    private void showPermissionMessageDialog() {
        MessageDialogFragment
                .newInstance(getString(R.string.permission_message))
                .show(getSupportFragmentManager(), FRAGMENT_MESSAGE_DIALOG);
    }

    private void showStatus(final boolean hearingVoice) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mStatus.setTextColor(hearingVoice ? mColorHearing : mColorNotHearing);
            }
        });
    }

    @Override
    public void onClick(View view) {

        Log.d("test", "onClick  view.getId()=" + view.getId());
//        Log.d("test", "onClick  R.id.but_translation="+R.id.but_translation);
        switch (view.getId()) {
            case R.id.but_speak_client:
                break;
            case R.id.but_speak_server:
                mSpeechService.Login(Option.src_id, Option.src_password);
                break;
        }
    }

    @Override
    public void onMessageDialogDismissed() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_RECORD_AUDIO_PERMISSION);
    }

//    private final SpeechService.Listener mSpeechServiceListener =
//            new SpeechService.Listener() {
//                @Override
//                public void onSpeechRecognized(final String text, final boolean isFinal) {
//                    if (isFinal) {
//                        mVoiceRecorder.dismiss();
//                    }
//                    if (mText != null && !TextUtils.isEmpty(text)) {
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                if (isFinal) {
//                                    mText.setText(null);
//                                    String upperString = text.substring(0, 1).toUpperCase() + text.substring(1);   //patli20180706
//                                    Log.d("test", "text.substring(0,1).toUpperCase()=" + text.substring(0, 1).toUpperCase());
//
//                                    ResultItem item = new ResultItem();
//                                    if (speech_type == 0) {
//                                        item.setClientText("C: " + upperString);
//                                        item.setServerText("S: ");
//                                    } else {
//                                        item.setServerText("S: " + upperString);
//                                        item.setClientText("C: ");
//                                    }
//
//                                    mAdapter.addResult(item);
//                                    Log.d("test", "itemList.size=" + itemList.size());
//                                    mRecyclerView.smoothScrollToPosition(0);
//
//                                } else {
//                                    String upperString = text.substring(0, 1).toUpperCase() + text.substring(1);   //patli20180706
//                                    Log.d("test", "text.substring(0,1).toUpperCase()=" + text.substring(0, 1).toUpperCase());
//                                    mText.setText(upperString);
//                                }
//                            }
//                        });
//                    }
//                }
//
//                @Override
//                public void onTransText(final String trans_text) {
//                    tr_str = trans_text.substring(0, 1).toUpperCase() + trans_text.substring(1);   //patli20180706 首字符大写
//                    Log.d("test", "onTransText text.substring(0,1).toUpperCase()=" + trans_text.substring(0, 1).toUpperCase());
//                    Logger.i("test", " tr_str=" + tr_str);
//                    TranstextViewHandler.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            if (mText != null) {
//                                mText.setText(tr_str);
//                                if (trans_target_lang.equals("zh-CN")) {
//
//                                    Logger.i("test", " Xunfei_TTS!");
//                                    Xunfei_TTS(tts_target_lang, tr_str);
//                                    TTS_limitQ.offer(new AudioItem(tr_str, tts_target_lang));   //add queue
//                                    if (speech_type == 0) {
//                                        SetRecyclerViewItemServer(0, tr_str);
//                                    } else {
//                                        SetRecyclerViewItemClient(0, tr_str);
//                                    }
//
//                                } else {
//
//                                }
//                            }
//                        }
//                    });
//
//                    tts_string = tr_str;
//                }
//
//
//                @Override
//                public void onTtsAudioData(byte[] audio_data) {
//
//                    if (!trans_target_lang.equals("zh-CN")) {
//
//                        if (audio_data != null) {
//
//                            Log.d("test", "onTtsAudioData len=" + audio_data.length);
//
//                            TTS_limitQ.offer(new AudioItem(audio_data, tts_target_lang));   //add queue
//
//                            playMp3(audio_data);
//
//                            TranstextViewHandler.post(new Runnable() {
//                                @Override
//                                public void run() {
//                                    if (mText != null) {
//                                        mText.setText(null);
//
//                                        if (speech_type == 0) {
//                                            SetRecyclerViewItemServer(0, tts_string);
//                                        } else {
//                                            SetRecyclerViewItemClient(0, tts_string);
//                                        }
//                                    }
//                                }
//                            });
//                        }
//                    }
//                }
//            };

    private static class ViewHolder extends RecyclerView.ViewHolder {

        TextView Clienttext;
        TextView Servertext;

        ViewHolder(final View parent) {
//            super(inflater.inflate(R.layout.item_result, parent, false));
            super(parent);
            Clienttext = (TextView) parent.findViewById(R.id.account_name);
            Servertext = (TextView) parent.findViewById(R.id.account_id);

//            Servertext.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
////                    Toast.makeText(itemView.getContext(), "Position:" + Integer.toString(getPosition()), Toast.LENGTH_SHORT).show();
//                    Log.d("test", "Position=" + getPosition());
//                }
//            });
        }
    }

    public class ResultItem {
        private String ClientText;
        private String ServerText;

        public ResultItem() {

        }

        public ResultItem(String clientText, String serverText) {
            this.ClientText = clientText;
            this.ServerText = serverText;
        }

        public String getClientText() {
            return ClientText;
        }

        public void setClientText(String clientText) {
            this.ClientText = clientText;
        }

        public String getServerText() {
            return ServerText;
        }

        public void setServerText(String serverText) {
            this.ServerText = serverText;
        }
    }

    public class AudioItem {
        private String audio_str;
        private String ln_code;
        private byte[] audio_data;

        public AudioItem(String audio_str, String ln_code) {
            this.audio_str = audio_str;
            this.ln_code = ln_code;
        }

        public AudioItem(byte[] audio_data, String ln_code) {
            this.audio_data = audio_data;
            this.ln_code = ln_code;
        }

        public String getAudio_str() {
            return audio_str;
        }

        public void setAudio_str(String audio_str) {
            this.audio_str = audio_str;
        }

        public String getLn_code() {
            return ln_code;
        }

        public void setLn_code(String ln_code) {
            this.ln_code = ln_code;
        }

        public byte[] getAudio_data() {
            return audio_data;
        }

        public void setAudio_data(byte[] audio_d) {
            this.audio_data = audio_d;
        }
    }

    private static class ResultAdapter extends RecyclerView.Adapter<ViewHolder> {

        private OnItemClickListener mItemClickListener;

        public interface OnItemClickListener {
            void onClick(int position);

            void onLongClick(int position);
        }

        public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
            this.mItemClickListener = onItemClickListener;
        }

        private List<ResultItem> mResults;

        ResultAdapter(List<ResultItem> results) {
//            if (results != null) {
//                mResults.addAll(results);
//            }
            mResults = results;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
            return new ViewHolder(itemView);
//           return new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            holder.Clienttext.setText(mResults.get(position).getClientText());
            holder.Servertext.setText(mResults.get(position).getServerText());

            if (mItemClickListener != null) {
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mItemClickListener.onClick(position);
                    }
                });
                holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        mItemClickListener.onLongClick(position);
                        return false;
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return mResults.size();
        }

        void addResult(ResultItem result) {
//            String upperString = result.substring(0,1).toUpperCase() + result.substring(1);   //patli20180706
//            Log.d("test", "result.substring(0,1).toUpperCase()=" + result.substring(0,1).toUpperCase());
            mResults.add(0, result);
            notifyItemInserted(0);
        }

        void SetResultServer(int pos, String result) {
//            String upperString = result.substring(0,1).toUpperCase() + result.substring(1);   //patli20180706
            Log.d("test", "SetResultServer pos=" + pos + "; str=" + result);
            mResults.get(pos).setServerText(result);
            notifyDataSetChanged();
            //        notifyItemInserted(0);
        }

        void SetResultClient(int pos, String result) {
//            String upperString = result.substring(0,1).toUpperCase() + result.substring(1);   //patli20180706
//            Log.d("test", "result.substring(0,1).toUpperCase()=" + result.substring(0,1).toUpperCase());
            Log.d("test", "SetResultClient pos=" + pos + "; str=" + result);
            mResults.get(pos).setClientText(result);
            notifyDataSetChanged();
//            notifyItemInserted(0);
        }

        public List<ResultItem> getResults() {
            return mResults;
        }

    }

//    private static String getUserAgent(Context context) {
//        String userAgent = "";
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
//            try {
//                userAgent = WebSettings.getDefaultUserAgent(context);
//            } catch (Exception e) {
//                userAgent = System.getProperty("http.agent");
//            }
//        } else {
//            userAgent = System.getProperty("http.agent");
//        }
//        StringBuffer sb = new StringBuffer();
//        for (int i = 0, length = userAgent.length(); i < length; i++) {
//            char c = userAgent.charAt(i);
//            if (c <= '\u001f' || c >= '\u007f') {
//                sb.append(String.format("\\u%04x", (int) c));
//            } else {
//                sb.append(c);
//            }
//        }
//        return sb.toString();
//    }

    private void TTS_grpc_PostData(String lncode, String name, int Gender, int audio_code, String tts_text) {
//        {
//            tts_string = tts_text;
//            try {
//                SposGrpc.SposBlockingStub stub = SposGrpc.newBlockingStub(tts_channel);
//                TtsReq request = TtsReq.newBuilder().setLang(lncode)
//                        .setGender(Gender)
//                        .setAudioCode(audio_code)
//                        .setText(tts_text).build();
//                Iterator<TtsReply> it = stub.ttsRequest(request);
//
//                byte[] audio_data = new byte[0];
//                while (it.hasNext()) {
//                    audio_data = ByteUtil.append(audio_data, it.next().getAudioData().toByteArray());
//                    Log.d("test", "audio_data len=" + audio_data.length);
//                    //                System.out.print(it.next().getAudioData());
//                }
//
//                TTS_limitQ.offer(new AudioItem(audio_data, lncode));   //add queue
//
//                playMp3(audio_data);
//
//                TranstextViewHandler.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        if (mText != null) {
//                            mText.setText(null);
//
//                            if (speech_type == 0) {
//                                SetRecyclerViewItemServer(0, tts_string);
//                            } else {
//                                SetRecyclerViewItemClient(0, tts_string);
//                            }
//                        }
//                    }
//                });
//
//            } catch (Exception e) {
//                Log.e("test", "Exception....");
//                e.printStackTrace();
//            }
//        }
    }

    private static final int REQUEST_TIMEOUT = 10 * 1000;  //设置请求超时5秒钟
    private static final int SO_TIMEOUT = 10 * 1000;   //设置等待数据超时时间5秒钟

    private void TTS_HttpPostData(String lncode, String name, String Gender, String audio_code, String tts_text) {
//        tts_string = tts_text;
//        try {
//            BasicHttpParams httpParams = new BasicHttpParams();
//            HttpConnectionParams.setConnectionTimeout(httpParams, REQUEST_TIMEOUT);
//            HttpConnectionParams.setSoTimeout(httpParams, SO_TIMEOUT);
//
//            HttpClient httpclient = new DefaultHttpClient(httpParams);
//            Properties prop = System.getProperties();
//            prop.setProperty("socksProxySet", "true");
//            prop.setProperty("socksProxyHost", "127.0.0.1");
//            prop.setProperty("socksProxyPort", "1080");
//
//            String uri = "https://texttospeech.googleapis.com/v1beta1/text:synthesize";
//            HttpPost httppost = new HttpPost(uri);
//            //添加http头信息
//            httppost.addHeader("X-Goog-Api-Key", "AIzaSyCcyM4N2EK3sE4fvOk4Mla9sS2g1hTumrk"); //认证token
//            httppost.addHeader("Content-Type", "application/json; charset=UTF-8");
//
//            JSONObject input_obj = new JSONObject();
//            input_obj.put("text", tts_text);
//
//            JSONObject voice_obj = new JSONObject();
//
//            voice_obj.put("languageCode", lncode);
//            voice_obj.put("name", name);
//            //           if(lncode.equals("ja-JP")) {
//
//            //           }else {
//            voice_obj.put("ssmlGender", Gender);
//            //        }
//
//            JSONObject audioConfig_obj = new JSONObject();
//
//            //          if(lncode.equals("ja-JP"))
//            //          {
//            audioConfig_obj.put("audioEncoding", "LINEAR16");
//            audioConfig_obj.put("pitch", "0.00");
//            audioConfig_obj.put("speakingRate", "1.00");
//            //         }
//            //         else {
//            audioConfig_obj.put("audioEncoding", audio_code);    //"MP3“
//            //         }
//
//            JSONObject data_obj = new JSONObject();
//            data_obj.put("input", input_obj);
//            data_obj.put("voice", voice_obj);
//            data_obj.put("audioConfig", audioConfig_obj);
//
//            Log.d("test", "tts data_obj.toString()=" + data_obj.toString());
//            httppost.setEntity(new StringEntity(data_obj.toString(), "UTF-8"));
//            HttpResponse response;
//            response = httpclient.execute(httppost);
//            //检验状态码，如果成功接收数据
//            int code = response.getStatusLine().getStatusCode();
//
//            Log.d("test", "TTS_HttpPostData respones code=" + code);
//            if (code == 200) {
//                HttpEntity httpEntity = response.getEntity();
//
//                if (httpEntity != null) {
//                    BufferedReader bufferedReader = new BufferedReader(
//                            new InputStreamReader(httpEntity.getContent(), "UTF-8"), 8 * 1024);
//                    StringBuilder entityStringBuilder = new StringBuilder();
//                    String line = null;
//                    Log.d("test", "bufferedReader....");
//                    while ((line = bufferedReader.readLine()) != null) {
//                        Logger.i("test", "line len=" + line.length());
////                        Logger.i("test", "line len="+line.length()+";line="+line);
//                        entityStringBuilder.append(line);
//                    }
//
////                    Logger.i("test", "string len=" +entityStringBuilder.toString().length()+";string=" + entityStringBuilder.toString());
//                    Logger.i("test", "string len=" + entityStringBuilder.toString().length());
//
//                    JSONObject rev_obj = new JSONObject(entityStringBuilder.toString());
//
//                    String audioContent = rev_obj.getString("audioContent");
//                    Logger.i("test", "audioContent len=" + audioContent.length() + ";audioContent_30=" + audioContent.substring(0, 30));
////                    Logger.i("test", "audioContent len=" + audioContent.length());
//
//                    TTS_limitQ.offer(new AudioItem(audioContent, lncode));   //add queue
//
//                    playMp3(audioContent);
//
//                    TranstextViewHandler.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            if (mText != null) {
//                                mText.setText(null);
//
//                                if (speech_type == 0) {
//                                    SetRecyclerViewItemServer(0, tts_string);
//                                } else {
//                                    SetRecyclerViewItemClient(0, tts_string);
//                                }
//                            }
//                        }
//                    });
//                }
//
//            }
//        } catch (ClientProtocolException e) {
//            e.printStackTrace();
//            Log.e("test", "ClientProtocolException....");
//        } catch (IOException e) {
//            Log.e("test", "IOException....");
//            e.printStackTrace();
//        } catch (Exception e) {
//            Log.e("test", "Exception....");
//            e.printStackTrace();
//        }
    }

    private void Xunfei_TTS(String tts_lang, String tts_text) {

        Logger.i("test", "tts_lang=" + tts_lang + ";tts_text=" + tts_text);

        Intent intent = new Intent("com.ut.pos.tts");
        if (tts_lang.contains("普通话")) {
            intent.putExtra("com.ut.pos.tts.lang", "pt");
        } else if (tts_lang.contains("廣東話")) {
            intent.putExtra("com.ut.pos.tts.lang", "gd");
        } else {
            intent.putExtra("com.ut.pos.tts.lang", "pt");
        }

        intent.putExtra("com.ut.pos.tts.str", tts_text);
        sendBroadcast(intent);
    }

    private void Trans_grpc_PostData(String q_text, String source_ln, String target_ln) {
//        try {
////            channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build();
//            SposGrpc.SposBlockingStub stub = SposGrpc.newBlockingStub(trans_channel);
//            TransReq request = TransReq.newBuilder().setSLn(source_ln)
//                    .setTLn(target_ln)
//                    .setText(q_text).build();
//            TransReply reply = stub.transRequest(request);
//            String tr_str0 = reply.getTransText();
//
////            String ss=StringEscapeUtils.unescapeHtml4(tr_str0);
//
//
//            Log.d("test", "Trans_grpc_PostData tr_str0=" + tr_str0);
//
//            tr_str = tr_str0.substring(0, 1).toUpperCase() + tr_str0.substring(1);   //patli20180706 首字符大写
//            Log.d("test", "text.substring(0,1).toUpperCase()=" + tr_str0.substring(0, 1).toUpperCase());
//            Logger.i("test", " tr_str=" + tr_str);
//            TranstextViewHandler.post(new Runnable() {
//                @Override
//                public void run() {
//                    if (mText != null) {
//                        mText.setText(tr_str);
//                        //                              Logger.i("test", " trans_target_lang=" + trans_target_lang+";target_ln.contains="+trans_target_lang.equals("zh-CN"));
//                        if (trans_target_lang.equals("zh-CN")) {
//
//                            Logger.i("test", " Xunfei_TTS!");
//                            Xunfei_TTS(tts_target_lang, tr_str);
//                            TTS_limitQ.offer(new AudioItem(tr_str, tts_target_lang));   //add queue
//                            if (speech_type == 0) {
//                                SetRecyclerViewItemServer(0, tr_str);
//                            } else {
//                                SetRecyclerViewItemClient(0, tr_str);
//                            }
//
//                        } else {
//                            //patli 20180629 add
//                            Log.d("test", "1111111trans tts.....");
//                            TTS_Operation tts_lp = new TTS_Operation();
//                            Map<String, String> params = new HashMap<String, String>();
//                            params.put("text", tr_str);
//
//                            params.put("languageCode", tts_target_lang);
//                            params.put("name", tts_target_voice);
//                            params.put("ssmlGender", tts_target_gender);
//                            params.put("audioEncoding", "MP3");
//                            tts_lp.execute(params);
//                            //patli end
//                        }
//                    }
//                }
//            });
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    private void Trans_HttpPostData(String q_text, String source_ln, String target_ln) {
//        try {
//            HttpClient httpclient = new DefaultHttpClient();
//            Properties prop = System.getProperties();
//            prop.setProperty("socksProxySet", "true");
//            prop.setProperty("socksProxyHost", "127.0.0.1");
//            prop.setProperty("socksProxyPort", "1080");
//
//            String uri = "https://translation.googleapis.com/language/translate/v2";
//            HttpPost httppost = new HttpPost(uri);
//            //添加http头信息
//            httppost.addHeader("X-Goog-Api-Key", "AIzaSyCcyM4N2EK3sE4fvOk4Mla9sS2g1hTumrk"); //认证token
//            httppost.addHeader("Content-Type", "application/json; charset=UTF-8");
//
//            JSONObject data_obj = new JSONObject();
//            data_obj.put("q", q_text);
//            data_obj.put("source", source_ln);
//            data_obj.put("target", target_ln);
//
//            Log.d("test", "data_obj.toString()=" + data_obj.toString());
//
//            StringEntity stringEntity = new StringEntity(data_obj.toString(), "UTF-8");
//            //        stringEntity.setContentEncoding("ISO-639-1");
//            httppost.setEntity(stringEntity);
//
//            HttpResponse response;
//            response = httpclient.execute(httppost);
//            //检验状态码，如果成功接收数据
//            int code = response.getStatusLine().getStatusCode();
//
//            Log.d("test", "respones code=" + code);
//            if (code == 200) {
//
//                HttpEntity httpEntity = response.getEntity();
//
//                if (httpEntity != null) {
//                    BufferedReader bufferedReader = new BufferedReader(
//                            new InputStreamReader(httpEntity.getContent(), "UTF-8"), 8 * 1024);
//                    StringBuilder entityStringBuilder = new StringBuilder();
//                    String line = null;
//                    Log.d("test", "bufferedReader....");
//                    while ((line = bufferedReader.readLine()) != null) {
//                        Logger.i("test", "line len=" + line.length() + ";line=" + line);
//                        entityStringBuilder.append(line);
//                    }
//
//                    Logger.i("test", "string len=" + entityStringBuilder.toString().length() + ";string=" + entityStringBuilder.toString());
//
//                    JSONObject rev_obj = new JSONObject(entityStringBuilder.toString());
//                    String data_str = rev_obj.getString("data");
//                    Logger.i("test", "data_str=" + data_str);
//
//                    JSONObject trs_obj = new JSONObject(data_str);
//                    String trs_str = trs_obj.getString("translations");
//                    Logger.i("test", "trs_str=" + trs_str);
//
//                    JSONArray tr_ary = new JSONArray(trs_str);
//
//                    if (tr_ary.length() >= 1) {
//                        Logger.i("test", " tr_ary.length()=" + tr_ary.length());
//                        String tr_str0 = tr_ary.getJSONObject(0).getString("translatedText");
//                        tr_str = tr_str0.substring(0, 1).toUpperCase() + tr_str0.substring(1);   //patli20180706 首字符大写
//                        Log.d("test", "text.substring(0,1).toUpperCase()=" + tr_str0.substring(0, 1).toUpperCase());
//                        Logger.i("test", " tr_str=" + tr_str);
//                        TranstextViewHandler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                if (mText != null) {
//                                    mText.setText(tr_str);
//                                    //                              Logger.i("test", " trans_target_lang=" + trans_target_lang+";target_ln.contains="+trans_target_lang.equals("zh-CN"));
//                                    if (trans_target_lang.equals("zh-CN")) {
//
//                                        Logger.i("test", " Xunfei_TTS!");
//                                        Xunfei_TTS(tts_target_lang, tr_str);
//                                        TTS_limitQ.offer(new AudioItem(tr_str, tts_target_lang));   //add queue
//                                        if (speech_type == 0) {
//                                            SetRecyclerViewItemServer(0, tr_str);
//                                        } else {
//                                            SetRecyclerViewItemClient(0, tr_str);
//                                        }
//
//                                    } else {
//                                        //patli 20180629 add
//                                        Log.d("test", "1111111trans tts.....");
//                                        TTS_Operation tts_lp = new TTS_Operation();
//                                        Map<String, String> params = new HashMap<String, String>();
//                                        params.put("text", tr_str);
//
//                                        params.put("languageCode", tts_target_lang);
//                                        params.put("name", tts_target_voice);
//                                        params.put("ssmlGender", tts_target_gender);
//                                        params.put("audioEncoding", "MP3");
//                                        tts_lp.execute(params);
//                                        //patli end
//                                    }
//                                }
//                            }
//                        });
//                    }
//                }
//
//            }
//        } catch (ClientProtocolException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    private void HttpPostData1() {
//        try {
//            String uri = "https://texttospeech.googleapis.com/v1beta1/text:synthesize";
//            String result = "";
//
//            Properties prop = System.getProperties();
//
//            prop.setProperty("socksProxySet", "true");
//            prop.setProperty("socksProxyHost", "127.0.0.1");
//            prop.setProperty("socksProxyPort", "1080");
//
//            URL url = new URL(uri);
//            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
//            SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
//            conn.setSSLSocketFactory(sslsocketfactory);
//            conn.setDoOutput(true);
//            conn.setDoInput(true);
//            //     conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
////        conn.setRequestProperty("ApiUserName", SoapHeader.VALUE_USERNAME);
////        conn.setRequestProperty("ApiPassword", SoapHeader.VALUE_PASSWORD);
//            conn.setRequestProperty("X-Goog-Api-Key", "AIzaSyCcyM4N2EK3sE4fvOk4Mla9sS2g1hTumrk");
//            conn.setRequestMethod("POST");
//            conn.setConnectTimeout(1 * 60 * 1000);
//            conn.setReadTimeout(3000000);  //3000秒读取时间，数据量大可以设置长些
//
//            JSONObject input_obj = new JSONObject();
//            input_obj.put("text", "hello,Android is a mobile system");
//            JSONObject voice_obj = new JSONObject();
//            voice_obj.put("languageCode", "en-gb");
//            voice_obj.put("name", "en-GB-Standard-A");
//            voice_obj.put("ssmlGender", "FEMALE");
//            JSONObject audioConfig_obj = new JSONObject();
//            audioConfig_obj.put("audioEncoding", "MP3");
//
//            JSONObject data_obj = new JSONObject();
//            data_obj.put("input", input_obj);
//            data_obj.put("voice", voice_obj);
//            data_obj.put("audioConfig", audioConfig_obj);
//
//            Log.d("test", "2222data_obj.toString()=" + data_obj.toString());
//            DataOutputStream out = new DataOutputStream(conn.getOutputStream());
//
//            out.writeBytes(data_obj.toString());
//            conn.connect();
//
//            if (conn.getResponseCode() == 200) {
//                InputStream input = conn.getInputStream();
//
//                //       write("syn.txt", input);
//
//                BufferedReader reader = new BufferedReader(new InputStreamReader(input), 8 * 1024);
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    //        result.append(line);
//                    Log.d("test", "222line len=" + line.length() + ";222line=" + line);
//                    //     result = result.concat(line);
//                }
//
////            byte [] buffer = new byte[4 * 1024];
////            while(input.read(buffer) != -1){
////                Log.d("test", "HttpPostData1  buffer.length="+buffer.length);
////                Log.d("test", "HttpPostData1  buffer="+buffer);
////                //        result = result.concat(buffer.toString());
////                result = result.concat(buffer.toString());
////      //          output.write(buffer);
////        //        output.flush();
//                //           }
//            }
//            //      Log.d("test", "result="+result);
//
//            out.flush();
//            out.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    String path = Environment.getExternalStorageDirectory() + "/";

    private int write(String fileName, InputStream in) {

        try {
            FileUtil fileUtil = new FileUtil();
            if (fileUtil.isFileExist(path + fileName)) {
                Log.d("test", "write IS FileExist!");
                return 1;
            } else {

                File resultFile = fileUtil.write2SDFromInput(path, fileName, in);
                if (resultFile == null)
                    Log.d("test", "resultFile IS null!");
                return -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

//        return 0;
    }

    public static void getMp3(String mps_str) {
        //      Base64 decoder = new BASE64Decoder();
        try {
            byte[] data = Base64.decode(mps_str.getBytes(), Base64.DEFAULT);
            /*for(int i=0 ;i<data.length;i++){
                if (data[i] < 0) {
                    data[i] += 256;
                }
            }*/
            OutputStream out = new FileOutputStream("click_BASE64.mp3");
            out.write(data);
            out.flush();
            out.close();
        } catch (Exception ex) {

        }
    }

    private void playMp3(byte[] audio_data) {
        try {
            // create temp file that will hold byte array
            File tempMp3 = null;

            tempMp3 = File.createTempFile("kurchina", "mp3", getCacheDir());

            Log.d("test", "tempMp3=" + tempMp3);

            tempMp3.deleteOnExit();
            FileOutputStream fos = new FileOutputStream(tempMp3);
            fos.write(audio_data);
            fos.close();

            // Tried reusing instance of media player
            // but that resulted in system crashes...
            MediaPlayer mediaPlayer = new MediaPlayer();

            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    Log.d(TAG, "OnError - Error code: " + what + " Extra code: " + extra);
                    switch (what) {
                        case -1004:
                            Log.d(TAG, "MEDIA_ERROR_IO");
                            break;
                        case -1007:
                            Log.d(TAG, "MEDIA_ERROR_MALFORMED");
                            break;
                        case 200:
                            Log.d(TAG, "MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK");
                            break;
                        case 100:
                            Log.d(TAG, "MEDIA_ERROR_SERVER_DIED");
                            break;
                        case -110:
                            Log.d(TAG, "MEDIA_ERROR_TIMED_OUT");
                            break;
                        case 1:
                            Log.d(TAG, "MEDIA_ERROR_UNKNOWN");
                            break;
                        case -1010:
                            Log.d(TAG, "MEDIA_ERROR_UNSUPPORTED");
                            break;
                    }
                    switch (extra) {
                        case 800:
                            Log.d(TAG, "MEDIA_INFO_BAD_INTERLEAVING");
                            break;
                        case 702:
                            Log.d(TAG, "MEDIA_INFO_BUFFERING_END");
                            break;
                        case 701:
                            Log.d(TAG, "MEDIA_INFO_METADATA_UPDATE");
                            break;
                        case 802:
                            Log.d(TAG, "MEDIA_INFO_METADATA_UPDATE");
                            break;
                        case 801:
                            Log.d(TAG, "MEDIA_INFO_NOT_SEEKABLE");
                            break;
                        case 1:
                            Log.d(TAG, "MEDIA_INFO_UNKNOWN");
                            break;
                        case 3:
                            Log.d(TAG, "MEDIA_INFO_VIDEO_RENDERING_START");
                            break;
                        case 700:
                            Log.d(TAG, "MEDIA_INFO_VIDEO_TRACK_LAGGING");
                            break;
                    }
                    return false;
                }
            });

            //           MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.t2);
            //         final MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.lovemelikeyoudo);
            // Tried passing path directly, but kept getting
            // "Prepare failed.: status=0x1"
            // so using file descriptor instead
            FileInputStream fis = new FileInputStream(tempMp3);
            mediaPlayer.setDataSource(fis.getFD());

            mediaPlayer.prepare();
            mediaPlayer.start();

//                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.t2);
//                mediaPlayer.start();

            fis.close();

        } catch (IOException ex) {
            String s = ex.toString();
            ex.printStackTrace();
            Log.d("test", "ex.toString()=" + ex.toString());
        }
    }

    private void playMp3(String mps_str) {
//        try {
//            // create temp file that will hold byte array
//            File tempMp3 = null;
//            byte[] mp3SoundByteArray = Base64.decode(mps_str.getBytes(), Base64.DEFAULT);
//            Log.d("test", "mp3SoundByteArray len=" + mp3SoundByteArray.length);
//
//            tempMp3 = File.createTempFile("kurchina", "mp3", getCacheDir());
//
//            Log.d("test", "tempMp3=" + tempMp3);
//
//            tempMp3.deleteOnExit();
//            FileOutputStream fos = new FileOutputStream(tempMp3);
//            fos.write(mp3SoundByteArray);
//            fos.close();
//
//            // Tried reusing instance of media player
//            // but that resulted in system crashes...
//            MediaPlayer mediaPlayer = new MediaPlayer();
//
//            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
//                @Override
//                public boolean onError(MediaPlayer mp, int what, int extra) {
//                    Log.d(TAG, "OnError - Error code: " + what + " Extra code: " + extra);
//                    switch (what) {
//                        case -1004:
//                            Log.d(TAG, "MEDIA_ERROR_IO");
//                            break;
//                        case -1007:
//                            Log.d(TAG, "MEDIA_ERROR_MALFORMED");
//                            break;
//                        case 200:
//                            Log.d(TAG, "MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK");
//                            break;
//                        case 100:
//                            Log.d(TAG, "MEDIA_ERROR_SERVER_DIED");
//                            break;
//                        case -110:
//                            Log.d(TAG, "MEDIA_ERROR_TIMED_OUT");
//                            break;
//                        case 1:
//                            Log.d(TAG, "MEDIA_ERROR_UNKNOWN");
//                            break;
//                        case -1010:
//                            Log.d(TAG, "MEDIA_ERROR_UNSUPPORTED");
//                            break;
//                    }
//                    switch (extra) {
//                        case 800:
//                            Log.d(TAG, "MEDIA_INFO_BAD_INTERLEAVING");
//                            break;
//                        case 702:
//                            Log.d(TAG, "MEDIA_INFO_BUFFERING_END");
//                            break;
//                        case 701:
//                            Log.d(TAG, "MEDIA_INFO_METADATA_UPDATE");
//                            break;
//                        case 802:
//                            Log.d(TAG, "MEDIA_INFO_METADATA_UPDATE");
//                            break;
//                        case 801:
//                            Log.d(TAG, "MEDIA_INFO_NOT_SEEKABLE");
//                            break;
//                        case 1:
//                            Log.d(TAG, "MEDIA_INFO_UNKNOWN");
//                            break;
//                        case 3:
//                            Log.d(TAG, "MEDIA_INFO_VIDEO_RENDERING_START");
//                            break;
//                        case 700:
//                            Log.d(TAG, "MEDIA_INFO_VIDEO_TRACK_LAGGING");
//                            break;
//                    }
//                    return false;
//                }
//            });
//
//            //           MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.t2);
//            //         final MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.lovemelikeyoudo);
//            // Tried passing path directly, but kept getting
//            // "Prepare failed.: status=0x1"
//            // so using file descriptor instead
//            FileInputStream fis = new FileInputStream(tempMp3);
//            mediaPlayer.setDataSource(fis.getFD());
//
//            mediaPlayer.prepare();
//            mediaPlayer.start();
//
////                MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.t2);
////                mediaPlayer.start();
//
//            fis.close();
//
//        } catch (IOException ex) {
//            String s = ex.toString();
//            ex.printStackTrace();
//            Log.d("test", "ex.toString()=" + ex.toString());
//        }
    }

    private class TTS_Operation extends AsyncTask<Map<String, String>, Void, String> {
        @Override
        protected String doInBackground(Map<String, String>... params) {

            Map<String, String> map = params[0];
            tts_channel = ManagedChannelBuilder.forAddress(Option.host, Option.port).usePlaintext(true).build();

            Log.d("test", "doInBackground tts_str=" + map.get("text"));

            TTS_grpc_PostData(map.get("languageCode"), map.get("name"), 2, 2, map.get("text"));
            //          TTS_HttpPostData(map.get("languageCode"), map.get("name"), map.get("ssmlGender"), map.get("audioEncoding"), map.get("text"));
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                tts_channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    private class Trans_Operation extends AsyncTask<Map<String, String>, Void, String> {
        @Override
        protected String doInBackground(Map<String, String>... params) {
            Map<String, String> map = params[0];

            Log.d("test", "doInBackground trans_str=" + map.get("q"));

            //          Trans_HttpPostData(map.get("q"), map.get("source"), map.get("target"));
            trans_channel = ManagedChannelBuilder.forAddress(Option.host, Option.port).usePlaintext(true).build();

            Trans_grpc_PostData(map.get("q"), map.get("source"), map.get("target"));
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                trans_channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

//    public static void synthesizeText(String text)
//            throws Exception {
//        // Instantiates a client
//        try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()) {
//            // Set the text input to be synthesized
//            SynthesisInput input = SynthesisInput.newBuilder()
//                    .setText(text)
//                    .build();
//
//            // Build the voice request
//            VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
//                    .setLanguageCode("en-US") // languageCode = "en_us"
//                    .setSsmlGender(SsmlVoiceGender.FEMALE) // ssmlVoiceGender = SsmlVoiceGender.FEMALE
//                    .build();
//
//            // Select the type of audio file you want returned
//            AudioConfig audioConfig = AudioConfig.newBuilder()
//                    .setAudioEncoding(AudioEncoding.MP3) // MP3 audio.
//                    .build();
//
//            // Perform the text-to-speech request
//            SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice,
//                    audioConfig);
//
//            // Get the audio contents from the response
//            ByteString audioContents = response.getAudioContent();
//
//            // Write the response to the output file.
//            try (OutputStream out = new FileOutputStream("output.mp3")) {
//                out.write(audioContents.toByteArray());
//                System.out.println("Audio content written to file \"output.mp3\"");
//            }
//        }
//    }

}
