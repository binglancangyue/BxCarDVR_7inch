package com.bx.carDVR.adas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import com.calmcar.adas.apiserver.AdasConf;
import com.calmcar.adas.apiserver.model.AdasPoint;
import com.calmcar.adas.apiserver.model.AdasRect;
import com.calmcar.adas.apiserver.model.CdwDetectInfo;
import com.calmcar.adas.apiserver.model.FrontCarInfo;
import com.calmcar.adas.apiserver.model.LdwDetectInfo;
import com.calmcar.adas.apiserver.out.MatDrawProcessMan;
import com.calmcar.adas.apiserver.view.AdasDrawViewPro;

/**
 * 绘制视图
 */

public class AdasDrawView4 extends AdasDrawViewPro {
    MatDrawProcessMan matDrawProcessManager;
    private float mScale;
    private Paint mPaint;
    double curWidth,curHeight;//坐标转换参数

    private Shader mShader;
    private Path path5;
    float  mScaleWidth,mScaleHeight;
    public AdasDrawView4(Context context) {
        this(context, null);
    }

    public AdasDrawView4(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AdasDrawView4(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mPaint = new Paint();
    }

    public  void  initScale(int width ,int height ){
        curWidth=width;
        curHeight=height;
        if(AdasConf.IN_FRAME_HEIGHT>0){
            mScale = Math.min(((float)height)/ AdasConf.IN_FRAME_HEIGHT, ((float)width)/ AdasConf.IN_FRAME_WIDTH);
        }else{
            mScale = 0;
        }
        mScaleWidth=((float)width)/ AdasConf.IN_FRAME_WIDTH;
        mScaleHeight=((float)height)/ AdasConf.IN_FRAME_HEIGHT;
        DrawUtil.initScale(width,height);
    }


    public void initDrawProcess(MatDrawProcessMan matDrawProcessManager) {
        this.matDrawProcessManager = matDrawProcessManager;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        super.surfaceCreated(surfaceHolder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        initScale(i1 ,i2);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        super.surfaceDestroyed(surfaceHolder);
    }



    public void drawBitmap(LdwDetectInfo ldwDetectInfo, CdwDetectInfo cdwDetectInfo) {
        Canvas canvas = getHolder().lockCanvas();
        if(canvas==null ) return;
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);// 清空画布
        updateCheckLaneState(ldwDetectInfo);
        if(!checkState) {
        	drawVanishPointCenter(canvas);
        }
//        drawROIRect(canvas,mPaint);
        if(ldwDetectInfo!=null ){
            PaintLine(canvas,ldwDetectInfo);
        }
        if(cdwDetectInfo!=null){
            PaintCar(canvas,cdwDetectInfo);
        }

        getHolder().unlockCanvasAndPost(canvas);
    }
    private void PaintCar(Canvas canvas, CdwDetectInfo cdwDetectInfo) {
        int showType=cdwDetectInfo.getShowType();
        FrontCarInfo frontCarInfo = cdwDetectInfo.getFrontCarInfo();

        if(showType==1){//发布
            for (int i = 0; i < cdwDetectInfo.carRects.size(); i++) {
                AdasRect adasRect = cdwDetectInfo.carRects.get(i);
                int label=1,carState=0;
                if(label==1 || label==2 || label==3){
                    if(carState == 1 || carState == 10){
                        DrawUtil.drawWarnRect1(canvas,mPaint,adasRect);
                    }else if(carState == 2 || carState == 20){
                        DrawUtil.drawWarnRect2(canvas,mPaint,adasRect);
                    }else if(carState == 3){
                        DrawUtil.drawNormalRectWhite(canvas,mPaint,adasRect);
                    }else{
                        DrawUtil.drawNormalRect(canvas,mPaint,adasRect);
                    }
                }else if(label==4 || label==5){
                    if(carState == 1 ){
                        DrawUtil. drawNormalRectPeo1(canvas,mPaint,adasRect);
                    }else if(carState == 2 ){
                        DrawUtil. drawNormalRectPeo2(canvas,mPaint,adasRect);
                    }else{
                        DrawUtil. drawNormalRectPeo(canvas,mPaint,adasRect);
                    }
                }else if(label==6){
                    DrawUtil.drawNormalRate(canvas,mPaint,adasRect);
                }
            }

            if(frontCarInfo!=null){
                int  carState=frontCarInfo.getFrontCarStateType();
                AdasRect adasRect=frontCarInfo.getCarRect();
                if(carState == 1 || carState == 10){//二级预警
                    DrawUtil.drawWarnRect2(canvas,mPaint,adasRect);
                }else if(carState == 2 || carState == 20){//一级预警
                    DrawUtil.drawWarnRect1(canvas,mPaint,adasRect);
                }else if(carState == 3){
                    DrawUtil.drawNormalRectWhite(canvas,mPaint,adasRect);
                }else{
                    DrawUtil.drawNormalRect(canvas,mPaint,adasRect);
                }
            }
        }

    }

    private void PaintLine(Canvas canvas, LdwDetectInfo ldwDetectInfo) {
        AdasPoint[] lineRect = ldwDetectInfo.lineRect;
//        canvas.drawText("当前车道线数目:---"+ldwDetectInfo.getLaneCount(),100, 100,mPaint);
        switch (ldwDetectInfo.getDetectState()) {
            case -1:
                drawLaneNormalWhite(canvas, lineRect);
                break;
            case 0://noValue
                break;
            case 1:
                drawLaneNormal(canvas, lineRect);
                break;
            case 2:
                drawLaneLeft(canvas, lineRect, ldwDetectInfo.getShowType());
                break;
            case 20:
                drawLaneLeft(canvas, lineRect, ldwDetectInfo.getShowType());
                break;
            case 3:
                drawLaneRight(canvas, lineRect, ldwDetectInfo.getShowType());
                break;
            case 30:
                drawLaneRight(canvas, lineRect, ldwDetectInfo.getShowType());
                break;
            case 4:
//                drawLaneDefault(canvas, lineRect);
                break;

        }
    }

    public void drawLaneNormalWhite(Canvas canvas, AdasPoint[] lineRect) {
        DrawUtil.drawLaneNormalWhite(canvas,mPaint, lineRect);
//        mPaint.reset();
//        mPaint.setColor(Color.WHITE);
//        mPaint.setAntiAlias(true);
//        mPaint.setStrokeWidth(3);//(5);
//        Point p, p1, p2;
//        AdasPoint lineRectP1 = new AdasPoint(lineRect[1].getX() + (-lineRect[1].getX() + lineRect[2].getX()) * 3 / 5, lineRect[1].getY() + (-lineRect[1].getY() + lineRect[2].getY()) * 3 / 5);
//        AdasPoint lineRectP4 = new AdasPoint(lineRect[4].getX() + (-lineRect[4].getX() + lineRect[3].getX()) * 3 / 5, lineRect[4].getY() + (-lineRect[4].getY() + lineRect[3].getY()) * 3 / 5);
//
//        drawLine(canvas, (float) lineRect[0].getX(), (float) lineRect[0].getY(), (float) lineRectP1.getX(), (float) lineRectP1.getY(), mPaint);
////      drawLine(canvas,(float) lineRectP1.getX(),(float)lineRectP1.getY(),(float)lineRect[2].getX(),(float)lineRect[2].getY(),mPaint);
////      drawLine(canvas,(float) lineRect[3].getX(),(float)lineRect[3].getY(),(float)lineRectP4.getX(),(float)lineRectP4.getY(),mPaint);
//        drawLine(canvas, (float) lineRectP4.getX(), (float) lineRectP4.getY(), (float) lineRect[5].getX(), (float) lineRect[5].getY(), mPaint);
//
//        p = new Point((lineRect[0].convertPoint().x + lineRect[5].convertPoint().x) / 2, lineRect[0].convertPoint().y);
//        double xValue = (-lineRect[0].convertPoint().x + lineRect[5].convertPoint().x) / 10;
//        drawLine(canvas, (float) lineRect[0].getX(), (float) lineRect[0].getY(), (float) (lineRect[0].getX() + xValue), (float) lineRect[0].getY(), mPaint);
//        drawLine(canvas, (float) (lineRect[5].getX() - xValue), (float) (lineRect[5].getY()), (float) lineRect[5].getX(), (float) lineRect[5].getY(), mPaint);
//
////        p1 = new Point((lineRect[0].convertPoint().x + lineRectP1.convertPoint().x) / 2, (lineRect[0].convertPoint().y + lineRectP1.convertPoint().y) / 2);
////        p2 = new Point((lineRectP4.convertPoint().x + lineRect[5].convertPoint().x) / 2, (lineRectP4.convertPoint().y + lineRect[5].convertPoint().y) / 2);
////        drawLine(canvas,(float) p1.x,(float)p1.y,(float) (p1.x + xValue),(float)p1.y,mPaint);
////        drawLine(canvas,(float)(p2.x - xValue), (float)p2.y,(float) p2.x,(float)p2.y,mPaint);
//
////        p1 = new Point((lineRectP1.convertPoint().x + lineRect[2].convertPoint().x) / 2, (lineRectP1.convertPoint().y + lineRect[2].convertPoint().y) / 2);
////        p2 = new Point((lineRect[3].convertPoint().x + lineRectP4.convertPoint().x) / 2, (lineRect[3].convertPoint().y + lineRectP4.convertPoint().y) / 2);
////        drawLine(canvas,(float) p1.x,(float)p1.y,(float) (p1.x + xValue),(float)p1.y,mPaint);
////        drawLine(canvas,(float)(p2.x - xValue), (float)p2.y,(float) p2.x,(float)p2.y,mPaint);
//
//        //绘制车道线区域
//        mShader = new LinearGradient((cvX(lineRect[2].getX()) + cvX(lineRect[3].getX())) / 2, cvY(lineRect[2].getY()), (float) (cvX(lineRect[0].getX()) + cvX(lineRect[5].getX())) / 2, cvY(lineRect[0].getY()), new int[]{Color.argb(200, 0, 223, 252), Color.argb(50, 0, 223, 252)}, null, Shader.TileMode.CLAMP);
//        path5 = new Path();
//        mPaint.setShader(mShader);
//        path5.moveTo(cvX(lineRect[0].getX()), cvY(lineRect[0].getY()));
//        path5.lineTo(cvX(lineRectP1.getX()), cvY(lineRectP1.getY()));
////        path5.lineTo(cvX( lineRect[2].getX()),cvY( lineRect[2].getY()));
////        path5.lineTo(cvX( lineRect[3].getX()),cvY(lineRect[3].getY()));
//        path5.lineTo(cvX(lineRectP4.getX()), cvY(lineRectP4.getY()));
//        path5.lineTo(cvX(lineRect[5].getX()), cvY(lineRect[5].getY()));
//        path5.close();
//        canvas.drawPath(path5, mPaint);
    }
    
    public void drawLaneDefault(Canvas canvas, AdasPoint[] lineRect) {
        mPaint.reset();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.WHITE);
        mPaint.setStrokeWidth(10);//(5);
        drawLine(canvas, (float) lineRect[0].getX(), (float) lineRect[0].getY(), (float) lineRect[2].getX(), (float) lineRect[2].getY(), mPaint);
//      drawDefaultLaneRect(canvas);
    }


