package com.example.communitysafetyapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

//Home screen: Buttons opens Active checks screen, People screen, or exit button to leave app
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //View Checks buttons opens the Active Checks screen
        Button buttonViewChecks = findViewById(R.id.btnViewChecks);
      buttonViewChecks.setOnClickListener(v ->{
            Intent i = new Intent(
                    MainActivity.this,
                    ActiveChecksActivity.class
            );
            startActivity(i);
        });

        //Manage People button opens the People screen
        Button buttonManagePeople = findViewById(R.id.btnManagePeople);
        buttonManagePeople.setOnClickListener(v ->{
            Intent i = new Intent(
                    MainActivity.this,
                    PeopleActivity.class
            );
            startActivity(i);
        });

        //Exit button leave the app
        Button buttonExit = findViewById(R.id.btnExit);
        buttonExit.setOnClickListener(v -> finishAffinity());
    }
}