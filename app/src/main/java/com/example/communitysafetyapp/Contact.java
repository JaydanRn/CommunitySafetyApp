package com.example.communitysafetyapp;

import java.io.Serializable;

public class Contact implements Serializable {
    public String name;
    public String phone;
    public String group;
    public boolean optIn;

    public Contact(String name, String phone, String group, boolean optIn) {
        this.name = name;
        this.phone = phone;
        this.group = group;
        this.optIn = optIn;
    }

    public String toString() {
        return name + " — " + phone + " (" + group + ")";
    }
}
