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

 import java.util.ArrayList;
 import java.util.List;
 import java.util.concurrent.ExecutorService;
 import java.util.concurrent.Executors;
 import java.util.concurrent.TimeUnit;

 import okhttp3.OkHttpClient;
 import okhttp3.Request;
 import okhttp3.RequestBody;
 import okhttp3.Response;

public class SpotifyService {
    private static final String REDIRECT_URI = "mymelody://callback";
    private static final String CLIENT_ID = "44d8159e766e496f9b8ce905397518af";
    private static final String BASE_SPOTIFY_URL = "https://api.spotify.com/v1";
    private static final OkHttpClient client = new OkHttpClient();
    private final ExecutorService executorService = Executors.newFixedThreadPool(4); // Number of threads can be adjusted
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
    //*****************    Get Requests    *****************
    //Tracks/Artists/etc calls
    public interface FetchTrackCallback {
        void onTrackFetched(List<Track> tracks);
        void onError();
    }
    public void fetchUserTopTracks(String accessToken, String rangeSelection, int numTracks, FetchTrackCallback callback) {
        String url = "/me/top/tracks?time_range=" + rangeSelection + "&limit=" + numTracks;
        executeSpotifyRequest(accessToken, url, "GET", new ResponseHandler() {
            @Override
            public void onSuccess(String responseData) {
                try {
                    Log.println(Log.VERBOSE, "Track Fetcher", "Received response for tracks!");
                    List<Track> trackList = new ArrayList<>();
                    JSONObject jsonResponse = new JSONObject(responseData);
                    JSONArray items = jsonResponse.getJSONArray("items");
                    Log.println(Log.VERBOSE, "num tracks", "received " + items.length() + "track");
                    if (items.length() > 0) {
                        for (int i = 0; i < items.length(); i++) {
                            JSONObject track = items.getJSONObject(i);
                            String trackID = track.getString("id");
                            String trackName = track.getString("name");
                            String previewUrl = track.getString("preview_url");
                            JSONArray artistsJson = track.getJSONArray("artists");
                            StringBuilder artistsBuilder = new StringBuilder();
                            for (int j = 0; j < artistsJson.length(); j++) {
                                JSONObject artistJson = artistsJson.getJSONObject(j);
                                if (j > 0) artistsBuilder.append(", ");
                                artistsBuilder.append(artistJson.getString("name"));
                            }
                            String artists = artistsBuilder.toString();
                            JSONArray album = track.getJSONObject("album").getJSONArray("images");
                            JSONObject albumImage = album.getJSONObject(0);
                            String albumCover = albumImage.getString("url");
                            String trackUrl = track.getJSONObject("external_urls").getString("spotify");
                            Track newTrack = new Track(trackID, trackName, 0, artists, albumCover, previewUrl, trackUrl);
                            Log.println(Log.VERBOSE, "Spotify Service", "Created track: " + newTrack.getName() + " - " + newTrack.getArtist() + " - " + trackUrl);
                            trackList.add(newTrack);
                        }
                        if (!trackList.isEmpty()) {
                            Log.println(Log.VERBOSE, "Finished track fetch", "Ready to run on main thread");
                            activity.runOnUiThread(() -> callback.onTrackFetched(trackList));
                        } else {
                            activity.runOnUiThread(() -> showToast("No top tracks found"));
                        }
                    }
                } catch (Exception e) {
                    Log.e("SpotifyService", "Error processing track data: " + e.getMessage());
                    activity.runOnUiThread(callback::onError);
                }
            }

            @Override
            public void onError() {
                activity.runOnUiThread(callback::onError);
            }
        });
    }

