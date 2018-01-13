package cz.javlik.glsl08;
// changed in xml - for an xchg test
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.*;
import android.hardware.*;
import android.widget.*;

import static android.opengl.GLES20.GL_CLAMP_TO_EDGE;
import static android.opengl.GLES20.GL_COLOR_ATTACHMENT0;
import static android.opengl.GLES20.GL_FRAMEBUFFER;
import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_RENDERBUFFER;
import static android.opengl.GLES20.GL_RGBA;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_MIN_FILTER;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_S;
import static android.opengl.GLES20.GL_TEXTURE_WRAP_T;
import static android.opengl.GLES20.GL_UNSIGNED_BYTE;


public class MainActivity extends Activity  {
    public static final String TAG = "glsl08:";
    private GLSurfaceView mGLSurfaceView;
    private ShaderRenderer mRenderer;
    private float mPreviousX, mPreviousY;
    private Sensor mSensor;
    private SensorManager mSensorManager;
	private TextView tv0, tv1, tv2;
	Ael ael;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mGLSurfaceView = new GLSurfaceView(this);
        mGLSurfaceView.setEGLContextClientVersion(2);
        mRenderer = new ShaderRenderer();
        mGLSurfaceView.setRenderer(mRenderer);
        mGLSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mGLSurfaceView.setKeepScreenOn(true);
        setContentView(mGLSurfaceView);
		/*setContentView(R.layout.activity_main);
		tv0 = (TextView)findViewById(R.id.activityMainTextView0);
		tv1 = (TextView)findViewById(R.id.activityMainTextView1);
		tv2 = (TextView)findViewById(R.id.activityMainTextView2);
        */
		ael = new Ael();
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
		mSensorManager.registerListener(ael, mSensor, SensorManager.SENSOR_DELAY_GAME);
        //mGLSurfaceView.setOnTouchListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        mGLSurfaceView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /*	@Override
     public boolean onTouch(View v, MotionEvent event) {
     //if(event.getAction() == MotionEvent.ACTION_MOVE) {
     mRenderer.mMouse[0] = event.getX();
     mRenderer.mMouse[1] = event.getY();
     //}
     return false;
     }
     */
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, we are only
        // interested in events where the touch position changed.
        if (e.getAction() == MotionEvent.ACTION_MOVE)
        {
            mRenderer.mMouse[0] = e.getX();
            mRenderer.mMouse[1] = e.getY();
        }