    public void drawLaneNormal(Canvas canvas, AdasPoint[] lineRect) {
        DrawUtil.drawLaneNormal(canvas,mPaint, lineRect);
        /*mPaint.reset();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.GREEN);
        mPaint.setStrokeWidth(3);//(5);
        Point p, p1, p2;
        AdasPoint lineRectP1=new AdasPoint(lineRect[1].getX()+(-lineRect[1].getX()+lineRect[2].getX())*3/5,lineRect[1].getY()+(-lineRect[1].getY()+lineRect[2].getY())*3/5);
        AdasPoint lineRectP4=new AdasPoint(lineRect[4].getX()+(-lineRect[4].getX()+lineRect[3].getX())*3/5,lineRect[4].getY()+(-lineRect[4].getY()+lineRect[3].getY())*3/5);

        drawLine(canvas,(float) lineRect[0].getX(),(float)lineRect[0].getY(),(float)lineRectP1.getX(),(float)lineRectP1.getY(),mPaint);

        drawLine(canvas,(float) lineRectP4.getX(),(float)lineRectP4.getY(),(float)lineRect[5].getX(),(float)lineRect[5].getY(),mPaint);

        p = new Point((lineRect[0].convertPoint().x + lineRect[5].convertPoint().x) / 2, lineRect[0].convertPoint().y);
        double xValue = (-lineRect[0].convertPoint().x + lineRect[5].convertPoint().x) / 10;
        drawLine(canvas,(float) lineRect[0].getX(),(float)lineRect[0].getY(),(float) (lineRect[0].getX()+xValue),(float)lineRect[0].getY(),mPaint);
        drawLine(canvas,(float)(lineRect[5].getX() - xValue), (float)(lineRect[5].getY()),(float) lineRect[5].getX(),(float) lineRect[5].getY(),mPaint);


        //绘制车道线区域
        mShader = new LinearGradient( (cvX(lineRect[2].getX())+cvX(lineRect[3].getX()))/2, cvY( lineRect[2].getY()), (float) (cvX(lineRect[0].getX())+cvX(lineRect[5].getX()))/2,  cvY(lineRect[0].getY()), new int[]{Color.argb(200,0,223,252),Color.argb(50,0,223,252)}, null, Shader.TileMode.CLAMP);
        path5 = new Path();
        mPaint.setShader(mShader);
        path5.moveTo(cvX(lineRect[0].getX()),cvY(lineRect[0].getY()));
        path5.lineTo(cvX(lineRectP1.getX()),cvY( lineRectP1.getY()));

        path5.lineTo(cvX( lineRectP4.getX()),cvY( lineRectP4.getY()));
        path5.lineTo(cvX(lineRect[5].getX()),cvY(lineRect[5].getY()));
        path5.close();
        canvas.drawPath(path5,mPaint);*/
    }

