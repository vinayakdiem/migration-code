package com.diemlife.plugins;

import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import play.Logger;
import play.db.jpa.JPAApi;

import javax.inject.Inject;
import javax.inject.Singleton;

import static java.lang.String.format;

@Singleton
public class HibernateSearchPlugin {

    private final JPAApi jpaApi;

    @Inject
    public HibernateSearchPlugin(final JPAApi jpaApi) {
        Logger.info("Injecting Hibernate Search plugin");
        this.jpaApi = jpaApi;
        onStart();
    }

    public void onStart() {
        FullTextEntityManager fullTextEntityManager = jpaApi.withTransaction(Search::getFullTextEntityManager);
        try {
            fullTextEntityManager.createIndexer().startAndWait();
        } catch (InterruptedException i) {
            Logger.error(format("HibernateSearchPlugin :: onStart : error starting hibernate index [%s]", i));
        }
    }
}
