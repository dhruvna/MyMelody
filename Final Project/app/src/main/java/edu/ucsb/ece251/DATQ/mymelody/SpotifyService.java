package edu.ucsb.ece251.DATQ.mymelody;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;


public class SpotifyService {
    private static final String REDIRECT_URI = "mymelody://callback";
    private static final String CLIENT_ID = "44d8159e766e496f9b8ce905397518af";
    private final Activity activity;

    public SpotifyService(Activity activity) {
        this.activity = activity;
    }

    // Create authentication request
    public void authenticateSpotify() {
        Log.println(Log.VERBOSE, "startauth", "Starting authentication process");
        AuthorizationRequest.Builder builder =
                new AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI);

        builder.setScopes(new String[]{"user-read-private", "playlist-read", "playlist-read-private", "streaming"});

        AuthorizationRequest request = builder.build();

        AuthorizationClient.openLoginInBrowser(activity, request);
    }

    public boolean handleAuthResponse(Intent intent) {
        Uri uri = intent.getData();
        if (uri != null) {
            AuthorizationResponse response = AuthorizationResponse.fromUri(uri);
            if (response.getType() == AuthorizationResponse.Type.TOKEN) {
                showToast(response.getAccessToken());
                return true;
            }
        }
        return false;
    }
    private void showToast(String message) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }
}
