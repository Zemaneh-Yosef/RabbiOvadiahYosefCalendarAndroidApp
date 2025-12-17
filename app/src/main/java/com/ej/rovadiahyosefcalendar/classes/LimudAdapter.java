package com.ej.rovadiahyosefcalendar.classes;

import android.content.Context;
import android.content.Intent;
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
import com.kosherjava.zmanim.hebrewcalendar.Daf;
import com.kosherjava.zmanim.hebrewcalendar.YerushalmiYomiCalculator;
import com.kosherjava.zmanim.hebrewcalendar.YomiCalculator;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class LimudAdapter extends RecyclerView.Adapter<LimudAdapter.ZmanViewHolder> {

    private List<LimudListEntry> limudim;
    private final Context context;
    private MaterialAlertDialogBuilder dialogBuilder;
    private final JewishDateInfo mJewishDateInfo;
    private static final String[] masechtotYerushalmiTransliterated = { "Berakhot", "Peah", "Demai", "Kilayim", "Sheviit",
            "Terumot", "Maasrot", "Maaser Sheni", "Challah", "Orlah", "Bikkurim", "Shabbat", "Eruvin", "Pesachim",
            "Beitzah", "Rosh Hashanah", "Yoma", "Sukkah", "Taanit", "Shekalim", "Megillah", "Chagigah", "Moed Katan",
            "Yevamot", "Ketubot", "Sotah", "Nedarim", "Nazir", "Gittin", "Kiddushin", "Bava Kamma", "Bava Metzia",
            "Bava Batra", "Shevuot", "Makkot", "Sanhedrin", "Avodah Zarah", "Horayot", "Niddah", "No Daf Today" };

    public LimudAdapter(Context context, List<LimudListEntry> limudim, JewishDateInfo jewishDateInfo) {
        this.limudim = limudim;
        this.context = context;
        dialogBuilder = new MaterialAlertDialogBuilder(context);
        mJewishDateInfo = jewishDateInfo;
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
        if (limudim.get(position).getLimudTitle().contains(context.getString(R.string.rambam_yomi_3_chapters))) {// special case because the string gets too long
            holder.mMiddleTextView.setMaxLines(4);
        } else {
            holder.mMiddleTextView.setMaxLines(1);// set to 1, otherwise it won't shrink the text
        }

        if (limudim.get(position) != null) {
            if (limudim.get(position).hasSource()) {// make name text bold
                if (Utils.isLocaleHebrew()) {
                    holder.mRightTextView.setText(limudim.get(position).getLimudTitle());
                    holder.mRightTextView.setTypeface(null, Typeface.BOLD);
                } else {
                    holder.mLeftTextView.setText(limudim.get(position).getLimudTitle());
                    holder.mLeftTextView.setTypeface(null, Typeface.BOLD);
                }
            } else {
                holder.mMiddleTextView.setText(limudim.get(position).getLimudTitle());
            }

            holder.itemView.setOnClickListener(v -> {
                dialogBuilder = new MaterialAlertDialogBuilder(context);
                if (limudim.get(position).getLimudTitle().contains(context.getString(R.string.daf_yomi))) {
                        String masechta = YomiCalculator.getDafYomiBavli(mJewishDateInfo.getJewishCalendar()).getMasechtaTransliterated();
                        int daf = YomiCalculator.getDafYomiBavli(mJewishDateInfo.getJewishCalendar()).getDaf();
                        String dafYomiLink = "https://www.sefaria.org/" + masechta + "." + daf + "a";
                        dialogBuilder.setTitle(context.getString(R.string.open_sefaria_link_for) + limudim.get(position).getLimudTitle().replace(context.getString(R.string.daf_yomi) + " ", "") + "?");
                        dialogBuilder.setMessage(R.string.this_will_open_the_sefaria_website_or_app_in_a_new_window_with_the_page);
                        dialogBuilder.setPositiveButton(context.getString(R.string.open), (dialog, which) -> {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(dafYomiLink));
                            context.startActivity(intent);
                        });
                        dialogBuilder.setNegativeButton(context.getString(R.string.dismiss), (dialog, which) -> dialog.dismiss());
                        dialogBuilder.show();
                    } else if (limudim.get(position).getLimudTitle().contains(context.getString(R.string.yerushalmi_yomi))) {
                    Daf dafYomiYerushalmi = YerushalmiYomiCalculator.getDafYomiYerushalmi(mJewishDateInfo.getJewishCalendar());
                    dafYomiYerushalmi.setYerushalmiMasechtaTransliterated(masechtotYerushalmiTransliterated);
                    String yerushalmiYomiLink = "https://www.sefaria.org/" + "Jerusalem_Talmud_" + dafYomiYerushalmi.getYerushalmiMasechtaTransliterated();
                    dialogBuilder.setTitle(context.getString(R.string.open_sefaria_link_for) + limudim.get(position).getLimudTitle().replace(context.getString(R.string.yerushalmi_yomi) + " ", "") + "?");
                    dialogBuilder.setMessage(R.string.this_will_open_the_sefaria_website_or_app_in_a_new_window_with_the_page);
                    dialogBuilder.setPositiveButton(context.getString(R.string.open), (dialog, which) -> {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(android.net.Uri.parse(yerushalmiYomiLink));
                                context.startActivity(intent);
                            });
                    dialogBuilder.setNegativeButton(context.getString(R.string.dismiss), (dialog, which) -> dialog.dismiss());
                    dialogBuilder.show();
                } else if (limudim.get(position).getLimudTitle().contains(context.getString(R.string.mishna_yomi))) {
                    String mishnaYomi = MishnaYomi.getMishnaForDate(mJewishDateInfo.getJewishCalendar(), false);
                    String mishnaYomiLink = "https://www.sefaria.org/" + "Mishnah_" + mishnaYomi;
                    dialogBuilder.setTitle(context.getString(R.string.open_sefaria_link_for) + limudim.get(position).getLimudTitle().replace(context.getString(R.string.mishna_yomi) + " ", "") + "?");
                    dialogBuilder.setMessage(R.string.this_will_open_the_sefaria_website_or_app_in_a_new_window_with_the_page);
                    dialogBuilder.setPositiveButton(context.getString(R.string.open), (dialog, which) -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(android.net.Uri.parse(mishnaYomiLink));
                        context.startActivity(intent);
                    });
                    dialogBuilder.setNegativeButton(context.getString(R.string.dismiss), (dialog, which) -> dialog.dismiss());
                    dialogBuilder.show();
                } else if (limudim.get(position).getLimudTitle().contains(context.getString(R.string.daily_nasi))) {
                    dialogBuilder.setTitle(limudim.get(position).getLimudTitle());
                    dialogBuilder.setMessage(limudim.get(position).getSource());
                    dialogBuilder.setNegativeButton(context.getString(R.string.dismiss), (dialog, which) -> dialog.dismiss());
                    dialogBuilder.show();
                } else if (limudim.get(position).hasSource()) {// keep the hasSource check to avoid other listings
                    dialogBuilder.setTitle(limudim.get(position).getLimudTitle());
                    dialogBuilder.setMessage(limudim.get(position).getDescription() + "\n-----\n" + context.getString(R.string.source) + limudim.get(position).getSource());
                    dialogBuilder.setNegativeButton(context.getString(R.string.dismiss), (dialog, which) -> dialog.dismiss());
                    dialogBuilder.show();
                }
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
