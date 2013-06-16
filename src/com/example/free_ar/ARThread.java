package com.example.free_ar;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.text.NumberFormat;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLU;
import android.os.Handler;
import android.view.Display;
import android.view.MotionEvent;

public class ARThread extends Thread implements SensorEventListener{

	private final static double PI = 3.1415926535897932384626433; // 円周率
	private SensorManager sensorManager;// センサーマネージャ
	private float[] values = new float[7];// 加速度と傾き値
	private Handler handler = new Handler();

	NumberFormat nFormat = NumberFormat.getInstance();//桁数調整用

	private float[] mAccValues = {Float.MAX_VALUE, 0.0f, 0.0f};
	private float[] mGeoMatrix = {Float.MAX_VALUE, 0.0f, 0.0f};

	public boolean runFlag;
	public boolean makeModelFlag;
	public boolean loadFlag;

	public MainActivity activity;
	private GLRenderer renderer;
	private Resources res;

	public float eye_x = 0;
	public float eye_y = 0;
	public float eye_z = 0;
	public float eye_ori = 0;

	public float obj_x = 0;
	public float obj_y = 0;
	public float obj_z = 0;

	public float top_x = 0;
	public float top_y = 0;
	public float top_z = 0;

	private OpenMQO mqo;

	private int tall = 0;

	private float[] sun		 = {0.0f, 0.0f, -1.0f, 0.0f};
	private float[] lPosition  = {0.0f, 1.0f, 1.0f, 1.0f};
	private float[] lDirection = {0.0f, -1.0f, -1.0f, 1.0f};
	private float[] ambient    = {0.6f, 0.6f, 0.6f, 1.0f};

	//影表示用ポリゴン
	private float[] shadow_v = {
			-30.0f, 0.0f, -30.0f,
			-30.0f, 0.0f, 30.0f,
			30.0f, 0.0f, 30.0f,
			30.0f, 0.0f, -30.0f
	};

	private float[] shadow_n = {
			0.0f, 1.0f, 0.0f,
			0.0f, 1.0f, 0.0f,
			0.0f, 1.0f, 0.0f,
			0.0f, 1.0f, 0.0f
	};

	private float[] shadow_c = {
			1.0f, 1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f, 1.0f,
			1.0f, 1.0f, 1.0f, 1.0f
	};

	private float[] shadow_t = {
			0.0f, 0.0f,
			0.0f, 1.0f,
			1.0f, 1.0f,
			1.0f, 0.0f
	};

	private short[] shadow_i = {
			0, 3, 1,
			3, 2, 1
	};

	private FloatBuffer shadow_vBuffer;
	private FloatBuffer shadow_nBuffer;
	private FloatBuffer shadow_cBuffer;
	private FloatBuffer shadow_tBuffer;
	private ShortBuffer shadow_iBuffer;

	private int shadow_texId;

	//コンストラクタ
	public ARThread(MainActivity act, GLRenderer rend){
		this.activity = act;
		this.renderer = rend;
		res = activity.getResources();

		//有効桁数設定
		nFormat.setMaximumFractionDigits(1);

		shadow_vBuffer = OpenMQO.makeFloatBuffer(shadow_v);
		shadow_nBuffer = OpenMQO.makeFloatBuffer(shadow_n);
		shadow_cBuffer = OpenMQO.makeFloatBuffer(shadow_c);
		shadow_tBuffer = OpenMQO.makeFloatBuffer(shadow_t);
		shadow_iBuffer = OpenMQO.makeShortBuffer(shadow_i);

        runFlag = true;
	}

	//身長のセット
	public void setTall(int tall){
		this.tall = tall;
	}

