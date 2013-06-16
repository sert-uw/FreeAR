package com.example.free_ar;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class StringView extends SurfaceView
	implements SurfaceHolder.Callback, Runnable{

	private SurfaceHolder holder;//ホルダー
	public Thread thread;

	private String[] str;

	private int tall = 0;

	private int w=0;
	private int h=0;

	private boolean loadFlag;

	//コンストラクタ
	public StringView(Context context) {
		super(context);

		init();
	}

	public StringView(Context context, AttributeSet attrs){
		super(context, attrs);

		init();
	}

	//初期化処理
	public void init(){
		//サーフェイスホルダーの生成
		holder = getHolder();
		holder.addCallback(this);
		holder.setFormat(PixelFormat.TRANSLUCENT);
		setZOrderOnTop(true);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		holder.setFixedSize(width, height);
		w = width;
		h = height;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		thread = new Thread(this);
		thread.start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		thread = null;
	}

	public void run(){
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setTypeface(Typeface.SANS_SERIF);

		Canvas canvas;
		while(thread != null){
			canvas = holder.lockCanvas();
			canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

			if(str != null){
				paint.setTextSize(30);
				paint.setColor(Color.RED);
				for(int i=0; i<str.length; i++){
					canvas.drawText(str[i], 0, 30 + 30*i, paint);
				}
			}

			if(tall != 0){
				paint.setTextSize(40);
				paint.setColor(Color.WHITE);
				canvas.drawText("高さ：" + tall + "cm", 0, h*5/6, paint);
			}

			paint.setTextSize(40);
			paint.setColor(Color.WHITE);
			canvas.drawText("状態のリセット", w*3/4, h*1/8, paint);

			if(loadFlag)
				canvas.drawText("モデルセット", w*3/4, h*7/8, paint);
			else
				canvas.drawText("Now Loading", w*3/4, h*7/8, paint);

			holder.unlockCanvasAndPost(canvas);
			try{
				Thread.sleep(50);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}

	public void setText(String[] str){
		this.str = str;
	}

	public void setTall(int tall){
		this.tall = tall;
	}

	public void setFlag(boolean flag){
		loadFlag = flag;
	}
}
