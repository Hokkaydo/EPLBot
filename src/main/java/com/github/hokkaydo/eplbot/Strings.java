package com.github.hokkaydo.eplbot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class Strings {

    private Strings() {}
    private static final Map<String, String> STRINGS_MAP = new HashMap<>();

    public static void load() throws JSONException {
        InputStream stream = Strings.class.getClassLoader().getResourceAsStream("strings.json");
        assert stream != null;
        JSONObject object = new JSONObject(new JSONTokener(stream));
        if(object.isEmpty()) return;
        JSONArray names = object.names();
        for (int i = 0; i < names.length(); i++) {
            String key = names.getString(i);
            STRINGS_MAP.put(key, object.getString(key));
        }
    }
    private static final String STRING_NOT_FOUND = "Erreur de traduction. Veuillez la signaler à la modération";

    public static String getString(String key) {
        if(!STRINGS_MAP.containsKey(key)) {
            String formatted = "Missing string : %s%n".formatted(key);
            Main.LOGGER.log(Level.WARNING, formatted);
            return STRING_NOT_FOUND;
        }
        return STRINGS_MAP.get(key);
    }

}
