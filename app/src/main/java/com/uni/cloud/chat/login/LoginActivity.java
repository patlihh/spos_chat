package com.uni.cloud.chat.login;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.uni.cloud.chat.ChatApplication;
import com.uni.cloud.chat.R;
import com.uni.cloud.chat.databinding.ActivityLoginBinding;
import com.uni.cloud.chat.indexrecyclerview.UniRecylerActivity;
import com.uni.cloud.chat.log.Logger;
import com.uni.cloud.chat.misc.Option;
import com.uni.cloud.chat.speech.ListUserActivity;
import com.uni.cloud.chat.speech.MainActivity;
import com.uni.cloud.chat.speech.ServiceUtils;
import com.uni.cloud.chat.speech.SpeechService;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static com.uni.cloud.chat.login.FlexibleFrameLayout.ORDER_LOGIN_STATE;
import static com.uni.cloud.chat.login.FlexibleFrameLayout.ORDER_SIGN_UP_STATE;


public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private ActivityLoginBinding binding;
    public ChatApplication mApp;

    public SpeechService mSpeechService;
    LoginFragment topLoginFragment;


    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            mSpeechService = SpeechService.from(binder);

            Log.d(TAG, "mSpeechService=" + mSpeechService);
            mSpeechService.setLoginListener(mLoginListener);
//            mStatus.setVisibility(View.VISIBLE);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSpeechService = null;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        if (ServiceUtils.isServiceRunning(this, "com.uni.cloud.chat..speech.SpeechService")) {
//            startUserListActivity();
//        } else {
            mApp = (ChatApplication) getApplication();
            super.onCreate(savedInstanceState);
            binding = DataBindingUtil.setContentView(this, R.layout.activity_login);


            topLoginFragment = new LoginFragment();

            //       SignUpFragment topSignUpFragment = new SignUpFragment();

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.login_fragment, topLoginFragment)
//                .replace(R.id.sign_up_fragment, topSignUpFragment)
                    .commit();


            //       binding.loginFragment.setRotation(-90);

//        binding.button.setOnSignUpListener(topSignUpFragment);
            //       binding.button.setOnLoginListener(topLoginFragment);

//        binding.button.setOnButtonSwitched(isLogin -> {
//            binding.getRoot()
//                    .setBackgroundColor(ContextCompat.getColor(
//                            this,
//                            isLogin ? R.color.colorPrimary : R.color.secondPage));
//        });

            binding.loginFragment.setVisibility(VISIBLE);
