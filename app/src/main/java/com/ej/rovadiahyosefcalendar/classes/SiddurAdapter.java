package com.ej.rovadiahyosefcalendar.classes;

import static android.content.Context.SENSOR_SERVICE;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.activities.SiddurViewActivity;

import java.util.ArrayList;

public class SiddurAdapter extends ArrayAdapter<String> {

    private final Context context;
    private final ArrayList<HighlightString> siddur;
    private final JewishDateInfo jewishDateInfo;
    private int textSize;
    private float[] gravity = new float[3];
    private float[] geomagnetic = new float[3];
    private float azimuth;
    private float currentDegree = 0f;

    public SiddurAdapter(Context context, ArrayList<HighlightString> siddur, int textSize, JewishDateInfo jewishDateInfo) {
        super(context, 0);
        this.context = context;
        this.siddur = siddur;
        this.textSize = textSize;
        this.jewishDateInfo = jewishDateInfo;
    }

    public void setTextSize(int size) {
        textSize = size;
    }

    @Override
    public int getCount() {
        return siddur.size();
    }

    @Override
    public String getItem(int position) {
        return siddur.get(position).toString();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.text_view, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.textView = convertView.findViewById(R.id.textView);
            viewHolder.imageView = convertView.findViewById(R.id.imageView);
            viewHolder.line = convertView.findViewById(R.id.line);
            convertView.setTag(viewHolder);
            viewHolder.defaultTextColor = viewHolder.textView.getCurrentTextColor();
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        String itemText = siddur.get(position).toString();
        viewHolder.textView.setText(itemText);
        viewHolder.textView.setTextSize(textSize);

        if (siddur.get(position).shouldBeHighlighted()) {
            convertView.setBackgroundColor(Color.YELLOW);
            viewHolder.textView.setTextColor(context.getColor(R.color.black));
        } else {
            convertView.setBackgroundColor(Color.TRANSPARENT);
            viewHolder.textView.setTextColor(viewHolder.defaultTextColor);
        }

        if (siddur.get(position).toString().equals("[break here]")) {
            viewHolder.textView.setVisibility(View.GONE);
            viewHolder.line.setVisibility(View.VISIBLE);
        } else {
            viewHolder.textView.setVisibility(View.VISIBLE);
            viewHolder.line.setVisibility(View.GONE);
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
        viewHolder.textView.setTypeface(Typeface.createFromAsset(context.getAssets(),"TaameyFrankCLM-Bold.ttf"), Typeface.BOLD);

        if (siddur.get(position).toString().endsWith("לַמְנַצֵּ֥חַ בִּנְגִינֹ֗ת מִזְמ֥וֹר שִֽׁיר׃ אֱֽלֹהִ֗ים יְחׇנֵּ֥נוּ וִיבָרְכֵ֑נוּ יָ֤אֵֽר פָּנָ֖יו אִתָּ֣נוּ סֶֽלָה׃ לָדַ֣עַת בָּאָ֣רֶץ דַּרְכֶּ֑ךָ בְּכׇל־גּ֝וֹיִ֗ם יְשׁוּעָתֶֽךָ׃ יוֹד֖וּךָ עַמִּ֥ים ׀ אֱלֹהִ֑ים י֝וֹד֗וּךָ עַמִּ֥ים כֻּלָּֽם׃ יִ֥שְׂמְח֥וּ וִירַנְּנ֗וּ לְאֻ֫מִּ֥ים כִּֽי־תִשְׁפֹּ֣ט עַמִּ֣ים מִישֹׁ֑ר וּלְאֻמִּ֓ים ׀ בָּאָ֖רֶץ תַּנְחֵ֣ם סֶֽלָה׃ יוֹד֖וּךָ עַמִּ֥ים ׀ אֱלֹהִ֑ים י֝וֹד֗וּךָ עַמִּ֥ים כֻּלָּֽם׃ אֶ֭רֶץ נָתְנָ֣ה יְבוּלָ֑הּ יְ֝בָרְכֵ֗נוּ אֱלֹהִ֥ים אֱלֹהֵֽינוּ׃ יְבָרְכֵ֥נוּ אֱלֹהִ֑ים וְיִֽירְא֥וּ א֝וֹת֗וֹ כׇּל־אַפְסֵי־אָֽרֶץ׃")) {
            viewHolder.imageView.setVisibility(View.VISIBLE);
//            if ((context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
//                viewHolder.imageView.setImageResource(R.drawable.dark_menorah);
//            } else {
//                viewHolder.imageView.setImageResource(R.drawable.light_menorah);
//            }
            viewHolder.imageView.setImageResource(R.drawable.menorah);//temporary
            viewHolder.imageView.setAdjustViewBounds(true);
        } else if (siddur.get(position).toString().equals("(Use this compass to help you find which direction South is in. Do not hold your phone straight up or place it on a table, hold it normally.) עזר לך למצוא את הכיוון הדרומי באמצעות המצפן הזה. אל תחזיק את הטלפון שלך בצורה ישרה למעלה או תנה אותו על שולחן, תחזיק אותו בצורה רגילה.:")) {
            convertView.setBackgroundColor(Color.BLACK);
            viewHolder.textView.setTextColor(Color.YELLOW);
            viewHolder.imageView.setVisibility(View.VISIBLE);
            viewHolder.imageView.setImageResource(R.drawable.compass);
            SensorManager sensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
            Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Sensor magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

            if (accelerometer != null) {
                sensorManager.registerListener(new SensorEventListener() {
                    @Override
                    public void onSensorChanged(SensorEvent event) {
                        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                            gravity = event.values;
                        }

                        float[] rotationMatrix = new float[9];
                        if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                            float[] orientationValues = new float[3];
                            SensorManager.getOrientation(rotationMatrix, orientationValues);

                            azimuth = (float) Math.toDegrees(orientationValues[0]);
                            azimuth = (azimuth + 360) % 360;

                            RotateAnimation ra = new RotateAnimation(
                                    currentDegree,
                                    -azimuth,
                                    Animation.RELATIVE_TO_SELF, 0.5f,
                                    Animation.RELATIVE_TO_SELF, 0.5f
                            );

                            ra.setDuration(250);
                            ra.setFillAfter(true);

                            //viewHolder.imageView.startAnimation(ra);
                            currentDegree = -azimuth;
                        }
                    }

                    @Override
                    public void onAccuracyChanged(Sensor sensor, int accuracy) {
                    }
                }, accelerometer, SensorManager.SENSOR_DELAY_UI);
            }
            if (magnetometer != null) {
                sensorManager.registerListener(new SensorEventListener() {
                    @Override
                    public void onSensorChanged(SensorEvent event) {
                        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                            geomagnetic = event.values;
                        }

                        float[] rotationMatrix = new float[9];
                        if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                            float[] orientationValues = new float[3];
                            SensorManager.getOrientation(rotationMatrix, orientationValues);

                            azimuth = (float) Math.toDegrees(orientationValues[0]);
                            azimuth = (azimuth + 360) % 360;

                            RotateAnimation ra = new RotateAnimation(
                                    currentDegree,
                                    -azimuth,
                                    Animation.RELATIVE_TO_SELF, 0.5f,
                                    Animation.RELATIVE_TO_SELF, 0.5f
                            );

                            ra.setDuration(250);
                            ra.setFillAfter(true);

                            viewHolder.imageView.startAnimation(ra);
                            currentDegree = -azimuth;
                        }
                    }

                    @Override
                    public void onAccuracyChanged(Sensor sensor, int accuracy) {

                    }
                }, magnetometer, SensorManager.SENSOR_DELAY_UI);
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

    static class ViewHolder {
        TextView textView;
        ImageView imageView;
        View line;
        int defaultTextColor; // Store the default color
    }
}
