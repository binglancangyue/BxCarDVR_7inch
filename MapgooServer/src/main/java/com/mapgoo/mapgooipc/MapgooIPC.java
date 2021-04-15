package com.mapgoo.mapgooipc;

/**
 * Created by Administrator on 2017/12/5 0005.
 */

public class MapgooIPC {
    /**
     * 来自服务端的命令，在
     * @see CmdDealCallBack.getCmd 中监听
     */
    public static final int MAPGOO_IPC_CMD_CAPTURE_PHOTO         = 0x00001; // 拍照，暂不用处理
    public static final int MAPGOO_IPC_CMD_CAPTURE_VIDEO         = 0x00002; // 抓拍视频，暂不用处理
    public static final int MAPGOO_IPC_CMD_CAPTURE_LIVE_START    = 0x00003; // 开始直播，暂不用处理
    public static final int MAPGOO_IPC_CMD_CAPTURE_LIVE_STOP     = 0x00004; // 停止直播，暂不用处理
    public static final int MAPGOO_IPC_CMD_START_PUT_VIDEO_FRAME = 0x02000; // 开始传输视频数据
    public static final int MAPGOO_IPC_CMD_STOP_PUT_VIDEO_FRAME  = 0x02001; // 停止传输视频数据

    /**
     * 来自ipc_client内部的连接状态通知，通知 ipc_client 与 ipc_server 的连接状态，在
     * @see ConnStatusNotifCallBack.connStatusNotif 中监听
     */
    public static final int MAPGOO_IPC_CONNECTED    = 1; //连接成功
    public static final int MAPGOO_IPC_DISCONNECTED = 0; //连接断开

    /*********** 发送给服务端的消息，通过接口 sendMsg 发送 **********/
    /* 震动消息 */
	public static final int MAPGOO_IPC_MSG_VIBRATE          = 0x00100; //消息类型：震动
	public static final int MAPGOO_IPC_SUBMSG_VIBRATE_PHONE = 0;       //子消息：震动告警图片
    public static final int MAPGOO_IPC_SUBMSG_VIBRATE_VIDEO = 1;       //子消息：震动告警视频
	/* 摄像头插拔状态*/
    public static final int MAPGOO_IPC_MSG_CAMERA_STATUS = 0x00101; //消息类型：摄像头状态
    public static final int MAPGOO_IPC_SUBMSG_CAMERA_OUT = 0;       //子消息：摄像头拔出
    public static final int MAPGOO_IPC_SUBMSG_CAMERA_IN  = 1;       //子消息：摄像头插入

    /**
     * 命令监听接口，用于监听来自服务端的命令；
     * 注意：目前只需要监听 MAPGOO_IPC_CMD_START_PUT_VIDEO_FRAME 和 MAPGOO_IPC_CMD_STOP_PUT_VIDEO_FRAME
     *       两个命令， 若有其它命令请过滤掉
     */
    public interface CmdDealCallBack {
        /**
         * 命令监听接口，用于监听来自服务端的命令；
         * @param cmd 服务端下发的命令
         * @param DataIndex 数据源的DataIndex，标识cmd要作用于哪个数据源上
         * @param time 发送数据的时长，单位秒，为负数时表示一直发送直到接收到到停止命令；[注意]暂时不用处理这个参数
         */
        void getCmd(int cmd, int DataIndex, long time);
    }

    private static CmdDealCallBack cmDealCallBack;

    /**
     * 注册用于监听来自服务端的命令的接口
     * @param cmdCallBack CmdDealCallBack接口的实现对象
     */
    public static void setCmdDealCallBack(CmdDealCallBack cmdCallBack) {
        cmDealCallBack = cmdCallBack;
    }

    /**
     * 连接状态监听接口，用于监听 icp_client 与 ipc_server 的连接状态
     */
    public interface ConnStatusNotifCallBack {
        /**
         * 用于监听 icp_client 与 ipc_server 的连接状态
         * 注意：不要在此函数内做耗时的操作!!!
         * @param status MAPGOO_IPC_CONNECTED：连接成功
         *                MAPGOO_IPC_DISCONNECTED：连接已断开
         */
        void connStatusNotif(int status);
    }


