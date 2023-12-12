package edu.ucsb.ece251.DATQ.mymelody;

public class Track {
    private String id;
    private String name;
    private Integer rating;
    private String artist;
    public Track() {} //need for firebase
    public Track(String id,String name, int rating, String artist) {
        this.id = id;
        this.name = name;
        this.rating = rating;
        this.artist = artist;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public String getId(){return id;}

    public void setName(String name) {
        this.name = name;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }
    public String getArtist() {
        return artist;
    }

}

