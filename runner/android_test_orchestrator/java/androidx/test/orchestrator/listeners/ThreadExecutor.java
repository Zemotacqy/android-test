package androidx.test.orchestrator.listeners;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

public class ThreadExecutor implements Runnable {

    private static String TAG = "THREAD_EXECUTOR";
    private String requestBody;


    public ThreadExecutor(String requestBody) {
        this.requestBody = requestBody;
    }

    @Override
    public void run() {
//        Log.d(TAG, "postData: " + this.requestBody );

        String response = "";

        Random r = new Random();
        try {
            Thread.sleep(r.nextInt(10)*1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        try {
            Log.d(TAG, "Making Request for: " + this.requestBody );
            URL url = new URL("https://868d-110-226-182-90.ngrok-free.app");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            OutputStream os = conn.getOutputStream();
            os.write(this.requestBody.getBytes("UTF-8"));
            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                StringBuilder responseBuilder = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    responseBuilder.append(inputLine);
                }
                in.close();
                response = responseBuilder.toString();
            } else {
                response = "Error: " + responseCode;
            }
        } catch (IOException e) {
            e.printStackTrace();
            response = "Error: " + e.getMessage();
        }
        Log.d(TAG, "Response: " + response);
    }
}
