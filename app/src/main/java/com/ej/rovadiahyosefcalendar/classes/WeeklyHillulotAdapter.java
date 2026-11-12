package com.ej.rovadiahyosefcalendar.classes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ej.rovadiahyosefcalendar.R;

import java.util.List;

/**
 * The weekly hillulot grid: one row per day that has hillulot, with the day on the left stretched down beside that day's
 * hillulot. Tapping a hillula opens the same dialog as in the daily list.
 */
public class WeeklyHillulotAdapter extends RecyclerView.Adapter<WeeklyHillulotAdapter.DayViewHolder> {

    /**
     * The hillulot of one day.
     * @param label e.g. "Sun 25\n14 Ḥesh"
     * @param isToday highlighted like today in the weekly zmanim
     * @param jewishDateInfo that day's date, for the dialogs
     */
    public record Day(String label, boolean isToday, JewishDateInfo jewishDateInfo, List<LimudListEntry> hillulot) {}

    private final Context mContext;
    private final List<Day> mDays;

    public WeeklyHillulotAdapter(Context context, List<Day> days) {
        mContext = context;
        mDays = days;
    }

    @NonNull
    @Override
    public DayViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.weekly_hillulot_day, parent, false);
        return new DayViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DayViewHolder holder, int position) {
        Day day = mDays.get(position);
        holder.mDay.setText(day.label());
        if (day.isToday()) {
            holder.mDay.setBackgroundColor(mContext.getColor(R.color.dark_gold));
        } else {
            holder.mDay.setBackgroundResource(R.drawable.border);
        }
        holder.mHillulot.setAdapter(new LimudAdapter(mContext, day.hillulot(), day.jewishDateInfo()));
    }

    @Override
    public int getItemCount() {
        return mDays.size();
    }

    public static class DayViewHolder extends RecyclerView.ViewHolder {

        final TextView mDay;
        final RecyclerView mHillulot;

        public DayViewHolder(@NonNull View itemView) {
            super(itemView);
            mDay = itemView.findViewById(R.id.weeklyHillulotDay);
            mHillulot = itemView.findViewById(R.id.weeklyHillulotDayRV);
            mHillulot.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            mHillulot.addItemDecoration(new DividerItemDecoration(itemView.getContext(), DividerItemDecoration.VERTICAL));
        }
    }
}
