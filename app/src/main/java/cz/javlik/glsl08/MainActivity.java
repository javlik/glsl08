package cz.javlik.glsl08;
// changed in xml - for an xchg test
import android.app.*;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.hardware.*;
import android.opengl.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.*;
import javax.microedition.khronos.egl.*;
import javax.microedition.khronos.opengles.*;

import android.graphics.Matrix;
import javax.microedition.khronos.egl.EGLConfig;


public class MainActivity extends Activity  {
	
    public static final String TAG = "glsl08:";
    private GLSurfaceView mGLSurfaceView;
    private ShaderRenderer mRenderer;
    private float mPreviousX, mPreviousY;
    private Sensor mSensor;
    private SensorManager mSensorManager;
	private TextView tv0, tv1, tv2;
	Ael ael;
	
	private void enabledFullScreenMode() {
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
							 WindowManager.LayoutParams.FLAG_FULLSCREEN);
		View decorView = getWindow().getDecorView();
		// Hide the status bar.
		int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
		decorView.setSystemUiVisibility(uiOptions);
	}
	

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
			
		ael = new Ael();
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
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
           // mRenderer.mMouse[0] = e.getX();
           // mRenderer.mMouse[1] = e.getY();
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
		double lastAngle1, lastAngle2;
		
		public Ael()
		{
			lastAngle1 = 1000f;
			lastAngle2 = 1000f;
		}

		@Override
		public void onSensorChanged(SensorEvent p1)
		{
			// TODO: Implement this method
			/*tv0.setText("" + p1.values[0]);
			tv1.setText("" + p1.values[1]);
			tv2.setText("" + p1.values[2]);*/
			double angle1 = Math.atan2(p1.values[2],p1.values[0]);
			double angle2 = Math.atan2(p1.values[0],p1.values[1]);
			
			if (lastAngle1+lastAngle2 != 2000f)
			{
				angle1 = (29*lastAngle1+angle1)/30;
				angle2 = (29*lastAngle2+angle2)/30;
			}
			
			mRenderer.mMouse[0] = (float)((angle1)-Math.PI/2f);
			mRenderer.mMouse[1] = (float)((angle2));
			
			lastAngle1=angle1;
			lastAngle2=angle2;
			
			//tv0.setText("" + mRenderer.mMouse[0]);
			//tv1.setText("" + mRenderer.mMouse[1]);
			
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
		private int miPosHandle;
        private float [] mReducedResolution;

		private int mFrameBufferObject;
		//private int mRenderBufferObject;
		private int mTexture;
		private int tempArray[];

        private float[] mMouse = new float[] {0,0,0};
		private float[] mPos = new float[] {0,0,0};
		private float[] _craft = new float[] {0,0,0};
		private float[] _local = new float[] {0,0,0};
		
        private int maPositionHandle;
        private long mStartTime;
		private Matrix M1;

		private static final String VERTEX_SHADER =
		"attribute vec2 position;" +
		"void main() {" +
		"gl_Position = vec4(position, 0., 1.);" +
		"}";
		private static final String FRAGMENT_SHADER =
		"#ifdef GL_FRAGMENT_PRECISION_HIGH\n" +
		"precision highp float;\n" +
		"#else\n" +
		"precision mediump float;\n" +
		"#endif\n" +
		"uniform vec2 resolution;" +
		"uniform sampler2D frame;" +
		"void main(void) {" +
		//"gl_FragColor = vec4(gl_FragCoord.x/resolution.x, 0., 0., 1.);"+
		"gl_FragColor = texture2D(frame," +
		"gl_FragCoord.xy / resolution.xy).rgba;" +
		"}";

		private static final int RATIO = 4;

		private int program = 0;
		private int surfaceProgram = 0;
		private int surfacePositionLoc;
		private final ByteBuffer vertexBuffer;
		private int surfaceResolutionLoc;
		private int surfaceFrameLoc;
		private final float surfaceResolution[] = new float[]{0, 0};

		private int[] mFrameBuffers;

		private int[] mFrameBufferTextures;

        private ShaderRenderer() {
            float[] rectData = new float[]{
                    -1f, -1f, -1f, 1f,
                    1f, -1f, 1f, 1f,
            };

            mRectData = ByteBuffer.allocateDirect(rectData.length * FLOAT_SIZE_BYTES)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            mRectData.put(rectData).position(0);


			vertexBuffer = ByteBuffer.allocateDirect(8);
			vertexBuffer.put(new byte[]{
								 -1, 1,
								 -1, -1,
								 1, 1,
								 1, -1}).position(0);

        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
			
            mVertexShader = readShader(R.raw.vertex_shader, MainActivity.this);
            mFragmentShader = readShader(R.raw.cubes2, MainActivity.this);
			
            program = createProgram(mVertexShader, mFragmentShader);
			GLES20.glUseProgram(program);
            miGlobalTimeHandle = GLES20.glGetUniformLocation(program, "time");
            miResolutionHandle = GLES20.glGetUniformLocation(program, "resolution");
            miMouseHandle = GLES20.glGetUniformLocation(program, "mouse");
			miPosHandle = GLES20.glGetUniformLocation(program, "pos");
			
            GLES20.glEnableVertexAttribArray(maPositionHandle);

            mRectData.position(0);
            GLES20.glVertexAttribPointer(maPositionHandle, 2, GLES20.GL_FLOAT, false, 0, mRectData);
            mStartTime = SystemClock.elapsedRealtime();
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            mResolution = new float[] {width, height};
            mReducedResolution = new float[] {width / RATIO, height / RATIO};
            GLES20.glViewport(0, 0, width, height);


// create fbop
			mFrameBuffers = new int[1];
			mFrameBufferTextures = new int[1];
			

			GLES20.glGenFramebuffers(1, mFrameBuffers, 0);
			GLES20.glGenTextures(1, mFrameBufferTextures, 0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0]);
			GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
								GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
								   GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
								   GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
								   GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
								   GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
			
			
				GLES20.glTexImage2D(
					GLES20.GL_TEXTURE_2D,
					0,
					GLES20.GL_RGBA,
					(int)mReducedResolution[0],
					(int)mReducedResolution[1],
					0,
					GLES20.GL_RGBA,
					GLES20.GL_UNSIGNED_BYTE,
					null);

			GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
										  GLES20.GL_TEXTURE_2D, mFrameBufferTextures[0], 0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);		

			
			
			mFrameBufferObject = mFrameBuffers[0];

			
			
	
			mTexture = mFrameBufferTextures[0];
            surfaceResolution[0] = width;
            surfaceResolution[1] = height;

            createSurfProgram();
			
            GLES20.glUseProgram(surfaceProgram);
            surfaceResolutionLoc = GLES20.glGetUniformLocation(
                    surfaceProgram, "resolution");
            surfaceFrameLoc = GLES20.glGetUniformLocation(
                    surfaceProgram, "frame");
            surfacePositionLoc = GLES20.glGetAttribLocation(
                    surfaceProgram, "position");
            GLES20.glEnableVertexAttribArray(surfacePositionLoc);
 
 }
 
 		public void update(){
			_craft[0] -= 0.02 * Math.cos(mMouse[0]-.65);
			_craft[1] -= 0.03 * Math.cos(mMouse[1]);
			_craft[2] -= 0.02 * _craft[1];
			
			//mPos[2]-=0.01 * Math.sin(mMouse[0]);
			//mPos[0]+=0.01 * Math.cos(mMouse[0]);
		}

        @Override
        public void onDrawFrame(GL10 gl) {

            // 1st stage
            update();
            GLES20.glUseProgram(program);
            GLES20.glVertexAttribPointer(0, 2, GLES20.GL_BYTE, false, 0, vertexBuffer);

            GLES20.glUniform3fv(miGlobalTimeHandle, 1, _craft, 0);
            GLES20.glUniform2fv(miResolutionHandle, 1, mReducedResolution, 0);
            long nowInSec = SystemClock.elapsedRealtime();
            GLES20.glUniform1f(miGlobalTimeHandle, ((float) (nowInSec - mStartTime)) / 1000f);
			GLES20.glUniform3fv(miPosHandle, 1, mPos, 0);

            GLES20.glViewport(0, 0, (int)mReducedResolution[0], (int)mReducedResolution[1]);
///*
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferObject);
			
			
            GLES20.glDrawArrays(
                    GLES20.GL_TRIANGLE_STRIP,
                    0,
                    4);

            // 2nd stage
////

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTexture);
			
            GLES20.glViewport(0,0, (int)mResolution[0], (int)mResolution[1]);

            GLES20.glUseProgram(surfaceProgram);
            GLES20.glVertexAttribPointer(surfacePositionLoc, 2, GLES20.GL_FLOAT, false, 0, mRectData);
//*/
 /////           // :: uniforms
            GLES20.glUniform2fv(
                    surfaceResolutionLoc,
                    1,
                    mResolution,
                    0);

			GLES20.glUniform1i(surfaceFrameLoc, 0);


            GLES20.glClear(
                    GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glDrawArrays(
                    GLES20.GL_TRIANGLE_STRIP,
                    0,
                    4);
/////
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

		void createSurfProgram()
		{
			int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
            if (vertexShader == 0) {
                return;
            }

            int pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
            if (pixelShader == 0) {
                return;
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
			surfaceProgram = program;
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


		private void checkGlError() {
			int error;
			while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
				Log.e(TAG, ": glError " + error);
			}
		}
		

    }
}