    public void drawLaneRight(Canvas canvas, AdasPoint[] lineRect, int showType) {
        DrawUtil.drawLaneRight(canvas,mPaint, lineRect);
//        mPaint.reset();
//        mPaint.setAntiAlias(true);
//        Point p, p1, p2;
//        if(showType==1) {
//            mPaint.setColor(Color.GREEN);
//            mPaint.setStrokeWidth(3);//(5);
//            AdasPoint lineRectP1=new AdasPoint(lineRect[1].getX()+(-lineRect[1].getX()+lineRect[2].getX())*3/5,lineRect[1].getY()+(-lineRect[1].getY()+lineRect[2].getY())*3/5);
//            AdasPoint lineRectP4=new AdasPoint(lineRect[4].getX()+(-lineRect[4].getX()+lineRect[3].getX())*3/5,lineRect[4].getY()+(-lineRect[4].getY()+lineRect[3].getY())*3/5);
//
//            drawLine(canvas,(float)lineRect[0].getX(),(float)lineRect[0].getY(),(float)lineRectP1.getX(),(float)lineRectP1.getY(),mPaint);
////            drawLine(canvas,(float)lineRectP1.getX(),(float)lineRectP1.getY(),(float)lineRect[2].getX(),(float)lineRect[2].getY(),mPaint);
//            mPaint.setColor(Color.RED);
////            drawLine(canvas,(float)lineRect[3].getX(),(float)lineRect[3].getY(),(float)lineRectP4.getX(),(float)lineRectP4.getY(),mPaint);
//            drawLine(canvas,(float)lineRectP4.getX(),(float)lineRectP4.getY(),(float)lineRect[5].getX(),(float)lineRect[5].getY(),mPaint);
//            p = new Point((lineRect[0].convertPoint().x + lineRect[5].convertPoint().x) / 2, lineRect[0].convertPoint().y);
//            double xValue = (-lineRect[0].convertPoint().x + lineRect[5].convertPoint().x) / 10;
//            mPaint.setColor(Color.GREEN);
//            drawLine(canvas,(float)lineRect[0].getX(),(float)lineRect[0].getY(),(float)(lineRect[0].getX()+xValue),(float)lineRect[0].getY(),mPaint);
//            mPaint.setColor(Color.RED);
//            drawLine(canvas,(float)(lineRect[5].getX()- xValue),(float)lineRect[5].getY(),(float)lineRect[5].getX(),(float)lineRect[5].getY(),mPaint);
//
//
//
//            //绘制车道线区域
////            mShader = new LinearGradient((float) (lineRect[2].getX()+lineRect[3].getX())/2, (float) lineRect[2].getY(), (float) (lineRect[0].getX()+lineRect[5].getX())/2, (float) lineRect[0].getY(), new int[]{Color.argb(200,255,0,0),Color.argb(0,255,0,0)}, null, Shader.TileMode.CLAMP);
//            mShader = new LinearGradient( (cvX(lineRect[2].getX())+cvX(lineRect[3].getX()))/2, cvY( lineRect[2].getY()), (float) (cvX(lineRect[0].getX())+cvX(lineRect[5].getX()))/2,  cvY(lineRect[0].getY()), new int[]{Color.argb(200,255,0,0),Color.argb(50,255,0,0)}, null, Shader.TileMode.CLAMP);
//            path5 = new Path();
//            mPaint.setShader(mShader);
//            path5.moveTo(cvX(lineRect[0].getX()),cvY(lineRect[0].getY()));
//            path5.lineTo(cvX(lineRectP1.getX()),cvY( lineRectP1.getY()));
//
//            path5.lineTo(cvX( lineRectP4.getX()),cvY( lineRectP4.getY()));
//            path5.lineTo(cvX(lineRect[5].getX()),cvY(lineRect[5].getY()));
//            path5.close();
//            canvas.drawPath(path5,mPaint);
//
//        } else{
//            mPaint.setColor(Color.GREEN);
//            mPaint.setStrokeWidth(3);//(5);
//            AdasPoint lineRectP1=new AdasPoint(lineRect[1].getX()+(-lineRect[1].getX()+lineRect[2].getX())*3/5,lineRect[1].getY()+(-lineRect[1].getY()+lineRect[2].getY())*3/5);
//            AdasPoint lineRectP4=new AdasPoint(lineRect[4].getX()+(-lineRect[4].getX()+lineRect[3].getX())*3/5,lineRect[4].getY()+(-lineRect[4].getY()+lineRect[3].getY())*3/5);
//
//            drawLine(canvas,(float)lineRect[0].getX(),(float)lineRect[0].getY(),(float)lineRectP1.getX(),(float)lineRectP1.getY(),mPaint);
////            drawLine(canvas,(float)lineRectP1.getX(),(float)lineRectP1.getY(),(float)lineRect[2].getX(),(float)lineRect[2].getY(),mPaint);
////            drawLine(canvas,(float)lineRect[3].getX(),(float)lineRect[3].getY(),(float)lineRectP4.getX(),(float)lineRectP4.getY(),mPaint);
//            drawLine(canvas,(float)lineRectP4.getX(),(float)lineRectP4.getY(),(float)lineRect[5].getX(),(float)lineRect[5].getY(),mPaint);
//
//            p = new Point((lineRect[0].convertPoint().x + lineRect[5].convertPoint().x) / 2, lineRect[0].convertPoint().y);
//            double xValue = (-lineRect[0].convertPoint().x + lineRect[5].convertPoint().x) / 10;
//            drawLine(canvas,(float)lineRect[0].getX(),(float)lineRect[0].getY(),(float)(lineRect[0].getX()+ xValue),(float)lineRect[0].getY(),mPaint);
//            drawLine(canvas,(float)(lineRect[5].getX()- xValue),(float)lineRect[5].getY(),(float)lineRect[5].getX(),(float)lineRect[5].getY(),mPaint);
//
//            mShader = new LinearGradient( (cvX(lineRect[2].getX())+cvX(lineRect[3].getX()))/2, cvY( lineRect[2].getY()), (float) (cvX(lineRect[0].getX())+cvX(lineRect[5].getX()))/2,  cvY(lineRect[0].getY()), new int[]{Color.argb(200,0,223,252),Color.argb(50,0,223,252)}, null, Shader.TileMode.CLAMP);
//            path5 = new Path();
//            mPaint.setShader(mShader);
//            path5.moveTo(cvX(lineRect[0].getX()),cvY(lineRect[0].getY()));
//            path5.lineTo(cvX(lineRectP1.getX()),cvY( lineRectP1.getY()));
//
//            path5.lineTo(cvX( lineRectP4.getX()),cvY( lineRectP4.getY()));
//            path5.lineTo(cvX(lineRect[5].getX()),cvY(lineRect[5].getY()));
//            path5.close();
//            canvas.drawPath(path5,mPaint);
//
//        }
    }

