
package com.diemlife.dao;

// Generated Jul 4, 2015 5:57:00 PM by Hibernate Tools 4.3.1

//FIXME Vinayak
//import com.feth.play.module.pa.providers.password.UsernamePasswordAuthUser;
//import com.feth.play.module.pa.user.AuthUser;
//import com.feth.play.module.pa.user.AuthUserIdentity;
//import com.feth.play.module.pa.user.EmailIdentity;
//import com.feth.play.module.pa.user.FirstLastNameIdentity;
//import com.feth.play.module.pa.user.NameIdentity;
import com.diemlife.constants.Util;
import com.diemlife.dto.QuestMemberDTO;
import com.diemlife.dto.UserToInviteDTO;
import com.diemlife.forms.ParticipantInfoForm;
import com.diemlife.forms.RegistrationForm;
import com.diemlife.models.*;
import play.Logger;
import com.diemlife.providers.MyUsernamePasswordAuthUser;
import com.diemlife.security.UsernamePasswordAuth;
import com.diemlife.services.UserService;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.*;
import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * Home object for domain model class User.
 *
 * @author Hibernate Tools
 * @see models.User
 */
@Repository
public class UserHome {

	@PersistenceContext
	private EntityManager entityManager;
	
    public void persist(User transientInstance) {

        try {
            entityManager.persist(transientInstance);
        } catch (RuntimeException e) {
            throw e; // or display error message
        }
    }

    public void remove(User persistentInstance) {

        try {
            entityManager.remove(persistentInstance);
        } catch (RuntimeException e) {
            throw e; // or display error message
        }
    }

    public User merge(User detachedInstance) {

        try {

            User result = entityManager.merge(detachedInstance);

            return result;
        } catch (RuntimeException e) {
            throw e; // or display error message
        }
    }

    public User findById(final Integer id) {
        if (id == null) {
            return null;
        }
        return entityManager.find(User.class, id);
    }

    public User2 getUserByUsername(Connection c, String username) {
        try (PreparedStatement ps = c.prepareStatement("select first_name, last_name from user where user_name = ?")) {
			ps.setString(1, username);
			
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
                    String firstname = rs.getString(1);
                    String lastname = rs.getString(2);

                    // only expect 1 result (or none)
                  //FIXME Vinayak
//					return new User2(username, firstname, lastname);
				}
			}
		} catch (Exception e) {
			Logger.error("getUserByUsername - error", e);
		}

		return null;
    }

    public User findByIdReference(Integer id) {
        try {
            return entityManager.getReference(User.class, id);
        } catch (NoResultException e) {
            Logger.info("user not found by reference");
            return null;
        } catch (Exception re) {
            Logger.error("error: ", re);
            throw re;
        }
    }

    public LinkedAccount getAccountByProvider(User user, String providerKey) {
        LinkedAccountHome dao = new LinkedAccountHome();

        return dao.findByProviderKey(user, providerKey, entityManager);
    }

    public void changePassword(User user, MyUsernamePasswordAuthUser authUser) {
    	//FIXME Vinayak
//        LinkedAccount a = this.getAccountByProvider(user, authUser.getProvider(), entityManager);

//        LinkedAccountHome dao = new LinkedAccountHome();
//
//        if (a == null) {
//            a = dao.create(user, authUser.getProvider(), authUser.getId(), entityManager);
//            a.setUser(user);
//        }
//
//        a.setProviderUserId(authUser.getHashedPassword());
//        a.getUser().setEmailValidated(true);
//
//        dao.merge(a, entityManager);
    }

    public void resetPassword(User user, MyUsernamePasswordAuthUser authUser) {
        // You might want to wrap this into a transaction
        this.changePassword(user, authUser);

        TokenActionHome tokenDao = new TokenActionHome();

        tokenDao.deleteByUser(user, "PASSWORD_RESET", entityManager);
    }

  //FIXME Vinayak
//    public boolean existsByAuthUserIdentity(AuthUserIdentity identity) {
//        User exp = null;
      //FIXME Vinayak
//        if (identity instanceof UsernamePasswordAuthUser) {
//            exp = findByUsernamePasswordIdentity(((UsernamePasswordAuthUser) identity).getEmail(), UsernamePasswordAuth.PROVIDER_KEY, entityManager);
//        } else {
//            exp = getAuthUserFind(identity, entityManager);
//        }

