package com.circlegate.liban.utils;

import org.json.JSONArray;
import org.json.JSONObject;

public class JSONUtils {
    public static JSONObject optJSONObjectNotNull(JSONObject json, String key) {
        JSONObject ret = json.optJSONObject(key);
        return ret == null ? new JSONObject() : ret;
    }

    public static JSONArray optJSONArraytNotNull(JSONObject json, String key) {
        JSONArray ret = json.optJSONArray(key);
        return ret == null ? new JSONArray() : ret;
    }

    public static String optStringNotNull(JSONObject json, String key) {
        return json.isNull(key) ? "" : json.optString(key); // jenom optString muze vratit hodnotu stringu: "null"!!!
    }
}
