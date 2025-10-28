package com.ej.rovadiahyosefcalendar.classes;

import static android.content.Context.MODE_PRIVATE;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.mJewishDateInfo;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.mROZmanimCalendar;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sCurrentLocationName;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sSetupLauncher;
import static com.ej.rovadiahyosefcalendar.activities.ui.zmanim.ZmanimFragment.sNextUpcomingZman;
import static com.ej.rovadiahyosefcalendar.activities.ui.zmanim.ZmanimFragment.sShabbatMode;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.os.Build;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.activities.SetupChooserActivity;
import com.ej.rovadiahyosefcalendar.activities.SetupElevationActivity;
import com.ej.rovadiahyosefcalendar.activities.SiddurViewActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

public class ZmanAdapter extends RecyclerView.Adapter<ZmanAdapter.ZmanViewHolder> {

    private List<ZmanListEntry> zmanim;
    private final OnClickListeners.OnZmanClickListener onZmanClickListener;
    private final SharedPreferences mSharedPreferences;
    private final Context context;
    private MaterialAlertDialogBuilder dialogBuilder;
    private final boolean isZmanimInHebrew;
    private final boolean isZmanimEnglishTranslated;
    private boolean wasTalitTefilinZmanClicked;

    public ZmanAdapter(Context context, List<ZmanListEntry> zmanim,
                       OnClickListeners.OnZmanClickListener onZmanClickListener) {
        this.zmanim = zmanim;
        this.onZmanClickListener = onZmanClickListener;
        this.context = context;
        mSharedPreferences = this.context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        isZmanimInHebrew = mSharedPreferences.getBoolean("isZmanimInHebrew", false);
        isZmanimEnglishTranslated = mSharedPreferences.getBoolean("isZmanimEnglishTranslated", false);
        dialogBuilder = new MaterialAlertDialogBuilder(context);
        dialogBuilder.setNegativeButton(context.getString(R.string.dismiss), (dialog, which) -> dialog.dismiss());
        dialogBuilder.create();
    }

