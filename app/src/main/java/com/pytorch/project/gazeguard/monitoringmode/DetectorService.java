package com.pytorch.project.gazeguard.monitoringmode;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.pytorch.project.gazeguard.common.SharedPrefsUtil;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.demo.objectdetection.R;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DetectorService extends Service implements LifecycleOwner{

    private Module mModule = null;
    private ResultView mResultView;
    protected HandlerThread mBackgroundThread;
    protected Handler mBackgroundHandler;
    private long mLastAnalysisResultTime;
    private String detectedClassName;

    private Context context;
    private final  int NOTIFICATION_ID = 1;
    private final String  CHANNEL_ID = "100";
    private boolean isDestroyed = false;
    private LifecycleRegistry mLifecycleRegistry;

    private SharedPreferences prefsTimer;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference mDatabase, screenTimeLimitRef ;
    private FirebaseFirestore firestore;

    private String uid;
    private static final String TIMER_KEY = "timer_seconds";
    private static final String IS_RUNNING_KEY = "is_running";

    private int screenTimeLimitInSeconds;

    public DetectorService() {
    }


    private final Handler handler = new Handler(Looper.getMainLooper());
    public static boolean isRunning = false;
    private boolean isPause = false;
    private int timerSeconds = 0;
    final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            timerSeconds++;
            updateTimer();
            handler.postDelayed(this, 1000);
        }
    };

    private void updateTimer() {
        int hours = timerSeconds / 3600;
        int minutes = (timerSeconds % 3600) / 60;
        int seconds = timerSeconds % 60;

        String time = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
        updateNotification(time);

        // Store the timer value in Firebase Realtime Database
        if (mDatabase != null) {
            mDatabase.setValue(time); // Store the formatted time
        }

    }

    @Override
    public void onCreate() {
        super.onCreate();

        prefsTimer = PreferenceManager.getDefaultSharedPreferences(this);

        // Firebase DB
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        firestore = FirebaseFirestore.getInstance();

        String childNumber = SharedPrefsUtil.getChildNumber(this);

        // Initialize Firebase Realtime Database reference
        if (currentUser != null) {
            uid = currentUser.getUid();
            // reference for "ScreenTime"
            mDatabase = FirebaseDatabase.getInstance().getReference()
                    .child("Registered Users").child(uid).child("Child").child(childNumber).child("ScreenTime");
            // reference for "screenTimeLimit"
            screenTimeLimitRef = FirebaseDatabase.getInstance().getReference()
                    .child("Registered Users").child(uid).child("Child").child(childNumber).child("screenTimeLimit");
        }

        context = this;

        try {
            if (mModule == null) {
                mModule = LiteModuleLoader.load(MainActivity.assetFilePath(getApplicationContext(), "best.torchscript.ptl"));
            }
        } catch (IOException e) {
            Log.e("Object Detection", "Error reading assets", e);
        }

        // Load the saved timer value from SharedPreferences
        timerSeconds = prefsTimer.getInt(TIMER_KEY, 0);
        isRunning = prefsTimer.getBoolean(IS_RUNNING_KEY, false);

        if (isRunning) {
            startTimer(); // Resume the timer if it was running
        } else {
            pauseTimer(); // Pause the timer
        }

        // Initialize the LifecycleRegistry
        mLifecycleRegistry = new LifecycleRegistry(this);
        mLifecycleRegistry.setCurrentState(Lifecycle.State.CREATED);
        startForeground(NOTIFICATION_ID, showNotification("00:00:00"));

        startBackgroundThread();

        handler.postDelayed(() -> {
            // Move the Lifecycle state to STARTED
            mLifecycleRegistry.setCurrentState(Lifecycle.State.STARTED);
            setupCameraX();
        }, 5000); // Delay before setting up CameraX


        fetchScreenTimeLimit();

        // Schedule the timer reset at 12 AM
//        scheduleTimerReset();

    }

    //    private int retryCount = 0;
