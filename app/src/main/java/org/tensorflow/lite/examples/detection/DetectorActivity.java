/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.detection;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.tensorflow.lite.examples.detection.customview.OverlayView;
import org.tensorflow.lite.examples.detection.customview.OverlayView.DrawCallback;
import org.tensorflow.lite.examples.detection.env.BorderedText;
import org.tensorflow.lite.examples.detection.env.ImageUtils;
import org.tensorflow.lite.examples.detection.env.Logger;
import org.tensorflow.lite.examples.detection.tflite.Classifier;
import org.tensorflow.lite.examples.detection.tflite.DetectorFactory;
import org.tensorflow.lite.examples.detection.tflite.YoloV5Classifier;
import org.tensorflow.lite.examples.detection.tracking.MultiBoxTracker;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {
    private static final Logger LOGGER = new Logger();
    private static final DetectorMode MODE = DetectorMode.TF_OD_API;
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.3f;
    private static final boolean MAINTAIN_ASPECT = true;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 640);
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    OverlayView trackingOverlay;
    private Integer sensorOrientation;

    private YoloV5Classifier detector;

    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    private boolean computingDetection = false;

    private long timestamp = 0;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private MultiBoxTracker tracker;

    private BorderedText borderedText;

    // TIMER
    private TextView timerText;
    private boolean isPlaying = false;
    public static boolean isLookingDetected = false;
    private boolean isRunning = false;
    private int timerSeconds = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            timerSeconds++;

            updateTimerUI();

            handler.postDelayed(this, 1000);
        }
    };

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private DatabaseReference userNameDatabase;

    Button startButton;
    private boolean isPaused = true;
    private SharedPreferences prefsTimer, prefsUserName;
    private String uid;

    //    Dialog Box
    private Button btnExit;
    private Dialog dialog;
    private Button btnDialogConfirm, btnDialogExit;
    private TextInputEditText editTextUserName;
    private String childNumber;
    private boolean isUsernameSet = false;
    private String savedUserName = "";
    private static final String PREF_USERNAME_KEY = "username";
    private static final String PREF_USERNAME_SET_KEY = "username_set";


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        LOGGER.d("onCreate " + this);
        super.onCreate(null);

        prefsTimer = PreferenceManager.getDefaultSharedPreferences(this);
        prefsUserName = PreferenceManager.getDefaultSharedPreferences(this);

        // Initialize your UI components
        timerText = findViewById(R.id.timerText);
        startButton = findViewById(R.id.start);

        // Firebase DB
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            uid = currentUser.getUid();
            mDatabase = FirebaseDatabase.getInstance().getReference()
                    .child("Registered Users").child(uid).child("Child").child("Child_1").child("ScreenTime");
        }

        isUsernameSet = prefsUserName.getBoolean(PREF_USERNAME_SET_KEY, false);
        if (isUsernameSet) {
            savedUserName = prefsUserName.getString(PREF_USERNAME_KEY, "");
        } else {
            showUsernameDialog();
        }

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPaused = !isPaused;

                Intent intent = new Intent(getApplicationContext(), DetectorService.class);
                startService(intent);
            }
        });


        FloatingActionButton fab = findViewById(R.id.backButtonMonitoringMode);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Choose.class);
                startActivity(intent);
