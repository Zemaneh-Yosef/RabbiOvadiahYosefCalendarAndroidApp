package com.ej.rovadiahyosefcalendar.classes;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.Map;

public class WearableCapabilityChecker {
    private final Context context;

    public WearableCapabilityChecker(Context context) {
        this.context = context;
    }

    public void checkIfWatchExists(final OnWatchCheckListener listener) {
        Task<List<Node>> nodesTask = Wearable.getNodeClient(context).getConnectedNodes();
        nodesTask.addOnSuccessListener(nodes -> {
            if (nodes.size() > 0) {
                listener.onWatchCheckResult(true);
            }
        });
    }

    public interface OnWatchCheckListener {
        void onWatchCheckResult(boolean hasWatch);
    }
}


