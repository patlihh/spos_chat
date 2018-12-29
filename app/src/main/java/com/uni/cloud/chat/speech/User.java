package com.uni.cloud.chat.speech;

public class User {
    String id;
    String name;
    boolean login;
    boolean online;
    boolean recvok;
    boolean IsLastSend;
    boolean IsLastRecv;

    public User(String id, String name, boolean islogin, boolean isonline) {
        this.id = id;
        this.name = name;
        this.login = islogin;
        this.online = isonline;
    }

    public User(String id, String name, boolean islogin, boolean isonline, boolean isrecvok) {
        this.id = id;
        this.name = name;
        this.login = islogin;
        this.online = isonline;
        this.recvok= isrecvok;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isLogin() {
        return login;
    }

    public void setLogin(boolean login) {
        this.login = login;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public boolean isRecvok() {
        return recvok;
    }

    public void setRecvok(boolean recvok) {
        this.recvok = recvok;
    }

    public boolean isLastSend() {
        return IsLastSend;
    }

    public void setLastSend(boolean lastSend) {
        IsLastSend = lastSend;
    }
}
