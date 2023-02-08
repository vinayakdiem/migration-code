package com.diemlife.services;

import com.diemlife.dto.UserSearchDTO;
import com.diemlife.models.User;
import play.Logger;
import play.db.jpa.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;

import org.springframework.stereotype.Service;

import java.util.List;

import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Created by andrewcoleman on 3/3/16.
 */

@Service
public class UserService {

	public static User getById(final Integer id) {
		//FIXME vinayak
//        return em.find(User.class, id);
    }

    public static User getByEmail(final String email) {
        if (isBlank(email)) {
            return null;
        }
        try {
        	//FIXME vinayak
//            return em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
//                    .setParameter("email", email)
//                    .getSingleResult();
        } catch (final NoResultException e) {
            Logger.warn("UserService :: getByEmail : User not found for email => " + email);
            return null;
        } catch (final PersistenceException e) {
            Logger.error("UserService :: getByEmail : Error finding user by email => " + email, e);
            throw e;
        }
    }

    public static boolean isFreeEmail(final String email) {
        try {
        	//FIXME vinayak
//            return em.createQuery("SELECT DISTINCT u.id FROM User u WHERE u.email = :email", Integer.class)
//                    .setParameter("email", email)
//                    .getResultList()
//                    .isEmpty();
        } catch (final PersistenceException e) {
            Logger.error("UserService :: isFreeEmail : Error checking free email => " + email, e);
            throw e;
        }
    }

    public static boolean doesUsernameExist(final String username) {
        try {
        	//FIXME vinayak
//            return em.createQuery("SELECT u.userName FROM User u WHERE :username = u.userName")
//                    .setParameter("username", username)
//                    .getResultList()
//                    .size() >= 1;
        } catch (final PersistenceException e) {
            Logger.error("UserService :: doesUsernameExist : Error checking existing username => " + username, e);
            throw e;
        }
    }

    public static Integer getByUsername(final String username) {
        try {
        	//FIXME vinayak
//            return em.createQuery("SELECT u.id FROM User u WHERE u.userName = :username", Integer.class)
//                    .setParameter("username", username)
//                    .getSingleResult();
        } catch (final NoResultException nre) {
            Logger.warn("UserService :: getByUsername : cannot get user id by username => " + username);
            return null;
        } catch (final PersistenceException e) {
            Logger.error("UserService :: getByUsername : failed getting user id by username => " + username, e);
            throw e;
        }
    }

    public static List<User> getUsers() {
        try {
        	//FIXME vinayak
//            return em.createQuery("SELECT u FROM User u", User.class).getResultList();
        } catch (final PersistenceException e) {
            Logger.error("UserService :: getUsers : error getting all users", e);
            throw e;
        }
    }

    public static List<UserSearchDTO> getAllUsersForSearch() {
        try {
        	//FIXME vinayak
//            return em.createQuery("SELECT NEW dto.UserSearchDTO("
//                    + " u.id,"
//                    + " u.firstName,"
//                    + " u.lastName,"
//                    + " u.userName,"
//                    + " u.missionStatement,"
//                    + " u.profilePictureURL"
//                    + ") FROM User u", UserSearchDTO.class)
//                    .setHint("org.hibernate.readOnly", true)
//                    .getResultList();
        } catch (final PersistenceException e) {
            Logger.error("UserService :: getAllUsersForSearch : error getting all users", e);
            throw e;
        }
    }

}
