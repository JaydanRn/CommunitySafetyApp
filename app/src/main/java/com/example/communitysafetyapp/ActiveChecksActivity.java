package com.example.communitysafetyapp;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;

public class ActiveChecksActivity extends AppCompatActivity {

    private static final String TAG = "ActiveChecksActivity";
    private static final String CHECKS_FILE = "checks.dat";

    private ListView lvChecks;
    private TextView tvChecksEmpty;
    private Button btnStartCheck;

    private ArrayList<Check> checks = new ArrayList<>();
    private ArrayAdapter<Check> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_active_checks);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        lvChecks       = findViewById(R.id.lvChecks);
        tvChecksEmpty  = findViewById(R.id.tvChecksEmpty);
        btnStartCheck  = findViewById(R.id.btnStartCheck);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, checks);
        lvChecks.setAdapter(adapter);

        btnStartCheck.setOnClickListener(v -> StartCheckDialogFragment.newInstance().show(getSupportFragmentManager(), "startCheck"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChecks();
    }

    private void loadChecks() {
        checks.clear();
        try (FileInputStream fis = openFileInput(CHECKS_FILE);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            while (true) {
                try {
                    checks.add((Check) ois.readObject());
                } catch (Exception eof) {
                    break;
                }
            }
            Log.d(TAG, "Loaded " + checks.size() + " checks");
        } catch (Exception e) {
            Log.d(TAG, "No checks file yet");
        }

        adapter.notifyDataSetChanged();

        if (checks.isEmpty()) {
            tvChecksEmpty.setVisibility(View.VISIBLE);
            lvChecks.setVisibility(View.GONE);
        } else {
            tvChecksEmpty.setVisibility(View.GONE);
            lvChecks.setVisibility(View.VISIBLE);
        }
    }
}