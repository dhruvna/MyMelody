package edu.ucsb.ece251.DATQ.mymelody;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.content.Intent;
import android.widget.Toast;
import android.widget.Button;
import android.widget.TextView;

import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;
import com.spotify.sdk.android.auth.AuthorizationClient;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private TextView LoginStatus;
    private Button LoginButton;
    private SpotifyService spotifyService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LoginStatus = findViewById(R.id.LoginStatus);
        LoginButton = findViewById(R.id.LoginButton);
        //Initialize Spotify Service
        spotifyService = new SpotifyService(this);
        LoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                spotifyService.authenticateSpotify();
            }
        });

        // Initialize WebView for Google Charts
//        WebView googleChartWebView = findViewById(R.id.google_chart_webview);
//        GoogleChartsWebView.setupWebView(googleChartWebView);
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        boolean loginSuccess = spotifyService.handleAuthResponse(intent);

        if(loginSuccess) {
            LoginStatus.setText("Login Successful!");
        } else {
            LoginStatus.setText("Login failed. Try again!");
        }
    }
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
