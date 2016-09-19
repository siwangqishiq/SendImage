package com.xinlan.sendimage;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.xinlan.sendimage.core.Pack;
import com.xinlan.sendimage.core.TransEngine;
import com.xinlan.sendimage.select.SelectPictureActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final int SELECT_IMAGE_CODE = 1;
    private View mSelectBtn;
    private View mSendBtn;
    private TextView mStatusText;
    private EditText mIpText;
    private ImageView mImageView;
    private String mFilePath;

    private boolean isRuning = true;
    private Handler uiHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImageView = (ImageView) findViewById(R.id.image_view);
        mSelectBtn = findViewById(R.id.select_btn);
        mSelectBtn.setOnClickListener(this);

        mStatusText = (TextView) findViewById(R.id.status_text);
        mIpText = (EditText) findViewById(R.id.ip_address);

        mSendBtn = findViewById(R.id.send_btn);
        mSendBtn.setOnClickListener(this);


        TransEngine.getInstance().setStatusView(mStatusText);
        //启动一个接收消息的线程
        Executors.newFixedThreadPool(1).execute(new Runnable() {
            @Override
            public void run() {
                DatagramSocket socket = null;
                try {
                    socket = new DatagramSocket(8964);
                    while (isRuning) {
                        listenNet(socket);
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                } finally {
                    if (socket != null) {
                        socket.close();
                    }
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.select_btn:
                SelectPictureActivity.start(this, SELECT_IMAGE_CODE);
                //finish();
                break;
            case R.id.send_btn:
                doSendAction();
                break;
        }//end switch
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            mFilePath = data.getStringExtra("imgPath");
            mStatusText.setText("选中图片:" + mFilePath);
        }

    }

    /*
     *
     *
     */
    protected void doSendAction() {

        new SendMsgTask().execute(mIpText.getText().toString().trim());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isRuning = false;
    }

    private final class SendMsgTask extends AsyncTask<String, Void, Integer> {
        @Override
        protected Integer doInBackground(String... params) {
            TransEngine.getInstance().addTask(mFilePath, params[0].toString().trim());
            return null;
        }
    }//end inner class

    //    private final class ReceiveTask extends AsyncTask<Void, Void, Void> {
//        @Override
//        protected Void doInBackground(Void... params) {
//
//            return null;
//        }
//    }
    private void listenNet(DatagramSocket socket) {
        FileOutputStream fos = null;
        try {
            Pack pack = TransEngine.receivePack(socket, true);
            if (pack.getType() == Pack.TYPE_PRE_SEND) {
                int uid = pack.getUid();
                String toAddress = pack.getDatagramPacket().getAddress().getHostAddress();
                int port = pack.getDatagramPacket().getPort();
                uid++;

                pack.setDatagramPacket(null);
                System.out.println(JSON.toJSONString(pack));


                final String filepath = getInnerSDCardPath() + File.separator + pack.getFilename();

                fos = new FileOutputStream(filepath);

                for (; uid <= pack.getTrunkNum(); uid++) {
                    Pack askPack = Pack.createAskPackage(uid);
                    TransEngine.sendPack(socket, askPack, toAddress, port);

                    Pack dataPack = TransEngine.receivePack(socket, false);
                    //System.out.println("received = " + " uid = " + dataPack.getUid() + "  trunkNum = " + dataPack.getTrunkNum());
                    showStatus("接收数据包 = " + dataPack.getUid()+"  "+dataPack.getType()+" "+dataPack.getData());
                    fos.write(dataPack.getData());
                }//end for uid

                if (uid > pack.getTrunkNum()) {
                    Pack endPack = Pack.createEndPackage();
                    TransEngine.sendPack(socket, endPack, toAddress, port);
                    showStatus("接收图片成功   保存路径于" + filepath);
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            ImageLoader.getInstance().displayImage("file://" + filepath, mImageView);
                        }
                    });
                }

            } else {

            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void showStatus(final String text) {
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                mStatusText.setText(text);
            }
        });
    }

    /**
     * 获取内置SD卡路径
     * @return
     */
    public static String getInnerSDCardPath() {
        return Environment.getExternalStorageDirectory().getPath();
    }
}//end class
