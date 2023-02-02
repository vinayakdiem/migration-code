package com.diemlife.controller;

import com.diemlife.constants.PromptType;
import com.diemlife.dao.PromptDAO;
import com.diemlife.dto.QuestMemberDTO;
import forms.SurveyResultsForm;
import forms.SurveyResultsForm.SurveyResultForm;
import com.diemlife.models.PromptUser;
import com.diemlife.models.User;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.util.MimeTypeUtils;
import play.Logger;
import play.data.Form;
import play.data.FormFactory;
import play.db.Database;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.db.NamedDatabase;
import play.mvc.Controller;
import play.mvc.Result;
import security.JwtSessionLogin;
import services.UserProvider;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.validation.constraints.NotNull;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

import static com.diemlife.constants.QuestMemberStatus.Doer;
import static com.diemlife.dao.UserHome.getUserQuestActivityByQuestId;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.apache.commons.lang3.ArrayUtils.isNotEmpty;

@JwtSessionLogin
public class SurveysController extends Controller {

    private final UserProvider userProvider;
    private final FormFactory formFactory;
    private final JPAApi jpaApi;

	private Database db;
	private Database dbRo;
	
    @Inject
    public SurveysController(Database db, @NamedDatabase("ro") Database dbRo, final UserProvider userProvider, final FormFactory formFactory, final JPAApi jpaApi) {
		this.db = db;
		this.dbRo = dbRo;
        this.userProvider = userProvider;
        this.formFactory = formFactory;
        this.jpaApi = jpaApi;
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result getUnreadSurvey() {
		Logger.debug("getUnreadSurvey - " + request());
		
        final User user = userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        String username = user.getUserName();
        
        final Integer questId = Optional.ofNullable(request().getQueryString("questId"))
                .filter(StringUtils::isNumeric)
                .map(Integer::parseInt)
                .orElse(null);
        
        if (questId != null) {
            final EntityManager em = jpaApi.em();
            final List<QuestMemberDTO> members = getUserQuestActivityByQuestId(questId, em);
            if (members.stream().noneMatch(member -> user.getId().equals(member.userId) && member.memberStatus.contains(Doer))) {
                // WTF goes here?
            }
            
			try (Connection c = dbRo.getConnection()) {
				PromptDAO promptDao = PromptDAO.getInstance();
				List<PromptUser> puList = promptDao.getUserPrompt(c, username, questId);
				
				JSONObject result = new JSONObject();

                JSONArray ja = new JSONArray();
                for (PromptUser pu : puList) {
                    JSONObject jo = new JSONObject();
                    jo.put("promptEvent", pu.getPromptEvent());
                    jo.put("type", PromptType.idToTag(pu.getMsgType()));
                    jo.put("promptText", pu.getMsg());
                    if (isNotEmpty(pu.getPromptOptions())) {
                        jo.put("promptOptions", new JSONArray(asList(pu.getPromptOptions())));
                    }
                    ja.put(jo);
                }
				
				result.put("questions", ja);
				
				String resultStr = result.toString();
				Logger.debug("getUnreadSurvey - user: " + username + ", quest: " + questId + ", result: " + resultStr);
			 
				return ok(resultStr).as(MimeTypeUtils.APPLICATION_JSON_VALUE);
			 
			} catch (Exception e) {
				Logger.error("getUnreadSurvey - error,  user: " + username + ", quest: " + questId, e);
			}
        } else {
			Logger.warn("getUnreadSurvey - missing questId");
		}
		
		return ok();
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result postSurveyResults(final @NotNull Long questId) {
		Logger.debug("postSurveyResults - " + questId);
		
        final Form<SurveyResultsForm> formBinding = formFactory.form(SurveyResultsForm.class).bindFromRequest();
        if (formBinding.hasErrors()) {
			Logger.warn("postSurveyResults - malformed submission: " + formBinding);
            return badRequest(formBinding.errorsAsJson());
        }
        final SurveyResultsForm form = formBinding.get();
        SurveyResultForm[] promptResults = form.getAnswers();
        if (isEmpty(promptResults)) {
			Logger.warn("postSurveyResults - no results submitted");
            return badRequest();
        }
        final User user = userProvider.getUser(session());
        if (user == null) {
			Logger.warn("postSurveyResults - no user session");
            return unauthorized();
        }
		
		String username = user.getUserName();
		
		try (Connection c = db.getConnection()) {
			for (final SurveyResultForm promptResult : promptResults) {

				Long promptEvent = promptResult.getPromptEvent();
				String msg = promptResult.getPromptText();
				int type = PromptType.tagToId(promptResult.getType());
				String answer = promptResult.getAnswer();
				Double lat = promptResult.getLat();
				Double lon = promptResult.getLon();

				Logger.debug("postSurveyResults - user: " + username + ", quest: " + questId + ", msg: " + msg + ", type: " + type + ", promptEvent: " + promptEvent + ", answer: " + answer);

				PromptDAO promptDao = PromptDAO.getInstance();
				if (promptDao.insertPromptResult(c, username, questId, System.currentTimeMillis(), promptEvent, msg, type, answer, lat, lon)) {
					if (!promptDao.deleteUserPrompt(c, username, questId, promptEvent, msg)) {
						// do anything?  do we care if user gets prompted again?
						Logger.warn("postSurveyResults - failed to delete prompt entry for user: " + username + ", quest: " + questId);
					}
				} else {
					Logger.warn("postSurveyResults - failed to insert response for user: " + username + ", quest: " + questId);
				}
			}
		} catch (Exception e) {
			// TODO: do anything?
		}
		
        return ok();
    }

	// I think the intent with this API call was to be able to assign a survey to a group of users.  I don't think we know exactly
	// how we're going to do that in the near term
    @Transactional
    @JwtSessionLogin(required = true)
    public Result pushSurveyToUsers(final Integer questId) {
		Logger.debug("pushSurveyToUsers");
		
        final User user = userProvider.getUser(session());
        /*
        final EntityManager em = jpaApi.em();
        final DynamicForm form = formFactory.form().bindFromRequest();
        final SurveyType type = Stream.of(SurveyType.values())
                .filter(value -> value.name().equals(form.get("type")))
                .findFirst()
                .orElse(null);
        if (type == null) {
            return badRequest();
        }
        final Integer surveyId = getCurrentSurveyIdForQuest(questId, type);
        if (canEditQuest(findById(questId, em), user)) {
            switch (type) {
                case BringToFront:
                    getUserQuestActivityByQuestId(questId, em)
                            .stream()
                            .filter(member -> member.memberStatus.contains(Doer))
                            .forEach(doer -> surveysDisplay.computeIfAbsent(doer.userId, key -> new HashMap<>()).put(surveyId, TRUE));
                    break;
                case StartQuest:
                    surveysDisplay.values()
                            .stream()
                            .flatMap(value -> value.entrySet().stream())
                            .filter(value -> value.getKey().equals(surveyId))
                            .forEach(entry -> entry.setValue(TRUE));
                    break;
                default:
                    break;
            }
             
            return ok();
        } else {
            return forbidden();
        }
        */
        
        return ok(); 
    }
}

