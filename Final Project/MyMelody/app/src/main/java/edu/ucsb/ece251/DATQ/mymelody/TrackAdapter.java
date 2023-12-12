package edu.ucsb.ece251.DATQ.mymelody;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;


public class TrackAdapter extends ArrayAdapter<Track> {
    private final String Userid;
    private final SpotifyService spotifyService;
    private boolean scrollEnabled = false;
    private final Context context;
    private MediaPlayer mediaPlayer;
    public TrackAdapter(Context context, ArrayList<Track> tracks, String id, SpotifyService spotifyService) {
        super(context, 0, tracks);
        this.Userid = id;
        this.spotifyService = spotifyService;
        this.context = context;
    }
    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.tracklist, parent, false);
            holder = new ViewHolder();
            holder.albumCover = convertView.findViewById(R.id.albumCover);
            holder.tvTrackName = convertView.findViewById(R.id.tvTrackName);
            holder.tvTrackName.setSelected(false);
            holder.etTrackScore = convertView.findViewById(R.id.etTrackScore);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Track track = getItem(position);
        holder.tvTrackName.setOnClickListener(v -> {
            if (!scrollEnabled) {
                holder.tvTrackName.setSelected(true);
                scrollEnabled = true;
            } else {
                holder.tvTrackName.setSelected(false);
                scrollEnabled = false;
            }
        });
        holder.tvTrackName.setOnLongClickListener(v -> {
            Log.println(Log.VERBOSE, "Test onclick", "TESTING on click for: " + track.getId());
            showConfirmationDialog(track);
            return false;
        });
        holder.albumCover.setOnClickListener(v -> {
            String spotifyUrl = track.getTrackUrl();
            Log.println(Log.VERBOSE, "Track Url", spotifyUrl);
            if (!TextUtils.isEmpty(spotifyUrl)) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(spotifyUrl));
                context.startActivity(intent);
            }
        });

        holder.albumCover.setOnLongClickListener(v -> {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                // If MediaPlayer is playing, pause it
                mediaPlayer.pause();
                mediaPlayer.release(); // Release the MediaPlayer resources
                mediaPlayer = null; // Set it to null

                // Update UI or provide feedback (e.g., change the icon to play)
            } else if (track != null && track.getPreviewUrl() != null && !track.getPreviewUrl().isEmpty()) {
                // Play the song snippet using the preview URL
                showPlaybackConfirmationDialog(track);
            } else {
                // Handle the case where the preview URL is not available
                Log.d("TrackAdapter", "Preview URL is not available for this track.");
            }
            return true; // Return true to indicate that the long click event is consumed
        });

        if (track != null) {
            Log.d("TrackAdapter", "Received track: " + track.getName() + " - " + track.getArtist() + " - " + track.getPreviewUrl());
            String displayText = track.getName() + " - " + track.getArtist(); // Combining track name with artist name
            holder.tvTrackName.setText(displayText);

            if (track.getAlbumCover() != null && !track.getAlbumCover().isEmpty()) {
                Picasso.get().load(track.getAlbumCover()).into(holder.albumCover);
            } else {
                // Set a placeholder image or handle the case where albumCover URL is empty
                holder.albumCover.setImageResource(R.drawable.spotify);
            }


            // Remove the existing TextWatcher
            if (holder.etTrackScore.getTag() instanceof TextWatcher) {
                holder.etTrackScore.removeTextChangedListener((TextWatcher) holder.etTrackScore.getTag());
            }

            holder.etTrackScore.setText(track.getRating() >= 0 && track.getRating()<=10 ? String.valueOf(track.getRating()) : "");
            onScoreChanged(track, track.getRating());
            TextWatcher textWatcher = new TextWatcher() {
                // beforeTextChanged, onTextChanged, afterTextChanged implementation
                @Override
                public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                    // No action needed here for this example
                }
                @Override
                public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                    // No action needed here for this example
                }
                @Override
                public void afterTextChanged(Editable s) {
                    try {
                        int rating = Integer.parseInt(s.toString());
                        track.setRating(rating);
                        saveTrackRating(track);
                    } catch (NumberFormatException e) {
                        track.setRating(0); // Or handle this as you see fit
                    }
                }

                // Other methods (beforeTextChanged, onTextChanged) can remain empty if not needed
            };

            holder.etTrackScore.addTextChangedListener(textWatcher);
            holder.etTrackScore.setTag(textWatcher); // Store the watcher in the tag for later removal
        }

        return convertView;
    }

    private void playSongSnippet(String previewUrl) {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(previewUrl);
                mediaPlayer.prepare();
                mediaPlayer.start();
                // Release the media player resources
                mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // Resume playback if it was paused
            mediaPlayer.start();
        }
    }
    private void showPlaybackConfirmationDialog(Track track) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Play Song Snippet");
        builder.setMessage("Do you want to play a snippet of " + track.getName() + "?");

        builder.setPositiveButton("Yes", (dialog, which) -> {
            // Play the song snippet using the preview URL
            playSongSnippet(track.getPreviewUrl());
        });

        builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }
    public void onScoreChanged(Track track, int newScore) {
        track.setRating(newScore); // Update the score in the Track object

        databaseReference.child("tracks" + Userid).child(track.getId()).setValue(track); // Update Firebase
    }

    private void saveTrackRating(Track track) {
        SharedPreferences prefs = getContext().getSharedPreferences("TrackPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(track.getId(), track.getRating());
        editor.apply();
    }

    private void showConfirmationDialog(Track track) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext()); // Use 'getActivity()' if in a fragment
        builder.setTitle("Queue Song");
        builder.setMessage("Do you want to queue " + track.getName() + "?");

        builder.setPositiveButton("Yes", (dialog, which) -> {
            // Handle the confirmation action here
            Log.println(Log.VERBOSE, "CONFIRMATION", "Confirming queue request for " + track.getName());
            spotifyService.addToQueue(track.getId());
        });
        builder.setNegativeButton("No", (dialog, which) -> {
            Log.println(Log.VERBOSE, "CONFIRMATION", "Queue request denied.");
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
    static class ViewHolder {
        ImageView albumCover;
        TextView tvTrackName;
        EditText etTrackScore;
    }
}
