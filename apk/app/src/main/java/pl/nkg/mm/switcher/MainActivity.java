package pl.nkg.mm.switcher;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class MainActivity extends AppCompatActivity {

    public static int latestLampState = -1;

    private Button buttonOn;
    private Button buttonOff;

    private int lastInQueue = -1;
    private boolean running = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_WIFI_STATE},
                        0);
        }


        buttonOn = (Button) findViewById(R.id.button_on);
        buttonOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommand(1);
            }
        });

        buttonOff = (Button) findViewById(R.id.button_off);
        buttonOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommand(0);
            }
        });

        updateState(latestLampState);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (latestLampState == -1) {
            sendCommand(9);
        }
    }

    private void sendCommand(int command) {
        Log.d("Running", running + ", " + lastInQueue);
        if (running) {
            lastInQueue = command;
            Log.d("Running", "saved in queue: " + command);
        } else {
            Log.d("Running", "do execute: " + command);
            running = true;
            lastInQueue = -1;
            new LightSwitch().execute(command);
        }
    }

    private void updateState(int state) {
        TextView textView = (TextView) findViewById(R.id.textView);
        switch (state) {
            case 0:
                textView.setText("świeci");
                buttonOn.setEnabled(false);
                buttonOff.setEnabled(true);
                break;

            case 1:
                textView.setText("zgaszona");
                buttonOn.setEnabled(true);
                buttonOff.setEnabled(false);
                break;

            default:
                textView.setText("...");
                buttonOn.setEnabled(true);
                buttonOff.setEnabled(true);
        }
    }

    private static long lastQuery;

    private class LightSwitch extends AsyncTask<Integer, Void, Integer> {

        private int latestCmd = 0;
        private int tries = 0;

        @Override
        protected Integer doInBackground(Integer... integers) {
            int cmd = integers[0];
            latestCmd = cmd;
            Integer response = null;
            Log.d("AsyncTask", "cmd=" + cmd);

            /*if (System.currentTimeMillis() - lastQuery < 2000) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }*/

            URL url = null;
            try {
                url = new URL("http://192.168.0.24/?cmd=" + cmd);
            } catch (MalformedURLException e) {
                throw new RuntimeException();
            }

            HttpURLConnection urlConnection = null;
            try {
                String content = "cmd=" + cmd + "\r\n";
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(3000);
                urlConnection.setReadTimeout(3000);
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);
                urlConnection.setInstanceFollowRedirects(false);
                urlConnection.setRequestProperty( "Content-Length", content);
                urlConnection.setUseCaches( false );
                DataOutputStream wr = new DataOutputStream( urlConnection.getOutputStream());
                wr.write(content.getBytes());


                InputStream in = urlConnection.getInputStream();
                int state = in.read();
                in.close();

                if (state != -1) {
                    response = state - 48;
                }
                Log.d("AsyncTask", "response=" + response);


            } catch (IOException e) {
                //Log.e("run", "run", e);
                Log.d("AsyncTask", "error on request", e);
                response = -1;
            } finally {
                urlConnection.disconnect();
            }

            return response;
        }

        @Override
        protected void onPostExecute(Integer response) {
            lastQuery = System.currentTimeMillis();

            if (response != null) {
                if (response == -1) {
                    if (tries < 3 && lastInQueue == -1) {
                        tries++;
                        sendCommand(latestCmd);
                    }
                } else {
                    latestLampState = response;
                }
            }
            updateState(latestLampState);
            running = false;
            if (lastInQueue != -1) {
                Log.d("Running", "execute from queue: " + lastInQueue);
                sendCommand(lastInQueue);
            }
        }
    }
}
