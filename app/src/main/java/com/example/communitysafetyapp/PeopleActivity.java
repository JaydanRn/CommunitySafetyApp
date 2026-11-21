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

        //Shows the contacts in a list
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, contacts);
        lvContacts.setAdapter(adapter);


        //Save your number and chooses the escalation contact
        btnSaveYou.setOnClickListener(v -> saveYou());
        btnPickEscalation.setOnClickListener(v -> pickEscalationContact());

        //Plus button opens the form to fill out to add a new contact
        fabAddContact.setOnClickListener(v -> {
            Intent i = new Intent(
                    PeopleActivity.this,
                    ItemContactActivity.class
            );
            startActivity(i);
        });

        // Tapping an added contact opens an edit mode with it details
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

        //Long pressing on the plus button starts a check-in
        fabAddContact.setOnLongClickListener(v -> {
            StartCheckDialogFragment.newInstance().show(getSupportFragmentManager(), "startCheck");
            return true;
        });

        //Back button closes the screen
        Button btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
    }


    //Reloads settings and contacts
    @Override
    protected void onResume() {
        super.onResume();
        loadSettings();
        loadContacts();
    }

    //Reads the saved settings then shows your contact and escalation contact
    private void loadSettings() {
        try {
            settings = FileHelper.loadSettings(this);

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

    //Reads the saved contacts and shows the list, or if it is empty
    private void loadContacts() {
        try {
            ArrayList<Contact> loadedContacts = FileHelper.loadContacts(this);
            contacts.clear();
            contacts.addAll(loadedContacts);
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

    //Writes the settings to the file and report failures
    private boolean saveSettings() {
        if (FileHelper.saveSettings(this, settings)) {
            return true;
        } else {
            Log.e(TAG, "Could not save settings");
            Toast.makeText(this, "Could not save settings", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    //saves your phone number
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

    //shows a list of contacts to choose who to alert when help is needed
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