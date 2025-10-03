package com.ej.rovadiahyosefcalendar.classes;

import static android.content.Context.SENSOR_SERVICE;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.activities.SiddurViewActivity;

import java.util.ArrayList;
import java.util.LinkedList;

public class SiddurAdapter extends ArrayAdapter<String> implements SensorEventListener {

    private final Context context;
    private final ArrayList<HighlightString> siddur;
    private final JewishDateInfo jewishDateInfo;
    private int textSize;
    private boolean isJustified;
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];
    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    private final LinkedList<Double> m_window = new LinkedList<>();
    private float currentDegree = 0f;
    private ImageView compass;

    public SiddurAdapter(Context context, ArrayList<HighlightString> siddur, int textSize, boolean isJustified, JewishDateInfo jewishDateInfo) {
        super(context, 0);
        this.context = context;
        this.siddur = siddur;
        this.textSize = textSize;
        this.isJustified = isJustified;
        this.jewishDateInfo = jewishDateInfo;
    }

    public void setTextSize(int size) {
        textSize = size;
    }

    public void setIsJustified(boolean isJustified) {
        this.isJustified = isJustified;
    }

    @Override
    public int getCount() {
        return siddur.size();
    }

    @Override
    public String getItem(int position) {
        return siddur.get(position).toString();
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.text_view, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.textView = convertView.findViewById(R.id.textView);
            viewHolder.imageView = convertView.findViewById(R.id.imageView);
            viewHolder.line = convertView.findViewById(R.id.line);
            viewHolder.line.setBackgroundColor(viewHolder.textView.getCurrentTextColor());
            convertView.setTag(viewHolder);
            viewHolder.defaultTextColor = viewHolder.textView.getCurrentTextColor();
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        String itemText = siddur.get(position).toString();
        viewHolder.textView.setText(itemText);
        viewHolder.textView.setTextDirection(View.TEXT_DIRECTION_RTL);
        viewHolder.textView.setTextSize(textSize);
        viewHolder.textView.setJustify(isJustified);
        if (siddur.get(position).shouldBeHighlighted()) {
            if ((context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                convertView.setBackgroundColor(context.getColor(R.color.goldenrod));
            } else {// light mode
                convertView.setBackgroundColor(context.getColor(R.color.mainly_BLUE));
            }
            viewHolder.textView.setTextColor(context.getColor(R.color.black));
        } else {
            convertView.setBackgroundColor(Color.TRANSPARENT);
            viewHolder.textView.setTextColor(viewHolder.defaultTextColor);
        }

        viewHolder.textView.setOnClickListener(l -> {
            if (siddur.get(position).toString().equals("Open Sefaria Siddur/פתח את סידור ספריה")) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.sefaria.org/Siddur_Edot_HaMizrach?tab=contents"));
                context.startActivity(browserIntent);
            }
            if (siddur.get(position).toString().equals("Mussaf is said here, press here to go to Mussaf")
                    || siddur.get(position).toString().equals("מוסף אומרים כאן, לחץ כאן כדי להמשיך למוסף")) {
                context.startActivity(new Intent(context, SiddurViewActivity.class)
                        .putExtra("prayer", "מוסף")
                        .putExtra("JewishDay", jewishDateInfo.getJewishCalendar().getJewishDayOfMonth())
                        .putExtra("JewishMonth", jewishDateInfo.getJewishCalendar().getJewishMonth())
                        .putExtra("JewishYear", jewishDateInfo.getJewishCalendar().getJewishYear())
                );
            }
        });
        switch (PreferenceManager.getDefaultSharedPreferences(context).getString("font", "Guttman Keren")) {
            case "Guttman Keren":
                viewHolder.textView.setTypeface(Typeface.createFromAsset(context.getAssets(), "Guttman Keren.ttf"), Typeface.NORMAL);
                viewHolder.textView.setLineSpacing(4f, 1.2f);
                break;
            case "Taamey Frank":
                viewHolder.textView.setTypeface(Typeface.createFromAsset(context.getAssets(), "Taamey_D.ttf"), Typeface.NORMAL);
                break;
            default:
                break;
        }

        if (siddur.get(position).isCategory()) {
            viewHolder.textView.setTypeface(Typeface.createFromAsset(context.getAssets(), "MANTB 2.ttf"), Typeface.NORMAL);
            viewHolder.textView.setGravity(Gravity.CENTER);
            viewHolder.textView.setTextSize(textSize + 8);
        } else {
            viewHolder.textView.setGravity(Gravity.NO_GRAVITY);
        }

        if (siddur.get(position).toString().equals("[break here]")) {
            viewHolder.textView.setVisibility(View.GONE);
            viewHolder.line.setVisibility(View.VISIBLE);
        } else if (siddur.get(position).isInfo()) {
            viewHolder.textView.setBackgroundColor(Color.DKGRAY);
            if ((context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO) {// light mode
                viewHolder.textView.setTextColor(Color.BLACK);
            }
            viewHolder.textView.setVisibility(View.VISIBLE);
            String summary = "▲ " + siddur.get(position).getSummary();
            viewHolder.textView.setText(summary);
            View.OnClickListener onClickListener = l -> {
                viewHolder.textView.wasClicked = !viewHolder.textView.wasClicked;
                if (viewHolder.textView.wasClicked) {
                    String full = "▼ " + siddur.get(position).getSummary() + siddur.get(position).toString();
                    viewHolder.textView.setText(full);
                } else {
                    viewHolder.textView.setText(summary);
                }
            };
            viewHolder.textView.setOnClickListener(onClickListener);
        } else {
            viewHolder.textView.setVisibility(View.VISIBLE);
            viewHolder.line.setVisibility(View.GONE);
            viewHolder.textView.setBackgroundColor(Color.TRANSPARENT);
            if ((context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO) {// light mode
                if (siddur.get(position).shouldBeHighlighted()) {
                    viewHolder.textView.setTextColor(Color.BLACK);
                } else {
                    viewHolder.textView.setTextColor(viewHolder.defaultTextColor);
                }
            }
        }

        if (siddur.get(position).toString().endsWith("לַמְנַצֵּ֥חַ בִּנְגִינֹ֗ת מִזְמ֥וֹר שִֽׁיר׃ אֱֽלֹהִ֗ים יְחׇנֵּ֥נוּ וִיבָרְכֵ֑נוּ יָ֤אֵֽר פָּנָ֖יו אִתָּ֣נוּ סֶֽלָה׃ לָדַ֣עַת בָּאָ֣רֶץ דַּרְכֶּ֑ךָ בְּכׇל־גּ֝וֹיִ֗ם יְשׁוּעָתֶֽךָ׃ יוֹד֖וּךָ עַמִּ֥ים ׀ אֱלֹהִ֑ים י֝וֹד֗וּךָ עַמִּ֥ים כֻּלָּֽם׃ יִ֥שְׂמְח֥וּ וִירַנְּנ֗וּ לְאֻ֫מִּ֥ים כִּֽי־תִשְׁפֹּ֣ט עַמִּ֣ים מִישֹׁ֑ר וּלְאֻמִּ֓ים ׀ בָּאָ֖רֶץ תַּנְחֵ֣ם סֶֽלָה׃ יוֹד֖וּךָ עַמִּ֥ים ׀ אֱלֹהִ֑ים י֝וֹד֗וּךָ עַמִּ֥ים כֻּלָּֽם׃ אֶ֭רֶץ נָתְנָ֣ה יְבוּלָ֑הּ יְ֝בָרְכֵ֗נוּ אֱלֹהִ֥ים אֱלֹהֵֽינוּ׃ יְבָרְכֵ֥נוּ אֱלֹהִ֑ים וְיִֽירְא֥וּ א֝וֹת֗וֹ כׇּל־אַפְסֵי־אָֽרֶץ׃")) {
            viewHolder.imageView.setVisibility(View.VISIBLE);
            viewHolder.imageView.setImageResource(R.drawable.menorah);//temporary
            viewHolder.imageView.setAdjustViewBounds(true);
        } else if (siddur.get(position).toString().equals("(Use this compass to help you find which direction South is in. Do not hold your phone straight up or place it on a table, hold it normally.) עזר לך למצוא את הכיוון הדרומי באמצעות המצפן הזה. אל תחזיק את הטלפון שלך בצורה ישרה למעלה או תנה אותו על שולחן, תחזיק אותו בצורה רגילה.:")) {
            convertView.setBackgroundColor(Color.BLACK);
            viewHolder.textView.setTextColor(Color.YELLOW);
            viewHolder.imageView.setVisibility(View.VISIBLE);
            viewHolder.imageView.setImageResource(R.drawable.compass);
            compass = viewHolder.imageView;
            SensorManager sensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
            Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Sensor magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

            if (accelerometer != null || magnetometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
                sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
            }

            if (accelerometer == null && magnetometer == null) {
                viewHolder.textView.setText("");
                viewHolder.imageView.setImageResource(0);
                viewHolder.imageView.setVisibility(View.GONE);
            }
        } else {
            viewHolder.imageView.setImageResource(0);
            viewHolder.imageView.setVisibility(View.GONE);
        }

        return convertView;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null) {
            return;
        }
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.length);
        }

        if (SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)) {
            SensorManager.getOrientation(rotationMatrix, orientationAngles);

            float smoothedAzimuthDegrees = filtrate(orientationAngles[0] / (2 * Math.PI) * 360);

            RotateAnimation ra = new RotateAnimation(
                    currentDegree,
                    -smoothedAzimuthDegrees,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
            );

            ra.setDuration(100);
            compass.startAnimation(ra);
            currentDegree = -smoothedAzimuthDegrees;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private float filtrate(Double value) {//Credit to Rafael Sheink
        m_window.add(value);
        if (m_window.size() > 50) {
            m_window.remove();
        }

        double sumx = 0.0;
        double sumy = 0.0;
        Object[] arr = m_window.toArray();
        for (Object anArr : arr) {
            if (anArr instanceof Double) {
                sumx += Math.cos((Double) anArr / 360 * (2 * Math.PI));
                sumy += Math.sin((Double) anArr / 360 * (2 * Math.PI));
            }
        }

        double avgx = sumx / m_window.size();
        double avgy = sumy / m_window.size();

        double temp = Math.atan2(avgy, avgx) / (2 * Math.PI) * 360;
        if (temp == 0.0) {
            return 0.0f;
        }

        if (temp > 0) {
            return (float) temp;
        } else {
            return (((float) temp) + 360) % 360;
        }
    }

    static class ViewHolder {
        JustifyTextView textView;
        ImageView imageView;
        View line;
        int defaultTextColor; // Store the default color
    }
}
