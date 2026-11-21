package com.example.communitysafetyapp;

import java.io.Serializable;

public class Settings implements Serializable {
    public String myNumber = "";
    public int defaultWindowMinutes = 15;
    public String defaultReason = "CHECK_IN";
    public int cooldownMinutes = 10;
    public int maxActiveChecks = 3;
}
