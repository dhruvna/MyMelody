package edu.ucsb.ece251.DATQ.mymelody;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ArtistActivity extends AppCompatActivity {
    private ArrayList<Artist> artistArrayList;
    private ArtistAdapter artistAdapter;
    private SpotifyService spotifyService;
    private String accessToken;
    private User currentUser;
    private boolean isDataLoaded = false;
    private boolean slider = false;
    private int rangeSetting;
    final static int lastMonth = 0;
    final static int last6Months = 1;
    final static int allTime = 2;
    private TextView artistCountTextView;
    private int numArtists = 10;  // Default value
    SeekBar artistSeekBar;
    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String userInfo = extras.getString("User Info");
            if (userInfo != null) currentUser = parseUserString(userInfo);
            accessToken = currentUser.getAccessToken();
        }

        spotifyService = new SpotifyService(this);
        artistArrayList = new ArrayList<>();
        artistAdapter = new ArtistAdapter(this, artistArrayList, currentUser.getId()); // Initialize ArtistAdapter with the artist list
        ListView artistListView = findViewById(R.id.ArtistList);
        artistListView.setAdapter(artistAdapter); // Set the adapter for the ListView
        FirebaseApp.initializeApp(this);
        artistCountTextView = findViewById(R.id.artistCountTextView);
        artistSeekBar = findViewById(R.id.artistSeekBar);

        Button sortButton = findViewById(R.id.btnSortArtists);
        sortButton.setOnClickListener(view -> showSortingOptions());

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
                        rangeSetting = lastMonth;
                        break;
                    case "Last 6 Months":
                        rangeSetting = last6Months;
                        break;
                    case "All Time":
                        rangeSetting = allTime;
                        break;
                }
                // Handle the selected item
                Log.println(Log.VERBOSE, "Range selected", "Range: " + selectedTimeRange);
                if(isDataLoaded)
                {
                    isDataLoaded = false;
                }
                else {
                    handleTimeRangeSelection(rangeSetting, numArtists);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        slider = false;
        if(artistSeekBar != null) {
            artistSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    numArtists = progress + 1;  // Since the range is 1 to 50
                    artistCountTextView.setText(numArtists + " Artists");
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    slider = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    // Fetch artists here immediately after user selection
                    if (slider) {
                        fetchUserTopArtists(accessToken, rangeSetting, numArtists);
                    }

                    slider = false;
                }
            });
        }

        loadPreferences();
    }

    private void savePreferences() {
        SharedPreferences prefs = getSharedPreferences("ArtistPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(artistArrayList);
        Log.d("SaveData", "Saving JSON: " + json);
        editor.putString("artistList", json);
        editor.putInt("numArtists", numArtists);
        editor.putInt("rangeSetting", rangeSetting);
        editor.putInt("seekBarPosition", numArtists - 1);
        editor.apply();
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences("ArtistPrefs", MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString("artistList", null);
        Type type = new TypeToken<ArrayList<Artist>>() {}.getType();
        artistArrayList = gson.fromJson(json, type);

        if (artistArrayList == null) {
            artistArrayList = new ArrayList<>();
            Log.d("ArtistArrayList","Empty");
            isDataLoaded = false;
        }
        if(artistArrayList!=null){
            Log.d("LoadData", "Loaded artist list size: " + artistArrayList.size());
            isDataLoaded = true;
            for (int i = 0; i < Math.min(5, artistArrayList.size()); i++) {
                Log.d("LoadData", "Artist " + i + ": " + artistArrayList.get(i).getName() + ", PFP: " + artistArrayList.get(i).getArtistURL());
            }
            artistAdapter = new ArtistAdapter(this, artistArrayList, currentUser.getId());
            ListView artistListView = findViewById(R.id.ArtistList);
            artistListView.setAdapter(artistAdapter);
        }

        int seekBarPosition = prefs.getInt("seekBarPosition", 9);
        numArtists = seekBarPosition + 1;
        artistSeekBar.setProgress(seekBarPosition);
        artistCountTextView.setText(numArtists + " Artists");
        rangeSetting = prefs.getInt("rangeSetting", lastMonth);
        numArtists = prefs.getInt("numArtists", 10);
        Spinner timeRange = findViewById(R.id.timeRange);
        if (timeRange != null) {
            timeRange.setSelection(rangeSetting);
        }

        if (accessToken != null && artistArrayList.isEmpty()) {
            fetchUserTopArtists(accessToken, rangeSetting, numArtists);
        } else {
            artistAdapter = new ArtistAdapter(this, artistArrayList, currentUser.getId());
            ListView artistListView = findViewById(R.id.ArtistList);
            artistListView.setAdapter(artistAdapter);
        }
    }

    private void sortArtistByScore(boolean ascending) {
        if (ascending) {
            // Sort ascending by name
            artistArrayList.sort((artist1, artist2) -> {
                int scoreComparison = Integer.compare(artist1.getRating(), artist2.getRating());
                if (scoreComparison == 0) {
                    return artist1.getName().compareToIgnoreCase(artist2.getName());
                }
                return scoreComparison;
            });
        } else {
            // Sort descending by score and then by name if scores are equal
            artistArrayList.sort((artist1, artist2) -> {
                int scoreComparison = Integer.compare(artist2.getRating(), artist1.getRating());
                if (scoreComparison == 0) {
                    return artist1.getName().compareToIgnoreCase(artist2.getName());
                }
                return scoreComparison;
            });
        }
        artistAdapter.notifyDataSetChanged();
        savePreferences();
    }

    private void fetchUserTopArtists(String accessToken, int rangeSetting, int numArtists) {
        spotifyService.fetchUserTopArtists(accessToken, rangeSetting, numArtists, new SpotifyService.FetchArtistCallback() {
            @Override
            public void onArtistFetched(List<Artist> artists) {
                artistArrayList.clear();

                for (int i = 0; i <= artists.size()-1; i++) {
                    checkAndStoreArtist(artists.get(i).getId());
                }
                artistAdapter.notifyDataSetChanged();
            }
            @Override
            public void onError() {
                showToast("Failed to fetch top artists.");
            }
        });
    }

    private boolean isArtistInList(Artist artist) {
        for (Artist existingArtist : artistArrayList) {
            if (existingArtist.getId().equals(artist.getId())) {
                // Found a matching artist in the list
                return true;
            }
        }
        return false; // No matching artist found
    }
    private void checkAndStoreArtist(String artistId) {
        databaseReference.child("artists" + currentUser.getId()).child(artistId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    // Artist not in Firebase, so fetch details from Spotify and store
                    fetchArtistDetailsFromSpotifyAndStore(artistId);
                } else {
                    Log.d("Firebase", "Data updated: " + dataSnapshot.getValue());
                    // Artist exists in Firebase, use it
                    Artist artist = dataSnapshot.getValue(Artist.class);
                    if (artist != null) {
                        if (!isArtistInList(artist)) {
                            artistArrayList.add(artist);
                            artistAdapter.notifyDataSetChanged();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w("Firebase", "Failed to read artist", databaseError.toException());
            }
        });
    }

    private void fetchArtistDetailsFromSpotifyAndStore(String artistId) {
        // Fetch artist details from Spotify
        spotifyService.fetchArtistDetails(artistId, new SpotifyService.FetchArtistDetailsCallback() {
            @Override
            public void onArtistDetailsFetched(Artist artist) {
                Log.d("ArtistActivity", "Displaying artist: " + artist.getName() + " - " + artist.getArtistURL() + " - " + artist.getArtistURL());
                // Store in Firebase
                databaseReference.child("artists" + currentUser.getId()).child(artistId).setValue(artist);
                artistArrayList.add(artist);
                artistAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError() {
                Log.e("Spotify", "Failed to fetch artist details");
            }
        });
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

    private void handleTimeRangeSelection(int rangeSetting, int numArtists) {
        // Fetch data for 'Last Month', 'Last 6 Months', or 'All Time'
        fetchUserTopArtists(accessToken, rangeSetting, numArtists);
        artistAdapter.notifyDataSetChanged();
    }
    @Override
    protected void onPause() {
        super.onPause();
        savePreferences();
    }
    @Override
    protected void onResume() {
        super.onResume();
        loadPreferences();
    }

    private void showSortingOptions() {
        String[] options = {"Sort Ascending", "Sort Descending"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Sorting Option");
        builder.setItems(options, (dialog, which) -> {
            // Descending
            sortArtistByScore(which == 0); // Ascending
        });
        builder.show();
    }
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}