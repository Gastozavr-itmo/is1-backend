package ru.se.ifmo.is1.model;

public class Coordinates {
    private Long id;
    private Double x; // not null, max 450
    private Long y;   // not null, > -422

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Double getX() { return x; }
    public void setX(Double x) { this.x = x; }
    public Long getY() { return y; }
    public void setY(Long y) { this.y = y; }
}
