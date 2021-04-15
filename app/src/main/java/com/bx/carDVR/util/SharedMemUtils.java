package com.bx.carDVR.util;

import android.util.Log;

import com.autonavi.amapauto.gdarcameraservice.MemoryFileFlag;


/**
 * 共享内存结构，前面二十个字节为HEADER，后面的所有部分是CONTENT。CONTENT的内容由HEADER中的一些字段决定
 * HEADER部分描述如下：
 * 0: 一个字节，可以放一些标志位，目前放置的是可读可写的标志位
 * 1: 一个字节，放置的是version code，版本号，版本号必须完全一致才可以正确解析
 * 2~5:四个字节，offset
 * 6~9:四个字节，length
 * 10~13:四个字节，contentSize
 * <p>
 * CONTENT部分描述如下：
 * 纯字节数组，内容按header中的offset+length+contentSize决定
 * 如果是yuvImage类型的话，offset == 0; length是要读取的长度; contentSize指的是整个CONTENT部分的大小
 */
public class SharedMemUtils {
    private static final String TAG = "SharedMemUtils";

    /**
     * 共享文件头的长度
     */
    public static final int HEADER_SIZE = 20;

    /**
     * 如果解析格式有变化，这个数值记得要加一
     */
    public static final byte VERSION_CODE = 1;


    public static void initHeader(byte[] header) {
        if (header == null || header.length < HEADER_SIZE) {
            Log.e(TAG, "initHeader ERROR!! --> header == null || header.length < HEADER_SIZE");
            return;
        }

        header[0] = MemoryFileFlag.CAN_WRITE.getFlag();
        header[1] = VERSION_CODE;
    }


    /**
     * 判断是否可读
     *
     * @param header
     * @return
     */
    public static boolean canRead(byte[] header) {
        if (header == null || header.length < HEADER_SIZE) {
            return false;
        }
        return header[0] == MemoryFileFlag.CAN_READ.getFlag();
    }

    /**
     * 设置成可读
     *
     * @param header
     * @return
     */
    public static boolean setCanRead(byte[] header) {
        if (header == null || header.length < HEADER_SIZE) {
            return false;
        }
        header[0] = MemoryFileFlag.CAN_READ.getFlag();
        return true;
    }


    /**
     * 判断是否可写
     *
     * @param header
     * @return
     */
    public static boolean canWrite(byte[] header) {
        if (header == null || header.length < HEADER_SIZE) {
            return false;
        }
        return header[0] == MemoryFileFlag.CAN_WRITE.getFlag();
    }

    /**
     * 设置为可写
     *
     * @param header
     * @return
     */
    public static boolean setWirtable(byte[] header) {
        if (header == null || header.length < HEADER_SIZE) {
            return false;
        }
        header[0] = MemoryFileFlag.CAN_WRITE.getFlag();
        return true;
    }


    /**
     * 设置开始位置
     * 2~5:四个字节，offset
     * <p>
     * 注意高低位的顺序：高在左，低在右
     *
     * @param header
     * @param start
     * @return
     */
    public static boolean setOffset(byte[] header, int start) {
        if (header == null || header.length < HEADER_SIZE) {
            return false;
        }
        header[2] = (byte) (start >> 24);
        header[3] = (byte) ((start & 0xff0000) >> 16);
        header[4] = (byte) ((start & 0xff00) >> 8);
        header[5] = (byte) (start & 0xff);
        return true;
    }

    /**
     * 获取到开始位置/偏移位置
     * 2~5:四个字节，offset
     *
     * @param header
     * @return
     */
    public static int getOffset(byte[] header) {
        if (header == null || header.length < HEADER_SIZE) {
            return -1;
        }

        int result = 0;
        result = header[2] & 0xff;
        result = result << 8 | header[3] & 0xff;
        result = result << 8 | header[4] & 0xff;
        result = result << 8 | header[5] & 0xff;

        return result;
    }


    /**
     * 设置读取的长度
     * 6~9:四个字节，length
     *
     * @param header
     * @param length
     * @return
     */
    public static boolean setLenght(byte[] header, int length) {
        if (header == null || header.length < HEADER_SIZE) {
            return false;
        }
        header[6] = (byte) (length >> 24);
        header[7] = (byte) ((length & 0xff0000) >> 16);
        header[8] = (byte) ((length & 0xff00) >> 8);
        header[9] = (byte) (length & 0xff);
        return true;
    }

    /**
     * 获取到需要读取的长度
     * 6~9:四个字节，length
     *
     * @param header
     * @return
     */
    public static int getLength(byte[] header) {
        if (header == null || header.length < HEADER_SIZE) {
            return -1;
        }

        int result = 0;
        result = header[6] & 0xff;
        result = result << 8 | header[7] & 0xff;
        result = result << 8 | header[8] & 0xff;
        result = result << 8 | header[9] & 0xff;

        return result;
    }


    /**
     * 设置整个content的大小
     * 10~13:四个字节，contentSize
     *
     * @param header
     * @param contentSize
     * @return
     */
    public static boolean setContentSize(byte[] header, int contentSize) {
        if (header == null || header.length < HEADER_SIZE) {
            return false;
        }

        header[10] = (byte)(contentSize >>> 24);
        header[11] = (byte)(contentSize >>> 16);
        header[12] = (byte)(contentSize >>> 8);
        header[13] = (byte)contentSize;
        return true;
    }


    /**
     * 设置contentSize的大小
     * 10~13:四个字节，contentSize
     *
     * @param header
     * @return
     */
    public static int getContentSize(byte[] header) {
        if (header == null || header.length < HEADER_SIZE) {
            return -1;
        }
        int result = 0;
        result = header[10] & 0xff;
        result = result << 8 | header[11] & 0xff;
        result = result << 8 | header[12] & 0xff;
        result = result << 8 | header[13] & 0xff;
        return result;
    }


//    public static void main(String[] args) {
//        byte[] header = new byte[20];
//
//        setContentSize(header, 255);
//        setOffset(header , 255);
//        setLenght(header , 255);
//
//        int size = getContentSize(header);
//        int offset = getOffset(header);
//        int length = getLength(header);
//
//        int xx;
//    }

}