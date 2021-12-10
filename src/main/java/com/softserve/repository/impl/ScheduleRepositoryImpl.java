package com.softserve.repository.impl;

import com.softserve.entity.*;
import com.softserve.entity.enums.EvenOdd;
import com.softserve.repository.ScheduleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@Slf4j
public class ScheduleRepositoryImpl extends BasicRepositoryImpl<Schedule, Long> implements ScheduleRepository {

    private static final String NOT_DISABLED_SQL = " and s.room.disable=false and s.lesson.semester.disable=false " +
            "and s.lesson.group.disable=false  and s.lesson.teacher.disable=false and s.lesson.subject.disable=false ";

    private static final String SELECT_COUNT = "select count (s.id) " +
            "from Schedule s where s.lesson.semester.id = :semesterId " +
            "and s.dayOfWeek = :dayOfWeek " +
            "and s.period.id = :classId " + NOT_DISABLED_SQL;

    private static final String GET_BY_ALL_PARAMETERS = "FROM Schedule s where s.period.id = :periodId " +
            "and s.lesson.id = :lessonId and s.dayOfWeek = :dayOfWeek and s.evenOdd = :evenOdd and s.room.id = :roomId";

    private static final String GET_BY_SEMESTER_TEACHER
            = "select s "
            + "from Schedule s "
            + "where s.lesson.semester.id = :semesterId "
            + "and s.lesson.teacher.id = :teacherId ";

    private static final String GET_BY_ROOM_SEMESTER
            = "SELECT s from Schedule s "
            + "join fetch s.lesson as l "
            + "join fetch l.semester as sem "
            + "join fetch sem.periods "
            + "join fetch  sem.groups "
            + "where s.room.id = :roomId "
            + "and s.lesson.semester.id = :semesterId "
            + "order by s.period.startTime asc ";

    private static final String CONFLICT_IN_GROUP_SCHEDULE
            = SELECT_COUNT +
            "and s.lesson.group.id = :groupId";

    private static final String DELETE_BY_SEMESTER_ID
            = "delete from Schedule s " +
            "where s.id in " +
            "(select sch.id " +
            "from Schedule sch " +
            "where sch.lesson.semester.id = :semesterId)";

    private static final String CONFLICT_IN_GROUP_SCHEDULE_REPEATS
            = CONFLICT_IN_GROUP_SCHEDULE
            + " and ( s.evenOdd = :evenOdd or s.evenOdd = 'WEEKLY')";

    private static final String COUNT_BY_GROUP_SEMESTER
            = "select count (s.id) " +
            "from  Schedule s " +
            "where s.lesson.semester.id = :semesterId " +
            "and s.lesson.group.id = :groupId"
            + NOT_DISABLED_SQL;

    private static final String GET_ALL_BY_TEACHER_SEMESTER
            = "From Schedule s " +
            "where s.lesson.semester.id = :semesterId " +
            "and s.lesson.teacher.id = :teacherId " +
            NOT_DISABLED_SQL;

    private static final String GET_BY_SEMESTER
            = "SELECT distinct s" +
            " from Schedule s " +
            "join fetch s.lesson sl " +
            "join fetch sl.semester slm " +
            "join fetch slm.periods " +
            "join fetch slm.groups " +
            "join fetch slm.daysOfWeek " +
            "where s.lesson.semester.id = :semesterId " +
            NOT_DISABLED_SQL;

    private static final String CONFLICT_TEACHER
            = SELECT_COUNT +
            "and s.lesson.teacher.id = :teacherId ";

    private static final String CONFLICT_TEACHER_EVEN
            = CONFLICT_TEACHER +
            "and ( s.evenOdd = :evenOdd or s.evenOdd = 'WEEKLY')";

    private static final String UNIQUE_GROUPS_BY_SEMESTER
            = "select distinct g1 from Group g1" +
            " where g1.id in" +
            " (select g.id from Schedule s join s.lesson.group g where s.lesson.semester.id = :semesterId"
            + NOT_DISABLED_SQL + ")";

