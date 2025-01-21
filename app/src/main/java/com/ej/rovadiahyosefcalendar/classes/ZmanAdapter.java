package com.ej.rovadiahyosefcalendar.classes;

import static android.content.Context.MODE_PRIVATE;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.SHARED_PREF;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sCurrentLocationName;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sCurrentTimeZoneID;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sLatitude;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sLongitude;
import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.sSetupLauncher;
import static com.ej.rovadiahyosefcalendar.activities.ui.zmanim.ZmanimFragment.sNextUpcomingZman;
import static com.ej.rovadiahyosefcalendar.activities.ui.zmanim.ZmanimFragment.sShabbatMode;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.Typeface;
import android.net.Uri;
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
import com.ej.rovadiahyosefcalendar.activities.GetUserLocationWithMapActivity;
import com.ej.rovadiahyosefcalendar.activities.SetupChooserActivity;
import com.ej.rovadiahyosefcalendar.activities.SetupElevationActivity;
import com.ej.rovadiahyosefcalendar.activities.SiddurViewActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ZmanAdapter extends RecyclerView.Adapter<ZmanAdapter.ZmanViewHolder> {

    private List<ZmanListEntry> zmanim;
    private final SharedPreferences mSharedPreferences;
    private final Context context;
    private MaterialAlertDialogBuilder dialogBuilder;
    private final DateFormat zmanimFormat;
    private final DateFormat visibleSunriseFormat;
    private final DateFormat roundUpFormat;
    private final boolean roundUpRT;
    private final boolean isZmanimInHebrew;

    private final Locale locale = Locale.getDefault();

    static public String dateFormatPattern(boolean showSeconds) {
        return (LocaleChecker.isLocaleHebrew() ? "H" : "h")
                + ":mm"
                + (showSeconds ? ":ss" : "")
                + (LocaleChecker.isLocaleHebrew() ? "" : " aa");
    }

    public ZmanAdapter(Context context, List<ZmanListEntry> zmanim) {
        this.zmanim = zmanim;
        this.context = context;
        mSharedPreferences = this.context.getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        isZmanimInHebrew = mSharedPreferences.getBoolean("isZmanimInHebrew", false);

        zmanimFormat = new SimpleDateFormat(
            ZmanAdapter.dateFormatPattern(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("ShowSeconds", false)),
            locale);
        if (sCurrentTimeZoneID == null) {
            sCurrentTimeZoneID = TimeZone.getDefault().getID();
        }
        zmanimFormat.setTimeZone(TimeZone.getTimeZone(sCurrentTimeZoneID));

        visibleSunriseFormat = new SimpleDateFormat(ZmanAdapter.dateFormatPattern(true), Locale.getDefault());
        visibleSunriseFormat.setTimeZone(TimeZone.getTimeZone(sCurrentTimeZoneID));

        roundUpRT = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("RoundUpRT", false);
        roundUpFormat = new SimpleDateFormat(ZmanAdapter.dateFormatPattern(false), Locale.getDefault());
        roundUpFormat.setTimeZone(TimeZone.getTimeZone(sCurrentTimeZoneID));
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
        if (zmanim.get(position) == null)
            return;

        if (zmanim.get(position).isZman()) {
            if (isZmanimInHebrew) {
                holder.mRightTextView.setTypeface(Typeface.DEFAULT_BOLD);
                holder.mRightTextView.setText(zmanim.get(position).getTitle());//zman name

                String zmanTime;

                if (zmanim.get(position).getZman() == null) {
                    zmanTime = "XX:XX";
                } else {
                    if (zmanim.get(position).isRTZman() && roundUpRT) {
                        zmanTime = roundUpFormat.format(zmanim.get(position).getZman());
                    } else if (zmanim.get(position).isVisibleSunriseZman()) {
                        zmanTime = visibleSunriseFormat.format(zmanim.get(position).getZman());
                    } else {
                        zmanTime = zmanimFormat.format(zmanim.get(position).getZman());
                    }
                    if (zmanim.get(position).getZman().equals(sNextUpcomingZman)) {
                        zmanTime += "◄";
                    }
                }
                holder.mLeftTextView.setText(zmanTime);
            } else {//switch the views for english
                holder.mLeftTextView.setTypeface(Typeface.DEFAULT_BOLD);
                holder.mLeftTextView.setText(zmanim.get(position).getTitle());//zman name

                String zmanTime = "";
                if (zmanim.get(position).getZman() != null && zmanim.get(position).getZman().equals(sNextUpcomingZman)) {
                    zmanTime = "➤";//add arrow
                }

                if (zmanim.get(position).getZman() == null) {
                    zmanTime = "XX:XX";
                } else {
                    if (zmanim.get(position).isRTZman() && roundUpRT) {
                        zmanTime += roundUpFormat.format(zmanim.get(position).getZman());
                    } else if (zmanim.get(position).isVisibleSunriseZman()) {
                        zmanTime += visibleSunriseFormat.format(zmanim.get(position).getZman());
                    } else {
                        zmanTime += zmanimFormat.format(zmanim.get(position).getZman());
                    }
                }
                holder.mRightTextView.setText(zmanTime);
            }
        } else {
            holder.mMiddleTextView.setText(zmanim.get(position).getTitle());
        }

        if (position == 2) {// make parasha text bold
            holder.mMiddleTextView.setTypeface(null, Typeface.BOLD);
        }

        holder.itemView.setOnClickListener(v -> {
            if (!sShabbatMode && PreferenceManager.getDefaultSharedPreferences(context).getBoolean("showZmanDialogs", true)) {
                if (isZmanimInHebrew) {
                    checkHebrewZmanimForDialog(position);
                } else if (mSharedPreferences.getBoolean("isZmanimEnglishTranslated", false)) {
                    checkTranslatedEnglishZmanimForDialog(position);
                } else {
                    checkEnglishZmanimForDialog(position);
                }

                if (position == 0) {// first entry will always be the location name
                    dialogBuilder.setTitle("Location info for: " + zmanim.get(position).getTitle());
                    String locationInfo = "Location Name: " + sCurrentLocationName + "\n" +
                            "Latitude: " + sLatitude + "\n" +
                            "Longitude: " + sLongitude + "\n" +
                            "Elevation: " +
                            (mSharedPreferences.getBoolean("useElevation", true) ?
                                    mSharedPreferences.getString("elevation" + sCurrentLocationName, "0") : "0")
                            + " meters" + "\n" +
                            "Time Zone: " + sCurrentTimeZoneID;
                    dialogBuilder.setMessage(locationInfo);
                    dialogBuilder.setPositiveButton(R.string.share, ((dialog, which) -> {
                        Intent sendIntent = new Intent(Intent.ACTION_SEND);
                        sendIntent.setType("text/plain");
                        sendIntent.putExtra(Intent.EXTRA_TEXT, "https://royzmanim.com/calendar?locationName=" + sCurrentLocationName.replace(" ", "+").replace(",", "%2C") + "&lat=" + sLatitude + "&long=" + sLongitude + "&elevation=" + getElevation() + "&timeZone=" + sCurrentTimeZoneID);
                        context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.share)));
                    }));
                    dialogBuilder.setNeutralButton(R.string.change_location, (dialog, which) -> {
                        context.startActivity(new Intent(context, GetUserLocationWithMapActivity.class));
                    });
                    if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("LuachAmudeiHoraah", false)) {
                        dialogBuilder.setNegativeButton(context.getString(R.string.setup_elevation), (dialog, which) ->
                                sSetupLauncher.launch(new Intent(context, SetupElevationActivity.class).putExtra("fromMenu", true)));
                    }
                    dialogBuilder.show();
                    resetDialogBuilder();
                }

                // second entry (position 1) is always the date

                if (position == 2 && !zmanim.get(position).getTitle().equals("No Weekly Parsha") && !zmanim.get(position).getTitle().equals("אין פרשת שבוע")) {// third entry will always be the weekly parsha
                    String parsha;
                    if (zmanim.get(position).getTitle().equals("לך לך")
                            || zmanim.get(position).getTitle().equals("חיי שרה")
                            || zmanim.get(position).getTitle().equals("כי תשא")
                            || zmanim.get(position).getTitle().equals("אחרי מות")
                            || zmanim.get(position).getTitle().equals("שלח לך")
                            || zmanim.get(position).getTitle().equals("כי תצא")
                            || zmanim.get(position).getTitle().equals("כי תבוא")
                            || zmanim.get(position).getTitle().equals("וזאת הברכה ")) {
                        parsha = zmanim.get(position).getTitle();// ugly, but leave the first word and second word in these cases
                    } else {
                        parsha = zmanim.get(position).getTitle().split(" ")[0];//get first word
                    }
                    String parshaLink = "https://www.sefaria.org/" + parsha;
                    dialogBuilder.setTitle(context.getString(R.string.open_sefaria_link_for) + parsha + "?");
                    dialogBuilder.setMessage(R.string.this_will_open_the_sefaria_website_or_app_in_a_new_window_with_the_weekly_parsha);
                    dialogBuilder.setPositiveButton(context.getString(R.string.ok), (dialog, which) -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(parshaLink));
                        context.startActivity(intent);
                    });
                    dialogBuilder.setNegativeButton(context.getString(R.string.dismiss), (dialog, which) -> dialog.dismiss());
                    dialogBuilder.show();
                    resetDialogBuilder();
                }

                if (zmanim.get(position).getTitle().contains(context.getString(R.string.three_weeks))
                        || zmanim.get(position).getTitle().contains(context.getString(R.string.nine_days))
                        || zmanim.get(position).getTitle().contains(context.getString(R.string.shevuah_shechal_bo))) {
                    showThreeWeeksDialog(zmanim.get(position).getTitle());
                }

                if (zmanim.get(position).getTitle().contains("וּלְכַפָּרַת פֶּשַׁע")) {
                    showUlChaparatPeshaDialog();
                }

                if (zmanim.get(position).getTitle().contains("ברכת הלבנה") || zmanim.get(position).getTitle().contains("Birchat HaLevana")) {
                    showBirchatLevanaDialog();
                }

                if (zmanim.get(position).getTitle().contains(context.getString(R.string.elevation))) {
                    showElevationDialog();
                }

                if (zmanim.get(position).getTitle().contains("Tekufa") || zmanim.get(position).getTitle().contains("תקופת")) {
                    showTekufaDialog();
                }

                if (zmanim.get(position).getTitle().contains("Tachanun") || zmanim.get(position).getTitle().contains("תחנון") || zmanim.get(position).getTitle().contains("צדקתך")) {
                    showTachanunDialog();
                }
                if (zmanim.get(position).getTitle().contains("Shmita") || zmanim.get(position).getTitle().contains("שמיטה")) {
                    showShmitaDialog();
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

    private void checkHebrewZmanimForDialog(int position) {
        if (zmanim.get(position).getTitle().contains("עלות השחר")) {
            showDawnDialog();
        } else if (zmanim.get(position).getTitle().contains("טלית ותפילין")) {
            showEarliestTalitTefilinDialog();
        } else if (zmanim.get(position).getTitle().contains("הנץ")) {
            showSunriseDialog();
        } else if (zmanim.get(position).getTitle().contains("אכילת חמץ")) {
            showAchilatChametzDialog();
        } else if (zmanim.get(position).getTitle().contains("ביעור חמץ")) {
            showBiurChametzDialog();
        } else if (zmanim.get(position).getTitle().contains("שמע מג\"א")) {
            showShmaMGADialog();
        } else if (zmanim.get(position).getTitle().contains("שמע גר\"א")) {
            showShmaGRADialog();
        } else if (zmanim.get(position).getTitle().contains("ברכת החמה")) {
            showBirchatHachamahDialog();
        } else if (zmanim.get(position).getTitle().contains("ברכות שמע")) {
            showBrachotShmaDialog();
        } else if (zmanim.get(position).getTitle().contains("חצות הלילה")) {
            showChatzotLaylaDialog();
        } else if (zmanim.get(position).getTitle().contains("מנחה גדולה")) {
            showMinchaGedolaDialog();
        } else if (zmanim.get(position).getTitle().contains("מנחה קטנה")) {
            showMinchaKetanaDialog();
        } else if (zmanim.get(position).getTitle().contains("פלג המנחה")) {
            showPlagDialog();
        } else if (zmanim.get(position).getTitle().contains("הדלקת נרות")) {
            showCandleLightingDialog();
        } else if (zmanim.get(position).getTitle().contains("שקיעה")) {
            showShkiaDialog();
        } else if (zmanim.get(position).getTitle().contains("צאת הכוכבים לחומרא")) {
            showTzaitLChumraDialog();
        } else if (zmanim.get(position).getTitle().contains("צאת הכוכבים")) {
            showTzaitDialog();
        } else if (zmanim.get(position).getTitle().contains("צאת תענית לחומרא")) {
            showTzaitTaanitLChumraDialog();
        } else if (zmanim.get(position).getTitle().contains("צאת תענית")) {
            showTzaitTaanitDialog();
        } else if (zmanim.get(position).getTitle().contains("צאת שבת/חג")
                ||zmanim.get(position).getTitle().contains("צאת שבת")
                ||zmanim.get(position).getTitle().contains("צאת חג")) {
            showTzaitShabbatDialog();
        } else if (zmanim.get(position).getTitle().contains("רבינו תם")) {
            showRTDialog();
        } else if (zmanim.get(position).getTitle().contains("חצות")) {
            showChatzotDialog();
        }
    }

    private void checkTranslatedEnglishZmanimForDialog(int position) {
        if (zmanim.get(position).getTitle().contains("Dawn")) {
            showDawnDialog();
        } else if (zmanim.get(position).getTitle().contains("Earliest Talit/Tefilin")) {
            showEarliestTalitTefilinDialog();
        } else if (zmanim.get(position).getTitle().contains("Sunrise")) {
            showSunriseDialog();
        } else if (zmanim.get(position).getTitle().contains("eat Chametz")) {
            showAchilatChametzDialog();
        } else if (zmanim.get(position).getTitle().contains("burn Chametz")) {
            showBiurChametzDialog();
        } else if (zmanim.get(position).getTitle().contains("Shema MG\"A")) {
            showShmaMGADialog();
        } else if (zmanim.get(position).getTitle().contains("Shema GR\"A")) {
            showShmaGRADialog();
        } else if (zmanim.get(position).getTitle().contains("Birkat HaChamah")) {
            showBirchatHachamahDialog();
        } else if (zmanim.get(position).getTitle().contains("Berakhot Shema")) {
            showBrachotShmaDialog();
        } else if (zmanim.get(position).getTitle().contains("Mid-day")) {
            showChatzotDialog();
        } else if (zmanim.get(position).getTitle().contains("Earliest Minḥa")) {
            showMinchaGedolaDialog();
        } else if (zmanim.get(position).getTitle().contains("Minḥa Ketana")) {
            showMinchaKetanaDialog();
        } else if (zmanim.get(position).getTitle().contains("Plag HaMinḥa")) {
            showPlagDialog();
        } else if (zmanim.get(position).getTitle().contains("Candle Lighting")) {
            showCandleLightingDialog();
        } else if (zmanim.get(position).getTitle().contains("Sunset")) {
            showShkiaDialog();
        } else if (zmanim.get(position).getTitle().contains("Nightfall (Stringent)")) {
            showTzaitLChumraDialog();
        } else if (zmanim.get(position).getTitle().contains("Nightfall")) {
            showTzaitDialog();
        } else if (zmanim.get(position).getTitle().contains("Fast Ends (Stringent)")) {
            showTzaitTaanitLChumraDialog();
        } else if (zmanim.get(position).getTitle().contains("Fast Ends")) {
            showTzaitTaanitDialog();
        } else if (zmanim.get(position).getTitle().contains("Shabbat/Chag Ends")
                || zmanim.get(position).getTitle().contains("Shabbat Ends")
                || zmanim.get(position).getTitle().contains("Chag Ends")) {
            showTzaitShabbatDialog();
        } else if (zmanim.get(position).getTitle().contains("Rabbeinu Tam")) {
            showRTDialog();
        } else if (zmanim.get(position).getTitle().contains("Midnight")) {
            showChatzotLaylaDialog();
        }
    }

    private void checkEnglishZmanimForDialog(int position) {
        if (zmanim.get(position).getTitle().contains("Alot Hashachar")) {
            showDawnDialog();
        } else if (zmanim.get(position).getTitle().contains("Earliest Talit/Tefilin")) {
            showEarliestTalitTefilinDialog();
        } else if (zmanim.get(position).getTitle().contains("HaNetz")) {
            showSunriseDialog();
        } else if (zmanim.get(position).getTitle().contains("Achilat Chametz")) {
            showAchilatChametzDialog();
        } else if (zmanim.get(position).getTitle().contains("Biur Chametz")) {
            showBiurChametzDialog();
        } else if (zmanim.get(position).getTitle().contains("Shema MG\"A")) {
            showShmaMGADialog();
        } else if (zmanim.get(position).getTitle().contains("Shema GR\"A")) {
            showShmaGRADialog();
        } else if (zmanim.get(position).getTitle().contains("Birkat HaChamah")) {
            showBirchatHachamahDialog();
        } else if (zmanim.get(position).getTitle().contains("Berakhot Shema")) {
            showBrachotShmaDialog();
        } else if (zmanim.get(position).getTitle().contains("Ḥatzot Layla")) {
            showChatzotLaylaDialog();
        } else if (zmanim.get(position).getTitle().contains("Minḥa Gedola")) {
            showMinchaGedolaDialog();
        } else if (zmanim.get(position).getTitle().contains("Minḥa Ketana")) {
            showMinchaKetanaDialog();
        } else if (zmanim.get(position).getTitle().contains("Plag HaMinḥa")) {
            showPlagDialog();
        } else if (zmanim.get(position).getTitle().contains("Candle Lighting")) {
            showCandleLightingDialog();
        } else if (zmanim.get(position).getTitle().contains("Sheqi'a")) {
            showShkiaDialog();
        } else if (zmanim.get(position).getTitle().contains("Tzet Hakokhavim L'Ḥumra")) {
            showTzaitLChumraDialog();
        } else if (zmanim.get(position).getTitle().contains("Tzet Hakokhavim")) {
            showTzaitDialog();
        } else if (zmanim.get(position).getTitle().contains("Tzet Taanit L'Ḥumra")) {
            showTzaitTaanitLChumraDialog();
        } else if (zmanim.get(position).getTitle().contains("Tzet Taanit")) {
            showTzaitTaanitDialog();
        } else if (zmanim.get(position).getTitle().contains("Tzet Shabbat/Chag")
                || zmanim.get(position).getTitle().contains("Tzet Chag")
                || zmanim.get(position).getTitle().contains("Tzet Shabbat")) {
            showTzaitShabbatDialog();
        } else if (zmanim.get(position).getTitle().contains("Rabbenu Tam")) {
            showRTDialog();
        } else if (zmanim.get(position).getTitle().contains("Chatzot")) {
            showChatzotDialog();
        }
    }

    public static InputStream loadInputStreamFromAssetFile(Context context, String fileName){
        AssetManager am = context.getAssets();
        try {
            return am.open(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String loadContentFromFile(Context context, String path){
        String content;
        try {
            InputStream is = loadInputStreamFromAssetFile(context, path);
            int size = 0;
            if (is != null) {
                size = is.available();
            }
            byte[] buffer = new byte[size];
            if (is != null) {
                is.read(buffer);
            }
            if (is != null) {
                is.close();
            }
            content = new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return content;
    }

    private void showDawnDialog() {
        try {
            String alotMarkdown = loadContentFromFile(context, "aloth.md");

            Parser parser = Parser.builder().build();
            Node document = parser.parse(alotMarkdown);
            HtmlRenderer renderer = HtmlRenderer.builder().build();
            String alotHTML = renderer.render(document);

            if (locale.getDisplayLanguage(new Locale("en", "US")).equals("Hebrew")) {
                AlertDialog alertDialog =  dialogBuilder.setTitle("Dawn - עלות השחר - Alot HaShachar")
                        .setMessage(R.string.alot_dialog)
                        .create();
                alertDialog.show();
            } else {
                AlertDialog alertDialog =  dialogBuilder.setTitle("Dawn - עלות השחר - Alot HaShachar")
                        .setMessage(HtmlCompat.fromHtml(alotHTML, HtmlCompat.FROM_HTML_MODE_LEGACY))
                        .create();
                alertDialog.show();
            }
        } catch (Exception ignored) {}
    }

    private void showEarliestTalitTefilinDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Earliest Talit/Tefilin - טלית ותפילין - Misheyakir")
                .setMessage(R.string.misheyakir_dialog)
                .create();
        alertDialog.show();
    }

    private void showSunriseDialog() {
        dialogBuilder.setTitle("Sunrise - הנץ - HaNetz")
                .setMessage(R.string.sunrise_dialog)
                .setPositiveButton(R.string.setup_visible_sunrise, (dialog, which) ->
                        sSetupLauncher.launch(new Intent(context, SetupChooserActivity.class).putExtra("fromMenu",true)))
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
        AlertDialog alertDialog = dialogBuilder.setTitle("Eating Chametz - אכילת חמץ - Achilat Chametz")
                .setMessage(R.string.achilat_chametz_dialog)
                .create();
        alertDialog.show();
    }

    private void showBiurChametzDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Burning Chametz - ביעור חמץ - Biur Chametz")
                .setMessage(R.string.biur_chametz_dialog)
                .create();
        alertDialog.show();
    }

    private void showShmaMGADialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Latest time for Shema (MG\"A) - שמע מג\"א - Shema MG\"A")
                .setMessage(R.string.shmaMGAdialog)
                .create();
        alertDialog.show();
    }

    private void showShmaGRADialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Latest time for Shema (GR\"A) - שמע גר\"א - Shema GR\"A")
                .setMessage(R.string.shmaGRAdialog)
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
        AlertDialog alertDialog = dialogBuilder.setTitle("Berachot Shema - ברכות שמע")
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
        AlertDialog alertDialog = dialogBuilder.setTitle("Earliest Minḥa - מנחה גדולה - Minḥa Gedola")
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
                .setMessage(R.string.plag_dialog)
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
        AlertDialog alertDialog = dialogBuilder.setTitle("Nightfall (Stringent) - צאת הכוכבים לחומרא - Tzet Hakokhavim L'Chumra")
                .setMessage(R.string.tzait_lchumra_dialog)
                .create();
        alertDialog.show();
    }

    private void showTzaitTaanitDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Fast Ends - צאת תענית - Tzet Taanit")
                .setMessage(R.string.taanit_ends_dialog)
                .create();
        alertDialog.show();
    }

    private void showTzaitTaanitLChumraDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Fast Ends (Stringent) - צאת תענית לחומרא - Tzet Taanit L'Ḥumra")
                .setMessage(R.string.taanit_ends_lchumra_dialog)
                .create();
        alertDialog.show();
    }

    private void showTzaitShabbatDialog() {
        AlertDialog alertDialog = dialogBuilder.setTitle("Shabbat/Chag Ends - צאת שבת/חג - Tzet Shabbat/Chag")
                .setMessage(String.format(context.getString(R.string.tzait_shabbat_dialog), PreferenceManager.getDefaultSharedPreferences(context).getString("EndOfShabbatOffset", "40")))
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
        AlertDialog alertDialog = dialogBuilder.setTitle("Midnight - חצות לילה - Ḥatzot Layla")
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
                .setPositiveButton(context.getString(R.string.setup_elevation), (dialog, which) ->
                        sSetupLauncher.launch(new Intent(context, SetupElevationActivity.class).putExtra("fromMenu",true)))
                .create();
        alertDialog.show();
        resetDialogBuilder();
    }

    private void showTekufaDialog() {
        try {
            String tekufaMarkdownEN = loadContentFromFile(context, "tekufot-en.md");
            String tekufaMarkdownHB = loadContentFromFile(context, "tekufa-hb.md");

            Parser parser = Parser.builder().build();
            HtmlRenderer renderer = HtmlRenderer.builder().build();

            Node documentEN = parser.parse(tekufaMarkdownEN);
            String tekufaHTMLEnglish = renderer.render(documentEN);

            Node documentHB = parser.parse(tekufaMarkdownHB);
            String tekufaHTMLHebrew = renderer.render(documentHB);

            if (locale.getDisplayLanguage(new Locale("en", "US")).equals("Hebrew")) {
                AlertDialog alertDialog = dialogBuilder.setTitle("Tekufa - Season - תקופה")
                        .setMessage(HtmlCompat.fromHtml(tekufaHTMLHebrew, HtmlCompat.FROM_HTML_MODE_LEGACY))
                        .create();
                alertDialog.show();
            } else {
                AlertDialog alertDialog = dialogBuilder.setTitle("Tekufa - Season - תקופה")
                        .setMessage(HtmlCompat.fromHtml(tekufaHTMLEnglish, HtmlCompat.FROM_HTML_MODE_LEGACY))
                        .create();
                alertDialog.show();
            }
        } catch (Exception ignored) {}
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
}
