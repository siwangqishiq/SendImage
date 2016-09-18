package com.xinlan.sendimage;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.xinlan.sendimage.core.TransEngine;
import com.xinlan.sendimage.select.SelectPictureActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    public static final int SELECT_IMAGE_CODE = 1;
    private View mSelectBtn;
    private View mSendBtn;
    private TextView mStatusText;
    private EditText mIpText;

    private String mFilePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSelectBtn = findViewById(R.id.select_btn);
        mSelectBtn.setOnClickListener(this);

        mStatusText = (TextView)findViewById(R.id.status_text);
        mIpText = (EditText)findViewById(R.id.ip_address);

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
     *
     *
     */
    protected void doSendAction(){
        new SendMsgTask().execute(mIpText.getText().toString().trim());
    }

    private final class SendMsgTask extends AsyncTask<String,Void,Integer>{

        @Override
        protected Integer doInBackground(String... params) {
            TransEngine.getInstance().addTask(mFilePath,params[0].toString().trim());
            return null;
        }
    }//end inner class

}//end class
