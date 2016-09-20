package com.xinlan.sendimage.core;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by panyi on 16/9/18.
 */
public class TransEngine {
    public static final int SEND_TO_PORT = 8964;
    public static final int RECEIVE_PORT = 8965;

    public static final int SOCKET_TIME_OUT = 10 * 1000;

    private static final int STATUS_IDLE = 0;//空闲状态
    private static final int STATUS_ANALYSIS = 1;//文件解析中...
    private static final int STATUS_SENDING = 2;//发送中...

    private TransEngine() {

    }

    private static TransEngine mInstance;

    private int mStatus = STATUS_IDLE;

    private String mSendToHost;

    private DatagramSocket socket = null;

    private TextView mStatusView;

    private static Object lock = new Object();

    private boolean mReceiveTaskIsRuning = false;

    private ExecutorService threadPool;//任务线程池

    private Handler mUIHandler = new Handler(Looper.getMainLooper());

    public interface IReceiveImageSuccess {
        void onReceiveImageSuccess(final String filepath);
    }

    private IReceiveImageSuccess mReceiveAction;

    public static TransEngine getInstance() {
        if (mInstance == null) {
            synchronized (lock) {
                mInstance = new TransEngine();
            }
        }
        return mInstance;
    }

    public void addTask(final String path, final String address) throws RuntimeException {
        if (threadPool == null) {
            threadPool = Executors.newFixedThreadPool(2);
        }
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                doSendTask(path, address);
            }
        });
    }

    protected void doSendTask(final String path, final String address) {
        if (mStatus != STATUS_IDLE) {
            //throw new RuntimeException("error 当前正在发送...");
            return;
        }
        this.mSendToHost = address;

        try {
            this.mStatus = STATUS_ANALYSIS;
            socket = new DatagramSocket(RECEIVE_PORT);
            socket.setSoTimeout(SOCKET_TIME_OUT);
            Pack preSendPack = Pack.createPrePackage(new File(path));
            Map<Integer, Pack> packs = parseFile(new File(path), preSendPack);

            //send init pack
            sendPack(socket, preSendPack, mSendToHost, SEND_TO_PORT);

            this.mStatus = STATUS_SENDING;
            Pack receivePack = receivePack(socket, false);
            while (receivePack != null && receivePack.getType() != Pack.TYPE_END) {
                //System.out.println("packet = " + JSON.toJSONString(receivePack));
                if (receivePack.getType() == Pack.TYPE_ASK) {
                    Pack sendPack = packs.get(receivePack.getUid());
                    //sendPack.setData(null);
                    sendPack(socket, sendPack, mSendToHost, SEND_TO_PORT);
                    updateStatusView("发送数据包" + sendPack.getUid() + "  总量 = " + sendPack.getTrunkNum());
                    receivePack = receivePack(socket, false);
                }
            }//end while

            if (receivePack.getType() == Pack.TYPE_END) {
                updateStatusView("图片发送成功!");
            }
            //receive ask or end
            //Pack askPack = receivePack(socket, false);

            socket.close();
        } catch (SocketException e) {
            e.printStackTrace();
            handleOnException(e);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            handleOnException(e);
        } catch (IOException e) {
            e.printStackTrace();
            handleOnException(e);
        } catch (Exception e) {
            handleOnException(e);
        }
        this.mStatus = STATUS_IDLE;
    }

    private void handleOnException(Exception e) {
        updateStatusView("发送错误 : " + e);
        this.mStatus = STATUS_IDLE;
        if (socket != null) {
            socket.close();
        }
    }

    protected Map<Integer, Pack> parseFile(final File file, Pack preSendPack) {
        Map<Integer, Pack> packs = new HashMap<Integer, Pack>();

        InputStream in = null;
        try {
            //System.out.println("以字节为单位读取文件内容，一次读多个字节：");
            // 一次读多个字节
            byte[] buffer = new byte[Pack.TRUNK_SIZE];
            int byteread = 0;
            in = new FileInputStream(file);
            // 读入多个字节到字节数组中，byteread为一次读入的字节数
            int uid_index = 1;
            while ((byteread = in.read(buffer)) != -1) {
                //System.out.write(tempbytes, 0, byteread);
                byte[] b = new byte[byteread];
                System.arraycopy(buffer, 0, b, 0, b.length);
                Pack p = Pack.createDataPackage(uid_index, preSendPack.getTrunkNum(), b);
                packs.put(uid_index, p);
                uid_index++;
            }
            //System.out.println("uid_index = " + uid_index);
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

    /**
     * 开始监听任务
     */
    public void startReceiveTask() {
        //启动一个接收消息的线程
        if (threadPool == null) {
            threadPool = Executors.newFixedThreadPool(2);
        }

        mReceiveTaskIsRuning = true;
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                DatagramSocket socket = null;
                try {
                    socket = new DatagramSocket(SEND_TO_PORT);
                    socket.setSoTimeout(SOCKET_TIME_OUT);
                    while (mReceiveTaskIsRuning) {
                        listen(socket);
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

    protected void listen(DatagramSocket socket) {
        FileOutputStream fos = null;
        try {
            socket.setSoTimeout(0);
            Pack pack = TransEngine.receivePack(socket, true);
            if (pack.getType() == Pack.TYPE_PRE_SEND) {
                socket.setSoTimeout(SOCKET_TIME_OUT);
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
                    updateStatusView("接收数据包 = " + dataPack.getUid() + "  " + dataPack.getType() + " " + dataPack.getData());
                    fos.write(dataPack.getData());
                }//end for uid

                if (uid > pack.getTrunkNum()) {
                    Pack endPack = Pack.createEndPackage();
                    TransEngine.sendPack(socket, endPack, toAddress, port);
                    updateStatusView("接收图片成功   保存路径于" + filepath);
                    mUIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mReceiveAction != null) {
                                mReceiveAction.onReceiveImageSuccess(filepath);
                            }
                        }
                    });
                }

            }
        } catch (SocketException e) {
            updateStatusView("出错 : " + e);
            e.printStackTrace();
        } catch (IOException e) {
            updateStatusView("出错 : " + e);
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

    /**
     * 终止
     */
    public void terminateReceiveTask() {
        this.mReceiveTaskIsRuning = false;
        threadPool.shutdownNow();
        threadPool = null;
    }

    public static void sendPack(DatagramSocket socket, Pack pack, String toAddress, int sendToPort) throws IOException {
        String json = JSON.toJSONString(pack);
        byte data[] = json.getBytes();
        DatagramPacket pck = new DatagramPacket(data, data.length,
                InetAddress.getByName(toAddress), sendToPort);
        socket.send(pck);
    }

    public static Pack receivePack(DatagramSocket socket, boolean appendDatagram) throws IOException {
        byte[] inServer = new byte[Pack.TRUNK_SIZE + 8 * 1024];
        //receive pkt
        DatagramPacket rcvPkt = new DatagramPacket(inServer, inServer.length);
        socket.receive(rcvPkt);

        String receiveTemp = new String(rcvPkt.getData(), 0, rcvPkt.getLength());
        Pack pck = JSON.parseObject(receiveTemp, Pack.class);
        if (appendDatagram) {
            pck.setDatagramPacket(rcvPkt);
        }
        return pck;
    }

    public void updateStatusView(final String text) {
        if (mStatusView != null) {
            mStatusView.post(new Runnable() {
                @Override
                public void run() {
                    if (mStatusView != null) {
                        mStatusView.setText(text);
                    }
                }
            });
        }
    }

    /**
     * 获取内置SD卡路径
     *
     * @return
     */
    public static String getInnerSDCardPath() {
        return Environment.getExternalStorageDirectory().getPath();
    }

    public void registerOnImageSuccessReceive(IReceiveImageSuccess action) {
        this.mReceiveAction = action;
    }

    public void setStatusView(TextView view) {
        this.mStatusView = view;
    }

}//end class