//    private static final int MAX_RETRIES = 3;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent != null) {
            String action = intent.getAction();
            if ("STOP_CAMERA".equals(action)) {
                stopCameraX();
                pauseTimer();
            } else if ("START_CAMERA".equals(action)) {
                setupCameraX();
            }
            if ("START_TIMER".equals(action)) {
                handler.postDelayed(this::setupCameraX, 5000); // delay
            } else if ("PAUSE_TIMER".equals(action)) {
                pauseTimer();
            }
        }
        return START_STICKY;
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Bitmap imgToBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    @WorkerThread
    @Nullable
    private EyeTrackerActivity.AnalysisResult analyzeImage(ImageProxy image, int rotationDegrees) {

        Bitmap bitmap = imgToBitmap(image.getImage());
        Matrix matrix = new Matrix();
        matrix.postRotate(270.0f);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, PrePostProcessor.mInputWidth, PrePostProcessor.mInputHeight, true);

        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, PrePostProcessor.NO_MEAN_RGB, PrePostProcessor.NO_STD_RGB);
        IValue[] outputTuple = mModule.forward(IValue.from(inputTensor)).toTuple();
        final Tensor outputTensor = outputTuple[0].toTensor();
        final float[] outputs = outputTensor.getDataAsFloatArray();

        float imgScaleX = (float)bitmap.getWidth() / PrePostProcessor.mInputWidth;
        float imgScaleY = (float)bitmap.getHeight() / PrePostProcessor.mInputHeight;

        float ivScaleX = 1.0f;
        float ivScaleY = 1.0f;
        if (mResultView != null) {
            ivScaleX = (float) mResultView.getWidth() / bitmap.getWidth();
            ivScaleY = (float) mResultView.getHeight() / bitmap.getHeight();
        }

        final ArrayList<Result> results = PrePostProcessor.outputsToNMSPredictions(outputs, imgScaleX, imgScaleY, ivScaleX, ivScaleY, 0, 0);


