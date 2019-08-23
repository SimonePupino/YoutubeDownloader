package com.example.youtubedownloader;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Environment;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    ProgressDialog p;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        final EditText urlField = findViewById(R.id.urlField);
        Button searchButton = findViewById(R.id.searchButton);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String urlStr = urlField.getText().toString();
                if (urlStr.isEmpty())
                    Snackbar.make(view, "No url provided", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                else {
                    GetHttpResponse asyncTask = new GetHttpResponse();
                    asyncTask.execute("https://getvideo.p.rapidapi.com/?url=" + urlStr);


                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class GetHttpResponse extends AsyncTask<String, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            p = new ProgressDialog(MainActivity.this);
            p.setMessage("Downloading file.");
            //p.setIndeterminate(false);
            p.setCancelable(false);
            p.setButton(DialogInterface.BUTTON_NEGATIVE, "Go Back", (DialogInterface.OnClickListener) null);
            p.setMax(100);
            p.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            p.show();
        }
        @Override
        protected String doInBackground(String... strings) {
            HttpResponse<String> response = null;

            try {
                response = Unirest.get(strings[0])
                        .header("x-rapidapi-host", "getvideo.p.rapidapi.com")
                        .header("x-rapidapi-key", "4092c2672cmsha96bbd4cc6e3b12p1b6650jsn3e6c93e601f1").asString();
            }
            catch (UnirestException e) {
                e.printStackTrace();
            }

            if (response.getCode() != 200)
                return "Response code was " + response.getCode() + ". Please retry.";



            // Review those variables
            JSONObject body;
            JSONArray streams;
            String filename;
            List<String> downloadUrl = new ArrayList<>();

            try {
                body = new JSONObject(response.getBody());
                streams = body.getJSONArray("streams");
                filename = body.getString("title") + ".m4a";
                //filename = body.getJSONObject("title").toString() + ".m4a";

                for (int i = 0; i < streams.length(); i++) {
                    if (streams.getJSONObject(i).get("format").equals("audio only") && streams.getJSONObject(i).get("extension").equals("m4a"))
                        downloadUrl.add(streams.getJSONObject(i).get("url").toString());
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return "There was a problem during JSON parsing. Please retry.";
            }

            // Save file to internal card using filename and download urls
            int count;
            try {

                // it will save the file in music folder instead of youtubedownloader subfolder
                File mFolder = new File(Environment
                        .getExternalStorageDirectory().toString() + "/Music/YoutubeDownloader");
                //File imgFile = new File(mFolder.getAbsolutePath() + filename);
                File imgFile = new File(mFolder.getAbsolutePath() + "/" + filename);

                if (!mFolder.exists()) {
                    mFolder.mkdir();
                }
                if (!imgFile.exists()) {
                    imgFile.createNewFile();
                }

                URL url = new URL(downloadUrl.get(0));
                URLConnection connection = url.openConnection();
                connection.connect();

                int lengthOfFile = connection.getContentLength();

                // download the file
                InputStream input = new BufferedInputStream(url.openStream(),
                        8192);
                // Output stream
                /*OutputStream output = new FileOutputStream(Environment
                        .getExternalStorageDirectory().toString()
                        + "/Music/YoutubeDownloader/" + filename);*/
                OutputStream output = new FileOutputStream(imgFile);

                byte data[] = new byte[1024];
                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    publishProgress("" + (int) ((total * 100) / lengthOfFile));

                    // writing data to file
                    output.write(data, 0, count);
                }
                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();

            } catch (Exception e) {
                e.printStackTrace();
                return "There was a problem during file download. Please retry.";
            }
            return "File downloaded!";
        }

        @Override
        protected void onPostExecute(String response) {
            super.onPostExecute(response);
            p.setMessage(response);
        }

        protected void onProgressUpdate(String... progress) {
            // setting progress percentage
            p.setProgress(Integer.parseInt(progress[0]));
        }

    }
}


