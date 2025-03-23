package com.ej.rovadiahyosefcalendar.classes;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ej.rovadiahyosefcalendar.R;

public class DummyZmanAdapter extends RecyclerView.Adapter<DummyZmanAdapter.ShimmerViewHolder> {
    private final int SHIMMER_ITEM_COUNT;

    public DummyZmanAdapter(int SHIMMER_ITEM_COUNT) {
        this.SHIMMER_ITEM_COUNT = SHIMMER_ITEM_COUNT;
    }

    @NonNull
    @Override
    public ShimmerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.dummy_entry, parent, false);
        return new ShimmerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShimmerViewHolder holder, int position) {
        // Do nothing, since this is just for shimmer effect
    }

    @Override
    public int getItemCount() {
        return SHIMMER_ITEM_COUNT;
    }

    public static class ShimmerViewHolder extends RecyclerView.ViewHolder {
        public ShimmerViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
