package com.uni.cloud.chat.misc;

public class Option {

    public static final String host = "www.gubstech.com";   //"18.191.92.76";
    public static final int port = 61055;
    public static final int ssl_port = 61056;
    public static final String test_host =  "139.224.221.196";
    public static final int test_port = 61055;      //"139.224.221.196":61056   192.168.10.82:61055;
    public static final String  dns_name =  "gubstech.com";

    public static final int TTS_FEMALE = 2;
    public static final int TTS_MP3_AUDIO_CODE = 2;

    public static final boolean Log_IsOpen = false;

    public static final boolean FlyTek_IsOpen = false;

    public static final boolean Ssl_IsOpen = true;   // true;   // true;

    public static final boolean test_IsOpen = true;

    public static final boolean Speex_IsOpen = false;

    public static final boolean FullScreen_IsOpen = false;

    public static final boolean Heart_IsOpen = true;

//    public static final String src_id = "U1536825106";
//    public static final String dest_id="U1536825121";
    public static final boolean IsK8s_test = true;
    public static final int k8s_test_port = 6001;
    public static final String k8s_test_host =  "18.191.92.76";         //"139.224.221.196";     //"www.gubstech.com";


    public static final String src_id = "U1536825121";
    public static final String dest_id="U1536825106";

    public static final String src_password= "1234567";

    public class UserStatus{
        public static final int   LOGOUT = 0;
        public static final int LOGIN  = 1;
    };

    public class SendMsgRes{
        public static final int SEND_MSG_OK               = 0;
        public static final int SEND_MSG_USER_LOGOUT      = 1;
        public static final int SEND_MSG_NO_DEST_USER     = 2;
        public static final int SEND_MSG_DEST_USER_LOGOUT = 3;
        public static final int SEND_MSG_SERVER_RECV_ERR = 4;
    }

    public static final int heart_time= 5*60;      //s

    public static final int heart_fail_max_num= 3;      //s

    public class Opus{
        public static final boolean Is_Opus               = true;   //true;
        public static final int SAMPLE_RATE      = 8000;
        public static final int NUM_CHANNELS     = 1;
        public static final int FRAME_SIZE = 160;

    }

}
