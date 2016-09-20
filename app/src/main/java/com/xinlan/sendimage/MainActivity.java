package com.xinlan.sendimage;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.xinlan.sendimage.core.TransEngine;
import com.xinlan.sendimage.select.SelectPictureActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final int SELECT_IMAGE_CODE = 1;
    private View mSelectBtn;
    private View mSendBtn;
    private TextView mStatusText;
    private EditText mIpText;
    private ImageView mImageView;
    private String mFilePath;

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
        TransEngine.getInstance().registerOnImageSuccessReceive(new TransEngine.IReceiveImageSuccess() {
            @Override
            public void onReceiveImageSuccess(String filepath) {
                ImageLoader.getInstance().displayImage("file://" + filepath, mImageView);
            }
        });
        //启动一个接收消息的线程
        TransEngine.getInstance().startReceiveTask();
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
        final String toAddress = mIpText.getText().toString().trim();
        final String filepath = mFilePath;
        TransEngine.getInstance().addTask(filepath, toAddress);
        //new SendMsgTask().execute(mIpText.getText().toString().trim());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TransEngine.getInstance().terminateReceiveTask();
    }
}//end class
