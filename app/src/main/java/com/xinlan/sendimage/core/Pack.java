package com.xinlan.sendimage.core;

import java.io.File;

/**
 * Created by panyi on 16/9/18.
 */
public class Pack {
    public static final int TYPE_PRE_SEND = 1;
    public static final int TYPE_SENDING = 2;
    public static final int TYPE_ASK = 3;

    public static final int TRUNK_SIZE = 2*1024;

    private int type;
    private long filesize;
    private String filename;
    private int trunkNum;
    private int uid;
    private byte[] data;

    public static Pack createPrePackage(File file){
        Pack pck = new Pack();
        pck.setType(TYPE_PRE_SEND);
        pck.setFilesize(file.length());
        pck.setFilename(file.getName());
        pck.setUid(0);

        int trunkNum = (int)(file.length() / TRUNK_SIZE) + 1;
        pck.setTrunkNum(trunkNum);

        return pck;
    }

    public static Pack createAskPackage(int uid){
        Pack pck = new Pack();
        pck.setType(TYPE_ASK);
        pck.setUid(uid);
        return pck;
    }

    public static Pack createDataPackage(int uid,byte[] binaryData){
        Pack pck = new Pack();
        pck.setType(TYPE_SENDING);
        pck.setUid(uid);
        pck.setData(binaryData);
        return pck;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getFilesize() {
        return filesize;
    }

    public void setFilesize(long filesize) {
        this.filesize = filesize;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getTrunkNum() {
        return trunkNum;
    }

    public void setTrunkNum(int trunkNum) {
        this.trunkNum = trunkNum;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }


}//end class
