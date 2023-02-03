package com.diemlife.controller;

import com.diemlife.acl.QuestEntityWithACL;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.github.slugify.Slugify;
import com.typesafe.config.Config;
import com.diemlife.constants.DeploymentEnvironments;
import com.diemlife.constants.ImageType;
import com.diemlife.dao.ExploreCategoriesDAO;
import com.diemlife.dao.HappeningDAO;
import com.diemlife.dao.QuestBackingDAO;
import com.diemlife.dao.QuestEventHistoryDAO;
import com.diemlife.dao.QuestImageDAO;
import com.diemlife.dao.QuestSavedDAO;
import com.diemlife.dao.QuestUserFlagDAO;
import com.diemlife.dao.QuestsDAO;
import com.diemlife.dao.ReferredQuestDAO;
import com.diemlife.dao.S3FileHome;
import com.diemlife.dao.UserHome;
import com.diemlife.dto.ActivityExportDTO;
import com.diemlife.dto.AsCommentsDTO;
import com.diemlife.dto.AsLikesDTO;
import com.diemlife.dto.LeaderboardMaxActivityDTO;
import com.diemlife.dto.QuestDTO;
import com.diemlife.dto.QuestLiteDTO;
import com.diemlife.dto.QuestPermissionsDTO;
import com.diemlife.dto.ReverseGeocodingDTO;
import com.diemlife.exceptions.QuestOperationForbiddenException;
import com.diemlife.exceptions.RequiredParameterMissingException;
import forms.CommentsForm;
import forms.LogActivityForm;
import forms.MessageAllForm;
import forms.QuestActionPointForm;
import forms.QuestEditForm;
import forms.QuestRenameForm;
import com.diemlife.models.ExploreCategories;
import com.diemlife.models.Happening;
import com.diemlife.models.QuestActivity;
import com.diemlife.models.QuestImage;
import com.diemlife.models.QuestTags;
import com.diemlife.models.QuestUserFlag;
import com.diemlife.models.Quests;
import com.diemlife.models.ReferredQuest;
import com.diemlife.models.S3File;
import com.diemlife.models.User;
import org.springframework.util.CollectionUtils;
import play.Logger;
import play.data.DynamicForm;
import play.data.Form;
import play.data.FormFactory;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.db.Database;
import play.db.NamedDatabase;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import com.diemlife.security.JwtSessionLogin;
import com.diemlife.services.ImageProcessingService;
import com.diemlife.services.NotificationService;
import com.diemlife.services.OutgoingEmailService;
import com.diemlife.services.QuestService;
import com.diemlife.services.QuestService;
import com.diemlife.services.ReportingService;
import com.diemlife.services.ReverseGeocodingService;
import com.diemlife.services.UserProvider;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.validation.constraints.NotNull;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.diemlife.acl.VoterPredicate.VotingResult.For;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.diemlife.constants.NotificationType.FOLLOWED;
import static com.diemlife.dao.QuestActivityHome.getQuestsNotInProgressByCategory;
import static com.diemlife.dao.QuestActivityHome.getUsersDoingQuest;
import static com.diemlife.dao.QuestTagsDAO.getQuestTagsById;
import static com.diemlife.dao.QuestTagsDAO.getQuestTagsByTag;
import static com.diemlife.dao.QuestsDAO.findByCategoryWithACL;
import static com.diemlife.dao.QuestsDAO.findById;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static com.diemlife.models.QuestEvents.QUEST_UN_SAVE;
import static org.apache.commons.lang3.BooleanUtils.isTrue;
import static org.springframework.util.StringUtils.isEmpty;
import static com.diemlife.utils.CsvUtils.writeCsvToStream;
import static com.diemlife.utils.QuestSecurityUtils.canEditQuest;
import static com.diemlife.utils.URLUtils.publicQuestSEOSlugs;


import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.apache.commons.lang3.StringUtils.trim;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import static org.apache.commons.lang3.StringUtils.trimToNull;
import static org.apache.commons.lang3.StringUtils.upperCase;

@JwtSessionLogin
public class QuestController extends Controller {

