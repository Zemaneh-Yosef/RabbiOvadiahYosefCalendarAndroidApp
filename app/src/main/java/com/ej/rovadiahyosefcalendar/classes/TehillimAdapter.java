package com.ej.rovadiahyosefcalendar.classes;

import android.content.Context;
import android.content.Intent;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
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
            int bookNumber;
            if (position == 0) {
                bookNumber = 1;
            } else if (position == 41) {
                bookNumber = 2;
            } else if (position == 72) {
                bookNumber = 3;
            } else if (position == 89) {
                bookNumber = 4;
            } else {
                bookNumber = 5;
            }
            if (Utils.isLocaleHebrew(context)) {
                book += " " + hebrewDateFormatter.formatHebrewNumber(bookNumber);
            } else {
                book += " " + bookNumber;
            }
            holder.tehillimBookNumber.setText(book);
            holder.tehillimBookNumber.setVisibility(View.VISIBLE);
        } else {
            holder.tehillimBookNumber.setVisibility(View.GONE);
        }
        holder.tehillimChapter.setText(hebrewDateFormatter.formatHebrewNumber(position + 1));

        TehilimChapter chapter = TehilimFactory.getChapters().get(position);

        String raw = Utils.removeNikudAndTaamim(chapter.getText());
        int big = chapter.getBigWords();

        CharSequence styled = makeFirstWordsBig(raw, big);

        holder.tehillimText.setText(styled);

        holder.tehillimChapterNumber.setText(String.valueOf(position + 1));
        holder.itemView.setOnClickListener(v -> context.startActivity(
                new Intent(context, SiddurViewActivity.class)
                        .putExtra("prayer", context.getString(R.string.tehilim))
                        .putExtra("tehilimIndex", position + 1)));
    }

    @Override
    public int getItemCount() {
        return TehilimFactory.getChapters().size();
    }

    private CharSequence makeFirstWordsBig(String text, int bigWords) {
        if (bigWords <= 0) return text;

        String[] words = text.split("\\s+");
        if (words.length == 0) return text;

        // Find the character index where the first X words end
        int endIndex = 0;
        for (int i = 0; i < Math.min(bigWords, words.length); i++) {
            endIndex += words[i].length() + 1; // +1 for the space
        }
        endIndex = Math.min(endIndex, text.length());

        SpannableString span = new SpannableString(text);

        // Make the first X words bigger
        span.setSpan(
            new RelativeSizeSpan(0.8f), // adjust size multiplier
            endIndex,
            text.length(),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        return span;
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
