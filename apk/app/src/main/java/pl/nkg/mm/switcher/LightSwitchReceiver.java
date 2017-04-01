package pl.nkg.mm.switcher;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static pl.nkg.mm.switcher.LightSwitchWidget.EXTRA_WIDGET_ID;

public class LightSwitchReceiver extends BroadcastReceiver {

    public static int latestLampState = -1;
    private static int lastInQueue = -1;
    private static boolean running = false;
    private static long lastQuery;

    @Override
    public void onReceive(Context context, Intent intent) {
        int id = intent.getIntExtra(LightSwitchWidget.EXTRA_WIDGET_ID, 0);
        Log.d("receiver", "ID " + id);
        sendCommand(context, LightSwitchWidget.state ? 0 : 1, id);
    }

    public static void sendCommand(Context context, int command, int widgetId) {
        Log.d("Running", running + ", " + lastInQueue);
        if (running) {
            lastInQueue = command;
            Log.d("Running", "saved in queue: " + command);
        } else {
            Log.d("Running", "do execute: " + command);
            running = true;
            lastInQueue = -1;
            new LightSwitch(context, widgetId).execute(command);
        }
    }

    private static void updateState(Context context, int state, int widgetId) {
        LightSwitchWidget.state = !LightSwitchWidget.state;
        switch (state) {
            case 0:
                LightSwitchWidget.state = true;
                break;

            case 1:
                LightSwitchWidget.state = false;
                break;

            default:
                LightSwitchWidget.state = false;
        }

        LightSwitchWidget.updateAppWidget(context, AppWidgetManager.getInstance(context), widgetId);
    }

    private static class LightSwitch extends AsyncTask<Integer, Void, Integer> {

        private int latestCmd = 0;
        private int tries = 0;
        private Context context;
        private int widgetId;

        public LightSwitch(Context context, int widgetId) {
            super();
            this.context = context;
            this.widgetId = widgetId;
        }

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
                url = new URL("http://192.168.2.200/?cmd=" + cmd);
            } catch (MalformedURLException e) {
                throw new RuntimeException();
            }

            HttpURLConnection urlConnection = null;
            try {
                String content = "cmd=" + cmd + "\r\n";
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(4000);
                urlConnection.setReadTimeout(4000);
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
                        sendCommand(context, latestCmd, widgetId);
                    }
                } else {
                    latestLampState = response;
                }
            }
            updateState(context, latestLampState, widgetId);
            running = false;
            if (lastInQueue != -1) {
                Log.d("Running", "execute from queue: " + lastInQueue);
                sendCommand(context, latestCmd, widgetId);
            }
        }
    }
}
