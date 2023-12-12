package edu.ucsb.ece251.DATQ.mymelody;

public class Artist {
    private String id;
    private String name;
    private int rating;
    private String[] genres;
    private String artistURL;
    private String spotifyURL;
    public Artist(){} //need for firebase
    public Artist(String id, String name, int rating, String artistUrl, String spotifyUrl) {
        this.id = id;
        this.name = name;
        this.rating = rating;
        this.artistURL = artistUrl;
        this.spotifyURL = spotifyUrl;
    }
    // Getters and setters
    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
    public String getId(){return id; }
    public void setGenres(String[] genres) { this.genres = genres; }

    private String[] getGenres() { return genres;}
    public String getSpotifyURL() { return spotifyURL; }
    public String getArtistURL() { return artistURL; }
    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }
}
