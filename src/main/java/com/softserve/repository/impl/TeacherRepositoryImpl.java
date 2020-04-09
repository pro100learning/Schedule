package com.softserve.repository.impl;

import com.softserve.entity.Teacher;
import com.softserve.repository.TeacherRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
@Slf4j
@SuppressWarnings("unchecked")
public class TeacherRepositoryImpl extends BasicRepositoryImpl<Teacher, Long> implements TeacherRepository {
    /**
     * The method used for getting list of teachers entities from database
     *
     * @return list of entities ordered by surname
     */
    @Override
    public List<Teacher> getAll() {
        log.info("Enter into getAll of TeacherRepositoryImpl");
        return sessionFactory.getCurrentSession().
                createQuery("select t from " + basicClass.getName() + " t" +
                        " order by t.surname ASC").getResultList();
    }

    // Checking if teacher is used in Lesson and TeacherWishes tables
    @Override
    public boolean checkReference(Teacher teacher) {
        log.info("In checkReference(teacher = [{}])", teacher);
        long count = (long) sessionFactory.getCurrentSession().createQuery
                ("select count (l.id) " +
                        "from Lesson l where l.teacher.id = :teacherId")
                .setParameter("teacherId", teacher.getId()).getSingleResult()
                + (long) sessionFactory.getCurrentSession().createQuery
                ("select count (tw.id) " +
                        "from TeacherWishes tw where tw.teacher.id = :teacherId")
                .setParameter("teacherId", teacher.getId()).getSingleResult();

        return count != 0;
    }
}
