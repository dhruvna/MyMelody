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

 import java.io.IOException;

 import okhttp3.OkHttpClient;
 import okhttp3.Request;
 import okhttp3.RequestBody;
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
                .setScopes(new String[]{"user-modify-playback-state", "user-read-playback-state", "user-read-email", "user-read-private", "user-read-recently-played", "playlist-read-private", "user-top-read", "user-follow-read", "user-read-currently-playing"})
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
    public interface FetchTrackDetailsCallback {
        void onTrackDetailsFetched(Track track);
        void onError();
    }

    public interface FetchArtistDetailsCallback {
        void onArtistDetailsFetched(Artist artist);
        void onError();
    }


    public void fetchTrackDetails(String trackId, FetchTrackDetailsCallback fetchTrackDetailsCallback) {
        new Thread(() -> {
            OkHttpClient client = new OkHttpClient();
            String url = "https://api.spotify.com/v1/tracks/" + trackId;

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    JSONObject trackJson = new JSONObject(responseData);

                    // Parse the JSON object to create a Track object
                    String id = trackJson.getString("id");
                    String name = trackJson.getString("name");

                    Track track = new Track(id, name, 0);

                    // Use Handler to run on UI thread
                    activity.runOnUiThread(() -> fetchTrackDetailsCallback.onTrackDetailsFetched(track));
                } else {
                    // Handle response failure
                    activity.runOnUiThread(fetchTrackDetailsCallback::onError);
                }
            } catch (Exception e) {
                Log.e("SpotifyService", "Error fetching track details: " + e.getMessage());
                activity.runOnUiThread(fetchTrackDetailsCallback::onError);
            }
        }).start();
    }

    public void fetchArtistDetails(String artistId, FetchArtistDetailsCallback fetchArtistDetailsCallback) {
        new Thread(() -> {
            OkHttpClient client = new OkHttpClient();
            String url = "https://api.spotify.com/v1/artists/" + artistId;

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    JSONObject trackJson = new JSONObject(responseData);

                    // Parse the JSON object to create a Track object
                    String id = trackJson.getString("id");
                    String name = trackJson.getString("name");

                    Artist artist = new Artist(id, name, 0);

                    // Use Handler to run on UI thread
                    activity.runOnUiThread(() -> fetchArtistDetailsCallback.onArtistDetailsFetched(artist));
                } else {
                    // Handle response failure
                    activity.runOnUiThread(fetchArtistDetailsCallback::onError);
                }
            } catch (Exception e) {
                Log.e("SpotifyService", "Error fetching artist details: " + e.getMessage());
                activity.runOnUiThread(fetchArtistDetailsCallback::onError);
            }
        }).start();
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
    public void fetchUserTopTracks(String accessToken, int rangeSetting, int numTracks, FetchTrackCallback callback) {
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
            url += "&limit=" + numTracks;
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
                            tracks.append("%21");
                            tracks.append(track.getString("id"));
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
    public void fetchUserTopArtists(String accessToken, int rangeSetting, int numArtists, FetchArtistCallback callback) {
        new Thread(() -> {
            OkHttpClient client = new OkHttpClient();
            String url = "";
            switch(rangeSetting) {
                case 0:
                    url = "https://api.spotify.com/v1/me/top/artists?time_range=short_term";
                    break;
                case 1:
                    url = "https://api.spotify.com/v1/me/top/artists?time_range=medium_term";
                    break;
                case 2:
                    url = "https://api.spotify.com/v1/me/top/artists?time_range=long_term";
                    break;
            }
            url += "&limit=" + numArtists;
            Request request = new Request.Builder()
                    .url(url)
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
                            artists.append("%21");
                            artists.append(artist.getString("id"));
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

    public interface FetchSongCallback {
        void onSongFetched(String songName, String artistName, String albumArtUrl, int progress, int duration);
        void onError();
    }
    public void fetchCurrentSong(FetchSongCallback callback) {
        new Thread(() -> {
            OkHttpClient client = new OkHttpClient();
            String url = "https://api.spotify.com/v1/me/player/currently-playing?market=US";
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if(!response.isSuccessful()) {
                    showToast("No active session");
                    return;
                }
                String responseData = response.body() != null ? response.body().string() : "";
                if (!responseData.isEmpty() && !responseData.equals("EMPTY_RESPONSE")) {
                    Log.println(Log.VERBOSE, "Current Song Fetcher", "Received response for current song");
                    JSONObject jsonResponse = new JSONObject(responseData);
                    if (jsonResponse.isNull("item")) {
                        showToast("No song currently playing.");
                        return;
                    }
                    // Extract track information
                    JSONObject track = jsonResponse.getJSONObject("item");
                    String trackName = track.getString("name");
                    JSONArray artistsArray = track.getJSONArray("artists");
                    String artistName = artistsArray.getJSONObject(0).getString("name"); // Assuming first artist
                    JSONObject album = track.getJSONObject("album");
                    String albumArtUrl = album.getJSONArray("images").getJSONObject(0).getString("url"); // Assuming first image
                    int progress = jsonResponse.getInt("progress_ms");
                    int duration = track.getInt("duration_ms");
                    // Use Handler to run on UI thread
                    activity.runOnUiThread(() -> callback.onSongFetched(trackName, artistName, albumArtUrl, progress, duration));
                } else {
                    // Handle response failure
                    showToast("No song currently playing.");
                }
            } catch (Exception e) {
                Log.println(Log.ERROR, "Track Fetcher", "Error fetching current track: " + e.getMessage());
                activity.runOnUiThread(callback::onError);
            }
        }).start();
    }

    public interface FetchDeviceIdCallback {
        void onDeviceIdFetched(String deviceId);
        void onError();
    }
    public void fetchCurrentDeviceId(String accessToken, FetchDeviceIdCallback callback) {
        new Thread(() -> {
            OkHttpClient client = new OkHttpClient();
            String url = "https://api.spotify.com/v1/me/player";

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseData);
                    if (!jsonResponse.isNull("device")) {
                        JSONObject device = jsonResponse.getJSONObject("device");
                        String deviceId = device.getString("id");
                        activity.runOnUiThread(() -> callback.onDeviceIdFetched(deviceId));
                    } else {
                        activity.runOnUiThread(callback::onError);
                    }
                } else {
                    activity.runOnUiThread(callback::onError);
                }
            } catch (Exception e) {
                activity.runOnUiThread(callback::onError);
            }
        }).start();
    }


    public interface playPauseCallback {
        void onPlayPauseSuccess();
        void onError();
    }
    public void playPause(String accessToken, boolean isPlaying, playPauseCallback callback) {
        new Thread(() -> {
            OkHttpClient client = new OkHttpClient();
            String url = "https://api.spotify.com/v1/me/player";
            RequestBody body = RequestBody.create("", null);

            Request.Builder builder = new Request.Builder()
                    .url(url)
                    .put(RequestBody.create("", null))
                    .addHeader("Authorization", "Bearer " + accessToken);
            if(isPlaying) {
                builder.url(url + "/pause").put(body);
            } else {
                builder.url(url + "/play").put(body);
            }
            Request request = builder.build();
            Log.println(Log.VERBOSE, "Play/Pause Request", "Sending: " + request);
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    Log.println(Log.VERBOSE, "play/pause success", "WE DID IT");
                    activity.runOnUiThread(callback::onPlayPauseSuccess);
                } else {
                    Log.println(Log.VERBOSE, "play/pause success", "WE FAILED");
                    activity.runOnUiThread(callback::onError);
                }
            } catch (Exception e) {
                Log.println(Log.VERBOSE, "play/pause success", "WE EXCEPTED");
                activity.runOnUiThread(callback::onError);
            }
        }).start();
    }
    private static void showToast(String message) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }

}
