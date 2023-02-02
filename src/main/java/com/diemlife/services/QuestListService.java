package com.diemlife.services;

import com.typesafe.config.Config;
import com.diemlife.constants.DeploymentEnvironments;
import com.diemlife.constants.QuestMode;
import com.diemlife.dao.FundraisingLinkDAO;
import com.diemlife.dao.MyQuestsListDAO;
import com.diemlife.dao.QuestTeamDAO;
import com.diemlife.dao.QuestsDAO;
import com.diemlife.dto.MyQuestDTO;
import com.diemlife.dto.QuestActivityDTO;
import com.diemlife.dto.QuestDTO;
import com.diemlife.dto.QuestListDTO;
import com.diemlife.dto.QuestListDetailDTO;
import com.diemlife.dto.QuestListFilterDTO;
import com.diemlife.dto.QuestUserFlagsDTO;
import com.diemlife.models.FundraisingLink;
import com.diemlife.models.QuestTeam;
import com.diemlife.models.Quests;
import com.diemlife.models.User;
import com.diemlife.models.UserSEO;
import org.springframework.util.StopWatch;
import play.Logger;
import play.db.Database;
import play.db.NamedDatabase;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import com.diemlife.utils.URLUtils.QuestSEOSlugs;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.diemlife.constants.QuestMode.PACE_YOURSELF;
import static com.diemlife.constants.QuestMode.SUPPORT_ONLY;
import static com.diemlife.constants.QuestMode.TEAM;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static com.diemlife.utils.URLUtils.publicQuestSEOSlugs;
import static com.diemlife.utils.URLUtils.publicTeamQuestSEOSlugs;

@Singleton
public class QuestListService {

    private final JPAApi jpaApi;
    private final Config config;
    private final FundraisingLinkDAO fundraisingLinkDao;
    private final Database dbRo;

    @Inject
    public QuestListService(final JPAApi jpaApi, final Config config, final FundraisingLinkDAO fundraisingLinkDao, @NamedDatabase("ro") Database dbRo) {
        this.jpaApi = jpaApi;
        this.config = config;
        this.fundraisingLinkDao = fundraisingLinkDao;
        this.dbRo = dbRo;
    }

    @Transactional(readOnly = true)
    public List<QuestDTO> loadMyQuestsForUser(final User loggedInUser, final User requestedUser) {
        final StopWatch timer = new StopWatch(format("Loading my Quests list for user '%s'", requestedUser.getEmail()));

        timer.start("Loading my Quests statistics");

        final EntityManager em = jpaApi.em();
        final MyQuestsListDAO myQuestsDao = new MyQuestsListDAO();
        
        List<MyQuestDTO> myQuests;
        try (Connection c = dbRo.getConnection()) {
            myQuests = myQuestsDao.loadMyQuests(c, requestedUser.getId());
        } catch (SQLException e) {
            Logger.error("loadMyQuestsForUser - can't fetch quest list", e);
            myQuests = Collections.emptyList();
        }

        timer.stop();
        timer.start("Loading all Quests data");

        final Map<Integer, MyQuestDTO> myQuestsMap = myQuests.stream().collect(toMap(myQuest -> myQuest.questId.intValue(), myQuest -> myQuest));
        final Map<Integer, Quests> allQuestsMap = QuestsDAO.findQuestsWithAclByIds(myQuestsMap.keySet(), em).getList(loggedInUser)
                .stream()
                .collect(toMap(Quests::getId, quest -> quest));

        timer.stop();
        timer.start("Loading user teams");

        final List<QuestTeam> requestedUserTeams = new QuestTeamDAO(em).listActiveTeamsForUser(requestedUser);

        timer.stop();
        timer.start("Loading user fundraisers");

        final List<FundraisingLink> requestedUserFundraisers = fundraisingLinkDao.getUserFundraisingLinks(requestedUser);
        final String envUrl = config.getString(DeploymentEnvironments.valueOf(config.getString("application.mode")).getBaseUrlKey());

        timer.stop();
        timer.start("Mapping results");

        final FundraisingLinkDAO fundraisingDao = new FundraisingLinkDAO(jpaApi);
        final List<QuestDTO> result = new ArrayList<>();
        for (final MyQuestDTO myQuest : myQuests) {
            final Quests quest = allQuestsMap.get(myQuest.questId.intValue());
            final Optional<QuestListDTO> resultOptional = quest == null
                    ? Optional.empty()
                    : Optional.ofNullable(QuestListDTO.toDTO(quest, loggedInUser));
            resultOptional
                    .map(dto -> dto.withDetail(mapToListDetails(myQuest))
                            .withFilter(mapToFilter(myQuest))
                            .withUserDoing(myQuest.questActivityExists))
                    .map(dto -> dto.withUserFlags(QuestUserFlagsDTO.builder()
                            .withFollowing(isTrue(myQuest.questFollowed))
                            .withSaved(isTrue(myQuest.questSaved))
                            .withStarred(isTrue(myQuest.questStarred))
                            .build()))
                    .map(dto -> dto.withActivityData(mapToActivity(myQuest)))
                    .map(dto -> dto.withSEOSlugs(mapSeoSlugs(
                            dto,
                            loggedInUser == null ? requestedUser : loggedInUser,
                            requestedUserTeams,
                            requestedUserFundraisers,
                            () -> myQuest.questActivityExists,
                            envUrl
                    )))
                    .map(dto -> dto.withIsQuestFundraiser(fundraisingDao.existsWithQuestAndFundraiserUser(dto, requestedUser)))
                    .ifPresent(result::add);
        }

        timer.stop();

        Logger.info(timer.prettyPrint());

        return result;
    }

