package com.example.free_ar;

import android.content.Context;
import android.graphics.Canvas;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraView extends SurfaceView
	implements SurfaceHolder.Callback{

	private SurfaceHolder holder;//ホルダー
	private Camera camera;//カメラ

	Canvas canvas;

	//コンストラクタ
	public CameraView(Context context) {
		super(context);

		init();
	}

	public CameraView(Context context, AttributeSet attrs){
		super(context, attrs);

		init();
	}

	//初期化処理
	public void init(){
		//サーフェイスホルダーの生成
		holder = getHolder();
		holder.addCallback(this);

		//プッシュバッファの指定
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	//サーフェイス生成イベントの処理
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		//カメラの初期化
		try{
			camera = Camera.open();
			camera.setPreviewDisplay(holder);
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	//サーフェイス変更イベントの処理
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		//カメラプレビューの開始
		Parameters parameters = camera.getParameters();
		parameters.setPreviewSize(width, height);
		camera.setParameters(parameters);
		camera.startPreview();
	}

	//サーフェイス開放イベントの処理
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		//カメラのプレビュー停止
		camera.setPreviewCallback(null);
		camera.stopPreview();
		camera.release();
		camera = null;
	}
}
