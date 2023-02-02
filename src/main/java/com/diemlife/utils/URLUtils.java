package com.diemlife.utils;

import static java.lang.String.format;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import com.diemlife.models.QuestImage;
import com.diemlife.models.QuestSEO;
import com.diemlife.models.QuestTeam;
import com.diemlife.models.UserSEO;
import com.github.slugify.Slugify;

import lombok.Data;
import play.Logger;

public final class URLUtils {

    private URLUtils() {
        super();
    }

    public static URL getImageUrl(final QuestImage image) {
        if (image == null) {
            return null;
        }
        try {
            return URI.create(image.getQuestImageUrl()).toURL();
        } catch (final MalformedURLException e) {
            Logger.warn(format("Unable to read URL from string '%s' for Quest image with ID [%s]", image.getQuestImageUrl(), image.getId()), e);

            return null;
        }
    }

    public static QuestSEOSlugs publicQuestSEOSlugs(final QuestSEO quest, final UserSEO doer, final String environmentUrlPrefix) {
        final Slugify slugger = new Slugify().withLowerCase(true);
        final String questSlug = slugger.slugify(quest.getTitle());
        final String doerSlug = slugger.slugify(doer.getUserName());
        final Integer questId = quest.getId();
        final Integer userId = doer.getId();
        final String path = format("/%s/%s/%s/%s", doerSlug, questSlug, questId, userId);
        return new QuestSEOSlugs(questSlug, questId, doerSlug, userId, path, environmentUrlPrefix + path);
    }

    public static QuestSEOSlugs publicTeamQuestSEOSlugs(final QuestTeam team, final String environmentUrlPrefix) {
        final Slugify slugger = new Slugify().withLowerCase(true);
        final String questSlug = slugger.slugify(team.getQuest().getTitle());
        final String teamSlug = slugger.slugify(team.getName());
        final Integer questId = team.getQuest().getId();
        final Integer userId = team.getCreator().getId();
        final String path = format("/%s/%s/%s/%s", teamSlug, questSlug, questId, userId);
        return new QuestSEOSlugs(questSlug, questId, teamSlug, userId, path, environmentUrlPrefix + path);
    }

    public static String seoFriendlyPublicQuestPath(final QuestSEO quest, final UserSEO doer) {
        return publicQuestSEOSlugs(quest, doer, "").seoFriendlyUrl;
    }

    public static String seoFriendlyTeamQuestUrl(final QuestTeam team, final String environmentUrlPrefix) {
        return environmentUrlPrefix + relativeTeamUrl(team);
    }

    public static String seoFriendlyUserProfilePath(final UserSEO user) {
        return "/profile/" + user.getId();
    }

    public static String seoFriendlierUserProfilePath(final UserSEO user) {
        return "/profile/" + user.getUserName();
    }

    public static String explorePageCategories(final String category) {
        Slugify slugify = new Slugify().withLowerCase(true);

        return "explore/" + slugify.slugify(category);
    }

    public static String relativeTeamUrl(final QuestTeam team) {
        final Slugify slug = new Slugify().withLowerCase(true).withTransliterator(true);
        return format("/%s/%s/%s/%s", slug.slugify(team.getName()), slug.slugify(team.getQuest().getTitle()), team.getQuest().getId(), team.getCreator().getId());
    }

    @Data
    public static class QuestSEOSlugs implements Serializable {
        private final String questTitleSlug;
        private final Integer questId;
        private final String userNameSlug;
        private final Integer userId;
        private final String shortUrl;
        private final String seoFriendlyUrl;
    }

}
