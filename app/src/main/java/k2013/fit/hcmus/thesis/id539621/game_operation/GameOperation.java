package k2013.fit.hcmus.thesis.id539621.game_operation;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.opengl.Matrix;
import android.os.CountDownTimer;
import android.os.Debug;
import android.util.Log;

import com.asha.vrlib.MDVRLibrary;
import com.asha.vrlib.texture.MD360BitmapTexture;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.lang.ref.WeakReference;

import k2013.fit.hcmus.thesis.id539621.R;
import k2013.fit.hcmus.thesis.id539621.activities.GamePlayActivity;
import k2013.fit.hcmus.thesis.id539621.model.Position;
import k2013.fit.hcmus.thesis.id539621.sensor.OrientationListener;

import static com.squareup.picasso.MemoryPolicy.NO_CACHE;
import static com.squareup.picasso.MemoryPolicy.NO_STORE;

/**
 * Created by cpu60011-local on 11/04/2017.
 */

public class GameOperation {
    private final int TIME_TICK = 5000;

    public static final String SP_IS_CORRECT = "IS_CORRECT";

    private GamePlayParams mParams;
    private WeakReference<GamePlayActivity> mWeakReference;
    private Target mTarget;
    private MDVRLibrary mVRLibrary;
    private OrientationListener mOrientationListener;
    private int mMode;

    private CountDownTimer mTimer;
    private long mRemainingTime = 0;
    private Position mCurLookAt = new Position(0,0,0);

    private boolean mIsInited = false;

    private double mDeltaX = 0f;
    private double mDeltaY = 0f;

    private float[] mViewMatrix = new float[16];
    private float[] mCurrentRotation = new float[16];
    private float[] mCurrentRotationPost = new float[16];
    private float[] mTempMatrix = new float[16];
    private final float eyeX = 0;
    private final float eyeY = 0;
    private final float eyeZ = 0;
    private final float lookX = 0;
    private final float lookY = 0;
    private final float lookZ = -1.0f;
    private final float upX = 0.0f;
    private final float upY = 1.0f;
    private final float upZ = 0.0f;

    public GameOperation() {}

    public GameOperation(GamePlayActivity activity, GamePlayParams params) {
        mWeakReference = new WeakReference<GamePlayActivity>(activity);
        mParams = params;
        mRemainingTime = mParams.getTime();

        if (params.getMode() == GamePlayParams.MODE_SENSOR) {
            mMode = MDVRLibrary.INTERACTIVE_MODE_MOTION;
        } else {
            mMode = MDVRLibrary.INTERACTIVE_MODE_TOUCH;
        }

        mOrientationListener = new OrientationListener(activity);
        mOrientationListener.callback = activity;
    }

    /************************************* GAME STATE FUNCTIONS ***********************************/
    public void init() {
        mVRLibrary = createVRLibrary();
        mIsInited = true;
    }

    public void pause(Context context) {
        mVRLibrary.onPause(context);
        mTimer.cancel();

        if (mMode == MDVRLibrary.INTERACTIVE_MODE_MOTION) {
            if(mOrientationListener != null) {
                mOrientationListener.unregisterListener();
            }
        }
    }

    public void resume(Context context) {
        mVRLibrary.onResume(context);

        if (mMode == MDVRLibrary.INTERACTIVE_MODE_MOTION) {
            if(mOrientationListener != null){
                mOrientationListener.registerListener();
            }
        }

        mTimer = new CountDownTimer(mRemainingTime, TIME_TICK) {
            @Override
            public void onTick(long millisUntilFinished) {
                mRemainingTime = millisUntilFinished;
                getActivity().timeTick();
            }

            @Override
            public void onFinish() {
                getActivity().timeFinish();
            }
        }.start();
    }

    public void stop(Context context) {
        mVRLibrary.onPause(context);
    }

    public void finish(Context context, boolean isTimeUp) {
        mTimer.cancel();

        boolean isCorrect = false;

        if (!isTimeUp) {
            // calculate result
            isCorrect = calcResult();
        }

        scrollToPosition(0,0);
        stop(context);

        storeGameResult(isCorrect);

        GamePlayActivity activity = getActivity();
        if (!activity.isDestroyed() && !activity.isFinishing()) {
            activity.showPopUp(true);
        }
    }

    public void destroy() {
        mVRLibrary.onDestroy();
    }

    /************************************ SUPPORT FUNCTIONS ***************************************/
    public MDVRLibrary createVRLibrary() {
        return MDVRLibrary.with(getActivity())
                .displayMode(MDVRLibrary.DISPLAY_MODE_NORMAL)
                .interactiveMode(mMode)
                .asBitmap(new MDVRLibrary.IBitmapProvider() {
                    @Override
                    public void onProvideBitmap(final MD360BitmapTexture.Callback callback) {
                        loadImage(getUri(), callback);
                    }
                }).build(R.id.gameplay_glview);
    }

    private MDVRLibrary getVRLibrary() {
        return mVRLibrary;
    }

    private void loadImage(Uri uri, final MD360BitmapTexture.Callback callback) {
        StackTraceElement[] st = Thread.currentThread().getStackTrace();

        mTarget = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                // notify if size changed
                getVRLibrary().onTextureResize(bitmap.getWidth(), bitmap.getHeight());

                // texture
                callback.texture(bitmap);
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {

            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }
        };
        Picasso.with(getActivity().getApplicationContext()).load(uri).resize(3072, 2048)
                .centerInside().memoryPolicy(NO_CACHE, NO_STORE).into(mTarget);
    }

    private Uri getUri() {
        try {
            Uri res = Uri.parse("android.resource://k2013.fit.hcmus.thesis.id539621/drawable/bergsjostolen");
            return res;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public GamePlayActivity getActivity() {
        return mWeakReference.get();
    }


    public void updateLookAt(double x, double y, double z) {
        mCurLookAt.setX(x);
        mCurLookAt.setY(y);
        mCurLookAt.setZ(z);
    }

    public boolean isInited() {
        return mIsInited;
    }

    private boolean calcResult() {

        double a1 = 0;
        double a2 = 0;
        double a3 = 10;
        double b1 = mCurLookAt.getX();
        double b2 = mCurLookAt.getY();
        double b3 = mCurLookAt.getZ();

        Log.d("calcResult",String.format("a1: %f, a2: %f, a3: %f, b1: %f, b2: %f, b3: %f", a1,a2,a3,b1,b2,b3));

        double alpha = Math.acos((a1*b1 + a2*b2 + a3*b3)/(Math.sqrt(a1*a1 + a2*a2 + a3*a3)*Math.sqrt(b1*b1 + b2*b2 + b3*b3)));

        Log.d("calcResult",String.format("alpha: %f", alpha));


        if(Math.toDegrees(alpha) >= -20 && Math.toDegrees(alpha) <= 20) {
            Log.d("calResult", "true");
            return true;
        }
        Log.d("calResult", "false");
        return false;
    }

    private void storeGameResult(boolean isCorrect) {
        GamePlayActivity activity = getActivity();
        if (!activity.isDestroyed() && !activity.isFinishing()) {
            SharedPreferences.Editor editor = activity.getPreferences(Context.MODE_PRIVATE).edit();

            editor.putBoolean(SP_IS_CORRECT, isCorrect);
            editor.commit();

        }
    }

    public void scrollToPosition(double delX, double delY) {
        mVRLibrary.testScroll(delX, delY);
    }
}
