package com.ej.rovadiahyosefcalendar.classes;

import android.app.Activity;
import android.app.Application;
import android.app.UiModeManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.FocusFinder;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.ActionMenuView;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

import com.ej.rovadiahyosefcalendar.R;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.shape.Shapeable;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashSet;
import java.util.Set;

/**
 * Makes the app usable with a D-pad (Android TV / Google TV, or a phone with a keyboard), in every window of the app: activities,
 * and the dialogs and popup menus they open.
 * <ul>
 *     <li>Draws a Material 3 focus ring (a 3dp ring in colorSecondary, with a thin colorSurface ring inside it so it also stands out on
 *     colored buttons, over a light state layer) on whatever has the focus. This replaces the gold background the zmanim and limudim
 *     lists used to show, which could get stuck on a row and wiped the user's custom row color. Many views only show focus with a faint state layer, which can't be seen from the couch.
 *     The ring is only drawn while the user isn't touching the screen, so touch users never see it.</li>
 *     <li>On TV, opens the on-screen keyboard as soon as a text field gets the focus. Otherwise there is no way to open it in fields that
 *     handle the OK button themselves, like the SearchView on the location screen.</li>
 * </ul>
 * Views can opt out of the ring with {@code view.setTag(R.id.dpad_no_focus_ring, true)}.
 */
public final class DpadSupport implements Application.ActivityLifecycleCallbacks {

    private final boolean mIsTv;
    private final Set<Activity> mResumedActivities = new HashSet<>();

    private DpadSupport(Context context) {
        mIsTv = isTv(context);
    }

    public static void install(Application application) {
        application.registerActivityLifecycleCallbacks(new DpadSupport(application));
    }

