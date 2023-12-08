package edu.ucsb.ece251.DATQ.mymelody;

public class Artist {
    private String id;
    private String name;
    private int rating;
    private String[] genres;
    private String pfpURL;
    public Artist(){} //need for firebase
    public Artist(String id, String name, int rating) {
        this.id = id;
        this.name = name;
        this.rating = rating;
    }
    // Getters and setters
    public void setName(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
    public String getId(){return id; }
    private void setGenres(String[] genres) { this.genres = genres; }

    private String[] getGenres() { return genres;}
    private void setURL(String pfpURL) {this.pfpURL = pfpURL; }
    private String getURL() { return pfpURL; }
    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }
}