    public void setZmanim(List<ZmanListEntry> zmanim) {
        this.zmanim = zmanim;
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
            view.setBackgroundColor(b ? context.getColor(R.color.dark_gold) : 0);
        });
        holder.setIsRecyclable(false);
        String title = zmanim.get(position).getTitle();
        Date zman = zmanim.get(position).getZman();
        if (zmanim.get(position) != null) {
            String zmanTime;
            if (zman == null) {
                zmanTime = "XX:XX";
            } else {
                zmanTime = Utils.formatZmanTime(context, zmanim.get(position));
            }
            if (zmanim.get(position).isZman()) {
                if (isZmanimInHebrew) {
                    holder.mRightTextView.setTypeface(Typeface.DEFAULT_BOLD);
                    holder.mRightTextView.setText(title);//zman name

                    if (zman != null && zman.equals(sNextUpcomingZman)) {
                        zmanTime += "◄";
                    }
                    holder.mLeftTextView.setText(zmanTime);
                } else {//switch the views for english
                    holder.mLeftTextView.setTypeface(Typeface.DEFAULT_BOLD);
                    holder.mLeftTextView.setText(title);//zman name

                    if (zman != null && zman.equals(sNextUpcomingZman)) {
                        zmanTime = "➤" + zmanTime;//add arrow
                    }
                    holder.mRightTextView.setText(zmanTime);
                }
                if (zmanim.get(position).is66MisheyakirZman()) {
                    holder.itemView.setAlpha(0f);
                    holder.itemView.setTranslationY(holder.itemView.getHeight());
                    holder.itemView.animate()
                            .alpha(1f)
                            .translationY(0)
                            .setDuration(300)
                            .start();
                    if (isZmanimInHebrew) {
                        holder.mRightTextView.setTypeface(Typeface.DEFAULT);
                    } else {
                        holder.mLeftTextView.setTypeface(Typeface.DEFAULT);
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        holder.mLeftTextView.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_NONE);
                    }
                    holder.mRightTextView.setTextSize(18);
                    holder.mLeftTextView.setTextSize(18);
                }
                if (title.contains(new ZmanimNames(isZmanimInHebrew, isZmanimEnglishTranslated).getPlagHaminchaString())) {
                    SpannableStringBuilder spannable = new SpannableStringBuilder(title);
                    int startIndex = title.indexOf("(");
                    if (startIndex != -1) {// Set smaller font size for the text inside parenthesis
                        spannable.setSpan(new AbsoluteSizeSpan(16, true), startIndex, title.indexOf(")") + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    if (isZmanimInHebrew) {
                        holder.mRightTextView.setText(spannable);
                    } else {
                        holder.mLeftTextView.setText(spannable);
                    }
                }
            } else {// not a zman
                holder.mMiddleTextView.setText(title);
            }

            holder.itemView.setOnClickListener(v -> {
                if (!sShabbatMode && PreferenceManager.getDefaultSharedPreferences(context).getBoolean("showZmanDialogs", true)) {

                    checkZmanimForDialog(position);

                    if (title.contains(context.getString(R.string.three_weeks))
                            || title.contains(context.getString(R.string.nine_days))
                            || title.contains(context.getString(R.string.shevuah_shechal_bo))) {
                        showThreeWeeksDialog(title);
                    }

                    if (title.contains("וּלְכַפָּרַת פֶּשַׁע")) {
                        showUlChaparatPeshaDialog();
                    }

                    if (title.contains("ברכת הלבנה") || title.contains("Birkat Halevana")) {
                        showBirchatLevanaDialog();
                    }

                    if (title.contains(context.getString(R.string.elevation))) {
                        showElevationDialog();
                    }

                    if (title.contains("Tekufa") || title.contains("תקופת")) {
                        showTekufaDialog();
                    }

                    if (title.contains("Tachanun") || title.contains("תחנון") || title.contains("צדקתך")) {
                        showTachanunDialog();
                    }
                    if (title.contains("Shemita") || title.contains("שמיטה")) {
                        showShmitaDialog();
                    }
                    if (title.contains("day of Omer") || title.contains("ימים לעומר")) {
                        showOmerDialog();
                    }
                }
            });

            if (mSharedPreferences.getBoolean("useImage", false)) {
                holder.itemView.setBackgroundResource(0);
            } else if (mSharedPreferences.getBoolean("customBackgroundColor", false) &&
                    !mSharedPreferences.getBoolean("useDefaultBackgroundColor", false)) {
                holder.itemView.setBackgroundColor(mSharedPreferences.getInt("bColor", 0x32312C));
            }

            if (mSharedPreferences.getBoolean("customTextColor", false)) {
                holder.mLeftTextView.setTextColor(mSharedPreferences.getInt("tColor", 0xFFFFFFFF));
                holder.mMiddleTextView.setTextColor(mSharedPreferences.getInt("tColor", 0xFFFFFFFF));
                holder.mRightTextView.setTextColor(mSharedPreferences.getInt("tColor", 0xFFFFFFFF));
            }

            if (zmanim.get(position).isShouldBeDimmed()) {
                holder.mLeftTextView.setTextColor(context.getResources().getColor(R.color.disabled_gray, context.getTheme()));
                holder.mRightTextView.setTextColor(context.getResources().getColor(R.color.disabled_gray, context.getTheme()));
            }

            if (zmanim.get(position).isBirchatHachamahZman()) {// it only happens every twenty eight years, so we should highlight it
                holder.itemView.setBackground(AppCompatResources.getDrawable(context, R.drawable.colorful_gradient_square));
                holder.mLeftTextView.setTextColor(context.getResources().getColor(R.color.black, context.getTheme()));
                holder.mRightTextView.setTextColor(context.getResources().getColor(R.color.black, context.getTheme()));
            }
        }
    }

    @Override
    public int getItemCount() {
        return zmanim.size();
    }

    private String getElevation() {
        if (!mSharedPreferences.getBoolean("useElevation", true)) {//if the user has disabled the elevation setting, set the elevation to 0
            return "0";
        } else {
            return mSharedPreferences.getString("elevation" + sCurrentLocationName, "0");
        }
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

    private void resetDialogBuilder() {
        dialogBuilder = new MaterialAlertDialogBuilder(context);
        dialogBuilder.setNegativeButton(context.getString(R.string.dismiss), (dialog, which) -> dialog.dismiss());
        dialogBuilder.create();
    }

    private void checkZmanimForDialog(int position) {
        ZmanimNames zmanimNames = new ZmanimNames(isZmanimInHebrew, isZmanimEnglishTranslated);
        if (zmanim.get(position).getTitle().contains(zmanimNames.getAlotString())) {
            showDawnDialog();
        } else if (zmanim.get(position).getTitle().contains(zmanimNames.getTalitTefilinString())) {
            if (wasTalitTefilinZmanClicked) {
                showEarliestTalitTefilinDialog();
            } else {
                wasTalitTefilinZmanClicked = true;
                onZmanClickListener.onItemClick();// request a new set of data
            }
        } else if (zmanim.get(position).getTitle().contains(zmanimNames.getHaNetzString())) {
            showSunriseDialog();
        } else if (zmanim.get(position).getTitle().contains(zmanimNames.getAchilatChametzString())) {
            showAchilatChametzDialog();
        } else if (zmanim.get(position).getTitle().contains(zmanimNames.getBiurChametzString())) {
            showBiurChametzDialog();
        } else if (zmanim.get(position).getTitle().contains(zmanimNames.getShmaMgaString())) {
            showShemaMGADialog();
        } else if (zmanim.get(position).getTitle().contains(zmanimNames.getShmaGraString())) {
            showShmaGRADialog();
        } else if (zmanim.get(position).getTitle().contains(zmanimNames.getBirkatHachamaString())) {
            showBirchatHachamahDialog();
        } else if (zmanim.get(position).getTitle().contains(zmanimNames.getBrachotShmaString())) {
            showBrachotShmaDialog();
        } else if (zmanim.get(position).getTitle().contains(zmanimNames.getChatzotLaylaString())) {
            showChatzotLaylaDialog();
        } else if (zmanim.get(position).getTitle().contains(zmanimNames.getMinchaGedolaString())) {
            showMinchaGedolaDialog();
        } else if (zmanim.get(position).getTitle().contains(zmanimNames.getMinchaKetanaString())) {
            showMinchaKetanaDialog();
        } else if (zmanim.get(position).getTitle().contains(zmanimNames.getPlagHaminchaString())) {
            showPlagDialog();
        } else if (zmanim.get(position).getTitle().contains(zmanimNames.getCandleLightingString())) {
            showCandleLightingDialog();
        } else if (zmanim.get(position).getTitle().contains(zmanimNames.getSunsetString()) && !zmanim.get(position).getTitle().contains("לפני השקיעה")) {
            showShkiaDialog();
        } else if (zmanim.get(position).getTitle().contains(zmanimNames.getTzaitHacochavimString() + " " + zmanimNames.getLChumraString())) {
            showTzaitLChumraDialog();
        } else if (zmanim.get(position).getTitle().contains(zmanimNames.getTzaitHacochavimString())) {
            showTzaitDialog();
        } else if (zmanim.get(position).getTitle().contains(zmanimNames.getTzaitString() + zmanimNames.getTaanitString() + zmanimNames.getEndsString() + " " + zmanimNames.getLChumraString())) {
            showTzaitTaanitLChumraDialog();
        } else if (zmanim.get(position).getTitle().contains(zmanimNames.getTzaitString() + zmanimNames.getTaanitString() + zmanimNames.getEndsString())) {
            showTzaitTaanitDialog();
        } else if (zmanim.get(position).getTitle().contains("צאת שבת/חג")
                || zmanim.get(position).getTitle().contains("צאת שבת")
                || zmanim.get(position).getTitle().contains("צאת חג")
                || zmanim.get(position).getTitle().contains("Shabbat/Chag Ends")
                || zmanim.get(position).getTitle().contains("Shabbat Ends")
                || zmanim.get(position).getTitle().contains("Chag Ends")) {
            showTzaitShabbatDialog();
        } else if (zmanim.get(position).getTitle().contains(zmanimNames.getRTString())) {
            showRTDialog();
        } else if (zmanim.get(position).getTitle().contains(zmanimNames.getChatzotString())) {
            showChatzotDialog();
        }
    }

    private Spanned loadContentFromFile(String path) {
        AssetManager am = context.getAssets();
        try {
            InputStream is = am.open(path);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return HtmlCompat.fromHtml(HtmlRenderer.builder()
                    .build()
                    .render(Parser.builder()
                            .build()
                            .parse(new String(buffer, StandardCharsets.UTF_8))), HtmlCompat.FROM_HTML_MODE_LEGACY);
        } catch (IOException ex) {
            ex.printStackTrace();
            return new SpannableString("");
        }
    }

    private void showDawnDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Dawn - עלות השחר - Alot HaShaḥar")
                .setMessage(Utils.isLocaleHebrew() ? loadContentFromFile("alothHB.md") : loadContentFromFile("aloth.md"))
                .create();
        alertDialog.show();
    }

    private void showEarliestTalitTefilinDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Earliest Talit/Tefilin - טלית ותפילין - Misheyakir")
                .setMessage(Utils.isLocaleHebrew() ? loadContentFromFile("misheyakirHB.md") : loadContentFromFile("misheyakir.md"))
                .create();
        alertDialog.show();
    }

    private void showSunriseDialog() {
        dialogBuilder.setTitle("Sunrise - הנץ - HaNetz")
                .setMessage(Utils.isLocaleHebrew() ? loadContentFromFile("hanetzHB.md") : loadContentFromFile("hanetz.md"))
                .setPositiveButton(R.string.setup_visible_sunrise, (dialog, which) ->
                        sSetupLauncher.launch(new Intent(context, SetupChooserActivity.class)))
                .setNegativeButton(context.getString(R.string.dismiss), (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = dialogBuilder.create();
        dialog.setOnShowListener(dialogInterface -> {//Make the button stick out for people to see it
            Button visibleSunrise = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            visibleSunrise.setBackgroundTintMode(null);
            visibleSunrise.setBackground(ContextCompat.getDrawable(context, R.drawable.colorful_gradient_square));
            visibleSunrise.setTextColor(context.getColor(R.color.black));
            visibleSunrise.setTypeface(Typeface.DEFAULT_BOLD);
        });
        dialog.show();
        resetDialogBuilder();
    }

    private void showAchilatChametzDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Eating Ḥametz - אכילת חמץ - Akhilat Ḥametz")
                .setMessage(R.string.achilat_chametz_dialog)
                .create();
        alertDialog.show();
    }

    private void showBiurChametzDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Burning Ḥametz - ביעור חמץ - Biur Ḥametz")
                .setMessage(R.string.biur_chametz_dialog)
                .create();
        alertDialog.show();
    }

    private void showShemaMGADialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Latest Shema MG\"A - סוף זמן שמע מג\"א")
                .setMessage(Utils.isLocaleHebrew() ? loadContentFromFile("kriatShemaHB.md") : loadContentFromFile("kriatShema.md"))
                .create();
        alertDialog.show();
    }

    private void showShmaGRADialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Latest Shema GR\"A - סוף זמן שמע גר\"א")
                .setMessage(Utils.isLocaleHebrew() ? loadContentFromFile("kriatShemaHB.md") : loadContentFromFile("kriatShema.md"))
                .create();
        alertDialog.show();
    }

    private void showBirchatHachamahDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Latest Birkat HaChamah - סוף זמן ברכת החמה - Sof Zeman Birkat HaChamah")
                .setMessage(R.string.birchat_hachama_dialog)
                .create();
        alertDialog.show();
    }

    private void showBrachotShmaDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Berakhot Shema - ברכות שמע")
                .setMessage(R.string.brachotSHMAdialog)
                .create();
        alertDialog.show();
    }

    private void showChatzotDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Mid-day - חצות - Ḥatzot")
                .setMessage(R.string.chatzot_dialog)
                .create();
        alertDialog.show();
    }

    private void showMinchaGedolaDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Earliest Minḥa - מנחה גדולה - Minḥa Gedolah")
                .setMessage(R.string.mincha_gedola_dialog)
                .create();
        alertDialog.show();
    }

    private void showMinchaKetanaDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Minḥa Ketana - מנחה קטנה")
                .setMessage(R.string.mincha_ketana_dialog)
                .create();
        alertDialog.show();

    }

    private void showPlagDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Plag HaMinḥa - פלג המנחה")
                .setMessage(Utils.isLocaleHebrew() ? loadContentFromFile("plagHaminchaHB.md") : loadContentFromFile("plagHamincha.md"))
                .create();
        alertDialog.show();
    }

    private void showCandleLightingDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Candle Lighting - הדלקת נרות")
                .setMessage(String.format(context.getString(R.string.candle_lighting_dialog), PreferenceManager.getDefaultSharedPreferences(context).getString("CandleLightingOffset", "20")))
                .create();
        alertDialog.show();
    }

    private void showShkiaDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Sunset - שקיעה - Sheqi'a")
                .setMessage(R.string.sunset_dialog)
                .create();
        alertDialog.show();
    }

    private void showTzaitDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Nightfall - צאת הכוכבים - Tzet Hakokhavim")
                .setMessage(R.string.tzait_dialog)
                .create();
        alertDialog.show();
    }

    private void showTzaitLChumraDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Nightfall (Stringent) - צאת הכוכבים לחומרא - Tzet Hakokhavim L'Ḥumra")
                .setMessage(R.string.tzait_lchumra_dialog)
                .create();
        alertDialog.show();
    }

    private void showTzaitTaanitDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Fast Ends - צאת תענית")
                .setMessage(R.string.taanit_ends_dialog)
                .create();
        alertDialog.show();
    }

    private void showTzaitTaanitLChumraDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Fast Ends (Stringent) - צאת תענית לחומרא")
                .setMessage(R.string.taanit_ends_lchumra_dialog)
                .create();
        alertDialog.show();
    }

    private void showTzaitShabbatDialog() {
        String shabbatSetting = "7.165°";
        if (mROZmanimCalendar != null && !mROZmanimCalendar.isUseAmudehHoraah()) {
            shabbatSetting = String.valueOf((int) mROZmanimCalendar.getAteretTorahSunsetOffset());
        }
        SharedPreferences mSettingsPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (mSettingsPreferences.getBoolean("overrideAHEndShabbatTime", false)) {
            String setting = mSettingsPreferences.getString("EndOfShabbatOpinion", "1");
            switch (setting) {
                case "1" -> {
                    if (mROZmanimCalendar != null) {
                        shabbatSetting = String.valueOf((int) mROZmanimCalendar.getAteretTorahSunsetOffset());
                    }
                }
                // do nothing for 2 because it's the same
                case "3" -> shabbatSetting = "";// don't show anything if we're using the lesser than the 2 options
            }
        }
        AlertDialog alertDialog = dialogBuilder.setTitle("Shabbat/Chag Ends (%) - (%) צאת שבת/חג".replace("%", shabbatSetting))
                .setMessage(Utils.isLocaleHebrew() ? loadContentFromFile("tzetShabbatHB.md") : loadContentFromFile("tzetShabbat.md"))
                .create();
        alertDialog.show();
    }

    private void showRTDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Rabbenu Tam - רבינו תם")
                .setMessage(R.string.rt_dialog)
                .create();
        alertDialog.show();
    }

    private void showChatzotLaylaDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Midnight - חצות הלילה - Ḥatzot Layla")
                .setMessage(R.string.chatzot_layla_dialog)
                .create();
        alertDialog.show();
    }

    private void showUlChaparatPeshaDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("וּלְכַפָּרַת פֶּשַׁע")
                .setMessage(R.string.ulchaparat_pesha_dialog)
                .create();
        alertDialog.show();
    }

    private void showBirchatLevanaDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("ברכת הלבנה - Birkat Halevana")
                .setMessage(R.string.birchat_halevana_)
                .setPositiveButton(context.getString(R.string.see_full_text), (dialog, which) ->
                        context.startActivity(new Intent(context, SiddurViewActivity.class).putExtra("prayer", context.getString(R.string.birchat_levana))))
                .create();
        alertDialog.show();
    }

    private void showElevationDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle(context.getString(R.string.elevation))
                .setMessage(R.string.elevation_dialog)
                .setPositiveButton(context.getString(R.string.setup_elevation), (dialog, which) -> context.startActivity(new Intent(context, SetupElevationActivity.class)
                        .putExtra("loneActivity", true)))
                .create();
        alertDialog.show();
        resetDialogBuilder();
    }

    private void showTekufaDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Tekufa - Season - תקופה")
                .setMessage(Utils.isLocaleHebrew() ? loadContentFromFile("tekufa-hb.md") : loadContentFromFile("tekufot-en.md"))
                .create();
        alertDialog.show();
    }

    private void showTachanunDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Tachanun - תחנון")
                .setMessage(R.string.tachanun_dialog)
                .create();
        alertDialog.show();
    }

    private void showShmitaDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Shemita - שמיטה")
                .setMessage(R.string.shmita_dialog)
                .create();
        alertDialog.show();
    }

    private void showThreeWeeksDialog(String title) {
        AlertDialog alertDialog = dialogBuilder.setTitle(title)
                .setMessage(R.string.three_weeks_dialog)
                .create();
        alertDialog.show();
    }

    private void showOmerDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Sefirat HaOmer - ספירת העומר")
                .setMessage(R.string.omer_dialog)
                .setPositiveButton(context.getString(R.string.see_full_text), (dialog, which) -> {
                    context.startActivity(new Intent(context, SiddurViewActivity.class)
                            .putExtra("prayer", context.getString(R.string.sefirat_haomer))
                            .putExtra("JewishDay", mJewishDateInfo.getJewishCalendar().getJewishDayOfMonth())
                            .putExtra("JewishMonth", mJewishDateInfo.getJewishCalendar().getJewishMonth())
                            .putExtra("JewishYear", mJewishDateInfo.getJewishCalendar().getJewishYear()));
                })
                .create();
        alertDialog.show();
    }
}
