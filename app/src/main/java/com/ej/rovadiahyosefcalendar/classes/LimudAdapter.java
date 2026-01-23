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
import com.ej.rovadiahyosefcalendar.activities.SiddurViewActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kosherjava.zmanim.hebrewcalendar.Daf;
import com.kosherjava.zmanim.hebrewcalendar.YerushalmiYomiCalculator;
import com.kosherjava.zmanim.hebrewcalendar.YomiCalculator;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.List;

public class LimudAdapter extends RecyclerView.Adapter<LimudAdapter.ZmanViewHolder> {

    private List<LimudListEntry> limudim;
    private final Context context;
    private MaterialAlertDialogBuilder dialogBuilder;
    private final JewishDateInfo mJewishDateInfo;
    private View.OnClickListener onSeeMoreClickListener = null;
    private static final String[] masechtosBavliSefariaTransliterated = { "Berakhot", "Shabbat", "Eruvin", "Pesachim", "Shekalim",
            "Yoma", "Sukkah", "Beitzah", "Rosh_Hashana", "Taanit", "Megillah", "Moed_Katan", "Chagigah", "Yevamot",
            "Ketubot", "Nedarim", "Nazir", "Sotah", "Gittin", "Kiddushin", "Bava_Kamma", "Bava_Metzia", "Bava_Batra",
            "Sanhedrin", "Makkot", "Shevuot", "Avodah_Zarah", "Horayot", "Zevachim", "Menachot", "Chullin", "Bekhorot",
            "Arakhin", "Temurah", "Keritot", "Meilah", "Kinnim", "Tamid", "Midot", "Niddah" };
    private static final String[] masechtotYerushalmiSephariaTransliterated = { "Berakhot", "Peah", "Demai", "Kilayim", "Sheviit",
            "Terumot", "Maasrot", "Maaser_Sheni", "Challah", "Orlah", "Bikkurim", "Shabbat", "Eruvin", "Pesachim",
            "Beitzah", "Rosh_Hashana", "Yoma", "Sukkah", "Taanit", "Shekalim", "Megillah", "Chagigah", "Moed_Katan",
            "Yevamot", "Ketubot", "Sotah", "Nedarim", "Nazir", "Gittin", "Kiddushin", "Bava_Kamma", "Bava_Metzia",
            "Bava_Batra", "Shevuot", "Makkot", "Sanhedrin", "Avodah_Zarah", "Horayot", "Niddah", "No Daf Today" };

    public LimudAdapter(Context context, List<LimudListEntry> limudim, JewishDateInfo jewishDateInfo) {
        this.limudim = limudim;
        this.context = context;
        dialogBuilder = new MaterialAlertDialogBuilder(context);
        mJewishDateInfo = jewishDateInfo;
        dialogBuilder.setNegativeButton(context.getString(R.string.dismiss), (dialog, which) -> dialog.dismiss());
        dialogBuilder.create();
    }

