package com.pytorch.project.gazeguard.monitoringmode;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageEvents;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
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
import androidx.annotation.OptIn;
import androidx.annotation.WorkerThread;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.core.Preview;
import androidx.camera.core.ImageAnalysis;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import android.app.usage.UsageStatsManager;


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
    private DatabaseReference mDatabase, screenTimeLimitRef, isUnlockDeviceNowRef;
    private FirebaseFirestore firestore;

    private String uid;
    private static final String TIMER_KEY = "timer_seconds";
    private static final String IS_RUNNING_KEY = "is_running";

    private int screenTimeLimitInSeconds;

    private ScheduledExecutorService executorService;

    // Adjusted constants for optimization
    private static final int FRAME_SKIP_COUNT = 300; // Skip more frames to reduce CPU usage
    private static final long MIN_ANALYSIS_INTERVAL = 3000; // # seconds between analyses
    private static final int TARGET_ANALYSIS_WIDTH = 400; // Smaller resolution
    private static final int TARGET_ANALYSIS_HEIGHT = 400;

    private int frameCounter = 0;
    private long lastFpsTimestamp = 0;
    private static final int FPS_CALC_INTERVAL = 1000; // Calculate FPS every second

    private BroadcastReceiver screenOffReceiver;

    private Map<String, Long> appUsageMap = new HashMap<>();
    private String currentForegroundApp = null;
    private long lastAppTrackTime = 0;

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
            // reference for "isUnlockDeviceNow"
            isUnlockDeviceNowRef = FirebaseDatabase.getInstance().getReference()
                    .child("Registered Users").child(uid).child("Child").child(childNumber).child("isUnlockDeviceNow");
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

        // Add listener for unlock trigger
        isUnlockDeviceNowRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && Boolean.TRUE.equals(snapshot.getValue(Boolean.class))) {
                    // Stop LockService when unlock is triggered
                    stopLockService();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Firebase", "Failed to read unlock status", error.toException());
            }
        });

        updateLockStatus();

        // Register screen off receiver
        registerScreenOffReceiver();

        initUsageTracking();
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
            switch (Objects.requireNonNull(action)) {
                case "KILL_SERVICE":
                    stopSelf();
                    break;
                case "START_TIMER":
                    setupCameraX();
                    break;
                case "STOP_TIMER":
                    stopTimer();
                    break;
                case "STOP_CAMERA":
                    stopCameraX();
                    pauseTimer();
                    stopTimer();
                    break;
                case "START_CAMERA":
                    // Stop LockService when DetectorService starts
                    stopLockService();
                    updateNotification("00:00:00");
                    setupCameraX();
                    break;
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
        try {
            out.close(); // Close the stream to free resources
        } catch (IOException e) {
            Log.e("DetectorService", "Error closing ByteArrayOutputStream", e);
        }
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    @NonNull
    @OptIn(markerClass = ExperimentalGetImage.class)
    @WorkerThread
    private EyeTrackerActivity.AnalysisResult analyzeImage(ImageProxy image, int rotationDegrees) {
        Bitmap bitmap = imgToBitmap(Objects.requireNonNull(image.getImage()));

        // Directly scale to target size in one step
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(
                bitmap,
                PrePostProcessor.mInputWidth,
                PrePostProcessor.mInputHeight,
                false  // bilinear filtering instead of bicubic
        );

        // Recycle the original bitmap only after ensuring resizedBitmap is created
        if (!bitmap.isRecycled()) {
            bitmap.recycle();
        }

        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, PrePostProcessor.NO_MEAN_RGB, PrePostProcessor.NO_STD_RGB);
        IValue[] outputTuple = mModule.forward(IValue.from(inputTensor)).toTuple();
        final Tensor outputTensor = outputTuple[0].toTensor();
        final float[] outputs = outputTensor.getDataAsFloatArray();

        float imgScaleX = (float)resizedBitmap.getWidth() / PrePostProcessor.mInputWidth;
        float imgScaleY = (float)resizedBitmap.getHeight() / PrePostProcessor.mInputHeight;

        // Recycle the resized bitmap after use
        if (!resizedBitmap.isRecycled()) {
            resizedBitmap.recycle();
        }

        final ArrayList<Result> results = PrePostProcessor.outputsToNMSPredictions(outputs, imgScaleX, imgScaleY, 1.0f, 1.0f, 0, 0);

        // Process detected results
        detectedClassName = results.isEmpty() ? "unknown" : PrePostProcessor.mClasses[results.get(0).classIndex];

        // Call eyeTimeTracker to check the eye status and update the timer accordingly
        eyeTimeTracker();

        trackAppUsage();

        return new EyeTrackerActivity.AnalysisResult(results);
    }

    private void startBackgroundThread() {

        executorService = Executors.newSingleThreadScheduledExecutor();
    }

    private void setupCameraX() {
        try {
            ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
            cameraProviderFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                    final CameraSelector cameraSelector = new CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                            .build();

                    final ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                            .setResolutionSelector(new ResolutionSelector.Builder()
                                    .setResolutionStrategy(new ResolutionStrategy(
                                            new Size(TARGET_ANALYSIS_WIDTH, TARGET_ANALYSIS_HEIGHT),
                                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER))
                                    .build())
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build();

                    imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), new ImageAnalysis.Analyzer() {
                        private long lastFrameTimestamp = 0;
                        private static final long FRAME_INTERVAL = 500; // 1000ms/10fps = 100ms between frames

                        @Override
                        public void analyze(@NonNull ImageProxy image) {
                            long currentTimestamp = System.currentTimeMillis();

                            // Skip frames to maintain approximately 10 FPS
                            if (currentTimestamp - lastFrameTimestamp < FRAME_INTERVAL) {
                                image.close();
                                return;
                            }
                            lastFrameTimestamp = currentTimestamp;

                            // Calculate FPS
//                            frameCounter++;
//                            long currentTime = SystemClock.elapsedRealtime();
//                            if (currentTime - lastFpsTimestamp >= FPS_CALC_INTERVAL) {
//                                int fps = frameCounter * 1000 / (int)(currentTime - lastFpsTimestamp);
//                                Log.d("CameraX", "FPS: " + fps);
//                                frameCounter = 0;
//                                lastFpsTimestamp = currentTime;
//                            }

                            // Existing analysis code
                            if (SystemClock.elapsedRealtime() - mLastAnalysisResultTime < MIN_ANALYSIS_INTERVAL) {
                                image.close();
                                return;
                            }

                            final EyeTrackerActivity.AnalysisResult result = analyzeImage(image, image.getImageInfo().getRotationDegrees());
                            mLastAnalysisResultTime = SystemClock.elapsedRealtime();
                            image.close();
                        }
                    });

                    // Unbind any bound use cases before rebinding
                    cameraProvider.unbindAll();

                    // Bind use cases to camera
                    cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, imageAnalysis);
                } catch (ExecutionException | InterruptedException e) {
                    Log.e("CameraX", "Error setting up camera", e);
                }
            }, ContextCompat.getMainExecutor(this));
        } catch (Exception e) {
            Log.e("CameraX", "Error getting camera provider", e);
        }
    }

    private void stopCameraX() {
        try {
            ProcessCameraProvider.getInstance(this).get().unbindAll();
//            Log.d("CameraX", "CameraX stopped successfully.");
        } catch (ExecutionException | InterruptedException e) {
//            Log.e("CameraX", "Error stopping CameraX", e);
        }
    }

    private final Object timerLock = new Object();

    private void startTimer() {
        synchronized (timerLock) {
            if (!isRunning) {
                isRunning = true;
                isPause = false;
                handler.postDelayed(runnable, 1000);
//                Log.d("DetectorService", "Timer started");
            }
        }
    }
    private void pauseTimer() {
        synchronized (timerLock) {
            if (isRunning) {
                handler.removeCallbacks(runnable);
                isRunning = false;
                isPause = true;
//                Log.d("DetectorService", "Timer paused");
            }
        }
    }
    private void stopTimer() {
        synchronized (timerLock) {
            handler.removeCallbacks(runnable);
            timerSeconds = 0;
            isRunning = false;
            isPause = true;
            saveTimerState();
            updateNotification("Screen Time Limit Exceeded");
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

    private void detector() {
        handler.postDelayed(runnable, 1000);
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return mLifecycleRegistry;
    }


    private void eyeTimeTracker() {
        if (isLooking() && !isRunning) {
            startTimer();
            isRunning = true;
        }
        else if (!isLooking() && isRunning){
            pauseTimer();
            isRunning = false;
        }

        // Track app usage regardless of looking status
        trackAppUsage();

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
                            screenTimeLimitInSeconds = screenTimeLimit;
                        } else {
                            screenTimeLimitInSeconds = Integer.MAX_VALUE; // set a default value
                        }
//                        Log.d("Firebase", "Screen time limit: " + screenTimeLimitInSeconds);
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
//            Log.d("Screen time limit", "Time Limit Exceeds " + screenTimeLimitInSeconds + " seconds");
            pauseTimer();
            resetTimer();

            saveAppUsageData();

            // Create and start LockService
            intentLockService = new Intent(context, LockService.class);
            startForegroundService(intentLockService);
        } else {
            Log.d("Screen time limit", "Time Limit NOT yet Exceeds " + screenTimeLimitInSeconds + " seconds");
        }
    }

    private void stopLockService() {
        Intent lockServiceIntent = new Intent(this, LockService.class);
        stopService(lockServiceIntent);
//        Log.d("DetectorService", "Stop! LockService");
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

    private void registerScreenOffReceiver() {
        screenOffReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                    // Stop camera and pause timer when screen is turned off
                    stopCameraX();
                    pauseTimer();
                } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                    // Resume camera and timer when screen is turned on
                    setupCameraX();
                    if (!isPause) {
                        startTimer();
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(screenOffReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        isDestroyed = true;
        Toast.makeText(context, "Stopping Service...", Toast.LENGTH_SHORT).show();

        updateLockStatus();

        // Stop the camera
        stopCameraX();

        // Clean up timer-related resources
        handler.removeCallbacks(runnable);
        saveTimerState();

        // Clean up background thread
        if (mBackgroundThread != null) {
            mBackgroundThread.quitSafely();
            try {
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e("DetectorService", "Error shutting down background thread", e);
            }
        }

        // Clean up Firebase listeners if they exist
        if (screenTimeLimitRef != null) {
            screenTimeLimitRef.removeEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {}
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
        if (isUnlockDeviceNowRef != null) {
            isUnlockDeviceNowRef.removeEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {}
                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }

        // Clean up ML model
        if (mModule != null) {
            mModule.destroy();
            mModule = null;
        }

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }

        // Set the Lifecycle state to DESTROYED
        mLifecycleRegistry.setCurrentState(Lifecycle.State.DESTROYED);

        // Unregister screen off receiver
        if (screenOffReceiver != null) {
            unregisterReceiver(screenOffReceiver);
            screenOffReceiver = null;
        }
    }

    private UsageStatsManager usageStatsManager;

    private void initUsageTracking() {
        usageStatsManager = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        lastAppTrackTime = System.currentTimeMillis();
    }

    private void trackAppUsage() {
        // Define a set of excluded package names (home screens and system UIs)
        Set<String> excludedPackages = new HashSet<>(Arrays.asList(
                "com.android.launcher",       // Default Android launcher
                "com.google.android.googlequicksearchbox", // Pixel launcher / Google Assistant
                "com.miui.home",              // MIUI home screen
                "com.samsung.android.launcher", // Samsung launcher
                "com.huawei.android.launcher", // Huawei launcher
                "com.oppo.launcher",          // Oppo launcher
                "com.vivo.launcher",          // Vivo launcher
                "com.android.systemui"        // System UI
        ));

        if (!isLooking() || !isRunning) {
            if (currentForegroundApp != null && lastAppTrackTime > 0) {
                long currentTime = System.currentTimeMillis();
                long timeSpent = currentTime - lastAppTrackTime;
                appUsageMap.put(currentForegroundApp,
                        appUsageMap.getOrDefault(currentForegroundApp, 0L) + timeSpent);
            }
            lastAppTrackTime = 0;
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (lastAppTrackTime == 0) {
            lastAppTrackTime = currentTime;
            return;
        }

        UsageEvents usageEvents = usageStatsManager.queryEvents(lastAppTrackTime, currentTime);
        UsageEvents.Event currentEvent = new UsageEvents.Event();

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(currentEvent);
            Log.d("AppUsageTracker", "Event: " + currentEvent.getPackageName() + ", Type: " + currentEvent.getEventType());

            if (currentEvent.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                // Ignore excluded packages
                if (excludedPackages.contains(currentEvent.getPackageName())) {
                    continue;
                }

                if (currentForegroundApp != null && lastAppTrackTime > 0) {
                    long timeSpent = currentEvent.getTimeStamp() - lastAppTrackTime;
                    appUsageMap.put(currentForegroundApp,
                            appUsageMap.getOrDefault(currentForegroundApp, 0L) + timeSpent);
                }
                currentForegroundApp = currentEvent.getPackageName();
                lastAppTrackTime = currentEvent.getTimeStamp();
            }
        }

        if (currentForegroundApp != null && lastAppTrackTime > 0) {
            long timeSpent = currentTime - lastAppTrackTime;
            appUsageMap.put(currentForegroundApp,
                    appUsageMap.getOrDefault(currentForegroundApp, 0L) + timeSpent);
            lastAppTrackTime = currentTime;
        }
    }


    private void saveAppUsageData() {
        if (appUsageMap.isEmpty()) return;

        // Update final usage time for current app
        if (currentForegroundApp != null && lastAppTrackTime > 0) {
            long timeSpent = System.currentTimeMillis() - lastAppTrackTime;
            appUsageMap.put(currentForegroundApp,
                    appUsageMap.getOrDefault(currentForegroundApp, 0L) + timeSpent);
        }

        String childUserName = SharedPrefsUtil.getUserName(context);
        Map<String, Object> usageData = new HashMap<>();
        usageData.put("timestamp", new Date());
        usageData.put("childName", childUserName);
        usageData.put("totalScreenTime", timerSeconds);

        Map<String, Object> appDetails = new HashMap<>();
        PackageManager pm = getPackageManager();
        for (Map.Entry<String, Long> entry : appUsageMap.entrySet()) {
            String packageName = entry.getKey();
            try {
                ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
                String appName = pm.getApplicationLabel(appInfo).toString();
                // Convert to seconds and store only if usage time is significant (e.g., > 1 second)
                long seconds = entry.getValue() / 1000;
                if (seconds > 0) {
                    appDetails.put(appName, seconds);
                }
            } catch (PackageManager.NameNotFoundException e) {
                long seconds = entry.getValue() / 1000;
                if (seconds > 0) {
                    appDetails.put(packageName, seconds);
                }
            }
        }
        usageData.put("appUsage", appDetails);

        firestore.collection("AppUsageRecords")
                .document(uid)
                .collection(childUserName)
                .document()
                .set(usageData)
                .addOnSuccessListener(aVoid -> {
                    // Clear tracking data after successful save
                    appUsageMap.clear();
                    currentForegroundApp = null;
                    lastAppTrackTime = 0;
                });
    }
}