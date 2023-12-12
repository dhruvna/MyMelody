package edu.ucsb.ece251.DATQ.mymelody;

public class Track {
    private String id;
    private String name;
    private Integer rating;
    private String artist;
    private String albumCover;
    private String previewUrl;
    private String trackUrl;
    public Track() {} //need for firebase
    public Track(String id,String name, int rating, String artist, String albumCover, String previewUrl, String trackUrl) {
        this.id = id;
        this.name = name;
        this.rating = rating;
        this.artist = artist;
        this.albumCover = albumCover;
        this.previewUrl = previewUrl;
        this.trackUrl = trackUrl;
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
    public String getAlbumCover() {
        return albumCover;
    }
    public String getPreviewUrl() {
        return previewUrl;
    }
    public String getTrackUrl() {
        return trackUrl;
    }
}

