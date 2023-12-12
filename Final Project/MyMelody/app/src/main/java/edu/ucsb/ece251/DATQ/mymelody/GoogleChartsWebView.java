package edu.ucsb.ece251.DATQ.mymelody;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoogleChartsWebView extends AppCompatActivity{
    private SpotifyService spotifyService;
    private String accessToken;
    private User currentUser;
    SeekBar genreSeekBar;
    private int fetchCount;
    private int selectedRange;
    private TextView genreCountTextView;
    private boolean isPageLoaded = false; // Flag to check if the WebView page has loaded
    private Map<String, Integer> genreCountReady = null; // Store genre count when ready
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

        genreSeekBar = findViewById(R.id.genreSeekBar);
        genreCountTextView = findViewById(R.id.genreCountTextView);
        Spinner timeRange = findViewById(R.id.timeRange);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.time_range_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeRange.setAdapter(adapter);
        timeRange.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedTimeRange = (String) parent.getItemAtPosition(position);
                switch(selectedTimeRange) {
                    case "Last Month":
                        selectedRange = 0;
                        break;
                    case "Last 6 Months":
                        selectedRange = 1;
                        break;
                    case "All Time":
                        selectedRange = 2;
                        break;
                }
                // Handle the selected item
                Log.println(Log.VERBOSE, "Range selected", "Range: " + selectedTimeRange);
                fetchTopArtists(accessToken, selectedRange, fetchCount);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });

        genreSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                fetchCount = progress + 1;  // Since the range is 1 to 50
                genreCountTextView.setText(fetchCount + " Artists");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Optionally implement
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Optionally implement
                // Fetch artists here if you want to fetch them immediately after user selection
                fetchTopArtists(accessToken, selectedRange, fetchCount);
            }
        });

        fetchTopArtists(accessToken, 1, 10);
        WebView webView = findViewById(R.id.googleCharts);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                isPageLoaded = true;
                if (genreCountReady != null) {
                    visualizeDataInChart(genreCountReady);
                }
            }
        });
        // Load a URL that hosts your Google Chart
        webView.loadUrl("file:///android_asset/charts.html");


    }

    private void fetchTopArtists(String accessToken, int rangeSetting, int numArtists) {
        spotifyService.fetchUserTopArtists(accessToken, rangeSetting, numArtists, new SpotifyService.FetchArtistCallback() {
            @Override
            public void onArtistFetched( List<Artist> artists) {
//                String[] artistBlocks = artists.split("%19"); // Split by %19 for each artist
//                String[] firstBlock = artistBlocks[0].split("%20", 2);
//                Map<String, Integer> genreCount = new HashMap<>();
//
//                processArtistBlock(firstBlock[1], genreCount);
//
//                for (int i = 1; i < artistBlocks.length; i++) {
//                    processArtistBlock(artistBlocks[i], genreCount);
//                }
//
//                visualizeDataInChart(genreCount);
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
        // Next steps...
    }
    private void visualizeDataInChart(Map<String, Integer> genreCount) {
        if (isPageLoaded) {
            WebView webView = findViewById(R.id.googleCharts);
            String chartData = convertGenreCountToChartData(genreCount);
            webView.evaluateJavascript("updateChart(" + chartData + ");", null);
        } else {
            genreCountReady = genreCount; // Store the data until the page is loaded
        }
    }

    private String convertGenreCountToChartData(Map<String, Integer> genreCount) {
        StringBuilder chartData = new StringBuilder("[['Genre', 'Count'],");

        for (Map.Entry<String, Integer> entry : genreCount.entrySet()) {
            chartData.append("['").append(entry.getKey()).append("', ").append(entry.getValue()).append("],");
        }

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