    public interface FetchArtistCallback {
        void onArtistFetched(List<Artist> artistList);
        void onError();
    }
    public void fetchUserTopArtists(String accessToken, String rangeSelection, int numArtists, FetchArtistCallback callback) {
        String url = "/me/top/artists?time_range=" + rangeSelection + "&limit=" + numArtists;
        executeSpotifyRequest(accessToken, url, "GET", new ResponseHandler() {
            @Override
            public void onSuccess(String responseData) {
                try {
                    Log.println(Log.VERBOSE, "Artist Fetcher", "Received response for artists!");
                    List<Artist> artistList = new ArrayList<>();
                    JSONObject jsonResponse = new JSONObject(responseData);
                    JSONArray items = jsonResponse.getJSONArray("items");
                    Log.println(Log.VERBOSE, "num artists", "received " + items.length() + "artists");
                    if (items.length() > 0) {
                        for (int i = 0; i < items.length(); i++) {
                            JSONObject artist = items.getJSONObject(i);
                            String artistID = artist.getString("id");
                            String artistName = artist.getString("name");
                            JSONObject artistPFP = artist.getJSONArray("images").getJSONObject(0);
                            String artistPFPUrl = artistPFP.getString("url");
                            String artistUrl = artist.getJSONObject("external_urls").getString("spotify");
                            Artist newArtist = new Artist(artistID, artistName, 0, artistPFPUrl, artistUrl);
                            JSONArray genresArray = artist.getJSONArray("genres");
                            List<String> genres = new ArrayList<>();
                            for (int j = 0; j < genresArray.length(); j++) {
                                genres.add(genresArray.getString(j));
                            }
                            newArtist.setGenres(genres);
                            Log.println(Log.VERBOSE, "Spotify Service", "Created artist: " + newArtist.getName() + " - " + newArtist.getArtistURL() + " - " + newArtist.getSpotifyURL());
                            Log.println(Log.VERBOSE, "Genre", newArtist.getGenres().toString());
                            artistList.add(newArtist);
                        }
                        if (!artistList.isEmpty()) {
                            Log.println(Log.VERBOSE, "Finished track fetch", "Ready to run on main thread");
                            activity.runOnUiThread(() -> callback.onArtistFetched(artistList));
                        } else {
                            activity.runOnUiThread(() -> showToast("No top tracks found"));
                        }
                    }
                } catch (Exception e) {
                    Log.e("SpotifyService", "Error processing track data: " + e.getMessage());
                    activity.runOnUiThread(callback::onError);
                }
            }
            @Override
            public void onError() {
                activity.runOnUiThread(callback::onError);
            }
        });
    }

    public interface FetchSongCallback {
        void onSongFetched(String songName, String artistName, String albumArtUrl, int progress, int duration);
        void onNoActiveSession();
        void onError();
    }
    public void fetchCurrentSong(FetchSongCallback callback) {
        String url = "/me/player/currently-playing?market=US";
        executeSpotifyRequest(accessToken, url, "GET", new ResponseHandler() {
            @Override
            public void onSuccess(String responseData) {
                try {
                    Log.println(Log.VERBOSE, "Current Song Fetcher", "Received response for current song");
                    JSONObject jsonResponse = new JSONObject(responseData);
                    if (jsonResponse.isNull("item")) {
                        showToast("No song currently playing.");
                        activity.runOnUiThread(callback::onNoActiveSession);
                        return;
                    }
                    // Extract current song information
                    JSONObject track = jsonResponse.getJSONObject("item");
                    String trackName = track.getString("name");
                    JSONArray artistsArray = track.getJSONArray("artists");
                    String artistName = artistsArray.getJSONObject(0).getString("name"); // Assuming first artist
                    JSONObject album = track.getJSONObject("album");
                    String albumArtUrl = album.getJSONArray("images").getJSONObject(0).getString("url"); // Assuming first image
                    int progress = jsonResponse.getInt("progress_ms");
                    int duration = track.getInt("duration_ms");
                    activity.runOnUiThread(() -> callback.onSongFetched(trackName, artistName, albumArtUrl, progress, duration));
                } catch (Exception e) {
                    Log.e("SpotifyService", "Error fetching current song: " + e.getMessage());
                    activity.runOnUiThread(callback::onError);
                }
            }
            @Override
            public void onError() {
                activity.runOnUiThread(callback::onError);
            }
        });
    }

    public interface FetchTrackDetailsCallback {
        void onTrackDetailsFetched(Track track);
        void onError();
    }

    public void fetchTrackDetails(String trackId, FetchTrackDetailsCallback fetchTrackDetailsCallback) {
        String url = "/tracks/" + trackId;
        executeSpotifyRequest(accessToken, url, "GET", new ResponseHandler() {
            @Override
            public void onSuccess(String responseData) {
                try {
                    JSONObject trackJson = new JSONObject(responseData);
                    // Extract track details from JSON
                    String id = trackJson.getString("id");
                    String name = trackJson.getString("name");
                    String previewUrl = trackJson.getString("preview_url");
                    JSONArray artistsJson = trackJson.getJSONArray("artists");
                    StringBuilder artistsBuilder = new StringBuilder();
                    for (int j = 0; j < artistsJson.length(); j++) {
                        JSONObject artistJson = artistsJson.getJSONObject(j);
                        if (j > 0) artistsBuilder.append(", ");
                        artistsBuilder.append(artistJson.getString("name"));
                    }
                    String artists = artistsBuilder.toString();
                    JSONArray album = trackJson.getJSONObject("album").getJSONArray("images");
                    JSONObject albumImage = album.getJSONObject(0); // Assuming the first image is the largest
                    String albumCover = albumImage.getString("url");
                    String trackUrl = trackJson.getJSONObject("external_urls").getString("spotify");
                    Track track = new Track(id, name, 0, artists, albumCover, previewUrl, trackUrl);
                    activity.runOnUiThread(() -> fetchTrackDetailsCallback.onTrackDetailsFetched(track));
                } catch (Exception e) {
                    Log.e("SpotifyService", "Error processing track data: " + e.getMessage());
                    activity.runOnUiThread(fetchTrackDetailsCallback::onError);
                }
            }

            @Override
            public void onError() {
                activity.runOnUiThread(fetchTrackDetailsCallback::onError);
            }
        });
    }

