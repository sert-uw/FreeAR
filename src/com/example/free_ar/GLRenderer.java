package com.example.free_ar;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;

//レンダラー
public class GLRenderer implements GLSurfaceView.Renderer {

	double WIDTH;
	double HEIGHT;

	double aspect;

	public ARThread ar;
	public GL10 gl;

	public GLRenderer(MainActivity activity){
		ar = new ARThread(activity, this);
	}

	// サーフェイス生成時に呼ばれる
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig eglConfig) {
		this.gl = gl;

		// 頂点配列の有効化
		gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
		gl.glEnableClientState(GL10.GL_NORMAL_ARRAY);
		gl.glEnable(GL10.GL_BLEND);
		gl.glEnable(GL10.GL_DEPTH_TEST);
		gl.glEnable(GL10.GL_LIGHTING);
		gl.glEnable(GL10.GL_LIGHT0);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(GL10.GL_CULL_FACE);
		gl.glShadeModel(GL10.GL_SMOOTH);

		ar.start();
	}

	// 画面サイズ変更時に呼ばれる
	@Override
	public void onSurfaceChanged(GL10 gl, int w, int h) {

		WIDTH = w;
		HEIGHT = h;

		System.out.println(w + " " + h);

		// 画面の表示領域の指定
		gl.glViewport(0, 0, w, h);

		aspect = WIDTH / HEIGHT;

		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();

		GLU.gluPerspective(gl, 40, (float)aspect, 50, 1000);
	}

	// 毎フレーム描画時に呼ばれる
	@Override
	public void onDrawFrame(GL10 gl) {
		try{
			// 画面のクリア
	        gl.glClear(
	        		GL10.GL_COLOR_BUFFER_BIT |
	        		GL10.GL_DEPTH_BUFFER_BIT
	        		);
	        if(ar != null)
	        	ar.subRenderer(gl);
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	public static final int loadTexture(GL10 gl, Bitmap bmp){
        //テクスチャメモリの確保
		int[] textureIds=new int[1];
		gl.glGenTextures(1,textureIds,0);
		//テクスチャへのビットマップ指定
		gl.glActiveTexture(GL10.GL_TEXTURE0);
		gl.glBindTexture(GL10.GL_TEXTURE_2D, textureIds[0]);
		GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bmp, 0);
		//テクスチャフィルタの指定
		gl.glTexParameterf(GL10.GL_TEXTURE_2D,
				GL10.GL_TEXTURE_MIN_FILTER,GL10.GL_NEAREST);
		gl.glTexParameterf(GL10.GL_TEXTURE_2D,
				GL10.GL_TEXTURE_MAG_FILTER,GL10.GL_NEAREST);
		return textureIds[0];
	}
}
