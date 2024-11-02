package com.pytorch.project.gazeguard.common;

import android.content.Context;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import org.pytorch.demo.objectdetection.R;

import it.sephiroth.android.library.tooltip.Tooltip;

public class TooltipFormatter {

    public void setToolTip(Context context, View view, String text) {
        Tooltip.make(context,
                new Tooltip.Builder(101)
                        .withStyleId(R.style.ToolTipLayoutStyle)
                        .anchor(view, Tooltip.Gravity.TOP)
                        .closePolicy(new Tooltip.ClosePolicy()
                                .insidePolicy(true, false)
                                .outsidePolicy(false, true), 3000)
                        .activateDelay(500)
                        .showDelay(100)
                        .text(text)
                        .maxWidth(800)
                        .withArrow(false)
                        .withOverlay(true)
                        .fadeDuration(100)
                        .floatingAnimation(Tooltip.AnimationBuilder.SLOW)
                        .build()).show();
    }
}

