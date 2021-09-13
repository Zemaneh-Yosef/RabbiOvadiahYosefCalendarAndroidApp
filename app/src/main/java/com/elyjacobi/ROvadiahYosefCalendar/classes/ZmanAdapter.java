package com.elyjacobi.ROvadiahYosefCalendar.classes;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.elyjacobi.ROvadiahYosefCalendar.R;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ZmanAdapter extends RecyclerView.Adapter<ZmanAdapter.ZmanViewHolder> {

    private final List<String> zmanim;

    public ZmanAdapter(List<String> zmanim) {
        this.zmanim = zmanim;
    }

    @NonNull
    @NotNull
    @Override
    public ZmanViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.entry, parent, false);
        return new ZmanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ZmanViewHolder holder, int position) {
        if (zmanim.get(position).contains("=")) {
            String[] zmanAndTime = zmanim.get(position).split("=");
            holder.mLeftTextView.setText(zmanAndTime[0]);//zman
            holder.mRightTextView.setText(zmanAndTime[1]);//time
        } else {
            holder.mMiddleTextView.setText(zmanim.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return zmanim.size();
    }

    static class ZmanViewHolder extends RecyclerView.ViewHolder {

        TextView mRightTextView;
        TextView mMiddleTextView;
        TextView mLeftTextView;

        public ZmanViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            setIsRecyclable(false);
            mLeftTextView = itemView.findViewById(R.id.zmanLeftTextView);
            mLeftTextView.setTypeface(Typeface.DEFAULT_BOLD);
            mMiddleTextView = itemView.findViewById(R.id.zmanMiddleTextView);
            mRightTextView = itemView.findViewById(R.id.zmanRightTextView);
        }
    }
}
