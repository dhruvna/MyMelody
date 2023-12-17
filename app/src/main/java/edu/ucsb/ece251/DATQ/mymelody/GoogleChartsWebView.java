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
    private String rangeSelection;
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
                        rangeSelection = "short_term";
                        break;
                    case "Last 6 Months":
                        rangeSelection = "medium_term";
                        break;
                    case "All Time":
                        rangeSelection = "long_term";
                        break;
                }
                // Handle the selected item
                Log.println(Log.VERBOSE, "Range selected", "Range: " + selectedTimeRange);
                fetchTopArtists(accessToken, rangeSelection, fetchCount);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
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
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Fetch artists immediately after user selection
                fetchTopArtists(accessToken, rangeSelection, fetchCount);
            }
        });

        fetchTopArtists(accessToken, "short_term", 10);
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

    private void fetchTopArtists(String accessToken, String rangeSelection, int numArtists) {
        spotifyService.fetchUserTopArtists(accessToken, rangeSelection, numArtists, new SpotifyService.FetchArtistCallback() {
            @Override
            public void onArtistFetched( List<Artist> artists) {
                Map<String, Integer> genreCount = new HashMap<>();
                for(int i = 0; i < artists.size()-1; i++) {
                    List<String> genres = artists.get(i).getGenres();
                    for (String genre : genres) {
                        if (!genre.isEmpty()) {
                            genreCount.put(genre, genreCount.getOrDefault(genre, 0) + 1);
                        }
                    }
                }
                visualizeDataInChart(genreCount);
            }
            @Override
            public void onError() {
                showToast("Failed to fetch top artists.");
            }
        });
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
