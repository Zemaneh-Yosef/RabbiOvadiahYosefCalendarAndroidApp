package com.EJ.ROvadiahYosefCalendar.classes;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

public class PreferenceListener extends WearableListenerService {

    private MessageClient.OnMessageReceivedListener listener;

    public void setOnMessageReceivedListener(MessageClient.OnMessageReceivedListener listener, Context context) {
        this.listener = listener;
        Wearable.getMessageClient(context).addListener(listener);
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        if (listener != null) {
            listener.onMessageReceived(messageEvent);
        }
    }
}

