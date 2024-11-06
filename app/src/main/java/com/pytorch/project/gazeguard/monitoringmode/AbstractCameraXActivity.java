// Copyright (c) 2020 Facebook, Inc. and its affiliates.
// All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package com.pytorch.project.gazeguard.monitoringmode;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.CameraSelector;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import androidx.camera.core.SurfaceRequest;
//import androidx.camera.core.Surface;
import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;

public abstract class AbstractCameraXActivity<T> extends BaseModuleActivity {
    private static final int REQUEST_CODE_CAMERA_PERMISSION = 200;
    private static final String[] PERMISSIONS = {Manifest.permission.CAMERA};


    private long mLastAnalysisResultTime;

    protected abstract int getContentViewLayoutId();

    protected abstract TextureView getCameraPreviewTextureView();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentViewLayoutId());

        startBackgroundThread();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS,
                    REQUEST_CODE_CAMERA_PERMISSION);
        } else {
            try {
                setupCameraX();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(
                                this,
                                "You can't use object detection example without granting CAMERA permission",
                                Toast.LENGTH_LONG)
                        .show();
                finish();
            } else {
                try {
                    setupCameraX();
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void setupCameraX() throws ExecutionException, InterruptedException {
        final TextureView textureView = getCameraPreviewTextureView();

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                final CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                final Preview preview = new Preview.Builder()
                        .build();

                // Create a surface provider from the TextureView
                preview.setSurfaceProvider(new Preview.SurfaceProvider() {
                    @Override
                    public void onSurfaceRequested(@NonNull SurfaceRequest request) {
                        Surface surface = new Surface(textureView.getSurfaceTexture());
                        request.provideSurface(surface, ContextCompat.getMainExecutor(AbstractCameraXActivity.this), result -> {
                            // Handle the result if needed
                        });
                    }
                });

                final ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(480, 640))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(@NonNull ImageProxy image) {
                        if (SystemClock.elapsedRealtime() - mLastAnalysisResultTime < 50) {
                            image.close();
                            return;
                        }

                        final T result = analyzeImage(image, image.getImageInfo().getRotationDegrees());
                        if (result != null) {
                            mLastAnalysisResultTime = SystemClock.elapsedRealtime();
                            runOnUiThread(() -> applyToUiAnalyzeImageResult(result));
                        }
                        image.close();
                    }
                });

                // Unbind any bound use cases before rebinding
                cameraProvider.unbindAll();

                // Bind use cases to camera
                cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview, imageAnalysis);
            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraX", "Error setting up camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }


    @WorkerThread
    @Nullable
    protected abstract T analyzeImage(ImageProxy image, int rotationDegrees);

    @UiThread
    protected abstract void applyToUiAnalyzeImageResult(T result);
}