    private static final String GET_LESSON_BY_DAY_SEMESTER_PERIOD_WEEK
            = "select l1 from Lesson l1" +
            " where l1.id in" +
            " (select l.id " +
            "from Schedule s " +
            "join s.lesson " +
            "l where (s.lesson.semester.id = :semesterId " +
            "and s.dayOfWeek = :dayOfWeek " +
            "and s.period.id = :periodId " +
            "and s.lesson.group.id = :groupId " +
            NOT_DISABLED_SQL + ") " +
            "and (s.evenOdd = :evenOdd or s.evenOdd = 'WEEKLY'))";

    private static final String GET_PERIODS_BY_GROUP_DAY_SEMESTER
            = "select distinct p1 from Period p1" +
            " where p1.id in" +
            " (select p.id " +
            "from Schedule s " +
            "join s.period p " +
            "where s.lesson.semester.id = :semesterId " +
            "and s.lesson.group.id = :groupId " +
            "and s.dayOfWeek = :dayOfWeek" +
            NOT_DISABLED_SQL + ") " +
            "order by p1.startTime";

    private static final String GET_ROOM_FOR_LESSON
            = "select r1 from Room r1" +
            " where r1.id in" +
            " (select r.id " +
            "from Schedule s " +
            "join s.room r " +
            "where (s.lesson.semester.id = :semesterId " +
            "and s.dayOfWeek = :dayOfWeek " +
            "and s.period.id = :periodId " +
            "and s.lesson.id = :lessonId " +
            NOT_DISABLED_SQL + ") " +
            "and (s.evenOdd = :evenOdd or s.evenOdd = 'WEEKLY'))";

    private static final String GET_DAYS_BY_GROUP_SEMESTER
            = "select distinct s.dayOfWeek " +
            "from  Schedule s " +
            "where s.lesson.semester.id = :semesterId " +
            "and s.lesson.group.id = :groupId";

    private static final String GET_DAYS_BY_TEACHER_SEMESTER
            = "select distinct s.dayOfWeek " +
            "from  Schedule s " +
            "where s.lesson.semester.id = :semesterId " +
            "and s.lesson.teacher.id = :teacherId" +
            NOT_DISABLED_SQL;

    private static final String GET_PERIOD_BY_TEACHER_DAY_SEMESTER_WEEK
            = "select distinct p1 " +
            "from Period p1" +
            " where p1.id in" +
            " (select p.id " +
            "from Schedule s " +
            "join s.period p " +
            "where (s.lesson.semester.id = :semesterId " +
            "and s.lesson.teacher.id = :teacherId " +
            "and s.dayOfWeek = :dayOfWeek " +
            NOT_DISABLED_SQL + ") " +
            "and (s.evenOdd = :evenOdd or s.evenOdd = 'WEEKLY')) " +
            "order by p1.startTime";

    private static final String GET_LESSON_BY_TEACHER_SEMESTER_DAY_PERIOD_WEEK
            = "select l1 " +
            "from Lesson l1" +
            " where l1.id in" +
            " (select l.id " +
            "from Schedule s " +
            "join s.lesson l " +
            "where (s.lesson.semester.id = :semesterId " +
            "and s.dayOfWeek = :dayOfWeek " +
            "and s.period.id = :periodId " +
            "and s.lesson.teacher.id = :teacherId " +
            NOT_DISABLED_SQL + ") " +
            "and (s.evenOdd = :evenOdd or s.evenOdd = 'WEEKLY'))";

    private static final String GET_BY_DATE_RANGE_TEACHER
            = "SELECT s " +
            "from Schedule s " +
            "join fetch s.lesson as l " +
            "join fetch l.semester as sem " +
            "join fetch sem.periods " +
            "join fetch  sem.groups " +
            "where s.lesson.semester.startDay <= :toDate " +
            "and s.lesson.semester.endDay >= :fromDate " +
            "and s.lesson.teacher.id = :teacherId";

