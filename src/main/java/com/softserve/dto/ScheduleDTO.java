package com.softserve.dto;

import com.softserve.entity.enums.EvenOdd;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;

@Getter
@Setter
@NoArgsConstructor
public class ScheduleDTO {
    private long id;
    private SemesterDTO semester;
    private DayOfWeek dayOfWeek;
    private EvenOdd evenOdd;
    private LessonInfoDTO lesson;
    private PeriodDTO period;
    private RoomDTO room;

}
