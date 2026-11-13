package com.example.communitysafetyapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class PeopleActivity extends AppCompatActivity {

    private static final String TAG = "PeopleActivity";

    private TextView tvEscalationContact, tvContactsEmpty;
    private EditText etMyNumber;
    private Button btnPickEscalation, btnSaveYou;
    private ListView lvContacts;
    private FloatingActionButton fabAddContact;

    private ArrayList<Contact> contacts = new ArrayList<>();
    private ArrayAdapter<Contact> adapter;
    private Settings settings = new Settings();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_people);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etMyNumber          = findViewById(R.id.etMyNumber);
        tvEscalationContact = findViewById(R.id.tvEscalationContact);
        btnPickEscalation   = findViewById(R.id.btnPickEscalation);
        btnSaveYou          = findViewById(R.id.btnSaveYou);
        lvContacts          = findViewById(R.id.lvContacts);
        tvContactsEmpty     = findViewById(R.id.tvContactsEmpty);
        fabAddContact       = findViewById(R.id.fabAddContact);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, contacts);
        lvContacts.setAdapter(adapter);

        btnSaveYou.setOnClickListener(v -> saveYou());
        btnPickEscalation.setOnClickListener(v -> pickEscalationContact());

        fabAddContact.setOnClickListener(v -> {
            Intent i = new Intent(
                    PeopleActivity.this,
                    ItemContactActivity.class
            );
            startActivity(i);
        });

        lvContacts.setOnItemClickListener((parent, view, position, id) -> {
            Contact c = contacts.get(position);
            Intent i = new Intent(
                    PeopleActivity.this,
                    ItemContactActivity.class
            );
            i.putExtra("name", c.name);
            i.putExtra("phone", c.phone);
            i.putExtra("group", c.group);
            startActivity(i);
        });

        fabAddContact.setOnLongClickListener(v -> {
            StartCheckDialogFragment.newInstance().show(getSupportFragmentManager(), "startCheck");
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSettings();
        loadContacts();
    }

    private void loadSettings() {
        try {
            settings = FileHelper.loadSettings(this);
            Log.d(TAG, "Settings loaded: " + settings.myNumber);

        } catch (Exception e) {
            Log.e(TAG, "Could not load settings", e);
            Toast.makeText(this, "Could not load settings", Toast.LENGTH_LONG).show();
            return;
        }

        etMyNumber.setText(settings.myNumber);
        if (settings.escalationContactPhone.isEmpty()) {
            tvEscalationContact.setText("Escalation Contact: none");
        } else {
            tvEscalationContact.setText("Escalation Contact: " + settings.escalationContactName
                    + " (" + settings.escalationContactPhone + ")");
        }
    }

    private void loadContacts() {
        try {
            ArrayList<Contact> loadedContacts = FileHelper.loadContacts(this);

            for (Contact c : contacts) {
                Log.d("FileDebug", "Contact: " + c.name + " | " + c.phone);
            }

            contacts.clear();
            contacts.addAll(loadedContacts);

            Log.d(TAG, "Loaded " + contacts.size() + " contacts");

        } catch (Exception e) {
            Log.e(TAG, "Could not load contacts", e);
            Toast.makeText(this, "Could not load contacts", Toast.LENGTH_LONG).show();
            return;
        }

        adapter.notifyDataSetChanged();

        if (contacts.isEmpty()) {
            tvContactsEmpty.setVisibility(View.VISIBLE);
            lvContacts.setVisibility(View.GONE);
        } else {
            tvContactsEmpty.setVisibility(View.GONE);
            lvContacts.setVisibility(View.VISIBLE);
        }

    }

    private boolean saveSettings() {
        if (FileHelper.saveSettings(this, settings)) {
            Log.d(TAG, "Settings saved");
            return true;
        } else {
            Log.e(TAG, "Could not save settings");
            Toast.makeText(this, "Could not save settings", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void saveYou() {
        String num = "";
        if (etMyNumber.getText() != null) {
            num = etMyNumber.getText().toString().trim();
        }
        if (num.isEmpty()) {
            etMyNumber.setError("Enter your number");
            return;
        }

        settings.myNumber = num;
        if (saveSettings()) {
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        }
    }

    private void pickEscalationContact() {
        if (contacts.isEmpty()) {
            Toast.makeText(this, "Add a contact first", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[contacts.size()];
        for (int i = 0; i < contacts.size(); i++) {
            names[i] = contacts.get(i).name + " (" + contacts.get(i).phone + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle("Choose escalation contact")
                .setItems(names, (dialog, which) -> {
                    Contact c = contacts.get(which);
                    settings.escalationContactName = c.name;
                    settings.escalationContactPhone = c.phone;
                    if (saveSettings()) {
                        tvEscalationContact.setText("Escalation Contact: " + c.name + " (" + c.phone + ")");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}