package com.ej.rovadiahyosefcalendar.classes;

import static android.content.Context.MODE_PRIVATE;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.mJewishDateInfo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ej.rovadiahyosefcalendar.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kosherjava.zmanim.hebrewcalendar.YomiCalculator;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

public class LimudAdapter extends RecyclerView.Adapter<LimudAdapter.ZmanViewHolder> {

    private List<LimudListEntry> limudim;
    private final SharedPreferences mSharedPreferences;
    private final Context context;
    private MaterialAlertDialogBuilder dialogBuilder;
    private final Locale locale = Locale.getDefault();

    public LimudAdapter(Context context, List<LimudListEntry> limudim) {
        this.limudim = limudim;
        this.context = context;
        mSharedPreferences = this.context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        dialogBuilder = new MaterialAlertDialogBuilder(context);
        dialogBuilder.setNegativeButton(context.getString(R.string.dismiss), (dialog, which) -> dialog.dismiss());
        dialogBuilder.create();
    }

    public void setLimudim(List<LimudListEntry> limudim) {
        this.limudim = limudim;
    }

    @NotNull
    @Override
    public ZmanViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.daily_entry, parent, false);
        return new ZmanViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ZmanViewHolder holder, int position) {
        holder.itemView.setOnFocusChangeListener((view, b) -> {// support for tv
            if (b) {
                view.setBackgroundColor(context.getColor(R.color.dark_gold));
            } else {
                view.setBackgroundColor(0);
            }
        });
        holder.setIsRecyclable(false);
        if (limudim.get(position) != null) {
            if (limudim.get(position).hasSource()) {// make parasha text bold
                holder.mLeftTextView.setText(limudim.get(position).getLimudTitle());
                holder.mLeftTextView.setTypeface(null, Typeface.BOLD);
                holder.mRightTextView.setText(limudim.get(position).getSource());
            } else {
                holder.mMiddleTextView.setText(limudim.get(position).getLimudTitle());
            }

            holder.itemView.setOnClickListener(v -> {
                    if (limudim.get(position).getLimudTitle().contains(context.getString(R.string.daf_yomi))) {
                        String masechta = YomiCalculator.getDafYomiBavli(mJewishDateInfo.getJewishCalendar()).getMasechtaTransliterated();
                        int daf = YomiCalculator.getDafYomiBavli(mJewishDateInfo.getJewishCalendar()).getDaf();
                        String dafYomiLink = "https://www.sefaria.org/" + masechta + "." + daf + "a";
                        dialogBuilder.setTitle("Open Sefaria link for " + limudim.get(position).getLimudTitle().replace(context.getString(R.string.daf_yomi) + " ", "") + "?");
                        dialogBuilder.setMessage("This will open the Sefaria website or app in a new window with the daf yomi.");
                        dialogBuilder.setPositiveButton(context.getString(R.string.ok), (dialog, which) -> {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(dafYomiLink));
                            context.startActivity(intent);
                        });
                        dialogBuilder.setNegativeButton(context.getString(R.string.dismiss), (dialog, which) -> dialog.dismiss());
                        dialogBuilder.show();
                    }
//                if (zmanim.get(position).getTitle().contains("Yerushalmi Yomi")) {//TODO: add sefaria link for yerushalmi yomi
//                    // open sefaria link for yerushalmi yomi
//                    String yerushalmiYomiLink = "https://www.sefaria.org/" + zmanim.get(position).getTitle().replace("Yerushalmi Yomi: ", "");
//                    dialogBuilder.setTitle("Open Sefaria link for " + zmanim.get(position).getTitle().replace("Yerushalmi Yomi: ", "") + "?");
//                    dialogBuilder.setMessage("This will open the Sefaria website or app in a new window with the yerushalmi yomi.");
//                    dialogBuilder.setPositiveButton("Open", (dialog, which) -> {
//                                Intent intent = new Intent(Intent.ACTION_VIEW);
//                                intent.setData(android.net.Uri.parse(yerushalmiYomiLink));
//                                context.startActivity(intent);
//                            });
//                    dialogBuilder.show();
//                    dialogBuilder.setPositiveButton("Dismiss", (dialog, which) -> dialog.dismiss());
//                }
            });

            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.disabled_gray, context.getTheme()));
        }
    }

    @Override
    public int getItemCount() {
        return limudim.size();
    }

    public static class ZmanViewHolder extends RecyclerView.ViewHolder {

        TextView mRightTextView;
        TextView mMiddleTextView;
        TextView mLeftTextView;

        public ZmanViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            setIsRecyclable(false);
            mLeftTextView = itemView.findViewById(R.id.zmanLeftTextView);
            mMiddleTextView = itemView.findViewById(R.id.zmanMiddleTextView);
            mRightTextView = itemView.findViewById(R.id.zmanRightTextView);
        }
    }
}
