package com.ej.rovadiahyosefcalendar.classes;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.ej.rovadiahyosefcalendar.R;

import java.util.List;

import uk.co.deanwild.materialshowcaseview.IShowcaseListener;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;
import uk.co.deanwild.materialshowcaseview.target.Target;

/**
 * A short walkthrough of the zmanim list, shown once after the first setup (see NotificationSetupActivity#showAfterFirstSetup):
 * <ol>
 *     <li>The Talit/Tefilin row is highlighted until the user taps on it, which adds the second opinion (66 minutes) under it.</li>
 *     <li>Both rows are then highlighted, and the user is asked to tap on either one, which opens its explanation.</li>
 * </ol>
 * The user can skip it at any step, and it isn't shown again.
 */
public class ZmanimShowcase {

    public static final String PREF_SHOW_SHOWCASE = "showZmanimShowcase";
    private static final int MASK_COLOUR = 0xE6000000;
    private static final long WAIT_FOR_SECOND_ROW_MILLIS = 450;// the 66 row slides in for 300ms (see ZmanAdapter)

    private final Activity mActivity;
    private final SharedPreferences mSharedPreferences;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private int mStep;// 0 = not showing, 1 = waiting for the talit/tefilin row to be tapped, 2 = waiting for either row to be tapped
    @Nullable private MaterialShowcaseView mShowing;
    @Nullable private Target mTarget;
    @Nullable private RecyclerView mList;
    // The list can be rebuilt while the walkthrough is showing (e.g. when the visible sunrise is calculated). The library moves the
    // highlight on every layout, but doesn't redraw it, so the old place would stay cut out.
    private final ViewTreeObserver.OnGlobalLayoutListener mRedrawOnLayout = () -> {
        MaterialShowcaseView showing = mShowing;
        if (showing != null && mTarget != null) {
            showing.setTarget(mTarget);
            showing.invalidate();
        }
    };

    public ZmanimShowcase(Activity activity, SharedPreferences sharedPreferences) {
        mActivity = activity;
        mSharedPreferences = sharedPreferences;
    }

    /**
     * Shows the first step if the walkthrough wasn't done yet and the talit/tefilin row is on the screen.
     */
    public void startIfNeeded(RecyclerView zmanimList) {
        if (mStep != 0 || !mSharedPreferences.getBoolean(PREF_SHOW_SHOWCASE, false)) {
            return;
        }
        zmanimList.postDelayed(() -> {
            if (mStep != 0 || mActivity.isFinishing()) {
                return;
            }
            View talitRow = findRow(zmanimList, ZmanListEntryType.MISHEYAKIR_60_ZMAN);
            if (talitRow == null || !talitRow.isShown() || talitRow.getHeight() == 0 || !talitRow.getGlobalVisibleRect(new Rect())
                    || !mActivity.hasWindowFocus()) {
                return;// not on the screen yet (or another page is on top of it), this is called again when the list is shown or the page comes back
            }
            mStep = 1;
            mShowing = buildShowcase(zmanimList, talitRow, rowsTarget(zmanimList, talitRow, ZmanListEntryType.MISHEYAKIR_60_ZMAN),
                    R.string.showcase_tap_talit);
            mShowing.addShowcaseListener(new IShowcaseListener() {
                @Override
                public void onShowcaseDisplayed(MaterialShowcaseView showcaseView) {}

                @Override
                public void onShowcaseDismissed(MaterialShowcaseView showcaseView) {
                    if (showcaseView != mShowing) {
                        return;// cancelled, or the next step already started
                    }
                    // Tapping the row also dismisses this step, and the list then calls onTalitTefilinExpanded(). If that didn't happen,
                    // the user pressed skip.
                    mHandler.postDelayed(() -> {
                        if (mStep == 1) {
                            finish();
                        }
                    }, WAIT_FOR_SECOND_ROW_MILLIS);
                }
            });
            mShowing.show(mActivity);
        }, 500);// after the list is laid out
    }

