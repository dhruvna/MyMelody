package edu.ucsb.ece251.DATQ.mymelody;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

import kotlin.jvm.optionals.OptionalsKt;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SpotifyService {
    private static final String REDIRECT_URI = "mymelody://callback";
    private static final String CLIENT_ID = "44d8159e766e496f9b8ce905397518af";
    private final Activity activity;

    private String accessToken = null;

    public SpotifyService(Activity activity) {
        this.activity = activity;
    }

    public void setAccessToken(String token) {
        this.accessToken = token;
        showToast(token);
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
                String token = response.getAccessToken();
                setAccessToken(token);
                return true;
            }
        }
        return false;
    }

    public void fetchUserTopTracks() {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                String url = "https://api.spotify.com/v1/me/top/tracks";
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .build();
                Log.println(Log.VERBOSE, "response",  request.toString());
                Log.d("SpotifyService", "Fetching top tracks");
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseData = response.body().string();
                        Log.d("SpotifyService", "Response data: " + responseData);
                        showToast("Tracks fetched");
                        // Parse and process the JSON response
                    } else {
                        Log.e("SpotifyService", "Unsuccessful response");
                    }
                } catch (Exception e) {
                    Log.e("SpotifyService", "Error fetching top tracks", e);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public boolean logOut() {
        if(accessToken != null) {
            accessToken = null;
            return true;
        }
        return false;
    }
    private void showToast(String message) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }
}
