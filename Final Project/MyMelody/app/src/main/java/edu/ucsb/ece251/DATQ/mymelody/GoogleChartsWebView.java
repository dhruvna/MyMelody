package edu.ucsb.ece251.DATQ.mymelody;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;

public class GoogleChartsWebView extends AppCompatActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.google_charts);
        WebView webView = findViewById(R.id.googleCharts);
        webView.getSettings().setJavaScriptEnabled(true);
        // Load a URL that hosts your Google Chart
        webView.loadUrl("file:///android_asset/charts.html");

        webView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                String genreDataJSArray = "[['Genre', 'Count'], ['Pop', 10], ['Rock', 15], ['Jazz', 5], ['EDM', 7], ['Rap', 2]]";

                webView.evaluateJavascript("drawChart(" + genreDataJSArray + ");", null);
            }
        });
    }
}
