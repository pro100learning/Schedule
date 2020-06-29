package com.softserve.service.impl;


import com.softserve.dto.*;
import com.softserve.entity.*;
import com.softserve.entity.enums.EvenOdd;
import com.softserve.exception.EntityNotFoundException;
import com.softserve.exception.ScheduleConflictException;
import com.softserve.mapper.*;
import com.softserve.repository.ScheduleRepository;
import com.softserve.service.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Transactional
@Service
@Slf4j
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;

    private final LessonService lessonService;
    private final RoomService roomService;
    private final GroupService groupService;
    private final TeacherWishesService teacherWishesService;
    private final TeacherService teacherService;
    private final SemesterService semesterService;
    private final TemporaryScheduleService temporaryScheduleService;

    private final GroupMapper groupMapper;
    private final PeriodMapper periodMapper;
    private final LessonsInScheduleMapper lessonsInScheduleMapper;
    private final RoomForScheduleMapper roomForScheduleMapper;
    private final TeacherMapper teacherMapper;
    private final LessonForTeacherScheduleMapper lessonForTeacherScheduleMapper;


    @Autowired
    public ScheduleServiceImpl(ScheduleRepository scheduleRepository, LessonService lessonService, RoomService roomService, GroupService groupService, TeacherWishesService teacherWishesService, TeacherService teacherService, SemesterService semesterService, GroupMapper groupMapper, PeriodMapper periodMapper, LessonsInScheduleMapper lessonsInScheduleMapper, RoomForScheduleMapper roomForScheduleMapper, TeacherMapper teacherMapper, LessonForTeacherScheduleMapper lessonForTeacherScheduleMapper, TemporaryScheduleService temporaryScheduleService) {
        this.scheduleRepository = scheduleRepository;
        this.lessonService = lessonService;
        this.roomService = roomService;
        this.groupService = groupService;
        this.teacherService = teacherService;
        this.semesterService = semesterService;
        this.temporaryScheduleService = temporaryScheduleService;
        this.groupMapper = groupMapper;
        this.teacherWishesService = teacherWishesService;
        this.periodMapper = periodMapper;
        this.lessonsInScheduleMapper = lessonsInScheduleMapper;
        this.roomForScheduleMapper = roomForScheduleMapper;
        this.teacherMapper = teacherMapper;
        this.lessonForTeacherScheduleMapper = lessonForTeacherScheduleMapper;
    }

    /**
     * Method gets information from Repository for particular Schedule with id parameter
     *
     * @param id Identity number of the Schedule
     * @return Schedule entity
     */
    @Override
    public Schedule getById(Long id) {
        log.info("In getById(id = [{}])", id);
        Schedule schedule = scheduleRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException(Schedule.class, "id", id.toString()));
        Hibernate.initialize(schedule.getLesson().getSemester().getDaysOfWeek());
        Hibernate.initialize(schedule.getLesson().getSemester().getPeriods());
        return schedule;
    }

    /**
     * Method gets information about all Schedules from Repository
     *
     * @return List of all Schedules
     */
    @Override
    public List<Schedule> getAll() {
        log.info("In getAll()");
        List<Schedule> schedules = scheduleRepository.getAll();
        for (Schedule schedule : schedules) {
            Hibernate.initialize(schedule.getLesson().getSemester().getDaysOfWeek());
            Hibernate.initialize(schedule.getLesson().getSemester().getPeriods());
        }
        return schedules;
    }

    /**
     * Method saves new Schedule to Repository
     *
     * @param object Schedule entity with info to be saved
     * @return saved Schedule entity
     */
    @Override
    public Schedule save(Schedule object) {
        log.info("In save(entity = [{}]", object);
        if (isConflictForGroupInSchedule(semesterService.getCurrentSemester().getId(), object.getDayOfWeek(), object.getEvenOdd(), object.getPeriod().getId(), object.getLesson().getId())) {
            log.error("Schedule for group with id [{}] has conflict with already existing", object.getLesson().getGroup().getId());
            throw new ScheduleConflictException("You can't create schedule item for this group, because one already exists");
        } else {
            return scheduleRepository.save(object);
        }
    }

    /**
     * Method updates information for an existing Schedule in Repository
     *
     * @param object Schedule entity with info to be updated
     * @return updated Schedule entity
     */
    @Override
    public Schedule update(Schedule object) {
        log.info("In update(entity = [{}]", object);
        if (isConflictForGroupInSchedule(object.getLesson().getSemester().getId(), object.getDayOfWeek(), object.getEvenOdd(), object.getPeriod().getId(), object.getLesson().getId())) {
            throw new ScheduleConflictException("You can't update schedule item for this group, because it violates already existing");
        } else {
            return scheduleRepository.update(object);
        }
    }

    /**
     * Method deletes an existing Schedule from Repository
     *
     * @param object Schedule entity to be deleted
     * @return deleted Schedule entity
     */
    @Override
    public Schedule delete(Schedule object) {
        return scheduleRepository.delete(object);
    }

    /**
     * Method returns necessary info to finish saving schedule
     *
     * @param semesterId the semester id in which schedule have to be saved
     * @param dayOfWeek  the semester id in which schedule have to be saved
     * @param evenOdd    lesson occurs by EVEN/ODD/WEEKLY
     * @param classId    period id in which schedule have to be saved
     * @param lessonId   lesson id that pretends t be saved
     * @return CreateScheduleInfoDTO - necessary info to finish saving schedule
     */
    @Override
    public CreateScheduleInfoDTO getInfoForCreatingSchedule(Long semesterId, DayOfWeek dayOfWeek, EvenOdd evenOdd, Long classId, Long lessonId) {
        log.info("In getInfoForCreatingSchedule (semesterId = [{}], dayOfWeek = [{}], evenOdd = [{}], classId = [{}], lessonId = [{}])", semesterId, dayOfWeek, evenOdd, classId, lessonId);
        //checking for missing parameters and wrong types is skipped, because it handles automatically by GlobalExceptionHandler
        if (isConflictForGroupInSchedule(semesterId, dayOfWeek, evenOdd, classId, lessonId)) {
            log.error("Schedule for group already exists");
            throw new ScheduleConflictException("You can't create schedule for this group, because one already exists");
        } else {
            CreateScheduleInfoDTO createScheduleInfoDTO = new CreateScheduleInfoDTO();
            createScheduleInfoDTO.setTeacherAvailable(isTeacherAvailableForSchedule(semesterId, dayOfWeek, evenOdd, classId, lessonId));
            createScheduleInfoDTO.setRooms(roomService.getAllRoomsForCreatingSchedule(semesterId, dayOfWeek, evenOdd, classId));
            createScheduleInfoDTO.setClassSuitsToTeacher(teacherWishesService.isClassSuits(lessonService.getById(lessonId).getTeacher().getId(), dayOfWeek, evenOdd, classId));
            return createScheduleInfoDTO;
        }

    }

    //verifies if group has conflict in schedule when it saves
    private boolean isConflictForGroupInSchedule(Long semesterId, DayOfWeek dayOfWeek, EvenOdd evenOdd, Long classId, Long lessonId) {
        log.info("In isConflictForGroupInSchedule(semesterId = [{}], dayOfWeek = [{}], evenOdd = [{}], classId = [{}], lessonId = [{}])", semesterId, dayOfWeek, evenOdd, classId, lessonId);
        //Get group ID from Lesson by lesson ID to search further by group ID
        Long groupId = lessonService.getById(lessonId).getGroup().getId();
        //If Repository doesn't count any records that means there are no conflicts for this group at that point of time
        return scheduleRepository.conflictForGroupInSchedule(semesterId, dayOfWeek, evenOdd, classId, groupId) != 0;
    }

    //verifies if teacher already has another schedule at  at some semester (by semester id) at some day for some period(by classId)
    private boolean isTeacherAvailableForSchedule(Long semesterId, DayOfWeek dayOfWeek, EvenOdd evenOdd, Long classId, Long lessonId) {
        log.info("In isTeacherAvailable (semesterId = [{}], dayOfWeek = [{}], evenOdd = [{}], classId = [{}], lessonId = [{}]", semesterId, dayOfWeek, evenOdd, classId, lessonId);
        //Get teacher ID from Lesson by lesson ID to search further by teacher ID
        Long teacherId = lessonService.getById(lessonId).getTeacher().getId();
        return scheduleRepository.conflictForTeacherInSchedule(semesterId, dayOfWeek, evenOdd, classId, teacherId) == 0;
    }

    /**
     * Method gets full schedule for groups in particular semester
     *
     * @param semesterId id of semester
     * @param groupId    group id
     * @return filled schedule for group
     */
    @Override
    public List<ScheduleForGroupDTO> getFullScheduleForGroup(Long semesterId, Long groupId) {
        log.info("In getFullSchedule(semesterId = [{}], groupId[{}])", semesterId, groupId);
        List<ScheduleForGroupDTO> scheduleForGroupDTOList = new ArrayList<>();
        List<Group> groupsForSchedule = new ArrayList<>();
        if (semesterId != null && groupId != null) {
            if (groupHasScheduleInSemester(semesterId, groupId)) {
                groupsForSchedule.add(groupService.getById(groupId));
                ScheduleForGroupDTO scheduleForGroupDTO = new ScheduleForGroupDTO();
                scheduleForGroupDTO.setGroup(groupMapper.groupToGroupDTO(groupsForSchedule.get(0)));
                scheduleForGroupDTO.setDays(getDaysWhenGroupHasClassesBySemester(semesterId, groupId));
                scheduleForGroupDTOList.add(scheduleForGroupDTO);
            }
            return scheduleForGroupDTOList;
        } else {
            groupsForSchedule.addAll(scheduleRepository.uniqueGroupsInScheduleBySemester(semesterId));
            for (Group group : groupsForSchedule) {
                ScheduleForGroupDTO scheduleForGroupDTO = new ScheduleForGroupDTO();
                scheduleForGroupDTO.setGroup(groupMapper.groupToGroupDTO(group));
                scheduleForGroupDTO.setDays(getDaysWhenGroupHasClassesBySemester(semesterId, group.getId()));
                scheduleForGroupDTOList.add(scheduleForGroupDTO);
            }
            return scheduleForGroupDTOList;
        }
    }

    //gets days when group has schedule and fill days by classes
    private List<DaysOfWeekWithClassesForGroupDTO> getDaysWhenGroupHasClassesBySemester(Long semesterId, Long groupId) {
        log.info("In getDaysWhenGroupHasClassesBySemester(semesterId = [{}], groupId = [{}])", semesterId, groupId);
        List<DaysOfWeekWithClassesForGroupDTO> daysOfWeekWithClassesForGroupDTOList = new ArrayList<>();
        List<DayOfWeek> weekList = scheduleRepository.getDaysWhenGroupHasClassesBySemester(semesterId, groupId);
        weekList.sort(Comparator.comparingInt(DayOfWeek::getValue));
        for (DayOfWeek day : weekList) {
            DaysOfWeekWithClassesForGroupDTO daysOfWeekWithClassesForGroupDTO = new DaysOfWeekWithClassesForGroupDTO();
            daysOfWeekWithClassesForGroupDTO.setDay(day);
            daysOfWeekWithClassesForGroupDTO.setClasses(getClassesForGroupBySemesterByDayOfWeek(semesterId, groupId, day));
            daysOfWeekWithClassesForGroupDTOList.add(daysOfWeekWithClassesForGroupDTO);

        }

        return daysOfWeekWithClassesForGroupDTOList;
    }

    //get classes in Day when group has schedule and fill classes by even/odd lessons
    private List<ClassesInScheduleForGroupDTO> getClassesForGroupBySemesterByDayOfWeek(Long semesterId, Long groupId, DayOfWeek day) {
        log.info("In getClassesForGroupBySemesterByDayOfWeek(semesterId = [{}], groupId = [{}], day = [{}])", semesterId, groupId, day);
        //get Classes in that Day for group
        List<Period> uniquePeriods = scheduleRepository.periodsForGroupByDayBySemester(semesterId, groupId, day);
        List<ClassesInScheduleForGroupDTO> classesInScheduleForGroupDTOList = new ArrayList<>();

        for (Period period : uniquePeriods) {
            ClassesInScheduleForGroupDTO classesInScheduleForGroupDTO = new ClassesInScheduleForGroupDTO();
            classesInScheduleForGroupDTO.setPeriod(periodMapper.convertToDto(period));
            classesInScheduleForGroupDTO.setWeeks(getLessonsForGroupForPeriodBySemesterAndDay(semesterId, groupId, period.getId(), day));
            classesInScheduleForGroupDTOList.add(classesInScheduleForGroupDTO);
        }

        return classesInScheduleForGroupDTOList;
    }

    //get and fill even and odd lessons for group at some semester (by semester id) at some day for some period(by periodId)
    private LessonInScheduleByWeekDTO getLessonsForGroupForPeriodBySemesterAndDay(Long semesterId, Long groupId, Long periodId, DayOfWeek day) {
        log.info("In getLessonsForGroupForPeriodBySemesterAndDay(semesterId = [{}], groupId = [{}], periodId = [{}], day = [{}])", semesterId, groupId, periodId, day);
        LessonInScheduleByWeekDTO lessonInScheduleByWeekDTO = new LessonInScheduleByWeekDTO();
        Lesson lesson = scheduleRepository.lessonForGroupByDayBySemesterByPeriodByWeek(semesterId, groupId, periodId, day, EvenOdd.EVEN).orElse(null);
        LessonsInScheduleDTO even = lessonsInScheduleMapper.lessonToLessonsInScheduleDTO(lesson);

        if (lesson != null) {
            even.setRoom(roomForScheduleMapper.roomToRoomForScheduleDTO(scheduleRepository.getRoomForLesson(semesterId, periodId, lesson.getId(), day, EvenOdd.EVEN)));
            lessonInScheduleByWeekDTO.setEven(even);
        }

        Lesson lesson2 = scheduleRepository.lessonForGroupByDayBySemesterByPeriodByWeek(semesterId, groupId, periodId, day, EvenOdd.ODD).orElse(null);
        LessonsInScheduleDTO odd = lessonsInScheduleMapper.lessonToLessonsInScheduleDTO(lesson2);
        if (lesson2 != null) {
            odd.setRoom(roomForScheduleMapper.roomToRoomForScheduleDTO(scheduleRepository.getRoomForLesson(semesterId, periodId, lesson2.getId(), day, EvenOdd.ODD)));
            lessonInScheduleByWeekDTO.setOdd(odd);
        }

        return lessonInScheduleByWeekDTO;
    }

    //verifies if group with groupId has Schedule in semester with semesterId
    private boolean groupHasScheduleInSemester(Long semesterId, Long groupId) {
        log.info("In groupHasScheduleInSemester(semesterId = [{}], groupId = [{}])", semesterId, groupId);
        return scheduleRepository.countSchedulesForGroupInSemester(semesterId, groupId) != 0;
    }

    /**
     * Method gets full schedule for groups in particular semester
     *
     * @param semesterId id of semester
     * @return filled schedule for all groups that have any lessons in that semester
     */
    @Override
    public ScheduleFullDTO getFullScheduleForSemester(Long semesterId) {
        ScheduleFullDTO scheduleFullDTO = new ScheduleFullDTO();
        SemesterMapper semesterMapper = new SemesterMapperImpl();
        scheduleFullDTO.setSemester(semesterMapper.semesterToSemesterDTO(semesterService.getById(semesterId)));

        List<ScheduleForGroupDTO> scheduleForGroupDTOList = new ArrayList<>();
        List<Group> groupsForSchedule = new ArrayList<>();

        groupsForSchedule.addAll(scheduleRepository.uniqueGroupsInScheduleBySemester(semesterId));
        for (Group group : groupsForSchedule) {
            ScheduleForGroupDTO scheduleForGroupDTO = new ScheduleForGroupDTO();
            scheduleForGroupDTO.setGroup(groupMapper.groupToGroupDTO(group));
            scheduleForGroupDTO.setDays(getDaysForSemester(semesterId, group.getId()));
            scheduleForGroupDTOList.add(scheduleForGroupDTO);

        }
        scheduleFullDTO.setSchedule(scheduleForGroupDTOList);
        return scheduleFullDTO;

    }

    private List<DaysOfWeekWithClassesForGroupDTO> getDaysForSemester(Long semesterId, Long groupId) {
        log.info("In getDaysForSemester(semesterId = [{}])", semesterId);
        List<DaysOfWeekWithClassesForGroupDTO> daysOfWeekWithClassesForGroupDTOList = new ArrayList<>();
        Set<DayOfWeek> weekList = semesterService.getById(semesterId).getDaysOfWeek();
        TreeSet<DayOfWeek> dayOfWeeks = new TreeSet<>(weekList);
        for (DayOfWeek day : dayOfWeeks) {
            DaysOfWeekWithClassesForGroupDTO daysOfWeekWithClassesForGroupDTO = new DaysOfWeekWithClassesForGroupDTO();
            daysOfWeekWithClassesForGroupDTO.setDay(day);
            daysOfWeekWithClassesForGroupDTO.setClasses(getClassesForSemesterByDay(semesterId, day, groupId));
            daysOfWeekWithClassesForGroupDTOList.add(daysOfWeekWithClassesForGroupDTO);

        }

        return daysOfWeekWithClassesForGroupDTOList;
    }

    private List<ClassesInScheduleForGroupDTO> getClassesForSemesterByDay(Long semesterId, DayOfWeek day, Long groupId) {
        log.info("In getClassesForSemester(semesterId = [{}])", semesterId);
        //get Classes in that Day for group
        Set<Period> semesterPeriods = semesterService.getById(semesterId).getPeriods();
        List<ClassesInScheduleForGroupDTO> classesInScheduleForGroupDTOList = new ArrayList<>();
        for (Period period : semesterPeriods) {
            ClassesInScheduleForGroupDTO classesInScheduleForGroupDTO = new ClassesInScheduleForGroupDTO();
            classesInScheduleForGroupDTO.setPeriod(periodMapper.convertToDto(period));
            classesInScheduleForGroupDTO.setWeeks(getLessonsForGroupForPeriodBySemesterAndDay(semesterId, groupId, period.getId(), day));
            classesInScheduleForGroupDTOList.add(classesInScheduleForGroupDTO);
        }

        return classesInScheduleForGroupDTOList;
    }

    /**
     * Method gets full schedule for teacher in particular semester
     *
     * @param semesterId id of semester
     * @param teacherId  id of teacher
     * @return filled schedule for teacher
     */
    @Override
    public ScheduleForTeacherDTO getScheduleForTeacher(Long semesterId, Long teacherId) {
        log.info("In getScheduleForTeacher(semesterId = [{}], teacherId[{}])", semesterId, teacherId);
        ScheduleForTeacherDTO scheduleForTeacherDTO = new ScheduleForTeacherDTO();
        SemesterMapper semesterMapper = new SemesterMapperImpl();
        scheduleForTeacherDTO.setSemester(semesterMapper.semesterToSemesterDTO(semesterService.getById(semesterId)));
        //get Teacher Info
        scheduleForTeacherDTO.setTeacher(teacherMapper.teacherToTeacherDTO(teacherService.getById(teacherId)));

        List<DayOfWeek> weekList = scheduleRepository.getDaysWhenTeacherHasClassesBySemester(semesterId, teacherId);
        weekList.sort(Comparator.comparingInt(DayOfWeek::getValue));

        List<DaysOfWeekWithClassesForTeacherDTO> daysOfWeekWithClassesForTeacherDTOList = new ArrayList<>();
        for (DayOfWeek day : weekList) {
            DaysOfWeekWithClassesForTeacherDTO daysOfWeekWithClassesForTeacherDTO = new DaysOfWeekWithClassesForTeacherDTO();
            daysOfWeekWithClassesForTeacherDTO.setDay(day);
            daysOfWeekWithClassesForTeacherDTO.setEvenWeek(getInfoForTeacherScheduleByWeek(semesterId, teacherId, day, EvenOdd.EVEN));
            daysOfWeekWithClassesForTeacherDTO.setOddWeek(getInfoForTeacherScheduleByWeek(semesterId, teacherId, day, EvenOdd.ODD));
            daysOfWeekWithClassesForTeacherDTOList.add(daysOfWeekWithClassesForTeacherDTO);
        }
        scheduleForTeacherDTO.setDays(daysOfWeekWithClassesForTeacherDTOList);
        return scheduleForTeacherDTO;
    }

    private ClassesInScheduleForTeacherDTO getInfoForTeacherScheduleByWeek(Long semesterId, Long teacherId, DayOfWeek day, EvenOdd evenOdd) {
        List<ClassForTeacherScheduleDTO> classForTeacherScheduleDTOList = new ArrayList<>();

        ClassesInScheduleForTeacherDTO classesInScheduleForTeacherDTO = new ClassesInScheduleForTeacherDTO();

        List<Period> periodList = scheduleRepository.periodsForTeacherBySemesterByDayByWeek(semesterId, teacherId, day, evenOdd);

        if (!periodList.isEmpty()) {
            for (Period period : periodList) {
                ClassForTeacherScheduleDTO classForTeacherScheduleDTO = new ClassForTeacherScheduleDTO();
                classForTeacherScheduleDTO.setPeriod(periodMapper.convertToDto(period));
                classForTeacherScheduleDTO.setLessons(getLessonsForTeacherBySemesterByDayByWeekByPeriod(semesterId, teacherId, day, evenOdd, period.getId()));
                classForTeacherScheduleDTOList.add(classForTeacherScheduleDTO);

            }
        }
        classesInScheduleForTeacherDTO.setPeriods(classForTeacherScheduleDTOList);
        return classesInScheduleForTeacherDTO;
    }

    private List<LessonForTeacherScheduleDTO> getLessonsForTeacherBySemesterByDayByWeekByPeriod(Long semesterId, Long teacherId, DayOfWeek day, EvenOdd evenOdd, Long periodId) {
        List<LessonForTeacherScheduleDTO> lessonForTeacherScheduleDTOList = new ArrayList<>();
        List<Lesson> lessons = scheduleRepository.lessonsForTeacherBySemesterByDayByPeriodByWeek(semesterId, teacherId, periodId, day, evenOdd);
        for (Lesson lesson : lessons) {
            LessonForTeacherScheduleDTO lessonForTeacherScheduleDTO = lessonForTeacherScheduleMapper.lessonToLessonForTeacherScheduleDTO(lesson);
            lessonForTeacherScheduleDTO.setRoom(scheduleRepository.getRoomForLesson(semesterId, periodId, lessonForTeacherScheduleDTO.getId(), day, evenOdd).getName());
            lessonForTeacherScheduleDTOList.add(lessonForTeacherScheduleDTO);
        }
        return lessonForTeacherScheduleDTOList;
    }

    @Override
    public List<Schedule> getAllSchedulesByTeacherIdAndSemesterId(Long teacherId, Long semesterId) {
        log.info("Enter into getAllSchedulesByTeacherIdAndSemesterId with teacherId = {}, semesterId = {}", teacherId, semesterId);
        return scheduleRepository.getAllSchedulesByTeacherIdAndSemesterId(teacherId, semesterId);
    }

    //get classes in Day when room has schedule and fill classes by even/odd lessons
    private List<RoomClassesInScheduleDTO> getPeriodsForRoomBySemesterByDayOfWeek(Long semesterId, Long roomId, DayOfWeek day) {
        log.info("In getPeriodsForRoomBySemesterByDayOfWeek(semesterId = [{}], groupId = [{}], day = [{}])", semesterId, roomId, day);
        //get Classes in that Day for group
        List<Period> uniquePeriods = scheduleRepository.getPeriodsForRoomBySemesterByDayOfWeek(semesterId, roomId, day);
        List<RoomClassesInScheduleDTO> roomClassesInScheduleDTOList = new ArrayList<>();
        RoomClassesInScheduleDTO roomClassesInScheduleDTO = new RoomClassesInScheduleDTO();
        List<LessonsInRoomScheduleDTO> evenLessonsInRoomScheduleDTOList = new ArrayList<>();
        List<LessonsInRoomScheduleDTO> oddLessonsInRoomScheduleDTOList = new ArrayList<>();
        for (Period period : uniquePeriods) {

            List<Lesson> evenLesson = scheduleRepository.lessonForRoomByDayBySemesterByPeriodByWeek(semesterId, roomId, period.getId(), day, EvenOdd.EVEN);
            for (Lesson lessonItem : evenLesson) {
                LessonsInRoomScheduleDTO even = lessonsInScheduleMapper.lessonToLessonsInRoomScheduleDTO(lessonItem);
                even.setSurname(lessonItem.getTeacher().getSurname());
                even.setSubjectName(lessonItem.getSubject().getName());
                even.setGroupId(lessonItem.getGroup().getId());
                even.setLessonId(lessonItem.getId());
                even.setGroupName(lessonItem.getGroup().getTitle());
                even.setClassName(period.getName());
                even.setClassId(period.getId());
                evenLessonsInRoomScheduleDTOList.add(even);
            }

            List<Lesson> oddLesson = scheduleRepository.lessonForRoomByDayBySemesterByPeriodByWeek(semesterId, roomId, period.getId(), day, EvenOdd.ODD);
            for (Lesson lessonItem : oddLesson) {
                LessonsInRoomScheduleDTO odd = lessonsInScheduleMapper.lessonToLessonsInRoomScheduleDTO(lessonItem);
                odd.setSurname(lessonItem.getTeacher().getSurname());
                odd.setSubjectName(lessonItem.getSubject().getName());
                odd.setGroupId(lessonItem.getGroup().getId());
                odd.setLessonId(lessonItem.getId());
                odd.setGroupName(lessonItem.getGroup().getTitle());
                odd.setClassName(period.getName());
                odd.setClassId(period.getId());
                oddLessonsInRoomScheduleDTOList.add(odd);
            }

        }
        roomClassesInScheduleDTO.setEven(evenLessonsInRoomScheduleDTOList);
        roomClassesInScheduleDTO.setOdd(oddLessonsInRoomScheduleDTOList);
        roomClassesInScheduleDTOList.add(roomClassesInScheduleDTO);
        return roomClassesInScheduleDTOList;
    }

    private List<DaysOfWeekWithClassesForRoomDTO> getDaysWhenRoomHasClassesBySemester(Long semesterId, Long roomId) {
        log.info("In getDaysWhenRoomHasClassesBySemester(semesterId = [{}], groupId = [{}])", semesterId, roomId);
        List<DaysOfWeekWithClassesForRoomDTO> daysOfWeekWithClassesForRoomDTOList = new ArrayList<>();

        for (DayOfWeek day : DayOfWeek.values()) {
            DaysOfWeekWithClassesForRoomDTO daysOfWeekWithClassesForRoomDTO = new DaysOfWeekWithClassesForRoomDTO();
            daysOfWeekWithClassesForRoomDTO.setDay(day);
            daysOfWeekWithClassesForRoomDTO.setClasses(getPeriodsForRoomBySemesterByDayOfWeek(semesterId, roomId, day));
            daysOfWeekWithClassesForRoomDTOList.add(daysOfWeekWithClassesForRoomDTO);
        }
        return daysOfWeekWithClassesForRoomDTOList;
    }

    @Override
    public List<ScheduleForRoomDTO> getScheduleForRooms(Long semesterId) {
        log.info("Enter into getScheduleForRooms");
        List<ScheduleForRoomDTO> scheduleForGroupDTOList = new ArrayList<>();
        List<Room> roomForDetails = roomService.getAll();

        for (Room room : roomForDetails) {
            ScheduleForRoomDTO scheduleForRoomDTO = new ScheduleForRoomDTO();
            scheduleForRoomDTO.setRoomId(room.getId());
            scheduleForRoomDTO.setRoomName(room.getName());
            scheduleForRoomDTO.setRoomType(room.getType().getDescription());
            scheduleForRoomDTO.setSchedules(getDaysWhenRoomHasClassesBySemester(semesterId, room.getId()));
            scheduleForGroupDTOList.add(scheduleForRoomDTO);
        }
        return scheduleForGroupDTOList;
    }

    @Override
    public List<Schedule> getSchedulesBySemester(Long semesterId) {
        log.info("In getScheduleBySemester(Long semesterId = [{}])", semesterId);
        List<Schedule> schedules = scheduleRepository.getScheduleBySemester(semesterId);
        for (Schedule schedule : schedules) {
            Hibernate.initialize(schedule.getLesson().getSemester().getDaysOfWeek());
            Hibernate.initialize(schedule.getLesson().getSemester().getPeriods());
        }
        return schedules;
    }

    /**
     * Method scheduleByDateRangeForTeacher get all schedules from db in particular date range
     *
     * @param fromDate  LocalDate from
     * @param toDate    LocalDate to
     * @param teacherId id teacher
     * @return list of schedules
     */
    @Override
    public Map<LocalDate, Map<Period, List<Schedule>>> scheduleByDateRangeForTeacher(LocalDate fromDate, LocalDate toDate, Long teacherId) {
        log.info("In scheduleByDateRangeForTeacher with fromDate = {} and toDate = {}", fromDate, toDate);
        List<Schedule> schedules = scheduleRepository.scheduleByDateRangeForTeacher(fromDate, toDate, teacherId);

        List<Schedule> dateRangeSchedule = new ArrayList<>();
        for (Schedule schedule : schedules) {
            Hibernate.initialize(schedule.getLesson().getSemester().getPeriods());
            if (isDateInSemesterDateRange(schedule, toDate)) {
                dateRangeSchedule.add(schedule);
            }
        }

return fullScheduleForTeacherByDateRange(dateRangeSchedule,  fromDate, toDate);
    }


    /**
     * Method temporaryScheduleByDateRangeForTeacher get all schedules and temporary schedules from db in particular date range
     *
     * @param fromDate  LocalDate from
     * @param toDate    LocalDate to
     * @param teacherId id teacher
     * @return list of schedules and temporary schedules
     */
    @Override
    public Map<LocalDate, Map<Period, List<Map<Schedule, TemporarySchedule>>>> temporaryScheduleByDateRangeForTeacher(LocalDate fromDate, LocalDate toDate, Long teacherId) {
        log.info("In temporaryScheduleByDateRangeForTeacher with fromDate = {} and toDate = {} and teacher = {}", fromDate, toDate, teacherId);
        List<Schedule> schedules = scheduleRepository.scheduleByDateRangeForTeacher(fromDate, toDate, teacherId);
        List<TemporarySchedule> temporarySchedules = temporaryScheduleService.temporaryScheduleByDateRangeForTeacher(fromDate, toDate, teacherId);
        List<TemporarySchedule> vacationByDateRangeForTeacher = temporaryScheduleService.vacationByDateRangeForTeacher(fromDate, toDate);

        List<Schedule> dateRangeSchedule = new ArrayList<>();
        for (Schedule schedule : schedules) {
            if (isDateInSemesterDateRange(schedule, toDate)) {
                dateRangeSchedule.add(schedule);
            }
        }

        return convertToMapTemporaryScheduleDateRange(fullScheduleForTeacherByDateRange(dateRangeSchedule, fromDate, toDate), temporarySchedules, vacationByDateRangeForTeacher);
    }

    /**
     * Method deleteSchedulesBySemesterId delete all schedules from db in with current seemsterId
     *
     * @param semesterId id Semester for delete schedule
     */
    @Override
    public void deleteSchedulesBySemesterId(Long semesterId) {
        log.info("In deleteSchedulesBySemesterId with semesterId = {}", semesterId);
        scheduleRepository.deleteSchedulesBySemesterId(semesterId);
    }

    /**
     * Method saveScheduleDuringCopy save Schedule in db
     *
     * @param schedule Schedule entity for save schedule in db
     * @return Schedule entity after saved in db
     */
    @Override
    public Schedule saveScheduleDuringCopy(Schedule schedule) {
        log.info("In saveScheduleDuringCopy with schedule = {}", schedule);
        return scheduleRepository.save(schedule);
    }

    /**
     * Method updateWithoutChecks update Schedule in db
     *
     * @param schedule Schedule entity for update schedule in db
     * @return Schedule entity after update in db
     */
    @Override
    public Schedule updateWithoutChecks(Schedule schedule) {
        log.info("In updateWithoutChecks with schedule = {}", schedule);
        return scheduleRepository.update(schedule);
    }

    //check date in semester date range, if yes return - true, else - false
    private boolean isDateInSemesterDateRange(Schedule schedule, LocalDate toDate) {
        DayOfWeek startSemester = schedule.getLesson().getSemester().getStartDay().getDayOfWeek();

        if (schedule.getEvenOdd() == EvenOdd.ODD) {
            if (startSemester.getValue() > schedule.getDayOfWeek().getValue()) {
                int i = startSemester.getValue() - schedule.getDayOfWeek().getValue();
                LocalDate firstCaseDate = schedule.getLesson().getSemester().getStartDay().plusDays(14 - i);

                return checkDateRangeForReturn(firstCaseDate, schedule.getLesson().getSemester().getEndDay(), toDate);
            }
            int k = schedule.getDayOfWeek().getValue() - startSemester.getValue();
            LocalDate secondCaseDate = schedule.getLesson().getSemester().getStartDay().plusDays(k);

            return checkDateRangeForReturn(secondCaseDate, schedule.getLesson().getSemester().getEndDay(), toDate);
        }

        if (schedule.getEvenOdd() == EvenOdd.EVEN || schedule.getEvenOdd() == EvenOdd.WEEKLY) {
            if (startSemester.getValue() > schedule.getDayOfWeek().getValue()) {
                int i = startSemester.getValue() - schedule.getDayOfWeek().getValue();
                LocalDate firstCaseDate = schedule.getLesson().getSemester().getStartDay().plusDays(7 - i);

                return checkDateRangeForReturn(firstCaseDate, schedule.getLesson().getSemester().getEndDay(), toDate);
            }
            int k = schedule.getDayOfWeek().getValue() - startSemester.getValue();
            if (schedule.getEvenOdd() == EvenOdd.WEEKLY) {
                LocalDate secondCaseDate = schedule.getLesson().getSemester().getStartDay().plusDays(k);
                return checkDateRangeForReturn(secondCaseDate, schedule.getLesson().getSemester().getEndDay(), toDate);
            }
            LocalDate thirdCaseDate = schedule.getLesson().getSemester().getStartDay().plusDays(7 + k);
            return checkDateRangeForReturn(thirdCaseDate, schedule.getLesson().getSemester().getEndDay(), toDate);
        }
        return false;
    }

    //this method use for don't duplicate code
    private boolean checkDateRangeForReturn(LocalDate dateForCheck, LocalDate semesterEndDate, LocalDate toDate) {
        return (dateForCheck.isBefore(semesterEndDate) || dateForCheck.isEqual(semesterEndDate)) &&
                (dateForCheck.isBefore(toDate) || dateForCheck.isEqual(toDate));
    }

    //check dates(even/odd/weekly) for distribution in baskets and create Map<LocalDate, Map<Period, List<Schedule>>>
    private Map<LocalDate, Map<Period, List<Schedule>>> fullScheduleForTeacherByDateRange(List<Schedule> schedules, LocalDate fromDate, LocalDate toDate) {
        Map<LocalDate, List<Schedule>> scheduleByDateRange = new LinkedHashMap<>();

        for (LocalDate date = fromDate; date.isBefore(toDate.plusDays(1)); date = date.plusDays(1)) {
            List<Schedule> scheduleList = new ArrayList<>();
            for (Schedule schedule : schedules) {
                if (date.getDayOfWeek() == schedule.getDayOfWeek() && (date.isBefore(schedule.getLesson().getSemester().getEndDay()) ||
                        date.isEqual(schedule.getLesson().getSemester().getEndDay())) && (date.isAfter(schedule.getLesson().getSemester().getStartDay())
                        || date.isEqual(schedule.getLesson().getSemester().getStartDay()))) {
                    int countStartDate = schedule.getLesson().getSemester().getStartDay().getDayOfWeek().getValue();
                    int countEndDate = date.getDayOfWeek().getValue();
                    int countDays = Integer.parseInt(String.valueOf(ChronoUnit.DAYS.between(
                            schedule.getLesson().getSemester().getStartDay().minusDays(countStartDate), date.plusDays(7 - countEndDate))));

                    switch (schedule.getEvenOdd()) {
                        case ODD:
                            if ((countDays / 7) % 2 != 0) {
                                scheduleList.add(schedule);
                            }
                            break;
                        case EVEN:
                            if ((countDays / 7) % 2 == 0) {
                                scheduleList.add(schedule);
                            }
                            break;
                        case WEEKLY:
                            scheduleList.add(schedule);
                            break;
                    }
                }
            }
            if (!scheduleList.isEmpty()) {
                scheduleByDateRange.put(date, scheduleList);
            }
        }
        return convertToMapScheduleDateRange(scheduleByDateRange);
    }

    //convert from Map<LocalDate, List<Schedule>> to Map<LocalDate, Map<Period, List<Schedule>>> for easy way to convert dto in future
    private Map<LocalDate, Map<Period, List<Schedule>>> convertToMapScheduleDateRange(Map<LocalDate, List<Schedule>> scheduleByDateRange) {
        Map<LocalDate, Map<Period, List<Schedule>>> map = new LinkedHashMap<>();

        for (Map.Entry<LocalDate, List<Schedule>> itr : scheduleByDateRange.entrySet()) {
            Map<Period, List<Schedule>> collect = itr.getValue().stream()
                    .collect(Collectors.groupingBy(Schedule::getPeriod));

            /*Map<Period, List<Schedule>> sorted = collect.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(Comparator.comparing(Period::getName)))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                            (oldValue, newValue) -> oldValue, LinkedHashMap::new));*/
            Map<Period, List<Schedule>> sorted = new LinkedHashMap<>();
            collect.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(Period::getName)))
                    .forEachOrdered(x -> sorted.put(x.getKey(), x.getValue()));
            map.put(itr.getKey(), sorted);
        }
        return map;
    }


    private Map<LocalDate, Map<Period, List<Map<Schedule, TemporarySchedule>>>> convertToMapTemporaryScheduleDateRange(Map<LocalDate, Map<Period, List<Schedule>>> scheduleByDateRange,
                                                                                                                       List<TemporarySchedule> temporarySchedules,
                                                                                                                       List<TemporarySchedule> vacationByDateRangeForTeacher) {
        Map<LocalDate, Map<Period, List<Map<Schedule, TemporarySchedule>>>> map = new LinkedHashMap<>();
        for (Map.Entry<LocalDate, Map<Period, List<Schedule>>> itr : scheduleByDateRange.entrySet()) {
            Map<Period, List<Map<Schedule, TemporarySchedule>>> periodListHashMap = new HashMap<>();

            for (Map.Entry<Period, List<Schedule>> entry : itr.getValue().entrySet()) {
                List<Map<Schedule, TemporarySchedule>> mapList = new ArrayList<>();
                Map<Schedule, TemporarySchedule> temporaryScheduleMap = new LinkedHashMap<>();

                for (Schedule schedule : entry.getValue()) {
                    TemporarySchedule temporarySchedule = temporarySchedules.stream().filter(temporarySchedule1 ->
                            temporarySchedule1.getScheduleId().equals(schedule.getId()) && temporarySchedule1.getDate().equals(itr.getKey())
                    ).findFirst().orElse(vacationByDateRangeForTeacher.stream().filter(temporarySchedule1 ->
                           temporarySchedule1.getDate().equals(itr.getKey())
                    ).findFirst().orElse(new TemporarySchedule()));

                    temporaryScheduleMap.put(schedule, temporarySchedule);
                    mapList.add(temporaryScheduleMap);
                }

                periodListHashMap.put(entry.getKey(), mapList);
            }


            map.put(itr.getKey(), periodListHashMap);

        }
        return map;
    }
}



