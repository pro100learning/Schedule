package com.softserve.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.Filters;
import org.hibernate.annotations.ParamDef;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.io.Serializable;


@NoArgsConstructor
@Setter
@Getter
@Entity
@Table(name = "rooms")

@FilterDef(name = "roomDisableFilter", parameters = {
        @ParamDef(name = "disable", type = "boolean"),
})

@Filters({
        @Filter(name = "roomDisableFilter", condition = "disable = :disable"),
})

public class Room implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @NotEmpty(message = "Name cannot be empty")
    @Size(min = 2, max = 35, message = "Name must be between 2 and 35 characters long")
    @Column(length = 35, nullable = false)
    private String name;

    @ManyToOne(targetEntity = RoomType.class)
    @JoinColumn(name = "room_type_id")
    private RoomType type;

    @Column(name = "disable", columnDefinition = "boolean default 'false'")
    private boolean disable = false;

    @Column(name = "sort_order")
    private Double sortOrder;
}
