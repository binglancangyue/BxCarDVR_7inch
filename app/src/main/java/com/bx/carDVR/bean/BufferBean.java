package com.bx.carDVR.bean;

import com.bx.carDVR.util.LogUtils;


public class BufferBean {
	private final int FRAME_HEADER = 4;//16;
	public byte[] isCanRead; // 1 can 0 no
	public byte[] isCanRead1; // 1 can 0 no
	public int mId = 0;
	public byte[] mBuffer0; // adas buffer
	public byte[] mBuffer1; // adas buffer

	public BufferBean(int size,int size1) {
		// init data
		isCanRead = new byte[FRAME_HEADER];
		isCanRead1 = new byte[FRAME_HEADER];
		Allocate(size,size1);
	}

	public void Allocate(int bufferSize,int size1) {
		if (bufferSize > 0 && size1 > 0) {
			mBuffer0 = new byte[bufferSize];
			mBuffer1 = new byte[size1];
		}
	}

	public void Change() {
		mId = (++mId) % 2;
	}
}
