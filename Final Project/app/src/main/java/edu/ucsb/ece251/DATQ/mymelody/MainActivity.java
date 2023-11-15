package edu.ucsb.ece251.DATQ.mymelody;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.webkit.WebView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Spotify Service
        SpotifyService spotifyService = new SpotifyService();

        // Initialize WebView for Google Charts
        WebView googleChartWebView = findViewById(R.id.google_chart_webview);
//        GoogleChartsWebView.setupWebView(googleChartWebView);
    }
}
