package edu.ucsb.ece251.DATQ.mymelody;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;

// Assuming Artist is a class you've defined with getName() and getRating() methods.
public class ArtistAdapter extends ArrayAdapter<Artist> {
    public ArtistAdapter(Context context, ArrayList<Artist> artists) {
        super(context, 0, artists);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.artistlist, parent, false);
        }

        TextView tvArtistName = convertView.findViewById(R.id.tvArtistName); // Make sure this ID exists
        EditText etArtistRating = convertView.findViewById(R.id.etArtistScore); // Make sure this ID exists

        Artist artist = getItem(position);

        if (artist != null) {
            tvArtistName.setText(artist.getName());
            etArtistRating.setText(artist.getRating() > 0 && artist.getRating() <= 10 ? String.valueOf(artist.getRating()) : "");

            etArtistRating.addTextChangedListener(new TextWatcher() {
                // Your TextWatcher implementation
                @Override
                public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                    // No action needed here for this example
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                    // No action needed here for this example
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    // Here you respond after the text has changed
                    try {
                        int rating = Integer.parseInt(editable.toString());
                        artist.setRating(rating);
                    } catch (NumberFormatException e) {
                        artist.setRating(0);
                    }
                }
            });
        }

        return convertView;
    }
}
