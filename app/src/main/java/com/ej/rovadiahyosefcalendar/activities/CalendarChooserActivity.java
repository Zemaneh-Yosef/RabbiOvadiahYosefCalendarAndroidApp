package com.ej.rovadiahyosefcalendar.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;

import com.ej.rovadiahyosefcalendar.R;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;

public class CalendarChooserActivity extends AppCompatActivity {

    private SharedPreferences mSettingsPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        String calendarDifference;
        if (Locale.getDefault().getDisplayLanguage(new Locale("en","US")).equals("Hebrew")) {
            calendarDifference = "לאפליקציה זו יש את היכולת להציג שני לוחות שנה נפרדים. בשנת 1990 פתח הרב עובדיה יוסף זצ\"ל בפרויקט יצירת לוח זמנים על פי הלכותיו ומנחגים. לכן ישב הרב עובדיה עם הרב שלמה בניזרי והרב אשר דרשן ויצר לוח זמנים בשם \"לוח המאור אור\". החיים\". הרב עובדיה עצמו פיקח על יצירת לוח השנה הזה והשתמש בו עד שעבר. הקוד של אותו לוח שנה אינו זמין לציבור, עם זאת, הם מסבירים איך עושים את החישובים בהקדמתם והאפליקציה הזו ביצעה הנדסה הפוכה של כל זמנים מלוח \"אור החיים\" ואישרו שהם מדויקים.\n\nיש גם אפשרות להשתמש בלוח השנה של עמודי חור\"ה שיצר הגאון רבי ליאור דהן שליט\"א. הגאון רבי ליאור דהן הוא מחבר הספר הפופולרי \"עמודי מורה\" , וכיוון שהוא מתגורר באמריקה, הוא יצא ליצור לוח שנה משלו על פי השקפותיו של הרב עובדיה. לוח השנה שלו דומה ללוח אור החיים עם הבדלים קלים, אולם על סמך ההלכה ברורה, הוא מתאים את אלוות וצייט על דרגות המיקום של המשתמש. הסבר מעמיק זמין באפליקציה. הזמנים היחידים המופיעים בלוח השנה של עמודי הורה שאינם מופיעים בלוח \"אור החיים\" נוהגים להדביק את המנחה לפי ההלכה ברורה וזמן לחומרה. אני מניח שההלכה ברורה לא הוצגה בלוח השנה של אור החיים כי ההלכה הייתה חדשה למדי באותה תקופה. Tzeit l'chumra הוא 20 זמניות דקות מתורגמות למעלות ומשמש בתרחישים מסוימים כמו מתי צום מסתיים. הרב בניזרי אמר לי שזה 20 דקות קבועות אחרי השקיעה. שאלתי את הרב בניזרי מדוע אין אזכור של צייט המחמיר הזה בלוח \"אור החיים\" והוא ענה שבלוח פשוט כתוב שהצומות מסתיימים בצייט וזה מתייחס לשני הזמנים.\n\nיש לציין ששני לוחות השנה השתמש בקו הרוחב ובקו האורך שאתה מספק כדי לחשב את הזמנים. יש רק הבדל של כמה דקות בין שני לוחות השנה בגלל התארים הנוספים. כמו כן, הרב דהן מסכים שאתה צריך להשתמש בלוח השנה של אור החיים בישראל. לוח השנה של עמודי הורה מיועד רק מחוץ לישראל. אולם הרב שלמה בניזרי שליט\"א סבור כי ניתן להשתמש בלוח \"אור החיים\" מחוץ לישראל, כמו כן הרב עובדיה מעולם לא הבדיל.";
        } else {
            calendarDifference = "This app has the capability to display two separate calendars. In 1990, Rabbi Ovadiah Yosef ZT\"L started a project to create a zmanim calendar according to his halachot and minhagim. Therefore, Rabbi Ovadiah sat down with Rabbi Shlomo Benizri and Rabbi Asher Darshan and created a zmanim calendar called \"Luach HaMaor Ohr HaChaim\". Rabbi Ovadiah himself oversaw this calendar's creation and used it until he passed. The code for that calendar is not available to the public, however, they explain how to do the calculations in their introduction and this app has reverse engineered all the zmanim of the Ohr Hachaim calendar and confirmed that they are accurate.\n\nThere is also an option to use the Amudei Horaah calendar created by Rabbi Leeor Dahan Shlita. Rabbi Leeor Dahan is the author of the popular sefer \"Amudei Horaah\", and as he lives in America, he has set out to create his own calendar according to Rabbi Ovadiah's views. His calendar is similar to the Ohr HaChaim calendar with minor differences, however, based on the Halacha Berurah, he adjusts Alot and Tzeit based on the degrees of the location of the user. In depth explanation is available in the app. The only zmanim that are shown in the Amudei Horaah calendar but are not shown in the Ohr HaChaim calendar are plag hamincha according to the Halacha Berurah and tzeit l'chumra. I assume the Halacha Berurah was not shown in the Ohr HaChaim calendar because the Halacha Berurah was fairly new at the time. Tzeit l'chumra is 20 zmaniyot minutes translated as degrees and is used in certain scenarios like for when a fast ends. Rabbi Benizri has told me that this zman is 20 regular minutes after sunset. I asked Rabbi Benizri why there was no mention of this stringent tzeit in the Ohr HaChaim calendar and he answered that the calendar just says that the fasts end at tzeit and it can refer to both times.\n\nIt should be noted that both calendars will use the latitude and longitude you provide to calculate the zmanim. There is just a difference of a few minutes between the two calendars because of the additional degrees added. Also, Rabbi Dahan agrees that you should be using the Ohr HaChaim calendar IN ISRAEL. The Amudei Horaah calendar is only to be used outside of israel. Rabbi Shlomo Benizri Shlita however holds that the Ohr HaChaim calendar CAN be used outside Israel as well as Rabbi Ovadiah never differentiated.";
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_chooser);
        Objects.requireNonNull(getSupportActionBar()).setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.action_bar_custom);//center the title
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
        difference.setOnClickListener(v -> new AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_DayNight)
                .setTitle(R.string.what_s_the_difference)
                .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                .setMessage(calendarDifference)
                .show());

        Button skip = findViewById(R.id.calendar_chooser_skip);
        skip.setOnClickListener(v -> finish());
    }

    private void saveInfoAndFinish(boolean b) {
        mSettingsPreferences.edit().putBoolean("LuachAmudeiHoraah", b).apply();
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.setup_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull @NotNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.help) {
            new AlertDialog.Builder(this, androidx.appcompat.R.style.Theme_AppCompat_DayNight)
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
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}