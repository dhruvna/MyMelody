 package edu.ucsb.ece251.DATQ.mymelody;

 import android.annotation.SuppressLint;
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
    @SuppressLint("StaticFieldLeak")
    private static Activity activity = null;
    private static String accessToken = null;

    public SpotifyService(Activity activity) {
        SpotifyService.activity = activity;
    }

    public void setAccessToken(String token) {
        accessToken = token;
    }

    // Create authentication request
    public void authenticateSpotify(Activity activity) {
        Log.println(Log.VERBOSE, "Starting Auth", "Starting authentication process");
        final AuthorizationRequest request = new AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI)
                .setScopes(new String[]{"user-read-email", "user-read-private", "user-read-recently-played", "playlist-read-private", "user-top-read", "user-follow-read"})
                .setShowDialog(true)
                .build();

        AuthorizationClient.openLoginInBrowser(activity, request);
    }
    public String handleAuthResponse(Intent intent) {
        Uri uri = intent.getData();
        if (uri != null) {
            AuthorizationResponse response = AuthorizationResponse.fromUri(uri);
            if (response.getType() == AuthorizationResponse.Type.TOKEN) {
                String token = response.getAccessToken();
                setAccessToken(token);
                Log.println(Log.VERBOSE, "Finished Auth", "Successfully completed authentication process");
                return token;
            }
        }
        return null;
    }

    //Get User Info
    public interface FetchUserInfoCallback {
        void onUserInfoFetched(User user);
        void onError();
    }
    public void fetchUserInfo(String AccessToken, FetchUserInfoCallback callback) {
        new Thread(() -> {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://api.spotify.com/v1/me")
                    .addHeader("Authorization", "Bearer " + AccessToken)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    JSONObject jsonObject = new JSONObject(responseData);
                    // Extract user information from the JSON object
                    String id = jsonObject.getString("id");
                    String username = jsonObject.has("display_name") ? jsonObject.getString("display_name") : "";
                    String email = jsonObject.has("email") ? jsonObject.getString("email") : "";
                    String profileLink = jsonObject.getJSONObject("external_urls").getString("spotify");
                    String imageUrl = "";
                    JSONArray images = jsonObject.getJSONArray("images");
                    if(images.length() > 0) {
                        JSONObject image = images.getJSONObject(1);
                        imageUrl = image.getString("url");
                    }
                    User user = new User(id, username, email, profileLink, imageUrl, AccessToken);
                    // Use Handler to run on UI thread
                    activity.runOnUiThread(() -> callback.onUserInfoFetched(user));
                } else {
                    // Run on the main thread
                    activity.runOnUiThread(callback::onError);
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

    //Tracks/Artists/etc calls
    public interface FetchTrackCallback {
        void onTrackFetched(String tracks);
        void onError();
    }
    public void fetchUserTopTracks(String accessToken, int rangeSetting, FetchTrackCallback callback) {
        new Thread(() -> {
            OkHttpClient client = new OkHttpClient();
            String url = "";
            switch(rangeSetting) {
                case 0:
                    url = "https://api.spotify.com/v1/me/top/tracks?time_range=short_term";
                    break;
                case 1:
                    url = "https://api.spotify.com/v1/me/top/tracks?time_range=medium_term";
                    break;
                case 2:
                    url = "https://api.spotify.com/v1/me/top/tracks?time_range=long_term";
                    break;
            }
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if(response.isSuccessful() && response.body() != null) {
                    Log.println(Log.VERBOSE, "Track Fetcher", "Received response for tracks!");
                    String responseData = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseData);
                    // Extract user information from the JSON object
                    JSONArray items = jsonResponse.getJSONArray("items");
                    Log.println(Log.VERBOSE, "num tracks", "received " + items.length() + "track");
                    if (items.length() > 0) {
                        StringBuilder tracks = new StringBuilder("" + items.length() + "%20");
                        for(int i = 0; i < items.length(); i++) {
                            JSONObject track = items.getJSONObject(i);
                            tracks.append(track.getString("name"));
                            tracks.append("%20");
                        }
                        String finalTracks = tracks.toString();
                        // Use Handler to run on UI thread
                        Log.println(Log.VERBOSE, "Finished track fetch", "Ready to run on main thread");
                        activity.runOnUiThread(() -> callback.onTrackFetched(finalTracks));
                    } else {
                        // Run on the main thread
                        activity.runOnUiThread(() -> showToast("No top tracks found"));
                    }
                }
            } catch (Exception e) {
                // Run on the main thread
                activity.runOnUiThread(callback::onError);
            }
        }).start();
    }

    public interface FetchArtistCallback {
        void onArtistFetched(String artists);
        void onError();
    }
    public void fetchUserTopArtists(String accessToken, FetchArtistCallback callback) {
        new Thread(() -> {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://api.spotify.com/v1/me/top/artists")
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if(response.isSuccessful() && response.body() != null) {
                    Log.println(Log.VERBOSE, "Artist Fetcher", "Received response for artists!");
                    String responseData = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseData);
                    // Extract user information from the JSON object
                    JSONArray items = jsonResponse.getJSONArray("items");
                    Log.println(Log.VERBOSE, "num artists", "received " + items.length() + "artists");
                    if (items.length() > 0) {
                        StringBuilder artists = new StringBuilder("" + items.length() + "%20");
                        for(int i = 0; i < items.length(); i++) {
                            JSONObject artist = items.getJSONObject(i);
                            artists.append(artist.getString("name"));
                            artists.append("%20");
                        }
                        String finalArtists = artists.toString();
                        // Use Handler to run on UI thread
                        Log.println(Log.VERBOSE, "Finished artist fetch", "Ready to run on main thread");
                        activity.runOnUiThread(() -> callback.onArtistFetched(finalArtists));
                    } else {
                        // Run on the main thread
                        activity.runOnUiThread(() -> showToast("No top artists found"));
                    }
                }
            } catch (Exception e) {
                // Run on the main thread
                activity.runOnUiThread(callback::onError);
            }
        }).start();
    }

    private static void showToast(String message) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }

}
