package com.example.john8324.cartravelclient;

import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.WebView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Scanner;

/**
 * Created by john8324 on 2016/10/29.
 */
class CarDataTask extends AsyncTask<Void, JSONObject, Void> {

    private AssetManager assetManager;
    private TextView textView;
    private WebView webView;
    private final String dateString = "20160827_102232";
    private final int vehicleID = 2584, vehicleType = 0;

    private final String[] match = {"Time (sec)", "Latitude", "Longitude", "Vehicle speed"};
    private final String[] actualKey = {"start_time", "latitude", "longitude", "vehicle_speed"};


    CarDataTask(AssetManager assetManager, TextView textView, WebView webView) {
        this.assetManager = assetManager;
        this.textView = textView;
        this.webView = webView;
    }

    private void actualPost(httpposter login, String apiFun, JSONObject jsonObject) {
        publishProgress(jsonObject);
        String postString = "apiFun=" + apiFun + "&json=" + jsonObject.toString();
        Log.d("postString", postString);
        login.doPost("http://140.113.216.201/carInfoApi.php", postString, login.cookie, "utf-8");
    }

    private void markAndMove(double latitude, double longitude, double speed) {
        webView.loadUrl("javascript:addNode({'latitude':'" + latitude + "','longitude':'" + longitude + "','vehicle_speed':'" + speed + "'});");
    }

    @Override
    protected Void doInBackground(Void... params) {
        Scanner input = openCsv();
        if (input == null) {
            return null;
        }

        // Login the server
        httpposter login = new httpposter();
        login.login();

        String[] key = null;
        boolean[] pass = null;
        while (!isCancelled() && input.hasNextLine()) {
            String[] row = input.nextLine().split(",");
            // Log.d("", "Len = " + cell.length);
            if (row.length == 1 && row[0].startsWith("#")) {
                continue;
            }
            if (key == null) {
                key = row;
                pass = new boolean[key.length];
                for (int i = 0; i < key.length; ++i) {
                    for (int j = 0; j < match.length; ++j) {
                        pass[i] = pass[i] || key[i].contains(match[j]);
                        if (pass[i]) {
                            key[i] = actualKey[j];
                            break;
                        }
                    }
                }

                final String startDate = dateString.substring(0, 4)+"-"+dateString.substring(4, 6)+"-"+dateString.substring(6, 8)+" "+dateString.substring(9, 11)+":"+dateString.substring(11, 13)+":"+dateString.substring(13, 15); // 2016-08-27 10:22:32
                JSONObject jsonObject = new JSONObject();
                try{
                    jsonObject.put("vehicle_id", vehicleID);
                    jsonObject.put("vehicle_type", vehicleType);
                    jsonObject.put("start_date", startDate);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                actualPost(login, "pathAdd", jsonObject);

            } else {
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("vehicle_id", vehicleID);
                    for (int i = 0; i < key.length; ++i) {
                        if (pass[i]) {
                            jsonObject.put(key[i], row[i]);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                actualPost(login, "infoAdd", jsonObject);

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        input.close();
        return null;
    }

    @Override
    protected void onProgressUpdate(JSONObject... values) {
        super.onProgressUpdate(values);
        double latitude = 0, longitude = 0, speed = 0;
        textView.setText(values[0].toString());
        try {
            latitude = Double.parseDouble((String)values[0].get(actualKey[1]));
            longitude = Double.parseDouble((String)values[0].get(actualKey[2]));
            speed = Double.parseDouble((String)values[0].get(actualKey[3]));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        markAndMove(latitude, longitude, speed);
    }

    private Scanner openCsv() {
        try {
            return new Scanner(assetManager.open("CSVLog_" + dateString + ".csv"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
