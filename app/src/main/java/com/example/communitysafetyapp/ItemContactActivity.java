package com.example.communitysafetyapp;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;

public class ItemContactActivity extends AppCompatActivity {

    private static final String TAG = "ItemContactActivity";
    private TextView tvFormTitle;
    private EditText etName, etPhone;
    private Spinner spinnerGroup;
    private Button btnSave, btnCancel;
    private Button btnDelete;
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
        btnDelete    = findViewById(R.id.btnDelete);

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

            btnDelete.setVisibility(View.VISIBLE);

            String groupExtra = getIntent().getStringExtra("group");
            if (groupExtra != null) {
                int pos = groupAdapter.getPosition(groupExtra);
                if (pos >= 0) spinnerGroup.setSelection(pos);
            }
        }

        btnSave.setOnClickListener(v -> saveContact());
        btnCancel.setOnClickListener(v -> finish());
        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Contact")
                    .setMessage("Are you sure you want to delete this contact?")
                    .setPositiveButton("Delete", (dialog, which) -> deleteContact())
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        Button btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
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

        ArrayList<Contact> contacts;

        try {
            contacts = FileHelper.loadContacts(this);

        } catch (Exception e) {
            Log.e(TAG, "Could not load contacts", e);
            Toast.makeText(this, "Could not load contacts", Toast.LENGTH_LONG).show();
            return;
        }

        // Check whether another contact already has this phone number
        for (Contact c : contacts) {

            if (PhoneHelper.phoneNumbersMatch(phone, c.phone)) {

                // Ignore the contact currently being edited
                if (isEditMode && c.phone.equals(originalPhone)) {
                    continue;
                }

                etPhone.setError("A contact with this number already exists");
                return;
            }
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
                    break;
                }
            }
            if (!found) {
                Toast.makeText(this, "Original contact not found", Toast.LENGTH_LONG).show();
                return;
            }
        } else {
            contacts.add(new Contact(name, phone, group, true));
        }

        if (FileHelper.saveContacts(this, contacts)) {
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
            finish();

        } else {
            Toast.makeText(this, "Save failed", Toast.LENGTH_LONG).show();
        }
    }
    private void deleteContact() {
        try {
            ArrayList<Contact> contacts = FileHelper.loadContacts(this);

            boolean removed = false;

            for (int i = 0; i < contacts.size(); i++) {
                if (contacts.get(i).phone.equals(originalPhone)) {
                    contacts.remove(i);
                    removed = true;
                    break;
                }
            }

            if (!removed) {
                Toast.makeText(this, "Contact not found", Toast.LENGTH_SHORT).show();
                return;
            }

            if (FileHelper.saveContacts(this, contacts)) {
                Toast.makeText(this, "Contact deleted", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Could not delete contact", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Could not delete contact", e);
            Toast.makeText(this, "Could not delete contact", Toast.LENGTH_SHORT).show();
        }
    }
}