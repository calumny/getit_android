package com.get;

//import android.support.v7.app.AppCompatActivity;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

public class MapActivity extends Activity {

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MapActivity .this);
// then you use
        Boolean gotIt = prefs.getBoolean(getResources().getString(R.string.prefs_got_it_key), false);

        if (gotIt) {
            setContentView(R.layout.activity_map_blue);
        } else {
            setTheme(R.style.RedAppTheme);
            setContentView(R.layout.activity_map);
        }

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        WebView webView = (WebView) findViewById(R.id.mapView);
        webView.getSettings().setJavaScriptEnabled(true);
        if (gotIt) {
            webView.setBackgroundColor(getResources().getColor(R.color.darkBlue));
        } else {
            webView.setBackgroundColor(getResources().getColor(R.color.darkPurple));
        }
//        webView.getSettings().setGeolocationEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webView.getSettings().setAppCacheEnabled(true);
        Double lat = getIntent().getDoubleExtra("LAT", 42.3601);
        Double lon = getIntent().getDoubleExtra("LON", -71.0589);
        String url  = "http://ec2-52-5-22-160.compute-1.amazonaws.com:81/?lat=" + lat + "&lon=" + lon  + "&got_it=" + gotIt ;
        webView.loadUrl(url);
        webView.setWebViewClient( new AppWebViewClients(progressBar));

    }
    public class AppWebViewClients extends WebViewClient {
        private ProgressBar progressBar;

        public AppWebViewClients(ProgressBar progressBar) {
            this.progressBar=progressBar;
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            // TODO Auto-generated method stub
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
     public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.activity_slide_in_reverse, R.anim.activity_slide_out_reverse);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_map, menu);
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

        if (id == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            overridePendingTransition(R.anim.activity_slide_in_reverse, R.anim.activity_slide_out_reverse);
            return true;
        }

        return false;
    }
}
