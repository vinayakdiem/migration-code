package com.diemlife.services;

import com.diemlife.constants.AccountStatus;
import com.diemlife.dao.StripeCustomerDAO;
import com.diemlife.dao.UserHome;
import com.diemlife.jwt.JwtSessionUser;
import com.diemlife.models.StripeEntity;
import com.diemlife.models.User;
import play.Logger;
import play.db.jpa.JPAApi;
import play.mvc.Http.Session;
import com.diemlife.providers.RedisAuthProvider;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

import static com.diemlife.constants.AccountStatus.NON_EXISTENT;
import static com.diemlife.constants.AccountStatus.UNVERIFIED;
import static com.diemlife.constants.AccountStatus.VERIFIED;
import static java.lang.String.format;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Created by andrewcoleman on 4/11/17.
 */
@Singleton
public class UserProvider {

    private final RedisAuthProvider redisAuthProvider;
    private final JPAApi jpaApi;

    @Inject
    public UserProvider(final RedisAuthProvider redisAuthProvider,
                        final JPAApi api) {
        this.redisAuthProvider = redisAuthProvider;
        this.jpaApi = api;
    }

    @Nullable
    public User getUser(Session session) {
        final String email = JwtSessionUser.userEmailFromSession(session);
        final String provider = JwtSessionUser.userProviderFromSession(session);
        if (isBlank(email)) {
            return null;
        } else {
            final User persistedUser = new UserHome().findByUsernamePasswordIdentity(email, provider, jpaApi.em());
            if (persistedUser == null) {
                Logger.error(format("User not found in database for email '%s' and authentication provider '%s'", email, provider));

                final String tokenHash = JwtSessionUser.userTokenHashFromSession(session);
                if (isNotBlank(tokenHash)) {
                    Logger.info(format("Removing broken JWT of user '%s' from Redis: %s", email, tokenHash));

                    redisAuthProvider.remove(tokenHash);
                }
            } else {
                persistedUser.setProvider(provider);
            }
            return persistedUser;
        }
    }

    public List<User> getUsersForSearch(final int userId, final String userName) {
        if (userId != 0 && userName != null) {
            return UserHome.getUsersNotCurrentlyFriendsWithByUserId(userId, userName, jpaApi.em());
        } else {
            return new ArrayList<>();
        }
    }

    public <T extends StripeEntity> T getStripeCustomerByUserId(final Integer userId, final Class<T> type) {
        if (userId == null) {
            return null;
        }
        final EntityManager em = jpaApi.em();
        final User currentUser = UserHome.findById(userId, em);
        if (currentUser == null) {
            return null;
        } else {
            return new StripeCustomerDAO(em).getByUserId(currentUser.getId(), type);
        }
    }

    public AccountStatus getAccountStatusByEmail(final String email) {
        final User user = UserHome.findByEmail(email, jpaApi.em());
        if (user == null) {
            return NON_EXISTENT;
        }
        if (isTrue(user.getActive()) && isTrue(user.getEmailValidated())) {
            return VERIFIED;
        } else {
            return UNVERIFIED;
        }
    }

    public List<User> getUsersWithAvatars(EntityManager entityManager) {
        List<User> usersWithAvatars;
        try {
            usersWithAvatars = entityManager.createQuery("SELECT u FROM User u WHERE u.profilePictureOriginal IS NOT NULL", User.class)
                    .getResultList();
        } catch (Exception e) {
            Logger.error("unable to get users original avatars");
            return null;
        }
        return usersWithAvatars;
    }

    public List<User> getUsersWithCover(EntityManager entityManager) {
        List<User> usersWithCoverPictures;
        try {
            usersWithCoverPictures = entityManager.createQuery("SELECT u FROM User u WHERE u.coverPictureOriginal IS NOT NULL", User.class)
                    .getResultList();
        } catch (Exception e) {
            Logger.error("unable to get users original covers");
            return null;
        }
        return usersWithCoverPictures;
    }

}
