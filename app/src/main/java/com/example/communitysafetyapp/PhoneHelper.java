package com.example.communitysafetyapp;

import android.os.Build;
import android.telephony.PhoneNumberUtils;

// Used to check the phone numbers for receiving SMS messages
public class PhoneHelper {

    public static boolean phoneNumbersMatch(String firstNumber, String secondNumber) {

        if (firstNumber == null || secondNumber == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return PhoneNumberUtils.areSamePhoneNumber(firstNumber, secondNumber, "TT");
        } else {
            return PhoneNumberUtils.compare(firstNumber, secondNumber);
        }
    }
}
