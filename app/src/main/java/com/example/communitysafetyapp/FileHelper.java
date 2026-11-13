package com.example.communitysafetyapp;

import android.content.Context;
import android.util.Log;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;


// Functions for reading/writing of checks, contacts and settings files
public class FileHelper {

    private static final String TAG = "FileHelper";
    private static final String CHECKS_FILE = "checks.dat";
    private static final String CONTACTS_FILE = "contacts.dat";
    private static final String SETTINGS_FILE = "settings.dat";

    // Check-ins
    public static ArrayList<Check> loadChecks(Context context)
            throws IOException, ClassNotFoundException {
        return loadList(context, CHECKS_FILE, Check.class);
    }

    public static boolean saveChecks(Context context, ArrayList<Check> checks) {
        return saveList(context, CHECKS_FILE, checks);
    }

    // Contacts
    public static ArrayList<Contact> loadContacts(Context context)
            throws IOException, ClassNotFoundException {
        return loadList(context, CONTACTS_FILE, Contact.class);
    }

    public static boolean saveContacts(Context context, ArrayList<Contact> contacts) {
        return saveList(context, CONTACTS_FILE, contacts);
    }

    // Settings
    public static Settings loadSettings(Context context)
            throws IOException, ClassNotFoundException {

        try (FileInputStream fis = context.openFileInput(SETTINGS_FILE);
             ObjectInputStream ois = new ObjectInputStream(fis)) {

            return (Settings) ois.readObject();

        } catch (FileNotFoundException e) {
            return new Settings();
        }
    }

    public static boolean saveSettings(Context context, Settings settings) {

        try (FileOutputStream fos = context.openFileOutput(SETTINGS_FILE, Context.MODE_PRIVATE);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {

            oos.writeObject(settings);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Could not save settings", e);
            return false;
        }
    }

    // Files containing multiple serialized objects.
    private static <T> ArrayList<T> loadList(Context context, String filename, Class<T> type)
            throws IOException, ClassNotFoundException {

        ArrayList<T> items = new ArrayList<>();

        try (FileInputStream fis = context.openFileInput(filename);
             ObjectInputStream ois = new ObjectInputStream(fis)) {

            while (true) {
                try {
                    items.add(type.cast(ois.readObject()));
                } catch (EOFException e) {
                    break;
                }
            }

        } catch (FileNotFoundException e) {
            //  Return empty list if file not found
        }

        return items;
    }

    private static <T> boolean saveList(Context context, String filename, ArrayList<T> items) {

        try (FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {

            for (T item : items) {
                oos.writeObject(item);
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Could not save " + filename, e);
            return false;
        }
    }
}
