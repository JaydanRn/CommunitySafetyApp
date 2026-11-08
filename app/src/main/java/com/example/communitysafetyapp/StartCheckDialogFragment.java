package com.example.communitysafetyapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class StartCheckDialogFragment extends DialogFragment {

    private static final String TAG = "StartCheckDialog";
    private static final String CONTACTS_FILE = "contacts.dat";
    private static final String CHECKS_FILE   = "checks.dat";
    private static final String SETTINGS_FILE = "settings.dat";
    private static final int SMS_PERMISSION_REQUEST = 100;

    // Argument keys
    private static final String ARG_CONTACT_PHONE = "arg_contact_phone";
    private static final String ARG_CONTACT_NAME  = "arg_contact_name";

    // Views
    private TextView tvDialogTitle, tvDialogContact;
    private Spinner spinnerContact, spinnerWindow;
    private RadioButton rbCheckIn, rbSafetyConcern;
    private Button btnStart, btnCancel;

    // Data
    private ArrayList<Contact> contacts = new ArrayList<>();
    private Settings settings = new Settings();

    private String selectedPhone;
    private String selectedName;

    private boolean pendingSend = false;

    public static StartCheckDialogFragment newInstance(String contactName, String contactPhone) {
        StartCheckDialogFragment f = new StartCheckDialogFragment();
        Bundle b = new Bundle();
        b.putString(ARG_CONTACT_NAME, contactName);
        b.putString(ARG_CONTACT_PHONE, contactPhone);
        f.setArguments(b);
        return f;
    }

    public static StartCheckDialogFragment newInstance() {
        return new StartCheckDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_start_check, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvDialogTitle    = view.findViewById(R.id.tvDialogTitle);
        tvDialogContact  = view.findViewById(R.id.tvDialogContact);
        spinnerContact   = view.findViewById(R.id.spinnerContact);
        spinnerWindow    = view.findViewById(R.id.spinnerWindow);
        rbCheckIn        = view.findViewById(R.id.rbCheckIn);
        rbSafetyConcern  = view.findViewById(R.id.rbSafetyConcern);
        btnStart         = view.findViewById(R.id.btnStart);
        btnCancel        = view.findViewById(R.id.btnCancel);

        loadSettings();
        loadContacts();

        ArrayAdapter<CharSequence> windowAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.check_windows,
                android.R.layout.simple_spinner_item
        );
        windowAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerWindow.setAdapter(windowAdapter);

        if (getArguments() != null && getArguments().getString(ARG_CONTACT_PHONE) != null) {
            selectedPhone = getArguments().getString(ARG_CONTACT_PHONE);
            selectedName  = getArguments().getString(ARG_CONTACT_NAME);

            tvDialogContact.setText("Checking on: " + selectedName + " (" + selectedPhone + ")");
            tvDialogContact.setVisibility(View.VISIBLE);
            spinnerContact.setVisibility(View.GONE);
        } else {
            if (contacts.isEmpty()) {
                Toast.makeText(requireContext(),
                        "No contacts yet - add one on the People screen",
                        Toast.LENGTH_LONG).show();
                dismiss();
                return;
            }

            String[] names = new String[contacts.size()];
            for (int i = 0; i < contacts.size(); i++) {
                names[i] = contacts.get(i).name + " (" + contacts.get(i).phone + ")";
            }
            ArrayAdapter<String> contactAdapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    names
            );
            contactAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerContact.setAdapter(contactAdapter);
            spinnerContact.setVisibility(View.VISIBLE);
        }

        btnStart.setOnClickListener(v -> onStartClicked());
        btnCancel.setOnClickListener(v -> dismiss());
    }

    private void onStartClicked() {
        if (selectedPhone == null) {
            int pos = spinnerContact.getSelectedItemPosition();
            if (pos < 0 || pos >= contacts.size()) {
                Toast.makeText(requireContext(), "Pick a contact", Toast.LENGTH_SHORT).show();
                return;
            }
            selectedPhone = contacts.get(pos).phone;
            selectedName  = contacts.get(pos).name;
        }

        String reason = "";
        if (rbSafetyConcern.isChecked()) {
            reason = "SAFETY_CONCERN";
        } else {
            reason = "CHECK_IN";
        }

        int windowMinutes = parseWindowMinutes(spinnerWindow.getSelectedItem().toString());

        Check check = new Check();
        check.id           = System.currentTimeMillis() + "_" + selectedPhone;
        check.contactName  = selectedName;
        check.contactPhone = selectedPhone;
        check.reason       = reason;
        check.startTime    = System.currentTimeMillis();
        check.deadline     = check.startTime + (windowMinutes * 60_000L);
        check.status       = Check.STATUS_WAITING;
        check.timeline.add(timeNow() + " - Check started (" + windowMinutes + " min, reason: " + reason + ")");

        if (!saveCheck(check)) {
            Toast.makeText(requireContext(), "Could not save check", Toast.LENGTH_LONG).show();
            return;
        }

        String msg = buildMessage(check);
        sendSms(selectedPhone, msg);

        Toast.makeText(requireContext(), "Check started for " + selectedName, Toast.LENGTH_SHORT).show();
        dismiss();
    }

    private String buildMessage(Check check) {
        String sender = "";
        if (settings.myNumber.isEmpty()) {
            sender = "Someone";
        } else {
            sender = settings.myNumber;
        }

        return "Hi " + check.contactName + ", " + sender + " is checking in on you. Reply OK if you're fine, or HELP if you need assistance.";
    }

    private void loadSettings() {
        try (FileInputStream fis = requireContext().openFileInput(SETTINGS_FILE);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            settings = (Settings) ois.readObject();
        } catch (Exception e) {
            Log.d(TAG, "No settings file - using defaults");
            settings = new Settings();
        }
    }

    private void loadContacts() {
        contacts.clear();
        try (FileInputStream fis = requireContext().openFileInput(CONTACTS_FILE);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            while (true) {
                try {
                    contacts.add((Contact) ois.readObject());
                } catch (Exception eof) {
                    break;
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "No contacts file yet");
        }
    }

    private boolean saveCheck(Check check) {
        ArrayList<Check> checks = new ArrayList<>();
        try (FileInputStream fis = requireContext().openFileInput(CHECKS_FILE);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            while (true) {
                try {
                    checks.add((Check) ois.readObject());
                } catch (Exception eof) {
                    break;
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "No checks file yet - creating");
        }

        checks.add(check);

        try (FileOutputStream fos = requireContext().openFileOutput(CHECKS_FILE, Context.MODE_PRIVATE);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            for (Check c : checks) {
                oos.writeObject(c);
            }
            Log.d(TAG, "Saved check: " + check.id);
            return true;
        } catch (Exception e) {
            Log.d(TAG, "Save check failed: " + e.getMessage(), e);
            return false;
        }
    }

    private void sendSms(String phone, String message) {
        if (requireContext().checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            pendingSend = true;
            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_REQUEST);
            pendingPhone = phone;
            pendingMessage = message;
            return;
        }
        doSend(phone, message);
    }

    private String pendingPhone;
    private String pendingMessage;

    private void doSend(String phone, String message) {
        if (!requireContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_TELEPHONY_MESSAGING)) {
            Toast.makeText(requireContext(), "SMS is not supported on this device", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            SmsManager manager;
            if (Build.VERSION.SDK_INT >= 31) {
                manager = requireContext().getSystemService(SmsManager.class);
            } else {
                manager = SmsManager.getDefault();
            }

            if (manager == null) {
                Toast.makeText(requireContext(), "SMS service unavailable", Toast.LENGTH_LONG).show();
                return;
            }

            manager.sendTextMessage(phone, null, message, null, null);
            Log.d(TAG, "SMS submitted to " + phone);
            Toast.makeText(requireContext(), "SMS submitted for sending", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.d(TAG, "SMS failed: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "SMS failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingSend && pendingPhone != null && pendingMessage != null) {
                    doSend(pendingPhone, pendingMessage);
                    pendingSend = false;
                }
            } else {
                Toast.makeText(requireContext(),
                        "SMS permission denied - check saved but SMS not sent",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private static String timeNow() {
        return new SimpleDateFormat("HH:mm", Locale.US).format(new Date());
    }

    private static int parseWindowMinutes(String label) {
        try {
            return Integer.parseInt(label.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 15;
        }
    }
}
