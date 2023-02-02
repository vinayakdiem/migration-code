package com.diemlife.controller;

import com.diemlife.constants.FriendRequestDirection;
import com.diemlife.constants.GlobalSearchMode;
import com.diemlife.dao.Page;
import com.diemlife.dao.UserHome;
import com.diemlife.dao.UserRelationshipDAO;
import com.diemlife.models.Brand;
import com.diemlife.models.User;
import org.apache.commons.lang3.tuple.Pair;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import security.JwtSessionLogin;
import services.UserProvider;
import com.diemlife.utils.SearchResponse;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.diemlife.dao.GlobalSearchDAO.searchGlobally;
import static java.lang.Integer.parseInt;
import static org.apache.commons.lang.StringUtils.isNumeric;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

public class GlobalSearchController extends Controller {

    private final JPAApi jpaApi;
    private final UserProvider userProvider;

    @Inject
    public GlobalSearchController(final JPAApi jpaApi, final UserProvider userProvider) {
        this.jpaApi = jpaApi;
        this.userProvider = userProvider;
    }

    @Transactional(readOnly = true)
    @JwtSessionLogin(required = true)
    public Result getFriendsIds() {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return forbidden();
        }
        final EntityManager em = this.jpaApi.em();
        final List<Pair<Integer, FriendRequestDirection>> friendsRequests = UserRelationshipDAO.getFriendRequestsByUserId(user.getId(), em);
        final Map<FriendRequestDirection, List<Integer>> friendsMap = new LinkedHashMap<>();
        Stream.of(FriendRequestDirection.values()).forEach(status -> friendsMap.put(status, new ArrayList<>()));
        friendsRequests.forEach(pair -> Optional.ofNullable(friendsMap.get(pair.getRight())).ifPresent(list -> list.add(pair.getLeft())));
        return ok(Json.toJson(friendsMap));
    }

    @Transactional
    @JwtSessionLogin
    public Result searchGlobal() {
        return search(request(), GlobalSearchMode.global, userProvider.getUser(session()));
    }

    @Transactional
    @JwtSessionLogin
    public Result searchQuests() {
        return search(request(), GlobalSearchMode.quests, userProvider.getUser(session()));
    }

    @Transactional
    @JwtSessionLogin
    public Result searchPeople() {
        return search(request(), GlobalSearchMode.people, userProvider.getUser(session()));
    }

    public Result search(final Http.Request request,
                         final GlobalSearchMode mode,
                         final User user) {
        if (user == null) {
            return forbidden();
        }
        final EntityManager em = jpaApi.em();
        final String queryString = request.getQueryString("q");
        final String startString = request.getQueryString("_start");
        final String limitString = request.getQueryString("_limit");
        if (!isNumeric(startString) || !isNumeric(limitString)) {
            return badRequest();
        }
        final Page<SearchResponse> searchResults = searchGlobally(
                trimToEmpty(queryString),
                parseInt(startString),
                parseInt(limitString),
                mode,
                user,
                em
        );
        
        List<SearchResponse> responses = new ArrayList<>();
        for (SearchResponse searchResponse : searchResults.getData()) {
        	User newUser = UserHome.findById(searchResponse.userId, em);
        	Brand company = UserHome.getCompanyForUser(newUser, this.jpaApi);
        	searchResponse.setBrand(company);
        	responses.add(searchResponse);
		}
        
        Page<SearchResponse> response =  new Page<SearchResponse>(searchResults.getStart(), searchResults.getLimit(), searchResults.isMore()).withData(responses);
        return ok(Json.toJson(response));
    }

}