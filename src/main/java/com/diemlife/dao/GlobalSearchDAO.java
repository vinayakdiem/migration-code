package com.diemlife.dao;

import acl.QuestEntityWithACL;
import constants.GlobalSearchMode;
import models.Quests;
import models.User;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import play.Logger;
import utils.SearchResponse;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.lucene.search.BooleanClause.Occur.MUST;
import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;
import static utils.URLUtils.seoFriendlyPublicQuestPath;
import static utils.URLUtils.seoFriendlyUserProfilePath;

public class GlobalSearchDAO {

    public static Page<SearchResponse> searchGlobally(final String query,
                                                      final int start,
                                                      final int limit,
                                                      final GlobalSearchMode mode,
                                                      final User currentUser,
                                                      final EntityManager em) {
        try {
            final FullTextEntityManager fullTextEntityManager = getFullTextEntityManager(em);

            final QueryBuilder questQueryBuilder = getQuestQueryBuilder(fullTextEntityManager);
            final Query questQuery = getTokenizedQuery(lowerCase(query), token -> getQuestQuery(token, questQueryBuilder));

            final QueryBuilder userQueryBuilder = getUserQueryBuilder(fullTextEntityManager);
            final Query userQuery = getTokenizedQuery(lowerCase(query), token -> getUserQuery(token, userQueryBuilder));

            final Query combinedQuery;
            final Class[] entityClasses;
            switch (mode) {
                case global:
                    combinedQuery = new BooleanQuery.Builder()
                            .add(questQuery, SHOULD)
                            .add(userQuery, SHOULD)
                            .build();
                    entityClasses = new Class[]{Quests.class, User.class};
                    break;
                case quests:
                    combinedQuery = questQuery;
                    entityClasses = new Class[]{Quests.class};
                    break;
                case people:
                    combinedQuery = userQuery;
                    entityClasses = new Class[]{User.class};
                    break;
                default:
                    combinedQuery = new MatchNoDocsQuery();
                    entityClasses = new Class[]{};
            }

            final Sort sort = isBlank(query)
                    ? new Sort(new SortField("modificationDate", SortField.Type.LONG, true))
                    : new Sort(SortField.FIELD_SCORE);
            final List searchResults = fullTextEntityManager.createFullTextQuery(combinedQuery, entityClasses)
                    .setFirstResult(start)
                    .setMaxResults(limit + 1)
                    .setSort(sort)
                    .setProjection(FullTextQuery.SCORE, FullTextQuery.EXPLANATION, FullTextQuery.THIS)
                    .getResultList();
            final boolean hasMore = searchResults.size() > limit;

            final List<SearchResponse> responses = new ArrayList<>();

            for (final Object foundItemProjections : hasMore ? searchResults.subList(0, searchResults.size() - 1) : searchResults) {
                final Object[] projections = (Object[]) foundItemProjections;
                final Float score = (Float) projections[0];
                final Object foundItem = projections[2];
                if (foundItem instanceof Quests) {
                    final QuestEntityWithACL questACL = new QuestEntityWithACL(() -> (Quests) foundItem, em);
                    final Quests quest = questACL.getEntity(currentUser);
                    if (quest != null && responses.size() < limit) {
                        responses.add(SearchResponse.fromQuest(quest)
                                .withScore(score)
                                .withPath(seoFriendlyPublicQuestPath(quest, quest.getUser())));

                        Logger.trace(format("GlobalSearchDAO :: searchGlobally : Score: %s; Found Quest '%s' with description '%s'",
                                score,
                                quest.getTitle(),
                                quest.getQuestFeed()
                        ));
                    }
                } else if (foundItem instanceof User) {
                    if (responses.size() < limit) {
                        final User user = (User) foundItem;
                        responses.add(SearchResponse.fromUser(user)
                                .withScore(score)
                                .withPath(seoFriendlyUserProfilePath(user)));

                        Logger.trace(format("GlobalSearchDAO :: searchGlobally : Score: %s; Found user '%s %s' with username '%s'",
                                score,
                                user.getFirstName(),
                                user.getLastName(),
                                user.getUserName()
                        ));
                    }
                } else {
                    Logger.warn("GlobalSearchDAO :: searchGlobally : Unexpected search result " + foundItem);
                }
            }
            return new Page<SearchResponse>(start, limit, hasMore).withData(responses);
        } catch (final PersistenceException e) {
            Logger.error("GlobalSearchDAO :: searchGlobally : error getting quests => " + e.getMessage(), e);
            return new Page<SearchResponse>(start, limit, false).withData(emptyList());
        }
    }

    private static FullTextEntityManager getFullTextEntityManager(final EntityManager em) {
        return Search.getFullTextEntityManager(em);
    }

    private static QueryBuilder getQuestQueryBuilder(final FullTextEntityManager fullTextEntityManager) {
        return fullTextEntityManager.getSearchFactory()
                .buildQueryBuilder()
                .forEntity(Quests.class)
                .get();
    }

    private static QueryBuilder getUserQueryBuilder(final FullTextEntityManager fullTextEntityManager) {
        return fullTextEntityManager.getSearchFactory()
                .buildQueryBuilder()
                .forEntity(User.class)
                .get();
    }

    private static Query getTokenizedQuery(final String query, final Function<String, Query> tokenSearch) {
        final String[] tokens = StringUtils.split(query);
        if (tokens != null && tokens.length > 0) {
            final BooleanQuery.Builder builder = new BooleanQuery.Builder();
            Stream.of(tokens).forEach(token -> {
                final Query tokenQuery = tokenSearch.apply(trimToEmpty(token));
                builder.add(tokenQuery, MUST);
            });
            return builder.build();
        } else {
            return tokenSearch.apply(EMPTY);
        }
    }

    private static Query getQuestQuery(final String query, final QueryBuilder queryBuilder) {
        return queryBuilder.keyword().wildcard()
                .onField("title").boostedTo(2.0f)
                .andField("questFeed")
                .matching(query.concat("*"))
                .createQuery();
    }

    private static Query getUserQuery(final String query, final QueryBuilder queryBuilder) {
        final Query activeUsersQuery = queryBuilder.keyword()
                .onField("active")
                .matching(true)
                .createQuery();
        final Query matchingUsersQuery = queryBuilder.keyword().wildcard()
                .onField("firstName").boostedTo(1.5f)
                .andField("lastName").boostedTo(1.5f)
                .andField("userName").boostedTo(0.5f)
                .matching(query.concat("*"))
                .createQuery();
        return queryBuilder.bool()
                .must(activeUsersQuery)
                .must(matchingUsersQuery)
                .createQuery();
    }

}