    /**
     * Called when the talit/tefilin row was tapped and the second opinion was added to the list. Shows the second step.
     */
    public void onTalitTefilinExpanded(RecyclerView zmanimList) {
        if (mStep != 1) {
            return;
        }
        mStep = 2;
        mHandler.postDelayed(() -> {
            View talitRow = findRow(zmanimList, ZmanListEntryType.MISHEYAKIR_60_ZMAN);
            View talit66Row = findRow(zmanimList, ZmanListEntryType.MISHEYAKIR_66_ZMAN);
            if (mActivity.isFinishing() || talitRow == null || talit66Row == null) {
                finish();
                return;
            }
            mShowing = buildShowcase(zmanimList, talitRow, rowsTarget(zmanimList, talitRow, ZmanListEntryType.MISHEYAKIR_60_ZMAN, ZmanListEntryType.MISHEYAKIR_66_ZMAN),
                    R.string.showcase_tap_either);
            mShowing.addShowcaseListener(new IShowcaseListener() {
                @Override
                public void onShowcaseDisplayed(MaterialShowcaseView showcaseView) {}

                @Override
                public void onShowcaseDismissed(MaterialShowcaseView showcaseView) {
                    if (showcaseView != mShowing) {
                        return;// cancelled
                    }
                    finish();// either a row was tapped and its explanation is showing, or the user pressed skip
                }
            });
            mShowing.show(mActivity);
        }, WAIT_FOR_SECOND_ROW_MILLIS);
    }

    /**
     * Removes the walkthrough from the screen without finishing it (e.g. when the user leaves the page), so it is shown again next time.
     */
    public void cancel() {
        mHandler.removeCallbacksAndMessages(null);
        MaterialShowcaseView showing = mShowing;
        mShowing = null;
        mStep = 0;
        stopRedrawing();
        if (showing != null) {
            showing.removeFromWindow();
        }
    }

    private void finish() {
        mStep = 0;
        mShowing = null;
        stopRedrawing();
        mSharedPreferences.edit().putBoolean(PREF_SHOW_SHOWCASE, false).apply();
    }

    private void stopRedrawing() {
        if (mList != null) {
            mList.getViewTreeObserver().removeOnGlobalLayoutListener(mRedrawOnLayout);
            mList = null;
        }
        mTarget = null;
    }

    private MaterialShowcaseView buildShowcase(RecyclerView zmanimList, View anchor, Target target, int text) {
        MaterialShowcaseView showcaseView = new MaterialShowcaseView.Builder(mActivity)
                .setTarget(anchor)
                .withRectangleShape(true)
                .setContentText(mActivity.getString(text))
                .setDismissText(mActivity.getString(R.string.skip))
                .setMaskColour(MASK_COLOUR)
                .setTargetTouchable(true)// the tap goes through to the row underneath
                .setDismissOnTargetTouch(true)
                .setDismissOnTouch(false)
                .setDelay(200)
                .build();
        showcaseView.setTarget(target);
        mTarget = target;
        if (mList == null) {
            mList = zmanimList;
            zmanimList.getViewTreeObserver().addOnGlobalLayoutListener(mRedrawOnLayout);
        }
        return showcaseView;
    }

    @Nullable
    private static View findRow(RecyclerView zmanimList, ZmanListEntryType type) {
        if (!(zmanimList.getAdapter() instanceof ZmanAdapter adapter) || zmanimList.getLayoutManager() == null) {
            return null;
        }
        List<ZmanListEntry> zmanim = adapter.getZmanim();
        for (int i = 0; i < zmanim.size(); i++) {
            if (zmanim.get(i).getZmanListEntryType() == type) {
                return zmanimList.getLayoutManager().findViewByPosition(i);
            }
        }
        return null;
    }

    private static Rect boundsInWindow(View view) {
        int[] location = new int[2];
        view.getLocationInWindow(location);
        return new Rect(location[0], location[1], location[0] + view.getWidth(), location[1] + view.getHeight());
    }

    /**
     * Highlights the rows of the given types. The rows are looked up again every time, because the list can be rebuilt while the
     * walkthrough is showing (e.g. when the visible sunrise is calculated), which replaces the row views; the old ones are then off the
     * screen, and nothing would be highlighted.
     */
    private static Target rowsTarget(RecyclerView zmanimList, View firstRow, ZmanListEntryType... types) {
        return new Target() {
            private Rect mLastBounds = boundsInWindow(firstRow);

            @Override
            public Point getPoint() {
                Rect bounds = getBounds();
                return new Point(bounds.centerX(), bounds.centerY());
            }

            @Override
            public Rect getBounds() {
                Rect bounds = null;
                for (ZmanListEntryType type : types) {
                    View row = findRow(zmanimList, type);
                    if (row == null || !row.isAttachedToWindow()) {
                        return new Rect(mLastBounds);// e.g. while the new list is laid out
                    }
                    if (bounds == null) {
                        bounds = boundsInWindow(row);
                    } else {
                        bounds.union(boundsInWindow(row));
                    }
                }
                if (bounds != null && !bounds.isEmpty()) {
                    mLastBounds = bounds;
                }
                return new Rect(mLastBounds);
            }
        };
    }
}
