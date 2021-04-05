package com.example.ownnavi;

public class Pos {
    private String name;
    private double longitude;
    private double latitude;
    private int radius;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public Pos(String name, double longitude, double latitude, int radius) {
        this.name = name;
        this.longitude = longitude;
        this.latitude = latitude;
        this.radius = radius;

    }
}
