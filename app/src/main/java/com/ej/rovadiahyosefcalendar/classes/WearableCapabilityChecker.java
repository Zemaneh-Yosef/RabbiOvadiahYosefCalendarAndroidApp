package com.ej.rovadiahyosefcalendar.classes;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.List;

public class WearableCapabilityChecker {
    private final Context context;

    public WearableCapabilityChecker(Context context) {
        this.context = context;
    }

    public void checkIfWatchExists(final OnWatchCheckListener listener) {
        Task<List<Node>> nodesTask = Wearable.getNodeClient(context).getConnectedNodes();
        nodesTask.addOnSuccessListener(nodes -> listener.onWatchCheckResult(!nodes.isEmpty()))
                .addOnFailureListener(e -> {
                    Log.e("From main app", "Failed to get the connected nodes: " + e);
                    listener.onWatchCheckResult(false);
                });
    }

    public interface OnWatchCheckListener {
        void onWatchCheckResult(boolean hasWatch);
    }
}