    private static ConnStatusNotifCallBack connStatusNotifCallBack;

    /**
     * 注册用于监听连接状态的接口
     * @param csnCallBack
     */
    public static void setConnStatusNotifCallBack(ConnStatusNotifCallBack csnCallBack) {
        connStatusNotifCallBack = csnCallBack;
    }

    /**
     * 绑定数据源(如前置摄像、后置摄像头等)到一个dataIndex，dataIndex将可用来在回调接口cmDealCallBack中控
     * 制开始或结束推送哪个数据源的数据。接收到MAPGOO_IPC_CONNECTED通知时需要重新绑定所有数据源。
     * 注意：此接口为阻塞模式，可能会有较大耗时
     * @param uniqueID 数据源名称，名称可以自己设置，比如前置摄像头可以是:包名_0,后置摄像头可以是：包名_1。
     *                 建议先绑定前置摄像头，后绑定后置摄像头
     * @return 成功则返回数据源所绑定的dataIndex，目前范围:0-15； 失败则返回负值
     */
    public native static int getDataIndexByUniqueID(String uniqueID);
	
    /**
     * 推送yuv数据，每次推送一个完整帧的数据，多线程发送时，需要调用者自己同步
     * @param DataIndex 数据源ID，通过getDataIndexByUniqueID获得的， 范围:0-15
     * @param yuvformat  1: NV21
     *                    2: NV12
     *                    3: I420
     *                    4: YV12
     * @param w YUV图像数据宽
     * @param h YUV图像数据高
     * @param arr YUV图像数据
     * @param datasize YUV图像数据长度
     * @return 0:成功; 其它:错误码
     * 错误码 -1: ClientContext 未初始化
     *        -2: 有参数非法
     *        -3: 发送数据到服务端失败
     *        -4: 创建客户端失败
     *        -5: 初始化客户端数据发送通道失败
     *      -100: 与参数 arr 相关的JNI操作错误
     */
    public static native int PutYUVFrame(int DataIndex,int yuvformat,int w,int h,byte[] arr,int datasize);

    /**
     * 推送h264数据，每次推送一个完整帧的数据，多线程发送时，需要调用者自己同步
     * @param DataIndex  数据源ID，通过getDataIndexByUniqueID获得的， 范围:0-15
     * @param arr h264数据
     * @param datasize h264数据大小
     * @return 0:成功; 其他:失败
     */
    public static native int PutH264Frame(int DataIndex, byte[] arr, int datasize);

    /**
     * 推送h265数据，每次推送一个完整帧的数据，多线程发送时，需要调用者自己同步
     * @param DataIndex  数据源ID，通过getDataIndexByUniqueID获得的， 范围:0-15
     * @param arr h265数据
     * @param datasize h265数据大小
     * @return 0:成功; 其他:失败
     */
    public static native int PutH265Frame(int DataIndex,int w,int h, byte[] arr, int datasize);

    /**
     * 获得ipc库版本号
     * @return ipc 客户端版本号
     */
    public native static String getVersion();

    /**
     * 发送消息到服务端
     * @param msg 消息类型
     * @param submsg 子消息
     * @param param1 参数1
     * @param param2 参数2
     * @param opt 操作类型， 暂保留，未使用
     * @return 目前永远返回true
     */
    public native static boolean sendMsg(int msg, int submsg, long param1, long param2, String opt );



    public static void OnCmd(int cmd,int DataIndex,long time){
        if(null != cmDealCallBack){
            cmDealCallBack.getCmd(cmd,DataIndex,time);
        }
    }

    public static void OnConnStatusNotif(int status){
        if(null != connStatusNotifCallBack){
            // 重要:不要在此函数内做耗时的操作!!!
            connStatusNotifCallBack.connStatusNotif(status);
        }
    }

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("mapgoo_ipc_client");
    }
}
