package edu.ucsb.ece251.DATQ.mymelody;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.content.Intent;
import android.widget.Toast;

import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;
import com.spotify.sdk.android.auth.AuthorizationClient;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE = 420;
    private static final String REDIRECT_URI = "mymelody://callback";
    private static final String CLIENT_ID = "44d8159e766e496f9b8ce905397518af";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authenticateSpotify();

//         Initialize Spotify Service
        SpotifyService spotifyService = new SpotifyService();

        // Initialize WebView for Google Charts
//        WebView googleChartWebView = findViewById(R.id.google_chart_webview);
//        GoogleChartsWebView.setupWebView(googleChartWebView);
    }

    private void authenticateSpotify() {
        Log.println(Log.VERBOSE, "startauth", "Starting authentication process");
        AuthorizationRequest.Builder builder =
                new AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI);

        builder.setScopes(new String[]{"user-read-private", "playlist-read", "playlist-read-private", "streaming"});

        AuthorizationRequest request = builder.build();

        AuthorizationClient.openLoginInBrowser(this, request);
    }
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Uri uri = intent.getData();
        if (uri != null) {
            AuthorizationResponse response = AuthorizationResponse.fromUri(uri);
            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    // Handle successful response
                    String token = response.getAccessToken();
                    showToast(token);
                    break;

                // Auth flow returned an error
                case ERROR:
                    // Handle error response
                    break;

                // Most likely auth flow was cancelled
                default:
                    // Handle other cases
            }
        }
    }
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
