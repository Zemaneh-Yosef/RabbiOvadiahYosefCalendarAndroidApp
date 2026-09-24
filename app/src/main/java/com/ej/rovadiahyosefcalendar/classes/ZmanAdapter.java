package com.ej.rovadiahyosefcalendar.classes;

import static android.content.Context.MODE_PRIVATE;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sJewishDateInfo;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sROZmanimCalendar;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.sSetupLauncher;
import static com.ej.rovadiahyosefcalendar.activities.ui.zmanim.ZmanimFragment.sNextUpcomingZman;
import static com.ej.rovadiahyosefcalendar.activities.ui.zmanim.ZmanimFragment.sShabbatMode;
import static com.ej.rovadiahyosefcalendar.classes.ZmanListEntryType.*;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Typeface;
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
import com.ej.rovadiahyosefcalendar.activities.SetupChaiTablesActivity;
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
    public boolean isZmanimInHebrew;
    private boolean wasTalitTefilinZmanClicked;

    public ZmanAdapter(Context context, List<ZmanListEntry> zmanim,
                       OnClickListeners.OnZmanClickListener onZmanClickListener) {
        this.zmanim = zmanim;
        this.onZmanClickListener = onZmanClickListener;
        this.context = context;
        mSharedPreferences = this.context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        isZmanimInHebrew = mSharedPreferences.getBoolean("isZmanimInHebrew", false);
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
        holder.itemView.setOnFocusChangeListener((view, b) -> {// support for TV
            view.setBackgroundColor(b ? context.getColor(R.color.dark_gold) : 0);
        });
        holder.setIsRecyclable(false);
        holder.mLeftTextViewSmall.setVisibility(View.GONE);
        holder.mRightTextViewSmall.setVisibility(View.GONE);
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
                if (zmanim.get(position).getZmanListEntryType() == FAST_ENDS_ZMAN ||
                        zmanim.get(position).getZmanListEntryType() == FAST_STARTS_NON_TISHA_BAV_ZMAN ||
                        zmanim.get(position).getZmanListEntryType() == FAST_STARTS_TISHA_BAV_ZMAN) {
                    holder.itemView.setVisibility(View.GONE);
                    holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
                } else {
                    holder.itemView.setVisibility(View.VISIBLE);
                    holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    ));
                }
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
                if (zmanim.get(position).getZmanListEntryType() == MISHEYAKIR_66_ZMAN) {
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
                    holder.mLeftTextView.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_NONE);
                    holder.mRightTextView.setTextSize(18);
                    holder.mLeftTextView.setTextSize(18);
                }
                if (zmanim.get(position).getZmanListEntryType() == PLAG_HAMINCHA_HB_ZMAN ||
                        zmanim.get(position).getZmanListEntryType() == PLAG_HAMINCHA_YY_ZMAN) {
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

                    switch (zmanim.get(position).getZmanListEntryType()) {
                        case THREE_WEEKS:
                        case NINE_DAYS:
                        case SHEVUA_SHECHAL_BO:
                            showThreeWeeksDialog(title);
                            break;
                        case ULCHAPARAT_PESHA:
                            showUlChaparatPeshaDialog();
                            break;
                        case BIRCHAT_HALEVANA:
                            showBirchatLevanaDialog();
                            break;
                        case TEKUFA_TIME:
                        case TEKUFA_LENGTH:
                            showTekufaDialog();
                            break;
                        case DAY_OF_OMER:
                            showOmerDialog();
                            break;
                        case TACHANUN:
                        case PURIM_MESHULASH:
                            showTachanunDialog();
                            break;
                        case SHMITA_YEAR:
                            showShmitaDialog();
                            break;
                        case ELEVATION_VALUE:
                            showElevationDialog();
                            break;
                            //Zmanim start here
                        case ALOT_HASHACHAR_ZMAN:
                            showDawnDialog();
                            break;
                        case MISHEYAKIR_60_ZMAN:
                        case MISHEYAKIR_66_ZMAN:
                            if (wasTalitTefilinZmanClicked) {
                                showEarliestTalitTefilinDialog();
                            } else {
                                wasTalitTefilinZmanClicked = true;
                                if (onZmanClickListener != null) {
                                    onZmanClickListener.onItemClick();// request a new set of data
                                }
                            }
                            break;
                        case HANETZ_ZMAN:
                            showSunriseDialog();
                            break;
                        case SOF_ZMAN_ACHILAT_CHAMETZ_ZMAN:
                            showAchilatChametzDialog();
                            break;
                        case SOF_ZMAN_BIUR_CHAMETZ_ZMAN:
                            showBiurChametzDialog();
                            break;
                        case SOF_ZMAN_SHMA_MGA_ZMAN:
                            showShemaMGADialog();
                            break;
                        case SOF_ZMAN_SHMA_GRA_ZMAN:
                            showShmaGRADialog();
                            break;
                        case BIRKAT_HACHMAH_ZMAN:
                            showBirchatHachamahDialog();
                            break;
                        case SOF_ZMAN_BERACHOT_SHMA_ZMAN:
                            showBrachotShmaDialog();
                            break;
                        case CHATZOT_ZMAN:
                            showChatzotDialog();
                            break;
                        case MINCHA_GEDOLAH_ZMAN:
                            showMinchaGedolaDialog();
                            break;
                        case MINCHA_KETANA_ZMAN:
                            showMinchaKetanaDialog();
                            break;
                        case PLAG_HAMINCHA_HB_ZMAN:
                        case PLAG_HAMINCHA_YY_ZMAN:
                            showPlagDialog();
                            break;
                        case CANDLELIGHTING_ZMAN:
                            showCandleLightingDialog();
                            break;
                        case SUNSET_ZMAN:
                            showShkiaDialog();
                            break;
                        case TZET_HAKOKHAVIM_ZMAN:
                            showTzaitDialog();
                            break;
                        case TZET_HAKOKHAVIM_LCHUMRA_ZMAN:
                            showTzaitLChumraDialog();
                            break;
                        case FAST_ENDS_ZMAN:
                            showTzaitTaanitDialog();
                            break;
                        case SHABBAT_CHAG_ENDS_ZMAN:
                            showTzaitShabbatDialog();
                            break;
                        case RABBENU_TAM_ZMAN:
                            showRTDialog();
                            break;
                        case CHATZOT_LAYLA_ZMAN:
                            showChatzotLaylaDialog();
                            break;
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

            if (zmanim.get(position).getZmanListEntryType() == ALOT_HASHACHAR_ZMAN) {
                View.OnClickListener onClickListener = (v -> showFastStartsChooserDialog(false));
                for (ZmanListEntry listEntry : zmanim) {
                    if (listEntry.getZmanListEntryType() == FAST_STARTS_NON_TISHA_BAV_ZMAN) {
                        if (isZmanimInHebrew) {
                            holder.mRightTextViewSmall.setText(listEntry.getTitle());
                            holder.mRightTextViewSmall.setVisibility(View.VISIBLE);
                        } else {//switch the views for english
                            holder.mLeftTextViewSmall.setText(listEntry.getTitle());
                            holder.mLeftTextViewSmall.setVisibility(View.VISIBLE);
                        }
                        holder.itemView.setOnClickListener(onClickListener);
                        break;
                    }
                }
            }

            if (zmanim.get(position).getZmanListEntryType() == SUNSET_ZMAN) {
                View.OnClickListener onClickListener = (v -> showFastStartsChooserDialog(true));
                for (ZmanListEntry listEntry : zmanim) {
                    if (listEntry.getZmanListEntryType() == FAST_STARTS_TISHA_BAV_ZMAN) {
                        if (isZmanimInHebrew) {
                            holder.mRightTextViewSmall.setText(listEntry.getTitle());
                            holder.mRightTextViewSmall.setVisibility(View.VISIBLE);
                        } else {//switch the views for english
                            holder.mLeftTextViewSmall.setText(listEntry.getTitle());
                            holder.mLeftTextViewSmall.setVisibility(View.VISIBLE);
                        }
                        holder.itemView.setOnClickListener(onClickListener);
                        break;
                    }
                }
            }

            if (zmanim.get(position).getZmanListEntryType() == TZET_HAKOKHAVIM_LCHUMRA_ZMAN) {
                View.OnClickListener onClickListener = (v -> showFastEndsChooserDialog());
                for (ZmanListEntry listEntry : zmanim) {
                    if (listEntry.getZmanListEntryType() == FAST_ENDS_ZMAN) {
                        if (isZmanimInHebrew) {
                            holder.mRightTextViewSmall.setText(listEntry.getTitle());
                            holder.mRightTextViewSmall.setVisibility(View.VISIBLE);
                        } else {//switch the views for english
                            holder.mLeftTextViewSmall.setText(listEntry.getTitle());
                            holder.mLeftTextViewSmall.setVisibility(View.VISIBLE);
                        }
                        holder.itemView.setOnClickListener(onClickListener);
                        break;
                    }
                }
            }

            if (zmanim.get(position).getZmanListEntryType() == ZmanListEntryType.BIRKAT_HACHMAH_ZMAN) {// it only happens every twenty-eight years, so we should highlight it
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

    public static class ZmanViewHolder extends RecyclerView.ViewHolder {

        TextView mRightTextView;
        TextView mRightTextViewSmall;
        TextView mMiddleTextView;
        TextView mLeftTextView;
        TextView mLeftTextViewSmall;

        public ZmanViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            setIsRecyclable(false);
            mLeftTextView = itemView.findViewById(R.id.zmanLeftTextView);
            mLeftTextViewSmall = itemView.findViewById(R.id.zmanLeftTextViewSmall);
            mMiddleTextView = itemView.findViewById(R.id.zmanMiddleTextView);
            mRightTextView = itemView.findViewById(R.id.zmanRightTextView);
            mRightTextViewSmall = itemView.findViewById(R.id.zmanRightTextViewSmall);
        }
    }

    private void resetDialogBuilder() {
        dialogBuilder = new MaterialAlertDialogBuilder(context);
        dialogBuilder.setNegativeButton(context.getString(R.string.dismiss), (dialog, which) -> dialog.dismiss());
        dialogBuilder.create();
    }

    private Spanned loadContentFromFile(String path) {
        AssetManager am = context.getAssets();
        try {
            InputStream is = am.open(path);
            int size = is.available();
            byte[] buffer = new byte[size];
            //noinspection ResultOfMethodCallIgnored
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

    private void showFastStartsChooserDialog(boolean isForSunset) {
        AlertDialog alertDialog = dialogBuilder.setTitle(R.string.choose_which_dialog_to_view)
                .setMessage(null)
                .setPositiveButton(R.string.fast_starts, (dialogInterface, i) -> showTaanitStartDialog())
                .setNegativeButton(isForSunset ? R.string.sunset : R.string.alot_hashachar, (dialogInterface, i) -> {
                    if (isForSunset) {
                        showShkiaDialog();
                    } else {
                        showDawnDialog();
                    }
                })
                .create();
        alertDialog.show();
        resetDialogBuilder();
    }

    private void showFastEndsChooserDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle(R.string.choose_which_dialog_to_view)
                .setMessage(null)
                .setPositiveButton(R.string.fast_ends, (dialogInterface, i) -> showTzaitTaanitDialog())
                .setNegativeButton(R.string.tzeit_hacochavim_l_chumra, (dialogInterface, i) -> showTzaitLChumraDialog())
                .create();
        alertDialog.show();
        resetDialogBuilder();
    }

    private void showTaanitStartDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Fast Starts - תענית מתחיל")
                .setMessage(R.string.fast_start_dialog)
                .create();
        alertDialog.show();
    }

    private void showDawnDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Dawn - עלות השחר - Alot HaShaḥar")
                .setMessage(Utils.isLocaleHebrew(context) ? loadContentFromFile("alothHB.md") : loadContentFromFile("aloth.md"))
                .create();
        alertDialog.show();
    }

    private void showEarliestTalitTefilinDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Earliest Talit/Tefilin - טלית ותפילין - Misheyakir")
                .setMessage(Utils.isLocaleHebrew(context) ? loadContentFromFile("misheyakirHB.md") : loadContentFromFile("misheyakir.md"))
                .create();
        alertDialog.show();
    }

    private void showSunriseDialog() {
        dialogBuilder.setTitle("Sunrise - הנץ - HaNetz")
                .setMessage(Utils.isLocaleHebrew(context) ? loadContentFromFile("hanetzHB.md") : loadContentFromFile("hanetz.md"))
                .setPositiveButton(R.string.setup_visible_sunrise, (dialog, which) -> {
                    if (sSetupLauncher != null) {
                        sSetupLauncher.launch(new Intent(context, SetupChaiTablesActivity.class));
                    }
                })
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
                .setMessage(Utils.isLocaleHebrew(context) ? loadContentFromFile("kriatShemaHB.md") : loadContentFromFile("kriatShema.md"))
                .create();
        alertDialog.show();
    }

    private void showShmaGRADialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Latest Shema GR\"A - סוף זמן שמע גר\"א")
                .setMessage(Utils.isLocaleHebrew(context) ? loadContentFromFile("kriatShemaHB.md") : loadContentFromFile("kriatShema.md"))
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
                .setMessage(Utils.isLocaleHebrew(context) ? loadContentFromFile("plagHaminchaHB.md") : loadContentFromFile("plagHamincha.md"))
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

    private void showTzaitShabbatDialog() {
        String shabbatSetting = "7.165°";
        if (sROZmanimCalendar != null && !sROZmanimCalendar.isUseAmudehHoraah()) {
            shabbatSetting = String.valueOf((int) sROZmanimCalendar.getAteretTorahSunsetOffset());
        }
        SharedPreferences mSettingsPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (mSettingsPreferences.getBoolean("overrideAHEndShabbatTime", false)) {
            String setting = mSettingsPreferences.getString("EndOfShabbatOpinion", "1");
            switch (setting) {
                case "1" -> {
                    if (sROZmanimCalendar != null) {
                        shabbatSetting = String.valueOf((int) sROZmanimCalendar.getAteretTorahSunsetOffset());
                    }
                }
                // do nothing for 2 because it's the same
                case "3" -> shabbatSetting = "";// don't show anything if we're using the lesser than the 2 options
            }
        }

        String title = shabbatSetting.isEmpty()
                ? "Shabbat/Chag Ends - צאת שבת/חג"
                : "Shabbat/Chag Ends (%) - (%) צאת שבת/חג".replace("%", shabbatSetting);

        AlertDialog alertDialog = dialogBuilder.setTitle(title)
                .setMessage(Utils.isLocaleHebrew(context) ? loadContentFromFile("tzetShabbatHB.md") : loadContentFromFile("tzetShabbat.md"))
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
        resetDialogBuilder();
    }

    private void showElevationDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle(context.getString(R.string.elevation).replace(":", ""))
                .setMessage(R.string.elevation_dialog)
                .setPositiveButton(context.getString(R.string.setup_elevation), (dialog, which) -> context.startActivity(new Intent(context, SetupElevationActivity.class)
                        .putExtra("loneActivity", true)))
                .create();
        alertDialog.show();
        resetDialogBuilder();
    }

    private void showTekufaDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Tekufa - Season - תקופה")
                .setMessage(Utils.isLocaleHebrew(context) ? loadContentFromFile("tekufa-hb.md") : loadContentFromFile("tekufot-en.md"))
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
                .setPositiveButton(context.getString(R.string.see_full_text), (dialog, which) -> context.startActivity(new Intent(context, SiddurViewActivity.class)
                        .putExtra("prayer", context.getString(R.string.sefirat_haomer))
                        .putExtra("JewishDay", sJewishDateInfo.getJewishCalendar().getJewishDayOfMonth())
                        .putExtra("JewishMonth", sJewishDateInfo.getJewishCalendar().getJewishMonth())
                        .putExtra("JewishYear", sJewishDateInfo.getJewishCalendar().getJewishYear())))
                .create();
        alertDialog.show();
        resetDialogBuilder();
    }
}
