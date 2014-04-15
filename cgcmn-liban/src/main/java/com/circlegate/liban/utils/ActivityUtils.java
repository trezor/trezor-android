package com.circlegate.liban.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Parcelable;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.widget.Toast;

import com.circlegate.liban.R;
import com.circlegate.liban.base.CommonClasses.IGlobalContext;

import java.util.List;
import java.util.Locale;

public class ActivityUtils {
    private static final String ACTIVITY_RESULT_PARCELABLE = ActivityUtils.class.getName() + "." + "ACTIVITY_RESULT_PARCELABLE";

    public static void setResultParcelable(Activity activity, int resultCode, Parcelable data) {
        Intent intent = new Intent();
        intent.putExtra(ACTIVITY_RESULT_PARCELABLE, data);
        activity.setResult(resultCode, intent);
    }

    public static <T extends Parcelable> T getResultParcelable(Intent resultData) {
        return resultData != null ? resultData.<T>getParcelableExtra(ACTIVITY_RESULT_PARCELABLE) : null;
    }

    public static void showSpeechRecognitionActivity(Activity activity, String msg) {
        showSpeechRecognitionActivity(activity, msg, null);
    }

    public static void showSpeechRecognitionActivity(Activity activity, String msg, Locale optLocale) {
        PackageManager pm = activity.getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);

        if (activities.size() == 0) {
            Toast.makeText(activity, R.string.voice_recognition_not_available, Toast.LENGTH_LONG).show();
        }
        else {
            Intent itVoiceRec = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            itVoiceRec.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            itVoiceRec.putExtra(RecognizerIntent.EXTRA_PROMPT, msg);
            // Na Android Wear zmena jazyka mozna nefunguje :( (prosinec 2014)
            if (optLocale != null) {
                String lang = optLocale.getLanguage();

                if (!TextUtils.isEmpty(lang)) {
                    String country = optLocale.getCountry();
                    //Toast.makeText(activity, lang + "-" + country, Toast.LENGTH_SHORT).show();

                    itVoiceRec.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang + (!TextUtils.isEmpty(country) ? ("-" + country) : ""));
                    itVoiceRec.putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", new String[]{ lang });
                }
            }
            activity.startActivityForResult(itVoiceRec, IGlobalContext.RQC_SPEECH_RECOGNITION);
        }
    }
}