    private static final String COUNT_BY_LESSON_ID
            = "select count (s.id) " +
            "from  Schedule s " +
            "where s.lesson.id = :lessonId " +
            NOT_DISABLED_SQL;

    private static final String COUNT_BY_LESSON_PERIOD_EVENODD_DAY
            = "select count (s.id) " +
            "from  Schedule s " +
            "where s.lesson.id = :lessonId " +
            "and s.period.id = :periodId " +
            "and s.dayOfWeek =:dayOfWeek " +
            "and (s.evenOdd =:evenOdd or s.evenOdd = 'WEEKLY')" +
            NOT_DISABLED_SQL;

    private static final String GET_BY_ID
            = "select s " +
            "from Schedule s " +
            "left join fetch s.lesson as l " +
            "left join fetch l.semester as sem " +
            "left join fetch sem.periods " +
            "left join fetch sem.groups " +
            "left join fetch sem.daysOfWeek " +
            "where s.id = :id ";

    private static final String GET_ALL
            = "select s " +
            "from Schedule s " +
            "left join fetch s.lesson as l " +
            "left join fetch l.semester as sem " +
            "left join fetch sem.periods " +
            "left join fetch sem.groups " +
            "left join fetch sem.daysOfWeek " +
            "where s.room.disable = false " +
            "and s.lesson.semester.disable = false " +
            "and s.lesson.group.disable = false  " +
            "and s.lesson.subject.disable = false " +
            "and s.lesson.teacher.disable = false ";

    private static final String GET_ORDERED_BY_ROOMS
            = "select s from Schedule s "
            + "join fetch s.lesson as l "
            + "join fetch l.semester as sem "
            + "join fetch sem.periods "
            + "join fetch  sem.groups "
            + "where s.lesson.semester = :semesterId "
            + "order by s.room.sortOrder";

    private static final String GET_BY_SEMESTER_GROUP
            = "select s " +
            "from Schedule s " +
            "where s.lesson.semester = :semesterId " +
            "and s.lesson.group = :groupId";


    @Override
    public Optional<Schedule> findById(Long id) {
        return sessionFactory.getCurrentSession()
                .createQuery(GET_BY_ID, Schedule.class)
                .setParameter("id", id)
                .uniqueResultOptional();
    }

    /**
     * Method searches if there are any saved records in schedule for particular group
     *
     * @param semesterId the semester id that the search is performed for
     * @param dayOfWeek  the day of the week that the search is performed for
     * @param evenOdd    lesson should occur by EVEN/ODD/WEEKLY
     * @param classId    id for period that the search is performed for
     * @param groupId    group id for which the search is performed for
     * @return 0 if there are no records(conflicts), else number of records
     */
    @Override
    public Long conflictForGroupInSchedule(Long semesterId, DayOfWeek dayOfWeek, EvenOdd evenOdd, Long classId, Long groupId) {
        log.info("In isConflictForGroupInSchedule(semesterId = [{}], dayOfWeek = [{}], evenOdd = [{}], classId = [{}], groupId = [{}])", semesterId, dayOfWeek, evenOdd, classId, groupId);
        //if schedule pretends to occur weekly need to check that there are no any already saved schedules for that Group
        if (evenOdd == EvenOdd.WEEKLY) {
            log.debug("Search when lesson repeats weekly");
            return sessionFactory.getCurrentSession()
                    .createQuery(CONFLICT_IN_GROUP_SCHEDULE, Long.class)
                    .setParameter("semesterId", semesterId)
                    .setParameter("dayOfWeek", dayOfWeek)
                    .setParameter("classId", classId)
                    .setParameter("groupId", groupId)
                    .getSingleResult();
        }

        //else schedule pretends to occur by even/odd need to check that here are no amy already saved schedules for that Group at the same half or weekly
        else {
            log.debug("Search when lesson repeats by even/odd");
            return sessionFactory.getCurrentSession().createQuery(CONFLICT_IN_GROUP_SCHEDULE_REPEATS, Long.class)
                    .setParameter("semesterId", semesterId)
                    .setParameter("dayOfWeek", dayOfWeek)
                    .setParameter("classId", classId)
                    .setParameter("groupId", groupId)
                    .setParameter("evenOdd", evenOdd)
                    .getSingleResult();
        }
    }

