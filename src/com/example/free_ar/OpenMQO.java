package com.example.free_ar;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.StringTokenizer;

import javax.microedition.khronos.opengles.GL10;

import android.content.res.Resources;
import android.graphics.BitmapFactory;

public class OpenMQO {

	public FloatBuffer[] vertexBuffer;// 頂点バッファ
	public FloatBuffer[] textureBuffer;// テクスチャバッファ
	public FloatBuffer[] colorBuffer;//カラーバッファ
	public FloatBuffer[] normalBuffer;//法線バッファ
	public ShortBuffer[] indexBuffer; // インデックスバッファ
	public int[]		  textureId;//テクスチャーID

	public short[][] indices = new short[20][];//面を構成する頂点
	public float[][] vertices = new float[20][];//頂点座標
	public float[][] vTextures = new float[20][];//頂点に対するテクスチャー座標
	public float[][] fTextures = new float[20][];//面に対するテクスチャー座標
	public float[][] fieldNl = new float[20][];//面法線ベクトル
	public float[][] vertixNl = new float[20][];//頂点法線ベクトル
	public float[][] verColors;
	public float[][] colors;//ベースカラー

	public int[] mIndex = new int[20];//マテリアルインデックス
	public float[] dif;//拡散光
	public float[] amb;//周囲光
	public float[] emi;//自己照明
	public float[] spc;//反射光
	public float[] power;//反射光の強さ
	public String[] texName;//テクスチャーパス

	public float[][] difColor;//拡散光色
	public float[][] ambColor;//周囲光色
	public float[][] emiColor;//自己照明色
	public float[][] spcColor;//反射光色

	public int tall = 0;

	public float scale = 1;

	public int oNum = 0;
	public int mNum = 0;
	public int vNum[] = new int[20];
	public int fNum[] = new int[20];

