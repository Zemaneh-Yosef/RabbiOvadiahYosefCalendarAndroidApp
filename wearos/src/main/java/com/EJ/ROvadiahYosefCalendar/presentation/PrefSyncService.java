package com.EJ.ROvadiahYosefCalendar.presentation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.google.android.gms.wearable.PutDataRequest.WEAR_URI_SCHEME;

import androidx.annotation.NonNull;

/**
 * A generic class for synchronizing {@code SharedPreferences} between devices on an Android Wear
 * personal-area network (PAN). More details (including instructions) at 
 * <a href="https://github.com/StringMon/prefsyncservice">Github</a>
 *
 * @author Sterling Udell
 * @version 1.0
 * @since 2016-01-01.
 */
@SuppressWarnings("unused, WeakerAccess")
public class PrefSyncService
        extends WearableListenerService {

    /**
     * The URI prefix used to identify preference values saved to the Data API by this class. It's
     * public so that it can be read by other pieces of code, if necessary, but probably shouldn't 
     * be changed.
     */
    public static final String DATA_SETTINGS_PATH = "/PrefSyncService/data/settings";

    /**
     * {@code PrefListener} waits this long before changes to {@code SharedPreferences} are synced 
     * to the PAN. This allows multiple changes made in quick succession to be batched for more 
     * efficient use of the Wear Data API. Feel free to tweak this value if needed.
     */
    public static int delayMillis = 1000;

    /**
     * If this String is set, the preference synchronization will use a {@code SharedPreferences}
     * file by this name (in the app's default directory), rather than the context's default
     * {@code SharedPreferences} file.
     */
    public static String sharedPrefsName = null;
    
    private static final String KEY_TIMESTAMP = "timestamp";
    
    private static String localNodeID;

    /**
     * A SyncFilter enables an app using {@code PrefListener} to specify which preferences should be
     * synchronized to other nodes on the PAN. The single method simply returns {@code true} or 
     * {@code false} depending on whether the supplied {@code key} should be synced.
     */
    public interface SyncFilter {
        boolean shouldSyncPref(String key);
    }
    
    /**
     * Re-synchronizes local preferences from the Data API if no such sync has yet been run.
     *
     * @param context The context of the preferences whose values are to be synced.
     * @param settings A {@code SharedPreferences} instance from which to retrieve values of the preferences.
     */
    public static void resyncIfNeeded(Context context, SharedPreferences settings) {
        if (!settings.contains(PrefListener.KEY_SYNC_DONE)) {
            new PrefListener(context, settings).resyncLocal();
        }
    }

    /**
     * PrefListener is the main API into preference synchronization. Instantiate
     * this class in your app, give it a {@code SyncFilter}, and use its {@code onResume} and 
     * {@code onPause} methods to stop and start synchronization.
     */
    public static class PrefListener
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        public SyncFilter syncFilter = null;

        public static final String KEY_SYNC_DONE = "PrefListener.sync_done";

        private final SharedPreferences settings;
        private final PrefHandler prefHandler;

        /**
         * Simplest ctor: just supply a context, and the default {@code SharedPreferences} for the app will
         * by synchronized.
         * <p>
         * Note that the {@code SharedPreferences} to be used can also be overridden by setting the 
         * static String {@code sharedPrefsName}.
         * 
         * @param context The context of the preferences whose values are to be synced.
         */
        public PrefListener(Context context) {
            this(context, TextUtils.isEmpty(sharedPrefsName) ?
                    PreferenceManager.getDefaultSharedPreferences(context) :
                    context.getSharedPreferences(sharedPrefsName, MODE_PRIVATE));
        }

        /**
         * If you want to synchronize a specific {@code SharedPreferences} file (rather than the app's 
         * default SharedPrefs), use this ctor.
         * 
         * @param context The context of the preferences whose values are to be synced.
         * @param settings A SharedPreferences instance from which to retrieve values of the preferences.
         */
        public PrefListener(Context context, SharedPreferences settings) {
            this.settings = settings;
            this.prefHandler = new PrefHandler(context);
        }

        /**
         * Begin listening for changes to the {@code SharedPreferences} to synchronize.
         */
        public void onResume() {
            settings.registerOnSharedPreferenceChangeListener(this);

            if (!settings.contains(KEY_SYNC_DONE)) {
                // It appears that no sync has been done on this device, so do one now.
                resyncLocal();
            }
        }

        /**
         * Stop listening for changes to the {@code SharedPreferences} to synchronize.
         */
        public void onPause() {
            settings.unregisterOnSharedPreferenceChangeListener(this);
        }

        /**
         * Re-synchronize all {@code SharedPreferences} values to the local node from any other attached 
         * nodes. It's up to the remote node(s) to decide which values to sync.
         */
        public void resyncLocal() {
            synchronized (prefHandler) {
                prefHandler.removeMessages(PrefHandler.ACTION_SYNC_ALL);
                prefHandler.obtainMessage(PrefHandler.ACTION_SYNC_ALL).sendToTarget();
            }
            settings.edit().putBoolean(KEY_SYNC_DONE, true).commit();
        }

        /**
         * Re-synchronize all matching {@code SharedPreferences} values from the local node to other 
         * attached nodes. 
         * <p>
         * Note that {@code SyncFilter} is required, otherwise we'd synchronize ALL prefs. If this is 
         * actually what you want, use a {@code SyncFilter} that always returns {@code true}.
         */
        public void resyncRemote() {
            if (syncFilter == null) {
                return;
            }

            Map<String, ?> allPrefs = settings.getAll();
            synchronized (prefHandler) {
                for (String key : allPrefs.keySet()) {
                    if (syncFilter.shouldSyncPref(key)) {
                        prefHandler.dataQueue.put(key, allPrefs.get(key));
                    }
                }

                prefHandler.clearFirst = true;
                prefHandler.removeMessages(PrefHandler.ACTION_SYNC_QUEUE);
                prefHandler.obtainMessage(PrefHandler.ACTION_SYNC_QUEUE).sendToTarget();
            }
        }

        /**
         * This is where changes to the local {@code SharedPreferences} are detected, and batched for 
         * synchronization to the Wear PAN. It's called by the system whenever the PrefListener
         * has been resumed and a change to the {@code SharedPreferences} occurs.
         * <p>
         * Note that, if you have other work to do with {@code SharedPreferences} changes, you're free
         * to override this method. Just be sure to call through to this ancestor (or sync won't 
         * occur).
         *
         * @param sharedPreferences The {@code SharedPreferences} that received
         *            the change.
         * @param key The key of the preference that was changed, added, or
         *            removed.
         */
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if ((syncFilter != null) && !syncFilter.shouldSyncPref(key) && !KEY_SYNC_DONE.equals(key)) {
                return;
            }

            Map<String, ?> allPrefs = sharedPreferences.getAll();
            synchronized (prefHandler) {
                prefHandler.dataQueue.put(key, allPrefs.get(key));

                // Wait a moment so that settings updates are batched
                prefHandler.removeMessages(PrefHandler.ACTION_SYNC_QUEUE);
                prefHandler.sendMessageDelayed(
                        prefHandler.obtainMessage(PrefHandler.ACTION_SYNC_QUEUE),
                        delayMillis);
            }
        }
    }

    /**
     * This is the receiving side of {@code PrefSyncService}, listening for changes coming from the
     * Wear Data API and saving them to the local SharedPreferences.
     * 
     * @param dataEvents the data incoming from the Data API
     */
    @SuppressLint({"CommitPrefEdits", "VisibleForTests"})
    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEvents) {

        if (localNodeID == null) {
            GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .build();
            googleApiClient.connect();

            Wearable.NodeApi.getLocalNode(googleApiClient)
                    .setResultCallback(getLocalNodeResult -> localNodeID = getLocalNodeResult.getNode().getId());
        }

        SharedPreferences settings;
        if (TextUtils.isEmpty(sharedPrefsName)) {
            settings = PreferenceManager.getDefaultSharedPreferences(this);
        } else {
            settings = getSharedPreferences(sharedPrefsName, MODE_PRIVATE);
        }
        SharedPreferences.Editor editor = settings.edit();
        Map<String, ?> allPrefs = settings.getAll();

        try {
            for (DataEvent event : dataEvents) {
                DataItem dataItem = event.getDataItem();
                Uri uri = dataItem.getUri();

                String nodeID = uri.getHost();
                if (nodeID.equals(localNodeID)) {
                    // Change originated on this device.
                    continue;
                }

                if (uri.getPath().startsWith(DATA_SETTINGS_PATH)) {
                    if (event.getType() == DataEvent.TYPE_DELETED) {
                        deleteItem(uri, editor, allPrefs);
                    } else {
                        saveItem(dataItem, editor, allPrefs);
                    }
                }
            }
        } finally {
            // We don't use apply() because we don't know what thread we're on
            editor.commit();
        }

        super.onDataChanged(dataEvents);
    }

    private static void deleteItem(Uri uri, SharedPreferences.Editor editor, Map<String, ?> allPrefs) {
        String key = uri.getLastPathSegment();
        if ((allPrefs == null) || allPrefs.containsKey(key)) {
            editor.remove(key);
        }
    }

    @SuppressLint("VisibleForTests")
    private static void saveItem(DataItem dataItem, SharedPreferences.Editor editor, Map<String, ?> allPrefs) {
        DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();
        if (dataMap.keySet().isEmpty()) {
            // Testing has shown that, when an item is deleted from the Data API, it
            // will often come through as an empty TYPE_CHANGED rather than a TYPE_DELETED
            deleteItem(dataItem.getUri(), editor, allPrefs);

        } else {
            for (String key : dataMap.keySet()) {
                Object value = dataMap.get(key);
                if (value == null) {
                    if ((allPrefs != null) && allPrefs.containsKey(key)) {
                        editor.remove(key);
                    }
                    continue;
                }
                if ((allPrefs != null) && value.equals(allPrefs.get(key))) {
                    // No change to value
                    continue;
                }
                if (key.equals(KEY_TIMESTAMP)) {
                    continue;
                }
                
                switch (value.getClass().getSimpleName()) {
                    case "Boolean": {
                        editor.putBoolean(key, (Boolean) value);
                        break;
                    }
                    case "Float": {
                        editor.putFloat(key, (Float) value);
                        break;
                    }
                    case "Integer": {
                        editor.putInt(key, (Integer) value);
                        break;
                    }
                    case "Long": {
                        editor.putLong(key, (Long) value);
                        break;
                    }
                    case "String": {
                        editor.putString(key, (String) value);
                        break;
                    }
                    case "Array": {
                        Set<String> strings = new HashSet<>();
                        Collections.addAll(strings, (String[]) value);
                        editor.putStringSet(key, strings);
                        break;
                    }
                }
            }
        }
    }

    private static class PrefHandler
            extends Handler
            implements GoogleApiClient.ConnectionCallbacks,
                    ResultCallback<DataItemBuffer> {
        private final Map<String, Object> dataQueue = new HashMap<>();

        private static final int ACTION_NONE = -1;
        private static final int ACTION_SYNC_QUEUE = 0;
        private static final int ACTION_SYNC_ALL = 1;

        @SuppressLint("VisibleForTests")
        private static final Uri DATA_SETTINGS_URI = new Uri.Builder()
                .scheme(WEAR_URI_SCHEME)
                .path(DATA_SETTINGS_PATH)
                .build();

        private final Context appContext;
        private final GoogleApiClient googleApiClient;
        private boolean clearFirst = false;
        private int pendingAction = ACTION_NONE;

        public PrefHandler(Context context) {
            appContext = context.getApplicationContext();

            googleApiClient = new GoogleApiClient.Builder(appContext)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .build();
            googleApiClient.connect();

            if (localNodeID == null) {
                Wearable.NodeApi.getLocalNode(googleApiClient)
                        .setResultCallback(getLocalNodeResult -> localNodeID = getLocalNodeResult.getNode().getId());
            }
        }

        @Override
        public void onConnected(Bundle bundle) {
            if (pendingAction > ACTION_NONE) {
                Message msg = obtainMessage(pendingAction);
                handleMessage(msg);
                msg.recycle();
                pendingAction = -1;
            }
        }

        @Override
        public void onConnectionSuspended(int i) {
        } // required for interface, not needed here

        @Override
        public void handleMessage(Message msg) {
            Log.d("PrefSyncService", "handleMessage: " + msg.what);
            if (googleApiClient.isConnected()) {
                if (msg.what == ACTION_SYNC_ALL) {
                    Wearable.DataApi.getDataItems(googleApiClient, DATA_SETTINGS_URI, DataApi.FILTER_PREFIX)
                            .setResultCallback(this);
                } else {
                    processQueue();
                }
            } else {
                if (msg.what >= pendingAction) {
                    pendingAction = msg.what;
                }
            }
        }

        @SuppressLint({"CommitPrefEdits", "VisibleForTests"})
        @Override
        public void onResult(@NonNull DataItemBuffer dataItems) {
            // This is the callback from the getDataItems() call in resyncAll()
            SharedPreferences.Editor editor;
            if (TextUtils.isEmpty(sharedPrefsName)) {
                editor = PreferenceManager.getDefaultSharedPreferences(appContext).edit();
            } else {
                editor = appContext.getSharedPreferences(sharedPrefsName, MODE_PRIVATE).edit();
            }
            try {
                if (dataItems.getStatus().isSuccess()) {
                    for (int i = dataItems.getCount() - 1; i >= 0; i--) {
                        Log.d("PrefSyncService", "Resync onResult: " + dataItems.get(i));
                        saveItem(dataItems.get(i), editor, null);
                    }
                }
            } finally {
                // We don't use apply() because we don't know what thread we're on
                editor.commit();
                dataItems.release();
            }
        }

        @SuppressLint("VisibleForTests")
        private void processQueue() {
            if (clearFirst) {
                // We clear items individually rather than with DataApi.deleteDataItems() so that
                // other clients will get notified of each deletion. Note also that we don't delete 
                // items for which we're sending a new value.
                final Set<String> newKeys = new HashSet<>(dataQueue.size());
                newKeys.addAll(dataQueue.keySet());
                Wearable.DataApi.getDataItems(googleApiClient)
                        .setResultCallback(buffer -> {
                            for (DataItem item : buffer) {
                                if (item.getUri().getPath().startsWith(DATA_SETTINGS_PATH) &&
                                        !newKeys.contains(item.getUri().getLastPathSegment())) {
                                    Wearable.DataApi.deleteDataItems(googleApiClient, item.getUri());
                                }
                            }
                        });
            }

            if (!dataQueue.isEmpty()) {
                // Create a data request to relay settings
                String path = DATA_SETTINGS_PATH;
                if (dataQueue.size() == 1) {
                    // Only one key being sent, so add it to the URI path
                    path += '/' + dataQueue.keySet().iterator().next();
                }
                PutDataMapRequest dataMapReq = PutDataMapRequest.create(path);
                DataMap dataMap = dataMapReq.getDataMap();

                if (clearFirst) {
                    dataMap.putLong(KEY_TIMESTAMP, System.nanoTime());
                }

                // Add the settings
                synchronized (this) {
                    for (String key : dataQueue.keySet()) {
                        Object value = dataQueue.get(key);
                        if (value == null) {
                            dataMap.remove(key);
                            continue;
                        }

                        switch (value.getClass().getSimpleName()) {
                            case "Boolean": {
                                dataMap.putBoolean(key, (Boolean) value);
                                break;
                            }
                            case "Float": {
                                dataMap.putFloat(key, (Float) value);
                                break;
                            }
                            case "Integer": {
                                dataMap.putInt(key, (Integer) value);
                                break;
                            }
                            case "Long": {
                                dataMap.putLong(key, (Long) value);
                                break;
                            }
                            case "String": {
                                dataMap.putString(key, (String) value);
                                break;
                            }
                            case "Set": {
                                @SuppressWarnings("unchecked")
                                Set<String> strings = (Set<String>) value;
                                dataMap.putStringArray(key, (String[]) strings.toArray());
                                break;
                            }
                        }
                    }
                    dataQueue.clear();
                }

                // Ship it!
                Wearable.DataApi.putDataItem(googleApiClient, dataMapReq.asPutDataRequest()
                        .setUrgent()); // ensure that it arrives in a timely manner
            }

            clearFirst = false;
        }
    }
}
