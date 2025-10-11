package ru.se.ifmo.is1.model;

public class Organization {
    private int id; //Значение поля должно быть больше 0, Значение этого поля должно быть уникальным, Значение этого поля должно генерироваться автоматически
    private String name; //Поле не может быть null, Строка не может быть пустой
    private Address officialAddress; //Поле не может быть null
    private Double annualTurnover; //Поле не может быть null, Значение поля должно быть больше 0
    private int employeesCount; //Значение поля должно быть больше 0
    private String fullName; //Значение этого поля должно быть уникальным, Длина строки не должна быть больше 1950, Поле может быть null
    private int rating; //Значение поля должно быть больше 0
    private Address postalAddress; //Поле не может быть null
}