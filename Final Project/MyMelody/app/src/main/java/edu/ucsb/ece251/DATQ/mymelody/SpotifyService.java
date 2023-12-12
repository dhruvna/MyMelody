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
 import java.util.ArrayList;
 import java.util.List;

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
        Log.println(Log.VERBOSE, "Login Process", "Request Sent, Handling in browser activity now.");
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
                    JSONArray artistsJson = trackJson.getJSONArray("artists");
                    StringBuilder artistsBuilder = new StringBuilder();
                    for (int j = 0; j < artistsJson.length(); j++) {
                        JSONObject artistJson = artistsJson.getJSONObject(j);
                        if (j > 0) artistsBuilder.append(", ");
                        artistsBuilder.append(artistJson.getString("name"));
                    }
                    String artists = artistsBuilder.toString();
                    Track track = new Track(id, name, 0, artists);

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

    public interface FetchArtistDetailsCallback {
        void onArtistDetailsFetched(Artist artist);
        void onError();
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
        void onTrackFetched(List<Track> tracks);
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
                    List<Track> trackList = new ArrayList<>();
                    String responseData = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseData);
                    // Extract user information from the JSON object
                    JSONArray items = jsonResponse.getJSONArray("items");
                    Log.println(Log.VERBOSE, "num tracks", "received " + items.length() + "track");
                    if (items.length() > 0) {
                        for(int i = 0; i < items.length(); i++) {
                            JSONObject track = items.getJSONObject(i);
                            String trackID = track.getString("id");
                            String trackName = track.getString("name");
                            JSONArray artistsJson = track.getJSONArray("artists");
                            StringBuilder artistsBuilder = new StringBuilder();
                            for (int j = 0; j < artistsJson.length(); j++) {
                                JSONObject artistJson = artistsJson.getJSONObject(j);
                                if (j > 0) artistsBuilder.append(", ");
                                artistsBuilder.append(artistJson.getString("name"));
                            }
                            String artists = artistsBuilder.toString();
                            Track newTrack = new Track(trackID, trackName, 0, artists);
                            Log.d("SpotifyService", "Created track: " + newTrack.getName() + " - " + newTrack.getArtist());
                            trackList.add(newTrack);
                        }
                        if (!trackList.isEmpty()) {
                            Log.println(Log.VERBOSE, "Finished track fetch", "Ready to run on main thread");
                            activity.runOnUiThread(() -> callback.onTrackFetched(trackList));
                        } else {
                            activity.runOnUiThread(() -> showToast("No top tracks found"));
                        }
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
                            String artistPFPUrl = artist.getJSONArray("images").getJSONObject(0).getString("url");
                            artists.append(artistPFPUrl);
                            artists.append("%18");
                            JSONArray genres = artist.getJSONArray("genres");
                            for(int j = 0; j < genres.length(); j++) {
                                artists.append(genres.getString(j));
                                if(j < genres.length() - 1) {
                                    artists.append(",");
                                }
                            }if(i < items.length() - 1) {
                                artists.append("%19");
                            }
                        }
                        String finalArtists = artists.toString();
                        // Use Handler to run on UI thread
                        Log.println(Log.VERBOSE, "Finished artist fetch", "Ready to run on main thread");
                        activity.runOnUiThread(() -> callback.onArtistFetched(finalArtists));
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
                } else if(responseData.equals("EMPTY_RESPONSE")){
                    // Handle response failure
                    showToast("No song currently playing.");
                }
            } catch (Exception e) {
                Log.println(Log.ERROR, "Track Fetcher", "Error fetching current track: " + e.getMessage());
                activity.runOnUiThread(callback::onError);
            }
        }).start();
    }

    public interface FetchDeviceStatusCallback {
        void onDeviceStatusFetched(String deviceId, Boolean is_playing, String repeat_state, Boolean shuffle_state);
        void onNoActiveSession();
        void onError();
    }
    public void fetchCurrentDeviceStatus(String accessToken, FetchDeviceStatusCallback callback) {
        new Thread(() -> {
            OkHttpClient client = new OkHttpClient();
            String url = "https://api.spotify.com/v1/me/player";

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }
                String responseData = response.body().string(); // Automatically closed after this statement
                if (responseData.isEmpty()) {
                    activity.runOnUiThread(callback::onNoActiveSession);
                    return;
                }
                JSONObject jsonResponse = new JSONObject(responseData);
                JSONObject device = jsonResponse.getJSONObject("device");
                String deviceId = device.getString("id");
                Boolean is_playing = jsonResponse.getBoolean("is_playing");
                String repeat_state = jsonResponse.getString("repeat_state");
                Boolean shuffle_state = jsonResponse.getBoolean("shuffle_state");
                activity.runOnUiThread(() -> callback.onDeviceStatusFetched(deviceId, is_playing, repeat_state, shuffle_state));
            } catch (Exception e) {
                Log.e("SpotifyService", "Error fetching device status", e); // Example logging
                activity.runOnUiThread(callback::onError);
            }
        }).start();
    }

