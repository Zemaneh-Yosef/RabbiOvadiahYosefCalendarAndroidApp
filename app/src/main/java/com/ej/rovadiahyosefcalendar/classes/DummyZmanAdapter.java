package com.ej.rovadiahyosefcalendar.classes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ej.rovadiahyosefcalendar.R;

public class DummyZmanAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final int SHIMMER_ITEM_COUNT;
    private static final int TYPE_CARD = 0;
    private static final int TYPE_TEXT = 1;

    public DummyZmanAdapter(int SHIMMER_ITEM_COUNT) {
        this.SHIMMER_ITEM_COUNT = SHIMMER_ITEM_COUNT;
    }

    @Override
    public int getItemViewType(int position) {
        // Make the first 2 items MaterialCardViews, and the rest TextViews
        if (position < 2) {
            return TYPE_CARD;
        }
        return TYPE_TEXT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_CARD) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.dummy_card_entry, parent, false);
            return new CardViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.dummy_entry, parent, false);
            return new TextViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // Do nothing, since this is just for the shimmer effect
    }

    @Override
    public int getItemCount() {
        return SHIMMER_ITEM_COUNT;
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public static class TextViewHolder extends RecyclerView.ViewHolder {
        public TextViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
