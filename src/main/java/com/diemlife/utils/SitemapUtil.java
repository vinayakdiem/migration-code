/*package com.diemlife.utils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import com.diemlife.models.Quests;
import com.redfin.sitemapgenerator.ChangeFreq;
import com.redfin.sitemapgenerator.WebSitemapGenerator;
import com.redfin.sitemapgenerator.WebSitemapUrl;
import com.typesafe.config.Config;

import dao.QuestsDAO;
import play.Environment;
import play.Logger;

public class SitemapUtil {

    private static final int MIN_QUEST_DETAIL_LENGTH = 120; //used to ensure our quest pages have some form of content

    private final Config config;
    private final Environment environment;

    @Inject
    public SitemapUtil(final Config config, final Environment environment) {
        this.config = config;
        this.environment = environment;
    }

    public void sitemapGenerator(EntityManager em) {
        final String baseUrl = currentEnvironmentBaseUrl();
        int numUrls = 0;
        String filePath = this.environment.rootPath().getPath();

        try {
            WebSitemapGenerator webSitemapGenerator = WebSitemapGenerator.builder(baseUrl, new File(filePath + "/public/sitemap"))
                    .gzip(false)
                    .build();

            List<Quests> allQuests = QuestsDAO.findAllPublic(em);

            //Add Quests
            for (Quests quest : allQuests) {
                if (quest.getQuestFeed().length() > MIN_QUEST_DETAIL_LENGTH) {
                    String url = URLUtils.seoFriendlyPublicQuestPath(quest, quest.getUser());
                    WebSitemapUrl webSitemapUrl = new WebSitemapUrl.Options(baseUrl + url)
                            .lastMod(quest.getDateModified())
                            .priority(0.8)
                            .changeFreq(ChangeFreq.DAILY)
                            .build();
                    webSitemapGenerator.addUrl(webSitemapUrl);
                    numUrls++;
                }
            }

            webSitemapGenerator.write();

            if (numUrls > 50000) {
                webSitemapGenerator.writeSitemapsWithIndex();
            }
        } catch (IOException e) {
            Logger.error("error building sitemap due to:", e);
        }
    }

    private String currentEnvironmentBaseUrl() {
        final String env = config.getString("play.env");
        final String baseUrl;
        if (env.equalsIgnoreCase("LOCAL")) {
            baseUrl = config.getString("play.localhost.url");
        } else if (env.equalsIgnoreCase("DEV")) {
            baseUrl = config.getString("play.dev.url");
        } else if (env.equalsIgnoreCase("STAGING")) {
            baseUrl = config.getString("play.staging.url");
        } else {
            baseUrl = config.getString("play.prod.url");
        }

        return baseUrl;
    }

}
*/