	//センサーリスナーの処理
	@Override
	public void onSensorChanged(SensorEvent event) {

		switch(event.sensor.getType()){
		case Sensor.TYPE_MAGNETIC_FIELD:
			//磁気センサーからのコールバック
			mGeoMatrix = event.values.clone();
			break;

		case Sensor.TYPE_ACCELEROMETER:
			//加速度センサーからのコールバック
			if(mAccValues[0] == Float.MAX_VALUE){
				mAccValues = event.values.clone();
			}else {
				//ローパスフィルタ
				mAccValues[0] = (event.values[0] * 0.2f) + (mAccValues[0] * 0.8f);
				mAccValues[1] = (event.values[1] * 0.2f) + (mAccValues[1] * 0.8f);
				mAccValues[2] = (event.values[2] * 0.2f) + (mAccValues[2] * 0.8f);
			}
			break;
		}

		if(mGeoMatrix[0] != Float.MAX_VALUE && mAccValues[0] != Float.MAX_VALUE){
			float[] R = new float[16];
			float[] I = new float[16];
			//回転行列の計算
			SensorManager.getRotationMatrix(R, I, mAccValues, mGeoMatrix);

			float[] orientation = new float[3];

			//座標から角度への変換
			SensorManager.getOrientation(R, orientation);
			//角度への変換と方向の補正

			double degress = Math.toDegrees(orientation[0]);

			//マイナス値になった場合の考慮
			if(degress < 0){
				degress = 360 + degress;
			}

			Display display = activity.getWindowManager().getDefaultDisplay();
			int rotate = display.getRotation() * 90;

			degress = (degress + rotate) % 360;

			//端末の傾きを計算
			double g=9.8, gx, gy, gz, gxz, gp, gr;
			gx = mAccValues[0];
			gy = mAccValues[1];
			gz = mAccValues[2];

			gxz = Math.sqrt(gx*gx + gz*gz);

			//ピッチとロールをラジアンで取得
			gr = Math.atan2(gy / g, gxz / g);
			gp = Math.atan2(gz / gxz, gx / gxz);

			//角度に変換
			gr = gr / PI * 180;
			gp = gp / PI * 180;

			values[0] = (float)gx;
			values[1] = (float)gy;
			values[2] = (float)gz;
			values[3] = (float)degress;
			values[4] = (float)gr;
			values[5] = (float)gp;
			values[6] = (float)(degress - eye_ori);

	        obj_y = (float)(eye_y - 100 * Math.cos((90-values[5])/180 * PI));
			obj_x = (float)(100 * Math.sin((90-values[5])/180 * PI) * Math.sin(values[6]/180 * PI));
			obj_z = (float)(eye_z - 100 * Math.sin((90-values[5])/180 * PI) * Math.cos(values[6]/180 * PI));

			top_x = (float)Math.sin(values[4]/180 * PI);
			top_y = (float)Math.cos(values[4]/180 * PI);
			top_z = 0;

			double distance = tall * Math.tan((90-values[5])/180 * PI);

	        String[] text = new String[8];
	        text[0] = "方位："+degress;
	        text[1] = "X軸方向加速度："+gx;
	        text[2] = "Y軸方向加速度："+gy;
	        text[3] = "Z軸方向加速度："+gz;
	        text[4] = "加速度ロール：" +gr;
	        text[5] = "加速度ピッチ：" +gp;
	        text[6] = "ヘディング："+values[6];
	        text[7] = "距離：" + distance;

	        activity.sView.setText(text);
		}

	}

	//センサー精度変更イベント
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	//タッチイベントの処理
	public void touchEvent(MotionEvent event){
		float touch_x = event.getX();
		float touch_y = event.getY();

		int action = event.getAction();

		if((action & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_DOWN){
			if(touch_x >= renderer.WIDTH*3/4){
				if(touch_y <= renderer.HEIGHT/4){
					tall = 0;
					makeModelFlag = false;
			        handler.post(new Runnable(){
			        	@Override
			        	public void run(){
			        		activity.getTall();
			        	}
			        });

				}else if(touch_y >= renderer.HEIGHT*3/4){
					if(makeModelFlag || !loadFlag)
						return;

					double distance = tall * Math.tan((90-values[5])/180 * PI);

					eye_x = 0;
					eye_y = tall;
					eye_z = (float)distance;
					eye_ori = values[3];

					obj_y = (float)(eye_y - 100 * Math.cos((90-values[5])/180 * PI));
					obj_z = (float)(eye_z - 100 * Math.sin((90-values[5])/180 * PI));

					top_x = (float)Math.sin(values[4]/180 * PI);
					top_y = (float)Math.cos(values[4]/180 * PI);
					top_z = 0;

					makeModelFlag = true;
				}
			}
		}
	}

	//メインスレッド
	public void run(){

		try{
			activity.glView.queueEvent(new Runnable(){
				public void run(){
					shadow_texId = GLRenderer.loadTexture(
							renderer.gl, BitmapFactory.decodeResource(
									res, res.getIdentifier(
											"shadow", "drawable", activity.getPackageName())));

					mqo = new OpenMQO(renderer.gl, res, "miku.mqo", activity);
					loadFlag = true;
					activity.sView.setFlag(true);
				}
			});
		}catch (Exception e){
			e.printStackTrace();
		}

		//センサーマネージャーの取得
		sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);

		//センサーの取得
		List<Sensor> list;
		list = sensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
		if (list.size() > 0)
			sensorManager.registerListener(this, list.get(0),
	                SensorManager.SENSOR_DELAY_GAME);

		list = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
		if (list.size() > 0)
			sensorManager.registerListener(this, list.get(0),
	                SensorManager.SENSOR_DELAY_GAME);

        handler.post(new Runnable(){
        	@Override
        	public void run(){
        		activity.getTall();
        	}
        });

		while(runFlag){

		}

		sensorManager.unregisterListener(this);
	}