    /**
     * Method gets the count of records in Schedule table for teacher if teacher has Schedule at some semester at some day(by even odd week) for some period
     *
     * @param semesterId id of semester
     * @param dayOfWeek  day of the week
     * @param evenOdd    even/odd/weekly
     * @param classId    id of period
     * @param teacherId  id of the teacher
     * @return the count of records in Schedule table
     */
    @Override
    public Long conflictForTeacherInSchedule(Long semesterId, DayOfWeek dayOfWeek, EvenOdd evenOdd, Long classId, Long teacherId) {
        log.info("In conflictForTeacherInSchedule(semesterId = [{}], dayOfWeek = [{}], evenOdd = [{}], classId = [{}], teacherId = [{}])", semesterId, dayOfWeek, evenOdd, classId, teacherId);
        if (evenOdd == EvenOdd.WEEKLY) {
            return sessionFactory.getCurrentSession()
                    .createQuery(CONFLICT_TEACHER,Long.class)
                    .setParameter("semesterId", semesterId)
                    .setParameter("dayOfWeek", dayOfWeek)
                    .setParameter("classId", classId)
                    .setParameter("teacherId", teacherId)
                    .getSingleResult();

        } else {
            return sessionFactory.getCurrentSession()
                    .createQuery(CONFLICT_TEACHER_EVEN, Long.class)
                    .setParameter("semesterId", semesterId)
                    .setParameter("dayOfWeek", dayOfWeek)
                    .setParameter("classId", classId)
                    .setParameter("teacherId", teacherId)
                    .setParameter("evenOdd", evenOdd)
                    .getSingleResult();
        }

    }

    /**
     * Method gets the list of unique groups in semester
     *
     * @param semesterId id of semester
     * @return list of unique groups in semester
     */
    @Override
    public List<Group> uniqueGroupsInScheduleBySemester(Long semesterId) {
        log.info("In uniqueGroupsInScheduleBySemester({})", semesterId);
        return sessionFactory.getCurrentSession()
                .createQuery(UNIQUE_GROUPS_BY_SEMESTER, Group.class)
                .setParameter("semesterId", semesterId)
                .getResultList();
    }

    /**
     * Method gets the list of periods/classes for group in some semester at some day
     *
     * @param semesterId id of the semester
     * @param groupId    id of the group
     * @param day        day of the week
     * @return the list of periods/classes
     */
    @Override
    public List<Period> periodsForGroupByDayBySemester(Long semesterId, Long groupId, DayOfWeek day) {
        log.info("In periodsForGroupByDayBySemester(semesterId = [{}], groupId = [{}], day = [{}])", semesterId, groupId, day);
        return sessionFactory.getCurrentSession()
                .createQuery(GET_PERIODS_BY_GROUP_DAY_SEMESTER,Period.class)
                .setParameter("semesterId", semesterId)
                .setParameter("groupId", groupId)
                .setParameter("dayOfWeek", day)
                .getResultList();
    }

    /**
     * Method gets Lesson for group in some semester at some day(even/odd) at some period/class
     *
     * @param semesterId id of the semester
     * @param groupId    id of the group
     * @param periodId   id of the period/class
     * @param day        day of the week
     * @param evenOdd    even/odd/weekly
     * @return Optional Lesson object
     */
    @Override
    public Optional<Lesson> lessonForGroupByDayBySemesterByPeriodByWeek(Long semesterId, Long groupId, Long periodId, DayOfWeek day, EvenOdd evenOdd) {
        log.info("In lessonForGroupByDayBySemesterByPeriodByWeek(semesterId = [{}], groupId = [{}], periodId = [{}], day = [{}], evenOdd = [{}])", semesterId, groupId, periodId, day, evenOdd);
        return sessionFactory.getCurrentSession()
                .createQuery(GET_LESSON_BY_DAY_SEMESTER_PERIOD_WEEK, Lesson.class)
                .setParameter("semesterId", semesterId)
                .setParameter("groupId", groupId)
                .setParameter("periodId", periodId)
                .setParameter("dayOfWeek", day)
                .setParameter("evenOdd", evenOdd)
                .uniqueResultOptional();
    }

