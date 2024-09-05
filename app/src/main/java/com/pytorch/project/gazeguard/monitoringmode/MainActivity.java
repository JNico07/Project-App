// Copyright (c) 2020 Facebook, Inc. and its affiliates.
// All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package com.pytorch.project.gazeguard.monitoringmode;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.pytorch.project.gazeguard.common.ChooseActivity;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.demo.objectdetection.R;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements Runnable {
    private int mImageIndex = 0;
//    private String[] mTestImages = {"test1.png", "test2.jpg", "test3.png", "test4.jpg"};

//    private ImageView mImageView;
    private ResultView mResultView;
    private Button mButtonDetect;
    private ProgressBar mProgressBar;
    private Bitmap mBitmap = null;
    private Module mModule = null;
    private float mImgScaleX, mImgScaleY, mIvScaleX, mIvScaleY, mStartX, mStartY;

    private boolean containerVisible = false;
    private FrameLayout container;

    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
//        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setContentView(R.layout.activity_main);

//        try {
//            mBitmap = BitmapFactory.decodeStream(getAssets().open(mTestImages[mImageIndex]));
//        } catch (IOException e) {
//            Log.e("Object Detection", "Error reading assets", e);
//            finish();
//        }

//        mImageView = findViewById(R.id.imageView);
//        mImageView.setImageBitmap(mBitmap);
//        mResultView = findViewById(R.id.resultView);
//        mResultView.setVisibility(View.INVISIBLE);

//        final Button buttonTest = findViewById(R.id.testButton);
//        buttonTest.setText(("Test Image 1/3"));
//        buttonTest.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                mResultView.setVisibility(View.INVISIBLE);
//                mImageIndex = (mImageIndex + 1) % mTestImages.length;
//                buttonTest.setText(String.format("Text Image %d/%d", mImageIndex + 1, mTestImages.length));
//
//                try {
//                    mBitmap = BitmapFactory.decodeStream(getAssets().open(mTestImages[mImageIndex]));
//                    mImageView.setImageBitmap(mBitmap);
//                } catch (IOException e) {
//                    Log.e("Object Detection", "Error reading assets", e);
//                    finish();
//                }
//            }
//        });


//        final Button buttonSelect = findViewById(R.id.selectButton);
//        buttonSelect.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                mResultView.setVisibility(View.INVISIBLE);
//
//                final CharSequence[] options = { "Choose from Photos", "Take Picture", "Cancel" };
//                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//                builder.setTitle("New Test Image");
//
//                builder.setItems(options, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int item) {
//                        if (options[item].equals("Take Picture")) {
//                            Intent takePicture = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
//                            startActivityForResult(takePicture, 0);
//                        }
//                        else if (options[item].equals("Choose from Photos")) {
//                            Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
//                            startActivityForResult(pickPhoto , 1);
//                        }
//                        else if (options[item].equals("Cancel")) {
//                            dialog.dismiss();
//                        }
//                    }
//                });
//                builder.show();
//            }
//        });

        final Button buttonLive = findViewById(R.id.startButton);
        buttonLive.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 1);
                } else {
                    startDetectorService();
                }
            }
        });



        try {
            mModule = LiteModuleLoader.load(MainActivity.assetFilePath(getApplicationContext(), "best.torchscript.ptl"));
            BufferedReader br = new BufferedReader(new InputStreamReader(getAssets().open("classes.txt")));
            String line;
            List<String> classes = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                classes.add(line);
            }
            PrePostProcessor.mClasses = new String[classes.size()];
            classes.toArray(PrePostProcessor.mClasses);
        } catch (IOException e) {
            Log.e("Object Detection", "Error reading assets", e);
            finish();
        }
    }


    @Override
    public void run() {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(mBitmap, PrePostProcessor.mInputWidth, PrePostProcessor.mInputHeight, true);
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, PrePostProcessor.NO_MEAN_RGB, PrePostProcessor.NO_STD_RGB);
        IValue[] outputTuple = mModule.forward(IValue.from(inputTensor)).toTuple();
        final Tensor outputTensor = outputTuple[0].toTensor();
        final float[] outputs = outputTensor.getDataAsFloatArray();
        final ArrayList<Result> results =  PrePostProcessor.outputsToNMSPredictions(outputs, mImgScaleX, mImgScaleY, mIvScaleX, mIvScaleY, mStartX, mStartY);

        runOnUiThread(() -> {
            mButtonDetect.setEnabled(true);
            mButtonDetect.setText(getString(R.string.detect));
            mProgressBar.setVisibility(ProgressBar.INVISIBLE);
            mResultView.setResults(results);
            mResultView.invalidate();
            mResultView.setVisibility(View.VISIBLE);
        });
    }

    //
    private boolean doubleBackToExitPressedOnce = false;
    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            Intent intent = new Intent(getApplicationContext(), ChooseActivity.class);
            startActivity(intent);
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000); // 2 seconds delay before resetting
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.stop) {
            Toast.makeText(this, "Measuring Screen Time Stopped", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.back) {
            Toast.makeText(this, "Going back...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(getApplicationContext(), ChooseActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) { // 1 is the request code for camera permission
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted, start the service
                startDetectorService();
            } else {
                // Permission was denied, show a message to the user
                Toast.makeText(this, "Camera permission is required to start the service", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startDetectorService() {
        Intent intentService = new Intent(getApplicationContext(), DetectorService.class);
        intentService.setAction("START_TIMER");
        Toast.makeText(getApplicationContext(), "Measuring Screen Time...", Toast.LENGTH_LONG).show();
        startService(intentService);
    }



}
