package com.uni.cloud.chat.login;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.ListPopupWindow;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.uni.cloud.chat.ChatApplication;
import com.uni.cloud.chat.R;
import com.uni.cloud.chat.misc.Option;


public class LoginFragment extends Fragment implements OnLoginListener {
    private static final String TAG = "LoginFragment";

    EditText et_account;
    EditText et_password;

    String account_id;
    String password;

    TextView version;

    LoginActivity parentActivity ;
    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView" );

        View inflate = inflater.inflate(R.layout.fragment_login, container, false);
//        inflate.findViewById(R.id.forgot_password).setOnClickListener(v ->
//                Toast.makeText(getContext(), "Forgot password clicked", Toast.LENGTH_SHORT).show());
        et_account = inflate.findViewById(R.id.et_account);
        et_password = inflate.findViewById(R.id.et_password);

        et_password.setText("1234567");

        parentActivity = (LoginActivity ) getActivity();

        version = inflate.findViewById(R.id.version);
        version.setText("v"+ChatApplication.getLocalVersionName(this.parentActivity));

        et_account.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(b){
                    showListPopulWindow();
                }
            }
        });


        inflate.findViewById(R.id.but_login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
       //         Toast.makeText(getContext(), "Login clicked", Toast.LENGTH_SHORT).show();
                account_id = et_account.getText().toString();
                password = et_password.getText().toString();
                if (account_id.length() == 0) {
                    Toast.makeText(getContext(), "account id not null", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.length() == 0) {
                    Toast.makeText(getContext(), "password not null", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d(TAG, "parentActivity=" + parentActivity);
                parentActivity.mSpeechService.Login(account_id,password);

     //           mSpeechService.Login(Option.src_id, "patli", "11.22.33.44.55.66");
            }
        });


        try {
            //Class myClass=Class.forName("com.uni.cloud.chat.speech.ListUserActivity");
            Intent intent =new Intent();
 //           intent.setClass(getActivity(), myClass);
            intent.setClassName("com.uni.cloud.chat.speech", "com.uni.cloud.chat.speech.ListUserActivity");
            if (getActivity().getPackageManager().resolveActivity(intent, 0) == null) {
                // 系统中不存在这个activity时采取的操作
                Log.d(TAG, "false" );
            }else{
                //系统中存在这个activity时采取的操作
                //startActivity(intent);
                Log.d(TAG, "true" );
            }


        }catch (Exception e){
            e.printStackTrace();
        }
        return inflate;
    }

    private void showListPopulWindow(){
        final String[] list = {"U1536825106", "U1536825121", "U1539571655","U1539571715","U1539571725","U1539571736"};//要填充的数据
        final ListPopupWindow listPopupWindow;
        listPopupWindow = new ListPopupWindow(getContext());
        listPopupWindow.setAdapter(new ArrayAdapter<String>(getContext(),android.R.layout.simple_list_item_1, list));//用android内置布局，或设计自己的样式
        listPopupWindow.setAnchorView(et_account);//以哪个控件为基准，在该处以logId为基准
        listPopupWindow.setModal(true);


        listPopupWindow.setOnItemClickListener(new AdapterView.OnItemClickListener() {//设置项点击监听
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
           //     et_account.setTextSize(10);
                et_account.setText(list[i]);//把选择的选项内容展示在EditText上
                listPopupWindow.dismiss();//如果已经选择了，隐藏起来
            }
        });
        listPopupWindow.show();//把ListPopWindow展示出来
    }

//    public void doClick(View view) {
//        switch (view.getId()) {
//            case R.id.but_login:
//                Toast.makeText(getContext(), "Login clicked", Toast.LENGTH_SHORT).show();
//                if (et_account.getText().length() == 0) {
//                    Toast.makeText(getContext(), "account id not null", Toast.LENGTH_SHORT).show();
//                    return;
//                }
//
//                if (et_password.getText().length() == 0) {
//                    Toast.makeText(getContext(), "password not null", Toast.LENGTH_SHORT).show();
//                    return;
//                }
//                break;
//        }
//    }

    @Override
    public void login() {
        Toast.makeText(getContext(), "Login", Toast.LENGTH_SHORT).show();
    }

    public String GetloginName() {
        return  account_id;
    }

    public String GetloginPassword() {
        return  password;
    }
}
