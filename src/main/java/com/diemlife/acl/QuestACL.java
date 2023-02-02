package com.diemlife.acl;

import com.diemlife.constants.PrivacyLevel;
import com.diemlife.dao.ContentReportDAO;
import com.diemlife.dao.QuestInviteDAO;
import com.diemlife.dao.UserHome;
import com.diemlife.dao.UserRelationshipDAO;
import com.diemlife.models.Quests;
import com.diemlife.models.User;

import javax.persistence.EntityManager;
import java.util.List;

import static com.diemlife.acl.VoterPredicate.VotingResult.Abstain;
import static com.diemlife.acl.VoterPredicate.VotingResult.Against;
import static com.diemlife.acl.VoterPredicate.VotingResult.For;
import static java.util.Collections.emptyList;

public class QuestACL extends ACL<Quests> {

    private final PublicLevelVoter publicLevelVoter;
    private final PrivateLevelVoter privateLevelVoter;
    private final FriendsLevelVoter friendsLevelVoter;
    private final InviteLevelVoter inviteLevelVoter;
    private final HiddenFromUserVoter hiddenFromUserVoter;

    QuestACL(final User candidate, final EntityManager entityManager) {
        super(candidate);
        this.publicLevelVoter = new PublicLevelVoter();
        this.privateLevelVoter = new PrivateLevelVoter();
        this.friendsLevelVoter = new FriendsLevelVoter(entityManager);
        this.inviteLevelVoter = new InviteLevelVoter(entityManager);
        this.hiddenFromUserVoter = new HiddenFromUserVoter(entityManager);
    }

    @Override
    public VotingResult test(final Quests quest) {
        if (quest == null) {
            return VotingResult.Against;
        } else if (quest.getPrivacyLevel() == null) {
            return For;
        }
        return matchPrivacyVoter(quest.getPrivacyLevel()).voteIsQuestVisible(quest)
                .and(hiddenFromUserVoter.voteIsQuestVisible(quest));
    }

    private Voter matchPrivacyVoter(final PrivacyLevel privacy) {
        switch (privacy) {
            case PUBLIC:
                return publicLevelVoter;
            case PRIVATE:
                return privateLevelVoter;
            case FRIENDS:
                return friendsLevelVoter;
            case INVITE:
                return inviteLevelVoter;
            default:
                return quest -> VotingResult.Abstain;
        }
    }

    private interface Voter {
        VotingResult voteIsQuestVisible(final Quests quest);
    }

    private final class PublicLevelVoter implements Voter {
        @Override
        public VotingResult voteIsQuestVisible(final Quests quest) {
            return For;
        }
    }

    private final class PrivateLevelVoter implements Voter {
        @Override
        public VotingResult voteIsQuestVisible(final Quests quest) {
            if (candidate == null) {
                return Abstain;
            }
            return candidate.getId().equals(quest.getCreatedBy())
                    ? For
                    : Against;
        }
    }

    private final class FriendsLevelVoter extends OtherUsersCheckingVoter implements Voter {
        private final Lazy<List<Integer>> friendIds = new Lazy<List<Integer>>() {
            @Override
            protected List<Integer> init() {
                if (candidate == null) {
                    return emptyList();
                } else {
                    return UserRelationshipDAO.getCurrentFriendsByUserId(candidate.getId(), entityManager);
                }
            }
        };

        private FriendsLevelVoter(final EntityManager entityManager) {
            super(entityManager);
        }

        @Override
        public VotingResult voteIsQuestVisible(final Quests quest, final User creator) {
            final boolean isOwner = candidate.getId().equals(creator.getId());
            final boolean isAdmin = isQuestAdmin(quest, candidate);
            return isOwner || isAdmin || friendIds.get().contains(creator.getId())
                    ? For
                    : Against;
        }
    }

    private final class InviteLevelVoter extends OtherUsersCheckingVoter implements Voter {
        private InviteLevelVoter(final EntityManager entityManager) {
            super(entityManager);
        }

        @Override
        public VotingResult voteIsQuestVisible(final Quests quest, final User creator) {
            final boolean isOwner = candidate.getId().equals(creator.getId());
            final boolean isAdmin = isQuestAdmin(quest, candidate);
            return isOwner || isAdmin || new QuestInviteDAO(entityManager).getInvitesForQuest(quest)
                    .stream()
                    .map(invite -> invite.invitedUser)
                    .anyMatch(invitedUser -> invitedUser.getId().equals(candidate.getId()))
                    ? For
                    : Against;
        }
    }

    private final class HiddenFromUserVoter implements Voter {
        private final EntityManager entityManager;

        HiddenFromUserVoter(final EntityManager entityManager) {
            this.entityManager = entityManager;
        }

        @Override
        public VotingResult voteIsQuestVisible(final Quests quest) {
            if (candidate == null) {
                return For;
            }
            return new ContentReportDAO(entityManager).getSpecificQuestReportsByUser(quest, candidate)
                    .stream()
                    .noneMatch(report -> report.hiddenFlag)
                    ? For
                    : Against;
        }
    }

    private abstract class OtherUsersCheckingVoter implements Voter {
        protected final EntityManager entityManager;

        OtherUsersCheckingVoter(final EntityManager entityManager) {
            this.entityManager = entityManager;
        }

        @Override
        public final VotingResult voteIsQuestVisible(final Quests quest) {
            if (candidate == null) {
                return Abstain;
            }
            final Integer creatorId = quest.getCreatedBy();
            if (creatorId == null) {
                return Against;
            }
            final User creator = UserHome.findById(creatorId, entityManager);
            if (creator == null) {
                return Against;
            } else {
                return voteIsQuestVisible(quest, creator);
            }
        }

        protected abstract VotingResult voteIsQuestVisible(final Quests quest, final User creator);
    }

    private static boolean isQuestAdmin(Quests quest, final User candidate) {
        return quest.getAdmins() != null
                && quest.getAdmins().stream().anyMatch(admin -> candidate.getId().equals(admin.getId()));
    }

}