    public LimudAdapter(Context context, List<LimudListEntry> limudim, JewishDateInfo jewishDateInfo, View.OnClickListener onSeeMoreClickListener) {
        this.limudim = limudim;
        this.context = context;
        this.onSeeMoreClickListener = onSeeMoreClickListener;
        dialogBuilder = new MaterialAlertDialogBuilder(context);
        mJewishDateInfo = jewishDateInfo;
        dialogBuilder.setNegativeButton(context.getString(R.string.dismiss), (dialog, which) -> dialog.dismiss());
        dialogBuilder.create();
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
                if (Utils.isLocaleHebrew(context)) {
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
                if (limudim.get(position).getLimudTitle().equals(context.getString(R.string.see_more)) && onSeeMoreClickListener != null) {
                    onSeeMoreClickListener.onClick(holder.itemView);
                    return;
                }
                dialogBuilder = new MaterialAlertDialogBuilder(context);
                if (limudim.get(position).getLimudTitle().contains(context.getString(R.string.daf_yomi))) {
                    Daf dafYomi = YomiCalculator.getDafYomiBavli(mJewishDateInfo.getJewishCalendar());
                    dafYomi.setMasechtaTransliterated(masechtosBavliSefariaTransliterated);
                    String masechta = dafYomi.getMasechtaTransliterated();
                    int daf = YomiCalculator.getDafYomiBavli(mJewishDateInfo.getJewishCalendar()).getDaf();
                    String dafYomiLink;
                    switch (masechta) {
                        case "Shekalim" -> dafYomiLink = "https://www.sefaria.org/Jerusalem_Talmud_" + masechta + "." + daf + "a";
                        case "Kinnim", "Midot" -> dafYomiLink = "https://www.dafyomi.org/index.php?masechta=meilah&daf=" + daf + "a";
                        case "Meilah" -> {
                            if (daf == 22) {
                                dafYomiLink = "https://www.dafyomi.org/index.php?masechta=meilah&daf=22a";
                            } else {
                                dafYomiLink  = "https://www.sefaria.org/" + masechta + "." + daf + "a";
                            }
                        }
                        default -> dafYomiLink  = "https://www.sefaria.org/" + masechta + "." + daf + "a";
                    }
                    dialogBuilder.setTitle(context.getString(R.string.open_sefaria_link_for) + limudim.get(position).getLimudTitle().replace(context.getString(R.string.daf_yomi) + " ", "") + "?")
                            .setMessage(R.string.this_will_open_the_sefaria_website_or_app_in_a_new_window_with_the_page)
                            .setPositiveButton(context.getString(R.string.open), (dialog, which) -> context.startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(dafYomiLink))))
                            .setNegativeButton(context.getString(R.string.dismiss), (dialog, which) -> dialog.dismiss())
                            .show();
                } else if (limudim.get(position).getLimudTitle().contains(context.getString(R.string.yerushalmi_yomi))) {
                    Daf dafYomiYerushalmi = YerushalmiYomiCalculator.getDafYomiYerushalmi(mJewishDateInfo.getJewishCalendar());
                    dafYomiYerushalmi.setYerushalmiMasechtaTransliterated(masechtotYerushalmiSephariaTransliterated);
                    String yerushalmiYomiLink = "https://www.sefaria.org/" + "Jerusalem_Talmud_" + dafYomiYerushalmi.getYerushalmiMasechtaTransliterated();
                    dialogBuilder.setTitle(context.getString(R.string.open_sefaria_link_for) + limudim.get(position).getLimudTitle().replace(context.getString(R.string.yerushalmi_yomi) + " ", "") + "?")
                            .setMessage(R.string.this_will_open_the_sefaria_website_or_app_in_a_new_window_with_the_page)
                            .setPositiveButton(context.getString(R.string.open), (dialog, which) -> context.startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(yerushalmiYomiLink))))
                            .setNegativeButton(context.getString(R.string.dismiss), (dialog, which) -> dialog.dismiss())
                            .show();
                } else if (limudim.get(position).getLimudTitle().contains(context.getString(R.string.mishna_yomi))) {
                    MishnaYomi mishnaYomi = new MishnaYomi(mJewishDateInfo.getJewishCalendar(), false);
                    String mishnaYomiLink = "https://www.sefaria.org/" + (mishnaYomi.getFirstMasechta().equals("Avot") ? "" : "Mishnah_") // apparently Pirkei Avot link is missing the Mishnah_ part
                            + replaceWithSefariaNames(mishnaYomi.getFirstMasechta()) + "." + mishnaYomi.getFirstPerek() + "." + mishnaYomi.getFirstMishna();
                    dialogBuilder.setTitle(context.getString(R.string.open_sefaria_link_for) + limudim.get(position).getLimudTitle().replace(context.getString(R.string.mishna_yomi) + " ", "") + "?")
                            .setMessage(R.string.this_will_open_the_sefaria_website_or_app_in_a_new_window_with_the_page)
                            .setPositiveButton(context.getString(R.string.open), (dialog, which) -> context.startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(mishnaYomiLink))))
                            .setNegativeButton(context.getString(R.string.dismiss), (dialog, which) -> dialog.dismiss())
                            .show();
                } else if (limudim.get(position).getLimudTitle().contains(context.getString(R.string.daily_halacha))) {
                    List<HalachaSegment> halachaYomi = HalachaYomi.INSTANCE.getDailyLearning(LocalDate.of(
                            mJewishDateInfo.getJewishCalendar().getGregorianYear(),
                            mJewishDateInfo.getJewishCalendar().getGregorianMonth() + 1,
                            mJewishDateInfo.getJewishCalendar().getGregorianDayOfMonth()));
                    if (halachaYomi != null) {
                        String halachaYomiLink = "https://www.sefaria.org/" +
                                (halachaYomi.get(0).getBookName().equals("שו\"ע - או\"ח") ?
                                        "Shulchan_Arukh%2C_Orach_Chayim." :
                                        "Kitzur_Shulchan_Arukh.")
                                + halachaYomi.get(0).getSiman()
                                + "."
                                + halachaYomi.get(0).getFirstSeif();
                        dialogBuilder.setTitle(context.getString(R.string.open_sefaria_link_for) + limudim.get(position).getLimudTitle().replace(context.getString(R.string.daily_halacha) + " ", "") + "?")
                                .setMessage(R.string.this_will_open_the_sefaria_website_or_app_in_a_new_window_with_the_page)
                                .setPositiveButton(context.getString(R.string.open), (dialog, which) -> context.startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(halachaYomiLink))))
                                .setNegativeButton(context.getString(R.string.dismiss), (dialog, which) -> dialog.dismiss())
                                .show();
                    }
                } else if (limudim.get(position).getLimudTitle().contains(context.getString(R.string.daily_tehilim))) {
                    if (limudim.get(position).getLimudTitle().contains(context.getString(R.string.weekly))) {
                        int[] weeklyTehilim = {1, 30, 51, 73, 90, 107, 120};
                        context.startActivity(new Intent(context, SiddurViewActivity.class)
                                .putExtra("prayer", context.getString(R.string.tehilim))
                                .putExtra("tehilimIndex", weeklyTehilim[mJewishDateInfo.getJewishCalendar().getDayOfWeek() - 1]));
                    } else if (limudim.get(position).getLimudTitle().contains(context.getString(R.string.monthly))) {
                        int[] monthlyTehilim = {1, 10, 18, 23, 29, 35, 39, 44, 49, 55, 60, 66, 69, 72, 77, 79, 83, 88, 90, 97, 104, 106, 108, 113, 119, 119, 120, 135, 140, 145};
                        context.startActivity(new Intent(context, SiddurViewActivity.class)
                                .putExtra("prayer", context.getString(R.string.tehilim))
                                .putExtra("tehilimIndex", monthlyTehilim[mJewishDateInfo.getJewishCalendar().getJewishDayOfMonth() - 1]));
                    }
                } else if (limudim.get(position).getLimudTitle().contains(context.getString(R.string.daily_nasi))) {
                    dialogBuilder.setTitle(limudim.get(position).getLimudTitle())
                            .setMessage(limudim.get(position).getSource())
                            .setNegativeButton(context.getString(R.string.dismiss), (dialog, which) -> dialog.dismiss())
                            .show();
                } else if (limudim.get(position).hasSource()) {// keep the hasSource check to avoid other listings
                    dialogBuilder.setTitle(limudim.get(position).getLimudTitle())
                            .setMessage(limudim.get(position).getDescription() + "\n-----\n" + context.getString(R.string.source) + limudim.get(position).getSource())
                            .setNegativeButton(context.getString(R.string.dismiss), (dialog, which) -> dialog.dismiss())
                            .show();
                }
            });

            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.disabled_gray, context.getTheme()));
        }
    }

    private String replaceWithSefariaNames(String masechta) {
        return switch (masechta) {
            case "Berachot" -> "Berakhot";
            case "Maaser Sheni" -> "Maaser_Sheni";
            case "Bikurim" -> "Bikkurim";
            case "Rosh Hashanah" -> "Rosh_Hashanah";
            case "Taanit" -> "Ta'anit";
            case "Moed Katan" -> "Moed_Katan";
            case "Avodah Zarah" -> "Avodah_Zarah";
            case "Avot" -> "Pirkei_Avot";
            case "Horiyot" -> "Horayot";
            case "Bechorot" -> "Bekhorot";
            case "Arachin" -> "Arakhin";
            case "Midot" -> "Middot";
            case "Keilim" -> "Kelim";
            case "Ohalot" -> "Oholot";
            case "Machshirin" -> "Makhshirin";
            case "Tevul Yom" -> "Tevul_Yom";
            case "Uktzin" -> "Oktzin";
            default -> masechta; // If no match is found, return the original string
        };
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