    //绘制部分
    public void drawLaneLeft(Canvas canvas, AdasPoint[] lineRect, int showType ) {
        DrawUtil.drawLaneLeft(canvas,mPaint, lineRect);
//        Point p, p1, p2;
//        mPaint.reset();
//        mPaint.setAntiAlias(true);
//        if(showType==1) {
//            mPaint.setColor(Color.RED);
//            mPaint.setStrokeWidth(3);//(5);
//            AdasPoint lineRectP1=new AdasPoint(lineRect[1].getX()+(-lineRect[1].getX()+lineRect[2].getX())*3/5,lineRect[1].getY()+(-lineRect[1].getY()+lineRect[2].getY())*3/5);
//            AdasPoint lineRectP4=new AdasPoint(lineRect[4].getX()+(-lineRect[4].getX()+lineRect[3].getX())*3/5,lineRect[4].getY()+(-lineRect[4].getY()+lineRect[3].getY())*3/5);
//
//
//            drawLine(canvas,(float)lineRect[0].getX(),(float)lineRect[0].getY(),(float)lineRectP1.getX(),(float)lineRectP1.getY(),mPaint);
////            drawLine(canvas,(float)lineRectP1.getX(),(float)lineRectP1.getY(),(float)lineRect[2].getX(),(float)lineRect[2].getY(),mPaint);
//            mPaint.setColor(Color.GREEN);
////            drawLine(canvas,(float)lineRect[3].getX(),(float)lineRect[3].getY(),(float)lineRectP4.getX(),(float)lineRectP4.getY(),mPaint);
//            drawLine(canvas,(float)lineRectP4.getX(),(float)lineRectP4.getY(),(float)lineRect[5].getX(),(float)lineRect[5].getY(),mPaint);
//            p = new Point((lineRect[0].convertPoint().x + lineRect[5].convertPoint().x) / 2, lineRect[0].convertPoint().y);
//            double xValue = (-lineRect[0].convertPoint().x + lineRect[5].convertPoint().x) / 10;
//            mPaint.setColor(Color.RED);
//            drawLine(canvas,(float)lineRect[0].getX(),(float)lineRect[0].getY(),(float)(lineRect[0].getX()+ xValue),(float)lineRect[0].getY(),mPaint);
//            mPaint.setColor(Color.GREEN);
//            drawLine(canvas,(float)(lineRect[5].getX()- xValue),(float)lineRect[5].getY(),(float)lineRect[5].getX(),(float)lineRect[5].getY(),mPaint);
//
//
//            mShader = new LinearGradient( (cvX(lineRect[2].getX())+cvX(lineRect[3].getX()))/2, cvY( lineRect[2].getY()), (float) (cvX(lineRect[0].getX())+cvX(lineRect[5].getX()))/2,  cvY(lineRect[0].getY()), new int[]{Color.argb(200,255,0,0),Color.argb(50,255,0,0)}, null, Shader.TileMode.CLAMP);
//            path5 = new Path();
//            mPaint.setShader(mShader);
//
//            path5.moveTo(cvX(lineRect[0].getX()),cvY(lineRect[0].getY()));
//            path5.lineTo(cvX(lineRectP1.getX()),cvY( lineRectP1.getY()));
//            path5.lineTo(cvX( lineRectP4.getX()),cvY( lineRectP4.getY()));
//            path5.lineTo(cvX(lineRect[5].getX()),cvY(lineRect[5].getY()));
//            path5.close();
//            canvas.drawPath(path5,mPaint);
//
//        }else{
//            mPaint.setColor(Color.GREEN);
//            mPaint.setStrokeWidth(3);//(5);
//            AdasPoint lineRectP1=new AdasPoint(lineRect[1].getX()+(-lineRect[1].getX()+lineRect[2].getX())*3/5,lineRect[1].getY()+(-lineRect[1].getY()+lineRect[2].getY())*3/5);
//            AdasPoint lineRectP4=new AdasPoint(lineRect[4].getX()+(-lineRect[4].getX()+lineRect[3].getX())*3/5,lineRect[4].getY()+(-lineRect[4].getY()+lineRect[3].getY())*3/5);
//
//            drawLine(canvas,(float)lineRect[0].getX(),(float)lineRect[0].getY(),(float)lineRectP1.getX(),(float)lineRectP1.getY(),mPaint);
////            drawLine(canvas,(float)lineRectP1.getX(),(float)lineRectP1.getY(),(float)lineRect[2].getX(),(float)lineRect[2].getY(),mPaint);
////            drawLine(canvas,(float)lineRect[3].getX(),(float)lineRect[3].getY(),(float)lineRectP4.getX(),(float)lineRectP4.getY(),mPaint);
//            drawLine(canvas,(float)lineRectP4.getX(),(float)lineRectP4.getY(),(float)lineRect[5].getX(),(float)lineRect[5].getY(),mPaint);
//
//            p = new Point((lineRect[0].convertPoint().x + lineRect[5].convertPoint().x) / 2, lineRect[0].convertPoint().y);
//            double xValue = (-lineRect[0].convertPoint().x + lineRect[5].convertPoint().x) / 10;
//            drawLine(canvas,(float)lineRect[0].getX(),(float)lineRect[0].getY(),(float)(lineRect[0].getX()+ xValue),(float)lineRect[0].getY(),mPaint);
//            drawLine(canvas,(float)(lineRect[5].getX()- xValue),(float)lineRect[5].getY(),(float)lineRect[5].getX(),(float)lineRect[5].getY(),mPaint);
//
//            mShader = new LinearGradient( (cvX(lineRect[2].getX())+cvX(lineRect[3].getX()))/2, cvY( lineRect[2].getY()), (float) (cvX(lineRect[0].getX())+cvX(lineRect[5].getX()))/2,  cvY(lineRect[0].getY()), new int[]{Color.argb(200,0,223,252),Color.argb(50,0,223,252)}, null, Shader.TileMode.CLAMP);
//            path5 = new Path();
//            mPaint.setShader(mShader);
//            path5.moveTo(cvX(lineRect[0].getX()),cvY(lineRect[0].getY()));
//            path5.lineTo(cvX(lineRectP1.getX()),cvY( lineRectP1.getY()));
//
//            path5.lineTo(cvX( lineRectP4.getX()),cvY( lineRectP4.getY()));
//            path5.lineTo(cvX(lineRect[5].getX()),cvY(lineRect[5].getY()));
//            path5.close();
//            canvas.drawPath(path5,mPaint);
//
//        }
    }

