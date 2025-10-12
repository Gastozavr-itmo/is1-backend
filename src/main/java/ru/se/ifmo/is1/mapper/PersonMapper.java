package ru.se.ifmo.is1.mapper;

import org.springframework.stereotype.Component;
import ru.se.ifmo.is1.dto.person.PersonCreateDTO;
import ru.se.ifmo.is1.dto.person.PersonViewDTO;
import ru.se.ifmo.is1.dto.person.PersonViewFullDTO;
import ru.se.ifmo.is1.dto.shared.LocationCreateDTO;
import ru.se.ifmo.is1.model.Location;
import ru.se.ifmo.is1.model.Person;

@Component
public class PersonMapper {

    public Person toEntity(PersonCreateDTO dto) {
        Person p = new Person();
        p.setName(dto.getName());
        p.setEyeColor(dto.getEyeColor());
        p.setHairColor(dto.getHairColor());
        LocationCreateDTO ld = dto.getLocation();
        if (ld != null) {
            Location l = new Location();
            l.setX(ld.getX());
            l.setY(ld.getY());
            l.setName(ld.getName());
            p.setLocation(l);
        }
        p.setHeight(dto.getHeight());
        p.setNationality(dto.getNationality());
        return p;
    }

    public PersonViewDTO toLight(Person p) {
        return PersonViewDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .build();
    }

    public PersonViewFullDTO toView(Person p) {
        return PersonViewFullDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .eyeColor(p.getEyeColor())
                .hairColor(p.getHairColor())
                .locationName(p.getLocation() != null ? p.getLocation().getName() : null)
                .height(p.getHeight())
                .nationality(p.getNationality())
                .build();
    }
}
