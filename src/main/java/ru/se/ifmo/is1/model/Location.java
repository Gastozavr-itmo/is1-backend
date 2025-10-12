package ru.se.ifmo.is1.model;

import lombok.Data;

@Data
public class Location {
    private long id;
    private Long x; //Поле не может быть null
    private Long y; //Поле не может быть null
    private String name; //Строка не может быть пустой, Поле не может быть null
}