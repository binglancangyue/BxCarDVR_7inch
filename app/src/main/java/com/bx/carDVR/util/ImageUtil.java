package com.bx.carDVR.util;


import android.graphics.ImageFormat;
import android.hardware.camera2.utils.SurfaceUtils;
import android.media.Image;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ImageUtil {
    public static final int YUV420P = 0;
    public static final int YUV420SP = 1;
    public static final int NV21 = 2;
    private static final String TAG = "ImageUtil";
    private static final String TAG_21="NV21";
    public static byte[] getBytesFromImageAsType2(Image image, int type) {
        try {
            //获取源数据，如果是YUV格式的数据planes.length = 3
            //plane[i]里面的实际数据可能存在byte[].length <= capacity (缓冲区总大小)
            final Image.Plane[] planes = image.getPlanes();

            //数据有效宽度，一般的，图片width <= rowStride，这也是导致byte[].length <= capacity的原因
            // 所以我们只取width部分
            int width = image.getWidth();
            int height = image.getHeight();
            Log.i(TAG_21, "height="+image.getHeight()+",width="+image.getWidth());
            //此处用来装填最终的YUV数据，需要1.5倍的图片大小，因为Y U V 比例为 4:1:1
            Log.i(TAG_21,"width * height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8="+width * height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8);
            byte[] yuvBytes = new byte[width * height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8];
            //目标数组的装填到的位置
            int dstIndex = 0;

            //临时存储uv数据的
            byte uBytes[] = new byte[width * height / 4];
            byte vBytes[] = new byte[width * height / 4];
            int uIndex = 0;
            int vIndex = 0;

            int pixelsStride, rowStride;
            for (int i = 0; i < planes.length; i++) {
                pixelsStride = planes[i].getPixelStride();
                rowStride = planes[i].getRowStride();

                ByteBuffer buffer = planes[i].getBuffer();

                //如果pixelsStride==2，一般的Y的buffer长度=640*480，UV的长度=640*480/2-1
                //源数据的索引，y的数据是byte中连续的，u的数据是v向左移以为生成的，两者都是偶数位为有效数据
                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);

                Log.d(TAG_21,"pixelsStride="+pixelsStride+"  rowStride="+rowStride+"  buffer.capacity()="+buffer.capacity()+"  i="+i);
                int srcIndex = 0;
                if (i == 0) {
                    // adddddd
                    // if(width ==rowStride)
                    {
                        System.arraycopy(bytes,srcIndex,yuvBytes,dstIndex, width*height);
                        srcIndex += width*height;
                        dstIndex += width*height;
                    }
                } else if (i == 1) {
                    //根据pixelsStride取相应的数据
                    for (int j = 0; j < height / 2; j++) {
                        for (int k = 0; k < width / 2; k++) {
                            uBytes[uIndex++] = bytes[srcIndex];
                            srcIndex += pixelsStride;
                        }
                        if (pixelsStride == 2) {
                            srcIndex += rowStride - width;
                        } else if (pixelsStride == 1) {
                            srcIndex += rowStride - width / 2;
                        }
                    }
                } else if (i == 2) {
                    //根据pixelsStride取相应的数据
                    for (int j = 0; j < height / 2; j++) {
                        for (int k = 0; k < width / 2; k++) {
                            vBytes[vIndex++] = bytes[srcIndex];
                            srcIndex += pixelsStride;
                        }
                        if (pixelsStride == 2) {
                            srcIndex += rowStride - width;
                        } else if (pixelsStride == 1) {
                            srcIndex += rowStride - width / 2;
                        }
                    }
                }
            }

//            image.close();

            //根据要求的结果类型进行填充
            switch (type) {
                case YUV420P:
                    System.arraycopy(uBytes, 0, yuvBytes, dstIndex, uBytes.length);
                    System.arraycopy(vBytes, 0, yuvBytes, dstIndex + uBytes.length, vBytes.length);
                    break;
                case YUV420SP:
                    for (int i = 0; i < vBytes.length; i++) {
                        yuvBytes[dstIndex++] = uBytes[i];
                        yuvBytes[dstIndex++] = vBytes[i];
                    }
                    break;
                case NV21:
                    Log.d(TAG_21,"uBytes.length="+uBytes.length);
                    System.arraycopy(uBytes, 0, yuvBytes, dstIndex, uBytes.length);
//                    System.arraycopy(vBytes, 0, yuvBytes, dstIndex + uBytes.length, vBytes.length);
                    for (int i = 0; i < vBytes.length; i++) {
                        yuvBytes[dstIndex++] = vBytes[i];
                        yuvBytes[dstIndex++] = uBytes[i];
                    }

                    break;
            }
            return yuvBytes;
        } catch (final Exception e) {
            if (image != null) {
                image.close();
            }
            Log.i(TAG, e.toString());
        }
        return null;
    }
    /***
     * 此方法内注释以640*480为例
     * 未考虑CropRect的
     */
    public static byte[] getBytesFromImageAsType(Image image, int type) {
        try {
            //获取源数据，如果是YUV格式的数据planes.length = 3
            //plane[i]里面的实际数据可能存在byte[].length <= capacity (缓冲区总大小)
            final Image.Plane[] planes = image.getPlanes();

            //数据有效宽度，一般的，图片width <= rowStride，这也是导致byte[].length <= capacity的原因
            // 所以我们只取width部分
            int width = image.getWidth();
            int height = image.getHeight();

            //此处用来装填最终的YUV数据，需要1.5倍的图片大小，因为Y U V 比例为 4:1:1
            byte[] yuvBytes = new byte[width * height * ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8];
            //目标数组的装填到的位置
            int dstIndex = 0;

            //临时存储uv数据的
            byte uBytes[] = new byte[width * height / 4];
            byte vBytes[] = new byte[width * height / 4];
            int uIndex = 0;
            int vIndex = 0;

            int pixelsStride, rowStride;
            for (int i = 0; i < planes.length; i++) {
                pixelsStride = planes[i].getPixelStride();
                rowStride = planes[i].getRowStride();

                ByteBuffer buffer = planes[i].getBuffer();

                //如果pixelsStride==2，一般的Y的buffer长度=640*480，UV的长度=640*480/2-1
                //源数据的索引，y的数据是byte中连续的，u的数据是v向左移以为生成的，两者都是偶数位为有效数据
                byte[] bytes = new byte[buffer.capacity()];
                buffer.get(bytes);

                int srcIndex = 0;
                if (i == 0) {

                    //直接取出来所有Y的有效区域，也可以存储成一个临时的bytes，到下一步再copy
                    for (int j = 0; j < height; j++) {
                        System.arraycopy(bytes, srcIndex, yuvBytes, dstIndex, width);
                        srcIndex += rowStride;
                        dstIndex += width;
                    }
                } else if (i == 1) {
                    //根据pixelsStride取相应的数据
                    for (int j = 0; j < height / 2; j++) {
                        for (int k = 0; k < width / 2; k++) {
                            uBytes[uIndex++] = bytes[srcIndex];
                            srcIndex += pixelsStride;
                        }
                        if (pixelsStride == 2) {
                            srcIndex += rowStride - width;
                        } else if (pixelsStride == 1) {
                            srcIndex += rowStride - width / 2;
                        }
                    }
                } else if (i == 2) {
                    //根据pixelsStride取相应的数据
                    for (int j = 0; j < height / 2; j++) {
                        for (int k = 0; k < width / 2; k++) {
                            vBytes[vIndex++] = bytes[srcIndex];
                            srcIndex += pixelsStride;
                        }
                        if (pixelsStride == 2) {
                            srcIndex += rowStride - width;
                        } else if (pixelsStride == 1) {
                            srcIndex += rowStride - width / 2;
                        }
                    }
                }
            }

            image.close();

            //根据要求的结果类型进行填充
            switch (type) {
                case YUV420P:
                    System.arraycopy(uBytes, 0, yuvBytes, dstIndex, uBytes.length);
                    System.arraycopy(vBytes, 0, yuvBytes, dstIndex + uBytes.length, vBytes.length);
                    break;
                case YUV420SP:
                    for (int i = 0; i < vBytes.length; i++) {
                        yuvBytes[dstIndex++] = uBytes[i];
                        yuvBytes[dstIndex++] = vBytes[i];
                    }
                    break;
                case NV21:
                    for (int i = 0; i < vBytes.length; i++) {
                        yuvBytes[dstIndex++] = vBytes[i];
                        yuvBytes[dstIndex++] = uBytes[i];
                    }
                    break;
            }
            return yuvBytes;
        } catch (final Exception e) {
            if (image != null) {
                image.close();
            }
            Log.i(TAG, e.toString());
        }
        return null;
    }
  public static void savePre2FIle( byte[] buff)
      {
          String filePath = "/mnt/sdcard/adas_preview";
          File tmpDir = new File(filePath);
          if (!tmpDir.exists()) {
              tmpDir.mkdirs();
          }
          SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
          String fileDate = dateformat.format(new Date());
          File previewFile = new File(filePath,fileDate + ".yuv");
          try {
              if (!previewFile.exists()) {
                  previewFile.createNewFile();
              }
              System.out.println("sava file");
              OutputStream os = new FileOutputStream(previewFile, false);
              os.write(buff);
              os.flush();
              os.close();

          } catch (IOException e) {
              e.printStackTrace();
          }
  }
    public static void savePre2FIle( byte[]buff,String fileName)
    {
        String filePath = "/mnt/sdcard/adas_preview2/";
        File tmpDir = new File(filePath);
        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }
        SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String fileDate = dateformat.format(new Date());
        File previewFile = new File(filePath,fileName+fileDate + ".yuv");
        try {
            if (!previewFile.exists()) {
                previewFile.createNewFile();
            }
            System.out.println("sava previewFile:::"+previewFile.getAbsolutePath()+previewFile.getName());
            OutputStream os = new FileOutputStream(previewFile, false);
            os.write(buff);
            os.flush();
            os.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static  int  readPre2File( byte[]buff,String fileName)
    {
        int nRet =0;
        File previewFile = new File("/mnt/sdcard/adas_preview2/camera12019-07-22-14-20-59.yuv");
        try {
            if (!previewFile.exists()) {
               return 0;
            }
            System.out.println("sava previewFile:::"+previewFile.getAbsolutePath()+previewFile.getName());
            InputStream os = new FileInputStream(previewFile);
           nRet= os.read(buff);

            os.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return  nRet;
    }

    public static boolean isSurfaceAbandon(Surface surface){
        boolean isAbandon = false;
        try {
            SurfaceUtils.getSurfaceDataspace(surface);
        }catch (Exception e){
            isAbandon =true;
        }
        return isAbandon;
    }
}

