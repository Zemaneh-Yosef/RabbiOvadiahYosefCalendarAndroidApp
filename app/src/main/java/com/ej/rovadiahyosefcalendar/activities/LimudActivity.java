package com.ej.rovadiahyosefcalendar.activities;

import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.ej.rovadiahyosefcalendar.R;
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class LimudActivity extends AppCompatActivity {

    /**
     * These calendars are used to know when daf/yerushalmi yomi started
     */
    private final static Calendar dafYomiStartDate = new GregorianCalendar(1923, Calendar.SEPTEMBER, 11);
    private final static Calendar dafYomiYerushalmiStartDate = new GregorianCalendar(1980, Calendar.FEBRUARY, 2);
    private final HebrewDateFormatter mHebrewDateFormatter = new HebrewDateFormatter();
    private RecyclerView mMainRecyclerView;
    private Button mNextDate;
    private Button mPreviousDate;
    private Button mCalendarButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_limud);

    }
}