//        }


    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        final View view = getWindow().getDecorView();

        final WindowManager.LayoutParams lp = (WindowManager.LayoutParams) view.getLayoutParams();

        lp.gravity = Gravity.CENTER;

        //       lp.width = 800;    //800;   //400;   //480;

        //      lp.height = 1000;    //1000;  //600;    //640;

        DisplayMetrics metrics = new DisplayMetrics();
        Display display = getWindowManager().getDefaultDisplay();
        display.getMetrics(metrics);

        Log.d("test", "widthPixels=" + metrics.widthPixels + ";heightPixels=" + metrics.heightPixels + ";xdpi=" + metrics.xdpi + ";ydpi=" + metrics.ydpi);

        if(!Option.FullScreen_IsOpen) {
            if (metrics.widthPixels <= 480) {
                lp.width = 400;    //800;   //400;   //480;
                lp.height = 420;    //1000;  //1200;    //640;
                Log.d("test", "widthPixels------------------0");
            } else if (metrics.widthPixels > 480 && metrics.widthPixels <= 720) {
                lp.width = 480;    //800;   //400;   //480;
                lp.height = 500;    //1000;  //1200;    //640;
                Log.d("test", "widthPixels------------------1");
            } else {
                lp.width = 800;    //800;   //400;   //480;
                lp.height = 820;    //1000;  //1200;    //640;
                Log.d("test", "widthPixels------------------2");
            }
        }else {
            lp.width = metrics.widthPixels;    //800;   //400;   //480;
            lp.height = metrics.heightPixels;    //1000;  //1200;    //640;
        }

        Log.d("test", "lp.width=" + lp.width + ";lp.height=" + lp.height);

        getWindowManager().updateViewLayout(view, lp);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart...");
        // Prepare Cloud Speech API
      bindService(new Intent(this, SpeechService.class), mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        if(mSpeechService !=null){
            mSpeechService.RemoveLoginListener(mLoginListener);
            unbindService(mServiceConnection);
            mSpeechService = null;
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        binding.loginFragment.setPivotX(binding.loginFragment.getWidth() / 2);
        binding.loginFragment.setPivotY(binding.loginFragment.getHeight());
//        binding.signUpFragment.setPivotX(binding.signUpFragment.getWidth() / 2);
//        binding.signUpFragment.setPivotY(binding.signUpFragment.getHeight());
    }

    public void switchFragment(View v) {
//        if (isLogin) {
//            binding.loginFragment.setVisibility(VISIBLE);
//            binding.loginFragment.animate().rotation(0).setListener(new AnimatorListenerAdapter() {
//                @Override
//                public void onAnimationEnd(Animator animation) {
//                    super.onAnimationEnd(animation);
////                    binding.signUpFragment.setVisibility(INVISIBLE);
////                    binding.signUpFragment.setRotation(90);
//                    binding.wrapper.setDrawOrder(ORDER_LOGIN_STATE);
//                }
//            });
//        } else {
//            binding.signUpFragment.setVisibility(VISIBLE);
//            binding.signUpFragment.animate().rotation(0).setListener(new AnimatorListenerAdapter() {
//                @Override
//                public void onAnimationEnd(Animator animation) {
//                    super.onAnimationEnd(animation);
//                    binding.loginFragment.setVisibility(INVISIBLE);
//                    binding.loginFragment.setRotation(-90);
//                    binding.wrapper.setDrawOrder(ORDER_SIGN_UP_STATE);
//                }
//            });
//        }
//
//        isLogin = !isLogin;
//        binding.button.startAnimation();
    }

    public void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
//                        EditText editText = (EditText) findViewById(R.id.editText);
//                        String message = editText.getText().toString();
//                        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
        finish();
    }

    public void startUniRecylerActivity() {
        Intent intent = new Intent(this, UniRecylerActivity.class);
//                        EditText editText = (EditText) findViewById(R.id.editText);
//                        String message = editText.getText().toString();
//                        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
        finish();
    }

    public void startUserListActivity() {
        Intent intent = new Intent(this, ListUserActivity.class);
//                        EditText editText = (EditText) findViewById(R.id.editText);
//                        String message = editText.getText().toString();
//                        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
        finish();
    }

    private final SpeechService.LoginListener mLoginListener =
            new SpeechService.LoginListener() {
                @Override
                public void onLogin(boolean ret) {
                    if (ret) {
                        Log.d(TAG, "onLogin success!");
//                        Toast.makeText(LoginActivity.this, "Login success!", Toast.LENGTH_SHORT).show();
//                        startMainActivity();
                        //    startUniRecylerActivity();
                      //注册或登录成功就将用户的信息保存到本地数据库中
                        mApp.isLogin = true;
                        startUserListActivity();
                    } else {
                        Log.d(TAG, "onLogin fail!");
                        //                       Toast.makeText(LoginActivity.this, "Login fail!", Toast.LENGTH_SHORT).show();

                    }
                }
            };

    public void saveLoginInfi() {
        SharedPreferences sp = getSharedPreferences("User", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit(); //SharedPreferences 本身不能读写数据，需要使用Editor

        Log.d(TAG, "onLogin name=" + topLoginFragment.GetloginName() + ";password=" + topLoginFragment.GetloginPassword());
        editor.putString("name", topLoginFragment.GetloginName());
        editor.putString("password", topLoginFragment.GetloginPassword());
        editor.commit(); //提交
    }

//    private boolean isExistMainActivity(Class<?> activity) {
//        Intent intent = new Intent(this, activity);
//        ComponentName cmpName = intent.resolveActivity(getPackageManager());
//        boolean flag = false;
//        Log.d(TAG, "cmpName name="+cmpName+"flag="+flag);
//        if (cmpName != null) { // 说明系统中存在这个activity    
//            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
//            List<ActivityManager.RunningTaskInfo> taskInfoList = am.getRunningTasks(10);//获取从栈顶开始往下查找的10个activity  
//            for (ActivityManager.RunningTaskInfo taskInfo : taskInfoList) {
//                if (taskInfo.baseActivity.equals(cmpName))// 说明它已经启动了    
//                    Log.d(TAG, "cmpName started");
//                    flag = true;
//                break;//跳出循环，优化效率  
//            }
//        }
//        Log.d(TAG, "cmpName flag="+flag);
//        return flag;
//    }

}



