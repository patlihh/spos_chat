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
import android.content.DialogInterface;
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
import android.support.v4.content.ContextCompat;
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

import android.widget.TextView;
import android.widget.Toast;

import com.google.protobuf.ByteString;
import com.uni.cloud.chat.ChatApplication;
import com.uni.cloud.chat.R;
import com.uni.cloud.chat.audio.AudioDataUtil;
import com.uni.cloud.chat.audio.OpusEncoder;
import com.uni.cloud.chat.dialog.MessageDialog;
import com.uni.cloud.chat.log.LogcatHelper;
import com.uni.cloud.chat.log.Logger;
import com.uni.cloud.chat.login.LoginActivity;
import com.uni.cloud.chat.misc.ByteUtil;
import com.uni.cloud.chat.misc.FileUtil;
import com.uni.cloud.chat.misc.LimitQueue;
import com.uni.cloud.chat.misc.Option;
import com.uni.cloud.chat.xunfei_tts.TtsDemo;


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import static com.uni.cloud.chat.misc.Option.Opus.FRAME_SIZE;
import static com.uni.cloud.chat.misc.Option.Opus.NUM_CHANNELS;


public class ListUserActivity extends AppCompatActivity implements MessageDialogFragment.Listener, View.OnClickListener {

    private static final String FRAGMENT_MESSAGE_DIALOG = "message_dialog";

    private static final String STATE_RESULTS = "results";

    private static final String TAG = "ListUserActivity";

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;

    private SpeechService mSpeechService;
    private ChatApplication mApp;

    private VoiceRecorder mVoiceRecorder;

    private String dest_id;
    // private String me_id;
    private TextView me_tv;
    private TextView voice_tv;

    private Boolean Is_haveVoice = false;

    private ManagedChannel trans_channel;
    private ManagedChannel tts_channel;

    ViewHolder lastSendVoiceItem;
    ViewHolder lastHaveVoiceItem;

    private static String last_recv_voice_id;
    private static String last_send_voice_id;
    private static boolean is_last_send_voice_ok;

    private static UserListAdapter mAdapter;
    private static RecyclerView mRecyclerView;

    private static List<UserItem> userItemList = new ArrayList<>();

    private static ListUserActivity listUserActivity;

    OpusEncoder encoder;
    //  byte[] inBuf = new byte[FRAME_SIZE * NUM_CHANNELS * 2];
    byte[] encBuf = new byte[1024];

    private final VoiceRecorder.Callback mVoiceCallback = new VoiceRecorder.Callback() {

        @Override
        public void onVoiceStart() {
            Log.d("test", "onVoiceStart mSpeechService=" + mSpeechService + "SampleRate=" + mVoiceRecorder.getSampleRate());
            if (mSpeechService != null) {
                mSpeechService.startSendVoiceing(dest_id, mVoiceRecorder.getSampleRate());
                Is_haveVoice = true;

                if (Option.Opus.Is_Opus) {
                    encoder = new OpusEncoder();
                    encoder.init(mVoiceRecorder.getSampleRate(), NUM_CHANNELS, OpusEncoder.OPUS_APPLICATION_VOIP);
                }

            }
        }

        @Override
        public void onVoice(byte[] data, int size) {
            if (mSpeechService != null && Is_haveVoice) {
//                Log.d("opus", "Not Speex onVoice data.length=" + data.length + ";size=" + size+";data="+ByteUtil. byte2HexStr(data));
                if (Option.Opus.Is_Opus) {
                    int encoded = encoder.encode(data, FRAME_SIZE, encBuf);
                    byte[] encBuf2 = Arrays.copyOf(encBuf, encoded);
                    Log.v("opus", "Encoded " + data.length + " bytes of audio into " + encoded + " bytes" + ";encBuf=" + ByteUtil.byte2HexStr(encBuf2));
                    mSpeechService.SendVoiceing(dest_id, mVoiceRecorder.getSampleRate(), encBuf2, encoded);
                } else {
                    mSpeechService.SendVoiceing(dest_id, mVoiceRecorder.getSampleRate(), data, size);
                }
            }
        }

        @Override
        public void onVoice(short[] data, int size) {
            if (mSpeechService != null && Is_haveVoice) {
                int spx_len = AudioDataUtil.raw2spx(data).length;
                Log.d("test", "Speex onVoice data.length=" + data.length + ";size=" + size + ";spx_len=" + spx_len);

                if (Option.Opus.Is_Opus) {
                    int encoded = encoder.encode(data, FRAME_SIZE, encBuf);
                    Log.v(TAG, "Encoded " + data.length + " bytes of audio into " + encoded + " bytes");
                    mSpeechService.SendVoiceing(dest_id, mVoiceRecorder.getSampleRate(), encBuf, encBuf.length);
                } else {
                    mSpeechService.SendVoiceing(dest_id, mVoiceRecorder.getSampleRate(), AudioDataUtil.raw2spx(data), spx_len);
                }
            }
        }

        @Override
        public void onVoiceEnd() {
            Log.d("test", "onVoiceEnd");
            Is_haveVoice = false;
            if (mSpeechService != null) {
                mSpeechService.finishSendVoiceing(dest_id, mVoiceRecorder.getSampleRate());

                if (Option.Opus.Is_Opus&&encoder!=null) {
                    encoder.close();
                }
            }
        }
    };

