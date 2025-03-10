package com.softserve.repository.impl;

import com.softserve.entity.User;
import com.softserve.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class UserRepositoryImpl extends BasicRepositoryImpl<User, Long> implements UserRepository {

    /**
     * The method used for getting User by email from database
     *
     * @param email String email used to find User by it
     * @return User
     */
    @Override
    public Optional<User> findByEmail(String email) {
        log.info("Enter into findByEmail method with email:{}", email);
        TypedQuery<User> query = sessionFactory.getCurrentSession().createNamedQuery("findEmail", User.class).setMaxResults(1);
        query.setParameter("email", email);
        List<User> user = query.getResultList();
        if (user.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(query.getResultList().get(0));
    }

    /**
     * The method used for getting User by token from database
     *
     * @param token String token used to find User by it
     * @return User
     */
    @Override
    public Optional<User> findByToken(String token) {
        log.info("Enter into findByToken  with token:{}", token);
        TypedQuery<User> query = sessionFactory.getCurrentSession().createNamedQuery("findToken", User.class).setMaxResults(1);
        query.setParameter("token", token);
        List<User> user = query.getResultList();
        if (user.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(query.getResultList().get(0));
    }

    /**
     * The method used for getting list of users from database, that have role USER in system
     *
     * @return list of entities Teacher
     */
    @Override
    public List<User> getAllUsersWithRoleUser() {
        log.info("Enter into getAllUsersWithRoleUser of UserRepositoryImpl");
        return sessionFactory.getCurrentSession().createQuery(
                "select u from User u " +
                        " where u.role = 'ROLE_USER' ", User.class)
                .getResultList();
    }


    /**
     * Modified update method, which merge entity before updating it
     *
     * @param entity user is going to be updated
     * @return User
     */
    @Override
    public User update(User entity) {
        log.info("Enter into update method with entity:{}", entity);
        entity = (User) sessionFactory.getCurrentSession().merge(entity);
        sessionFactory.getCurrentSession().update(entity);
        return entity;
    }

    // Checking if user is used in Teacher table
    @Override
    protected boolean checkReference(User user) {
        log.info("In checkReference(user = [{}])", user);
        Long count = (Long) sessionFactory.getCurrentSession().createQuery
                ("select count (t.id) " +
                        "from Teacher t where t.userId = :userId")
                .setParameter("userId", user.getId())
                .getSingleResult();
        return count != 0;
    }

}
