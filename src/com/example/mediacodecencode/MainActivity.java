package com.example.mediacodecencode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.LinearLayout;

@SuppressWarnings("deprecation")
@SuppressLint("NewApi")
public class MainActivity extends Activity  implements SurfaceHolder.Callback,PreviewCallback{

	private SurfaceView surfaceview;
	
    private SurfaceHolder surfaceHolder;
	

	private Camera camera;
	
    private Parameters parameters;
    
    int width = 1280;
    
    int height = 720;
    
    int framerate = 30;//帧率
    
    int biterate = 8500*1000;//比特率
    
    private static int yuvqueuesize = 10;//缓存yuv数据 帧数, 避免内存占用过大
    
	public static ArrayBlockingQueue<byte[]> YUVQueue = new ArrayBlockingQueue<byte[]>(yuvqueuesize); 
	
	private AvcEncoder avcCodec;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
//		initPar();
		
		setContentView(R.layout.activity_main);
		surfaceview = (SurfaceView)findViewById(R.id.surfaceview);
		
        LinearLayout.LayoutParams lp= new LinearLayout.LayoutParams(width, height);
        lp.gravity = Gravity.CENTER;
        surfaceview.setLayoutParams(lp);//动态设置
        
        surfaceHolder = surfaceview.getHolder();
        surfaceHolder.addCallback(this);
        SupportAvcCodec();
	}


	private void initPar() {
		WindowManager wm = this.getWindowManager();
	    height = wm.getDefaultDisplay().getWidth();
	     width= wm.getDefaultDisplay().getHeight();
	}
	

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        camera = getBackCamera();
        startcamera(camera);
		avcCodec = new AvcEncoder(width,height,framerate,biterate);
		avcCodec.StartEncoderThread();
		
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (null != camera) {
        	camera.setPreviewCallback(null);
        	camera.stopPreview();
            camera.release();
            camera = null;
            avcCodec.StopThread();
        }
    }


    /**
     * 摄像头的回调函数，每一帧数据
     * //w*h 直接决定了当前摄像头返回的数据大小，YUV420格式，占用内存 w*h*3/2, 每个像素1.5字节；
     * http://blog.csdn.net/u011046042/article/details/50849299
     * */
	@Override
	public void onPreviewFrame(byte[] data, android.hardware.Camera camera) {
		// TODO Auto-generated method stub
		Log.i("MeidaCodec", "fr-size="+data.length);
		putYUVData(data,data.length);
	}
	
	public void putYUVData(byte[] buffer, int length) {
		if (YUVQueue.size() >= 10) {
			Log.i("MeidaCodec","ERROR drop video data ");
			YUVQueue.poll();//超出则丢弃，
		}
		YUVQueue.add(buffer);
	}
	

	/**
	 * 遍历支持的编码形式
	 * */
	private boolean SupportAvcCodec(){
		if(Build.VERSION.SDK_INT>=18){
			for(int j = MediaCodecList.getCodecCount() - 1; j >= 0; j--){
				MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(j);
	
				String[] types = codecInfo.getSupportedTypes();
				for (int i = 0; i < types.length; i++) {
					if (types[i].equalsIgnoreCase("video/avc")) {
						return true;
					}
				}
			}
		}
		return false;
	}
	

    private void startcamera(Camera mCamera){
        if(mCamera != null){
            try {
                mCamera.setPreviewCallback(this);
                mCamera.setDisplayOrientation(90);
                if(parameters == null){
                    parameters = mCamera.getParameters();
                }
                parameters = mCamera.getParameters();
                
                parameters.setPreviewFormat(ImageFormat.NV21);//摄像头采集数据
                parameters.getSupportedPreviewSizes();//获取可行的宽高
                parameters.setPreviewSize(width, height);
                mCamera.setParameters(parameters);
                mCamera.setPreviewDisplay(surfaceHolder);
                mCamera.startPreview();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @TargetApi(9)
	private Camera getBackCamera() {
        Camera c = null;
        try {
            c = Camera.open(0); // attempt to get a Camera instance
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c; // returns null if camera is unavailable
    }


}
