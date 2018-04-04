package com.example.fluffyclass.Utilities;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorSpace;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Trace;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.fluffyclass.Classifier;
import com.example.fluffyclass.MainActivity;
import com.example.fluffyclass.TensorFlowImageClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static android.content.ContentValues.TAG;
import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

/**
 * Created by colmo on 01/04/2018.
 */
/* TODO how does this data respresent itself? */
/* TODO get the model on to the phone */
public class CameraUtils extends AppCompatActivity {

    private Camera mCamera;
    private static final String TAG = "CameraUtils.java";
    private static final int RBG_FORMAT = ImageFormat.RGB_565;
    private static final int IMG_SIZE = 32;


    private Classifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();


    private static final int INPUT_SIZE = 32;
    private static final int NUM_CHANNELS = 3;
    private static final String INPUT_NAME = "conv2d_38_input";
    private static final String OUTPUT_NAME = "activation_64/Softmax";

    private static final String MODEL_FILE = "file:///android_asset/frozen_fluffy_imagesize_32.pb";
    private static final String LABEL_FILE =
            "file:///android_asset/labels.txt";

    private final Context mContext;
    private final Map<String, Integer> map = new HashMap<String, Integer>();
    private final String[] mLabels;

    private Map<String, ProgressBar> mapProgressBars;


    public void execTakePicture(){
        Camera.PictureCallback mPicture;
        mPicture = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                Log.i(TAG, "Data format" + data.length);
                pushDataToClassifier(data);
                updateProgressBars();
                Log.i(TAG, "Finished push");

            }
        };

        mCamera.takePicture(null, null, mPicture);

        Log.i(TAG, "CLASSES" + map.keySet().toString());

    }

    public CameraUtils(SurfaceTexture surface, Context context, String[] labels,
                        Map<String, ProgressBar> pBars) {
        if (mCamera == null) {
            mCamera = Camera.open();
        }
        Camera.Parameters mParameters = mCamera.getParameters();
        mCamera.setParameters(mParameters);
        try {
            mCamera.setPreviewTexture(surface);
            mCamera.setDisplayOrientation(90);
            mCamera.startPreview();
        } catch (IOException ioe) {
            Log.e("Check permissions!", "Camera failed");
            ioe.printStackTrace();
            // Something bad happened
        }
        mContext = context;

        mLabels = labels;

        mapProgressBars = pBars;
    }

    private float[][] toFloatArray(int[][] arr) {
        if (arr == null) return null;
        int n = arr.length;
        float[][] ret = new float[n][n];
        for (int j = 0; j<n; j++) {
            for (int i = 0; i < n; i++) {
                ret[i][j] = (float) arr[i][j];
            }
        }
        return ret;
    }

    private void pushDataToClassifier(byte[] data) {

        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
        bitmap = Bitmap.createScaledBitmap(bitmap, IMG_SIZE, IMG_SIZE, false);

        int[] pix = new int[IMG_SIZE * IMG_SIZE];
        bitmap.getPixels(pix, 0, IMG_SIZE, 0, 0, IMG_SIZE, IMG_SIZE);

        float[] floatValues = new float[IMG_SIZE*IMG_SIZE*NUM_CHANNELS];
        for (int i = 0; i < pix.length; ++i) {
            final int val = pix[i];
            floatValues[i * 3 + 0] = (float) (((val >> 16) & 0xFF) * 1.0 / 255);
            floatValues[i * 3 + 1] = (float) (((val >> 8) & 0xFF) * 1.0 / 255);
            floatValues[i * 3 + 2] = (float) (((val & 0xFF) * 1.0) / 255);
        }

        resetMap();

        List<Classifier.Recognition> classifications = classifier.recognizeImage(floatValues);
        String key;
        float val;
        for (Classifier.Recognition classed: classifications){

            key = toTitleCase(classed.getTitle());
            val = classed.getConfidence() * 100;
            Log.i("CLASSES", classed.toString() + " " + val);
            map.put(key, (int) val);
        }

    Log.i(TAG, "Finished pushing data to dict");
    }

    public void initTensorFlowAndLoadModel() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = TensorFlowImageClassifier.create(
                            mContext.getAssets(),
                            MODEL_FILE,
                            LABEL_FILE,
                            INPUT_SIZE,
                            INPUT_NAME,
                            OUTPUT_NAME);
                    Log.d(TAG, "Load Success");
                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }

    public boolean destroy() {
        if (mCamera != null) {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.lock();
                mCamera.release();
                mCamera=null;

            }
        }
        String state = ((mCamera == null) ? "is null": "not null");
        Log.i(TAG, state);
        executor.execute(new Runnable() {
            @Override
            public void run() {
                classifier.close();
            }
        });
        return true;
    }


    private void updateProgressBars() {
        int progress;
        for (String key: mLabels) {
            progress = map.get(key);
            Log.i("VALUE", String.valueOf(progress));
            mapProgressBars.get(key).setProgress(progress);
        }
    }

    private void resetMap(){
        for (String lab: mLabels){
            map.put(lab, 0);
        }
    }

    public static String toTitleCase(String input) {
        StringBuilder titleCase = new StringBuilder();
        boolean nextTitleCase = true;

        for (char c : input.toCharArray()) {
            if (Character.isSpaceChar(c)) {
                nextTitleCase = true;
            } else if (nextTitleCase) {
                c = Character.toTitleCase(c);
                nextTitleCase = false;
            }

            titleCase.append(c);
        }

        return titleCase.toString();
    }

}