    public static QuestSEOSlugs mapSeoSlugs(final QuestDTO quest,
                                            final User currentUser,
                                            final List<QuestTeam> requestedUserTeams,
                                            final List<FundraisingLink> requestedUserFundraisers,
                                            final Supplier<Boolean> hasCurrentUserQuestActivity,
                                            final String environmentUrl) {
        final Optional<FundraisingLink> requestedUserFundraiser = requestedUserFundraisers.stream()
                .filter(link -> link.quest.getId().equals(quest.id))
                .findFirst();
        if (requestedUserFundraiser.isPresent()) {
            return publicQuestSEOSlugs(quest, currentUser, environmentUrl);
        } else if (TEAM.getKey().equals(quest.getActivityMode())) {
            final QuestTeam team = mapToTeam(quest, requestedUserTeams);
            if (team == null || team.isDefaultTeam()) {
                return publicQuestSEOSlugs(quest, quest.user, environmentUrl);
            } else {
                return publicTeamQuestSEOSlugs(team, environmentUrl);
            }
        } else if (SUPPORT_ONLY.getKey().equals(quest.getActivityMode())) {
            return publicQuestSEOSlugs(quest, quest.user, environmentUrl);
        } else if (PACE_YOURSELF.getKey().equals(quest.getActivityMode())) {
            final UserSEO slugUser = hasCurrentUserQuestActivity.get() ? currentUser : quest.user;
            return publicQuestSEOSlugs(quest, slugUser, environmentUrl);
        } else {
            return publicQuestSEOSlugs(quest, quest.user, environmentUrl);
        }
    }

    private static QuestListDetailDTO mapToListDetails(final MyQuestDTO my) {
        return QuestMode.PACE_YOURSELF.equals(my.questActivityMode) ?
                new QuestListDetailDTO(
                        my.questId.intValue(),
                        my.questActivityMode,
                        my.tasksCompletedDoer.longValue(),
                        my.tasksTotalDoer.longValue(),
                        my.tasksCompletedCreator.longValue(),
                        my.tasksTotalCreator.longValue()
                ) :
                new QuestListDetailDTO(
                        my.questId.intValue(),
                        my.questActivityMode,
                        my.tasksCompletedCreator.longValue(),
                        my.tasksTotalCreator.longValue(),
                        my.tasksCompletedCreator.longValue(),
                        my.tasksTotalCreator.longValue()
                );
    }

    private static QuestListFilterDTO mapToFilter(final MyQuestDTO myQuest) {
        return QuestListFilterDTO.builder()
                .active(isTrue(myQuest.questInProgress))
                .completed(isTrue(myQuest.questCompleted))
                .saved(isTrue(myQuest.questSaved))
                .build();
    }

    private static QuestActivityDTO mapToActivity(final MyQuestDTO my) {
        return new QuestActivityDTO(
                my.questId.intValue(),
                my.userId.intValue(),
                my.questActivityStatus,
                my.questActivityMode,
                my.questRepeatable,
                my.questCompleteCounter.intValue());
    }

    private static QuestTeam mapToTeam(final QuestDTO quest, final List<QuestTeam> requestedUserTeams) {
        return requestedUserTeams.stream()
                .filter(team -> team.getQuest().getId().equals(quest.id))
                .findFirst()
                .orElse(null);
    }

}