    public interface FetchArtistDetailsCallback {
        void onArtistDetailsFetched(Artist artist);
        void onError();
    }
    public void fetchArtistDetails(String artistId, FetchArtistDetailsCallback fetchArtistDetailsCallback) {
        String url = "/artists/" + artistId;
        executeSpotifyRequest(accessToken, url, "GET", new ResponseHandler() {
            @Override
            public void onSuccess(String responseData) {
                // Process the response and invoke callback with artist details
                // Similar to existing implementation
                try {
                    JSONObject artistJson = new JSONObject(responseData);
                    // Parse the JSON object to create a Track object
                    String artistID = artistJson.getString("id");
                    String artistName = artistJson.getString("name");
                    String artistPFPUrl = artistJson.getJSONArray("images").getJSONObject(0).getString("url");
                    String artistUrl = artistJson.getJSONObject("external_urls").getString("spotify");

                    Artist newArtist = new Artist(artistID, artistName, 0, artistPFPUrl, artistUrl);
                    Log.println(Log.VERBOSE, "Spotify Service", "Created artist: " + newArtist.getName() + " - " + newArtist.getArtistURL() + " - " + newArtist.getSpotifyURL());
                    // Use Handler to run on UI thread
                    activity.runOnUiThread(() -> fetchArtistDetailsCallback.onArtistDetailsFetched(newArtist));
                } catch (Exception e) {
                    Log.e("SpotifyService", "Error processing artist data: " + e.getMessage());
                    activity.runOnUiThread(fetchArtistDetailsCallback::onError);
                }
            }

            @Override
            public void onError() {
                activity.runOnUiThread(fetchArtistDetailsCallback::onError);
            }
        });
    }

