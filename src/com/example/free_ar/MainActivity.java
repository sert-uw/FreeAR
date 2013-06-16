package com.example.free_ar;

import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

public class MainActivity extends Activity {
	public StringView sView;
	public GLSurfaceView glView;
	public GLRenderer rnd;

	private Spinner spinner;
	public int tall = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		//StringViewの生成
		sView = new StringView(this);

		//CameraViewの生成
		CameraView cView = new CameraView(this);

		//GLSurfaceViewの生成
		glView = new GLSurfaceView(this);

		glView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
		rnd = new GLRenderer(this);
		glView.setRenderer(rnd);
		glView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
		setContentView(glView);
		addContentView(sView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		addContentView(cView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event){
		rnd.ar.touchEvent(event);
		return true;
	}

	//バックキーが押されたときの処理
	@Override
	public boolean dispatchKeyEvent(KeyEvent e){
		if(e.getAction() == KeyEvent.ACTION_DOWN){
			if(e.getKeyCode() == KeyEvent.KEYCODE_BACK){
				sView.thread = null;
				rnd.ar.runFlag = false;
				this.finish();
			}
		}
		return super.dispatchKeyEvent(e);
	}

	//ホームボタンが押されたり、他のアプリが起動したときの処理
	@Override
	public void onUserLeaveHint(){
		sView.thread = null;
		rnd.ar.runFlag = false;
		this.finish();
	}

	//身長取得
	public void getTall(){
		//スピナーの生成
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(
				this, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(
				android.R.layout.simple_spinner_dropdown_item);

		for(int i=50; i<300; i += 10)
			adapter.add(String.valueOf(i) + "cm");
		spinner = new Spinner(this);
		spinner.setAdapter(adapter);
		spinner.setSelection(10);

		textDialog(this, "端末の床からの高さを選択", spinner,
				new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(which == DialogInterface.BUTTON_POSITIVE){
					try{
						tall = Integer.parseInt(spinner.getSelectedItem().toString().replaceAll("[^0-9]", ""));
						sView.setTall(tall);
						rnd.ar.setTall(tall);
					}catch (Exception e){
						e.printStackTrace();
					}
				}
			}
		});
	}

	//スピナーダイアログ
	private static void textDialog(Context context, String title,
			Spinner spinner, DialogInterface.OnClickListener listener){
		AlertDialog.Builder ad = new AlertDialog.Builder(context);
		ad.setTitle(title);
		ad.setView(spinner);
		ad.setPositiveButton("OK", listener);
		ad.show();
	}

}