//        // Log detected classes and scores
//        for (Result result : results) {
//            detectedClassName = PrePostProcessor.mClasses[result.classIndex];
//
////            Log.d("Object Detection", "Detected class: " + PrePostProcessor.mClasses[result.classIndex] + ", Score: " + result.score);
////            Log.d("eyeTracker", "Detected class: " + detectedClassName + " : " + isLooking());
//
//            eyeTimeTracker();
//        }

        // Process detected results
        detectedClassName = results.isEmpty() ? "unknown" : PrePostProcessor.mClasses[results.get(0).classIndex];

        // Call eyeTimeTracker to check the eye status and update the timer accordingly
        eyeTimeTracker();

        return new EyeTrackerActivity.AnalysisResult(results);

    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraXBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void setupCameraX() {
        try {
            final ImageAnalysisConfig imageAnalysisConfig = new ImageAnalysisConfig.Builder()
                    .setLensFacing(CameraX.LensFacing.FRONT)
                    .setTargetResolution(new Size(480, 640))
                    .setCallbackHandler(mBackgroundHandler)
                    .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                    .build();

            final ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);
            imageAnalysis.setAnalyzer((image, rotationDegrees) -> {
                if (SystemClock.elapsedRealtime() - mLastAnalysisResultTime < 100) {
                    return;
                }

                final EyeTrackerActivity.AnalysisResult result = analyzeImage(image, rotationDegrees);
                if (result != null) {
                    mLastAnalysisResultTime = SystemClock.elapsedRealtime();
                }
            });

            CameraX.bindToLifecycle((LifecycleOwner) this, imageAnalysis);
        } catch (Exception e) {
            Log.e("CameraX Setup", "Error setting up CameraX", e);
            handler.postDelayed(this::setupCameraX, 10000); // Retry in 10 seconds
        }
    }
    private void stopCameraX() {
        try {
            CameraX.unbindAll();
            Log.d("CameraX", "CameraX stopped successfully.");
        } catch (Exception e) {
            Log.e("CameraX", "Error stopping CameraX", e);
        }
    }



    private void startTimer() {
        if (!isRunning) {
            isRunning = true;
            isPause = true;
            handler.postDelayed(runnable, 1000);
        }
        else {
            isPause = false;
        }
        saveTimerState();
        updateTimer(); // Ensure the timer is updated immediately
    }
    private void pauseTimer() {
        if (isRunning && isPause) {
            handler.removeCallbacks(runnable);
            isRunning = false;
            saveTimerState();
        }
    }

    private void saveTimerState() {
        SharedPreferences.Editor editor = prefsTimer.edit();
        editor.putInt(TIMER_KEY, timerSeconds);
        editor.putBoolean(IS_RUNNING_KEY, isRunning);
        editor.apply();
    }

    private void updateNotification(String timer) {
        Notification notification = showNotification(timer);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private Notification showNotification(String timer) {
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(
                new NotificationChannel(CHANNEL_ID, "Foreground Notification",
                        NotificationManager.IMPORTANCE_HIGH)
        );

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Screen Time")
                .setContentText(timer)
//                .setSubText(className)
                .setSmallIcon(R.drawable.notification_icon)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isDestroyed = true;
        Toast.makeText(context, "Stopping Service...", Toast.LENGTH_SHORT).show();

        // Set the Lifecycle state to DESTROYED
        mLifecycleRegistry.setCurrentState(Lifecycle.State.DESTROYED);
    }

    private void detector() {
        handler.postDelayed(runnable, 1000);
    }


    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }




    private void eyeTimeTracker() {
        Log.d("eyeTracker", "Detected class: " + detectedClassName + " : " + isLooking());

        if (isLooking() && !isRunning) {
            startTimer();
            isRunning = true;
        }
        else if (!isLooking() && isRunning){
            pauseTimer();
            isRunning = false;
        }

        if (screenTimeLimitInSeconds > 0) {
            checkScreenTimeLimit();
        }

    }

    private boolean isLooking() {
        switch (detectedClassName) {
            case "":
                return false;
            case "unknown":
                return false;
            case "not_looking":
                return false;
            case "closed":
                return false;
            case "looking":
                return true;
            default:
                return false;
        }
    }

    private void scheduleTimerReset() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 16);
        calendar.set(Calendar.MINUTE, 46);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long initialDelay = calendar.getTimeInMillis() - System.currentTimeMillis();
        if (initialDelay < 0) {
            initialDelay += 24 * 60 * 60 * 1000; // Add 24 hours if the time has already passed
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                resetTimer();
                handler.postDelayed(this, 24 * 60 * 60 * 1000); // Schedule next reset in 24 hours
            }
        }, initialDelay);
    }

    private void resetTimer() {
        // Store the current screen time in Firestore
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        Map<String, Object> screenTimeData = new HashMap<>();
        screenTimeData.put("date", currentDate);
        screenTimeData.put("screenTime", timerSeconds);


        String childUserName = SharedPrefsUtil.getUserName(context);
        if (currentUser != null) {
            String uid = currentUser.getUid();

            // Get the unique ID for the document
            String uniqueId = firestore.collection("ScreenTimeRecords")
                    .document(uid)              // Replace uid with the actual user's UID
                    .collection(childUserName)       // Replace childName with the actual child's name
                    .document()                  // Auto-generates a unique ID
                    .getId();

            firestore.collection("ScreenTimeRecords")
                    .document(uid)
                    .collection(childUserName)
                    .document(uniqueId)
                    .set(screenTimeData);
//                    .addOnSuccessListener(aVoid -> Log.d("Firestore", "Screen time data successfully written!"))
//                    .addOnFailureListener(e -> Log.w("Firestore", "Error writing screen time data", e));
        }

        // Reset the timer
        timerSeconds = 0;
        saveTimerState();
        updateNotification("00:00:00");
    }

    private void fetchScreenTimeLimit() {
        if (screenTimeLimitRef != null) {
            screenTimeLimitRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // Fetch the new screen time limit value from Firebase
                        Integer screenTimeLimit = snapshot.getValue(Integer.class);
                        if (screenTimeLimit != null) {
//                            screenTimeLimitInSeconds = screenTimeLimit * 3600;
                            screenTimeLimitInSeconds = 5;
                        } else {
                            screenTimeLimitInSeconds = Integer.MAX_VALUE; // set a default value
                        }
                        Log.d("Firebase", "Screen time limit: " + screenTimeLimitInSeconds);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("Firebase", "Failed to read screen time limit", error.toException());
                }
            });
        }
    }


    Intent intentLockService;
    private void checkScreenTimeLimit() {
        if (timerSeconds >= screenTimeLimitInSeconds) {
            Log.d("Screen time limit", "Time Limit Exceeds" + screenTimeLimitInSeconds + " seconds");
            pauseTimer();
            resetTimer();

            intentLockService = new Intent(this, LockService.class);
            startForegroundService(intentLockService);
        } else {
            Log.d("Screen time limit", "Time Limit NOT yet Exceeds " + screenTimeLimitInSeconds + " seconds");
            if (intentLockService != null) {
                stopService(intentLockService);
            }
        }
    }


}