    public void drawRectCarGreen(Canvas canvas, AdasRect adasRect) {
        mPaint.reset();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(5);//(1);
        mPaint.setColor(Color.GREEN);
        mPaint.setStyle(Paint.Style.STROKE);
        if(adasRect.isShowDis()){
            drawRectAndText(canvas,(float)adasRect.getT1().getX(),(float)adasRect.getT1().getY(),
                    (float)adasRect.getBr().getX(),(float)adasRect.getBr().getY(),mPaint,(int)adasRect.getAbsDis()+"");
        }else{
            drawRect(canvas,(float)adasRect.getT1().getX(),(float)adasRect.getT1().getY(),
                    (float)adasRect.getBr().getX(),(float)adasRect.getBr().getY(),mPaint);
        }

    }

    public void drawRectCarOrange(Canvas canvas, AdasRect carRect) {
        mPaint.reset();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(5);//(1);
        mPaint.setColor(Color.YELLOW);
        mPaint.setStyle(Paint.Style.STROKE);
        drawRect(canvas,(float)carRect.getT1().getX(),(float)carRect.getT1().getY(),
                (float)carRect.getBr().getX(),(float)carRect.getBr().getY(),mPaint);
    }

    public void drawRectCarRed(Canvas canvas, AdasRect carRect) {
        mPaint.reset();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(5);//(1);
        mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.STROKE);
        drawRect(canvas,(float)carRect.getT1().getX(),(float)carRect.getT1().getY(),
                (float)carRect.getBr().getX(),(float)carRect.getBr().getY(),mPaint);
    }

    public void drawRectCarBlue(Canvas canvas, AdasRect carRect) {
        mPaint.reset();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(5);//(1);
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.STROKE);
        drawRect(canvas,(float)carRect.getT1().getX(),(float)carRect.getT1().getY(),
                (float)carRect.getBr().getX(),(float)carRect.getBr().getY(),mPaint);
    }

    public void drawVanishPointCenter(Canvas canvas ){
//        mPaint.reset();
//        double centerX = AdasConf.VP_X*mScaleWidth;
//        double centerY=  AdasConf.VP_Y*mScaleHeight;
//        mPaint.setAntiAlias(true);
//        mPaint.setStrokeWidth(2);//(1);
//        mPaint.setColor(Color.GREEN);
//        mPaint.setStyle(Paint.Style.STROKE);
//        canvas.drawLine((float)(centerX -80),(float)centerY,(float)(centerX +80),(float)centerY,mPaint);
//        canvas.drawLine((float)centerX,(float)(centerY-6),(float)centerX,(float)(centerY+6),mPaint);
//
//        double centerX0 = AdasConf.VP_X_TEMP*mScaleWidth;
//        double centerY0=  AdasConf.VP_Y_TEMP*mScaleHeight;
//
//        mPaint.setStrokeWidth(2);//(1);
//        mPaint.setColor(Color.RED);
//        mPaint.setStyle(Paint.Style.STROKE);
//        canvas.drawLine((float)(centerX0 -80),(float)centerY0,(float)(centerX0 +80),(float)centerY0,mPaint);
//        canvas.drawLine((float)centerX0,(float)(centerY0-6),(float)centerX0,(float)(centerY0+6),mPaint);
        DrawUtil.drawVanishPointCenter(canvas,mPaint, AdasConf.VP_X, AdasConf.VP_Y);

    }

    public void drawLine(Canvas canvas  , float startX, float startY, float stopX, float stopY, Paint paint) {
        startX =  mScaleWidth*startX;
        startY = mScaleHeight*startY;
        stopX=  mScaleWidth*stopX;
        stopY= mScaleHeight*stopY;
        canvas.drawLine(startX,startY,stopX,stopY,paint);
    }
    public void drawRect(Canvas canvas  , float tlX, float tlY, float brX, float brY, Paint paint) {
        tlX =  mScaleWidth * tlX;
        tlY = mScaleHeight * tlY ;
        brX =  mScaleWidth  * brX;
        brY =mScaleHeight*  brY  ;
        canvas.drawRect(tlX,tlY, brX,brY,paint);
    }

    public void drawRectAndText(Canvas canvas  , float tlX, float tlY, float brX, float brY, Paint paint, String speedValue) {
        tlX =  mScaleWidth * tlX;
        tlY = mScaleHeight * tlY ;
        brX =  mScaleWidth  * brX;
        brY =mScaleHeight*  brY  ;
        canvas.drawRect(tlX,tlY, brX,brY,paint);
        //绘制数据
        mPaint.reset();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(2);//(1);
        mPaint.setColor(Color.rgb(204,63,63));
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setTextSize(24);
        canvas.drawText(speedValue+" m",tlX, tlY,paint);
    }

    public  float cvX(double startX){
        return (float) (startX*mScaleWidth);
    }

    public float cvY(double startY){
        return  (float) (startY*mScaleHeight);
    }


    public void drawDefaultLaneRect(Canvas canvas ) {
        mPaint.reset();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(1);//(1);
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.FILL);
        float startX=(float) AdasConf.VP_X;
        float startY=(float) AdasConf.VP_Y;

        float pointX10=startX-25;
        float pointY10=startY+30;//45


        float pointX11=pointX10-15;
        float pointY11=pointY10;//45


        float pointX12=pointX11-20/(float) 0.8;
        float pointY12=pointY10+20;

        float pointX13=pointX10-20/(float) 0.8;
        float pointY13=pointY12;//45

        path5 = new Path();
        path5.moveTo(cvX(pointX10),cvY(pointY10));
        path5.lineTo(cvX(pointX11),cvY(pointY11));
        path5.lineTo(cvX(pointX12),cvY(pointY12));
        path5.lineTo(cvX(pointX13),cvY(pointY13));


        float pointX20=pointX13-10/(float) 0.8;
        float pointY20=pointY13+10;


        float pointX21=pointX20-15;
        float pointY21=pointY20;//45


        float pointX22=pointX21-40/(float) 0.8;
        float pointY22=pointY21+40;

        float pointX23=pointX20-40/(float) 0.8;
        float pointY23=pointY22;//45


        path5.moveTo(cvX(pointX20),cvY(pointY20));
        path5.lineTo(cvX(pointX21),cvY(pointY21));
        path5.lineTo(cvX(pointX22),cvY(pointY22));
        path5.lineTo(cvX(pointX23),cvY(pointY23));


        float pointX30=pointX23-10/(float) 0.8;
        float pointY30=pointY23+10;


        float pointX31=pointX30-15;
        float pointY31=pointY30;//45


        float pointX32=pointX31-60/(float) 0.8;
        float pointY32=pointY31+60;

        float pointX33=pointX30-60/(float) 0.8;
        float pointY33=pointY32;//45


        path5.moveTo(cvX(pointX30),cvY(pointY30));
        path5.lineTo(cvX(pointX31),cvY(pointY31));
        path5.lineTo(cvX(pointX32),cvY(pointY32));
        path5.lineTo(cvX(pointX33),cvY(pointY33));


        float pointX60=startX+25;
        float pointY60=startY+30;//45


        float pointX61=pointX60+15;
        float pointY61=pointY60;//45


        float pointX62=pointX61+20/(float) 0.8;
        float pointY62=pointY61+20;

        float pointX63=pointX60+20/(float) 0.8;
        float pointY63=pointY62;//45

        path5.moveTo(cvX(pointX60),cvY(pointY60));
        path5.lineTo(cvX(pointX61),cvY(pointY61));
        path5.lineTo(cvX(pointX62),cvY(pointY62));
        path5.lineTo(cvX(pointX63),cvY(pointY63));


        float pointX50=pointX63+10/(float) 0.8;
        float pointY50=pointY63+10;


        float pointX51=pointX50+15;
        float pointY51=pointY50;//45


        float pointX52=pointX51+40/(float) 0.8;
        float pointY52=pointY51+40;

        float pointX53=pointX50+40/(float) 0.8;
        float pointY53=pointY52;//45

        path5.moveTo(cvX(pointX50),cvY(pointY50));
        path5.lineTo(cvX(pointX51),cvY(pointY51));
        path5.lineTo(cvX(pointX52),cvY(pointY52));
        path5.lineTo(cvX(pointX53),cvY(pointY53));


        float pointX40=pointX53+10/(float) 0.8;
        float pointY40=pointY53+10;

        float pointX41=pointX40+15;
        float pointY41=pointY40;//45


        float pointX42=pointX41+60/(float) 0.8;
        float pointY42=pointY41+60;

        float pointX43=pointX40+60/(float) 0.8;
        float pointY43=pointY42;//45


        path5.moveTo(cvX(pointX40),cvY(pointY40));
        path5.lineTo(cvX(pointX41),cvY(pointY41));
        path5.lineTo(cvX(pointX42),cvY(pointY42));
        path5.lineTo(cvX(pointX43),cvY(pointY43));

        path5.close();
        canvas.drawPath(path5,mPaint);

    }

    int checkNum=30;
    boolean checkState=false;
    public boolean  updateCheckLaneState(LdwDetectInfo ldwDetectInfo){
        if(checkState){
           return checkState;
        }else{
            if (ldwDetectInfo !=null && ldwDetectInfo.getDetectState()==1) {
                checkNum-=1;
                if (checkNum<=0) {
                    checkState=true;
                }
            }else{
                if(checkNum<30){
                    checkNum+=1;
                }else{
                    checkNum=30;
                }
            }
            return checkState;
        }
    }

}
