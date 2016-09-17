package com.xinlan.sendimage;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.xinlan.sendimage.select.SelectPictureActivity;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final int SELECT_IMAGE_CODE = 1;
    private View mSelectBtn;
    private View mSendBtn;
    private TextView mStatusText;

    private String mFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSelectBtn = findViewById(R.id.select_btn);
        mSelectBtn.setOnClickListener(this);

        mStatusText = (TextView)findViewById(R.id.status_text);

        mSendBtn = findViewById(R.id.send_btn);
        mSendBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.select_btn:
                SelectPictureActivity.start(this,SELECT_IMAGE_CODE);
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
        if(data!=null){
            mFilePath = data.getStringExtra("imgPath");
            mStatusText.setText("选中图片:"+mFilePath);
        }

    }

    /*
     * 发送操作
     * 1.  创建一个DatagramSocket对象

			DatagramSocket socket = new  DatagramSocket (4567);

		2.  创建一个 InetAddress ， 相当于是地址

			InetAddress serverAddress = InetAddress.getByName("想要发送到的那个IP地址");

		3.  这是随意发送一个数据

			String str = "hello";

		4.  转为byte类型

			byte data[] = str.getBytes();

 		5.  创建一个DatagramPacket 对象，并指定要讲这个数据包发送到网络当中的哪个地址，以及端口号

			DatagramPacket  package = new DatagramPacket (data , data.length , serverAddress , 4567);

		6.  调用DatagramSocket对象的send方法 发送数据

			 socket . send(package);
     *
     *
     */
    protected void doSendAction(){
        new SendMsgTask().execute(0);
    }

    private final class SendMsgTask extends AsyncTask<Integer,Void,Integer>{

        @Override
        protected Integer doInBackground(Integer... params) {
            try {
                DatagramSocket socket = new  DatagramSocket (8964);
                InetAddress serverAddress = InetAddress.getByName("192.168.1.8");
                String str =mFilePath;
                byte data[] = str.getBytes();
                DatagramPacket pck = new DatagramPacket (data , data.length , serverAddress , 8964);
                socket.send(pck);
                socket.close();
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }//end class
}//end class
