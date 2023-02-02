package com.diemlife.security;

import be.objectify.deadbolt.java.AbstractDeadboltHandler;
import be.objectify.deadbolt.java.DynamicResourceHandler;
import be.objectify.deadbolt.java.ExecutionContextProvider;
import be.objectify.deadbolt.java.models.Subject;
import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.user.AuthUserIdentity;
import com.diemlife.constants.JpaConstants;
import com.diemlife.dao.UserHome;
import com.diemlife.models.User;
import play.db.jpa.JPAApi;
import play.mvc.Http;
import play.mvc.Result;

import javax.persistence.EntityManager;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

//import play.libs.F;
//import play.libs.F.Promise;

public class MyDeadboltHandler extends AbstractDeadboltHandler {

    private final PlayAuthenticate auth;
    private final JPAApi jpaApi;

    public MyDeadboltHandler(final PlayAuthenticate auth, final ExecutionContextProvider exContextProvider, final JPAApi api) {
        super(exContextProvider);
        this.auth = auth;
        this.jpaApi = api;
    }

    @Override
    public CompletionStage<Optional<Result>> beforeAuthCheck(Http.Context context) {
        if (this.auth.isLoggedIn(context.session())) {
            // user is logged in
            return CompletableFuture.completedFuture(Optional.empty());
        } else {
            // user is not logged in
            return CompletableFuture.completedFuture(Optional.of(unauthorized()));
        }
    }

    @Override
    public CompletionStage<Optional<? extends Subject>> getSubject(final Http.Context context) {
        EntityManager em = this.jpaApi.em(JpaConstants.DB);

        AuthUserIdentity u = this.auth.getUser(context);

        UserHome userDao = new UserHome();
        User user = userDao.findByAuthUserIdentity(u, em);

        em.close();
        // Caching might be a good idea here
        return CompletableFuture.completedFuture(Optional.ofNullable((Subject)user));
    }

    @Override
    public CompletionStage<Optional<DynamicResourceHandler>> getDynamicResourceHandler(Http.Context context) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public CompletionStage<Result> onAuthFailure(final Http.Context context, final Optional<String> content) {
        // if the user has a cookie with a valid user and the local user has
        // been deactivated/deleted in between, it is possible that this gets
        // shown. You might want to consider to sign the user out in this case.
        return CompletableFuture.completedFuture(forbidden("Forbidden"));
    }
}
