/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.window;

import static android.view.WindowInsets.Type.systemBars;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.DisplayCutout;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.systemui.compose.ComposeInitializer;
import com.android.systemui.statusbar.data.repository.StatusBarConfigurationController;

/**
 * Status bar view
 * We now extend WindowRootView so that we can host Compose views
 */
public class StatusBarWindowView extends FrameLayout {

    public static final String TAG = "PhoneStatusBarWindowView";
    public static final boolean DEBUG = false;

    private int mLeftInset = 0;
    private int mRightInset = 0;
    private int mTopInset = 0;

    private static final long DOUBLE_TAP_TIMEOUT_MS = 300;
    private static final float DOUBLE_TAP_SLOP_PX = 100f;

    @Nullable private StatusBarConfigurationController mConfigurationController;
    private final PowerManager mPowerManager;
    private long mLastTapTime;
    private float mLastTapX;
    private float mLastTapY;

    public StatusBarWindowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setClipChildren(false);
        mPowerManager = context.getSystemService(PowerManager.class);
    }

    /**
     * MikuUI double-tap-to-sleep. This view is inflated on a background thread, so a
     * GestureDetector (which needs a Handler/Looper) cannot be used here. Track the taps
     * manually instead. The status bar lives in its own window and the scene container no
     * longer routes these taps to the legacy shade handler.
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            long now = SystemClock.uptimeMillis();
            float dx = ev.getX() - mLastTapX;
            float dy = ev.getY() - mLastTapY;
            if (now - mLastTapTime <= DOUBLE_TAP_TIMEOUT_MS
                    && dx * dx + dy * dy <= DOUBLE_TAP_SLOP_PX * DOUBLE_TAP_SLOP_PX
                    && doubleTapToSleepEnabled()) {
                mLastTapTime = 0;
                mPowerManager.goToSleep(now);
            } else {
                mLastTapTime = now;
                mLastTapX = ev.getX();
                mLastTapY = ev.getY();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private boolean doubleTapToSleepEnabled() {
        if (mPowerManager == null || !mPowerManager.isInteractive()) {
            return false;
        }
        return Settings.Secure.getInt(getContext().getContentResolver(),
                Settings.Secure.DOUBLE_TAP_SLEEP_GESTURE, 1) != 0;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        ComposeInitializer.INSTANCE.onAttachedToWindow(this);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        ComposeInitializer.INSTANCE.onDetachedFromWindow(this);
    }

    /**
     * Sets the {@link StatusBarConfigurationController} that is associated with the display that
     * this view is attached to.
     */
    public void setStatusBarConfigurationController(
            @NonNull StatusBarConfigurationController configurationController) {
        mConfigurationController = configurationController;
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        StatusBarConfigurationController configurationController = mConfigurationController;
        if (configurationController != null) {
            configurationController.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets windowInsets) {
        final Insets insets = windowInsets.getInsetsIgnoringVisibility(systemBars());
        mLeftInset = insets.left;
        mRightInset = insets.right;
        mTopInset = 0;
        DisplayCutout displayCutout = getRootWindowInsets().getDisplayCutout();
        if (displayCutout != null) {
            mTopInset = displayCutout.getWaterfallInsets().top;
        }
        applyMargins();
        return windowInsets;
    }

    private void applyMargins() {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getLayoutParams() instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();
                if (lp.rightMargin != mRightInset || lp.leftMargin != mLeftInset
                        || lp.topMargin != mTopInset) {
                    lp.rightMargin = mRightInset;
                    lp.leftMargin = mLeftInset;
                    lp.topMargin = mTopInset;
                    child.requestLayout();
                }
            }
        }
    }
}
