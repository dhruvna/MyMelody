package edu.ucsb.ece251.DATQ.mymelody;

import android.webkit.WebView;
import android.webkit.WebViewClient;

public class GoogleChartsWebView {
    public static void setupWebView(WebView webView) {
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setJavaScriptEnabled(true);
        // Load a URL that hosts your Google Chart
        webView.loadUrl("file:///android_asset/google_chart.html");
    }
}