		/*
		 switch (e.getAction()) {
		 case MotionEvent.ACTION_MOVE:

		 /*
		 float dx = x - mPreviousX;
		 float dy = y - mPreviousY;

		 // reverse direction of rotation above the mid-line
		 if (y > getHeight() / 2) {
		 dx = dx * -1 ;
		 }

		 // reverse direction of rotation to left of the mid-line
		 if (x < getWidth() / 2) {
		 dy = dy * -1 ;
		 }

		 mRenderer.setAngle(
		 mRenderer.getAngle() +
		 ((dx + dy) * TOUCH_SCALE_FACTOR));  // = 180.0f / 320
		 requestRender();

		 }

		 mPreviousX = x;
		 mPreviousY = y;
		 return true;
		 */
        return false;
    }

	protected class Ael implements SensorEventListener
	{

		@Override
		public void onSensorChanged(SensorEvent p1)
		{
			// TODO: Implement this method
			tv0.setText("" + p1.values[0]);
			tv1.setText("" + p1.values[1]);
			tv2.setText("" + p1.values[2]);
		}

		@Override
		public void onAccuracyChanged(Sensor p1, int p2)
		{
			// TODO: Implement this method
		}


	}

    protected class ShaderRenderer implements GLSurfaceView.Renderer {
        private static final int FLOAT_SIZE_BYTES = Float.SIZE / Byte.SIZE;
        private FloatBuffer mRectData;
        private String mVertexShader;
        private String mFragmentShader;
        private float[] mResolution;
        private int miResolutionHandle;
        private int miGlobalTimeHandle;
        private int miMouseHandle;
        private IntBuffer frameBuffer;
        private IntBuffer texture;


        private float[] mMouse = new float[] {0,0};
        private int maPositionHandle;
        private long mStartTime;

        private ShaderRenderer() {
            float[] rectData = new float[]{
                    -1f, -1f, -1f, 1f,
                    1f, -1f, 1f, 1f,
            };

            mRectData = ByteBuffer.allocateDirect(rectData.length * FLOAT_SIZE_BYTES)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            mRectData.put(rectData).position(0);

            mVertexShader = readShader(R.raw.vertex_shader, MainActivity.this);
            mFragmentShader = readShader(R.raw.waves, MainActivity.this);
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            int program = createProgram(mVertexShader, mFragmentShader);

            miGlobalTimeHandle = GLES20.glGetUniformLocation(program, "time");
            miResolutionHandle = GLES20.glGetUniformLocation(program, "resolution");
            miMouseHandle = GLES20.glGetUniformLocation(program, "mouse");

            GLES20.glUseProgram(program);
            GLES20.glEnableVertexAttribArray(maPositionHandle);
            mRectData.position(0);
            GLES20.glVertexAttribPointer(maPositionHandle, 2, GLES20.GL_FLOAT, false, 0, mRectData);

            mStartTime = SystemClock.elapsedRealtime();






        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            mResolution = new float[] {width, height};
            GLES20.glViewport(0, 0, width, height);

            GLES20.glGenFramebuffers(1, frameBuffer);
            GLES20.glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);

//Create a texture
            GLES20.glGenTextures(1, texture);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

//Attach the texture to the framebuffer
            GLES20.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, texture, 0);

        }

        @Override
        public void onDrawFrame(GL10 gl) {
            GLES20.glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer);
            GLES20.glBindRenderbuffer(GL_RENDERBUFFER, texture);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);

            // :: uniforms
            GLES20.glUniform2fv(miMouseHandle, 1, mMouse, 0);
            GLES20.glUniform2fv(miResolutionHandle, 1, mResolution, 0);
            long nowInSec = SystemClock.elapsedRealtime();
            GLES20.glUniform1f(miGlobalTimeHandle, ((float) (nowInSec - mStartTime)) / 1000f);
            // uniforms ::

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            mGLSurfaceView.requestRender();
        }

        private int loadShader(int shaderType, String source) {
            int shader = GLES20.glCreateShader(shaderType);
            if (shader != 0) {
                GLES20.glShaderSource(shader, source);
                GLES20.glCompileShader(shader);
                int[] compiled = new int[1];
                GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
                if (compiled[0] == 0) {
                    Log.e(TAG, "Could not compile shader " + shaderType + ":");
                    Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
                    GLES20.glDeleteShader(shader);
                    shader = 0;
                }
            }
            return shader;
        }

        private int createProgram(String vertexSource, String fragmentSource) {
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource);
            if (vertexShader == 0) {
                return 0;
            }

            int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource);
            if (pixelShader == 0) {
                return 0;
            }

            int program = GLES20.glCreateProgram();
            if (program != 0) {
                GLES20.glAttachShader(program, vertexShader);
                GLES20.glAttachShader(program, pixelShader);
                GLES20.glLinkProgram(program);
                int[] linkStatus = new int[1];
                GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
                if (linkStatus[0] != GLES20.GL_TRUE) {
                    Log.e(TAG, "Could not link program: ");
                    Log.e(TAG, GLES20.glGetProgramInfoLog(program));
                    GLES20.glDeleteProgram(program);
                    program = 0;
                }
            }
            return program;
        }

        private String readShader(int resource, Context context) {
            Resources resources = context.getResources();
            InputStream inputStream = resources.openRawResource(resource);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            char[] buffer = new char[32];
            int charsRead;
            StringBuilder stringBuilder = new StringBuilder();

            try {
                while ((charsRead = bufferedReader.read(buffer)) != -1) {
                    stringBuilder.append(buffer, 0, charsRead);
                }
            } catch (IOException e) {
                Log.e(TAG, "Reading rsrc error" + resource, e);
            }

            return stringBuilder.toString();
        }
    }
}

