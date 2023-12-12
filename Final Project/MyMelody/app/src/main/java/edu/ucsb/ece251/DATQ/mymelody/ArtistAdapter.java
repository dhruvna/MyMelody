package edu.ucsb.ece251.DATQ.mymelody;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import java.util.ArrayList;


public class ArtistAdapter extends ArrayAdapter<Artist> {
    private final String Userid;
    private final Context context;

    public ArtistAdapter(Context context, ArrayList<Artist> artists, String id) {
        super(context, 0, artists);
        this.Userid = id;
        this.context = context;
    }
    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.artistlist, parent, false); // Ensure this is the correct layout
            holder = new ViewHolder();
            holder.artistPFP = convertView.findViewById(R.id.artistPFP);
            holder.tvArtistName = convertView.findViewById(R.id.tvArtistName);
            holder.tvArtistName.setSelected(false);
            holder.etArtistScore = convertView.findViewById(R.id.etArtistScore);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        Artist artist = getItem(position);

        holder.artistPFP.setOnClickListener(v -> {
            String spotifyUrl = artist.getSpotifyURL();
            Log.println(Log.VERBOSE, "Artist Url", spotifyUrl);
            if (!TextUtils.isEmpty(spotifyUrl)) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(spotifyUrl));
                context.startActivity(intent);
            }
        });
        if (artist != null) {
            Log.d("ArtistAdapter", "Received artist: " + artist.getName() + " - " + artist.getArtistURL() + " - " + artist.getSpotifyURL());
            holder.tvArtistName.setText(artist.getName());

            if (artist.getArtistURL() != null) {
                Picasso.get().load(artist.getArtistURL()).into(holder.artistPFP);
            } else {
                // Set a placeholder image or handle the case where albumCover URL is empty
                holder.artistPFP.setImageResource(R.drawable.spotify);
            }

            // Remove the existing TextWatcher
            if (holder.etArtistScore.getTag() instanceof TextWatcher) {
                holder.etArtistScore.removeTextChangedListener((TextWatcher) holder.etArtistScore.getTag());
            }

            holder.etArtistScore.setText(artist.getRating() >= 0 && artist.getRating()<=10 ? String.valueOf(artist.getRating()) : "");
            onScoreChanged(artist, artist.getRating());
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
                        artist.setRating(rating);
                        saveArtistRating(artist);
                    } catch (NumberFormatException e) {
                        artist.setRating(0); // Or handle this as you see fit
                    }
                }

                // Other methods (beforeTextChanged, onTextChanged) can remain empty if not needed
            };

            holder.etArtistScore.addTextChangedListener(textWatcher);
            holder.etArtistScore.setTag(textWatcher); // Store the watcher in the tag for later removal
        }

        return convertView;
    }

    public void onScoreChanged(Artist artist, int newScore) {
        artist.setRating(newScore);

        databaseReference.child("artists" + Userid).child(artist.getId()).setValue(artist); // Update Firebase
    }

    private void saveArtistRating(Artist artist) {
        SharedPreferences prefs = getContext().getSharedPreferences("ArtistPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(artist.getId(), artist.getRating());
        editor.apply();
    }

    static class ViewHolder {
        ImageView artistPFP;
        TextView tvArtistName;
        EditText etArtistScore;
    }
}
