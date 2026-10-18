package com.pikminassistant.v2;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

public class PikminAccessibilityService
        extends AccessibilityService {

    public static PikminAccessibilityService instance;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onAccessibilityEvent(
            AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    public void tap(float x, float y) {
        if (Build.VERSION.SDK_INT < 24) return;

        Path path = new Path();
        path.moveTo(x, y);

        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(
                        path, 0, 100);

        GestureDescription gesture =
                new GestureDescription.Builder()
                        .addStroke(stroke)
                        .build();

        dispatchGesture(gesture, null, null);
    }
public void drag(
        float x1, float y1,
        float x2, float y2,
        long duration) {

    Path path = new Path();
    path.moveTo(x1, y1);
    path.lineTo(x2, y2);

    GestureDescription gesture =
        new GestureDescription.Builder()
            .addStroke(
                new GestureDescription.StrokeDescription(
                    path, 0, duration))
            .build();

    dispatchGesture(gesture, null, null);
}
    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }
}
