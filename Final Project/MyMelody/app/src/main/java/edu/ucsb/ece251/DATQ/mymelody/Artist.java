package edu.ucsb.ece251.DATQ.mymelody;

public class Artist {
    private String id;
    private String name;
    private int rating;
    private boolean isSavedToFirebase = false;
    public Artist(){} //need for firebase
    public Artist(String id, String name, int rating) {
        this.id = id;
        this.name = name;
        this.rating = rating;
    }
    public boolean isSavedToFirebase() {
        return isSavedToFirebase;
    }

    public void setSavedToFirebase(boolean savedToFirebase) {
        isSavedToFirebase = savedToFirebase;
    }

    // Getters and setters
    public String getName() {
        return name;
    }
    public String getId(){return id; }

    public void setName(String name) {
        this.name = name;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }
}
