package com.bx.carDVR.util;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;


import java.io.IOException;
import java.nio.ByteBuffer;

public class AvcEncoder {

    private static final String TAG = "BxAvcEncoder";

    private MediaCodec mediaCodec;
    private int width;
    private int height;
    private int frameRate = 20;

    public AvcEncoder(int width, int height) {
        this.width = width;
        this.height = height;
        initMediaCodec();
    }

    private void initMediaCodec() {
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");
            MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc",width,height);
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE,500000);
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE,frameRate);
            mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            mediaCodec.configure(mediaFormat,null,null,1);
            mediaCodec.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopEncoder() {
        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec = null;
        }
    }

    private byte[] m_info;
    public int offerEncoder(byte[] input,byte[] output) {
        //LogUtils.getInstance().d(TAG, "offerEncoder:"+input.length+"+"+output.length);
        int pos = 0;
        //byte[] yuv420sp = new byte[width * height * 3 / 2];
        //机器是NV21数据转成NV12
        //NV21ToNV12(input, yuv420sp, m_width, m_height);
        //input = yuv420sp;
        if (null != input && mediaCodec != null) {
            try{
                ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
                ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
                int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
                if (inputBufferIndex >= 0) {
                    ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                    inputBuffer.clear();
                    inputBuffer.put(input);
                    mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, System.nanoTime() / 1000L, 0);
                }
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo,0);
                while (outputBufferIndex >= 0) {
                    ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                    byte[] outData = new byte[bufferInfo.size];
                    outputBuffer.get(outData);

                    if(m_info != null){
                        System.arraycopy(outData, 0,  output, pos, outData.length);
                        pos += outData.length;
                    }else{//保存pps sps 只有开始时 第一个帧里有， 保存起来后面用
                        ByteBuffer spsPpsBuffer = ByteBuffer.wrap(outData);
                        if (spsPpsBuffer.getInt() == 0x00000001) {
                            m_info = new byte[outData.length];
                            System.arraycopy(outData, 0, m_info, 0, outData.length);
                        }else {
                            return -1;
                        }
                    }
                    if(output[4] == 0x65) {//key frame 编码器生成关键帧时只有 00 00 00 01 65 没有pps sps， 要加上
                        System.arraycopy(m_info, 0,  output, 0, m_info.length);
                        System.arraycopy(outData, 0,  output, m_info.length, outData.length);
                    }
                    mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                    outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                }
            } catch(Throwable t) {
                t.printStackTrace();
            }
        }
        return pos;
    }

}
