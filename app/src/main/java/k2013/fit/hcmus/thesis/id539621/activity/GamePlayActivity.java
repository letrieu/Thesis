package k2013.fit.hcmus.thesis.id539621.activity;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.custom.HandlerSingleton;
import com.custom.OnScrollCallback;
import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import k2013.fit.hcmus.thesis.id539621.R;
import k2013.fit.hcmus.thesis.id539621.dialog.DialogGameFailed;
import k2013.fit.hcmus.thesis.id539621.dialog.DialogGamePause;
import k2013.fit.hcmus.thesis.id539621.dialog.DialogGameSuccess;
import k2013.fit.hcmus.thesis.id539621.dialog.DialogHelper;
import k2013.fit.hcmus.thesis.id539621.dialog.DialogPregame;
import k2013.fit.hcmus.thesis.id539621.game_operation.GameOperation;
import k2013.fit.hcmus.thesis.id539621.game_operation.GamePlayParams;
import k2013.fit.hcmus.thesis.id539621.model.GameLevel;
import k2013.fit.hcmus.thesis.id539621.model.Position;
import k2013.fit.hcmus.thesis.id539621.model.Sound;
import k2013.fit.hcmus.thesis.id539621.sensor.OrientationCallback;
import k2013.fit.hcmus.thesis.id539621.sound.BinauralSound;

public class GamePlayActivity extends BaseActivity implements OnScrollCallback, OrientationCallback {
    public static final String IS_CREATED = "IS_CREATED";
    public static final String GAME_MODE = "GAME_MODE";
    public static final String HAS_SHOW_DEMO = "HAS_SHOW_DEMO";
    public static final String HAS_SENSOR = "HAS_SENSOR";
    public static final String IMG_PATH = "IMG_PATH";
    public static final String TARGET_SOUND = "TARGET_SOUND";
    public static final String BACKGROUND_SOUND = "BACKGROUND_SOUND";
    public static final String DISTRACT_SOUND = "DISTRACT_SOUND";
    public static final String TOTAL_TIME = "TOTAL_TIME";

    private GameOperation mGame;
    private GameLevel[] levels;
    private int levelIndex;
    private GameLevel level;
    private int modeGame;
    private boolean hasSensor;
    private boolean hasShowDemo;
    private String[] backgroundImageList = {"background1", "background6", "md100811705851170500", "bergsjostolen"};

    private final float eyeX = 0;
    private final float eyeY = 0;
    private final float eyeZ = 0;
    private final float lookX = 0;
    private final float lookY = 0;
    private final float lookZ = -1.0f;
    private final float upX = 0.0f;
    private final float upY = 1.0f;
    private final float upZ = 0.0f;

    private float[] mViewMatrix = new float[16];
    private float[] mCurrentRotation = new float[16];
    private float[] mCurrentRotationPost = new float[16];
    private float[] mCurrentRotationZ = new float[16];
    private float[] mTempMatrix = new float[16];

    private int mTargetSound;
    Position mTtargetPosition;
    private int mBackgroundSound;
    private Vector<Integer> mDistractSounds;
    private String targetSoundPath;
    private String mImgPath;

    private Sound targetSound;
    private Sound backgroundSound;
    private ArrayList<Sound> distractSound;

    private int totalTime = 0;

