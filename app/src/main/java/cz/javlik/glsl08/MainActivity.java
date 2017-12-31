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
import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.*;
import android.hardware.*;


public class MainActivity extends Activity  {
    public static final String TAG = "glsl08:";
    private GLSurfaceView mGLSurfaceView;
    private ShaderRenderer mRenderer;
    private float mPreviousX, mPreviousY;
    private Sensor mSensor;
    private SensorManager mSensorManager;

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

        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
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



    protected class ShaderRenderer implements GLSurfaceView.Renderer {
        private static final int FLOAT_SIZE_BYTES = Float.SIZE / Byte.SIZE;
        private FloatBuffer mRectData;
        private String mVertexShader;
        private String mFragmentShader;
        private float[] mResolution;
        private int miResolutionHandle;
        private int miGlobalTimeHandle;
        private int miMouseHandle;

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
        }

        @Override
        public void onDrawFrame(GL10 gl) {
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