    private final JPAApi jpaApi;
    private final UserProvider userProvider;
    private final OutgoingEmailService emailService;
    private final NotificationService notificationService;
    private final ReportingService reportingService;
    private final ReverseGeocodingService reverseGeocodingService;
    private final QuestService questService;
    private final FormFactory formFactory;
    private final Config config;
    private final Database db;
    private final Database dbRo;
    private final ImageProcessingService imageService;
    private final MessagesApi messagesApi;

    @Inject
    public QuestController(
                           Database db,
                           @NamedDatabase("ro") Database dbRo,
                           final JPAApi jpaApi,
                           final UserProvider userProvider,
                           final OutgoingEmailService emailService,
                           final NotificationService notificationService,
                           final ReportingService reportingService,
                           final ReverseGeocodingService reverseGeocodingService,
                           final QuestService questService,
                           final FormFactory formFactory,
                           final MessagesApi messagesApi,
                           final Config config,
                           final ImageProcessingService imageService) {
        this.jpaApi = jpaApi;
        this.userProvider = userProvider;
        this.emailService = emailService;
        this.notificationService = notificationService;
        this.reportingService = reportingService;
        this.reverseGeocodingService = reverseGeocodingService;
        this.questService = questService;
        this.imageService = imageService;
        this.formFactory = formFactory;
        this.config = config;
        this.db = db;
        this.dbRo = dbRo;
        this.messagesApi = messagesApi;
    }

    @Transactional
    @JwtSessionLogin
    public Result getRelatedQuests(@Nonnull final Integer questId) {
        final EntityManager em = this.jpaApi.em();
        final User user = this.userProvider.getUser(session());
        List<QuestTags> tags = new ArrayList<>();
        List<Quests> quests = new ArrayList<>();

        Quests quest = findById(questId, em);
        if (quest != null) {
            List<QuestTags> questTags = getQuestTagsById(questId, em);


            for (QuestTags questTag : questTags) {
                tags.addAll(getQuestTagsByTag(questTag.getTag(), em));
            }

            for (QuestTags tag : tags) {
                if (!tag.getQuestId().equals(questId)) {
                    final QuestEntityWithACL questACL = new QuestEntityWithACL(() -> findById(tag.getQuestId(), em), em);
                    if (For.equals(questACL.eligible(user))) {
                        Logger.info("adding quest = " + questACL.getEntity(user).getId());
                        quests.add(questACL.getEntity(user));
                    }
                }
            }

            if (user != null && quests.size() == 0) {
                return ok(Json.toJson(withSEOSlugs(QuestDTO.listToDTO(
                        getQuestsNotInProgressByCategory(user, 5, quest.getPillar(), em).getList(user)))));
            } else if (user == null && quests.size() == 0) {
                return ok(Json.toJson(withSEOSlugs(QuestDTO.listToDTO(
                        findByCategoryWithACL(quest.getPillar(), em).getList(null)))));
            } else {
                return ok(Json.toJson(withSEOSlugs(QuestDTO.listToDTO(quests))));
            }
        } else {
            return noContent();
        }
    }

    private List<QuestDTO> withSEOSlugs(final List<QuestDTO> list) {
        final String envUrl = config.getString(DeploymentEnvironments.valueOf(config.getString("application.mode")).getBaseUrlKey());
        return list.stream()
                .map(dto -> dto.withSEOSlugs(publicQuestSEOSlugs(dto, dto.user, envUrl)))
                .collect(toList());
    }

    @Transactional
    public Result getQuestRecommendation(final Integer questId) {
        if (questId == null) {
            return badRequest();
        }
        final EntityManager em = this.jpaApi.em();
        final Quests quest = em.find(Quests.class, questId);
        if (quest == null) {
            return notFound();
        } else {
            return ok(Json.toJson(QuestLiteDTO.toDTO(quest)));
        }
    }

