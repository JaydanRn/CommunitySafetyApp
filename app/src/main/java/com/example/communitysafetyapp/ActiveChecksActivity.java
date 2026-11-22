package com.example.communitysafetyapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

public class ActiveChecksActivity extends AppCompatActivity {

    private static final String TAG = "ActiveChecksActivity";
    private static final String ACTION_CHECK_UPDATED = "com.example.communitysafetyapp.CHECK_UPDATED";

    //holds every check-in, displayChecks only holds the one on the current tab
    private final ArrayList<Check> checks = new ArrayList<>();
    private final ArrayList<Check> displayedChecks = new ArrayList<>();

    //Checks whether the Checks tab is opened or closed
    private boolean showingClosed = false;

    private ListView lvChecks;
    private TextView tvChecksEmpty;
    private Button btnStartCheck;
    private ArrayAdapter<Check> adapter;
    private CountDownTimer countDownTimer;

    // Reload the saved check-ins when SmsReceiver processes an incoming SMS.
    private final BroadcastReceiver checkUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshChecks();
        }
    };

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
        adapter = new ArrayAdapter<Check>(this, R.layout.item_check, displayedChecks) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.item_check, parent, false);
                }

                //Fill this card with the details of on check-in
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

        //Two tabs that Open Checks and Closed Checks
        TabLayout tabChecks = findViewById(R.id.tabChecks);

        tabChecks.addTab(tabChecks.newTab().setText("Open Checks"));
        tabChecks.addTab(tabChecks.newTab().setText("Closed Checks"));

        tabChecks.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                //switch the list to the selected tab
                showingClosed = tab.getPosition() == 1;
                updateCheckList();
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        //Back button that closes this screen
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        lvChecks.setAdapter(adapter);

        //opens the pop-up form
        btnStartCheck.setOnClickListener(v -> StartCheckDialogFragment.newInstance().show(getSupportFragmentManager(), "startCheck"));
    }


    // Register while the activity is visible.
    @Override
    protected void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter(ACTION_CHECK_UPDATED);
        ContextCompat.registerReceiver(
                this,
                checkUpdateReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
    }

    //reload the check-ins everytime the screen pops up
    @Override
    protected void onResume() {
        super.onResume();
        refreshChecks();
    }

    //stops the countdown when the screen is hidden
    @Override
    protected void onPause() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
        super.onPause();
    }

    //stop listening for updates
    @Override
    protected void onStop() {
        unregisterReceiver(checkUpdateReceiver);
        super.onStop();
    }

    // Reload check-ins after creating one or receiving an SMS update.
    public void refreshChecks() {
        loadChecks();
        startTimer();
    }

    //read saved check-ins and updates the list
    private void loadChecks() {
        try {
            ArrayList<Check> loadedChecks = FileHelper.loadChecks(this);
            checks.clear();
            checks.addAll(loadedChecks);
        } catch (Exception e) {
            Log.e(TAG, "Could not load checks", e);
            return;
        }

        updateCheckList();
    }


    //Shows the open or closed checks-ins based on the tab
    private void updateCheckList() {
        displayedChecks.clear();

        for (Check check : checks) {
            boolean isClosed = Check.STATUS_CLOSED.equals(check.status);

            if (isClosed == showingClosed) {
                displayedChecks.add(check);
            }
        }

        adapter.notifyDataSetChanged();

        //no check-ins messages
        if (displayedChecks.isEmpty()) {
            lvChecks.setVisibility(View.GONE);
            tvChecksEmpty.setVisibility(View.VISIBLE);

            if (showingClosed) {
                tvChecksEmpty.setText("No closed check-ins");
            } else {
                tvChecksEmpty.setText("No open check-ins");
            }

        } else {
            lvChecks.setVisibility(View.VISIBLE);
            tvChecksEmpty.setVisibility(View.GONE);
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

        //Countdown update
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

    // Update the checks file - resending updates the same check record
    private void saveChecks() {

        if (!FileHelper.saveChecks(this, checks)) {
            Log.e(TAG, "Could not save checks");
        }
    }

}