    /**
     * Method gets Room for Lesson in some semester at some day(even/odd) at some period/class
     *
     * @param semesterId id of the semester
     * @param periodId   id of the period/class
     * @param lessonId   id of the lesson
     * @param day        day of the week
     * @param evenOdd    even/odd/weekly
     * @return Room object
     */
    @Override
    public Room getRoomForLesson(Long semesterId, Long periodId, Long lessonId, DayOfWeek day, EvenOdd evenOdd) {
        log.info("In getRoomForLesson(semesterId = [{}], periodId = [{}], lessonId = [{}], day = [{}], evenOdd = [{}])", semesterId, periodId, lessonId, day, evenOdd);
        return sessionFactory.getCurrentSession().createQuery(GET_ROOM_FOR_LESSON, Room.class)
                .setParameter("semesterId", semesterId)
                .setParameter("lessonId", lessonId)
                .setParameter("periodId", periodId)
                .setParameter("dayOfWeek", day)
                .setParameter("evenOdd", evenOdd)
                .getSingleResult();
    }

    /**
     * Method gets unique days when Group have classes in semester
     *
     * @param semesterId id of the semester
     * @param groupId    id of the group
     * @return List of days
     */
    @Override
    public List<DayOfWeek> getDaysWhenGroupHasClassesBySemester(Long semesterId, Long groupId) {
        log.info("In getDaysWhenGroupHasClassesBySemester(semesterId = [{}], groupId = [{}])", semesterId, groupId);
        return sessionFactory.getCurrentSession()
                .createQuery(GET_DAYS_BY_GROUP_SEMESTER, DayOfWeek.class)
                .setParameter("semesterId", semesterId)
                .setParameter("groupId", groupId)
                .getResultList();
    }

    /**
     * Method counts schedule records in db for group in the semester
     *
     * @param semesterId id of the semester
     * @param groupId    id og the group
     * @return number of records in db
     */
    @Override
    public Long countSchedulesForGroupInSemester(Long semesterId, Long groupId) {
        log.info("In countSchedulesForGroupInSemester(semesterId = [{}], groupId = [{}])", semesterId, groupId);
        return sessionFactory.getCurrentSession()
                .createQuery(COUNT_BY_GROUP_SEMESTER, Long.class)
                .setParameter("semesterId", semesterId)
                .setParameter("groupId", groupId)
                .getSingleResult();
    }

    /**
     * Method gets unique days when Teacher has classes in semester
     *
     * @param semesterId id of the semester
     * @param teacherId  id of the teacher
     * @return List of days
     */
    @Override
    public List<DayOfWeek> getDaysWhenTeacherHasClassesBySemester(Long semesterId, Long teacherId) {
        log.info("In getDaysWhenTeacherHasClassesBySemester(semesterId = [{}], teacherId = [{}])", semesterId, teacherId);
        return sessionFactory.getCurrentSession()
                .createQuery(GET_DAYS_BY_TEACHER_SEMESTER, DayOfWeek.class)
                .setParameter("semesterId", semesterId)
                .setParameter("teacherId", teacherId)
                .getResultList();
    }

    /**
     * Method gets the list of periods/classes for teacher in some semester at some day
     *
     * @param semesterId id of the semester
     * @param teacherId  id of the teacher
     * @param day        day of the week
     * @return the list of periods/classes
     */
    @Override
    public List<Period> periodsForTeacherBySemesterByDayByWeek(Long semesterId, Long teacherId, DayOfWeek day, EvenOdd evenOdd) {
        log.info("In periodsForTeacherBySemesterByDayByWeek(semesterId = [{}], teacherId = [{}], day = [{}], evenOdd = [{}])", semesterId, teacherId, day, evenOdd);
        return sessionFactory.getCurrentSession()
                .createQuery(GET_PERIOD_BY_TEACHER_DAY_SEMESTER_WEEK, Period.class)
                .setParameter("semesterId", semesterId)
                .setParameter("teacherId", teacherId)
                .setParameter("dayOfWeek", day)
                .setParameter("evenOdd", evenOdd)
                .getResultList();
    }

