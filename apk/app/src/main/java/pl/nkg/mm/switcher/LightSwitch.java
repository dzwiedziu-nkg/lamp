package pl.nkg.mm.switcher;

import org.json.JSONObject;

import android.app.DownloadManager;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class LightSwitch implements Runnable {

    public static int latestLampState = -1;

    @Override
    public void run() {
        URL url = null;
        try {
            url = new URL("http://192.168.2.200/?pin=13");
        } catch (MalformedURLException e) {
            throw new RuntimeException();
        }

        HttpURLConnection urlConnection = null;
        try {
            String content = "pin=13";
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.setInstanceFollowRedirects(false);
            urlConnection.setRequestProperty( "Content-Length", content);
            urlConnection.setUseCaches( false );
            DataOutputStream wr = new DataOutputStream( urlConnection.getOutputStream());
            wr.write(content.getBytes());

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());

            in.close();
        } catch (IOException e) {
            //Log.e("run", "run", e);
        } finally {
            urlConnection.disconnect();
        }
    }
}
