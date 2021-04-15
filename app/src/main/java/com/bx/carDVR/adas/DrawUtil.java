
package com.bx.carDVR.adas;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import com.calmcar.adas.apiserver.AdasConf;
import com.calmcar.adas.apiserver.model.AdasPoint;
import com.calmcar.adas.apiserver.model.AdasRect;

import org.opencv.core.Point;

import java.text.DecimalFormat;


/**
 * @author lelexiao
 * @date 2019-12-05
 * @description com.calmcar.adas.apiserver.vcui
 * @project ADMS_UI_PRO
 */

public class DrawUtil {
    public static final int HORIZONTAL =80;
    public static final int VERTICAL = 6;
    static  float  mScaleWidth,mScaleHeight;

/**
     *  屏幕分辨率
     * @param width  屏幕分辨率宽
     * @param height 屏幕分辨率高
     */

    public  static  void  initScale(int width ,int height ){
        mScaleWidth=((float)width)/ AdasConf.IN_FRAME_WIDTH;
        mScaleHeight=((float)height)/ AdasConf.IN_FRAME_HEIGHT;
    }

    public static  float cvX(double startX){
        return (float) (startX*mScaleWidth);
    }

    public static  float cvY(double startY){
        return  (float) (startY*mScaleHeight);
    }

    public static  void drawVanishPointCenter(Canvas canvas, Paint mPaint, double vcenterX, double vcenterY) {
        mPaint.reset();
        double centerX =cvX( vcenterX);
        double centerY =cvY( vcenterY);
        mPaint.setStrokeWidth(3);//(1);
        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.STROKE);
        canvas.drawLine((float) (centerX - HORIZONTAL), (float) centerY, (float) (centerX + HORIZONTAL), (float) centerY, mPaint);
        canvas.drawLine((float) centerX, (float) (centerY - VERTICAL), (float) centerX, (float) (centerY + VERTICAL), mPaint);
    }

    public static  void drawRealPointCenter(Canvas canvas, Paint mPaint, double centerX, double centerY) {
        mPaint.reset();
        mPaint.setStrokeWidth(2);//(1);
        mPaint.setColor(Color.GREEN);
        mPaint.setStyle(Paint.Style.STROKE);
        canvas.drawLine((float) (centerX - HORIZONTAL), (float) centerY, (float) (centerX + HORIZONTAL), (float) centerY, mPaint);
        canvas.drawLine((float) centerX, (float) (centerY - VERTICAL), (float) centerX, (float) (centerY + VERTICAL), mPaint);
    }


    public  static  void drawRoiAreaCenter(Canvas canvas, Paint mPaint, double vcenterX, double vcenterY) {
        mPaint.reset();
        mPaint.setStrokeWidth(2);
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.STROKE);

        Point centre=new Point( vcenterX,vcenterY);

        Path path5 = new Path();

        path5.moveTo(cvX(centre.x - 30), cvY(centre.y + 30 - 100));
        path5.lineTo(cvX(centre.x - 30),cvY(centre.y + 30));

        path5.lineTo(cvX(centre.x - 30-90),cvY( centre.y + 30+90));

        path5.lineTo(cvX(centre.x - 30-180),cvY( centre.y + 30+180));

        path5.lineTo(cvX(centre.x - 30-180),cvY( centre.y + 30+180+120));
        path5.lineTo(cvX(centre.x + 30+180),cvY( centre.y + 30+180+120));

        path5.lineTo(cvX(centre.x + 30+180),cvY( centre.y + 30+180));

        path5.lineTo(cvX(centre.x + 30+90),cvY( centre.y + 30+90));

        path5.lineTo(cvX(centre.x + 30),cvY(centre.y + 30));

        path5.lineTo(cvX(centre.x + 30),cvY( centre.y + 30 - 100));

        path5.close();
        canvas.drawPath(path5,mPaint);
    }



    public static  void drawLaneNormalWhite(Canvas canvas, Paint mPaint, AdasPoint[] lineRect) {
        mPaint.reset();
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(1);//(5);
        mPaint.setAntiAlias(true);

        AdasPoint lineRectP1 = new AdasPoint(lineRect[1].getX() + (-lineRect[1].getX() + lineRect[2].getX()) * 3 / 5, lineRect[1].getY() + (-lineRect[1].getY() + lineRect[2].getY()) * 3 / 5);
        AdasPoint lineRectP4 = new AdasPoint(lineRect[4].getX() + (-lineRect[4].getX() + lineRect[3].getX()) * 3 / 5, lineRect[4].getY() + (-lineRect[4].getY() + lineRect[3].getY()) * 3 / 5);

        drawLine(canvas, (float) lineRect[0].getX(), (float) lineRect[0].getY(), (float) lineRectP1.getX(), (float) lineRectP1.getY(), mPaint);
        drawLine(canvas, (float) lineRectP4.getX(), (float) lineRectP4.getY(), (float) lineRect[5].getX(), (float) lineRect[5].getY(), mPaint);

        double xValue = (-lineRect[0].convertPoint().x + lineRect[5].convertPoint().x) / 10;
        drawLine(canvas, (float) lineRect[0].getX(), (float) lineRect[0].getY(), (float) (lineRect[0].getX() + xValue), (float) lineRect[0].getY(), mPaint);
        drawLine(canvas, (float) (lineRect[5].getX() - xValue), (float) (lineRect[5].getY()), (float) lineRect[5].getX(), (float) lineRect[5].getY(), mPaint);

        //绘制车道线区域
        LinearGradient mShader = new LinearGradient((cvX(lineRectP1.getX()) + cvX(lineRectP4.getX())) / 2, cvY(lineRectP1.getY()), (float) (cvX(lineRect[0].getX()) + cvX(lineRect[5].getX())) / 2, cvY(lineRect[0].getY()), new int[]{Color.argb(10, 0, 180, 255), Color.argb(180, 0, 180, 255)}, null, Shader.TileMode.CLAMP);
        Path path5 = new Path();
        mPaint.setShader(mShader);
        path5.moveTo(cvX(lineRect[0].getX()), cvY(lineRect[0].getY()));
        path5.lineTo(cvX(lineRectP1.getX()), cvY(lineRectP1.getY()));

        path5.lineTo(cvX(lineRectP4.getX()), cvY(lineRectP4.getY()));
        path5.lineTo(cvX(lineRect[5].getX()), cvY(lineRect[5].getY()));
        path5.close();
        canvas.drawPath(path5, mPaint);
    }

    public static  void drawLaneNormal(Canvas canvas, Paint mPaint, AdasPoint[] lineRect) {
        mPaint.reset();
        mPaint.setColor(Color.GREEN);
        mPaint.setStrokeWidth(1);//(5);
        mPaint.setAntiAlias(true);

        AdasPoint lineRectP1 = new AdasPoint(lineRect[1].getX() + (-lineRect[1].getX() + lineRect[2].getX()) * 3 / 5, lineRect[1].getY() + (-lineRect[1].getY() + lineRect[2].getY()) * 3 / 5);
        AdasPoint lineRectP4 = new AdasPoint(lineRect[4].getX() + (-lineRect[4].getX() + lineRect[3].getX()) * 3 / 5, lineRect[4].getY() + (-lineRect[4].getY() + lineRect[3].getY()) * 3 / 5);

        drawLine(canvas, (float) lineRect[0].getX(), (float) lineRect[0].getY(), (float) lineRectP1.getX(), (float) lineRectP1.getY(), mPaint);
        drawLine(canvas, (float) lineRectP4.getX(), (float) lineRectP4.getY(), (float) lineRect[5].getX(), (float) lineRect[5].getY(), mPaint);

        double xValue = (-lineRect[0].convertPoint().x + lineRect[5].convertPoint().x) / 10;
        drawLine(canvas, (float) lineRect[0].getX(), (float) lineRect[0].getY(), (float) (lineRect[0].getX() + xValue), (float) lineRect[0].getY(), mPaint);
        drawLine(canvas, (float) (lineRect[5].getX() - xValue), (float) (lineRect[5].getY()), (float) lineRect[5].getX(), (float) lineRect[5].getY(), mPaint);

        //绘制车道线区域
        LinearGradient mShader = new LinearGradient((cvX(lineRectP1.getX()) + cvX(lineRectP4.getX())) / 2, cvY(lineRectP1.getY()), (float) (cvX(lineRect[0].getX()) + cvX(lineRect[5].getX())) / 2, cvY(lineRect[0].getY()), new int[]{Color.argb(10, 0, 180, 255), Color.argb(180, 0, 180, 255)}, null, Shader.TileMode.CLAMP);
        Path path5 = new Path();
        mPaint.setShader(mShader);
        path5.moveTo(cvX(lineRect[0].getX()), cvY(lineRect[0].getY()));
        path5.lineTo(cvX(lineRectP1.getX()), cvY(lineRectP1.getY()));

        path5.lineTo(cvX(lineRectP4.getX()), cvY(lineRectP4.getY()));
        path5.lineTo(cvX(lineRect[5].getX()), cvY(lineRect[5].getY()));
        path5.close();
        canvas.drawPath(path5, mPaint);
    }

    public static void drawLaneLeft(Canvas canvas, Paint mPaint, AdasPoint[] lineRect) {
        mPaint.reset();
        mPaint.setColor(Color.GREEN);
        mPaint.setStrokeWidth(1);//(5);
        mPaint.setAntiAlias(true);

        AdasPoint lineRectP1 = new AdasPoint(lineRect[1].getX() + (-lineRect[1].getX() + lineRect[2].getX()) * 3 / 5, lineRect[1].getY() + (-lineRect[1].getY() + lineRect[2].getY()) * 3 / 5);
        AdasPoint lineRectP4 = new AdasPoint(lineRect[4].getX() + (-lineRect[4].getX() + lineRect[3].getX()) * 3 / 5, lineRect[4].getY() + (-lineRect[4].getY() + lineRect[3].getY()) * 3 / 5);
        mPaint.setColor(Color.RED);
        drawLine(canvas, (float) lineRect[0].getX(), (float) lineRect[0].getY(), (float) lineRectP1.getX(), (float) lineRectP1.getY(), mPaint);
        mPaint.setColor(Color.GREEN);
        drawLine(canvas, (float) lineRectP4.getX(), (float) lineRectP4.getY(), (float) lineRect[5].getX(), (float) lineRect[5].getY(), mPaint);

        double xValue = (-lineRect[0].convertPoint().x + lineRect[5].convertPoint().x) / 10;
        mPaint.setColor(Color.RED);
        drawLine(canvas, (float) lineRect[0].getX(), (float) lineRect[0].getY(), (float) (lineRect[0].getX() + xValue), (float) lineRect[0].getY(), mPaint);
        mPaint.setColor(Color.GREEN);
        drawLine(canvas, (float) (lineRect[5].getX() - xValue), (float) (lineRect[5].getY()), (float) lineRect[5].getX(), (float) lineRect[5].getY(), mPaint);

        //绘制车道线区域
        LinearGradient mShader = new LinearGradient(cvX(lineRectP1.getX()) , cvY(lineRectP1.getY()), cvX(lineRectP4.getX()) , cvY(lineRectP4.getY()), new int[]{Color.argb(180, 255, 60, 0), Color.argb(50, 0, 180, 255)}, null, Shader.TileMode.CLAMP);
        Path path5 = new Path();
        mPaint.setShader(mShader);
        path5.moveTo(cvX(lineRect[0].getX()), cvY(lineRect[0].getY()));
        path5.lineTo(cvX(lineRectP1.getX()), cvY(lineRectP1.getY()));

        path5.lineTo(cvX(lineRectP4.getX()), cvY(lineRectP4.getY()));
        path5.lineTo(cvX(lineRect[5].getX()), cvY(lineRect[5].getY()));
        path5.close();
        canvas.drawPath(path5, mPaint);

    }

    public static void drawLaneRight(Canvas canvas, Paint mPaint, AdasPoint[] lineRect) {
        mPaint.reset();
        mPaint.setColor(Color.GREEN);
        mPaint.setStrokeWidth(1);
        mPaint.setAntiAlias(true);

        AdasPoint lineRectP1 = new AdasPoint(lineRect[1].getX() + (-lineRect[1].getX() + lineRect[2].getX()) * 3 / 5, lineRect[1].getY() + (-lineRect[1].getY() + lineRect[2].getY()) * 3 / 5);
        AdasPoint lineRectP4 = new AdasPoint(lineRect[4].getX() + (-lineRect[4].getX() + lineRect[3].getX()) * 3 / 5, lineRect[4].getY() + (-lineRect[4].getY() + lineRect[3].getY()) * 3 / 5);

        drawLine(canvas, (float) lineRect[0].getX(), (float) lineRect[0].getY(), (float) lineRectP1.getX(), (float) lineRectP1.getY(), mPaint);
        mPaint.setColor(Color.RED);
        drawLine(canvas, (float) lineRectP4.getX(), (float) lineRectP4.getY(), (float) lineRect[5].getX(), (float) lineRect[5].getY(), mPaint);
        mPaint.setColor(Color.GREEN);
        double xValue = (-lineRect[0].convertPoint().x + lineRect[5].convertPoint().x) / 10;
        drawLine(canvas, (float) lineRect[0].getX(), (float) lineRect[0].getY(), (float) (lineRect[0].getX() + xValue), (float) lineRect[0].getY(), mPaint);
        mPaint.setColor(Color.RED);
        drawLine(canvas, (float) (lineRect[5].getX() - xValue), (float) (lineRect[5].getY()), (float) lineRect[5].getX(), (float) lineRect[5].getY(), mPaint);

        //绘制车道线区域
        LinearGradient mShader = new LinearGradient( cvX(lineRectP4.getX()) , cvY(lineRectP4.getY()), cvX(lineRectP1.getX()) , cvY(lineRectP1.getY()),new int[]{Color.argb(180, 255, 60, 0), Color.argb(50, 0, 180, 255)}, null, Shader.TileMode.CLAMP);
        Path path5 = new Path();
        mPaint.setShader(mShader);
        path5.moveTo(cvX(lineRect[0].getX()), cvY(lineRect[0].getY()));
        path5.lineTo(cvX(lineRectP1.getX()), cvY(lineRectP1.getY()));

        path5.lineTo(cvX(lineRectP4.getX()), cvY(lineRectP4.getY()));
        path5.lineTo(cvX(lineRect[5].getX()), cvY(lineRect[5].getY()));
        path5.close();
        canvas.drawPath(path5, mPaint);
    }

    public  static   void drawNormalRect(Canvas canvas, Paint mPaint , AdasRect adasRect) {
        double tx = cvX(adasRect.getT1().getX());
        double ty = cvY(adasRect.getT1().getY());
        double rx = cvX(adasRect.getBr().getX());
        double ry = cvY(adasRect.getBr().getY());
        float centerX = (float) (tx + rx) / 2;
        float centerY = (float) (ty + ry) / 2;
        float radius = (float) (rx - tx) / 2 > (ry - ty) / 2 ? (float) (rx - tx) / 2 : (float) (ry - ty) / 2;
        //绘制数据
        mPaint.reset();
        mPaint.setStrokeWidth(1);//(1);
        mPaint.setStyle(Paint.Style.FILL);
        Shader mShader = new RadialGradient(centerX, centerY, radius, Color.argb(50, 0, 223, 252), Color.argb(100, 0, 223, 252), Shader.TileMode.REPEAT);
        mPaint.setShader(mShader);
        canvas.drawCircle(centerX, centerY, radius, mPaint);

        mPaint.reset();
        mPaint.setStrokeWidth(1);//(1);
        mPaint.setColor(Color.GREEN);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        canvas.drawCircle(centerX, centerY, radius + 5, mPaint);
        canvas.drawCircle(centerX, centerY, radius + 10, mPaint);
        canvas.drawCircle(centerX, centerY, radius + 15, mPaint);

        //绘制数据

        if ((int) adasRect.getAbsDis() != 0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            String showValue = (int) adasRect.getAbsDis() + "m";
            mPaint.setTextSize(18);
            canvas.drawText(showValue, (float) tx - 15, (float) ty - 15, mPaint);

        }
    }
    public  static   void drawNormalRectWhite(Canvas canvas, Paint mPaint , AdasRect adasRect){
        double  tx=cvX(adasRect.getT1().getX());
        double  ty=cvY(adasRect.getT1().getY());
        double  rx=cvX(adasRect.getBr().getX());
        double  ry=cvY(adasRect.getBr().getY());
        float  centerX=(float) (tx+rx)/2;
        float  centerY=(float)(ty+ry)/2;
        float  radius= (float)(rx-tx)/2 > (ry-ty)/2 ? (float)(rx-tx)/2:(float)(ry-ty)/2;
        //绘制数据
        mPaint.reset();
        mPaint.setStrokeWidth(1);//(1);
        mPaint.setStyle(Paint.Style.FILL);
        Shader mShader = new RadialGradient(centerX,centerY,radius, Color.argb(50, 0, 223, 252), Color.argb(100, 0, 223, 252), Shader.TileMode.REPEAT);
        mPaint.setShader(mShader);
        canvas.drawCircle(centerX,centerY,radius,mPaint);

        mPaint.reset();
        mPaint.setStrokeWidth(1);//(1);
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        canvas.drawCircle(centerX,centerY,radius+5,mPaint);
        canvas.drawCircle(centerX,centerY,radius+10,mPaint);
        canvas.drawCircle(centerX,centerY,radius+15,mPaint);

        //绘制数据
        if((int)adasRect.getAbsDis()!=0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            String showValue = (int) adasRect.getAbsDis() + "m";
            mPaint.setTextSize(18);
            canvas.drawText(showValue, (float) tx - 15, (float) ty - 15, mPaint);
        }
    }



    public  static   void drawWarnRect1(Canvas canvas, Paint mPaint , AdasRect adasRect){
        double  tx=cvX(adasRect.getT1().getX());
        double  ty=cvY(adasRect.getT1().getY());
        double  rx=cvX(adasRect.getBr().getX());
        double  ry=cvY(adasRect.getBr().getY());
        float  centerX=(float) (tx+rx)/2;
        float  centerY=(float)(ty+ry)/2;
        float  radius= (float)(rx-tx)/2 > (ry-ty)/2 ? (float)(rx-tx)/2:(float)(ry-ty)/2;
        //绘制数据
        mPaint.reset();
        mPaint.setStrokeWidth(1);//(1);
        mPaint.setStyle(Paint.Style.FILL);
        Shader mShader = new RadialGradient(centerX,centerY,radius, Color.argb(50, 255, 0, 0), Color.argb(100, 255, 0, 0), Shader.TileMode.REPEAT);
        mPaint.setShader(mShader);
        canvas.drawCircle(centerX,centerY,radius,mPaint);

        mPaint.reset();
        mPaint.setStrokeWidth(1);//(1);
        mPaint.setColor(Color.rgb( 255, 0, 0));
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        canvas.drawCircle(centerX,centerY,radius+5,mPaint);
        canvas.drawCircle(centerX,centerY,radius+10,mPaint);
        canvas.drawCircle(centerX,centerY,radius+15,mPaint);

        //绘制数据
        if((int)adasRect.getAbsDis()!=0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            String showValue = (int) adasRect.getAbsDis() + "m";
            mPaint.setTextSize(18);
            canvas.drawText(showValue, (float) tx - 15, (float) ty - 15, mPaint);
        }
    }

    public  static  void drawWarnRect2(Canvas canvas, Paint mPaint , AdasRect adasRect){
        double  tx=cvX(adasRect.getT1().getX());
        double  ty=cvY(adasRect.getT1().getY());
        double  rx=cvX(adasRect.getBr().getX());
        double  ry=cvY(adasRect.getBr().getY());
        float  centerX=(float) (tx+rx)/2;
        float  centerY=(float)(ty+ry)/2;
        float  radius= (float)(rx-tx)/2 > (ry-ty)/2 ? (float)(rx-tx)/2:(float)(ry-ty)/2;
        //绘制数据
        mPaint.reset();
        mPaint.setStrokeWidth(1);//(1);
        mPaint.setStyle(Paint.Style.FILL);
        Shader mShader = new RadialGradient(centerX,centerY,radius, Color.argb(50, 255, 255, 0), Color.argb(100, 255, 255, 0), Shader.TileMode.REPEAT);
        mPaint.setShader(mShader);
        canvas.drawCircle(centerX,centerY,radius,mPaint);

        mPaint.reset();
        mPaint.setStrokeWidth(1);//(1);
        mPaint.setColor(Color.rgb( 255, 255, 0));
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        canvas.drawCircle(centerX,centerY,radius+5,mPaint);
        canvas.drawCircle(centerX,centerY,radius+10,mPaint);
        canvas.drawCircle(centerX,centerY,radius+15,mPaint);

        if((int)adasRect.getAbsDis()!=0) {
            //绘制数据
            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            String showValue = (int) adasRect.getAbsDis() + "m";
            mPaint.setTextSize(18);
            canvas.drawText(showValue, (float) tx - 15, (float) ty - 15, mPaint);
        }
    }


    public  static  void drawNormalRectPeo(Canvas canvas, Paint mPaint , AdasRect adasRect){
        float  tx=cvX(adasRect.getT1().getX());
        float  ty=cvY(adasRect.getT1().getY());
        float  rx=cvX(adasRect.getBr().getX());
        float  ry=cvY(adasRect.getBr().getY());
        //绘制数据
        mPaint.reset();
        mPaint.setStrokeWidth(1);//(1);
        mPaint.setStyle(Paint.Style.FILL);
        Shader mShader = new LinearGradient(tx,ty,rx,ry, Color.argb(50, 0, 223, 252), Color.argb(100, 0, 223, 252), Shader.TileMode.REPEAT);
        mPaint.setShader(mShader);
        canvas.drawRect(tx,ty,rx,ry,mPaint);

        mPaint.reset();
        mPaint.setStrokeWidth(1);//(1);
        mPaint.setColor(Color.GREEN);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        canvas.drawRect(tx,ty,rx,ry,mPaint);
        canvas.drawRect(tx-2,ty-2,rx+2,ry+2,mPaint);
        canvas.drawRect(tx-5,ty-5,rx+5,ry+5,mPaint);

        //绘制数据
        if((int)adasRect.getAbsDis()!=0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            String showValue = (int) adasRect.getAbsDis() + "m";
            mPaint.setTextSize(18);
            canvas.drawText(showValue, tx - 5, ty - 5, mPaint);
        }
    }


    public  static  void drawNormalRectPeo1(Canvas canvas, Paint mPaint , AdasRect adasRect){
        float  tx=cvX(adasRect.getT1().getX());
        float  ty=cvY(adasRect.getT1().getY());
        float  rx=cvX(adasRect.getBr().getX());
        float  ry=cvY(adasRect.getBr().getY());
        //绘制数据
        mPaint.reset();
        mPaint.setStrokeWidth(1);//(1);
        mPaint.setStyle(Paint.Style.FILL);
        Shader mShader = new LinearGradient(tx,ty,rx,ry, Color.argb(50, 255, 0, 0), Color.argb(100, 255, 0, 0), Shader.TileMode.REPEAT);
        mPaint.setShader(mShader);
        canvas.drawRect(tx,ty,rx,ry,mPaint);

        mPaint.reset();
        mPaint.setStrokeWidth(1);//(1);
        mPaint.setColor(Color.rgb( 255, 0, 0));
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        canvas.drawRect(tx,ty,rx,ry,mPaint);
        canvas.drawRect(tx-2,ty-2,rx+2,ry+2,mPaint);
        canvas.drawRect(tx-5,ty-5,rx+5,ry+5,mPaint);
        //绘制数据
        if((int)adasRect.getAbsDis()!=0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            String showValue = (int) adasRect.getAbsDis() + "m";
            mPaint.setTextSize(18);
            canvas.drawText(showValue, tx - 5, ty - 5, mPaint);
        }
    }


    public  static  void drawNormalRectPeo2(Canvas canvas, Paint mPaint , AdasRect adasRect){
        float  tx=cvX(adasRect.getT1().getX());
        float  ty=cvY(adasRect.getT1().getY());
        float  rx=cvX(adasRect.getBr().getX());
        float  ry=cvY(adasRect.getBr().getY());
        //绘制数据
        mPaint.reset();
        mPaint.setStrokeWidth(1);//(1);
        mPaint.setStyle(Paint.Style.FILL);
        Shader mShader = new LinearGradient(tx,ty,rx,ry, Color.argb(50, 255, 255, 0), Color.argb(100, 255, 255, 0), Shader.TileMode.REPEAT);
        mPaint.setShader(mShader);
        canvas.drawRect(tx,ty,rx,ry,mPaint);

        mPaint.reset();
        mPaint.setStrokeWidth(1);//(1);
        mPaint.setColor(Color.rgb( 255, 255, 0));
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        canvas.drawRect(tx,ty,rx,ry,mPaint);
        canvas.drawRect(tx-2,ty-2,rx+2,ry+2,mPaint);
        canvas.drawRect(tx-5,ty-5,rx+5,ry+5,mPaint);

        //绘制数据
        if((int)adasRect.getAbsDis()!=0) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            String showValue = (int) adasRect.getAbsDis() + "m";
            mPaint.setTextSize(18);
            canvas.drawText(showValue, tx - 5, ty - 5, mPaint);
        }
    }


    public  static   void drawNormalRate(Canvas canvas, Paint mPaint , AdasRect adasRect){
        double  tx=cvX(adasRect.getT1().getX());
        double  ty=cvY(adasRect.getT1().getY());
        double  rx=cvX(adasRect.getBr().getX());
        double  ry=cvY(adasRect.getBr().getY());
        float  centerX=(float) (tx+rx)/2;
        float  centerY=(float)(ty+ry)/2;
        float  radius= (float)(rx-tx)/2 > (ry-ty)/2 ? (float)(rx-tx)/2:(float)(ry-ty)/2;
        //绘制数据
        mPaint.reset();
        mPaint.setStrokeWidth(1);//(1);
        mPaint.setStyle(Paint.Style.FILL);
        Shader mShader = new RadialGradient(centerX,centerY,radius, Color.argb(50, 0, 223, 252), Color.argb(100, 0, 223, 252), Shader.TileMode.REPEAT);
        mPaint.setShader(mShader);
        canvas.drawCircle(centerX,centerY,radius,mPaint);

        mPaint.reset();
        mPaint.setStrokeWidth(1);//(1);
        mPaint.setColor(Color.GREEN);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);
        canvas.drawCircle(centerX,centerY,radius+5,mPaint);
        canvas.drawCircle(centerX,centerY,radius+10,mPaint);
        canvas.drawCircle(centerX,centerY,radius+15,mPaint);

        String showValue=adasRect.getCarState()+"";
        //绘制数据
        mPaint.setTextSize(18);
        canvas.drawText(showValue,centerX, centerY,mPaint);

    }

    private static void drawLine(Canvas canvas  , float startX, float startY, float stopX, float stopY, Paint paint) {
        canvas.drawLine(cvX(startX),cvY(startY),cvX(stopX),cvY(stopY),paint);
    }


}

