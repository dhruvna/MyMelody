package edu.ucsb.ece251.DATQ.mymelody;

import android.os.Bundle;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

public class GoogleChartsWebView extends AppCompatActivity{
    private SpotifyService spotifyService;
    private String accessToken;
    private User currentUser;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.google_charts);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String userInfo = extras.getString("User Info");
            if (userInfo != null) currentUser = parseUserString(userInfo);
            accessToken = currentUser.getAccessToken();
        }
        spotifyService = new SpotifyService(this);
        fetchTopArtists(accessToken, 1, 10);
        WebView webView = findViewById(R.id.googleCharts);
        webView.getSettings().setJavaScriptEnabled(true);
        // Load a URL that hosts your Google Chart
        webView.loadUrl("file:///android_asset/charts.html");
    }

    private void fetchTopArtists(String accessToken, int rangeSetting, int numArtists) {
        spotifyService.fetchUserTopArtists(accessToken, rangeSetting, numArtists, new SpotifyService.FetchArtistCallback() {
            @Override
            public void onArtistFetched(String artists) {
                String[] artistBlocks = artists.split("%19"); // Split by %19 for each artist
                String[] firstBlock = artistBlocks[0].split("%20", 2);
                Map<String, Integer> genreCount = new HashMap<>();

                processArtistBlock(firstBlock[1], genreCount);

                for (int i = 1; i < artistBlocks.length; i++) {
                    processArtistBlock(artistBlocks[i], genreCount);
                }

                visualizeDataInChart(genreCount);
            }

            private void processArtistBlock(String block, Map<String, Integer> genreCount) {
                String[] artistInfo = block.split("%21");
                String artistName = artistInfo[0];
                String[] idUrlAndGenres = artistInfo[1].split("%20", 2);
                String artistId = idUrlAndGenres[0];

                // The profile URL and genres are split by "%18"
                String[] urlAndGenres = idUrlAndGenres[1].split("%18", 2);
                String artistPFPUrl = urlAndGenres[0]; // Artist Profile Picture URL
                String genresString = urlAndGenres.length > 1 ? urlAndGenres[1] : "";
                String[] genres = genresString.split(",");

                // Process and count genres
                for (String genre : genres) {
                    if (!genre.isEmpty()) {
                        genreCount.put(genre, genreCount.getOrDefault(genre, 0) + 1);
                    }
                }

                // Store artist name, id, and profile URL for future use
                // For example, store them in a map or class member (not shown here)
                storeArtistInfoForLaterUse(artistId, artistName, artistPFPUrl);
            }

            @Override
            public void onError() {
                showToast("Failed to fetch top artists.");
            }
        });
    }

    private void storeArtistInfoForLaterUse(String artistId, String artistName, String artistPFPUrl) {
        // Implementation depends on how you want to store and use this data later.
        // This could involve storing in a map, list, or class member.
    }
    private void visualizeDataInChart(Map<String, Integer> genreCount) {
        WebView webView = findViewById(R.id.googleCharts);

        // Convert genreCount map to a suitable format for Google Charts
        String chartData = convertGenreCountToChartData(genreCount);

        // Update the chart in WebView
        webView.evaluateJavascript("updateChart(" + chartData + ");", null);
    }

    private String convertGenreCountToChartData(Map<String, Integer> genreCount) {
        // Start with the array structure and column definitions for Google Charts
        StringBuilder chartData = new StringBuilder("[['Genre', 'Count'],");

        for (Map.Entry<String, Integer> entry : genreCount.entrySet()) {
            // Each entry in the format: ['Genre', count],
            chartData.append("['").append(entry.getKey()).append("', ").append(entry.getValue()).append("],");
        }

        // Remove the last comma and close the array
        if (chartData.length() > 0) chartData.setLength(chartData.length() - 1);
        chartData.append("]");

        return chartData.toString();
    }


    public User parseUserString(String userString) {
        String[] lines = userString.split("\n");
        if (lines.length != 6) return null;
        return new User(
                lines[0].substring(lines[0].indexOf(": ") + 2),
                lines[1].substring(lines[1].indexOf(": ") + 2),
                lines[2].substring(lines[2].indexOf(": ") + 2),
                lines[3].substring(lines[3].indexOf(": ") + 2),
                lines[4].substring(lines[4].indexOf(": ") + 2),
                lines[5].substring(lines[5].indexOf(": ") + 2)
        );
    }
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
