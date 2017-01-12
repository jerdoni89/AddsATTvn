package com.attvn.adtruevn.model;

/**
 * Created by app on 1/4/17.
 */

public class SessionInfo {
    private String sessid;
    private String session_name;

    public SessionInfo() {
    }

    public String getSessid() {
        return sessid;
    }

    public void setSessid(String sessid) {
        this.sessid = sessid;
    }

    public String getSession_name() {
        return session_name;
    }

    public void setSession_name(String session_name) {
        this.session_name = session_name;
    }
}