    /**
     * Method gets Lesson for teacher in some semester at some day(even/odd) at some period/class
     *
     * @param semesterId id of the semester
     * @param teacherId  id of the group
     * @param periodId   id of the period/class
     * @param day        day of the week
     * @param evenOdd    even/odd/weekly
     * @return Optional Lesson object
     */
    @Override
    public List<Lesson> lessonsForTeacherBySemesterByDayByPeriodByWeek(Long semesterId, Long teacherId, Long periodId, DayOfWeek day, EvenOdd evenOdd) {
        log.info("In lessonsForTeacherBySemesterByDayByPeriodByWeek(semesterId = [{}], teacherId = [{}], periodId = [{}], day = [{}], evenOdd = [{}])", semesterId, teacherId, periodId, day, evenOdd);
        return sessionFactory.getCurrentSession()
                .createQuery(GET_LESSON_BY_TEACHER_SEMESTER_DAY_PERIOD_WEEK, Lesson.class)
                .setParameter("semesterId", semesterId)
                .setParameter("teacherId", teacherId)
                .setParameter("periodId", periodId)
                .setParameter("dayOfWeek", day)
                .setParameter("evenOdd", evenOdd)
                .getResultList();
    }

    @Override
    public List<Schedule> getAllSchedulesByTeacherIdAndSemesterId(Long teacherId, Long semesterId) {
        log.info("Enter into getAll of TeacherRepositoryImpl");
        return sessionFactory.getCurrentSession()
                .createQuery(GET_ALL_BY_TEACHER_SEMESTER, Schedule.class)
                .setParameter("semesterId", semesterId)
                .setParameter("teacherId", teacherId)
                .getResultList();
    }

    /**
     * Method gets schedule by schedule all schedule parameters
     *
     * @param schedule schedule with parameters
     * @return List of all schedules
     */
    @Override
    public Schedule getScheduleByObject(Schedule schedule) {
        log.info("Enter into getScheduleByObject");
        return sessionFactory.getCurrentSession()
                .createQuery(GET_BY_ALL_PARAMETERS, Schedule.class)
                .setParameter("periodId", schedule.getPeriod().getId())
                .setParameter("lessonId", schedule.getLesson().getId())
                .setParameter("dayOfWeek", schedule.getDayOfWeek())
                .setParameter("evenOdd", schedule.getEvenOdd())
                .setParameter("roomId", schedule.getRoom().getId())
                .getSingleResult();
    }

    /**
     * Method geets all schedules from db in particular semester
     *
     * @param semesterId id of the semester
     * @return list of schedules
     */
    @Override
    public List<Schedule> getScheduleBySemester(Long semesterId) {
        log.info("In getScheduleBySemester(semesterId = [{}])", semesterId);

        return sessionFactory.getCurrentSession()
                .createQuery(GET_BY_SEMESTER, Schedule.class)
                .setParameter("semesterId", semesterId)
                .getResultList();

    }

