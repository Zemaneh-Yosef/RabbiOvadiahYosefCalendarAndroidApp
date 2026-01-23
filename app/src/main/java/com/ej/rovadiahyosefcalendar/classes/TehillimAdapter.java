package com.ej.rovadiahyosefcalendar.classes;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.activities.SiddurViewActivity;
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter;

public class TehillimAdapter extends RecyclerView.Adapter<TehillimAdapter.TehillimViewHolder> {

    private final Context context;
    private final HebrewDateFormatter hebrewDateFormatter = new HebrewDateFormatter();

    public TehillimAdapter(Context context) {
        this.context = context;
        this.hebrewDateFormatter.setUseGershGershayim(false);
    }

    @NonNull
    @Override
    public TehillimViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.tehillim_entry, parent, false);
        return new TehillimViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TehillimViewHolder holder, int position) {
        if (position == 0 || position == 41 || position == 72 || position == 89 || position == 106) {
            String book = context.getString(R.string.book);
            if (position == 0) {
                book += " " + 1;
            } else if (position == 41) {
                book += " " + 2;
            } else if (position == 72) {
                book += " " + 3;
            } else if (position == 89) {
                book += " " + 4;
            } else {
                book += " " + 5;
            }
            holder.tehillimBookNumber.setText(book);
            holder.tehillimBookNumber.setVisibility(View.VISIBLE);
        } else {
            holder.tehillimBookNumber.setVisibility(View.GONE);
        }
        holder.tehillimChapter.setText(hebrewDateFormatter.formatHebrewNumber(position + 1));
        holder.tehillimText.setText(Utils.removeNikudAndTaamim(TehilimFactory.chapters[position]));
        holder.tehillimChapterNumber.setText(String.valueOf(position + 1));
        holder.itemView.setOnClickListener(v -> context.startActivity(
                new Intent(context, SiddurViewActivity.class)
                        .putExtra("prayer", context.getString(R.string.tehilim))
                        .putExtra("tehilimIndex", position + 1)));
    }

    @Override
    public int getItemCount() {
        return TehilimFactory.chapters.length;
    }

    public static class TehillimViewHolder extends RecyclerView.ViewHolder {

        TextView tehillimBookNumber;
        TextView tehillimChapter;
        TextView tehillimText;
        TextView tehillimChapterNumber;

        public TehillimViewHolder(@NonNull View itemView) {
            super(itemView);
            tehillimBookNumber = itemView.findViewById(R.id.tehillimBookNumber);
            tehillimChapter = itemView.findViewById(R.id.tehillimChapter);
            tehillimText = itemView.findViewById(R.id.tehillimText);
            tehillimChapterNumber = itemView.findViewById(R.id.tehillimChapterNumber);
        }
    }
}