    private final SpeechService.HeartListener mHeartListener =
            new SpeechService.HeartListener() {
                @Override
                public void onHeart(boolean ret) {
                    if (ret) {
                        Log.d(TAG, "onHeart success!");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                RefreshAdapterItem();
                            }
                        });
                    } else {
                        Log.d(TAG, "onHeart fail!");
                    }
                }
            };

    private final SpeechService.RecMsgListener mRecvMsgListener =
            new SpeechService.RecMsgListener() {
                @Override
                public void onRecMsg(String voice_id) {
                    Log.d(TAG, "onRecMsg....., voice_id=" + voice_id);
                    last_recv_voice_id = voice_id;
                    if (voice_tv != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d(TAG, "onRecMsg111....., voice_id=" + voice_id);

                                voice_tv.setTextColor(getResources().getColor(R.color.red));
                                voice_tv.setText("from: " + mAdapter.getUserNameById(voice_id));

                                SetItemHaveVoice(mAdapter.getItemIndexbById(voice_id));
                            }
                        });
                    }

                    //chang online flag
                    if (!mAdapter.GetUserOnline(voice_id)) {
                        mAdapter.SetUserOnline(voice_id, true);
                        mAdapter.notifyDataSetChanged();
                        mRecyclerView.requestLayout();
                    }
                }
            };

    private final SpeechService.SendMsgListener mSendMsgListener =
            new SpeechService.SendMsgListener() {
                @Override
                public void onSendMsg(int res, String msg) {
                    Log.d(TAG, "onSendMsg..., res=" + res + ";msg=" + msg);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            last_send_voice_id = dest_id;
                            if (res != Option.SendMsgRes.SEND_MSG_OK) {
                                SetItemSendVoice(mAdapter.getItemIndexbById(dest_id), false);
                                mVoiceRecorder.stoping();
                                switch (res) {
                                    case Option.SendMsgRes.SEND_MSG_USER_LOGOUT:
                                        //                        mSpeechService.Login(me_id,password);
                                        break;
                                    case Option.SendMsgRes.SEND_MSG_NO_DEST_USER:
                                        //              Toast.makeText(ListUserActivity.this,"no dest user",Toast.LENGTH_SHORT).show();
                                        break;
                                    case Option.SendMsgRes.SEND_MSG_DEST_USER_LOGOUT:
                                        //               Toast.makeText(ListUserActivity.this,"dest user not login",Toast.LENGTH_SHORT).show();
                                        break;
                                }
                            } else {
                                SetItemSendVoice(mAdapter.getItemIndexbById(dest_id), true);
                            }
                        }
                    });

                }
            };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {

            Log.d(TAG, "ListUserActivity onServiceConnected...");
            mSpeechService = SpeechService.from(binder);

            me_tv.setText(mApp.getOwer_name());

            RefreshAdapterItem();

            mSpeechService.setSendMsgListener(mSendMsgListener);
            mSpeechService.setRecvMsgListener(mRecvMsgListener);
            mSpeechService.setHeartListener(mHeartListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "ListUserActivity onServiceDisconnected...");
            mSpeechService = null;
        }

    };

    public ListUserActivity() {
        listUserActivity = this;
    }

    public static ListUserActivity getListUserActivity() {
        return listUserActivity;
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

        mApp = (ChatApplication) getApplication();

        Log.d(TAG, "onCreate, mApp.isLogin=" + mApp.isLogin);
        if (!mApp.isLogin) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }

        setContentView(R.layout.activity_userlist);

        final Resources resources = getResources();
        final Resources.Theme theme = getTheme();

        me_tv = findViewById(R.id.tv_me);
        voice_tv = findViewById(R.id.tv_voice_name);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mAdapter = new UserListAdapter(userItemList);
        mRecyclerView.setAdapter(mAdapter);

        mAdapter.setOnItemClickListener(new UserListAdapter.OnItemClickListener() {
            @Override
            public void onClick(int position) {
                Log.d("test", "您点击了" + position + "行");
                mVoiceRecorder.stoping();
            }

            @Override
            public void onLongClick(int position) {
                Log.d("test", "您长按点击了" + position + "行");
            }
        });

        mAdapter.setOnItemTouchListener(new UserListAdapter.OnItemTouchListener() {
            @Override
            public void onTouch(int position, MotionEvent motionEvent) {
                //             Log.d(TAG, "Touch:" + position + "行" + "event=" + motionEvent.getAction());
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    if(mAdapter.mUsers.get(position).Isonline){
                        dest_id = mAdapter.GetUserId(position);
                        Log.d(TAG, "start recorder..., dest_id:" + mAdapter.GetUserId(position));
                        mVoiceRecorder.starting();
                    }
                }

                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    if(mAdapter.mUsers.get(position).Isonline) {
                        dest_id = mAdapter.GetUserId(position);
                        Log.d(TAG, "stop recorder..., dest_id:" + dest_id);
                        mVoiceRecorder.stoping();
                    }
                }
            }
        });

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

        findViewById(R.id.but_logout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog alert = new AlertDialog.Builder(ListUserActivity.this)
                        .setTitle("Confirm exit the app?")
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        // 点击“确认”后的操作
                                        mSpeechService.Logout(mApp.getOwer_id());
                                                     System.exit(0);
                                    }
                                })

                        .setNegativeButton(android.R.string.cancel, null).show();

            }
        });

    }

    @Override
    public void onAttachedToWindow() {

        super.onAttachedToWindow();

        final View view = getWindow().getDecorView();

        final WindowManager.LayoutParams lp = (WindowManager.LayoutParams) view.getLayoutParams();

        lp.gravity = Gravity.CENTER;

        DisplayMetrics metrics = new DisplayMetrics();
        Display display = getWindowManager().getDefaultDisplay();
        display.getMetrics(metrics);

        Log.d("test", "widthPixels=" + metrics.widthPixels + ";heightPixels=" + metrics.heightPixels + ";xdpi=" + metrics.xdpi + ";ydpi=" + metrics.ydpi);

        if (!Option.FullScreen_IsOpen) {
            if (metrics.widthPixels <= 480) {
                lp.width = 400;    //800;   //400;   //480;
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
        } else {
            lp.width = metrics.widthPixels;    //800;   //400;   //480;
            lp.height = metrics.heightPixels;    //1000;  //1200;    //640;
        }

        Log.d("test", "lp.width=" + lp.width + ";lp.height=" + lp.height);

        getWindowManager().updateViewLayout(view, lp);

    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart....");
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
        super.onStart();
    }

    @Override
    protected void onRestart() {
        Log.d(TAG, "onRestart.....");
        super.onRestart();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop....");
        // Stop listening to voice
        stopVoiceRecorder();

        if(mSpeechService != null){
            mSpeechService.RemoveHeartListener(mHeartListener);
            mSpeechService.RemoveSendMsgListener(mSendMsgListener);
            mSpeechService.RemoveRecvMsgListener(mRecvMsgListener);
            unbindService(mServiceConnection);
            mSpeechService = null;
        }
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState");
//        if (mAdapter != null) {
//            outState.putStringArrayList(STATE_RESULTS, mAdapter.getResults());
//        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG, "onRestoreInstanceState");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume mSpeechService="+mSpeechService);

        if(mSpeechService==null)
            bindService(new Intent(this, SpeechService.class), mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
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

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startVoiceRecorder() {
        Log.e(TAG, "startVoiceRecorder mVoiceRecorder=" + mVoiceRecorder);
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
        }
        mVoiceRecorder = new VoiceRecorder(mVoiceCallback);
        mVoiceRecorder.start();
    }

    private void stopVoiceRecorder() {
        Log.e(TAG, "stopVoiceRecorder mVoiceRecorder=" + mVoiceRecorder);
        if (mVoiceRecorder != null) {
            mVoiceRecorder.stop();
            mVoiceRecorder = null;
        }
    }

    private synchronized void RefreshAdapterItem(){
        Log.d(TAG, "RefreshAdapterItem mUsers size=" + mApp.mUsers.size());
        mAdapter.mUsers.clear();
        //     mAdapter.notifyDataSetChanged();

        for (User usr : mApp.mUsers) {
            Log.d(TAG, "id=" + usr.id + ";name=" + usr.name+ ";islogin=" + usr.login+ ";isonline=" + usr.online);
            mAdapter.addUserItem(new UserItem(usr.name, usr.id, usr.login, usr.online));
        }

        mAdapter.notifyDataSetChanged();
        mRecyclerView.requestLayout();
    }

    private void showPermissionMessageDialog() {
        MessageDialogFragment
                .newInstance(getString(R.string.permission_message))
                .show(getSupportFragmentManager(), FRAGMENT_MESSAGE_DIALOG);
    }

    public void SetItemHaveVoice(int position) {
        Log.d(TAG, "SetItemHaveVoice1111 selectItem position=" + position);
        mAdapter.notifyDataSetChanged();
        mRecyclerView.requestLayout();
        ViewHolder viewHolder = (ViewHolder) mRecyclerView.findViewHolderForAdapterPosition(position);
        Log.d(TAG, "SetItemHaveVoice1111 selectItem viewHolder=" + viewHolder);
        if (viewHolder != null) {
            Log.d(TAG, "SetItemHaveVoice1111 selectItem position text=" + viewHolder.Idtext.getText());

            if (lastHaveVoiceItem != null && lastHaveVoiceItem != viewHolder) {
                lastHaveVoiceItem.Idtext.setBackgroundColor(getResources().getColor(R.color.white));
            }
            Log.d(TAG, "selectItem position=" + position + " set green!");
            viewHolder.Idtext.setBackgroundColor(getResources().getColor(R.color.green));
            lastHaveVoiceItem = viewHolder;
        }else {
            Log.e(TAG, "SetItemHaveVoice1111 selectItem viewHolder null");
        }
//        mAdapter.notifyDataSetChanged();
//        View v = mRecyclerView.getChildAt(position);
//        if(v != null) {
//            RecyclerView.ViewHolder viewHolder = mRecyclerView.getChildViewHolder(v);
//            if (lastHaveVoiceItem != null && lastHaveVoiceItem != viewHolder) {
//                lastHaveVoiceItem.itemView.findViewById(R.id.account_id).setBackgroundColor(getResources().getColor(R.color.white));
//            }
//            viewHolder.itemView.findViewById(R.id.account_id).setBackgroundColor(getResources().getColor(R.color.green));
//            lastHaveVoiceItem = viewHolder;
////            mAdapter.notifyDataSetChanged();
//        }else {
//            Log.e(TAG, "SetItemHaveVoice1111 selectItem viewHolder null");
//        }

    }

    public void SetItemSendVoice(int position, boolean is_ok) {
        Log.d(TAG, "SetItemSendVoice selectItem position=" + position + ";is_ok=" + is_ok);
        is_last_send_voice_ok = is_ok;
        mAdapter.notifyDataSetChanged();
        mRecyclerView.requestLayout();
        ViewHolder viewHolder = (ViewHolder) mRecyclerView.findViewHolderForAdapterPosition(position);

        if (viewHolder != null) {
            Log.d(TAG, "selectItem viewHolder=" + viewHolder + ";lastSendVoiceItem=" + lastSendVoiceItem);
            if (lastSendVoiceItem != null && lastSendVoiceItem != viewHolder) {
                Log.d(TAG, "selectItem set lastSendVoiceItem");
                lastSendVoiceItem.Nametext.setBackgroundColor(getResources().getColor(R.color.white));
            }
            lastSendVoiceItem = viewHolder;

            if (is_ok) {
                Log.d(TAG, "selectItem position=" + position + " set blue!");
                viewHolder.Nametext.setBackgroundColor(getResources().getColor(R.color.blue));
            } else {
                Log.d(TAG, "selectItem position=" + position + " set red!");
                viewHolder.Nametext.setBackgroundColor(getResources().getColor(R.color.red));
            }

            Log.d(TAG, "lastSendVoiceItem11111=" + lastSendVoiceItem);
        }else {
            Log.e(TAG, "SetItemSendVoice selectItem viewHolder null");
        }

    }

    @Override
    public void onClick(View view) {

        Log.d("test", "onClick  view.getId()=" + view.getId());
//        Log.d("test", "onClick  R.id.but_translation="+R.id.but_translation);
        switch (view.getId()) {
//            case R.id.but_speak_client:
//                break;
//            case R.id.but_speak_server:
//                mSpeechService.Login(Option.src_id, Option.src_password);
//                break;
        }
    }

    @Override
    public void onMessageDialogDismissed() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                REQUEST_RECORD_AUDIO_PERMISSION);
    }


    private static class ViewHolder extends RecyclerView.ViewHolder {

        TextView Nametext;
        TextView Idtext;

        ViewHolder(final View parent) {
//            super(inflater.inflate(R.layout.item_result, parent, false));
            super(parent);
            Nametext = (TextView) parent.findViewById(R.id.account_name);
            Idtext = (TextView) parent.findViewById(R.id.account_id);

//            Servertext.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
////                    Toast.makeText(itemView.getContext(), "Position:" + Integer.toString(getPosition()), Toast.LENGTH_SHORT).show();
//                    Log.d("test", "Position=" + getPosition());
//                }
//            });
        }
    }

    public class UserItem {
        private String NameText;
        private String IdText;
        private boolean Islogin;
        private boolean Isonline;

        public UserItem() {

        }

        public UserItem(String nameText, String idText) {
            this.NameText = nameText;
            this.IdText = idText;
        }

        public UserItem(String nameText, String idText, boolean islogin, boolean isonline) {
            this.NameText = nameText;
            this.IdText = idText;
            this.Islogin = islogin;
            this.Isonline = isonline;
        }

        public String getIdText() {
            return IdText;
        }

        public void setIdText(String idText) {
            this.IdText = idText;
        }

        public String getNameText() {
            return NameText;
        }

        public void setNameText(String nameText) {
            this.NameText = nameText;
        }

        public boolean isIslogin() {
            return Islogin;
        }

        public void setIslogin(boolean islogin) {
            Islogin = islogin;
        }

        public boolean isIsonline() {
            return Isonline;
        }

        public void setIsonline(boolean isonline) {
            Isonline = isonline;
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

    private static class UserListAdapter extends RecyclerView.Adapter<ViewHolder> {

        private OnItemClickListener mItemClickListener;
        private OnItemTouchListener mItemTouchListener;


        public interface OnItemClickListener {
            void onClick(int position);

            void onLongClick(int position);
        }

        public interface OnItemTouchListener {
            void onTouch(int position, MotionEvent motionEvent);

        }

        public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
            this.mItemClickListener = onItemClickListener;
        }

        public void setOnItemTouchListener(OnItemTouchListener onItemTouchListener) {
            this.mItemTouchListener = onItemTouchListener;
        }

        private List<UserItem> mUsers = new ArrayList<>();

        UserListAdapter(List<UserItem> users) {
            mUsers = users;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
            return new ViewHolder(itemView);
            //          return new ViewHolder(LayoutInflater.from(parent.getContext()), parent);
        }

        private String selectedId;
        private View lastSelectedView;

        public void selectItem(String id) {
//            this.selectedId = id;
            int position = 0;
            for (UserItem usr : mUsers) {
                if (usr.getIdText().equals(id)) {
                    Log.d(TAG, "selectItem position=" + position);
                    notifyItemChanged(position);
                    return;
                }
                position++;
            }

        }

        public int getItemIndexbById(String id) {
            int position = 0;
            for (UserItem usr : mUsers) {
                if (usr.getIdText().equals(id)) break;
                position++;
            }
            Log.d(TAG, "getItemIndexbById position=" + position);

            return position;
        }

        public void setItemSendMsg(String id) {
//            this.selectedId = id;
        }

        public void onBindViewHolder(ViewHolder holder, final int position) {

            Log.d(TAG, "onBindViewHolder position=" + position + ";Isonline=" + mUsers.get(position).Isonline);
            Log.d(TAG, "onBindViewHolder last_recv_voice_id=" + last_recv_voice_id + ";last_send_voice_id=" + last_send_voice_id);

            holder.Nametext.setText(mUsers.get(position).getNameText());
            holder.Idtext.setText(mUsers.get(position).getIdText());

            holder.Nametext.setBackgroundColor(getListUserActivity().getResources().getColor(R.color.white));
            holder.Idtext.setBackgroundColor(getListUserActivity().getResources().getColor(R.color.white));
            holder.Idtext.setTextColor(getListUserActivity().getResources().getColor(R.color.dividerColor));

            if (!mUsers.get(position).Isonline) {
                holder.Nametext.setTextColor(getListUserActivity().getResources().getColor(R.color.dividerColor));
            } else {
                holder.Nametext.setTextColor(getListUserActivity().getResources().getColor(R.color.black));

//                //set last recv voice
//                if(mUsers.get(position).getIdText().equals(last_recv_voice_id)){
//                    holder.Idtext.setBackgroundColor(getListUserActivity().getResources().getColor(R.color.green));
//                }
//
//                //set last send voice
//                if(mUsers.get(position).getIdText().equals(last_send_voice_id)){
//                    if(is_last_send_voice_ok)
//                        holder.Nametext.setBackgroundColor(getListUserActivity().getResources().getColor(R.color.blue));
//                    else
//                        holder.Nametext.setBackgroundColor(getListUserActivity().getResources().getColor(R.color.red));
//                }
            }
            //set last recv voice
            if (mUsers.get(position).getIdText().equals(last_recv_voice_id)) {
                holder.Idtext.setBackgroundColor(getListUserActivity().getResources().getColor(R.color.green));
            }
            //set last send voice
            if (mUsers.get(position).getIdText().equals(last_send_voice_id)) {
                if (is_last_send_voice_ok)
                    holder.Nametext.setBackgroundColor(getListUserActivity().getResources().getColor(R.color.blue));
                else
                    holder.Nametext.setBackgroundColor(getListUserActivity().getResources().getColor(R.color.red));
            }

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

            if (mItemTouchListener != null) {
                holder.itemView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        mItemTouchListener.onTouch(position, motionEvent);
                        return false;
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
            return mUsers.size();
        }

        void addUserItem(UserItem user) {
//            String upperString = result.substring(0,1).toUpperCase() + result.substring(1);   //patli20180706
//            Log.d("test", "result.substring(0,1).toUpperCase()=" + result.substring(0,1).toUpperCase());
            if (!isExistUser(user.getIdText())) {
                mUsers.add(0, user);
                notifyItemInserted(0);
            }
        }

        void SetUserId(int pos, String result) {
//            String upperString = result.substring(0,1).toUpperCase() + result.substring(1);   //patli20180706
            Log.d("test", "SetResultServer pos=" + pos + "; str=" + result);
            mUsers.get(pos).setIdText(result);
        //    notifyDataSetChanged();
            //        notifyItemInserted(0);
        }

        void SetUserName(int pos, String name) {
//            String upperString = result.substring(0,1).toUpperCase() + result.substring(1);   //patli20180706
//            Log.d("test", "result.substring(0,1).toUpperCase()=" + result.substring(0,1).toUpperCase());
            Log.d("test", "SetResultClient pos=" + pos + "; str=" + name);
            mUsers.get(pos).setNameText(name);
    //        notifyDataSetChanged();

//            notifyItemInserted(0);
        }

        String GetUserId(int pos) {
//            String upperString = result.substring(0,1).toUpperCase() + result.substring(1);   //patli20180706
            Log.d("test", "GetUserId pos=" + pos);
            return mUsers.get(pos).getIdText();
            //        notifyItemInserted(0);
        }

        String GetUserName(int pos) {
//            String upperString = result.substring(0,1).toUpperCase() + result.substring(1);   //patli20180706
            Log.d("test", "GetUserName pos=" + pos);
            return mUsers.get(pos).getNameText();
            //        notifyItemInserted(0);
        }

        public List<UserItem> getmUsers() {
            return mUsers;
        }

        public boolean isExistUser(String id) {
            for (UserItem usr : mUsers) {
                if (usr.getIdText().contentEquals(id)) return true;
            }
            return false;
        }

        public String getUserNameById(String id) {
            Log.d("test", "getUserNameById id=" + id);
            for (UserItem usr : mUsers) {
                if (usr.getIdText().contentEquals(id)) {
                    return usr.getNameText();
                }
            }
            return "";
        }

        void SetUserOnline(String id, boolean is_online) {
            Log.d("test", "SetUserOnline id=" + id);
            for (UserItem usr : mUsers) {
                if (usr.getIdText().contentEquals(id)) {
                    usr.setIslogin(is_online);
                    usr.setIsonline(is_online);
                }
            }
            //         notifyDataSetChanged();
        }

        boolean GetUserOnline(String id) {
            Log.d("test", "GetUserOnline id=" + id);
            for (UserItem usr : mUsers) {
                if (usr.getIdText().contentEquals(id)) {
                    return usr.isIsonline();
                }
            }
            return false;
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

}



