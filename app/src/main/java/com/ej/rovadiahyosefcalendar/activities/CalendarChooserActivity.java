package com.ej.rovadiahyosefcalendar.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.ej.rovadiahyosefcalendar.R;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.Locale;

public class CalendarChooserActivity extends AppCompatActivity {

    private SharedPreferences mSettingsPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String calendarDifference;
        if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
            calendarDifference = "\n" +
                    "האפליקציה יכולה להציג שני לוחות שנה נפרדים. בשנת 1990, רבי עובדיה יוסף זצ\"ל התחיל פרויקט ליצירת לוח זמנים על פי ההלכות והמנהגים שלו. יחד עם רבי שלמה בניזרי ורבי אשר דרשן, יצר לוח זמנים בשם \"לוח המאור אור החיים\". רבי עובדיה עצמו השגיח על יצירת הלוח והשתמש בו עד לפטירתו. האפליקציה עברה על כל זמני לוח אור החיים ואישרה את דיוקם.\n" +
                    "\n" +
                    "יש גם אפשרות להשתמש בלוח זמנים שנקרא \"עמודי הוראה\", הנוצר על ידי רבי ליאור דהן שליט\"א. רבי ליאור דהן הוא סופר ספר בשם \"עמודי הוראה\", וכיושב בארצות הברית, הוא יצר לוח זמנים על פי עמדתו של רבי עובדיה. לוחו דומה ללוח אור החיים עם הבדלים קטנים; בהתאם להלכה ברורה בסימן 261 הלכה 13, הוא מתאים את הדקות זמניות בהתאם לקווי הרוחב של המשתמש. לדוגמה, בישראל, עלות השחר הוא 72 דקות זמניות לפני הזריחה, אך בניו יורק יעמידו כ-80 דקות זמניות, ובצרפת זה יהיה יותר. כדאי לציין כי ילקוט יוסף אינו נראה נוטה להסכמה לחישובים אלה (ראה עין יצחק חלק 3 עמוד 230). כדאי גם לציין כי לוח עמודי הוראה מציג את פלג המנחה על פי ההלכה ברורה, וגם צאת הכוכבים לחומרה. צאת הכוכבים לחומרה משמש במקרים מסוימים, כמו למתי שהצום נגמר. שאלתי את רבי בניזרי למה אין מופיעה הזכרת זמן צאת הכוכבים לחומרה בלוח אור החיים, והוא ענה שהלוח פשוט אומר שהצום נגמרת בצאת הכוכבים והזמן יכול להתייחס לשני הזמנים.\n" +
                    "\n" +
                    "חשוב לציין כי שני הלוחות ישתמשו בקווי רוחב ואורך שאתה סופק כדי לחשב את זמני היום. ההבחנה בין שני הלוחות היא בגלל הדקות זמניות הנוספות המתווספות באזורים יותר צפוניים/דרומיים. לוח עמודי הוראה רק לשימוש מחוץ לישראל, אך יש רבנים שחוזרים גם על כך כי גם תוכניות החישוב של לוח אור החיים יכולות לשמש מחוץ לישראל. לכן, השארתי למשתמש לבחור איזה לוח הוא רוצה לעקוב אחרי. יש רבנים בשני הצדדים, אז יש לך על מה לסמוך בכל מקרה.";
        } else {
            calendarDifference = "This app has the capability to display two separate calendars. In 1990, Rabbi Ovadiah Yosef ZT\"L started a project to create a zmanim calendar according to his halachot and minhagim. Rabbi Ovadiah sat down with Rabbi Shlomo Benizri and Rabbi Asher Darshan and created a zmanim calendar called \"Luach HaMaor Ohr HaChaim\". Rabbi Ovadiah himself oversaw this calendar's creation and used it until he passed. This app has reverse engineered all the zmanim of the Ohr Hachaim calendar and confirmed that they are accurate.\n\nThere is also an option to use the Amudei Horaah calendar created by Rabbi Leeor Dahan Shlita. Rabbi Leeor Dahan is the author of the popular sefer \"Amudei Horaah\", and as he lives in America, he has set out to create his own calendar according to Rabbi Ovadiah's views. His calendar is similar to the Ohr HaChaim calendar with minor differences, based on the Halacha Berurah in Siman 261 Halacha 13, he adjusts the seasonal minutes based on the latitude of the user. For example, in Israel Alot/Dawn is 72 seasonal minutes before Sunrise, however, in New York, it would come out to around 80 seasonal minutes, and in France it would be even more. It should be noted that the Yalkut Yosef does not seem to agree to these calculations (See Eyin Yitzchak Chelek 3 Amud 230). It should also be noted that the Amudei Horaah calendar shows plag hamincha according to the Halacha Berurah as well as tzeit/nightfall l'chumra. Tzeit l'chumra is used in certain scenarios like for when a fast ends. I asked Rabbi Benizri why there was no mention of this stringent tzeit in the Ohr HaChaim calendar and he answered that the calendar just says that the fasts end at tzeit and it can refer to both times.\n\nPlease note that both calendars will use the latitude and longitude you provide to calculate the zmanim. The difference between the two calendars is because of the additional seasonal minutes added in more northern/southern areas. The Amudei Horaah calendar is only to be used outside of Israel, however, many rabbanim also hold that the Ohr HaChaim calendar's calculations CAN be used outside of Israel as well. Therefore, I left it up to the user to choose which calendar they want to follow. There are rabbanim on both sides, so you have on what to rely on either way.";
        }
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_calendar_chooser);

        MaterialToolbar materialToolbar = findViewById(R.id.topAppBar);
        if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
            materialToolbar.setSubtitle("");
        }
        materialToolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.help) {
                new AlertDialog.Builder(this, R.style.alertDialog)
                        .setTitle(R.string.help_using_this_app)
                        .setPositiveButton(R.string.ok, null)
                        .setMessage(R.string.helper_text)
                        .show();
                return true;
            } else if (id == R.id.restart) {
                startActivity(new Intent(this, FullSetupActivity.class));
                finish();
                return true;
            }
            return false;
        });

        LinearLayout layout = findViewById(R.id.calendar_buttons);
        float screenWidth = getResources().getDisplayMetrics().widthPixels / getResources().getDisplayMetrics().density;
        if (screenWidth < 400) {
            layout.setOrientation(LinearLayout.VERTICAL);
        }
        mSettingsPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        Button ohrHachaim = findViewById(R.id.ohrHachaimButton);
        Button amudeiHoraah = findViewById(R.id.amudeiHoraahButton);

        ohrHachaim.setOnClickListener(v -> saveInfoAndFinish(false));
        amudeiHoraah.setOnClickListener(v -> saveInfoAndFinish(true));

        Button difference = findViewById(R.id.calendar_chooser_difference);
        difference.setOnClickListener(v -> new AlertDialog.Builder(this, R.style.alertDialog)
                .setTitle(R.string.what_s_the_difference)
                .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                .setMessage(calendarDifference)
                .show());

        Button skip = findViewById(R.id.calendar_chooser_skip);
        skip.setOnClickListener(v -> finish());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void saveInfoAndFinish(boolean b) {
        mSettingsPreferences.edit().putBoolean("LuachAmudeiHoraah", b).putBoolean("useElevation", !b).apply();
        finish();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }

    @Override
    public void onUserInteraction() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        super.onUserInteraction();
    }
}