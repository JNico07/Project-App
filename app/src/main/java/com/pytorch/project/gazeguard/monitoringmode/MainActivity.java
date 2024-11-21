// Copyright (c) 2020 Facebook, Inc. and its affiliates.
// All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package com.pytorch.project.gazeguard.monitoringmode;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.pytorch.project.gazeguard.common.ChooseActivity;
import com.pytorch.project.gazeguard.common.SharedPrefsUtil;

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
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements Runnable {
    private int mImageIndex = 0;
//    private String[] mTestImages = {"test1.png", "test2.jpg", "test3.png", "test4.jpg"};

//    private ImageView mImageView;
    private Button mButtonDetect;
    private ResultView mResultView;
    private ProgressBar mProgressBar;
    private Bitmap mBitmap = null;
    private Module mModule = null;
    private float mImgScaleX, mImgScaleY, mIvScaleX, mIvScaleY, mStartX, mStartY;

    private boolean containerVisible = false;
    private FrameLayout container;

    private SwitchCompat deviceAdminSwitch;
    private ComponentName componentName;
    private DevicePolicyManager devicePolicyManager;
    private static final int RESULT_ENABLE = 123;
    private boolean isAdminOn;
    private FirebaseAuth mAuth;
    private Button buttonLive;
    private ProgressBar buttonProgressBar;
    private boolean isRestart = false;
    private View loadingContainer;


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
        setContentView(R.layout.activity_main);
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
//        }

        mAuth = FirebaseAuth.getInstance();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }


        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        deviceAdminSwitch = findViewById(R.id.admin_switch);
        devicePolicyManager = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        componentName = new ComponentName(this, MyDeviceAdminReceiver.class);

        buttonLive = findViewById(R.id.startButton);
        buttonProgressBar = findViewById(R.id.buttonProgressBar);
        loadingContainer = findViewById(R.id.loadingContainer);
        
        buttonLive.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 1);
                } else {
                    startDetectorService();
                }
            }
        });

        deviceAdminSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "For Lock Screen");
                startActivityForResult(intent, RESULT_ENABLE);
            } else {
                // Prompt for password verification when turning off the admin switch
                View customView = getLayoutInflater().inflate(R.layout.dialog_password_input, null);
                TextInputEditText passwordInput = customView.findViewById(R.id.password_input);

                new MaterialAlertDialogBuilder(this)
                        .setTitle("Turn Off Device Admin")
                        .setMessage("Please Enter your Account Password to turn off device admin")
                        .setView(customView)
                        .setPositiveButton("Confirm", (dialog, which) -> {
                            String password = Objects.requireNonNull(passwordInput.getText()).toString().trim();
                            if (password.isEmpty()) {
                                Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
                                // Re-check the switch to "on" since password was not provided
                                deviceAdminSwitch.setChecked(true);
                            } else {
                                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                                FirebaseUser currentUser = mAuth.getCurrentUser();
                                if (currentUser != null) {
                                    String email = currentUser.getEmail();
                                    if (email != null) {
                                        mAuth.signInWithEmailAndPassword(email, password)
                                                .addOnCompleteListener(task -> {
                                                    if (task.isSuccessful()) {
                                                        // Password is correct, proceed to disable device admin
                                                        devicePolicyManager.removeActiveAdmin(componentName);
                                                        Toast.makeText(this, "Device admin turned off", Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        // Incorrect password; revert the switch to "on"
                                                        deviceAdminSwitch.setChecked(true);
                                                        Toast.makeText(this, "Incorrect password.", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }
                                }
                            }
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            dialog.dismiss();
                            // Re-check the switch to "on" if the dialog is cancelled
                            deviceAdminSwitch.setChecked(true);
                        })
                        .show();
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

        updateLockStatus();
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
        getMenuInflater().inflate(R.menu.monitoring_toolbar_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.stop:
                // Create a custom dialog layout
                View customView = getLayoutInflater().inflate(R.layout.dialog_password_input, null);
                TextInputEditText passwordInput = customView.findViewById(R.id.password_input);

                new MaterialAlertDialogBuilder(this)
                        .setTitle("Stop Measuring Screen Time")
                        .setMessage("Please Enter your Account Password to Stop Measuring Screen Time.")
                        .setView(customView)
                        .setPositiveButton("Confirm", (dialog, which) -> {
                            String password = Objects.requireNonNull(passwordInput.getText()).toString().trim();
                            if (password.isEmpty()) {
                                Toast.makeText(this, "Password cannot be empty", Toast.LENGTH_SHORT).show();
                            } else {
                                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                                FirebaseUser currentUser = mAuth.getCurrentUser();
                                if (currentUser != null) {
                                    String email = currentUser.getEmail();
                                    if (email != null) {
                                        mAuth.signInWithEmailAndPassword(email, password)
                                                .addOnCompleteListener(task -> {
                                                    if (task.isSuccessful()) {
                                                        // Password is correct, proceed to stop the service
                                                        Toast.makeText(this, "Measuring Screen Time Stopped", Toast.LENGTH_SHORT).show();
                                                        Intent detectorService = new Intent(getApplicationContext(), DetectorService.class);
                                                        detectorService.setAction("KILL_SERVICE");
                                                        startService(detectorService);

                                                        buttonLive.setEnabled(true);
                                                        buttonLive.setText("START");
                                                        isRestart = true;  // Set flag when service is stopped
                                                    } else {
                                                        Toast.makeText(this, "Incorrect password.", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }
                                }
                            }
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .show();
                break;

            case R.id.exit:
                Toast.makeText(this, "Exit", Toast.LENGTH_SHORT).show();
                finishAffinity();
                break;

            default:
                break;
        }
        return true;
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
        if (devicePolicyManager.isAdminActive(componentName)) {
            Intent intentService = new Intent(getApplicationContext(), DetectorService.class);
            intentService.setAction("START_TIMER");
            startService(intentService);

            // Make the button unclickable and update the text
            buttonLive.setEnabled(false);
            buttonLive.setText("Measuring Screen Time...");

            loadingScreen();

            Toast.makeText(getApplicationContext(), "Measuring Screen Time...", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(MainActivity.this, "You need to enable device admin..!", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == RESULT_ENABLE) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "You have enabled device admin features", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Problem to enabled device admin features", Toast.LENGTH_SHORT).show();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isAdminOn = devicePolicyManager.isAdminActive(componentName);
        deviceAdminSwitch.setChecked(isAdminOn);

        // Check if the DetectorService is running and update button state
        if (isServiceRunning(DetectorService.class)) {
            Button buttonLive = findViewById(R.id.startButton);
            buttonLive.setEnabled(false);
            buttonLive.setText("Measuring Screen Time...");
        }
    }
    // Helper method to check if a service is running
    private boolean isServiceRunning(Class<?> serviceClass) {
        android.app.ActivityManager manager = (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (android.app.ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void updateLockStatus() {
        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            String childNumber = SharedPrefsUtil.getChildNumber(this);

            DatabaseReference lockStatusRef = FirebaseDatabase.getInstance().getReference()
                    .child("Registered Users")
                    .child(uid)
                    .child("Child")
                    .child(childNumber)
                    .child("isDeviceLocked");

            lockStatusRef.setValue(false);
        }
    }

    private void loadingScreen() {
        // Show loading state
        buttonLive.setVisibility(View.INVISIBLE);
        loadingContainer.setVisibility(View.VISIBLE);

        // Use different delay based on whether it's a restart
        int delayDuration = isRestart ? 30000 : 5000;

        // Delay for specified duration
        new Handler().postDelayed(() -> {
            loadingContainer.setVisibility(View.GONE);
            buttonLive.setVisibility(View.VISIBLE);
            isRestart = false;  // Reset the flag after use
        }, delayDuration);
    }
}
