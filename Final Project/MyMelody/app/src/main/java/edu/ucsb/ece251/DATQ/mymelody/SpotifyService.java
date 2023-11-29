package edu.ucsb.ece251.DATQ.mymelody;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SpotifyService {

    private static final int REQUEST_CODE = 1337;
    private static final String REDIRECT_URI = "mymelody://callback";
    private static final String CLIENT_ID = "44d8159e766e496f9b8ce905397518af";
    private static Activity activity = null;
    private static String accessToken = null;

    public SpotifyService(Activity activity) {
        this.activity = activity;
    }

    public static void setAccessToken(String token) {
        accessToken = token;
        showToast(token);
    }

    // Create authentication request
    public void authenticateSpotify(Activity activity) {
        Log.println(Log.VERBOSE, "startauth", "Starting authentication process");
        final AuthorizationRequest request = new AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI)
                .setScopes(new String[]{"user-read-private", "playlist-read", "playlist-read-private", "streaming"})
                .build();

        AuthorizationClient.openLoginActivity(activity, REQUEST_CODE, request);
    }

    public static boolean onActivityResult(int requestCode, int resultCode, Intent intent) {
        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, intent);
            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    // Handle successful response
                    String token = response.getAccessToken();
                    setAccessToken(token);
                    showToast(token);
                    return true;
                // Auth flow returned an error
                case ERROR:
                    // Handle error response
                    return false;

                // Most likely auth flow was cancelled
                default:
                    // Handle other cases
                    return false;
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
            AuthorizationClient.clearCookies(activity);
            return true;
        }
        return false;
    }
    private static void showToast(String message) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }
}