//        return exp != null;
        //return exp.findRowCount() > 0;
//    }

    public User findByEmail(String email) {
        if (isBlank(email)) {
            return null;
        }
        try {
            Query query = entityManager.createQuery("SELECT DISTINCT u FROM User u WHERE lower(u.email) = :email AND u.active = true");
            query.setParameter("email", email.toLowerCase());

            return (User) query.getSingleResult();
        } catch (NoResultException e) {
            Logger.info("user not found");
            return null;
        } catch (Exception re) {
            Logger.error("error: ", re);
            throw re;
        }
    }

    public User findByName(String username) {
        return entityManager.createQuery("SELECT DISTINCT u FROM User u " +
                " WHERE lower(u.userName) = :username AND u.active = true ", User.class)
                .setParameter("username", username)
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public User findByUsernamePasswordIdentity(final String email, final String provider) {
        try {
            return entityManager.createQuery("SELECT DISTINCT u FROM LinkedAccount l " +
                    "JOIN l.user u " +
                    "LEFT JOIN FETCH u.securityRoles " +
                    "LEFT JOIN FETCH u.userPermissions " +
                    "WHERE LOWER(l.providerKey) = :pKey AND LOWER(u.email) = :email AND u.active = true", User.class)
                    .setParameter("pKey", provider)
                    .setParameter("email", email)
                    .getSingleResult();
        } catch (final NoResultException e) {
            Logger.info("UserHome.findByUsernamePasswordIdentity :: User not found with email " + email);

            return null;
        } catch (final PersistenceException e) {
            Logger.error(e.getMessage(), e);

            throw e;
        }
    }

    private User getAuthUserFind(
    		//FIXME Vinayak
//    		AuthUserIdentity identity
    		) {

        try {
            Query query = entityManager.createQuery("SELECT DISTINCT u FROM LinkedAccount l JOIN l.user u LEFT JOIN FETCH u.securityRoles LEFT JOIN FETCH u.userPermissions WHERE lower(l.providerKey) = :pKey AND lower(l.providerUserId) = :userId AND u.active = true");
//            query.setParameter("pKey", identity.getProvider().toLowerCase());
//            query.setParameter("userId", identity.getId().toLowerCase());

            User user = (User) query.getSingleResult();

            return user;
        } catch (NoResultException e) {
            Logger.info("user not found");
            return null;
        } catch (Exception re) {
            Logger.error("error: ", re);
            throw re;
        }
    }
  //FIXME Vinayak
//    public User findByAuthUserIdentity(AuthUserIdentity identity) {
//        if (identity == null) {
//            return null;
//        }
//        if (identity instanceof UsernamePasswordAuthUser) {
//            return findByUsernamePasswordIdentity(((UsernamePasswordAuthUser) identity).getEmail(), UsernamePasswordAuth.PROVIDER_KEY,  entityManager);
//        } else {
//            return getAuthUserFind(identity, entityManager);
//        }
//    }

//    public User create(AuthUser authUser) {
//        return this.createByRole(authUser, controllers.Application.USER_ROLE, null, entityManager);
//    }

  //FIXME Vinayak
//    public User createByRole(AuthUser authUser, String roleName, String tiUserId) {
//        User user = new User();
//        
//        if (authUser instanceof MyUsernamePasswordAuthUser) {
//        	MyUsernamePasswordAuthUser myUsernamePasswordAuthUser = (MyUsernamePasswordAuthUser) authUser;
//            if(myUsernamePasswordAuthUser.getUserId()!=null) {
//            	user.setId(myUsernamePasswordAuthUser.getUserId());
//        	}
//        }
//        
//        SecurityRoleHome roleDao = new SecurityRoleHome();
//
//        SecurityRole role = roleDao.findByRoleName(roleName, entityManager);
//
//        Set<SecurityRole> roles = user.getSecurityRoles();
//        roles.add(role);
//
//        UserProfileHome userProfileDao = new UserProfileHome();
//
//        UserProfile userProfile = new UserProfile();
//        userProfile.setTiUserId(tiUserId);
//
//        userProfile = userProfileDao.merge(userProfile, entityManager);
//
//        user.setSecurityRoles(roles);
//        user.setUserProfile(userProfile);
//
//        user.setActive(true);
//        user.setLastLogin(new Date());
//        user.setCreatedOn(new Date());
//        user.setUpdatedOn(new Date());
//        user.setIsUserBrand("N");
//
//        if (authUser instanceof EmailIdentity) {
//            EmailIdentity identity = (EmailIdentity) authUser;
//            // Remember, even when getting them from FB & Co., emails should be
//            // verified within the application as a security breach there might
//            // break your security as well!
//            user.setEmail(identity.getEmail());
//            user.setEmailValidated(false);
//        }
//
//        if (authUser instanceof NameIdentity) {
//            NameIdentity identity = (NameIdentity) authUser;
//            String name = identity.getName();
//            if (name != null) {
//                user.setName(name);
//            }
//        }
//
//        if (authUser instanceof FirstLastNameIdentity) {
//            FirstLastNameIdentity identity = (FirstLastNameIdentity) authUser;
//            String firstName = identity.getFirstName();
//            String lastName = identity.getLastName();
//            if (firstName != null) {
//                user.setFirstName(firstName);
//            }
//            if (lastName != null) {
//                user.setLastName(lastName);
//            }
//        }
//
//        user = this.merge(user, entityManager);
//
//        LinkedAccountHome accountsDao = new LinkedAccountHome();
//
//        accountsDao.create(user, "password", authUser.getId(), entityManager);
//
//        return user;
//    }

    public User addNonAuthInfoToUser(User user, RegistrationForm input) {

    	//FIXME Vinayak
//        user.setFirstName(input.getFirstName());
//        user.setLastName(input.getLastName());

        //by default set the username to whatever trails the @
//        int emailIndex = input.getEmail().indexOf("@");

//        String proposedUserName = input.getEmail()
//                .substring(0, emailIndex)
//                .replaceAll("[^A-Za-z0-9]", "");

//        Integer subStrLength = proposedUserName.length() > 21 ? 21 : proposedUserName.length();
//        final String finalUserName = proposedUserName.substring(0, subStrLength);
//
//        if (!UserService.doesUsernameExist(finalUserName, em)) {
//            user.setUserName(finalUserName);
//        } else {
//            Random random = new Random();
//            user.setUserName(finalUserName + random.nextInt(1000));
//        }
//
//        if (input.getGoals() != null) {
//            user.setMissionStatement(input.getGoals());
//        }
//
//        if (input.getCountry() != null) {
//            user.setCountry(input.getCountry());
//        }
//
//        if (input.getZip() != null) {
//            user.setZip(input.getZip());
//        }
//
//        if (input.getReceiveEmail() != null) {
//            user.setReceiveEmail(input.getReceiveEmail());
//        } else {
//            user.setReceiveEmail("false");
//        }

        //ToDO: New: Check if personal info will be populated during registration
        //populatePersonalInfo(user,input.getParticipantInfo());

        user = this.merge(user);

        return user;
    }

    public void populatePersonalInfo(User user, ParticipantInfoForm participantPersonalInfo){
        PersonalInfo personalInfo = new PersonalInfo();
        personalInfo.firstName = participantPersonalInfo.getFirstName();
        personalInfo.lastName = participantPersonalInfo.getFirstName();
        personalInfo.email = participantPersonalInfo.getEmail();
        personalInfo.homePhone = participantPersonalInfo.getHomePhone();
        personalInfo.cellPhone = participantPersonalInfo.getCellPhone();
        personalInfo.birthDate = participantPersonalInfo.getBirthDate();
        personalInfo.age = isNumeric(participantPersonalInfo.getAge()) ? parseInt(participantPersonalInfo.getAge()) : null;
        personalInfo.gender = participantPersonalInfo.getGender();
        personalInfo.shirtSize = participantPersonalInfo.getShirtSize();
        personalInfo.burgerTemp = participantPersonalInfo.getBurgerTemp();
        personalInfo.withCheese = participantPersonalInfo.getWithCheese();
        personalInfo.specialRequests = participantPersonalInfo.getSpecialRequests();
        user.setPersonalInfo(personalInfo);
    }

    public void addUserFavoritesToUser(User user, String favorite) {
        UserFavorites userFavorites = new UserFavorites();
        Date date = new Date();
        if (user != null) {
            userFavorites.setUserId(user.getId());
            userFavorites.setFavorite(favorite);
            userFavorites.setDateCreated(date);
            userFavorites.setDateModified(date);

            entityManager.persist(userFavorites);
        }
    }

    public void updateUserFavorites(User user, String favorite) {
        final Date now = new Date();
        if (user != null && favorite != null) {
            final TypedQuery<String> query = entityManager.createQuery("SELECT uf.favorite FROM UserFavorites uf WHERE uf.userId = :userId",
                    String.class);
            query.setParameter("userId", user.getId());
            final List<String> userFavorites = query.getResultList();
            if (userFavorites.contains(favorite)) {
                Logger.info(format("Skipping '%s' favorite update of user [%s]", favorite, user.getEmail()));
            } else {
                Logger.info(format("Updating '%s' favorite of user [%s]", favorite, user.getEmail()));
                final UserFavorites newUserFavorite = new UserFavorites();
                newUserFavorite.setUserId(user.getId());
                newUserFavorite.setFavorite(favorite);
                newUserFavorite.setDateCreated(now);
                newUserFavorite.setDateModified(now);

                entityManager.persist(newUserFavorite);
            }
        }
    }

    public void removeUserFavoritesForUser(User user, String favorite) {
        if (user != null && favorite != null) {
            final TypedQuery<UserFavorites> query = entityManager.createQuery("SELECT uf FROM UserFavorites uf WHERE uf.userId = :userId",
                    UserFavorites.class);
            query.setParameter("userId", user.getId());
            final List<UserFavorites> userFavorites = query.getResultList();
            for (final UserFavorites userFavorite : userFavorites) {
                if (equalsIgnoreCase(userFavorite.getFavorite(), favorite)) {
                    Logger.info(format("Removing '%s' favorite of user [%s]", favorite, user.getEmail()));
                    entityManager.remove(userFavorite);
                }
            }
        }
    }

    public List<UserFavorites> getUserFavoritesByUserId(Integer userId) {
        if (userId != null) {
            try {
                Query query = entityManager.createQuery("SELECT uf from UserFavorites uf WHERE uf.userId = :userId");
                query.setParameter("userId", userId);
                return (List<UserFavorites>) query.getResultList();
            } catch (NoResultException nre) {
                return null;
            } catch (Exception ex) {
                Logger.error("UserHome :: getUserFavoritesByUserId : error finding user favorites => " + ex, ex);
                return null;
            }
        }
        return null;
    }

    public User verify(User unverified) {
        // You might want to wrap this into a transaction
        unverified.setEmailValidated(true);
        final User verified = this.merge(unverified);

        TokenActionHome tokenDao = new TokenActionHome();

        tokenDao.deleteByUser(unverified, "EMAIL_VERIFICATION", entityManager);

        return verified;
    }

    public void merge(User currentUser, User otherUser) {

        Set<LinkedAccount> currentUserAccounts = currentUser.getLinkedAccounts();

        LinkedAccountHome linkedAccountDao = new LinkedAccountHome();

        for (LinkedAccount acc : otherUser.getLinkedAccounts()) {
            currentUserAccounts.add(linkedAccountDao.create(currentUser, acc.getProviderKey(), acc.getProviderUserId(), entityManager));
        }

        // do all other merging stuff here - like resources, etc.
        currentUser.setLinkedAccounts(currentUserAccounts);

        Set<SecurityRole> roles = otherUser.getSecurityRoles();

        for (SecurityRole role : roles) {
            currentUser.getSecurityRoles().add(role);
        }

        // deactivate the merged user that got added to this one
        otherUser.setActive(false);

        this.merge(otherUser);
        this.merge(currentUser);
    }

  //FIXME Vinayak
//    public void merge(AuthUser oldUser, AuthUser newUser) {
//
//        User oldUserDb = this.findByAuthUserIdentity(oldUser);
//        User newUserDb = this.findByAuthUserIdentity(newUser);
//
//        this.merge(oldUserDb, newUserDb);
//    }

    public List<User> getAllUsersNotCurrentUser(int userId) {
        try {
            Logger.info("UserHome.getAllUsers() :: getting all users.");
            return entityManager.createQuery("SELECT u from User u where u.id <> :userId", User.class)
                    .setParameter("userId", userId)
                    .getResultList();
        } catch (final PersistenceException e) {
            Logger.error("UserHome.getAllUsers() :: unable to find users for search");
            return emptyList();
        }
    }

    public List<UserToInviteDTO> getAllUsersToInviteDTOs(final int currentUserId) {
        try {
            Logger.info("UserHome.getAllUsersToInviteDTOs() :: getting all users to invite by " + currentUserId);
            return entityManager.createQuery("SELECT NEW dto.UserToInviteDTO(u.email, u.name) from User u where u.id <> :userId", UserToInviteDTO.class)
                    .setParameter("userId", currentUserId)
                    .getResultList();
        } catch (final PersistenceException e) {
            Logger.error("UserHome.getAllUsersToInviteDTOs() :: unable to find users for search");
            return emptyList();
        }
    }

    public List<User> getUsersNotCurrentlyFriendsWithByUserId(Integer userId, String userName) {
        EntityTransaction tx = null;
        try {
        	//FIXME Vinayak
//            List<Integer> currentFriendIds = UserRelationshipDAO.getCurrentFriendsByUserId(userId);
//            Logger.info("UserHome.getUsersNotCurrentlyFriendsWithByUserId() :: got current friends");
//            if (!currentFriendIds.isEmpty()) {
//                tx = entityManager.getTransaction();
//                tx.begin();
//                currentFriendIds.add(userId);
//                Logger.info("CurrentFriends => " + Arrays.toString(currentFriendIds.toArray()));
//                Query query = entityManager.createQuery("SELECT u.id, u.name, u.email FROM User u where u.id NOT IN (:currentFriends) and u.name = :userName or u.email = :userName");
//                query.setParameter("currentFriends", currentFriendIds);
//                query.setParameter("userName", userName);
//                List<User> users = (List<User>) query.getResultList();
//                tx.commit();
//                return users;
//            } else {
                return getAllUsersNotCurrentUser(userId);
//            }
        } catch (Exception e) {
            tx.rollback();
            Logger.error("UserHome.getAllUsers() :: unable to find users for search");
            return null;
        }
    }

    public List<User> getUsersNotFriendsByUserId(Integer userId) {
        try {
			//FIXME Vinayak
//            Set<Integer> friends = new HashSet<>(UserRelationshipDAO.getCurrentFriendsByUserId(userId));
//            friends.addAll(UserRelationshipDAO.getPendingFriendsByUserId(userId));

//            if (!friends.isEmpty()) {
//                friends.add(userId);
//                Query query = entityManager.createQuery("SELECT u FROM User u where u.id NOT IN (:friends) and u.id != :userId");
//                query.setParameter("friends", friends);
//                query.setParameter("userId", userId);
//                return query.getResultList();
//            } else {
                return getAllUsersNotCurrentUser(userId);
//            }
        } catch (PersistenceException e) {
            Logger.error("UserHome.getUsersNotFriendsByUserId() :: unable to find users for search => " + e, e);
            return emptyList();
        }
    }

    public List<User> getUsersByIds(List<Integer> userIds) {
        if (Util.isEmpty(userIds)) {
            return emptyList();
        }
        try {
            return entityManager.createQuery("SELECT u FROM User u WHERE u.id IN (:userIds)", User.class)
                    .setParameter("userIds", userIds)
                    .getResultList();
        } catch (final PersistenceException ex) {
            Logger.error("UserHome :: getUsersByIds :: ERROR => " + ex, ex);
            throw new PersistenceException(ex);
        }
    }

    public List<Integer> getUsersByIdWithBrand(List<Integer> userIds) {
        if (userIds != null && !userIds.isEmpty()) {
            try {
                userIds.size();
                Query query = entityManager.createQuery("SELECT u.id FROM User u WHERE u.id IN (:userIds)");
                query.setParameter("userIds", userIds);
                List<Integer> users = (List<Integer>) query.getResultList();
                userIds.addAll(users);
                // add brands into the mix to show in feed always.
                return addBrandsToFriendsList(userIds);
            } catch (PersistenceException ex) {
                throw new PersistenceException(ex);
            }
        }
        return emptyList();
    }

    public void updateUserProfilePicture(Integer userId, String s3FileURL) {
    	//FIXME Vinayak
//        User user = UserHome.findById(userId);
//        if (user != null) {
//            Logger.info("UserHome :: updateUserProfilePicture :: for user => " + user.getId());
//            try {
//                // S3 does not handle spaces, lets change to a friendly link
//                user.setProfilePictureURL(s3FileURL.replace("+", "%2B"));
//                entityManager.merge(user);
//                Logger.info("user.getProfPicURL = " + user.getProfilePictureURL());
//            } catch (Exception ex) {
//                Logger.error("UserHome :: updateUserProfilePicure => " + ex, ex);
//            }
//        }
    }

    public void uploadUserCoverPicture(Integer userId, String s3FileURL) {
    	//FIXME Vinayak
//        User user = UserHome.findById(userId);
//        if (user != null) {
//            // S3 does not handle spaces, lets change to a friendly link
//            Logger.info("UserHome :: uploadUserCoverPicture :: for user => " + user.getId());
//            try {
//                user.setCoverPictureURL(s3FileURL.replace("+", "%2B"));
//                entityManager.merge(user);
//                Logger.info("user.getCoverURL = " + user.getCoverPictureURL());
//            } catch (Exception ex) {
//                Logger.error("UserHome :: updateUserProfilePicure => " + ex, ex);
//            }
//        }
    }

    public String getUserProfilePictureByUserId(Integer userId) {
        if (userId != null) {
            try {
                Query query = entityManager.createQuery("SELECT u.profilePictureURL FROM User u WHERE u.id = :userId");
                query.setParameter("userId", userId);
                return (String) query.getSingleResult();
            } catch (NoResultException nre) {
                return null;
            } catch (Exception ex) {
                Logger.info("UserHome :: getUserProfilePictureByUserId : no profilepicture for user => " + ex, ex);
                return null;
            }
        }
        return null;
    }

    public List<QuestMemberDTO> getUserQuestActivityByQuestId(final Integer questId) {
        if (questId == null) {
            return emptyList();
        }
        try {
			// Note: hibernate doesn't like null here so 0 is used in place of null.  Since there is no corresponding records with id = 0,
			// this is probably ok
            return entityManager.createQuery("SELECT " +
                    "NEW dto.QuestMemberDTO(" +
                    "   qa.questId, " +
                    "   COALESCE(u.id, 0), " +
                    "   u.userName, " +
                    "   u.name, " +
                    "   u.profilePictureURL, " +
                    "   u.firstName, " +
                    "   u.lastName, " +
                    "   '', " +
                    "   u.zip, " +
                    "   pc.city, " +
                    "   pc.state, " +
                    "   pc.point, " +
                    "   COALESCE(u.isUserBrand, 'N'), " +
                    "   FALSE, " +
                    "   FALSE, " +
                    "   COUNT(DISTINCT qt_c.id), " +
                    "   COUNT(DISTINCT qt_a.id), " +
                    "   MAX(qa.cyclesCounter), " +
                    "   0, " +
                    "   CASE " +
                    "     WHEN qa.status = 'IN_PROGRESS' AND (qa.mode = 'PACE_YOURSELF' OR qa.mode = 'TEAM') THEN 'Doer' " +
                    "     WHEN qa.status = 'IN_PROGRESS' AND qa.mode = 'SUPPORT_ONLY' THEN 'Supporter' " +
                    "     WHEN qa.status = 'COMPLETE' THEN 'Achiever' " +
                    "     ELSE 'Unknown'" +
                    "   END, " +
                    "   0 " +
                    ") " +
                    "FROM QuestActivity qa " +
                    "INNER JOIN User u ON u.id = qa.userId " +
                    "LEFT OUTER JOIN QuestTasks qt_a ON qt_a.questId = qa.questId AND qt_a.userId = qa.userId " +
                    "LEFT OUTER JOIN QuestTasks qt_c ON qt_c.id = qt_a.id AND qt_c.taskCompleted = 'TRUE' " +
                    "LEFT OUTER JOIN PostalCodeUs pc ON pc.zip = u.zip " +
                    "WHERE qa.questId = :questId " +
                    "GROUP BY qa.questId, qa.userId " +
                    "ORDER BY qa.addedDate DESC", QuestMemberDTO.class)
                    .setParameter("questId", questId)
                    .getResultList();
        } catch (final PersistenceException e) {
            Logger.error(format("Error getting user quest activity for Quest with ID [%s]", questId), e);
            return emptyList();
        }
    }

    public List<QuestMemberDTO> getQuestBackings(final Integer questId, final Integer doerId, Integer fundraisingTeamCreatorId) {
        if (questId == null || doerId == null) {
            return emptyList();
        }
        TypedQuery<QuestMemberDTO> query = entityManager.createQuery("SELECT NEW dto.QuestMemberDTO(" +
                "  qrt.quest.id, " +
                "  CASE WHEN qrt.isAnonymous = TRUE OR qrt.from.user.id IS NULL THEN 0 ELSE qrt.from.user.id END, " +
                "  CASE WHEN qrt.isAnonymous = TRUE THEN 'anonymous' ELSE COALESCE(CAST(pi.id AS string), qrt.from.user.userName) END, " +
                "  CASE WHEN qrt.isAnonymous = TRUE THEN 'Anonymous Hero' " +
                "       WHEN qb.offlineMode = TRUE THEN CONCAT(pi.firstName, ' ', pi.lastName) " +
                "       ELSE CONCAT(COALESCE(pi.firstName, qrt.from.user.firstName), ' ', COALESCE(pi.lastName, qrt.from.user.lastName)) END, " +
                "  CASE WHEN qrt.isAnonymous = TRUE THEN '' ELSE COALESCE(qrt.from.user.profilePictureURL, '') END, " +
                "  CASE WHEN qrt.isAnonymous = TRUE THEN 'Anonymous' ELSE COALESCE(pi.firstName, qrt.from.user.firstName) END, " +
                "  CASE WHEN qrt.isAnonymous = TRUE THEN 'Hero' ELSE COALESCE(pi.lastName, qrt.from.user.lastName) END, " +
                "  CASE WHEN qb.backerFirstName IS NOT NULL OR qb.backerLastName IS NOT NULL " +
                "       THEN CONCAT(COALESCE(qb.backerFirstName, ''), ' ', COALESCE(qb.backerLastName, '')) ELSE '' END, " +
                "  CASE WHEN qrt.isAnonymous = TRUE THEN '' " +
                "       WHEN qb.offlineMode = TRUE THEN pc.zip" +
                "       ELSE COALESCE(pc.zip, qrt.from.user.zip) END, " +
                "  pc.city, " +
                "  pc.state, " +
                "  pc.point, " +
                "  CASE WHEN qrt.isAnonymous = TRUE THEN 'N' ELSE qrt.from.user.isUserBrand END, " +
                "  CASE WHEN qrt.isAnonymous = TRUE THEN TRUE ELSE FALSE END, " +
                "  CASE WHEN qb.offlineMode IS NULL THEN FALSE ELSE qb.offlineMode END, " +
                "  0, " +
                "  0, " +
                "  0, " +
                "  CASE WHEN qb.id IS NULL THEN 0 ELSE COALESCE(qb.amountInCents, 0) END, " +
                "  'Backer', " +
                "  pi.teamId" +
                ") " +
                "FROM QuestRelatedTransactions qrt " +
                "LEFT OUTER JOIN QuestBackings qb ON qb.paymentTransaction.id = qrt.id " +
                "LEFT OUTER JOIN PersonalInfo pi ON pi.id = qb.billingPersonalInfo.id " +
                "LEFT OUTER JOIN Addresses a ON a.id = qb.billingAddress.id " +
                "LEFT OUTER JOIN PostalCodeUs pc ON pc.zip = a.zip AND qrt.isAnonymous != TRUE " +
                (fundraisingTeamCreatorId == null ? "" : "LEFT OUTER JOIN FundraisingTransactions ft ON qrt.id = ft.id ") +
                "WHERE qrt.quest.id = :questId AND qrt.valid = TRUE " +
                "AND (qrt.to.user.id = :doerId OR qrt.quest.createdBy = :doerId) " +
                (fundraisingTeamCreatorId == null ? "" : " AND ft.intermediary.id = :fundraisingTeamCreatorId ") +
                "GROUP BY qrt.id " +
                "ORDER BY qrt.createdOn DESC", QuestMemberDTO.class);

        query.setParameter("questId", questId);
        query.setParameter("doerId", doerId);
        if (fundraisingTeamCreatorId != null) {
            query.setParameter("fundraisingTeamCreatorId", fundraisingTeamCreatorId);
        }
        return query.getResultList();
    }

    public List<QuestMemberDTO> getQuestRaisedFunds(final Integer questId, final Integer viaUserId) {
        if (questId == null || viaUserId == null) {
            return emptyList();
        }
        return entityManager.createQuery("SELECT NEW dto.QuestMemberDTO(" +
                "  ft.quest.id, " +
                "  ft.from.user.id, " +
                "  qb.amountInCents, " +
                "  'Backer', " +
                "  pi.teamId" +
                ") " +
                "FROM FundraisingTransactions ft " +
                "LEFT OUTER JOIN QuestBackings qb ON qb.paymentTransaction.id = ft.id " +
                "LEFT OUTER JOIN PersonalInfo pi ON pi.id = qb.billingPersonalInfo.id " +
                "WHERE ft.quest.id = :questId AND ft.intermediary.id = :doerId AND ft.valid = TRUE " +
                "GROUP BY ft.id " +
                "ORDER BY ft.createdOn DESC", QuestMemberDTO.class)
                .setParameter("questId", questId)
                .setParameter("doerId", viaUserId)
                .getResultList();
    }

    public List<QuestMemberDTO> getQuestParticipants(final Integer questId, final Integer doerId) {
        if (questId == null || doerId == null) {
            return emptyList();
        }
        return entityManager.createQuery("SELECT NEW dto.QuestMemberDTO(" +
                "  tpt.event.quest.id, " +
                "  COALESCE(u.id, 0), " +
                "  COALESCE(u.userName, CAST(pi.id AS string)), " +
                "  CONCAT(pi.firstName, ' ', pi.lastName), " +
                "  u.profilePictureURL, " +
                "  hp.person.firstName, " +
                "  hp.person.lastName, " +
                "  '', " +
                "  a.zip, " +
                "  pc.city, " +
                "  pc.state, " +
                "  pc.point, " +
                "  COALESCE(u.isUserBrand, 'N'), " +
                "  FALSE, " +
                "  FALSE, " +
                "  0, " +
                "  0, " +
                "  0, " +
                "  0, " +
                "  'Doer', " +
                "  pi.teamId" +
                ") " +
                "FROM HappeningParticipants hp " +
                "INNER JOIN Happenings h ON h.id = hp.event.id " +
                "INNER JOIN TicketPurchaseTransactions tpt ON tpt.id = hp.order.id AND tpt.event.id = h.id " +
                "LEFT OUTER JOIN PersonalInfo pi ON pi.id = hp.person.id " +
                "LEFT OUTER JOIN Addresses a ON a.id = hp.person.id " +
                "LEFT OUTER JOIN User u ON u.email = pi.email " +
                "LEFT OUTER JOIN PostalCodeUs pc ON pc.zip = u.zip " +
                "WHERE tpt.event.quest.id = :questId AND tpt.to.user.id = :doerId AND tpt.valid = TRUE " +
                "ORDER BY tpt.createdOn DESC", QuestMemberDTO.class)
                .setParameter("questId", questId)
                .setParameter("doerId", doerId)
                .getResultList();
    }

    private List<Integer> addBrandsToFriendsList(List<Integer> userIds) {
        try {
            //changed to only diemlife brands to start - users need to follow brands in order to see them in feed.
            Query query = entityManager.createQuery("SELECT u.id FROM User u WHERE u.isUserBrand = 'Y'");
            ArrayList<Integer> brandIds = (ArrayList<Integer>) query.getResultList();

            if (brandIds.size() > 0) {
                brandIds.addAll(userIds);
            }

            return brandIds;
        } catch (Exception ex) {
            Logger.error("UserHome :: addBrandsToFriendsList : error adding brands to user list => " + ex, ex);
            return userIds;
        }
    }

    public Brand getCompanyForUser(User user) {
        CompanyRoleDAO representativeDao = new CompanyRoleDAO();
        CompanyRole representative = representativeDao.getCompanyRoleForUser(user);

        if (representative != null) {
        	// FIXME Vinayak
//            return representative.getCompany();
        }

        return null;
    }
}