    @Transactional
    public Result getReferredQuests(final Integer questId) {
        if (questId == null) {
            return badRequest();
        }
        final EntityManager em = this.jpaApi.em();
        final Happening happening = new HappeningDAO(em).getHappeningByQuestId(questId);
        if (happening == null) {
            return notFound();
        }
        final List<ReferredQuest> referred = new ReferredQuestDAO(em).findReferredQuestForHappening(happening);
        if (CollectionUtils.isEmpty(referred)) {
            return ok(Json.newArray());
        } else {
            return ok(Json.newArray().addAll(referred.stream().map(Json::toJson).collect(toList())));
        }
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result followQuest(final Integer questId, final Boolean value) {
        checkNotNull(questId, "questId");
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        final EntityManager em = this.jpaApi.em();

        final QuestUserFlagDAO questUserFlagDAO = new QuestUserFlagDAO(em);
        if (isTrue(value)) {
            if (questUserFlagDAO.isFollowedQuestForUser(questId, user.getId())) {
                Logger.debug(format("Already followed Quest with ID [%s] by user '%s'", questId, user.getEmail()));

                return ok();
            }
            final Quests quest = QuestsDAO.findById(questId, em);
            if (quest == null) {
                return notFound();
            }
            final QuestUserFlag followFlag = questUserFlagDAO.followQuestForUser(quest, user);
            if (followFlag.flagValue) {
                Logger.info(format("Successfully followed Quest with ID [%s] by user '%s'", quest.getId(), user.getEmail()));

                notificationService.addQuestNotification(
                        findById(questId, em).getUser().getId(),
                        FOLLOWED,
                        user.getId(),
                        questId
                );
            } else {
                Logger.warn(format("Failed to follow Quest with ID [%s] by user '%s'", quest.getId(), user.getEmail()));
            }
        } else {
            final Quests quest = QuestsDAO.findById(questId, em);
            if (quest == null) {
                return notFound();
            }
            final QuestUserFlag followFlag = questUserFlagDAO.unFollowQuestForUser(quest, user);
            if (followFlag.flagValue) {
                Logger.warn(format("Failed to un-follow Quest with ID [%s] by user '%s'", quest.getId(), user.getEmail()));
            } else {
                Logger.info(format("Successfully un-followed Quest with ID [%s] by user '%s'", quest.getId(), user.getEmail()));

                notificationService.clearCache(user.getId());
            }
        }
        return ok();
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result isFollowedQuest(final Integer questId) {
        checkNotNull(questId, "questId");
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        final EntityManager em = this.jpaApi.em();
        return ok(BooleanNode.valueOf(new QuestUserFlagDAO(em).isFollowedQuestForUser(questId, user.getId())));
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result unSaveQuest(final @NotNull Integer questId) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        final EntityManager em = this.jpaApi.em();
        if (!QuestSavedDAO.doesQuestSavedExistForUser(questId, user.getId(), em)) {
            return notFound();
        }
        QuestEventHistoryDAO.addEventHistory(questId, user.getId(), QUEST_UN_SAVE, questId, em);
        QuestSavedDAO.removeQuestForUser(questId, user.getId(), em);
        return ok();
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result cancelQuest(final @NotNull Integer questId) {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return unauthorized();
        }
        final EntityManager em = this.jpaApi.em();
        final Quests quest = QuestsDAO.findById(questId, em);
        if (quest == null) {
            return notFound(format("Cancel failed : Quest with ID [%s] not found", questId));
        }

        try (Connection cRo = dbRo.getConnection()) {
            if (questService.cancelQuest(cRo, quest, user)) {
                return ok();
            } else {
                return notFound(format("Cancel failed : No active Quests found for user '%s' and Quest with ID [%s]", user.getEmail(), quest.getId()));
            }
        } catch (SQLException e) {
            Logger.error("cancelQuest - unable to cancel quest", e);
            return internalServerError();
        }
    }

    @Transactional
    @JwtSessionLogin
    public Result getRecentFollowingQuestsActivity(final Integer userId) {
        checkNotNull(userId, "userId");
        final EntityManager entityManager = this.jpaApi.em();
        final User user = UserHome.findById(userId, entityManager);
        if (user == null) {
            return notFound();
        }
        final List<Integer> questIds = new QuestUserFlagDAO(entityManager).getQuestsBeingFollowedForUser(user);
        if (questIds.size() == 0) {
            return noContent();
        } else {
            final String envUrl = config.getString(DeploymentEnvironments.valueOf(config.getString("application.mode")).getBaseUrlKey());
            final List<QuestDTO> questDTOS = questIds.stream()
                    .map(id -> QuestDTO.toDTO(findById(id, entityManager)))
                    .map(dto -> dto.withSEOSlugs(publicQuestSEOSlugs(dto, dto.user, envUrl)))
                    .collect(toList());
            return ok(Json.toJson(questDTOS));
        }
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result sendMessageAllQuestDoers(final Integer questId) {
        final EntityManager entityManager = this.jpaApi.em();

        final Form<MessageAllForm> formData = formFactory.form(MessageAllForm.class).bindFromRequest(request());
        if (formData.hasErrors()) {
            return badRequest(formData.errorsAsJson());
        }
        final MessageAllForm form = formData.get();

        final User user = this.userProvider.getUser(session());
        final AtomicBoolean isAllowed = new AtomicBoolean(false);

        checkNotNull(questId, "questId");
        checkNotNull(user, "user");

        if (!user.isUserBrand()) {
            return forbidden();
        }
        final List<QuestActivity> doers = getUsersDoingQuest(questId, form.getGroup(),entityManager);
        final List<String> doersEmails = doers
                .stream()
                .map(doer -> UserHome.findById(doer.getUserId(), entityManager)
                        .getEmail())
                .collect(toList());

        final List<String> backersEmails = new QuestBackingDAO(entityManager).getBackersEmails(questId);
        final Set<String> destinationEmails = new LinkedHashSet<>();
        destinationEmails.addAll(doersEmails);
        destinationEmails.addAll(backersEmails);

        final Quests quest = findById(questId, entityManager);
        if (quest.getUser().getId().equals(user.getId())) {
            isAllowed.set(true);
        } else if (!isEmpty(quest.getAdmins())) {
            quest.getAdmins().forEach(admin -> {
                if (admin.getId().equals(user.getId())) {
                    isAllowed.set(true);
                }
            });
        }
        if (isAllowed.get()) {
            if (isEmpty(doersEmails)) {
                Logger.warn(format("Emails list is empty for broadcast message to all doers of Quest with ID %s", quest.getId()));

                return noContent();
            } else {
                emailService.sendMessageAllDoersEmail(request(), destinationEmails, quest, form.getSubject(), form.getMessage(), true, quest.getUser(), user);

                Logger.info(format(
                        "Sent broadcast email to all doers of Quest with ID %s [%s] using data: %s",
                        quest.getId(),
                        String.join("," , doersEmails),
                        form.toString()
                ));

                return ok();
            }
        } else {
            Logger.warn(format("User with ID %s is not authorized to send broadcast message to all doers of Quest with ID %s", user.getId(), quest.getId()));

            return unauthorized();
        }
    }

    @Transactional(readOnly = true)
    @JwtSessionLogin(required = true)
    public Result downloadActivityReport(final Integer questId, final String start, final String end) {
        final User user = this.userProvider.getUser(session());
        if (user == null || !user.isUserBrand()) {
            return forbidden();
        }
        final EntityManager em = jpaApi.em();
        final Quests quest = em.find(Quests.class, questId);
        if (quest == null) {
            return badRequest();
        }
        final byte[] data = reportingService.createQuestActivityReport(quest, LocalDate.parse(start), LocalDate.parse(end));
        final String questTitleSlug = new Slugify().slugify(quest.getTitle());
        return ok(data)
                .as("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .withHeader(CONTENT_DISPOSITION, format("attachment; filename=\"%s__%s__%s.xlsx\"", questTitleSlug, start, end));
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result setQuestGeolocation(final Integer questId) {

        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return forbidden();
        }
        final EntityManager em = jpaApi.em();
        final Quests quest = em.find(Quests.class, questId);

        Form<QuestActionPointForm> formData = formFactory.form(QuestActionPointForm.class).bindFromRequest(request());
        if (formData.hasErrors()) {
            return badRequest(formData.errorsAsJson());
        }

        final QuestActionPointForm form = formData.get();

        if (!canEditQuest(quest, user)) {
            return forbidden();
        }

        ReverseGeocodingDTO reverseGeocodingDTO = reverseGeocodingService.getQuestCityByGeopoint(form, quest);
        if (reverseGeocodingDTO == null) {
            return badRequest("Cannot find place with given coordinates");
        }
        return ok(Json.toJson(reverseGeocodingDTO));
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result generateExploreCategoriesFromExistingQuestCategories() {
        final User user = this.userProvider.getUser(session());
        if (user == null) {
            return forbidden();
        }
        final EntityManager em = jpaApi.em();

        List<ExploreCategories> exploreCategory = ExploreCategoriesDAO.addExploreCategoriesOfExistQuests(em);

        if (exploreCategory.isEmpty()) {
            return badRequest("Cannot find categories");
        }

        return ok(Json.toJson(exploreCategory));
    }

    @Transactional
    @JwtSessionLogin(required = true)
    public Result renameQuest(final Integer questId) {
        final Form<QuestRenameForm> form = formFactory.form(QuestRenameForm.class).bindFromRequest(request());
        if (form.hasErrors()) return badRequest(form.errorsAsJson());

        String questName = form.get().getQuestName();
        Logger.info("Renaming quest {} to {}", questId, questName);
        final User user = this.userProvider.getUser(session());

        if (user == null) return forbidden();

        try {
            this.questService.renameQuest(questId, questName, user);
            return ok();
        }
        catch(QuestOperationForbiddenException e) {
            return forbidden();
        }
        catch(NoSuchElementException e) {
            return notFound();
        }
    }
    
    @Transactional
    @JwtSessionLogin(required = true)
    public Result logActivity(Integer questId) {
    
    	final Form<LogActivityForm> formBinding = formFactory.form(LogActivityForm.class).bindFromRequest();
        if (formBinding.hasErrors()) {
            return badRequest(formBinding.errorsAsJson());
        }

        final LogActivityForm form = formBinding.get();
        
    	final User user = this.userProvider.getUser(session());
         if (user == null) {
            return unauthorized();
         }
        final EntityManager em = this.jpaApi.em();
         
		  Map<String, Boolean> result = new HashMap<>();
         result.put("Success", true);
         String imageURL=null;
         try {
        	 questService.addLogActivity(questId, user.getId(), form, form.getImage());
         }catch(Exception ex) {
        	 result.put("Success", false);
        	 ex.printStackTrace();
         }
         	
         return ok(Json.toJson(result));
    }

    @Transactional
    @JwtSessionLogin(required = false)
    public Result getLogActivity(Integer questId,Integer pageNumber,Integer pageSize) {
    	
    	return ok(Json.toJson(questService.getLogActivity(questId,pageNumber,pageSize)));
    	
    }
	    @Transactional
	    @JwtSessionLogin(required = false)
	    public Result getInputJson() {
	    	
	    	LogActivityForm form = new LogActivityForm();
	    	return ok(Json.toJson(form));
	    }
	    
	    
	    
	    private URL saveDataUrlImage(final String dataUrl) throws IOException {
	        final String contentType = removeEnd(removeStart(substringBefore(dataUrl, ","), "data:"), ";base64");
	        final String extension = substringAfterLast(contentType, "/");
	        final byte[] imageData = Base64.getDecoder().decode(substringAfter(dataUrl, ","));
	        try (final ByteArrayInputStream imageStream = new ByteArrayInputStream(imageData);
	             final ByteArrayInputStream dataStream = new ByteArrayInputStream(imageData)) {
	            final BufferedImage bufferedImage = ImageIO.read(imageStream);
	            final S3File s3File = new S3File();

	            s3File.setName("__LogActivity." + extension);
	            s3File.setContentData(dataStream);
	            s3File.setContentLength(imageData.length);
	            s3File.setImgWidth(bufferedImage.getWidth());
	            s3File.setImgHeight(bufferedImage.getHeight());
	            s3File.setContentType(contentType);

	            final S3FileHome s3FileHome = new S3FileHome(config);

	            s3FileHome.saveUserMedia(singletonList(s3File), jpaApi.em());

	            return s3FileHome.getUrl(s3File, true);
	        }
	    }
	    
	    
	    @Transactional
	    @JwtSessionLogin(required = true)
	    public Result addComments(Integer questId) {
	    
	    	final Form<CommentsForm> formBinding = formFactory.form(CommentsForm.class).bindFromRequest();
	        if (formBinding.hasErrors()) {
	            return badRequest(formBinding.errorsAsJson());
	        }

	        final CommentsForm form = formBinding.get();
	        
	    	final User user = this.userProvider.getUser(session());
	         if (user == null) {
	            return unauthorized();
	         }
	    
	         final EntityManager em = this.jpaApi.em();
	        
	         List<AsCommentsDTO> asComments = new ArrayList<>();
	         
	         try {
	        	asComments =  questService.addComments(questId, user.getId(), form);
	    
	         }catch(Exception ex) {
	        	 ex.printStackTrace();
	         }
	         	
	         return ok(Json.toJson(asComments));
	    }

	    @Transactional
	    @JwtSessionLogin(required = false)
	    public Result addLikes(Integer questId, Integer actvityRecordListValueId) {
	    
	    	final User user = this.userProvider.getUser(session());
	         if (user == null) {
	            return unauthorized();
	         }
	    
	        
	         final EntityManager em = this.jpaApi.em();
	         
	         AsLikesDTO asLikesDTO = new AsLikesDTO();

	         try {
	        	 asLikesDTO = questService.addLikes(questId, user.getId(), actvityRecordListValueId);
	    
	         }catch(Exception ex) {
	        	 ex.printStackTrace();
	         }
	         	
	         return ok(Json.toJson(asLikesDTO));
	    }

	    
	    @Transactional
	    @JwtSessionLogin(required = true)
	    public Result exportectivites(final @NotNull Integer questId) {
	    
	    	 final User user = userProvider.getUser(session());
	         if (user == null) {
	             return unauthorized();
	         }
	    	
	    	List<ActivityExportDTO> actvitiesExport = questService.getActivityExportData(questId);
	    	Quests quests = questService.getQuestById(questId);
	    	String questTitle = quests.getTitle()==null ? "_" :"_"+quests.getTitle()+"_";
	    	questTitle = questTitle + LocalDateTime.now(ZoneId.of("US/Eastern"));
	    	 
	    	String fileName = "DIEMlife_activity"+questTitle+".csv";
	    	 final Messages messages = messagesApi.preferred(request());
	         try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
	             writeCsvToStream(actvitiesExport, output, UTF_8, messages::at);

	             return ok(new String(output.toByteArray(), UTF_8), UTF_8.name())
	                     .as("text/csv")
	                     .withHeader(CONTENT_DISPOSITION, "attachment; filename="+fileName);
	         } catch (final IOException e) {
	             Logger.error(e.getMessage(), e);

	             return internalServerError();
	         }
	    }
	    
	    @Transactional
	    @JwtSessionLogin(required = true)
	    public Result editActivityFeed(final @NotNull Integer activityRecordValueId) {
	    
	    	Boolean updated = false; 
	    	final Form<LogActivityForm> formBinding = formFactory.form(LogActivityForm.class).bindFromRequest();
	    	
	    	final LogActivityForm form = formBinding.get(); 
	    	
	     final User user = userProvider.getUser(session());
	         if (user == null) {
	             return unauthorized();
	         }

	         try {
	         	         
	         updated = questService.editLogAcivity(activityRecordValueId, form.getImage(),form,user);
	         
	    }catch(Exception ex) {
	    	
	    }
	         Map<String, Boolean> result = new HashMap<>();
	         result.put("Success", updated);
	         
	         return ok(Json.toJson(result));
	    }  
	    
	    @Transactional
	    @JwtSessionLogin(required = true)
	    public Result deleteActivityFeed(final @NotNull Integer activityRecordValueId) {
	    
	    	 Boolean updated = false;
	    	 
	    	 final User user = userProvider.getUser(session());
	         if (user == null) {
	             return unauthorized();
	         }
	       
	    try {
	    	 updated = questService.deleteLogAcivity(activityRecordValueId,user);
	         
	    }catch(Exception ex) {
	    	updated = false;
	    }
	         Map<String, Boolean> result = new HashMap<>();
	         result.put("Success", updated);
	         
	         return ok(Json.toJson(result));
	    }   

	    @Transactional
	    @JwtSessionLogin(required = false)
	    public Result maxActivity(final @NotNull Integer questId) {
	    
	    	List<LeaderboardMaxActivityDTO> leaderboardMaxActivityDTO = new ArrayList<>();
	    try {
	    	 leaderboardMaxActivityDTO = questService.leaderboardMaxActivity(questId);
	         
	    }catch(Exception ex) {
	    	ex.printStackTrace();
	    }
	         return ok(Json.toJson(leaderboardMaxActivityDTO));
	    } 
	    

}
