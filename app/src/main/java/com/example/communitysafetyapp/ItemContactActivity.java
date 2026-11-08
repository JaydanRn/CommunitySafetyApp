package com.example.communitysafetyapp;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class ItemContactActivity extends AppCompatActivity {

    private static final String TAG = "ItemContactActivity";
    private static final String CONTACTS_FILE = "contacts.dat";

    private TextView tvFormTitle;
    private EditText etName, etPhone;
    private Spinner spinnerGroup;
    private Button btnSave, btnCancel;

    private boolean isEditMode = false;
    private String originalPhone = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_item_contact);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvFormTitle  = findViewById(R.id.tvFormTitle);
        etName       = findViewById(R.id.etName);
        etPhone      = findViewById(R.id.etPhone);
        spinnerGroup = findViewById(R.id.spinnerGroup);
        btnSave      = findViewById(R.id.btnSave);
        btnCancel    = findViewById(R.id.btnCancel);

        ArrayAdapter<CharSequence> groupAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.contact_groups,
                android.R.layout.simple_spinner_item
        );
        groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGroup.setAdapter(groupAdapter);

        String nameExtra = getIntent().getStringExtra("name");
        if (nameExtra != null) {
            isEditMode = true;
            originalPhone = getIntent().getStringExtra("phone");

            tvFormTitle.setText("Edit Contact");
            etName.setText(nameExtra);
            etPhone.setText(originalPhone);

            String groupExtra = getIntent().getStringExtra("group");
            if (groupExtra != null) {
                int pos = groupAdapter.getPosition(groupExtra);
                if (pos >= 0) spinnerGroup.setSelection(pos);
            }
        }

        btnSave.setOnClickListener(v -> saveContact());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void saveContact() {
        String name = "";
        if (etName.getText() != null) {
            name = etName.getText().toString().trim();
        }

        String phone = "";
        if (etPhone.getText() != null) {
            phone = etPhone.getText().toString().trim();
        }

        String group = spinnerGroup.getSelectedItem().toString();

        if (name.isEmpty()) {
            etName.setError("Name required");
            return;
        }
        if (phone.isEmpty() || phone.length() < 7) {
            etPhone.setError("Enter a valid phone number");
            return;
        }

        ArrayList<Contact> contacts = new ArrayList<>();
        try (FileInputStream fis = openFileInput(CONTACTS_FILE);
             ObjectInputStream ois = new ObjectInputStream(fis)) {

            while (true) {
                try {
                    contacts.add((Contact) ois.readObject());
                } catch (Exception eof) {
                    break;
                }
            }
            Log.d(TAG, "Loaded " + contacts.size() + " contacts");

        } catch (Exception e) {
            Log.d(TAG, "No contacts file yet - creating new one");
        }

        if (isEditMode) {
            boolean found = false;
            for (int i = 0; i < contacts.size(); i++) {
                Contact c = contacts.get(i);
                if (c.phone.equals(originalPhone)) {
                    c.name = name;
                    c.phone = phone;
                    c.group = group;
                    found = true;
                    Log.d(TAG, "Updated contact at index " + i);
                    break;
                }
            }
            if (!found) {
                Toast.makeText(this, "Original contact not found", Toast.LENGTH_LONG).show();
                return;
            }
        } else {
            contacts.add(new Contact(name, phone, group, true));
            Log.d(TAG, "Added new contact: " + name);
        }

        try (FileOutputStream fos = openFileOutput(CONTACTS_FILE, Context.MODE_PRIVATE);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {

            for (Contact c : contacts) {
                oos.writeObject(c);
            }
            Log.d(TAG, "Saved " + contacts.size() + " contacts");
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
            finish();

        } catch (Exception e) {
            Log.d(TAG, "Save failed: " + e.getMessage(), e);
            Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}