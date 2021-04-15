package com.bx.carDVR.util;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;


import com.ecar.ecarjttsdk.ECarJttManage;

import java.io.IOException;
import java.nio.ByteBuffer;

public class H264Encoder {
    private static final String TAG = "BxH264Encoder";

    private MediaCodec mediaCodec;
    private int width;
    private int height;
    private int frameRate = 20;

    private long generateIndex = 0;
    private long pts = 0;
    private byte[] configbyte;
    public H264Encoder(int width, int height) {
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

    public int offerEncoder(byte[] input, int channelNum, ECarJttManage eCarJttManage) {
        int pos = 0;
        try {
            ByteBuffer[] inputBuffers = this.mediaCodec.getInputBuffers();
            ByteBuffer[] outputBuffers = this.mediaCodec.getOutputBuffers();
            int inputBufferIndex = this.mediaCodec.dequeueInputBuffer(-1L);
            if (inputBufferIndex >= 0) {
                pts = computePresentationTime(generateIndex);
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                inputBuffer.put(input);
                this.mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, pts, 0);
                generateIndex += 1;
            }

            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0L);
            while (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                byte[] outData = new byte[bufferInfo.size];
                outputBuffer.get(outData);
                //BUFFER_FLAG_CODEC_CONFIG，sps pps
                if (bufferInfo.flags == 2) {
                    configbyte = new byte[bufferInfo.size];
                    configbyte = outData;
                } else if (bufferInfo.flags == 1) {//关键帧
                    byte[] keyframe = new byte[bufferInfo.size + configbyte.length];
                    System.arraycopy(configbyte, 0, keyframe, 0, configbyte.length);
                    System.arraycopy(outData, 0, keyframe, configbyte.length, outData.length);
                    //JTT808Manager.getInstance().videoLive(keyframe, channelNum, liveClient);
                    //outputStream.write(keyframe, 0, keyframe.length);
                    eCarJttManage.sendVideoData(keyframe,channelNum);
                    pos = keyframe.length;
                } else {//非关键帧
                    //outputStream.write(outData, 0, outData.length);
                    //JTT808Manager.getInstance().videoLive(outData, channelNum, liveClient);
                    eCarJttManage.sendVideoData(outData,channelNum);
                    pos = outData.length;
                }
                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0L);
            }
        } catch (Throwable var13) {
            var13.printStackTrace();
        }
        return pos;
    }

    private long computePresentationTime(long frameIndex) {
        return 132 + frameIndex * 1000000 / 20;
    }
}
