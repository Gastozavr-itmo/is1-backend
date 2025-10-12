package ru.se.ifmo.is1.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data

public class Person {
    private long id;
    private String name; //Поле не может быть null, Строка не может быть пустой
    private Color eyeColor; //Поле может быть null
    private Color hairColor; //Поле может быть null
    private Location location; //Поле может быть null
    private double height; //Значение поля должно быть больше 0
    private Country nationality; //Поле не может быть null
}