package edu.ucsb.ece251.DATQ.mymelody;

public class Track {
    private String id;
    private String name;
    private Integer rating;
    private String artist;
    private String albumCover;
    public Track() {} //need for firebase
    public Track(String id,String name, int rating, String artist, String albumCover) {
        this.id = id;
        this.name = name;
        this.rating = rating;
        this.artist = artist;
        this.albumCover = albumCover;
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

}

