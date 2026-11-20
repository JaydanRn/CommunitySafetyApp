package com.example.communitysafetyapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.content.Intent;

import java.util.ArrayList;
import java.util.Locale;

// Receive the SMS messages and interpret the responses to update the checks
public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        // Check that an SMS was received
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            return;
        }

        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);

        if (messages == null || messages.length == 0) {
            return;
        }

        // Get the sender and message
        String sender = messages[0].getDisplayOriginatingAddress();

        StringBuilder body = new StringBuilder();

        for (SmsMessage sms : messages) {
            body.append(sms.getDisplayMessageBody());
        }

        String response = body.toString().trim().toUpperCase(Locale.ROOT);

        // Only accept OK or HELP responses for now (case-insensitive)
        // TODO: handle messages that are neither
        if (!response.equals("OK") && !response.equals("HELP")) {
            return;
        }

        ArrayList<Check> checks;

        try {
            checks = FileHelper.loadChecks(context);
        } catch (Exception e) {
            Log.e(TAG, "Could not load checks", e);
            return;
        }

        boolean updated = false;

        for (Check check : checks) {

            if ((Check.STATUS_WAITING.equals(check.status)
                    || Check.STATUS_TIMED_OUT.equals(check.status))
                    && PhoneHelper.phoneNumbersMatch(sender, check.contactPhone)) {

                long responseTime = System.currentTimeMillis();

                check.lateResponse = responseTime > check.deadline;

                if (response.equals("OK")) {
                    check.status = Check.STATUS_RESOLVED_OK;

                } else {
                    check.status = Check.STATUS_RESOLVED_HELP;
                }

                if (check.lateResponse) {
                    check.timeline.add("Late SMS received: " + response);
                } else {
                    check.timeline.add("Received SMS: " + response);
                }

                updated = true;
                break;
            }
        }

        if (!updated) {
            return;
        }

        // Save updated checks
        if (FileHelper.saveChecks(context, checks)) {
            Intent updateIntent = new Intent(
                    "com.example.communitysafetyapp.CHECK_UPDATED");

            updateIntent.setPackage(context.getPackageName());
            context.sendBroadcast(updateIntent);
        }
    }
}