    private int mDelX = 0;
    private int mDelY = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_gameplay);

        if (savedInstanceState == null) {
            hasShowDemo = false;

            SharedPreferences sharedPreferences = this.getSharedPreferences("gameSetting", Context.MODE_PRIVATE);
            if (sharedPreferences != null) {
                modeGame = sharedPreferences.getInt("gameMode", GamePlayParams.MODE_TOUCH);
                hasSensor = sharedPreferences.getBoolean("hasSensor", false);

                if (modeGame == GamePlayParams.MODE_TOUCH) {
                    findViewById(R.id.gameplay_btnSwitch).setBackgroundResource(R.drawable.a_gameplay_icon_touch);
                } else {
                    findViewById(R.id.gameplay_btnSwitch).setBackgroundResource(R.drawable.a_gameplay_icon_motion);
                }

                String gameLevelsString = sharedPreferences.getString("gameLevels", "");
                Gson gson = new Gson();
                levels = gson.fromJson(gameLevelsString, GameLevel[].class);
            }
            HandlerSingleton.init(this, null);

            levelIndex = getIntent().getIntExtra("LevelIndex", 0);
            level = levels[levelIndex];

            setupGameParam();
        } else {
            // Read param from savedInstance...
            hasShowDemo = savedInstanceState.getBoolean(GamePlayActivity.HAS_SHOW_DEMO);
            modeGame = savedInstanceState.getInt(GamePlayActivity.GAME_MODE);
            hasSensor = savedInstanceState.getBoolean(GamePlayActivity.HAS_SENSOR);
            totalTime = savedInstanceState.getInt(GamePlayActivity.TOTAL_TIME);

            SharedPreferences sharedPreferences = this.getSharedPreferences("gameSetting", Context.MODE_PRIVATE);
            if (sharedPreferences != null) {
                String gameLevelsString = sharedPreferences.getString("gameLevels", "");
                Gson gson = new Gson();
                levels = gson.fromJson(gameLevelsString, GameLevel[].class);
            }

            levelIndex = getIntent().getIntExtra("LevelIndex", 0);
            level = levels[levelIndex];
            //timeleft
            mImgPath = savedInstanceState.getString(GamePlayActivity.IMG_PATH);
            targetSound = savedInstanceState.getParcelable(GamePlayActivity.TARGET_SOUND);
            backgroundSound = savedInstanceState.getParcelable(GamePlayActivity.BACKGROUND_SOUND);
            distractSound = savedInstanceState.getParcelableArrayList(GamePlayActivity.DISTRACT_SOUND);

            if (mGame == null) {
                GamePlayParams params = new GamePlayParams();
                params.setTime(level.getTime()*1000 - totalTime);
                params.setMode(modeGame);
                params.setTargetSound(targetSound);
                params.setBackgroundSound(backgroundSound);
                params.setDistractSounds(distractSound);
                params.setBackgroundImg(mImgPath);

                mGame = new GameOperation(this, params);
            }

            //Load target sound
            if(targetSound != null){
                mTargetSound = BinauralSound.addSource(targetSound.getSoundPath());
                BinauralSound.setPosition(mTargetSound, targetSound.getPosition() );
                if(targetSound.getType() == Sound.TYPE_REPEAT){
                    BinauralSound.setLoop(mTargetSound, true);
                }
                else {
                    BinauralSound.setLoop(mTargetSound, false);
                }
            }

            //Load background sound
            if(backgroundSound != null){
                mBackgroundSound = BinauralSound.addSource(backgroundSound.getSoundPath());
                BinauralSound.setLoop(mBackgroundSound, true);
            }

            //Load distract sound
            mDistractSounds = new Vector<>();
            if(distractSound != null){
                for (Sound sound: distractSound) {
                    int soundTemp = BinauralSound.addSource(sound.getSoundPath());
                    BinauralSound.setPosition(soundTemp, sound.getPosition());
                    mDistractSounds.add(soundTemp);
                    if(sound.getType() == Sound.TYPE_REPEAT){
                        BinauralSound.setLoop(soundTemp, true);
                    }
                    else {
                        BinauralSound.setLoop(soundTemp, false);
                    }
                }
            }

            BinauralSound.playSound(mTargetSound);
            BinauralSound.playSound(mBackgroundSound);
            for (int distractsound : mDistractSounds) {
                BinauralSound.playSound(distractsound);
            }
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (savedInstanceState != null) {
            boolean isCreated = savedInstanceState.getBoolean(GamePlayActivity.IS_CREATED, true);
            if (isCreated) {
                return;
            }
        }

        pregame();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mGame.isInited()) {
            mGame.initGame();
        }
    }

    @Override
    public void onBackPressed() {
        // do nothing
    }

    protected void onResume() {
        super.onResume();
        mGame.resume(this);
        if (hasShowDemo) {
            BinauralSound.playSound(mTargetSound);
            BinauralSound.playSound(mBackgroundSound);
            for (int distractsound : mDistractSounds) {
                BinauralSound.playSound(distractsound);
            }
        }
    }

    protected void onPause() {
        super.onPause();

        pauseSound();
        mGame.pause(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(GamePlayActivity.IS_CREATED, true);

        outState.putInt(GamePlayActivity.TOTAL_TIME, totalTime);
        outState.putInt(GamePlayActivity.GAME_MODE, modeGame);
        outState.putBoolean(GamePlayActivity.HAS_SENSOR,hasSensor);
        outState.putString(GamePlayActivity.IMG_PATH, mImgPath);
        outState.putParcelable(GamePlayActivity.TARGET_SOUND, targetSound);
        outState.putParcelable(GamePlayActivity.BACKGROUND_SOUND,backgroundSound);
        outState.putParcelableArrayList(GamePlayActivity.DISTRACT_SOUND,distractSound);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGame.destroy();
    }

    float delX = 0.0f, delY = 0.0f;
    @Override
    public void customOnScroll(float velocityX, float velocityY) {

        if(modeGame == GamePlayParams.MODE_TOUCH) {
            delX = delX - ((int) velocityX) / Resources.getSystem().getDisplayMetrics().density * 0.2f;
            delY = delY - ((int) velocityY) / Resources.getSystem().getDisplayMetrics().density * 0.2f;

            mDelX = (int) delX;
            mDelY = (int) delY;
            changeListenerOrientation(-delY, -delX, 0);
        }
    }

    @Override
    public void onOrientationChanged(float[] rotationMatrix) {
        Matrix.setIdentityM(mViewMatrix, 0);
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

        Matrix.setIdentityM(mTempMatrix, 0);
        Matrix.multiplyMM(mTempMatrix, 0, mViewMatrix, 0, rotationMatrix, 0);

        System.arraycopy(mTempMatrix, 0, mViewMatrix, 0, 16);

        mGame.updateLookAt(mViewMatrix[8], -mViewMatrix[9], -mViewMatrix[10]);
        BinauralSound.setListenerOrientation(mViewMatrix[8], -mViewMatrix[9], -mViewMatrix[10],
                mViewMatrix[4], mViewMatrix[5], mViewMatrix[6]);

    }

    private void changeListenerOrientation(double horizontal, double vertical, double z) {
        Matrix.setIdentityM(mViewMatrix, 0);
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);
        Matrix.setIdentityM(mCurrentRotation, 0);
        Matrix.rotateM(mCurrentRotation, 0, (float)horizontal, 1.0f, 0.0f, 0.0f);
        Matrix.setIdentityM(mCurrentRotationPost, 0);
        Matrix.rotateM(mCurrentRotationPost, 0, (float)vertical, 0.0f, 1.0f, 0.0f);

        Matrix.setIdentityM(mCurrentRotationZ, 0);
        Matrix.rotateM(mCurrentRotationZ, 0, (float)z, 0.0f, 0.0f, 1.0f);

        Matrix.setIdentityM(mTempMatrix, 0);
        Matrix.multiplyMM(mTempMatrix, 0, mCurrentRotation, 0, mCurrentRotationPost, 0);

        Matrix.multiplyMM(mCurrentRotation, 0, mTempMatrix, 0, mCurrentRotationZ, 0);

        Matrix.multiplyMM(mTempMatrix, 0, mViewMatrix, 0, mCurrentRotation, 0);
        System.arraycopy(mTempMatrix, 0, mViewMatrix, 0, 16);

        mGame.updateLookAt(mViewMatrix[8], -mViewMatrix[9], -mViewMatrix[10]);
        BinauralSound.setListenerOrientation(mViewMatrix[8], -mViewMatrix[9], -mViewMatrix[10],
                mViewMatrix[4], mViewMatrix[5], mViewMatrix[6]);
    }

    public void getCorrectPos(int result) {
        if (result == GameOperation.GAME_SUCCESS)
            return;

        boolean isFound = false;

        for (int i = 0; i < 360; i+=10) {
            for (int j = -90; j <= 90; j+=10) {
                changeListenerOrientation(-j, -i, 0);

                isFound = mGame.calcResult();
                if (isFound) {
                    mDelX = i;
                    mDelY = j;
                    break;
                }
            }

            if (isFound) {
                break;
            }
        }
    }

    private void setupGameParam(){
        hasShowDemo = false;
        GamePlayParams params = new GamePlayParams();
        params.setTime(level.getTime()*1000);
        params.setMode(modeGame);

        Random r = new Random();
        int randomPos = r.nextInt(backgroundImageList.length);
        mImgPath = "android.resource://k2013.fit.hcmus.thesis.id539621/raw/" + backgroundImageList[randomPos];
        params.setBackgroundImg(mImgPath);

        //Set target sound
        int targetDistance = r.nextInt(5) + 5;
        int targetAlpha = r.nextInt(361);

        mTtargetPosition = new Position(targetDistance * Math.sin(Math.toRadians(targetAlpha)), 0,
                targetDistance * Math.cos(Math.toRadians(targetAlpha)));

        if(level.isHas_horizontal()) {
            float y = r.nextFloat()*2 - 1;
            mTtargetPosition.setY(y);
        }

        List<File> files = getListFiles(new File(Environment.getExternalStorageDirectory() + "/TinnitusRelief/Package1/Target"));

        int targetSoundPosition = r.nextInt(files.size());

        targetSoundPath = files.get(targetSoundPosition).getPath();
        params.setTargetSound(new Sound(files.get(targetSoundPosition).getPath(), 20, Sound.TYPE_REPEAT, mTtargetPosition));

        //SET background sound
        if(level.isHas_background_sound()){
            List<File> backgroundSoundFiles = getListFiles(new File(Environment.getExternalStorageDirectory() + "/TinnitusRelief/Package1/BackgroundSound"));
            int backgroundSoundPosition = r.nextInt(backgroundSoundFiles.size());
            params.setBackgroundSound(new Sound(backgroundSoundFiles.get(backgroundSoundPosition).getPath(), 20, Sound.TYPE_REPEAT, new Position(0,0,0)));
        }

        if(level.getDistract_sound() > 0){
            List<File> distractFiles = getListFiles(new File(Environment.getExternalStorageDirectory() + "/TinnitusRelief/Package1/DistractSound"));
            Collections.shuffle(distractFiles);
            ArrayList<Sound> array = new ArrayList<>();
            for(int i = 0; i < level.getDistract_sound() && i < distractFiles.size(); i++) {
                int distractDistance = r.nextInt(11) + 5;
                int distractAlpha = r.nextInt(361);

                Position distractPos = new Position(distractDistance * Math.sin(Math.toRadians(distractAlpha)), r.nextFloat()*2 - 1,
                        distractDistance * Math.cos(Math.toRadians(distractAlpha)));

                array.add(new Sound(distractFiles.get(i).getPath(),20, Sound.TYPE_REPEAT, distractPos));
            }
            params.setDistractSounds(array);
        }

        if (mGame == null) {
            mGame = new GameOperation(this, params);
        }

        targetSound = params.getTargetSound();
        backgroundSound = params.getBackgroundSound();
        distractSound = params.getDistractSounds();


        //Load target sound
        if(params.getTargetSound() != null){
            mTargetSound = BinauralSound.addSource(params.getTargetSound().getSoundPath());
            BinauralSound.setPosition(mTargetSound, params.getTargetSound().getPosition() );
            if(params.getTargetSound().getType() == Sound.TYPE_REPEAT){
                BinauralSound.setLoop(mTargetSound, true);
            }
            else {
                BinauralSound.setLoop(mTargetSound, false);
            }
        }

        //Load background sound
        if(params.getBackgroundSound() != null){
            mBackgroundSound = BinauralSound.addSource(params.getBackgroundSound().getSoundPath());
            BinauralSound.setVolume(mBackgroundSound, 0.7f);
            BinauralSound.setLoop(mBackgroundSound, true);
        }

        //Load distract sound
        mDistractSounds = new Vector<>();
        if(params.getDistractSounds() != null){
            for (Sound sound: params.getDistractSounds()) {
                int soundTemp = BinauralSound.addSource(sound.getSoundPath());
                BinauralSound.setPosition(soundTemp, sound.getPosition());
                mDistractSounds.add(soundTemp);
                if(sound.getType() == Sound.TYPE_REPEAT){
                    BinauralSound.setLoop(soundTemp, true);
                }
                else {
                    BinauralSound.setLoop(soundTemp, false);
                }
            }
        }
    }

    public void gamePlayOnClick(View v) {
        switch (v.getId()) {
            case R.id.gameplay_btnPause: {
                showCustomDialog(DialogHelper.REQ_CODE_DIALOG_GAME_PAUSE);
                break;
            }

            case R.id.gameplay_btnSelect: {
                mGame.finish(this, false);
                break;
            }

            case R.id.gameplay_btnSwitch: {
                if(hasSensor){
                    if (modeGame == GamePlayParams.MODE_TOUCH) {
                        findViewById(R.id.gameplay_btnSwitch).setBackgroundResource(R.drawable.a_gameplay_icon_motion);
                    } else {
                        findViewById(R.id.gameplay_btnSwitch).setBackgroundResource(R.drawable.a_gameplay_icon_touch);
                    }

                    switchGameMode();
                } else {
                    Toast.makeText(this, getString(R.string.a_gameplay_msg_no_sensor), Toast.LENGTH_SHORT).show();
                }
                break;
            }

            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case DialogHelper.REQ_CODE_DIALOG_GAME_PAUSE: {
                int res = data.getIntExtra(DialogHelper.RES_TITLE, DialogHelper.RES_CODE_CANCEL);

                if (res == DialogHelper.RES_CODE_CANCEL) {
                    // STORE game state here...

                    finish();
                }

                break;
            }

            case DialogHelper.REQ_CODE_DIALOG_GAME_SUCCESS: {
                int res = data.getIntExtra(DialogHelper.RES_TITLE, DialogHelper.RES_CODE_CANCEL);

                if (res == DialogHelper.RES_CODE_CANCEL) {
                    // STORE game state here...

                    finish();
                } else {
                    nextAction();
                }

                break;
            }

            case DialogHelper.REQ_CODE_DIALOG_GAME_FAILED: {
                int res = data.getIntExtra(DialogHelper.RES_TITLE, DialogHelper.RES_CODE_CANCEL);

                if (res == DialogHelper.RES_CODE_CANCEL) {
                    // STORE game state here...

                    finish();
                } else {
                    nextAction();
                }

                break;
            }

            case GameResultActivity.REQ_CODE: {
                int res = data.getIntExtra(GameResultActivity.RES_CODE, GameResultActivity.CODE_CANCEL);

                if (res == GameResultActivity.CODE_CANCEL) {
                    // STORE game state here...

                    finish();
                } else {
                    nextAction();
                }

                break;
            }

            case DialogHelper.REQ_CODE_DIALOG_PREGAME: {
                hasShowDemo = true;
                break;
            }

            default:
                break;
        }
    }

    public void showCustomDialog(int id) {
        switch (id) {
            case DialogHelper.REQ_CODE_DIALOG_GAME_PAUSE: {
                Intent intent = new Intent(this, DialogGamePause.class);
                startActivityForResult(intent, DialogHelper.REQ_CODE_DIALOG_GAME_PAUSE);
                break;
            }

            case DialogHelper.REQ_CODE_DIALOG_GAME_SUCCESS: {
                Intent intent = new Intent(this, DialogGameSuccess.class);
                startActivityForResult(intent, DialogHelper.REQ_CODE_DIALOG_GAME_SUCCESS);
                break;
            }

            case DialogHelper.REQ_CODE_DIALOG_GAME_FAILED: {
                Intent intent = new Intent(this, DialogGameFailed.class);
                startActivityForResult(intent, DialogHelper.REQ_CODE_DIALOG_GAME_FAILED);
                break;
            }

            case DialogHelper.REQ_CODE_DIALOG_PREGAME: {
                Intent intent = new Intent(this, DialogPregame.class);
                intent.putExtra(DialogHelper.REQ_TITLE_DIALOG_PREGAME_SOUND_ID, targetSoundPath);
                startActivityForResult(intent, DialogHelper.REQ_CODE_DIALOG_PREGAME);
                break;
            }

            default:
                break;
        }
    }

    private void updateProgressBar(int progress) {
        ProgressBar pb = (ProgressBar) findViewById(R.id.gameplay_progressbar);
        ObjectAnimator animator = ObjectAnimator.ofInt(pb,"progress", progress);
        animator.setDuration(GameOperation.TIME_TICK);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }

    private void pauseSound() {
        BinauralSound.pauseSound(mTargetSound);
        BinauralSound.pauseSound(mBackgroundSound);
        for (int distractsound: mDistractSounds){
            BinauralSound.pauseSound(distractsound);
        }
    }

    /*************************************** GAMEPLAY FUNCTIONS ***********************************/
    public void timeTick() {
        totalTime += GameOperation.TIME_TICK;
        int progress = totalTime * 100 / (level.getTimeMilis());

        updateProgressBar(progress);
    }

    public void timeFinish() {
        updateProgressBar(100);
        mGame.finish(this, true);
    }

    public void showGameResult() {
        // Retrieve data from GameOperation
        SharedPreferences sp = getPreferences(Context.MODE_PRIVATE);
        int result = sp.getInt(GameOperation.SP_IS_CORRECT, GameOperation.GAME_FAILED);

        pauseSound();
        getCorrectPos(result);

        Intent intent = new Intent(this, GameResultActivity.class);
        intent.putExtra(GameResultActivity.GAME_RES, result);
        intent.putExtra(GameResultActivity.IMG_PATH, mImgPath);
        intent.putExtra(GameResultActivity.SOUND_ID, mTargetSound);
        intent.putExtra(GameResultActivity.POS_X, mDelX);
        intent.putExtra(GameResultActivity.POS_Y, mDelY);
        intent.putExtra(GameResultActivity.TARGET_SOUND, targetSound.getSoundPath());
        intent.putExtra(GameResultActivity.TARGET_POSITION, targetSound.getPosition());
        startActivityForResult(intent, GameResultActivity.REQ_CODE);
    }

    public void nextAction() {
        // Retrieve data from GameOperation
        SharedPreferences sp = getPreferences(Context.MODE_PRIVATE);
        int result = sp.getInt(GameOperation.SP_IS_CORRECT, GameOperation.GAME_FAILED);
        boolean isCorrect = (result == GameOperation.GAME_SUCCESS);
        clearSharedPreference();

        if (isCorrect) {
            nextGame();
        } else {
            replay();
        }
    }

    public void nextGame() {
        BinauralSound.clearAll();
        if(levelIndex + 1 < levels.length) {
            finish();
            Intent i = getIntent();
            i.putExtra("LevelIndex", levelIndex + 1);

            startActivity(i);
        } else {
            finish();
        }
    }

    public void replay() {
        BinauralSound.clearAll();
        finish();
        startActivity(getIntent());
    }

    public void pregame() {
        showCustomDialog(DialogHelper.REQ_CODE_DIALOG_PREGAME);
    }

    public void clearSharedPreference() {
        SharedPreferences.Editor editor = getPreferences(Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.commit();
    }

    private List<File> getListFiles(File parentDir) {
        ArrayList<File> inFiles = new ArrayList<File>();
        File[] files = parentDir.listFiles();

        if(files == null){
            MainActivity.storeData(this);
            files = parentDir.listFiles();
            if(files == null){
                return inFiles;
            }
        }
        for (File file : files) {
            if (file.isDirectory()) {
                inFiles.addAll(getListFiles(file));
            } else {
                if(file.getName().endsWith(".wav")){
                    inFiles.add(file);
                }
            }
        }
        return inFiles;
    }

    private void switchGameMode(){
        modeGame = (modeGame == GamePlayParams.MODE_TOUCH)?GamePlayParams.MODE_SENSOR:GamePlayParams.MODE_TOUCH;
        mGame.switchMode(modeGame);

        SharedPreferences sharedPreferences= this.getSharedPreferences("gameSetting", Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putInt("gameMode", modeGame);
        editor.apply();
    }
}