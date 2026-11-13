
package com.example.communitysafetyapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

public class ActiveChecksActivity extends AppCompatActivity {

    private static final String TAG = "ActiveChecksActivity";
    private ListView lvChecks;
    private TextView tvChecksEmpty;
    private Button btnStartCheck;

    private ArrayList<Check> checks = new ArrayList<>();
    private ArrayAdapter<Check> adapter;
    private CountDownTimer countDownTimer;

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


        // Use custom cards in the listView ie item_check xml layout
        adapter = new ArrayAdapter<Check>(this, R.layout.item_check, checks) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.item_check, parent, false);
                }

                Check check = getItem(position);

                TextView tvName = convertView.findViewById(R.id.tvCheckName);
                TextView tvReason = convertView.findViewById(R.id.tvCheckReason);
                TextView tvStatus = convertView.findViewById(R.id.tvCheckStatus);
                TextView tvTimer = convertView.findViewById(R.id.tvCheckTimer);
                TextView tvCreatedDate = convertView.findViewById(R.id.tvCreatedDate);
                TextView tvClosedDate = convertView.findViewById(R.id.tvClosedDate);
                Button btnResend = convertView.findViewById(R.id.btnResend);
                Button btnClose = convertView.findViewById(R.id.btnClose);

                if (check != null) {

                    // Use getters from Check to display
                    tvName.setText(check.contactName);
                    tvReason.setText("Reason: " + check.getDisplayReason());
                    tvStatus.setText("Status: " + check.getDisplayStatus());

                    // No response yet
                    if (Check.STATUS_WAITING.equals(check.status)) {
                        tvTimer.setVisibility(View.VISIBLE);
                        tvTimer.setText("Time: " + check.getTimeRemaining());

                        // Timed out
                    } else if (Check.STATUS_TIMED_OUT.equals(check.status)) {
                        tvTimer.setVisibility(View.VISIBLE);
                        tvTimer.setText("Time: Response time expired");

                    } else {
                        tvTimer.setVisibility(View.GONE);
                    }

                    java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat(
                            "dd MMM yyyy, hh:mm a",
                            java.util.Locale.getDefault()
                    );

                    // Displaying time check was sent (changes when resending)
                    tvCreatedDate.setText("Sent: " + dateFormat.format(
                            new java.util.Date(check.startTime)));

                    // Displaying time check was closed (if it was)
                    if (Check.STATUS_CLOSED.equals(check.status) && check.closedTime > 0) {
                        tvClosedDate.setVisibility(View.VISIBLE);
                        tvClosedDate.setText("Closed: " + dateFormat.format(new java.util.Date(check.closedTime)));
                    } else {
                        tvClosedDate.setVisibility(View.GONE);
                    }

                    boolean expired = System.currentTimeMillis() >= check.deadline;

                    boolean unanswered = Check.STATUS_WAITING.equals(check.status)
                            || Check.STATUS_TIMED_OUT.equals(check.status);

                    // Give the user the option to resend if no response and deadline passed
                    if (expired && unanswered) {
                        btnResend.setVisibility(View.VISIBLE);

                        btnResend.setOnClickListener(v -> {
                            new AlertDialog.Builder(ActiveChecksActivity.this)
                                    .setTitle("Resend Check-In")
                                    .setMessage("Resend the check-in SMS to " + check.contactName + "?")
                                    .setPositiveButton("Resend", (dialog, which) -> resendCheck(check.id))
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        });

                    } else {
                        btnResend.setVisibility(View.GONE);
                        btnResend.setOnClickListener(null);
                    }

                    // Display close option, only if it can be closed
                    if (check.canClose()) {
                        btnClose.setVisibility(View.VISIBLE);

                        btnClose.setOnClickListener(v -> {
                            new AlertDialog.Builder(ActiveChecksActivity.this)
                                    .setTitle("Close Check-In")
                                    .setMessage("Are you sure you want to close this check-in?")
                                    .setPositiveButton("Close", (dialog, which) -> closeCheck(check.id))
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        });

                    } else {
                        btnClose.setVisibility(View.GONE);
                        btnClose.setOnClickListener(null);
                    }
                }

                return convertView;
            }
        };

        lvChecks.setAdapter(adapter);

        btnStartCheck.setOnClickListener(v -> StartCheckDialogFragment.newInstance().show(getSupportFragmentManager(), "startCheck"));
    }


    // Timer/Countdown functions
    @Override
    protected void onResume() {
        super.onResume();
        loadChecks();
        startTimer();
    }

    @Override
    protected void onPause() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        super.onPause();
    }

    private void loadChecks() {
        try {
            ArrayList<Check> loadedChecks = FileHelper.loadChecks(this);
            checks.clear();
            checks.addAll(loadedChecks);

            Log.d(TAG, "Loaded " + checks.size() + " checks");

        } catch (Exception e) {
            Log.e(TAG, "Could not load checks", e);
            return;
        }

        adapter.notifyDataSetChanged();

        // Check for existing check-ins (active and closed)
        if (checks.isEmpty()) {
            tvChecksEmpty.setVisibility(View.VISIBLE);
            lvChecks.setVisibility(View.GONE);
        } else {
            tvChecksEmpty.setVisibility(View.GONE);
            lvChecks.setVisibility(View.VISIBLE);
        }
    }


    // Use a single CountDownTimer for each check's countdown, if multiple are active
    // longestRemaining is the check with the longest timer, and is how long the shared timer runs
    private void startTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        long currentTime = System.currentTimeMillis();
        long longestRemaining = 0;

        for (Check check : checks) {
            if (Check.STATUS_WAITING.equals(check.status)) {
                long remaining = check.deadline - currentTime;

                if (remaining > longestRemaining) {
                    longestRemaining = remaining;
                }
            }
        }

        if (longestRemaining <= 0) {
            adapter.notifyDataSetChanged();
            return;
        }

        countDownTimer = new CountDownTimer(longestRemaining, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onFinish() {
                adapter.notifyDataSetChanged();
                countDownTimer = null;
            }
        }.start();
    }


    // User resends the check-in after receiving no response within the window
    private void resendCheck(String checkId) {

        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        loadChecks();

        Check selectedCheck = null;

        for (Check check : checks) {
            if (checkId.equals(check.id)) {
                selectedCheck = check;
                break;
            }
        }

        if (selectedCheck == null) {
            Toast.makeText(this, "Check-in not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Check check = selectedCheck;


        // Can only resend once the deadline has passed and there was no response
        if ((!Check.STATUS_WAITING.equals(check.status)
                && !Check.STATUS_TIMED_OUT.equals(check.status))
                || System.currentTimeMillis() < check.deadline) {
            Toast.makeText(this, "Check-in is not eligible for resend", Toast.LENGTH_SHORT).show();
            return;
        }

        // Reuse the original response window selected by the user
        // Todo: let them change the window
        long duration = check.deadline - check.startTime;

        if (duration <= 0) {
            duration = 15 * 60000L;
        }

        String message = "Hi " + check.contactName
                + ", this is another safety check-in. "
                + "Reply OK if you're fine, or HELP if you need assistance.";

        // SMS resending
        try {
            SmsManager.getDefault().sendTextMessage(
                    check.contactPhone, null, message, null, null);

            check.startTime = System.currentTimeMillis();
            check.deadline = check.startTime + duration;
            check.status = Check.STATUS_WAITING;
            check.lateResponse = false;

            check.timeline.add("Check-in SMS resent");

            saveChecks();
            adapter.notifyDataSetChanged();
            startTimer();

            Toast.makeText(this, "Check-in resend requested", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e(TAG, "Could not resend check-in", e);
            Toast.makeText(this, "Could not resend SMS", Toast.LENGTH_SHORT).show();
        }
    }

    // Close an already resolved check
    private void closeCheck(String checkId) {

        try {
            ArrayList<Check> loadedChecks = FileHelper.loadChecks(this);

            Check selectedCheck = null;

            for (Check check : loadedChecks) {
                if (checkId.equals(check.id)) {
                    selectedCheck = check;
                    break;
                }
            }

            if (selectedCheck == null) {
                Toast.makeText(this, "Check-in not found", Toast.LENGTH_SHORT).show();
                return;
            }

            // Ensure the check-in can be closed
            if (!selectedCheck.canClose()) {
                Toast.makeText(this, "Check-in cannot be closed yet", Toast.LENGTH_SHORT).show();
                return;
            }

            // Preserve the previous status before closing
            selectedCheck.timeline.add("Check closed - Previous status: "
                    + selectedCheck.getDisplayStatus());

            selectedCheck.closedTime = System.currentTimeMillis();
            selectedCheck.status = Check.STATUS_CLOSED;

            // Save the updated checks
            if (FileHelper.saveChecks(this, loadedChecks)) {

                loadChecks();
                startTimer();

                Toast.makeText(this, "Check-in closed", Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(this, "Could not close check-in", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Could not close check-in", e);
            Toast.makeText(this, "Could not close check-in", Toast.LENGTH_SHORT).show();
        }
    }

    // Used to display the checks when one is created
    public void refreshChecks() {
        loadChecks();
        startTimer();
    }

    // Update the checks file - resending updates the same check record
    private void saveChecks() {

        if (!FileHelper.saveChecks(this, checks)) {
            Log.e(TAG, "Could not save checks");
        }
    }
}