    //Get User Info
    public interface FetchUserInfoCallback {
        void onUserInfoFetched(User user);
        void onError();
    }
    public void fetchUserInfo(String accessToken, FetchUserInfoCallback callback) {
        String url = "/me";
        executeSpotifyRequest(accessToken, url, "GET", new ResponseHandler() {
            @Override
            public void onSuccess(String responseData) {
                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    // Extract user information from the JSON object
                    String id = jsonObject.getString("id");
                    String username = jsonObject.has("display_name") ? jsonObject.getString("display_name") : "";
                    String email = jsonObject.has("email") ? jsonObject.getString("email") : "";
                    String profileLink = jsonObject.getJSONObject("external_urls").getString("spotify");
                    String imageUrl = "";
                    JSONArray images = jsonObject.getJSONArray("images");
                    if (images.length() > 0) {
                        JSONObject image = images.getJSONObject(0); // Assuming the first image is the user's profile picture
                        imageUrl = image.getString("url");
                    }
                    User user = new User(id, username, email, profileLink, imageUrl, accessToken);
                    // Use Handler to run on UI thread
                    activity.runOnUiThread(() -> callback.onUserInfoFetched(user));
                } catch (Exception e) {
                    Log.e("SpotifyService", "Error processing user data: " + e.getMessage());
                    activity.runOnUiThread(callback::onError);
                }
            }

            @Override
            public void onError() {
                activity.runOnUiThread(callback::onError);
            }
        });
    }
    public interface FetchDeviceStatusCallback {
        void onDeviceStatusFetched(String deviceId, Boolean is_playing, String repeat_state, Boolean shuffle_state);
        void onNoActiveSession();
        void onError();
    }
    public void fetchCurrentDeviceStatus(String accessToken, FetchDeviceStatusCallback callback) {
        String url = "/me/player";
        executeSpotifyRequest(accessToken, url, "GET", new ResponseHandler() {
            @Override
            public void onSuccess(String responseData) {
                try {
                    JSONObject jsonResponse = new JSONObject(responseData);
                    if (jsonResponse.isNull("device")) {
                        activity.runOnUiThread(callback::onNoActiveSession);
                        return;
                    }
                    JSONObject device = jsonResponse.getJSONObject("device");
                    String deviceId = device.getString("id");
                    Boolean is_playing = jsonResponse.getBoolean("is_playing");
                    String repeat_state = jsonResponse.getString("repeat_state");
                    Boolean shuffle_state = jsonResponse.getBoolean("shuffle_state");
                    activity.runOnUiThread(() -> callback.onDeviceStatusFetched(deviceId, is_playing, repeat_state, shuffle_state));
                } catch (Exception e) {
                    Log.e("SpotifyService", "Error fetching device status: " + e.getMessage());
                    activity.runOnUiThread(callback::onError);
                }
            }
            @Override
            public void onError() {
                activity.runOnUiThread(callback::onError);
            }
        });
    }

    //*****************    Put Requests    *****************
    public interface shufRepPlayPauseCallback {
        void onShufRepPlayPauseSuccess();
        void onError();
    }
    public void shufRepPlayPause(String accessToken, String ShufRepPlayPause, shufRepPlayPauseCallback callback) {
        String url = "/me/player";
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
        executeSpotifyRequest(accessToken, url, "PUT", new ResponseHandler() {
            @Override
            public void onSuccess(String responseData) {
                Log.println(Log.VERBOSE, "ShufRep/PlayPause Status", ShufRepPlayPause + " success");
                activity.runOnUiThread(callback::onShufRepPlayPauseSuccess);
            }
            @Override
            public void onError() {
                Log.println(Log.VERBOSE, "ShufRep/PlayPause Status", "Failed to " + ShufRepPlayPause + " track");
                activity.runOnUiThread(callback::onError);
            }
        });
    }

    //*****************    Post Requests    *****************
    public interface skipSongCallback {
        void onSkipSongSuccess();
        void onError();
    }
    public void skipSong(String accessToken, String direction, skipSongCallback callback) {
        String urlSuffix = direction.equals("previous") ? "previous" : "next";
        String url = "/me/player/" + urlSuffix;
        Log.println(Log.VERBOSE, "Skip Song Request", "Sending: " + url);
        executeSpotifyRequest(accessToken, url, "POST", new ResponseHandler() {
            @Override
            public void onSuccess(String responseData) {
                Log.println(Log.VERBOSE, "Skip Song Status", "Skipping " + direction);
                activity.runOnUiThread(callback::onSkipSongSuccess);
            }

            @Override
            public void onError() {
                Log.println(Log.VERBOSE, "Skip Song Status", "Failed to skip " + direction);
                activity.runOnUiThread(callback::onError);
            }
        });
    }
    public void addToQueue(String trackID) {
        String url = "/me/player/queue?uri=spotify%3Atrack%3A" + trackID;
        executeSpotifyRequest(accessToken, url, "POST", new ResponseHandler() {
            @Override
            public void onSuccess(String responseData) {
                Log.println(Log.VERBOSE, "Queue Song Status", "Successfully queued song.");
            }
            @Override
            public void onError() {
                Log.println(Log.VERBOSE, "Queue Song Status", "Failed to queue song.");
            }
        });
    }

    //*****************    Helper Functions    *****************
    private void executeSpotifyRequest(String access_token, String url, String requestType, ResponseHandler handler) {
        executorService.submit(() -> {
            Request.Builder builder = new Request.Builder()
                    .url(BASE_SPOTIFY_URL + url)
                    .addHeader("Authorization", "Bearer " + access_token);
            switch(requestType) {
                case "POST":
                    builder.post(RequestBody.create("", null));
                    break;
                case "PUT":
                    builder.put(RequestBody.create("", null));
                    break;
                case "GET":
                default:
                    break;
            }
            Request request = builder.build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseData = response.body() != null ? response.body().string() : "";
                    handler.onSuccess(responseData);
                } else {
                    handler.onError();
                }
            } catch (Exception e) {
                Log.e("SpotifyService", "Error: " + e.getMessage());
                handler.onError();
            }
        });
    }
    private interface ResponseHandler {
        void onSuccess(String responseData);
        void onError();
    }
    public void shutdownThreads() {
        if (!executorService.isShutdown()) {
            executorService.shutdown(); // Disable new tasks from being submitted
            try {
                // Wait a while for existing tasks to terminate
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow(); // Cancel currently executing tasks
                }
            } catch (InterruptedException ie) {
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
        }
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
