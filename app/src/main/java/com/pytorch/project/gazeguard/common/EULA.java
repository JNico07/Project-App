package com.pytorch.project.gazeguard.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Html;
import android.widget.Button;
import androidx.core.widget.NestedScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.pytorch.demo.objectdetection.R;

public class EULA {
    private static final String PREFS_NAME = "EULAPrefs";
    private static final String EULA_ACCEPTED_KEY = "eula_accepted";
    static SharedPreferences preferences;

    public static boolean isEULAAccepted(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(EULA_ACCEPTED_KEY, false);
    }

    public static void showEULA(Context context, EULAListener listener) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle("End-User License Agreement")
                .setView(R.layout.eula_dialog)
                .setCancelable(false)
                .setPositiveButton("Accept", null)
                .setNegativeButton("Decline", (dialog, which) -> {
                    preferences.edit().putBoolean(EULA_ACCEPTED_KEY, false).apply();
                    if (listener != null) {
                        listener.onEULADeclined();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();

        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setEnabled(false);

        NestedScrollView scrollView = dialog.findViewById(R.id.eulaScrollView);
        TextView eulaText = dialog.findViewById(R.id.eulaTextView);

        assert eulaText != null;
        eulaText.setText(Html.fromHtml(context.getString(R.string.EULA), Html.FROM_HTML_MODE_LEGACY));

        assert scrollView != null;
        scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (scrollY + v.getHeight() >= eulaText.getHeight()) {
                positiveButton.setEnabled(true);
                positiveButton.setOnClickListener(view -> {
                    preferences.edit().putBoolean(EULA_ACCEPTED_KEY, true).apply();
                    if (listener != null) {
                        listener.onEULAAccepted();
                    }
                    dialog.dismiss();
                });
            }
        });
    }

    public interface EULAListener {
        void onEULAAccepted();
        void onEULADeclined();
    }
}