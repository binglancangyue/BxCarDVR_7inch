package com.zqc.share;

import java.io.IOException;

public class ShareBuffer {
	private static String TAG = "ShareBuffer";

	// Returns 'true' if purged, 'false' otherwise
	private static native int native_read(byte[] buffer, int srcOffset, int destOffset, int count, int cameraId, boolean isPreview) throws IOException;
	public static native void native_init(int cameraId, int width, int height, int zoomLevel, boolean isPreview) throws IOException;
	private static native void native_shareBufferEnable(int cameraId, boolean enable, boolean isPreview) /*throws IOException*/;

	static {
		System.loadLibrary("sharebuffer");
	}
	/**
	 * Reads bytes from the memory file.
	 * Will throw an IOException if the file has been purged.
	 *
	 * @param buffer byte array to read bytes into.
	 * @param srcOffset offset into the memory file to read from.
	 * @param destOffset offset into the byte array buffer to read into.
	 * @param count number of bytes to read.
	 * @return number of bytes read.
	 * @throws IOException if the memory file has been purged or deactivated.
	 */
	public int readBytes(byte[] buffer, int srcOffset, int destOffset, int count, int cameraId, boolean isPreview)
			throws IOException {
		native_read(buffer, srcOffset, destOffset, count, cameraId, isPreview);
		return count;
	}

	/**
	 * Allocates a new ashmem region. The region is initially not purgable.
	 *
	 * @param name optional name for the file (can be null).
	 * @param length of the memory file in bytes, must be positive.
	 * @throws IOException if the memory file could not be created.
	 */
	public ShareBuffer() {
		//native_init(cameraId, width, height, zoomLevel, isPreview);
	}

	public void setShareBufferEnable(int cameraId, boolean enable, boolean isPreview)
		/*throws IOException*/ {
		native_shareBufferEnable(cameraId, enable, isPreview);
	}

	public void init(int cameraId, int width, int height, int zoomLevel, boolean isPreview) throws IOException{
		native_init(cameraId, width, height, zoomLevel, isPreview);
	}
}