//    Put Requests *****************
    public interface shufRepPlayPauseCallback {
        void onShufRepPlayPauseSuccess();
        void onError();
    }
    public void shufRepPlayPause(String accessToken, String ShufRepPlayPause, shufRepPlayPauseCallback callback) {
        new Thread(() -> {
            OkHttpClient client = new OkHttpClient();
            String url = "https://api.spotify.com/v1/me/player";
            switch (ShufRepPlayPause) {
                case "pause":
                    url +="/pause";
                    break;
                case "play":
                    url +="/play";
                    break;
                case "shuffleOn":
                    url += "/shuffle?state=true";
                    break;
                case "shuffleOff":
                    url += "/shuffle?state=false";
                    break;
                case "repeatAll":
                    url += "/repeat?state=context";
                    break;
                case "repeatOne":
                    url += "/repeat?state=track";
                    break;
                case "repeatOff":
                    url += "/repeat?state=off";
                    break;
            }
            Request.Builder builder = new Request.Builder()
                    .url(url)
                    .put(RequestBody.create("", null))
                    .addHeader("Authorization", "Bearer " + accessToken);
            Request request = builder.build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    Log.println(Log.VERBOSE, "ShufRep/PlayPause Status", ShufRepPlayPause + " success");
                    activity.runOnUiThread(callback::onShufRepPlayPauseSuccess);
                } else {
                    Log.println(Log.VERBOSE, "ShufRep/PlayPause Status", "Failed to " + ShufRepPlayPause + " track");
                    activity.runOnUiThread(callback::onError);
                }
            } catch (Exception e) {
                Log.println(Log.VERBOSE, "ShufRep/PlayPause Status", "Exception with ShufRep/PlayPause request");
                activity.runOnUiThread(callback::onError);
            }
        }).start();
    }

    //    Post Requests *****************
    public interface skipSongCallback {
        void onSkipSongSuccess();
        void onError();
    }
    public void skipSong(String accessToken, String direction, skipSongCallback callback) {
        new Thread(() -> {
            OkHttpClient client = new OkHttpClient();
            String url = "https://api.spotify.com/v1/me/player";
            if(direction.equals("previous")) {
                url +="/previous";
            } else if (direction.equals("next")){
                url +="/next";
            }
            Request.Builder builder = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create("", null))
                    .addHeader("Authorization", "Bearer " + accessToken);
            Request request = builder.build();
            Log.println(Log.VERBOSE, "Skip Song Request", "Sending: " + request);
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    Log.println(Log.VERBOSE, "Skip Song Status", "Skipping " + direction);
                    activity.runOnUiThread(callback::onSkipSongSuccess);
                } else {
                    Log.println(Log.VERBOSE, "Skip Song Status", "Failed to skip " + direction);
                    activity.runOnUiThread(callback::onError);
                }
            } catch (Exception e) {
                Log.println(Log.VERBOSE, "Skip Song Status", "Exception skipping " + direction);
                activity.runOnUiThread(callback::onError);
            }
        }).start();
    }
    public void addToQueue(String trackID) {
        new Thread(() -> {
            OkHttpClient client = new OkHttpClient();
            String url = "https://api.spotify.com/v1/me/player/queue?uri=spotify%3Atrack%3A" + trackID;
            Request.Builder builder = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create("", null))
                    .addHeader("Authorization", "Bearer " + accessToken);
            Request request = builder.build();
            Log.println(Log.VERBOSE, "Skip Song Request", "Sending: " + request);
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    Log.println(Log.VERBOSE, "Queue Song Success", "Successfully queued song.");
                } else {
                    Log.println(Log.VERBOSE, "Queue Song Success", "Failed to queue song.");
                }
            } catch (Exception e) {
                Log.println(Log.VERBOSE, "Queue Song Success", "Exception queuing song.");
            }
        }).start();
    }
    private static void showToast(String message) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }

}
