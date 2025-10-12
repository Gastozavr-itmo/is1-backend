package ru.se.ifmo.is1.model;

import lombok.Data;

@Data
public class Address {
    private long id;
    private String zipCode; //Длина строки не должна быть больше 29, Поле не может быть null
    private Location town; //Поле не может быть null
}