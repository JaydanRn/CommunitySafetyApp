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

    public String id;
    public String contactName;
    public String contactPhone;
    public String reason;
    public long startTime;
    public long deadline;
    public String status;
    public ArrayList<String> timeline = new ArrayList<>();

    public Check(){}

    public boolean isActive(){
        return (STATUS_WAITING.equals(status) || STATUS_ESCALATED_PHYSICAL.equals(status) || STATUS_ESCALATED_EMERGENCY.equals(status));
    }

    public String toString(){
        return contactName + " - " + reason + " - " + status;
    }
}
