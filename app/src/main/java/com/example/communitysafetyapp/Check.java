
package com.example.communitysafetyapp;

import java.io.Serializable;
import java.util.ArrayList;

public class Check implements Serializable {
    public static final String STATUS_WAITING = "WAITING";
    public static final String STATUS_RESOLVED_OK = "RESOLVED_OK";
    public static final String STATUS_RESOLVED_HELP = "RESOLVED_HELP";
    public static final String STATUS_ESCALATED_PHYSICAL = "ESCALATED_PHYSICAL";
    public static final String STATUS_ESCALATED_EMERGENCY = "ESCALATED_EMERGENCY";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String STATUS_TIMED_OUT = "TIMED_OUT";

    public String id;
    public String contactName;
    public String contactPhone;
    public String reason;
    public long startTime;
    public long closedTime;
    public long deadline;
    public String status;
    public boolean lateResponse = false;
    public ArrayList<String> timeline = new ArrayList<>();

    public Check(){}

    public boolean isActive(){
        return (STATUS_WAITING.equals(status) || STATUS_ESCALATED_PHYSICAL.equals(status) || STATUS_ESCALATED_EMERGENCY.equals(status));
    }

    // Get current status for check-in
    public String getDisplayStatus() {

        String displayStatus;

        if (STATUS_RESOLVED_OK.equals(status)) {
            displayStatus = "Safe";
        } else if (STATUS_RESOLVED_HELP.equals(status)) {
            displayStatus = "Needs Help";
        } else if (STATUS_TIMED_OUT.equals(status)
                || (STATUS_WAITING.equals(status) && System.currentTimeMillis() >= deadline)) {
            displayStatus = "Timed Out";
        } else if (STATUS_WAITING.equals(status)) {
            displayStatus = "Awaiting Response";
        } else {
            displayStatus = status;
        }

        if (lateResponse) {
            displayStatus += " (Late Response)";
        }

        return displayStatus;
    }

    // Get user selected reason for the check-in
    public String getDisplayReason() {

        if ("CHECK_IN".equals(reason)) {
            return "Routine Check-In";
        } else if ("SAFETY_CONCERN".equals(reason)) {
            return "Safety Concern";
        } else {
            return reason;
        }
    }

    @Override
    public String toString() {

        String display = contactName
                + "\nReason: " + getDisplayReason()
                + "\nStatus: " + getDisplayStatus();

        if (STATUS_WAITING.equals(status)) {
            display += "\nTime: " + getTimeRemaining();
        } else if (STATUS_TIMED_OUT.equals(status)) {
            display += "\nTime: Response time expired";
        }

        return display;
    }

    // Used to determine if a user can close a check i.e it was resolved,
    // or no response and deadline passed
    public boolean canClose() {

        return STATUS_RESOLVED_OK.equals(status)
                || STATUS_RESOLVED_HELP.equals(status)
                || STATUS_TIMED_OUT.equals(status)
                || (STATUS_WAITING.equals(status)
                && System.currentTimeMillis() >= deadline);
    }

    // Get the remaining time to update the countdown, called vis  adapter.notifyDataSetChanged()
    public String getTimeRemaining() {

        if (!STATUS_WAITING.equals(status)) {
            return "";
        }

        long remaining = deadline - System.currentTimeMillis();

        if (remaining <= 0) {
            return "Response time expired";
        }

        long totalSeconds = (remaining + 999) / 1000;

        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        return String.format(java.util.Locale.getDefault(), "%02d:%02d remaining", minutes, seconds);
    }
}
