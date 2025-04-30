package com.ej.rovadiahyosefcalendar.activities;

import static com.ej.rovadiahyosefcalendar.activities.MainFragmentManager.SHARED_PREF;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ej.rovadiahyosefcalendar.R;
import com.ej.rovadiahyosefcalendar.classes.ExceptionHandler;

public class ShowErrorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
        setContentView(R.layout.activity_show_error);
        TextView error = findViewById(R.id.error);
        error.setText(getIntent().getStringExtra("error"));
        if (getIntent().getBooleanExtra("isNotifDebug", false)) {
            Button deleteLogs = findViewById(R.id.deleteLogs);
            deleteLogs.setVisibility(View.VISIBLE);
            deleteLogs.setOnClickListener(v -> {
                error.setText("");
                getSharedPreferences(SHARED_PREF, MODE_PRIVATE).edit().putString("debugNotifs", "").apply();
            });
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.error_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}