	//描画処理
	public void subRenderer(GL10 gl){
		if(!makeModelFlag || mqo == null)
			return;

		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();

		GLU.gluLookAt(gl, eye_x, eye_y, eye_z, obj_x, obj_y, obj_z, top_x, top_y, top_z);
		gl.glTranslatef(0, mqo.tall/2, 0);
		gl.glScalef(mqo.scale, mqo.scale, mqo.scale);

		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, sun, 0);
		//gl.glLightfv(GL10.GL_LIGHT1, GL10.GL_POSITION, lPosition, 0);
		//gl.glLightfv(GL10.GL_LIGHT1, GL10.GL_SPOT_DIRECTION, lDirection, 0);
		gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT, ambient, 0);

		//影の表示
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		gl.glBindTexture(GL10.GL_TEXTURE_2D, shadow_texId);
		gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, shadow_tBuffer);

		// 頂点のポインタを設定
		gl.glVertexPointer(3, GL10.GL_FLOAT, 0, shadow_vBuffer);

		//法線のポインタを設定
		gl.glNormalPointer(GL10.GL_FLOAT, 0, shadow_nBuffer);

		//色のポインタを指定
		gl.glColorPointer(4, GL10.GL_FLOAT, 0, shadow_cBuffer);

		// 時計周りの面を表に指定
		gl.glFrontFace(GL10.GL_CW);

		// オブジェクトの描画
		gl.glDrawElements(GL10.GL_TRIANGLES, shadow_i.length, GL10.GL_UNSIGNED_SHORT, shadow_iBuffer);

		gl.glDisable(GL10.GL_TEXTURE_2D);
		gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

		for(int i=0; i<mqo.oNum; i++){

			//拡散光の設定
			gl.glMaterialfv(GL10.GL_FRONT, GL10.GL_DIFFUSE, mqo.difColor[mqo.mIndex[i]], 0);

			//周囲光の設定
			gl.glMaterialfv(GL10.GL_FRONT, GL10.GL_AMBIENT, mqo.ambColor[mqo.mIndex[i]], 0);

			//自己照明の設定
			gl.glMaterialfv(GL10.GL_FRONT, GL10.GL_EMISSION, mqo.emiColor[mqo.mIndex[i]], 0);

			//反射光の設定
			gl.glMaterialfv(GL10.GL_FRONT, GL10.GL_SPECULAR, mqo.spcColor[mqo.mIndex[i]], 0);

			//反射強度の設定
			gl.glMaterialf(GL10.GL_FRONT, GL10.GL_SHININESS, mqo.power[mqo.mIndex[i]]);

			if(mqo.textureId[mqo.mIndex[i]] != -1){
				gl.glEnable(GL10.GL_TEXTURE_2D);
				gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
				gl.glBindTexture(GL10.GL_TEXTURE_2D, mqo.textureId[mqo.mIndex[i]]);
				gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mqo.textureBuffer[i]);
			}

			// 頂点のポインタを設定
			gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mqo.vertexBuffer[i]);

			//法線のポインタを設定
			gl.glNormalPointer(GL10.GL_FLOAT, 0, mqo.normalBuffer[i]);

			//色のポインタを指定
			gl.glColorPointer(4, GL10.GL_FLOAT, 0, mqo.colorBuffer[i]);

			// 時計周りの面を表に指定
			gl.glFrontFace(GL10.GL_CW);

			// オブジェクトの描画
			gl.glDrawElements(GL10.GL_TRIANGLES, mqo.indices[i].length, GL10.GL_UNSIGNED_SHORT, mqo.indexBuffer[i]);

			gl.glDisable(GL10.GL_TEXTURE_2D);
			gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
		}
	}
}
