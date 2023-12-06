package edu.ucsb.ece251.DATQ.mymelody;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


import java.util.ArrayList;


public class ArtistAdapter extends ArrayAdapter<Artist> {
    public ArtistAdapter(Context context, ArrayList<Artist> artists) {
        super(context, 0, artists);
    }
    DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.artistlist, parent, false); // Ensure this is the correct layout
            holder = new ViewHolder();
            holder.tvArtistName = convertView.findViewById(R.id.tvArtistName);
            holder.etArtistScore = convertView.findViewById(R.id.etArtistScore);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Artist artist = getItem(position);

        if (artist != null && artist.isSavedToFirebase()) {
            holder.tvArtistName.setText(artist.getName());

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
        databaseReference.child("artists").child(artist.getId()).setValue(artist); // Update Firebase
    }

    private void saveArtistRating(Artist artist) {
        SharedPreferences prefs = getContext().getSharedPreferences("ArtistPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(artist.getId(), artist.getRating());
        editor.apply();
    }

    static class ViewHolder {
        TextView tvArtistName;
        EditText etArtistScore;
    }
}
