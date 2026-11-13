package com.example.communitysafetyapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        try {
            Log.d("FileDebug", "CHECKS: " + FileHelper.loadChecks(this));
            Log.d("FileDebug", "CONTACTS: " + FileHelper.loadContacts(this));
            Log.d("FileDebug", "SETTINGS: " + FileHelper.loadSettings(this));
        } catch (Exception e) {
            Log.e("FileDebug", "Could not read files", e);
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button buttonViewChecks = findViewById(R.id.btnViewChecks);
      buttonViewChecks.setOnClickListener(v ->{
            Intent i = new Intent(
                    MainActivity.this,
                    ActiveChecksActivity.class
            );
            startActivity(i);
        });

        Button buttonManagePeople = findViewById(R.id.btnManagePeople);
        buttonManagePeople.setOnClickListener(v ->{
            Intent i = new Intent(
                    MainActivity.this,
                    PeopleActivity.class
            );
            startActivity(i);
        });

        Button buttonExit = findViewById(R.id.btnExit);
        //buttonViewChecks.setOnClickListener(v -> finishAffinity);
    }
}