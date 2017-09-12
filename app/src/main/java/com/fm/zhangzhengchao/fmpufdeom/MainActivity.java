package com.fm.zhangzhengchao.fmpufdeom;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.nfc.FormatException;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;

import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.fm.zhangzhengchao.fmpufdeom.model.Data;
import com.fm.zhangzhengchao.fmpufdeom.model.Response;
import com.fm.zhangzhengchao.fmpufdeom.utils.HexstringAndBytesConvert;
import com.fm.zhangzhengchao.fmpufdeom.utils.MyRandom;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    // NFC适配器
    private NfcAdapter nfcAdapter = null;
    // 传达意图
    private PendingIntent pi = null;
    // 滤掉组件无法响应和处理的Intent
    private IntentFilter tagDetected = null;
    // 是否支持NFC功能的标签
    private boolean isNFC_support = false;
    // NFC TAG
    private Tag tagFromIntent;
    private NfcV nfcV;
    private boolean isConnect = false;
    //SOCKET
    private Socket mSocket;
    //服务器返回数据
    String respond;
    private ProgressDialog progressDialog1;
    @BindView(R.id.promt)
    TextView textInfo;
    @BindView(R.id.tv_tagid)
    TextView tv_tagid;
    @BindView(R.id.tv_tag_status)
    TextView tv_tag_status;
    @BindView(R.id.connectServer)
    TextView tv_conncetServer;
    @BindView(R.id.sv_tag)
    ScrollView sv_tag;
    @BindView(R.id.sv_server)
    ScrollView sv_server;
    @BindView(R.id.tb)
    Toolbar tb;
    @OnClick(R.id.tv_title_right)
    public void clear(){
        tv_conncetServer.setText("");
        textInfo.setText("");
        tv_tag_status.setTextColor(Color.RED);
        tv_tag_status.setText("请靠近标签");
        tv_tagid.setText("");
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //绑定控件
        ButterKnife.bind(this);
        //初始化TOOLBAR
        setSupportActionBar(tb);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        //初始化NFC
        initNFCData();
        //初始化网络
        initNetwork();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSocket.disconnect();
    }



    private void initNetwork() {
        String url = "http://101.95.168.50:15698";
        try {
            mSocket = IO.socket(url);
        } catch (URISyntaxException e) {
            Log.i(TAG, "出错啦~~~`");
            throw new RuntimeException(e);

        }
        Emitter.Listener onNewMessage = new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                respond = (String) args[0];

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int result = -2;
                        String recoveredKey = null;
                        String decryptedToken=null;
                        try {
                            JSONObject jsonObject = new JSONObject(new JSONTokener(respond));
                            result = jsonObject.getInt("result");
                            recoveredKey = jsonObject.getString("recoveredKey");
                            decryptedToken= jsonObject.getString("decryptedToken");
                            Log.i(TAG, "结果为：" + result);
                            Log.i(TAG, "恢复秘钥为：" + recoveredKey);
                        } catch (JSONException e) {
                            Log.i(TAG, "解析出错");
                            e.printStackTrace();
                        }
                        tv_conncetServer.append("恢复秘钥为：" + recoveredKey + "\n");

                        tv_conncetServer.append("解密结果:"+decryptedToken+"\n");
                        if (result == 1) {
                            tv_conncetServer.append("认证结果:" + "成功" + "\n");
                        } else {
                            tv_conncetServer.append("认证结果:" + "失败" + "\n");
                        }

                        sv_server.post(new Runnable() {
                            public void run() {
                                sv_server.fullScroll(View.FOCUS_DOWN);
                            }
                        });
                        progressDialog1.dismiss();
                    }
                });

            }
        };
        Emitter.Listener errormessage = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.i(TAG, "连接失败");
                mSocket.close();
                isConnect = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                    }
                });

            }
        };
        Emitter.Listener successmessage = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                isConnect = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                    }
                });

            }
        };

        mSocket.on("task.result", onNewMessage);
        mSocket.on("error", errormessage);
        mSocket.on("disconnect", errormessage);
        mSocket.on("connect_timeout", errormessage);
        mSocket.on("connect_error", errormessage);
        mSocket.on("connect", successmessage);
        mSocket.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isNFC_support == false) {
            // 如果设备不支持NFC或者NFC功能没开启，就return掉
            return;
        }
        // 开始监听NFC设备是否连接
        startNFC_Listener();

        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(this.getIntent()
                .getAction())) {
            // 注意这个if中的代码几乎不会进来，因为刚刚在上一行代码开启了监听NFC连接，下一行代码马上就收到了NFC连接的intent，这种几率很小
            // 处理该intent
            processIntent(this.getIntent());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isNFC_support == true) {
            // 当前Activity如果不在手机的最前端，就停止NFC设备连接的监听
            stopNFC_Listener();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // 当前app正在前端界面运行，这个时候有intent发送过来，那么系统就会调用onNewIntent回调方法，将intent传送过来
        // 我们只需要在这里检验这个intent是否是NFC相关的intent，如果是，就调用处理方法
        Log.i(TAG, "进入NEWINTENT");
        Log.i(TAG, "action为：" + intent.getAction());
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()) ||
                NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            processIntent(intent);
        }
    }


    private void initNFCData() {
        // 初始化设备支持NFC功能
        isNFC_support = true;
        // 得到默认nfc适配器
//        NfcManager manager = (NfcManager) getSystemService(Context.NFC_SERVICE);
//        nfcAdapter = manager.getDefaultAdapter();
        nfcAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
        // 提示信息定义
        String metaInfo = "";
        // 判定设备是否支持NFC或启动NFC
        if (nfcAdapter == null) {
            Log.i(TAG,"nfcADAPTER为空");
            metaInfo = "设备不支持NFC！";
            Toast.makeText(this, metaInfo, Toast.LENGTH_SHORT).show();
            isNFC_support = false;
        }
        if (!nfcAdapter.isEnabled()) {
            metaInfo = "请在系统设置中先启用NFC功能！";
            Toast.makeText(this, metaInfo, Toast.LENGTH_SHORT).show();
            isNFC_support = false;
        }

        if (isNFC_support == true) {
            init_NFC();
        } else {
            textInfo.setTextColor(Color.RED);
            textInfo.setText(metaInfo);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {



        return super.onCreateOptionsMenu(menu);
    }


    public void processIntent(Intent intent) {
        if (isNFC_support == false) {
            return;
        }
        // 取出封装在intent中的TAG
        tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        textInfo.setTextColor(Color.BLUE);
        tv_tag_status.setTextColor(Color.GREEN);
        tv_tag_status.setText("获取标签成功");
        String tagId = HexstringAndBytesConvert.bytesToHex(tagFromIntent.getId());
        tv_tagid.setText("标签ID为:" + tagId);
        Toast.makeText(this, "找到卡片", Toast.LENGTH_SHORT).show();
        nfcV = NfcV.get(tagFromIntent);
        //生成随机数

        String ramrandom = MyRandom.generate4ByteRandomNumber();

        String apdu1 = "22d61d" + tagId + "000d"+ramrandom;
        String ramData = readfromTag(nfcV, apdu1);
        displayRespond(apdu1, ramData);

        String authrandom=MyRandom.generate4ByteRandomNumber();

        String apdu2 = "22b21d" + tagId + authrandom;
        String authData = readfromTag(nfcV, apdu2);
        displayRespond(apdu2, authData);
        sv_tag.post(new Runnable() {
            public void run() {
                sv_tag.fullScroll(View.FOCUS_DOWN);
            }
        });
        if (isConnect) {
            showMyDialog();
            sendDataToserver(tagId, ramData, authData,ramrandom,authrandom);
        } else {
            tv_conncetServer.append("请先连接服务器");
        }
    }

    private void showMyDialog() {
        //创建ProgressDialog对象
        progressDialog1 = new ProgressDialog(
                MainActivity.this);
        //设置进度条风格，风格为圆形，旋转的
        progressDialog1.setProgressStyle(
                ProgressDialog.STYLE_SPINNER);
        //设置ProgressDialog 标题
        progressDialog1.setTitle("正在从服务器获取信息...");
        //设置ProgressDialog 提示信息
        progressDialog1.setMessage("请稍等...");
        //设置ProgressDialog 标题图标
        progressDialog1.setIcon(android.R.drawable.btn_star);
        //设置ProgressDialog 的进度条是否不明确
        progressDialog1.setIndeterminate(false);
        //设置ProgressDialog 是否可以按退回按键取消
        progressDialog1.setCancelable(true);
        //设置取消按钮
//        progressDialog.setButton("取消",
//        new ProgressDialogButtonListener());
//        让ProgressDialog显示
        progressDialog1.show();
    }

    private void sendDataToserver(String tagId, String ramData, String authData,String ramRandonm,String authRandom) {
        Data data = new Data();
        data.setUid(tagId);
        ramData = ramData.substring(2, ramData.length());
        data.setRamData(ramData);
        data.setRamRandom(ramRandonm);
        authData = authData.substring(2, authData.length());
        data.setAuthData(authData);
        data.setAuthRandom(authRandom);
        Response response = new Response();
        response.setData(data);
        Gson gson = new Gson();
        String jsonStr = gson.toJson(response, Response.class);
        tv_conncetServer.append("请求认证：" + jsonStr + "\n");
        mSocket.emit("reader.response", jsonStr);

    }

    //展示数据
    private void displayRespond(String apdu, String respond) {
        if (apdu.contains("22d61d")) {
            textInfo.append("读取SRAM：" + apdu + "\n");
            textInfo.append("返回结果:"+respond+"\n");
        } else if (apdu.contains("22b21d")) {
            textInfo.append("单向认证：" + apdu + "\n");
            textInfo.append("返回结果:"+respond+"\n");
        }
    }


    //读取数据
    private String readfromTag(NfcV nfcV, String apdu) {
        String read = null;
        try {
            nfcV.connect();
            read = read(tagFromIntent, apdu);
        } catch (IOException e) {
            Log.e(TAG, "读取时报错");
            return "读取时报错,请重试" + e.getMessage();
        } catch (FormatException e) {
            e.printStackTrace();
        } finally {
            try {
                nfcV.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        return read;
    }

    // 读取方法
    private String read(Tag tag, String apdu) throws IOException, FormatException {
        if (tag != null) {
            //打开连接
            byte[] bytes = nfcV.transceive(HexstringAndBytesConvert.hexToBytes(apdu));
            String read = HexstringAndBytesConvert.bytesToHex(bytes);
            return read;

        } else {
            Toast.makeText(MainActivity.this, "设备与nfc卡连接断开，请重新连接...",
                    Toast.LENGTH_SHORT).show();
            return null;
        }

    }

    private void startNFC_Listener() {
        // 开始监听NFC设备是否连接，如果连接就发pi意图
        nfcAdapter.enableForegroundDispatch(this, pi,
                /*new IntentFilter[] { tagDetected }*/null, null);
        Log.i(TAG, "开启监听了");
    }

    private void stopNFC_Listener() {
        // 停止监听NFC设备是否连接
        nfcAdapter.disableForegroundDispatch(this);
    }

    private void init_NFC() {
        // 初始化PendingIntent，当有NFC设备连接上的时候，就交给当前Activity处理
        pi = PendingIntent.getActivity(this, 0, new Intent(this, getClass())
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        // 新建IntentFilter，使用的是第二种的过滤机制
        tagDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
    }
}
