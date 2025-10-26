package ru.se.ifmo.is1.ws;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data @AllArgsConstructor
public class ChangeEvent {
    private String entity;
    private String action;
    private Number id;
}