	public OpenMQO(GL10 gl, Resources res, String fileName, MainActivity activity){
		boolean mFlag = false;
		boolean vFlag = false;
		boolean fFlag = false;
		boolean mLoadFlag = false;

		String readData = null;
		StringTokenizer st;

		int count = 0;
		int count2 = 0;
		int objCount = -1;

		int iNum;

		try{
			BufferedReader br = new BufferedReader(new InputStreamReader(
					activity.getResources().getAssets().open(fileName)));

			String str;
			while((str = br.readLine()) != null){
				//マテリアル情報の読み込み準備
				if(str.indexOf("Material") != -1 && str.indexOf("{") != -1){
					mNum = Integer.parseInt(str.replaceAll("[^0-9]", ""));
					colors = new float[mNum][4];
					dif = new float[mNum];
					amb = new float[mNum];
					emi = new float[mNum];
					spc = new float[mNum];
					power = new float[mNum];
					texName = new String[mNum];
					mFlag = true;
					count = 0;

				}

				//各オブジェクトの開始を確認
				else if(str.indexOf("Object") != -1 && str.indexOf("{") != -1){
					objCount++;
					mLoadFlag = false;
					count = 0;
					count2 = 0;
				}

				//頂点情報の読み込み準備
				else if(str.indexOf("vertex") != -1 && str.indexOf("{") != -1){
					vNum[objCount] = Integer.parseInt(str.replaceAll("[^0-9]", ""));
					vertices[objCount] = new float[vNum[objCount]*3];
					vTextures[objCount] = new float[vNum[objCount]*2];
					vFlag = true;
					count = 0;

				}

				//面情報の読み込み準備
				else if(str.indexOf("face") != -1 && str.indexOf("{") != -1){
					fNum[objCount] = Integer.parseInt(str.replaceAll("[^0-9]", ""));
					indices[objCount] = new short[fNum[objCount]*3];
					fTextures[objCount] = new float[fNum[objCount]*6];
					fFlag = true;
					count = 0;

				}else{

					//マテリアル情報の読み出し
					if(mFlag){
						if(str.trim().equals("}")){
							mFlag = false;
							continue;
						}

						if((iNum = str.indexOf("dif")) != -1)
							dif[count/4] = Float.parseFloat(str.substring(
									str.indexOf("(", iNum) + 1, str.indexOf(")", iNum)));

						if((iNum = str.indexOf("amb")) != -1)
							amb[count/4] = Float.parseFloat(str.substring(
									str.indexOf("(", iNum) + 1, str.indexOf(")", iNum)));

						if((iNum = str.indexOf("emi")) != -1)
							emi[count/4] = Float.parseFloat(str.substring(
									str.indexOf("(", iNum) + 1, str.indexOf(")", iNum)));

						if((iNum = str.indexOf("spc")) != -1)
							spc[count/4] = Float.parseFloat(str.substring(
									str.indexOf("(", iNum) + 1, str.indexOf(")", iNum)));

						if((iNum = str.indexOf("power")) != -1)
							power[count/4] = Float.parseFloat(str.substring(
									str.indexOf("(", iNum) + 1, str.indexOf(")", iNum)));

						if((iNum = str.indexOf("tex")) != -1){
							texName[count/4] = str.substring(
									str.indexOf("(", iNum) + 1, str.indexOf(")", iNum));
							if((iNum = texName[count/4].indexOf("/")) != -1){
								texName[count/4] = texName[count/4].substring(iNum + 1);
							}
							texName[count/4] = texName[count/4].substring(0, texName[count/4].indexOf("."));
						}

						if((iNum = str.indexOf("col")) != -1){
							readData = str.substring(
									str.indexOf("(", iNum) + 1, str.indexOf(")", iNum));
							st = new StringTokenizer(readData, " ");
							while(st.hasMoreTokens()){
								colors[count/4][count%4] = Float.parseFloat(st.nextToken());
								count++;
							}
						}
					}

					//頂点情報の読み出し
					else if(vFlag){
						if(str.trim().equals("}")){
							vFlag = false;
							continue;
						}

						readData = str.trim();
						st = new StringTokenizer(readData, " ");
						while(st.hasMoreTokens()){
							vertices[objCount][count] = Float.parseFloat(st.nextToken());
							count++;
						}

					}

					//面情報の読み出し
					else if(fFlag){
						if(str.trim().equals("}")){
							fFlag = false;
							continue;
						}

						if(str.indexOf("M") != -1 && !mLoadFlag){
							mIndex[objCount] = Integer.parseInt(str.substring(
									str.indexOf("(", str.indexOf("M")) + 1, str.indexOf(")", str.indexOf("M"))));
							mLoadFlag = true;
						}

						readData = str.substring(str.indexOf("(") + 1, str.indexOf(")"));
						st = new StringTokenizer(readData, " ");
						while(st.hasMoreTokens()){
							indices[objCount][count] = Short.parseShort(st.nextToken());
							count++;
						}

						if((iNum = str.indexOf("UV")) != -1){
							readData = str.substring(str.indexOf("(", iNum) + 1, str.indexOf(")", iNum));
							st = new StringTokenizer(readData, " ");
							while(st.hasMoreTokens()){
								fTextures[objCount][count2] = Float.parseFloat(st.nextToken());
								count2++;
							}
						}
					}
				}
			}

			br.close();
		}catch (Exception e){
			e.printStackTrace();
		}finally {

		}

		textureId = new int[mNum];

		for(int i=0; i<mNum; i++){
			if(texName[i] != null){
				textureId[i] = GLRenderer.loadTexture(
						gl, BitmapFactory.decodeResource(
								res, res.getIdentifier(
										texName[i], "drawable", activity.getPackageName())));
			}else{
				textureId[i] = -1;
			}
		}

		oNum = objCount + 1;

		//各色の設定
		difColor = new float[mNum][4];
		ambColor = new float[mNum][4];
		emiColor = new float[mNum][4];
		spcColor = new float[mNum][4];
		verColors = new float[oNum][];

		for(int o=0; o<oNum; o++){
			verColors[o] = new float[vNum[o]*4];

			for(int i=0; i<vNum[o]*4; i++){
				verColors[o][i] = colors[mIndex[o]][i%4];
			}
		}

		for(int i=0; i<mNum; i++){
			for(int j=0; j<3; j++){
				difColor[i][j] = dif[i];
				ambColor[i][j] = amb[i];
				emiColor[i][j] = emi[i];
				spcColor[i][j] = spc[i];
			}
			difColor[i][3] = 1.0f;
			ambColor[i][3] = 1.0f;
			emiColor[i][3] = 1.0f;
			spcColor[i][3] = 1.0f;
		}

		for(int i=0; i<colors.length; i++){
			difColor[i/4][i%4] = colors[i/4][i%4] * dif[i/4];
			ambColor[i/4][i%4] = colors[i/4][i%4] * dif[i/4];
			emiColor[i/4][i%4] = colors[i/4][i%4] * dif[i/4];
			spcColor[i/4][i%4] = colors[i/4][i%4] * dif[i/4];
		}

		float[] v0 = new float[3];
		float[] v1 = new float[3];
		float[] v2 = new float[3];

		float length;

		fieldNl = new float[oNum][];
		vertixNl = new float[oNum][];

		//各面の法線ベクトルの算出
		for(int o=0; o<oNum; o++){
			//法線ベクトルの算出
			fieldNl[o] = new float[fNum[o]*3];
			vertixNl[o] = new float[vNum[o]*3];

			for(int i=0; i<fNum[o]; i++){
				v0[0] = vertices[o][indices[o][i*3]*3];
				v0[1] = vertices[o][indices[o][i*3]*3 + 1];
				v0[2] = vertices[o][indices[o][i*3]*3 + 2];

				v1[0] = vertices[o][indices[o][i*3 + 1]*3];
				v1[1] = vertices[o][indices[o][i*3 + 1]*3 + 1];
				v1[2] = vertices[o][indices[o][i*3 + 1]*3 + 2];

				v2[0] = vertices[o][indices[o][i*3 + 2]*3];
				v2[1] = vertices[o][indices[o][i*3 + 2]*3 + 1];
				v2[2] = vertices[o][indices[o][i*3 + 2]*3 + 2];

				fieldNl[o][i*3]     = (v1[1] - v0[1])*(v2[2] - v0[2]) - (v1[2] - v0[2])*(v2[1] - v0[1]);
				fieldNl[o][i*3 + 1] = (v1[2] - v0[2])*(v2[0] - v0[0]) - (v1[0] - v0[0])*(v2[2] - v0[2]);
				fieldNl[o][i*3 + 2] = (v1[0] - v0[0])*(v2[1] - v0[1]) - (v1[1] - v0[1])*(v2[0] - v0[0]);

				length = (float)Math.sqrt(
						Math.pow(fieldNl[o][i*3], 2) +
						Math.pow(fieldNl[o][i*3 + 1], 2) +
						Math.pow(fieldNl[o][i*3 + 2], 2));

				fieldNl[o][i*3]     /= length;
				fieldNl[o][i*3 + 1] /= length;
				fieldNl[o][i*3 + 2] /= length;
			}
		}

		//各頂点の法線ベクトルの算出
		for(int o=0; o<oNum; o++){
			for(int i=0; i<fNum[o]; i++){
				vertixNl[o][indices[o][i*3]*3]     += fieldNl[o][i*3];
				vertixNl[o][indices[o][i*3]*3 + 1] += fieldNl[o][i*3 + 1];
				vertixNl[o][indices[o][i*3]*3 + 2] += fieldNl[o][i*3 + 2];

				vertixNl[o][indices[o][i*3 + 1]*3]     += fieldNl[o][i*3];
				vertixNl[o][indices[o][i*3 + 1]*3 + 1] += fieldNl[o][i*3 + 1];
				vertixNl[o][indices[o][i*3 + 1]*3 + 2] += fieldNl[o][i*3 + 2];

				vertixNl[o][indices[o][i*3 + 2]*3]     += fieldNl[o][i*3];
				vertixNl[o][indices[o][i*3 + 2]*3 + 1] += fieldNl[o][i*3 + 1];
				vertixNl[o][indices[o][i*3 + 2]*3 + 2] += fieldNl[o][i*3 + 2];
			}
		}

		for(int o=0; o<oNum; o++){
			for(int i=0; i<vNum[o]; i++){
				length = (float)Math.sqrt(
						Math.pow(vertixNl[o][i*3], 2) +
						Math.pow(vertixNl[o][i*3 + 1], 2) +
						Math.pow(vertixNl[o][i*3 + 2], 2));

				vertixNl[o][i*3]     /= length;
				vertixNl[o][i*3 + 1] /= length;
				vertixNl[o][i*3 + 2] /= length;
			}
		}

		//各頂点に対するテクスチャー座標を決定する
		for(int o=0; o<oNum; o++){
			if(textureId[mIndex[o]] == -1)
				continue;

			for(int i=0; i<fNum[o]; i++){
				vTextures[o][indices[o][i*3]*2]     = fTextures[o][i*6];
				vTextures[o][indices[o][i*3]*2 + 1] = fTextures[o][i*6 + 1];

				vTextures[o][indices[o][i*3 + 1]*2]     = fTextures[o][i*6 + 2];
				vTextures[o][indices[o][i*3 + 1]*2 + 1] = fTextures[o][i*6 + 3];

				vTextures[o][indices[o][i*3 + 2]*2]     = fTextures[o][i*6 + 4];
				vTextures[o][indices[o][i*3 + 2]*2 + 1] = fTextures[o][i*6 + 5];
			}
		}

		vertexBuffer = new FloatBuffer[oNum];// 頂点バッファ
		textureBuffer = new FloatBuffer[oNum];// テクスチャバッファ
		colorBuffer = new FloatBuffer[oNum];//カラーバッファ
		normalBuffer = new FloatBuffer[oNum];//法線バッファ
		indexBuffer = new ShortBuffer[oNum]; // インデックスバッファ

		for(int o=0; o<oNum; o++){
			// index
			indexBuffer[o] = makeShortBuffer(indices[o]);

			// vertex
			vertexBuffer[o] = makeFloatBuffer(vertices[o]);

			// texture
			textureBuffer[o] = makeFloatBuffer(vTextures[o]);

			// normal
			normalBuffer[o] = makeFloatBuffer(vertixNl[o]);

			// Color
			colorBuffer[o] = makeFloatBuffer(verColors[o]);
		}

	}

	// float配列→floatバッファ
	public static FloatBuffer makeFloatBuffer(float[] array) {
		FloatBuffer fb = ByteBuffer.allocateDirect(array.length * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();
		fb.put(array).position(0);
		return fb;
	}

	public static ShortBuffer makeShortBuffer(short[] arr) {
		ByteBuffer bb = ByteBuffer.allocateDirect(arr.length * 4);
		bb.order(ByteOrder.nativeOrder());
		ShortBuffer fb = bb.asShortBuffer();
		fb.put(arr);
		fb.position(0);
		return fb;
	}
}
