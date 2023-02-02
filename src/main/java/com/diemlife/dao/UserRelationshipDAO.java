package dao;

import constants.FriendRequestDirection;
import constants.UserRelationshipStatus;
import exceptions.RequiredParameterMissingException;
import models.UserRelationship;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import play.Logger;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static constants.FriendRequestDirection.approved;
import static constants.FriendRequestDirection.received;
import static constants.FriendRequestDirection.sent;
import static constants.UserRelationshipStatus.ACCEPTED;
import static constants.UserRelationshipStatus.NONE;
import static constants.UserRelationshipStatus.PENDING;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

/**
 * Created by andrew on 10/31/16.
 */
public class UserRelationshipDAO {

    public void persist(UserRelationship transientInstance, EntityManager entityManager) {

        EntityTransaction tx = null;
        try {
            tx = entityManager.getTransaction();
            tx.begin();

            entityManager.persist(transientInstance);

            tx.commit();
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) tx.rollback();
            throw e; // or display error message
        }
    }

    public static void remove(UserRelationship persistentInstance, EntityManager entityManager) {

        try {
            entityManager.remove(persistentInstance);
        } catch (Exception e) {
            Logger.error("UserRelationshipDAO :: remove : error removing user relationship => " + e, e);
        }
    }

    public static void addFriendsByUserId(Integer userId, String friendId, EntityManager em) {

        try {
            UserRelationship userRelationship = new UserRelationship();
            userRelationship.setUserOneId(userId);
            userRelationship.setUserTwoId(Integer.valueOf(friendId));
            userRelationship.setStatus(0);
            userRelationship.setActionUserId(userId);

            em.persist(userRelationship);

        } catch (Exception ex) {
            Logger.error("UserRelationshipDAO :: addFriendsByUserId failed for userId and friendId => " + userId + " " + friendId + " " + ex, ex);
        }
    }

    public static void updateUserRelationshipStatus(final UserRelationship relationship,
                                                    final UserRelationshipStatus status,
                                                    final EntityManager em) {
        if (relationship == null) {
            throw new RequiredParameterMissingException("relationship");
        } else {
            relationship.setStatus(status.ordinal());
            em.merge(relationship);
        }
    }

    public static void updateUserRelationshipStatus(final Integer userId,
                                                    final Integer friendId,
                                                    final UserRelationshipStatus status,
                                                    final EntityManager em) {
        final UserRelationship relationship = findAnyUsersRelationship(userId, friendId, em);
        if (relationship == null) {
            Logger.info(format("No relationship found between users [%s] and [%s] - nothing to update", userId, friendId));
        } else {
            relationship.setStatus(status.ordinal());
            em.merge(relationship);
        }
    }

    public static UserRelationship findAnyUsersRelationship(final Integer userId,
                                                            final Integer friendId,
                                                            final EntityManager em) {
        Logger.debug("UserRelationshipDAO :: Searching for user relationship of userId = " + userId + " and friendId = " + friendId);
        final UserRelationship userToFriend = findUserRelationship(userId, friendId, em);
        return userToFriend == null ? findUserRelationship(friendId, userId, em) : userToFriend;
    }

    private static UserRelationship findUserRelationship(final Integer userId, final Integer friendId, final EntityManager em) {
        Logger.debug("UserRelationshipDAO :: findUserRelationship userId = : " + userId + " friendId = " + friendId);

        try {
            return em.createQuery("SELECT ur FROM UserRelationship ur where ur.userOneId = :userId AND ur.userTwoId = :friendId", UserRelationship.class)
                    .setParameter("userId", userId)
                    .setParameter("friendId", friendId)
                    .getSingleResult();
        } catch (final NoResultException e) {
            Logger.debug("UserRelationshipDAO :: findUserRelationship not result found for userId = : " + userId + " friendId = " + friendId);

            return null;
        }
    }

    public static Integer checkForFriendshipStatus(Integer userId, Integer friendId, EntityManager em) {
        Integer relationshipStatus;

        try {
            Query query = em.createQuery("SELECT ur from UserRelationship ur where ur.userOneId = :userId AND ur.userTwoId = :friendId");
            query.setParameter("userId", userId);
            query.setParameter("friendId", friendId);
            UserRelationship userRelationship = (UserRelationship) query.getSingleResult();
            if (userRelationship != null) {
                Logger.info("First user query  =" + userRelationship.getId());
                /* Status hints =
                    0 = Pending
                    1 = Accepted
                    2 = Declined
                    3 = Blocked
                * */
                relationshipStatus = Integer.valueOf(userRelationship.getStatus());
                return relationshipStatus;
            }
        } catch (NoResultException nre) {
            //Trying to see if the other user requested it now, since first was wrong:
            //Need to ensure that they are not friends already
            try {
                Query querySecond = em.createQuery("SELECT ur from UserRelationship ur where ur.userTwoId = :userId AND ur.userOneId = :friendId");
                querySecond.setParameter("userId", userId);
                querySecond.setParameter("friendId", friendId);
                UserRelationship userRelationshipSecond = (UserRelationship) querySecond.getSingleResult();
                if (userRelationshipSecond != null) {
                /* Status hints =
                    0 = Pending
                    1 = Accepted
                    2 = Declined
                    3 = Blocked
                * */
                    relationshipStatus = Integer.valueOf(userRelationshipSecond.getStatus());
                    return relationshipStatus;
                }
            } catch (NoResultException ex) {
                //do nothing
            }
        } catch (Exception ex) {
            Logger.info("UserRelationshipDAO :: checkForFriendshipStatus : error getting friends => " + ex, ex);
            return null;
        }

        return null;
    }

    public static List<UserRelationship> getPendingFriendRequestsByUserIdNeedingAction(Integer userId, EntityManager em) {

        ArrayList<UserRelationship> pendingFriendRequests = new ArrayList<>();
        try {
            Query query = em.createQuery("SELECT ur FROM UserRelationship ur where ur.userTwoId = :userId AND ur.status = 0 AND ur.actionUserId <> :userId");
            query.setParameter("userId", userId);
            ArrayList<UserRelationship> userRelationships = (ArrayList<UserRelationship>) query.getResultList();
            if (userRelationships.size() != 0) {
                return userRelationships;
            } else {
                Query querySecond = em.createQuery("SELECT ur FROM UserRelationship ur WHERE ur.userOneId = :userId AND ur.status = 0 AND ur.actionUserId <> :userId");
                querySecond.setParameter("userId", userId);
                userRelationships = (ArrayList<UserRelationship>) querySecond.getResultList();
                if (userRelationships.size() != 0) {
                    return userRelationships;
                }

                return emptyList();
            }

        } catch (NoResultException nre) {
            return emptyList();
        } catch (Exception ex) {
            Logger.error("UserRelationshipDAO :: getPendingFriendRequestsByUserIdNeedingAction failed => " + ex, ex);
            return emptyList();
        }
    }

    public static List<UserRelationship> getPendingFriendRequestsByUserIdNoAction(Integer userId, EntityManager em) {
        try {
            Query query = em.createQuery("SELECT ur FROM UserRelationship ur where ur.userTwoId = :userId AND ur.status = 0 AND ur.actionUserId = :userId");
            query.setParameter("userId", userId);
            ArrayList<UserRelationship> userRelationships = (ArrayList<UserRelationship>) query.getResultList();
            if (userRelationships.size() != 0) {
                return userRelationships;
            } else {
                Query querySecond = em.createQuery("SELECT ur FROM UserRelationship ur WHERE ur.userOneId = :userId AND ur.status = 0 AND ur.actionUserId = :userId");
                querySecond.setParameter("userId", userId);
                userRelationships = (ArrayList<UserRelationship>) querySecond.getResultList();
                if (userRelationships.size() != 0) {
                    return userRelationships;
                }
                return emptyList();
            }

        } catch (NoResultException nre) {
            return emptyList();
        } catch (Exception ex) {
            Logger.error("UserRelationshipDAO :: getPendingFriendRequestsByUserIdNoAction : failed => " + ex, ex);
            return emptyList();
        }
    }

    public static List<Integer> getCurrentFriendsByUserId(Integer userId, EntityManager em) {
        ArrayList<Integer> currentFriendIds = new ArrayList<>();
        try {
            TypedQuery<UserRelationship> query = em.createQuery("SELECT ur FROM UserRelationship ur WHERE ur.status = 1 AND ur.userOneId = :userId", UserRelationship.class);
            query.setParameter("userId", userId);
            List<UserRelationship> userRelationshipsOne = query.getResultList();

            TypedQuery<UserRelationship> querySecond = em.createQuery("SELECT ur FROM UserRelationship ur WHERE ur.status = 1 AND ur.userTwoId = :userId", UserRelationship.class);
            querySecond.setParameter("userId", userId);
            List<UserRelationship> userRelationshipsTwo = querySecond.getResultList();

            ArrayList<UserRelationship> finalUserRelationships = new ArrayList<>(userRelationshipsOne);
            if (!userRelationshipsTwo.isEmpty()) {
                finalUserRelationships.addAll(userRelationshipsTwo);
            }

            for (UserRelationship userRelationship : finalUserRelationships) {
                if (userRelationship.getUserTwoId() != userId) {
                    currentFriendIds.add(userRelationship.getUserTwoId());
                } else {
                    currentFriendIds.add(userRelationship.getUserOneId());
                }
            }
            return currentFriendIds;
        } catch (NoResultException ex) {
            Logger.warn(format("no friends found for user [%s]", userId));
            return emptyList();
        } catch (PersistenceException ex) {
            Logger.error("UserRelationshipDAO :: getCurrentFriendsByUserId : failed => " + ex, ex);
            throw new PersistenceException(ex);
        }
    }

    public static List<Pair<Integer, FriendRequestDirection>> getFriendRequestsByUserId(final Integer userId, final EntityManager em) {
        if (userId == null) {
            return emptyList();
        }
        return em.createQuery("SELECT r FROM UserRelationship r WHERE r.status IN (:status) AND (r.userOneId = :userId OR r.userTwoId = :userId)", UserRelationship.class)
                .setParameter("status", asList(PENDING.ordinal(), ACCEPTED.ordinal()))
                .setParameter("userId", userId)
                .getResultList()
                .stream()
                .map(relation -> {
                    final Integer otherUserId = userId.equals(relation.getUserOneId())
                            ? relation.getUserTwoId()
                            : relation.getUserOneId();
                    final UserRelationshipStatus status = Stream.of(UserRelationshipStatus.values())
                            .filter(value -> value.ordinal() == relation.getStatus())
                            .findFirst()
                            .orElse(NONE);
                    switch (status) {
                        case PENDING:
                            return userId.equals(relation.getActionUserId())
                                    ? new ImmutablePair<>(otherUserId, sent)
                                    : new ImmutablePair<>(otherUserId, received);
                        case ACCEPTED:
                            return new ImmutablePair<>(otherUserId, approved);
                        default:
                            return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static List<Integer> getPendingFriendsByUserId(Integer userId, EntityManager em) {

        ArrayList<Integer> currentFriendIds = new ArrayList<>();
        try {
            Logger.info("UserRelationshipDAO :: getPendingFriendsByUserId : userId => " + userId);
            TypedQuery<UserRelationship> query = em.createQuery("SELECT ur FROM UserRelationship ur WHERE ur.status = 0 AND ur.userOneId = :userId", UserRelationship.class);
            query.setParameter("userId", userId);
            List<UserRelationship> userRelationshipsOne = query.getResultList();

            TypedQuery<UserRelationship> querySecond = em.createQuery("SELECT ur FROM UserRelationship ur WHERE ur.status = 0 AND ur.userTwoId = :userId", UserRelationship.class);
            querySecond.setParameter("userId", userId);
            List<UserRelationship> userRelationshipsTwo = querySecond.getResultList();


            ArrayList<UserRelationship> finalUserRelationships = new ArrayList<>(userRelationshipsOne);
            if (!userRelationshipsTwo.isEmpty()) {
                finalUserRelationships.addAll(userRelationshipsTwo);
            }

            for (UserRelationship userRelationship : finalUserRelationships) {
                if (userRelationship.getUserTwoId() != userId) {
                    currentFriendIds.add(userRelationship.getUserTwoId());
                } else {
                    currentFriendIds.add(userRelationship.getUserOneId());
                }
            }

            return currentFriendIds;

        } catch (Exception ex) {
            Logger.error("UserRelationshipDAO :: getPendingFriendsByUserId : failed => " + ex, ex);
            return emptyList();
        }
    }

    public static UserRelationship getUserRelationshipByUserIdAndFriendId(Integer userId, Integer friendId, EntityManager em) {

        if (userId != null && friendId != null) {
            try {
                Query query = em.createQuery("SELECT ur FROM UserRelationship ur WHERE ur.status = 1 AND ur.userOneId = :userId AND ur.userTwoId = :friendId");
                query.setParameter("userId", userId);
                query.setParameter("friendId", friendId);
                UserRelationship userRelationship = (UserRelationship) query.getSingleResult();

                return userRelationship;
            } catch (NoResultException nre) {

                Query secondQuery = em.createQuery("SELECT ur FROM UserRelationship ur WHERE ur.status = 1 AND ur.userOneId = :userId AND ur.userTwoId = :friendId");
                secondQuery.setParameter("userId", friendId);
                secondQuery.setParameter("friendId", userId);
                UserRelationship userRelationship = (UserRelationship) secondQuery.getSingleResult();

                return userRelationship;
            } catch (Exception ex) {
                Logger.info("UserRelationshipDAO :: getUserRelationshipByUserIdAndFriendId : error getting friend by user id and friend id => " + ex, ex);
            }
        }

        return null;

    }

}
