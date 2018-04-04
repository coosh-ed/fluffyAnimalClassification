package com.example.fluffyclass;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.fluffyclass.Utilities.CameraUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener{

    private TextureView mTextureView;
    private Button mButton;
    private CameraUtils mCameraUtils;

    private ProgressBar loadingBar;
    private TableLayout table;

    private ProgressBar mCatProgress;
    private ProgressBar mDogProgress;
    private ProgressBar mPenguinProgress;

    private Map<String, ProgressBar> mapProgressBars =
            new HashMap<String, ProgressBar>();

    private final String[] animals = new String[] {"Cat", "Dog", "Penguin"};


    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mTextureView = (TextureView)findViewById(R.id.camera_tv);
        mTextureView.setSurfaceTextureListener(this);

        loadingBar = (ProgressBar) findViewById(R.id.progress_bar);
        table = (TableLayout) findViewById(R.id.table_classes);

        mapProgressBars();

        mButton = (Button) findViewById(R.id.button_capture);
        mButton.setOnClickListener(mOnClickListener);
    }

    private void mapProgressBars() {
        mapProgressBars.put(animals[0],
                (ProgressBar) findViewById(R.id.progressBar2));
        mapProgressBars.put(animals[1],
                (ProgressBar) findViewById(R.id.progressBar3));
        mapProgressBars.put(animals[2],
                (ProgressBar) findViewById(R.id.progressBar4));
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraUtils.destroy();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraUtils = new CameraUtils(mTextureView.getSurfaceTexture(), getApplicationContext(),
                animals, mapProgressBars);
        mCameraUtils.initTensorFlowAndLoadModel();
        mTextureView.setSurfaceTextureListener(this);
    }

    private View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            new CameraOperation().execute();
        }
    };

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mCameraUtils = new CameraUtils(mTextureView.getSurfaceTexture(), getApplicationContext(),
                animals, mapProgressBars);
        mCameraUtils.initTensorFlowAndLoadModel();
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return mCameraUtils.destroy();
    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        return;
    }

    public void onSurfaceTextureUpdated(SurfaceTexture surface){
        return;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraUtils.destroy();

    }

    private class CameraOperation extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... Params) {
            mCameraUtils.execTakePicture();
            return null;
        }

        @Override
        protected void onPostExecute(Void Params) {
            super.onPostExecute(Params);
            loadingBar.setVisibility(View.INVISIBLE);
            table.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            table.setVisibility(View.INVISIBLE);
            loadingBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onProgressUpdate(Void... values) {}
    }


    }
