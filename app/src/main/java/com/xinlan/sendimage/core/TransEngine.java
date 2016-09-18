package com.xinlan.sendimage.core;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by panyi on 16/9/18.
 */
public class TransEngine {
    public static final int SEND_TO_PORT = 8964;
    public static final int RECEIVE_PORT = 8965;

    private static final int STATUS_IDLE = 0;//空闲状态
    private static final int STATUS_ANALYSIS = 1;//文件解析中...
    private static final int STATUS_SENDING = 2;//发送中...

    private TransEngine() {

    }

    private static TransEngine mInstance;

    private int mStatus = STATUS_IDLE;

    private String mSendToHost;
    private int mSendToHostPort = 8964;

    public static synchronized TransEngine getInstance() {
        if (mInstance == null) {
            mInstance = new TransEngine();
        }
        return mInstance;
    }


    public void addTask(final String path,final String address) throws RuntimeException{
        if(mStatus != STATUS_IDLE){
            throw new RuntimeException("error 当前正在发送...");
        }

        this.mSendToHost = address;

        DatagramSocket socket = null;
        try {
            this.mStatus = STATUS_ANALYSIS;
            socket = new DatagramSocket(RECEIVE_PORT);
            InetAddress serverAddress = InetAddress.getByName(mSendToHost);

            Pack preSendPck = Pack.createPrePackage(new File(path));
            String preSendJson = JSON.toJSONString(preSendPck);

            Map<Integer,Pack> packs = parseFile(new File(path));
            byte data[] = preSendJson.getBytes();
            DatagramPacket pck = new DatagramPacket (data , data.length ,
                    serverAddress , SEND_TO_PORT);
            socket.send(pck);

            //receve ask
            

            this.mStatus = STATUS_SENDING;

            socket.close();
        } catch (SocketException e) {
            e.printStackTrace();
            handleOnException();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            handleOnException();
        } catch (IOException e) {
            e.printStackTrace();
            handleOnException();
        }catch ( Exception e){
            handleOnException();
        }

        this.mStatus = STATUS_IDLE;
    }

    private void handleOnException(){
        this.mStatus = STATUS_IDLE;
    }

    private String receiveData(Socket socket){

        return "";
    }

    protected Map<Integer,Pack> parseFile(final File file){
        Map<Integer,Pack> packs = new HashMap<Integer,Pack>();

        InputStream in = null;

        try {
            //System.out.println("以字节为单位读取文件内容，一次读多个字节：");
            // 一次读多个字节
            byte[] tempbytes = new byte[Pack.TRUNK_SIZE];
            int byteread = 0;
            in = new FileInputStream(file);
            // 读入多个字节到字节数组中，byteread为一次读入的字节数
            int uid_index = 1;
            while ((byteread = in.read(tempbytes)) != -1) {
                System.out.write(tempbytes, 0, byteread);
                Pack p = Pack.createDataPackage(uid_index,tempbytes);
                packs.put(uid_index,p);
                uid_index++;
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e1) {
                }
            }
        }

        return packs;
    }
}//end class
