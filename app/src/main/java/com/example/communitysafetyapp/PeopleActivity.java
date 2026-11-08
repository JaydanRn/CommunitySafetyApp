package com.example.communitysafetyapp;

import android.content.Context;
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
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class PeopleActivity extends AppCompatActivity {

    private static final String TAG = "PeopleActivity";
    private static final String CONTACTS_FILE = "contacts.dat";
    private static final String SETTINGS_FILE = "settings.dat";
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

        fabAddContact.setOnClickListener(v ->{
            Intent i = new Intent(
                    PeopleActivity.this,
                    ItemContactActivity.class
            );
            startActivity(i);
        });

        lvContacts.setOnItemClickListener(((parent, view, position, id) -> {
            Contact c = contacts.get(position);
            Intent i = new Intent(
                    PeopleActivity.this,
                    ItemContactActivity.class
            );
            i.putExtra("name", c.name);
            i.putExtra("phone", c.phone);
            i.putExtra("group", c.group);
            startActivity(i);
        }));

        fabAddContact.setOnLongClickListener(v -> {
            StartCheckDialogFragment.newInstance().show(getSupportFragmentManager(), "startCheck");
            return true;
        });
    }
    @Override
    protected void onResume(){
        super.onResume();
        loadSettings();
        loadContacts();
    }

    private void loadSettings(){
        try (FileInputStream fis = openFileInput(SETTINGS_FILE);
             ObjectInputStream ois = new ObjectInputStream(fis)) {

            settings = (Settings) ois.readObject();
            Log.d(TAG, "Settings loaded: " + settings.myNumber);

        } catch (Exception e) {
            Log.d(TAG, "No settings file yet (first run) — using defaults");
            settings = new Settings();
        }

        etMyNumber.setText(settings.myNumber);
        if (settings.escalationContactPhone.isEmpty()) {
            tvEscalationContact.setText("Escalation Contact: none");
        } else {
            tvEscalationContact.setText("Escalation Contact: " + settings.escalationContactName
                    + " (" + settings.escalationContactPhone + ")");
        }
    }

    private void loadContacts(){
        contacts.clear();
        try (FileInputStream fis = openFileInput(CONTACTS_FILE);
             ObjectInputStream ois = new ObjectInputStream(fis)) {

            while (true) {
                try {
                    Contact c = (Contact) ois.readObject();
                    contacts.add(c);
                } catch (Exception eof) {
                    break;
                }
            }
            Log.d(TAG, "Loaded " + contacts.size() + " contacts");

        } catch (Exception e) {
            Log.d(TAG, "No contacts file yet — starting empty");
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

    private void saveSettings(){
        try (FileOutputStream fos = openFileOutput(SETTINGS_FILE, Context.MODE_PRIVATE);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {

            oos.writeObject(settings);
            Log.d(TAG, "Settings saved");

        } catch (Exception e) {
            Log.d(TAG, "Settings save failed: " + e.getMessage(), e);
            Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void saveYou(){
        String num = "";
        if (etMyNumber.getText() != null){
            num = etMyNumber.getText().toString().trim();
        }
        if (num.isEmpty()) {
            etMyNumber.setError("Enter your number");
            return;
        }

        settings.myNumber = num;
        saveSettings();
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
    }

    private void pickEscalationContact(){
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
                    saveSettings();
                    tvEscalationContact.setText("Escalation Contact: " + c.name + " (" + c.phone + ")");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}