//                finish();
            }
        });



    }

    private void showUsernameDialog() {

        dialog = new Dialog(DetectorActivity.this);
        dialog.setContentView(R.layout.custom_dialog_box);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.custom_dialog_bg));
        dialog.setCancelable(false);

        dialog.show();

        btnDialogExit = dialog.findViewById(R.id.btnDialogExit);
        btnDialogConfirm = dialog.findViewById(R.id.btnDialogConfirm);
        editTextUserName = dialog.findViewById(R.id.userName);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        userNameDatabase = FirebaseDatabase.getInstance().getReference().child("Registered Users").child(uid).child("Child");

        userNameDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int childCount = (int) dataSnapshot.getChildrenCount();

                childNumber = "Child_" + (childCount + 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        btnDialogConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userName = String.valueOf(editTextUserName.getText());

                if (currentUser != null) {
                    // save username on Database
                    uid = currentUser.getUid();
                    userNameDatabase = FirebaseDatabase.getInstance().getReference()
                            .child("Registered Users").child(uid).child("Child").child(childNumber).child("name");
                    userNameDatabase.setValue(userName);

                    // save username Locally
                    SharedPreferences.Editor editor = prefsUserName.edit();
                    editor.putString(PREF_USERNAME_KEY, savedUserName);
                    editor.putBoolean(PREF_USERNAME_SET_KEY, true);
                    editor.apply();
                }

                dialog.dismiss();
            }
        });
        btnDialogExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), Choose.class);
                startActivity(intent);
                dialog.dismiss();
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences.Editor editor = prefsTimer.edit();
        editor.putInt("timerSeconds", timerSeconds);
        editor.apply();
    }


    // Load the timer value from SharedPreferences when the app is resumed
    @Override
    public void onResume() {
        super.onResume();
        // Load the timer value, default is 0 if not found
        timerSeconds = prefsTimer.getInt("timerSeconds", 0);
        updateTimerUI(); // Update UI with loaded timer value
    }

    private void startTimer() {
        if (!isRunning) {
            handler.postDelayed(runnable, 1000);
            isRunning = true;
        }
    }

    private void stopTimer() {
        if (isRunning) {
            handler.removeCallbacks(runnable);
            isRunning = false;
        }
    }

    private void resetTimer() {
        stopTimer();

        timerSeconds = 0;
        timerText.setText("00:00:00");
    }

    private void updateTimerUI() {
        // Update timer value in UI
        // You can customize this method to update the timer display format as needed
        // For example, convert timerSeconds to hours, minutes, and seconds
        int hours = timerSeconds / 3600;
        int minutes = (timerSeconds % 3600) / 60;
        int seconds = timerSeconds % 60;

        String time = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
        timerText.setText(time);

        // Store the timer value in Firebase Realtime Database
        if (mDatabase != null) {
            mDatabase.setValue(time); // Store the formatted time string
        }
    }


    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        final int modelIndex = modelView.getCheckedItemPosition();
        final String modelString = modelStrings.get(modelIndex);

        try {
            detector = DetectorFactory.getDetector(getAssets(), modelString);
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        int cropSize = detector.getInputSize();

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                new DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        tracker.draw(canvas);
                        if (isDebug()) {
                            tracker.drawDebug(canvas);
                        }
                    }
                });

        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    protected void updateActiveModel() {
        // Get UI information before delegating to background
        final int modelIndex = modelView.getCheckedItemPosition();
        final int deviceIndex = deviceView.getCheckedItemPosition();
        String threads = threadsTextView.getText().toString().trim();
        final int numThreads = Integer.parseInt(threads);

        handler.post(() -> {
            if (modelIndex == currentModel && deviceIndex == currentDevice
                    && numThreads == currentNumThreads) {
                return;
            }
            currentModel = modelIndex;
            currentDevice = deviceIndex;
            currentNumThreads = numThreads;

            // Disable classifier while updating
            if (detector != null) {
                detector.close();
                detector = null;
            }

            // Lookup names of parameters.
            String modelString = modelStrings.get(modelIndex);
            String device = deviceStrings.get(deviceIndex);

            LOGGER.i("Changing model to " + modelString + " device " + device);

            // Try to load model.

            try {
                detector = DetectorFactory.getDetector(getAssets(), modelString);
                // Customize the interpreter to the type of device we want to use.
                if (detector == null) {
                    return;
                }
            }
            catch(IOException e) {
                e.printStackTrace();
                LOGGER.e(e, "Exception in updateActiveModel()");
                Toast toast =
                        Toast.makeText(
                                getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
                toast.show();
                finish();
            }


            if (device.equals("CPU")) {
                detector.useCPU();
            } else if (device.equals("GPU")) {
                detector.useGpu();
            } else if (device.equals("NNAPI")) {
                detector.useNNAPI();
            }
            detector.setNumThreads(numThreads);

            int cropSize = detector.getInputSize();
            croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

            frameToCropTransform =
                    ImageUtils.getTransformationMatrix(
                            previewWidth, previewHeight,
                            cropSize, cropSize,
                            sensorOrientation, MAINTAIN_ASPECT);

            cropToFrameTransform = new Matrix();
            frameToCropTransform.invert(cropToFrameTransform);
        });
    }

    @Override
    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }

        // Check if object classification is paused
        if (isPaused) {
            readyForNextImage();
            // Keep running the processing loop even when paused
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    processImage(); // Call processImage() again after a delay
                    startTimer();
                }
            }, 100);
            stopTimer();
            return;
        }


        computingDetection = true;
        LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);



        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        LOGGER.i("Running detection on image " + currTimestamp);
                        final long startTime = SystemClock.uptimeMillis();
                        final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                        Log.e("CHECK", "run: " + results.size());

                        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                        final Canvas canvas = new Canvas(cropCopyBitmap);
                        final Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setStyle(Style.STROKE);
                        paint.setStrokeWidth(2.0f);

                        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                        switch (MODE) {
                            case TF_OD_API:
                                minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                                break;
                        }

                        final List<Classifier.Recognition> mappedRecognitions =
                                new LinkedList<Classifier.Recognition>();

//                        boolean isLookingDetected = false;

                        for (final Classifier.Recognition result : results) {
                            final RectF location = result.getLocation();
                            if (location != null && result.getConfidence() >= minimumConfidence) {
                                canvas.drawRect(location, paint);

                                cropToFrameTransform.mapRect(location);

                                result.setLocation(location);
                                mappedRecognitions.add(result);

                                // Check if "looking" class is detected
                                if ("looking".equals(result.getTitle())) {
                                    isLookingDetected = true;
                                } else {
                                    isLookingDetected = false;
                                }

                                // Print the detecting class name
                                Log.d("DetectedClassName", "Class: " + result.getTitle());
                                // Print the boolean value to Logcat
                                Log.d("isLookingDetected", "Boolean value: " + isLookingDetected);
                            }
                        }

                        tracker.trackResults(mappedRecognitions, currTimestamp);
                        trackingOverlay.postInvalidate();

                        computingDetection = false;

                        runOnUiThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        showFrameInfo(previewWidth + "x" + previewHeight);
                                        showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
                                        showInference(lastProcessingTimeMs + "ms");



                                        // Keep running the processing loop
                                        handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (isLookingDetected && !isRunning) {
                                                    startTimer();
                                                    isRunning = true;
                                                }
                                                else if (!isLookingDetected && isRunning){
                                                    stopTimer();
                                                    isRunning = false;
                                                }

                                                processImage(); // Call processImage() again after processing is done
                                            }
                                        }, 100);

                                    }
                                });
                    }
                });
    }

    @Override
    protected int getLayoutId() {
        return R.layout.tfe_od_camera_connection_fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    // Which detection model to use: by default uses Tensorflow Object Detection API frozen
    // checkpoints.
    private enum DetectorMode {
        TF_OD_API
    }

    @Override
    protected void setUseNNAPI(final boolean isChecked) {
        runInBackground(() -> detector.setUseNNAPI(isChecked));
    }

    @Override
    protected void setNumThreads(final int numThreads) {
        runInBackground(() -> detector.setNumThreads(numThreads));
    }
}