    /**
     * @return true if the app is running on a TV (Android TV / Google TV), where there is no touch screen and the user has a D-pad remote.
     */
    public static boolean isTv(Context context) {
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        return (uiModeManager != null && uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION)
                || context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK);
    }

    /**
     * Opens the on-screen keyboard for the given text field. Use it where a field handles the OK button itself (so Android doesn't open
     * the keyboard when OK is pressed on it), so TV users can get the keyboard back after closing it.
     */
    public static void showKeyboard(View textField) {
        textField.requestFocus();
        textField.post(() -> {
            InputMethodManager imm = (InputMethodManager) textField.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(textField, 0);
            }
        });
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        View decorView = activity.getWindow().getDecorView();
        if (mIsTv && decorView.getTag(R.id.dpad_support_attached) == null) {
            applyTvSafeArea(activity);
        }
        attach(decorView);
    }

    /**
     * Keeps everything inside the TV's safe area: some TVs cut off the edges of the picture (overscan), and Google's TV guidelines ask for
     * a 5% margin (48dp on the sides, 27dp at the top and bottom). There are no system bars on TV, so the usual insets don't do it.
     */
    private static void applyTvSafeArea(Activity activity) {
        View content = activity.findViewById(android.R.id.content);
        if (content != null) {
            float density = activity.getResources().getDisplayMetrics().density;
            int horizontal = Math.round(48 * density);
            int vertical = Math.round(27 * density);
            content.setPadding(horizontal, vertical, horizontal, vertical);
        }
    }

    /**
     * @return true if the window wasn't followed yet
     */
    private boolean attach(View windowRoot) {
        if (windowRoot.getTag(R.id.dpad_support_attached) != null) {
            return false;
        }
        windowRoot.setTag(R.id.dpad_support_attached, Boolean.TRUE);
        WindowHelper helper = new WindowHelper(windowRoot);
        ViewTreeObserver observer = windowRoot.getViewTreeObserver();
        observer.addOnGlobalFocusChangeListener(helper);
        observer.addOnTouchModeChangeListener(helper);
        observer.addOnPreDrawListener(helper);
        windowRoot.post(() -> {// the window may already have its focus (e.g. a dialog found after it opened)
            helper.makeLongDialogMessageScrollable();
            helper.moveFocusIntoList(windowRoot.findFocus());
            helper.updateRing();
        });
        observer.addOnWindowFocusChangeListener(hasFocus -> {
            if (hasFocus) {
                windowRoot.post(() -> {// the first focus in a new window doesn't always come with a focus change
                    helper.makeLongDialogMessageScrollable();
                    helper.moveFocusIntoList(windowRoot.findFocus());
                    helper.updateRing();
                });
            } else {// a dialog or a popup menu probably opened on top of this window, which is a window of its own
                helper.updateRing();
                windowRoot.post(this::attachToAllWindows);
            }
        });
        return true;
    }

    /**
     * @return true if a window that wasn't followed yet was found
     */
    private boolean attachToAllWindows() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return true;// no public way to find the other windows; dialogs keep their default focus look
        }
        boolean foundNewWindow = false;
        for (View root : android.view.inspector.WindowInspector.getGlobalWindowViews()) {
            foundNewWindow |= attach(root);
        }
        return foundNewWindow;
    }

    private static boolean isScrollContainer(@Nullable View view) {
        return view instanceof RecyclerView || view instanceof ScrollView || view instanceof NestedScrollView;
    }

    /**
     * Follows the focus in one window: draws the ring on the focused view, and opens the keyboard for text fields on TV.
     */
    private final class WindowHelper implements ViewTreeObserver.OnGlobalFocusChangeListener, ViewTreeObserver.OnTouchModeChangeListener,
            ViewTreeObserver.OnPreDrawListener {

        private final View mRoot;
        private final FocusRingDrawable mRing;
        @Nullable
        private View mRingTarget;// the focused view
        @Nullable
        private View mRingHost;// the view whose overlay draws the ring: the focused view's parent
        private final Rect mRingRect = new Rect();
        private final Rect mLastRingRect = new Rect();

        WindowHelper(View root) {
            mRoot = root;
            mRing = new FocusRingDrawable(root.getResources().getDisplayMetrics().density);
        }

        @Override
        public void onGlobalFocusChanged(View oldFocus, View newFocus) {
            moveFocusIntoList(newFocus);
            updateRing();
            if (mIsTv && newFocus != null && !newFocus.isInTouchMode() && newFocus.onCheckIsTextEditor() && newFocus.isEnabled()) {
                showKeyboard(newFocus);
            }
        }

        /**
         * A list that takes the focus itself shows nothing, and the next press may jump to a row at the bottom of the screen. This moves
         * the focus to the first thing in it that is on screen instead (e.g. in the settings, the siddur tab, the sidebar, dialogs).
         */
        void moveFocusIntoList(@Nullable View focused) {
            if (isScrollContainer(focused) && !focused.isInTouchMode()) {
                ViewGroup list = (ViewGroup) focused;
                list.post(() -> focusFirstChildOnScreen(list));
            }
        }

        private void focusFirstChildOnScreen(ViewGroup list) {
            if (!list.isFocused()) {
                return;// the focus already moved on
            }
            View first = FocusFinder.getInstance().findNextFocus(list, null, View.FOCUS_DOWN);// searches from the top of what's on screen
            if (first != null) {
                first.requestFocus();
            }
        }

        @Override
        public void onTouchModeChanged(boolean isInTouchMode) {
            updateRing();
        }

        @Override
        public boolean onPreDraw() {
            updateRing();// follows the selection in lists, and views that change size
            return true;
        }

        /**
         * Puts the ring around the focused view. It is drawn by the parent, not by the focused view itself: views like MaterialButton
         * clip what they draw to their shape (a pill, smaller than the view), which would cut the ring off.
         */
        private void updateRing() {
            // no ring behind a dialog or a menu: only the window the D-pad is in shows one
            View target = mRoot.isInTouchMode() || !mRoot.hasWindowFocus() ? null : findRingTarget(mRoot.findFocus());
            View host = target != null && target.getParent() instanceof ViewGroup parent ? parent : target;
            if (target != mRingTarget || host != mRingHost) {
                if (mRingHost != null) {
                    mRingHost.getOverlay().remove(mRing);
                }
                mRingTarget = target;
                mRingHost = host;
                mLastRingRect.setEmpty();// so the ring's shape is set for the new view below
                if (host != null) {
                    mRing.setColors(
                            MaterialColors.getColor(target, com.google.android.material.R.attr.colorSecondary, 0xFF625B71),
                            MaterialColors.getColor(target, com.google.android.material.R.attr.colorSurface, 0xFFFFFFFF));
                    host.getOverlay().add(mRing);
                }
            }
            if (target == null) {
                return;
            }
            if (host == target) {
                mRingRect.set(0, 0, target.getWidth(), target.getHeight());
            } else {
                target.getHitRect(mRingRect);// where the view is in its parent, following scrolling and animations
            }
            if (!mRingRect.equals(mLastRingRect)) {
                if (mRingRect.width() != mLastRingRect.width() || mRingRect.height() != mLastRingRect.height()) {
                    mRing.setCornerRadius(Math.min(cornerRadiusOf(target), Math.min(mRingRect.width(), mRingRect.height()) / 2f));
                }
                mLastRingRect.set(mRingRect);
                mRing.setBounds(mRingRect);
            }
        }

        @Nullable
        private View findRingTarget(@Nullable View focused) {
            if (focused == null || focused == mRoot) {
                return null;
            }
            if (focused instanceof AbsListView list) {// list items in dialogs and popup menus are selected, not focused
                return list.getSelectedView();
            }
            if (Boolean.TRUE.equals(focused.getTag(R.id.dpad_no_focus_ring))
                    || (isScrollContainer(focused) && hasFocusableChild((ViewGroup) focused))// the focus moves on into it
                    || focused instanceof WebView
                    || focused.getClass().getName().startsWith("androidx.compose.")// Compose draws its own focus (see SiddurScreen)
                    || (focused instanceof EditText && isInTextInputLayout(focused))) {// the text field's box already shows the focus
                return null;
            }
            if (focused.getWidth() >= mRoot.getWidth() && focused.getHeight() >= mRoot.getHeight() * 0.9f) {
                return null;// a whole page, a ring around the screen tells nothing
            }
            return focused;
        }

        private boolean hasFocusableChild(ViewGroup group) {
            for (View focusable : group.getFocusables(View.FOCUS_DOWN)) {
                if (focusable != group) {
                    return true;
                }
            }
            return false;
        }

        /**
         * A dialog's message that is too long for the screen can only be scrolled by touch: the D-pad goes straight to the buttons.
         * This lets the message take the focus, starting there, so up/down scroll it (and down at its end moves on to the buttons).
         */
        void makeLongDialogMessageScrollable() {
            View message = mRoot.findViewById(androidx.appcompat.R.id.scrollView);
            if (message == null) {
                message = mRoot.findViewById(mRoot.getResources().getIdentifier("scrollView", "id", "android"));
            }
            if (isScrollContainer(message) && !message.isFocusable() && message.isShown()
                    && (message.canScrollVertically(1) || message.canScrollVertically(-1))) {
                message.setFocusable(true);
                if (!message.isInTouchMode()) {
                    message.requestFocus();
                }
            }
        }

        private boolean isInTextInputLayout(View view) {
            for (ViewParent parent = view.getParent(); parent instanceof ViewGroup; parent = parent.getParent()) {
                if (parent instanceof TextInputLayout) {
                    return true;
                }
            }
            return false;
        }

        private float cornerRadiusOf(View view) {
            float density = view.getResources().getDisplayMetrics().density;
            if (view instanceof Shapeable shapeable) {// buttons, cards, chips: follow their shape
                RectF bounds = new RectF(0, 0, view.getWidth(), view.getHeight());
                return shapeable.getShapeAppearanceModel().getTopLeftCornerSize().getCornerSize(bounds);
            }
            if (view instanceof ImageButton || view.getParent() instanceof ActionMenuView) {// icon buttons and toolbar actions are round
                return Math.min(view.getWidth(), view.getHeight()) / 2f;
            }
            return 12 * density;// Material 3's medium corner, for list rows and the like
        }
    }

    /**
     * The ring itself. It is drawn inside the view's bounds, since the overlay can't draw outside of them.
     */
    private static final class FocusRingDrawable extends Drawable {
        private final Paint mFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mOuterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint mInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float mOuterWidth;
        private final float mInnerWidth;
        private final RectF mRect = new RectF();
        private float mCornerRadius;

        FocusRingDrawable(float density) {
            mOuterWidth = 3 * density;
            mInnerWidth = 1.5f * density;
            mOuterPaint.setStyle(Paint.Style.STROKE);
            mOuterPaint.setStrokeWidth(mOuterWidth);
            mInnerPaint.setStyle(Paint.Style.STROKE);
            mInnerPaint.setStrokeWidth(mInnerWidth);
        }

        void setColors(int ringColor, int innerColor) {
            mFillPaint.setColor((ringColor & 0x00FFFFFF) | 0x29000000);// a 16% state layer, like Material 3's focused state
            mOuterPaint.setColor(ringColor);
            mInnerPaint.setColor(innerColor);
            invalidateSelf();
        }

        void setCornerRadius(float cornerRadius) {
            mCornerRadius = cornerRadius;
            invalidateSelf();
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            Rect bounds = getBounds();
            float inset = mOuterWidth / 2;
            mRect.set(bounds.left + inset, bounds.top + inset, bounds.right - inset, bounds.bottom - inset);
            float radius = Math.max(0, mCornerRadius - inset);
            canvas.drawRoundRect(mRect, radius, radius, mFillPaint);
            canvas.drawRoundRect(mRect, radius, radius, mOuterPaint);

            inset = mOuterWidth + mInnerWidth / 2;
            mRect.set(bounds.left + inset, bounds.top + inset, bounds.right - inset, bounds.bottom - inset);
            radius = Math.max(0, mCornerRadius - inset);
            canvas.drawRoundRect(mRect, radius, radius, mInnerPaint);
        }

        @Override
        public void setAlpha(int alpha) {
            mFillPaint.setAlpha(alpha * 0x29 / 255);
            mOuterPaint.setAlpha(alpha);
            mInnerPaint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            mFillPaint.setColorFilter(colorFilter);
            mOuterPaint.setColorFilter(colorFilter);
            mInnerPaint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        // A dialog shown while the activity starts up (e.g. once the zmanim are ready) never takes the focus away from the activity's
        // window, since that window never had it. So until the window gets the focus, keep looking for new windows.
        mResumedActivities.add(activity);
        View decorView = activity.getWindow().getDecorView();
        decorView.post(new Runnable() {
            @Override
            public void run() {
                boolean foundNewWindow = attachToAllWindows();
                if (!foundNewWindow && !decorView.hasWindowFocus() && mResumedActivities.contains(activity)) {
                    decorView.postDelayed(this, 300);
                }
            }
        });
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        mResumedActivities.remove(activity);
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {}
}
