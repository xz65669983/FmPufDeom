package com.fm.zhangzhengchao.fmpufdeom.com.fm.zhangzhengchao.fmpufdeom.websocket;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;


import java.net.URI;

/**
 * Created by zhangzhengchao on 2017/8/24.
 */

public class PufWebSockerClient extends WebSocketClient {
    private static final String TAG = "PufWebSockerClient";

    public PufWebSockerClient(URI serverUri) {
        super(serverUri);
    }

    public PufWebSockerClient(URI serverUri, Draft protocolDraft) {
        super(serverUri, protocolDraft);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {

        Log.i(TAG,"打开连接：opened connection" );
    }

    @Override
    public void onMessage(String message) {
        Log.i(TAG,"收到参数:" +message);

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.i( TAG,"关闭连接"+"Connection closed by " + ( remote ? "remote peer" : "us" )
                +"---reason:"+reason);
    }

    @Override
    public void onError(Exception ex) {
        Log.e(TAG,"发生错误"+ex.getMessage());
    }
}