    /**
     * Method gets information about all schedules from DB
     *
     * @return List of all schedules
     */
    @Override
    public List<Schedule> getAll() {
        log.debug("In getAll()");
        return sessionFactory.getCurrentSession()
                .createQuery(GET_ALL, Schedule.class)
                .getResultList();
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
    public List<Schedule> scheduleByDateRangeForTeacher(LocalDate fromDate, LocalDate toDate, Long teacherId) {
        log.info("In scheduleByDateRangeForTeacher with fromDate = {} and toDate = {}", fromDate, toDate);
        return sessionFactory.getCurrentSession()
                .createQuery(GET_BY_DATE_RANGE_TEACHER, Schedule.class)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .setParameter("teacherId", teacherId)
                .getResultList();
    }

    /**
     * Method scheduleForRoomBySemester get all schedules for specific  room and  semester
     *
     * @param semesterId
     * @param roomId
     * @return list of schedules
     */
    @Override
    public List<Schedule> scheduleForRoomBySemester(Long semesterId, Long roomId) {
        log.info("In scheduleForRoomBySemester with semesterId = {} and roomId = {}", semesterId, roomId);
        return sessionFactory.getCurrentSession()
                .createQuery(GET_BY_ROOM_SEMESTER, Schedule.class)
                .setParameter("roomId", roomId)
                .setParameter("semesterId", semesterId)
                .getResultList();
    }


    /**
     * Method deleteSchedulesBySemesterId delete all schedules from db in with current semesterId
     *
     * @param semesterId id Semester for delete schedule
     */
    @Override
    public void deleteSchedulesBySemesterId(Long semesterId) {
        log.info("In deleteSchedulesBySemesterId with semesterId = {}", semesterId);
        sessionFactory.getCurrentSession()
                .createQuery(DELETE_BY_SEMESTER_ID)
                .setParameter("semesterId", semesterId)
                .executeUpdate();
    }

    /**
     * Method counts schedule records in db for lesson by lessonsId
     *
     * @param lessonId id of the lesson
     * @return count of records in db
     */
    @Override
    public Long countInputLessonsInScheduleByLessonId(Long lessonId) {
        log.info("In countInputLessonsInScheduleByLessonId(lessonId = [{}])", lessonId);
        return sessionFactory.getCurrentSession()
                .createQuery(COUNT_BY_LESSON_ID, Long.class)
                .setParameter("lessonId", lessonId)
                .getSingleResult();
    }

    /**
     * Method counts schedule records in db for lesson by lessonsId, periodId, EvenOdd and DayOfWeek
     *
     * @param lessonId id of the lesson
     * @param periodId id of the Period
     * @param evenOdd  Even/Odd
     * @param day      day of Week
     * @return count of records in db
     */
    @Override
    public Long countByLessonIdPeriodIdEvenOddDayOfWeek(Long lessonId, Long periodId, EvenOdd evenOdd, DayOfWeek day) {
        log.info("In countByLessonIdPeriodIdEvenOddDayOfWeek(lessonId = [{}], periodId = [{}], evenOdd = [{}], day = [{}])", lessonId, periodId, evenOdd, day);
        return sessionFactory.getCurrentSession()
                .createQuery(COUNT_BY_LESSON_PERIOD_EVENODD_DAY, Long.class)
                .setParameter("lessonId", lessonId)
                .setParameter("periodId", periodId)
                .setParameter("dayOfWeek", day)
                .setParameter("evenOdd", evenOdd)
                .getSingleResult();
    }

    @Override
    public List<Schedule> getSchedulesBySemesterTeacher(Long semesterId, Long teacherId) {
        log.info("Entered getSchedulesBySemesterTeacher({}, {})", semesterId, teacherId);
        return sessionFactory.getCurrentSession()
                .createQuery(GET_BY_SEMESTER_TEACHER, Schedule.class)
                .setParameter("semesterId", semesterId)
                .setParameter("teacherId", teacherId)
                .getResultList();
    }

    @Override
    public List<Schedule> getSchedulesOrderedByRooms(Long semesterId) {
        return sessionFactory.getCurrentSession()
                .createQuery(GET_ORDERED_BY_ROOMS, Schedule.class)
                .setParameter("semesterId", semesterId)
                .getResultList();
    }

    @Override
    public List<Schedule> getSchedulesBySemesterGroup(Long semesterId, Long groupId){
        log.info("Entered getScheulesBySemesterGroup({},{})", semesterId, groupId);
        return sessionFactory.getCurrentSession()
                .createQuery(GET_BY_SEMESTER_GROUP, Schedule.class)
                .setParameter("semesterId", semesterId)
                .setParameter("groupId", groupId)
                .getResultList();
    }
}
