package com.github.hokkaydo.eplbot.module.shop;

import com.github.hokkaydo.eplbot.Main;
import com.github.hokkaydo.eplbot.database.DatabaseManager;
import com.github.hokkaydo.eplbot.module.shop.model.Item;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.sql.DataSource;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopProcessor extends ListenerAdapter {



        private long guildId;

        //Create an empty list for relevant roles
        private static Map<String,Integer> roles = new HashMap<>();
        private static Map<Integer, Item> shop = new HashMap<>();


        public ShopProcessor(long guildId) {
            super();
            Main.getJDA().addEventListener(this);
            DataSource datasource = DatabaseManager.getDataSource();
            this.guildId = guildId;
            try {
                loadRoles();
                loadShop();
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        public static void loadRoles() throws JSONException {
            InputStream stream = ShopProcessor.class.getClassLoader().getResourceAsStream("roles.json");
            assert stream != null;
            JSONObject object = new JSONObject(new JSONTokener(stream));
            if(object.isEmpty()) return;
            JSONArray names = object.names();
            for (int i = 0; i < names.length(); i++) {
                String key = names.getString(i);
                roles.put(key, object.getInt(key));
            }
        }

        public static void loadShop() throws JSONException {
            InputStream stream = ShopProcessor.class.getClassLoader().getResourceAsStream("shop.json");
            assert stream != null;
            JSONArray items = new JSONArray(new JSONTokener(stream));
            if (items.isEmpty()) {
                System.out.println("The 'items' array in 'shop.json' is empty.");
                return;
            }

            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                shop.put(item.getInt("id"), new Item(
                        item.getInt("id"),
                        item.getString("name"),
                        item.getInt("cost"),
                        item.getString("description"),
                        item.getInt("type"),
                        (float) item.getDouble("multiplier")
                ));
            }

        }
        public void getItems(String role) {

        }
        public void addItem(Item item) {

            List<String> lines;
            try {
                lines = Files.readAllLines(Paths.get("shop.json"), StandardCharsets.UTF_8);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            InputStream stream = ShopProcessor.class.getClassLoader().getResourceAsStream("shop.json");
            assert stream != null;
            JSONArray jsonArray = new JSONArray(new JSONTokener(stream));
            JSONObject object = new JSONObject();
            object.put("id", item.id());
            object.put("name", item.name());
            object.put("cost", item.cost());
            object.put("description", item.description());
            object.put("type", item.type());
            object.put("multiplier", item.multiplier());
            jsonArray.put(object);
            System.out.println(jsonArray.toString(2));
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("shop.json"))) {
                writer.write(jsonArray.toString(2));
            } catch (IOException e) {
                e.printStackTrace();
            }

            shop.put(item.id(), item);
        }

        public void addRoleToJSON(String role) {
            JSONObject object = new JSONObject();
            object.put(role, 0);
            roles.put(role, 0);

        }
        public List<Item> getShop() {
            return List.copyOf(shop.values());
        }





}
