 package edu.ucsb.ece251.DATQ.mymelody;

 import android.app.Activity;
 import android.content.Intent;
 import android.net.Uri;
 import android.util.Log;
 import android.widget.Toast;

 import com.spotify.sdk.android.auth.AuthorizationClient;
 import com.spotify.sdk.android.auth.AuthorizationRequest;
 import com.spotify.sdk.android.auth.AuthorizationResponse;

 import org.json.JSONArray;
 import org.json.JSONObject;

 import okhttp3.OkHttpClient;
 import okhttp3.Request;
 import okhttp3.Response;

public class SpotifyService {

    private static final String REDIRECT_URI = "mymelody://callback";
    private static final String CLIENT_ID = "44d8159e766e496f9b8ce905397518af";
    private static Activity activity = null;
    private static String accessToken = null;

    public SpotifyService(Activity activity) {
        SpotifyService.activity = activity;
    }

    public void setAccessToken(String token) {
        accessToken = token;
        showToast(accessToken);
    }
    // Create authentication request
    public void authenticateSpotify(Activity activity) {
        Log.println(Log.VERBOSE, "startauth", "Starting authentication process");
        final AuthorizationRequest request = new AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI)
                .setScopes(new String[]{"user-read-email", "user-read-private", "user-read-recently-played", "playlist-read-private", "user-top-read", "user-follow-read"})
                .setShowDialog(true)
                .build();

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

    public interface FetchTrackCallback {
        void onTrackFetched(String tracks);
        void onError();
    }
    public void fetchUserTopTracks(FetchTrackCallback callback) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                String url = "https://api.spotify.com/v1/me/top/tracks";
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .build();
                Log.println(Log.VERBOSE, "response", request.toString());
                Log.d("SpotifyService", "Fetching top tracks");
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseData = response.body().string();
                        JSONObject jsonObject = new JSONObject(responseData);
                        JSONArray items = jsonObject.getJSONArray("items");

                        if (items.length() > 0) {
                            Log.d("TOP TRACKS", "GOT SOMETHING?");
                            String tracks = "" + items.length() + "%20";
                            for(int i = 0; i < items.length(); i++) {
                                JSONObject track = items.getJSONObject(i);
                                tracks += track.getString("name");
                                tracks += "%20";
                            }
                            // Display the name of the top track in a Toast
                            String finalTracks = tracks;
                            activity.runOnUiThread(() -> callback.onTrackFetched(finalTracks));
                        } else {
                            activity.runOnUiThread(() -> showToast("No top tracks found"));
                        }
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
    public interface FetchUserInfoCallback {
        void onUserInfoFetched(User user);
        void onError();
    }

    public void fetchUserInfo(FetchUserInfoCallback callback) {
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url("https://api.spotify.com/v1/me")
                        .addHeader("Authorization", "Bearer " + accessToken)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseData = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseData);
                        String username = jsonResponse.getString("display_name");
                        String email = jsonResponse.getString("email");
                        String id = jsonResponse.getString("id");
                        String profileUrl = jsonResponse.getJSONObject("external_urls").getString("spotify");
                        String imageUrl = "";
                        JSONArray images = jsonResponse.getJSONArray("images");
                        if(images != null && images.length() > 0) {
                            JSONObject image = images.getJSONObject(1);
                            imageUrl = image.getString("url");
                        }
                        User user = new User(id, username, email, profileUrl, imageUrl);
                        // Run on the main thread
                        activity.runOnUiThread(() -> callback.onUserInfoFetched(user));
                    } else {
                        // Run on the main thread
                        activity.runOnUiThread(callback::onError);

                    }
                }
            } catch (Exception e) {
                // Run on the main thread
                activity.runOnUiThread(callback::onError);
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
//

}
