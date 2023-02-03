package com.diemlife.dao;

import com.diemlife.acl.QuestsListWithACL;
import com.diemlife.constants.Interests;
import com.diemlife.constants.PrivacyLevel;
import com.diemlife.constants.QuestCreatorTypes;
import com.diemlife.constants.QuestMode;
import com.diemlife.dto.AllPillarsCount;
import com.diemlife.dto.AsAttributeDTO;
import com.diemlife.dto.AsCommentsDTO;
import com.diemlife.dto.AsLikesDTO;
import com.diemlife.dto.AsUnitDTO;
import com.diemlife.dto.LeaderboardMaxActivityDTO;
import com.diemlife.dto.LogActivityDTO;
import com.diemlife.dto.UserSummaryDTO;
import com.diemlife.exceptions.RequiredParameterMissingException;
import forms.CommentsForm;
import forms.LogActivityForm;
import com.diemlife.models.AsActivity;
import com.diemlife.models.AsActivityRecordValue;
import com.diemlife.models.AsAttributeUnit;
import com.diemlife.models.AsComments;
import com.diemlife.models.AsLikes;
import com.diemlife.models.AsPillar;
import com.diemlife.models.AsUserTags;
import com.diemlife.models.QuestRecordValue;
import com.diemlife.models.Quests;
import com.diemlife.models.Quests2;
import com.diemlife.models.User;
import org.hibernate.search.exception.EmptyQueryException;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.util.StopWatch;
import play.Logger;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.diemlife.acl.QuestsListWithACL.emptyListWithACL;
import static com.diemlife.constants.PrivacyLevel.PRIVATE;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.springframework.util.StringUtils.hasText;

/**
 * Created by andrewcoleman on 3/6/16.
 */
public class QuestsDAO {

    public static List<Quests> all(EntityManager em) {
        try {
            em.setFlushMode(FlushModeType.AUTO);
            Query query = em.createQuery("SELECT q FROM Quests q ORDER BY q.dateModified DESC");
            query.setHint("org.hibernate.cacheable", true);
            query.setHint("org.hibernate.readOnly", true);

            return (ArrayList<Quests>) query.getResultList();
        } catch (Exception ex) {
            ex.printStackTrace();
            return emptyList();
        }

    }

    public static List<Quests> findAllPublic(EntityManager entityManager) {
        Query query = entityManager.createQuery("SELECT q FROM Quests q WHERE q.privacyLevel != :privacyLevel", Quests.class);
        query.setParameter("privacyLevel", PRIVATE);
        return query.getResultList();
    }

    public static QuestsListWithACL getAllQuestsWithACL(EntityManager em) {
        try {
            Query query = em.createQuery("SELECT q FROM Quests q ORDER BY q.dateModified DESC");
            query.setHint("org.hibernate.cacheable", true);
            query.setHint("org.hibernate.readOnly", true);

            List<Quests> quests = query.getResultList();

            return new QuestsListWithACL(() -> quests, em);
        } catch (final PersistenceException e) {
            Logger.error("QuestsDAO :: getAllQuestsWithACL : error getting quests => " + e, e);
            return emptyListWithACL();
        }
    }

    public static QuestsListWithACL findQuestsWithAclByIds(final Collection<Integer> ids, final EntityManager em) {
        if (ids == null || ids.isEmpty()) {
            return emptyListWithACL();
        }

        final StopWatch timer = new StopWatch(format("Loading %s Quests by IDs", ids.size()));
        try {
            timer.start();

            final TypedQuery<Quests> query = em.createQuery("SELECT q FROM Quests q WHERE q.id IN :ids ", Quests.class)
                    .setParameter("ids", ids);
            return new QuestsListWithACL(query::getResultList, em);
        } catch (final PersistenceException e) {
            Logger.error("Error finding in progress quests ::  Exception => " + e, e);
            return emptyListWithACL();
        } finally {
            timer.stop();

            Logger.info(timer.shortSummary());
        }
    }

    public static QuestsListWithACL getQuestsBySearchCriteria(String value, EntityManager entityManager) {

        try {
            FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(entityManager);

            QueryBuilder queryBuilder = fullTextEntityManager.getSearchFactory()
                    .buildQueryBuilder()
                    .forEntity(Quests.class)
                    .get();

            org.apache.lucene.search.Query query = queryBuilder.keyword()
                    .onField("questFeed")
                    .andField("title")
                    .matching(value)
                    .createQuery();

            FullTextQuery jpaQuery = fullTextEntityManager.createFullTextQuery(query, Quests.class);
            List<Quests> quests = jpaQuery.getResultList();
            Logger.info("Quests Search = " + quests.size());

            return new QuestsListWithACL(() -> quests, entityManager);
        } catch (EmptyQueryException e) {
            Logger.warn("QuestsDAO :: getQuestsBySearchCriteria : stop word used in query => " + e);
            return emptyListWithACL();
        }
    }

    public static List<Quests> createdBy(final User user, final EntityManager em) {
        if (user == null) {
            throw new RequiredParameterMissingException("user");
        }
        return em.createQuery("SELECT q FROM Quests q WHERE q.createdBy = :userId ORDER BY q.dateCreated DESC", Quests.class)
                .setParameter("userId", user.getId())
                .getResultList();
    }

    public static Quests findQuestForDoer(final Integer questId, final User doer, final EntityManager em) {
        if (questId == null) {
            throw new RequiredParameterMissingException("questId");
        }
        if (doer == null) {
            throw new RequiredParameterMissingException("doer");
        }
        return em.createQuery("SELECT q " +
                "FROM Quests q " +
                "INNER JOIN QuestActivity qa ON qa.questId = q.id " +
                "WHERE qa.questId = :questId AND qa.userId = :doerId " +
                "ORDER BY qa.addedDate DESC", Quests.class)
                .setParameter("questId", questId)
                .setParameter("doerId", doer.getId())
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public static Quests createQuest(String questName, String questDescription, String shortDescription, Interests pillar,
                                     Integer userId, Integer originalUser, boolean copyAllowed, boolean isBackBtnDisabled, boolean editableMilestones,
                                     boolean milestoneControlsDisabled, boolean taskViewDisabled,
                                     boolean multiTeamsAllowed, Integer version, PrivacyLevel privacyLevel, QuestCreatorTypes type,
                                     boolean fundraising, String photo, QuestMode mode, EntityManager em) {
        if (questName == null
                || questDescription == null
                || userId == null
                || type == null) {
            throw new IllegalArgumentException();
        }

        final Quests newQuest = new Quests();
        final Date date = new Date();

        setQuestOwners(newQuest, originalUser, userId);

        newQuest.setQuestFeed(questDescription);
        newQuest.setShortDescription(shortDescription);
        newQuest.setModifiedBy(userId);
        newQuest.setTitle(questName);
        newQuest.setDateCreated(date);
        newQuest.setDateModified(date);
        newQuest.setStatus(1);
        newQuest.setSharedCount(0);
        newQuest.setSavedCount(0);
        newQuest.setCommentCount(0);
        //newQuest.setPillar(pillar.name());
        newQuest.setVersion(version == null ? 1 : version + 1);
        newQuest.setPrivacyLevel(privacyLevel);
        newQuest.setType(type.name().toLowerCase());
        newQuest.setFundraising(fundraising);
        newQuest.setCopyAllowed(copyAllowed);
        newQuest.setBackBtnDisabled(isBackBtnDisabled);
        newQuest.setEditableMilestones(editableMilestones);
        newQuest.setMilestoneControlsDisabled(milestoneControlsDisabled);
        newQuest.setTaskViewDisabled(taskViewDisabled);
        newQuest.setMultiTeamsEnabled(multiTeamsAllowed);
        newQuest.setWeight(0.0f);
        newQuest.setViews(1);
        if (hasText(photo)) {
            newQuest.setPhoto(photo);
        }
        newQuest.setMode(mode);

        try {
            em.persist(newQuest);
            em.flush();

            return newQuest;
        } catch (final PersistenceException e) {
            Logger.error(format("Error creating Quest with title '%s' for user with ID [%s]", questName, userId), e);

            throw e;
        }
    }

    public static Integer addQuestPhotoByQuestId(Integer questId, Integer userId, String photoURL, EntityManager em) {
        Date date = new Date();

        try {
            if (questId != null && userId != null && photoURL != null) {
                Quests quest = QuestsDAO.findById(questId, em);

                quest.setPhoto(photoURL);
                quest.setDateModified(date);

                em.merge(quest);

                return quest.getId();
            }
        } catch (Exception ex) {
            Logger.info("QuestsDAO :: addQuestPhotoByQuestId : error adding photo for quest => " + ex, ex);
            return questId;
        }

        return questId;
    }

    private static void setQuestOwners(Quests newQuest, Integer originalUserId, Integer newUserId) {
        if (originalUserId != null) {
            newQuest.setCreatedBy(newUserId);
            newQuest.setOrigCreatedBy(originalUserId);
        } else {
            newQuest.setCreatedBy(newUserId);
            newQuest.setOrigCreatedBy(newUserId);
        }
    }

    public static void update(Quests quest, EntityManager em) {
        try {
            if (quest != null) {
                em.merge(quest);
                Logger.info("Commit Successful for updating quest");
            }
        } catch (RuntimeException re) {
            Logger.error("Persist Failed => " + re, re);
        } catch (Exception ex) {
            Logger.error("Error merging/updating existing Quest.");
        }
    }

    public static Quests findById(Integer id, EntityManager em) {
        if (id == null) {
            return null;
        }
        return em.find(Quests.class, id);
    }

    public static Quests2 getQuest(Connection c, long questId) {
        try (PreparedStatement ps = c.prepareStatement("select title from quest_feed where id = ?")) {
			ps.setLong(1, questId);
			
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
                    String title = rs.getString(1);

                    // only expect 1 result (or none)
					return new Quests2(questId, title);
				}
			}
		} catch (Exception e) {
			Logger.error("getQuest - error", e);
		}

		return null;
    }

    public static Quests findByIdPublicQuests(Integer id, EntityManager em) {
        try {
            Query query = em.createQuery("SELECT q FROM Quests q WHERE q.id = :id " +
                    "AND q.privacyLevel != :privacy");
            query.setParameter("id", id);
            query.setParameter("privacy", PRIVATE);
            return (Quests) query.getSingleResult();
        } catch (NoResultException nre) {
            Logger.info("QuestsDAO :: findByIdPublicQuests : quest not found for Id => " + id);
            return null;
        } catch (Exception ex) {
            Logger.info("QuestsDAO :: findByIdPublicQuests : quest not found for Id => " + ex, ex);
            return null;
        }
    }

    public static List<Quests> findByCategory(String category, User user, EntityManager em) {
        try {
            Query query = em.createNamedQuery("findByCategory", Quests.class);
            query.setParameter("categoryCodeIn", category);
            List<Quests> quests = query.getResultList();

            List<Quests> questsPendingForUser = QuestActivityHome.getInProgressQuestsForUser(user, em, null, null).getList(user);

            //Removing all duplicate quests so we do not show user the quests they are currently doing
            quests.removeAll(new HashSet<>(questsPendingForUser));

            return quests;
        } catch (Exception e) {
            Logger.error("QuestsServices :: findByCategory : error finding quests by category => " + e, e);
            return null;
        }
    }

    public static QuestsListWithACL findByCategoryWithACL(final String category, final EntityManager entityManager) {
        try {
            Query query = entityManager.createQuery("SELECT q FROM Quests q WHERE q.pillar = :category ORDER BY q.dateCreated DESC");
            query.setParameter("category", category);
            List<Quests> quests = query.getResultList();
            return new QuestsListWithACL(() -> quests, entityManager);
        } catch (NoResultException e) {
            Logger.warn(format("no quests found for category [%s]", category));
            return emptyListWithACL();
        } catch (PersistenceException e) {
            Logger.error(format("error finding quests by category [%s]", category));
            throw new PersistenceException(e);
        }
    }

    public static void incrementShareCountByQuestId(Integer questId, EntityManager em) {

        try {
            Quests quest = findById(questId, em);

            if (quest != null) {
                quest.setSharedCount(quest.getSharedCount() + 1);
            }
        } catch (Exception ex) {
            Logger.error("QuestsDAO :: incrementShareCountByQuestId : error incrementing shared count for quest => " + ex, ex);
        }
    }

    public static void incrementViewCountByQuestId(final Quests quest, EntityManager entityManager) {
        try {
            if (quest != null) {
                quest.setViews(quest.getViews() + 1);
                entityManager.merge(quest);
            }
        } catch (PersistenceException e) {
            Logger.error(format("QuestsDAO :: incrementShareCountByQuestId : error incrementing shared count for quest: [%s]", e));
            throw new PersistenceException(e);
        }
    }

    public static QuestsListWithACL findAllQuestsByIdsWithACL(final List<Integer> questIds, EntityManager em) {
        List<Quests> quests = new ArrayList<>();
        questIds.forEach(id -> {
            quests.add(findById(id, em));
        });
        return new QuestsListWithACL(() -> quests, em);
    }
    
    public static Integer  addlogActivity(Integer questId, Integer userId, LogActivityForm form, String imageURL, EntityManager em) {
        Date date = new Date();
        
        AsActivityRecordValue asActivityRecordValue = new AsActivityRecordValue();
        
        asActivityRecordValue.setActitvityId(form.getActivityId());
        //asActivityRecordValue.setActvityRecordId(form.get);
        asActivityRecordValue.setAttributeId(form.getAttributeId());
        asActivityRecordValue.setComment(form.getComment());
        asActivityRecordValue.setCreatedBy(userId);
        asActivityRecordValue.setCreatedDate(date);
        //asActivityRecordValue.setGeoPoint(geoPoint);
        asActivityRecordValue.setImageURL(imageURL);
        //asActivityRecordValue.setModifiedBy(userId);
        //asActivityRecordValue.setModifiedDate(date);
        asActivityRecordValue.setPillarId(form.getPillarId());
        asActivityRecordValue.setTitle(form.getTitle());
        asActivityRecordValue.setUnitId(form.getUnitId());
        asActivityRecordValue.setAttributeValue(form.getAttributeValue());
        //asActivityRecordValue.setValueNumeric(valueNumeric);
        //asActivityRecordValue.setValueString(valueString);
        asActivityRecordValue.setDeleted(false);
        em.persist(asActivityRecordValue);
        
        QuestRecordValue questRecordValue = new QuestRecordValue(); 
        questRecordValue.setQuestId(questId);
        questRecordValue.setActvityRecordValueId(asActivityRecordValue.getId());
        questRecordValue.setPillarId(form.getPillarId());
        em.merge(questRecordValue);
        
      return  asActivityRecordValue.getId();
       
    }
    /*
     * 
     */
    public static void addtags(Integer actvityRecordValueId,List<String> tags, EntityManager em) {
    	
    	for (String tag : tags) {
    		AsUserTags asUserTags = new AsUserTags();
    		asUserTags.setTag(tag);
    		asUserTags.setActvityRecordValueId(actvityRecordValueId);
    		em.persist(asUserTags);
		}
    	
    }
    
    
    public static List<LogActivityDTO> getlogActivity(Integer questId,Integer pageNumber,Integer pageSize, EntityManager em) {
    	
    	List<Object[]> results = new ArrayList<>();
    	if(pageSize!=null && pageSize>0) {
    		results = em.createQuery("SELECT asArv.id, asArv.imageURL,asArv.comment,asArv.title,asArv.attributeValue,asArv.attributeId,asArv.actitvityId,asArv.unitId, asArv.createdBy,asArv.pillarId, asArv.createdDate, asArv.modifiedBy from AsActivityRecordValue asArv"
    			+" where asArv.id IN(SELECT actvityRecordValueId from  QuestRecordValue where questId=:questId) AND asArv.deleted=false ORDER BY asArv.createdDate desc"  )
    		      .setParameter("questId", questId)
    		      .setFirstResult((pageNumber-1) * pageSize)
    		      .setMaxResults(pageSize)
    		      .getResultList();
    	}else {
    	
    		results = em.createQuery("SELECT asArv.id, asArv.imageURL,asArv.comment,asArv.title,asArv.attributeValue,asArv.attributeId,asArv.actitvityId,asArv.unitId, asArv.createdBy,asArv.pillarId, asArv.createdDate, asArv.modifiedBy from AsActivityRecordValue asArv"
    			+" where asArv.id IN(SELECT actvityRecordValueId from  QuestRecordValue where questId=:questId) AND asArv.deleted=false ORDER BY asArv.createdDate desc"  )
    		      .setParameter("questId", questId)
    		      .getResultList();
    	}
    	
    	List<LogActivityDTO> logActivitesDATO = new ArrayList();
    	LogActivityDTO logActivityDTO = new LogActivityDTO();
    	for (Object[] row : results) {
    		logActivityDTO = new LogActivityDTO();
    		
    		logActivityDTO.setActvityRecordValueId((Integer)row[0]);
    		logActivityDTO.setImageURL((String)row[1]);
    		logActivityDTO.setComment((String)row[2]);
    		logActivityDTO.setTitle((String)row[3]);
    		logActivityDTO.setAttributeValue((String)row[4]);
    		if((Integer)row[5]!=null)
    		logActivityDTO.setAttributeName(getAttributeNameById((Integer)row[5],em));
    		
    		if((Integer)row[6]!=null)
    		logActivityDTO.setActivityName(getActivityNameById((Integer)row[6],em));
    		
    		if((Integer)row[7]!=null) {
	    		AsAttributeUnit asAttributeUnit = getAttributeUnitById((Integer)row[7], em); 
	    		
	    		logActivityDTO.setAbbreviation(asAttributeUnit.getAbbreviation());
	    		logActivityDTO.setUnitNamePlural(asAttributeUnit.getUnitNamePlural());
	    		logActivityDTO.setUnitNameSingular(asAttributeUnit.getUnitNameSingular());
	    		}
    		logActivityDTO.setTags(getTagsByActivityRecordValue(logActivityDTO.getActvityRecordValueId(),em));
    		
    		logActivityDTO.setUserId((Integer)row[8]);
    		
    		User user = getUserById(logActivityDTO.getUserId(),em);
    		logActivityDTO.setUserFirstName(user.getFirstName());
    		logActivityDTO.setUserLastName(user.getLastName());
    		logActivityDTO.setUserName(user.getUserName());
    		logActivityDTO.setUserImageUrl(user.getProfilePictureURL());
    		logActivityDTO.setEmail(user.getEmail());
    		
    		logActivityDTO.setPillarName((getPillarById((Integer)row[9],em)).getName());
    		
    		logActivityDTO.setCreationDateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date)row[10]));
    		logActivityDTO.setCreationDate(new SimpleDateFormat("yyyy-MM-dd").format((Date)row[10]));
    		
    		List<AsCommentsDTO> userComments = getLogActivityComments(logActivityDTO.getActvityRecordValueId(),em);
    		
    		logActivityDTO.setUserComments(userComments);
    		logActivityDTO.setLikes(getUserLikes(logActivityDTO.getActvityRecordValueId(),em));
    		
    		if((Integer)row[11]!=null) {
    			logActivityDTO.setUpdated(true);
    		}else {
    			logActivityDTO.setUpdated(false);
    		}
    		logActivitesDATO.add(logActivityDTO);
    		
    	}
    
    	return logActivitesDATO;
    	
    }
    
    
    public static List<String> getTagsByActivityRecordValue(Integer actvityRecordValueId, EntityManager em){
    	
    	List<String> results = em.createQuery("SELECT asut.tag from AsUserTags asut"
    			+" where asut.actvityRecordValueId = :actvityRecordValueId")
    		      .setParameter("actvityRecordValueId", actvityRecordValueId)
    		      .getResultList();
    	List<String> tags = new ArrayList();
    	
    	return results;
    }
    
    
public static String getAttributeNameById(Integer attributeId, EntityManager em){
    	
    	String result = (String)em.createQuery("SELECT asa.name from AsAttribute asa"
    			+" where asa.id = :attributeId")
    		      .setParameter("attributeId", attributeId)
    		      .getSingleResult();
    	
    	return result;
    }

public static String getActivityNameById(Integer activityId, EntityManager em){
	
	String result = (String)em.createQuery("SELECT asact.name from AsActivity asact"
			+" where asact.id = :activityId")
		      .setParameter("activityId", activityId)
		      .getSingleResult();
	
	return result;
}

public static AsAttributeUnit getAttributeUnitById(Integer attributeUnitId, EntityManager em){
	
	AsAttributeUnit result = (AsAttributeUnit)em.createQuery("From AsAttributeUnit asau"
			+" where asau.id = :attributeUnitId")
		      .setParameter("attributeUnitId", attributeUnitId)
		      .getSingleResult();
	
	return result;
}

public static User getUserById(Integer userId,EntityManager em) {
	
	return em.find(User.class,userId);
}

public static List<AllPillarsCount> getTotalPillarCountByUserIds(List<Integer> userIds , EntityManager em){
	
	List<AllPillarsCount> allPillarsCounts = new ArrayList<>();
	
	User user = getUserById(userIds.get(0),em);
	
	List<Object[]> results = new ArrayList<>();
	
	if(!("Y".equalsIgnoreCase(user.getIsUserBrand()))){
			results = em.createQuery("SELECT count(asArv.pillarId),asArv.pillarId from AsActivityRecordValue asArv"
    			+" where asArv.createdBy IN(:userIds) AND asArv.deleted=false group by asArv.pillarId"  )
    		      .setParameter("userIds", userIds)
    		      .getResultList();
	}else {
	
	List<Integer> quests = em.createQuery("SELECT questTask.questId from QuestTasks questTask"
			+" where questTask.createdBy IN(:userIds) group by questTask.questId",Integer.class)
		      .setParameter("userIds", userIds)
		      .getResultList();
		
		if(quests!=null && quests.size()>0) {
			results = em.createQuery("SELECT count(qrval.pillarId),qrval.pillarId from QuestRecordValue qrval, AsActivityRecordValue asArv"
					+" where qrval.questId IN(:quests) AND qrval.actvityRecordValueId=asArv.id AND asArv.deleted=false group by qrval.pillarId") 
				      .setParameter("quests", quests)
				      .getResultList();
		}
	}
			
		AllPillarsCount allPillarsCount = new AllPillarsCount();
			for (Object[] row : results) {
				allPillarsCount = new AllPillarsCount();
				
				allPillarsCount.setCount((Long)row[0]);
				allPillarsCount.setId((Integer)row[1]);
				allPillarsCounts.add(allPillarsCount);
			}
	
	return allPillarsCounts;
}

public static AsPillar getPillarById(Integer pillarId,EntityManager em) {
	
	return em.find(AsPillar.class,pillarId);
}

public static List<AsCommentsDTO> addComments(Integer questId, Integer userId, CommentsForm form,EntityManager em) {
	AsComments asComments = new AsComments();
	
	asComments.setActivityRecordValueId(form.getActivityRecordValueId());
	asComments.setComments(form.getComments());
	asComments.setQuestId(questId);
	asComments.setCreatedBy(userId);
	asComments.setCreatedDate(new Date());
	asComments.setDeleted(false);
	asComments.setParentCommentsId(form.getParentCommentsId());
	
	em.persist(asComments);
	
	
	List<AsCommentsDTO> userComments = getLogActivityComments(form.getActivityRecordValueId(),em);
	
	return userComments;
}

public static AsLikesDTO addLikes(Integer questId, Integer userId, Integer activityRecordListValueId, EntityManager em) {
	
	AsLikes likes = isLikeAlreadyExist(userId,activityRecordListValueId,em);
	if(likes!=null && likes.getId()>0) {
		em.remove(likes);
	}else {
		
		AsLikes asLikes = new AsLikes();
		asLikes.setActivityRecordValueId(activityRecordListValueId);
		asLikes.setQuestId(questId);
		asLikes.setCreatedBy(userId);
		asLikes.setCreatedDate(new Date());
		em.persist(asLikes);
	}
	
	AsLikesDTO asLikesDTO = getUserLikes(activityRecordListValueId,em);
	
	return asLikesDTO;
}


public static List<AsCommentsDTO> getLogActivityComments(Integer activityRecordValueId,EntityManager em ){
	
	List<AsCommentsDTO> asCommentsDTOs = new ArrayList<>();
	
	List<AsComments> results = em.createQuery("SELECT asComments from AsComments asComments where asComments.activityRecordValueId=:activityRecordValueId AND asComments.parentCommentsId IS NULL order by asComments.createdDate desc",AsComments.class)
		      .setParameter("activityRecordValueId", activityRecordValueId)
		      .getResultList();
	
	
	if(results!=null && results.size()>0) {
		for (AsComments asComments : results) {
			AsCommentsDTO asCommentsDTO = new AsCommentsDTO();
			asCommentsDTO.setCommentId(asComments.getId());
			asCommentsDTO.setActvityRecordValueId(asComments.getActivityRecordValueId());
			asCommentsDTO.setComment(asComments.getComments());
			asCommentsDTO.setCreationDate(new SimpleDateFormat("yyyy-MM-dd").format(asComments.getCreatedDate()));
			asCommentsDTO.setCreationDateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(asComments.getCreatedDate()));
			asCommentsDTO.setUserId(asComments.getCreatedBy());
			User user = getUserById(asComments.getCreatedBy(),em);
			asCommentsDTO.setUserFirstName(user.getFirstName());
			asCommentsDTO.setUserLastName(user.getLastName());
			asCommentsDTO.setUserName(user.getUserName());
			asCommentsDTO.setUserImageUrl(user.getProfilePictureURL());
			asCommentsDTOs.add(asCommentsDTO);
		}
	}
	return asCommentsDTOs;
}

public static AsLikesDTO getUserLikes(Integer activityRecordValueId,EntityManager em){
	
	AsLikesDTO likes = new AsLikesDTO();
	List<UserSummaryDTO> userSummaryDTOs = new ArrayList<>();
	Long count = em.createQuery("SELECT count(asLikes.createdBy) from AsLikes asLikes"
			+" where asLikes.activityRecordValueId=:activityRecordValueId", Long.class  )
		      .setParameter("activityRecordValueId", activityRecordValueId)
		      .getSingleResult();
	
	List<User> results = em.createQuery("SELECT user from User user where user.id IN(SELECT asLikes.createdBy from AsLikes asLikes where asLikes.activityRecordValueId=:activityRecordValueId order by asLikes.createdDate desc)",User.class)
		      .setParameter("activityRecordValueId", activityRecordValueId)
		      .getResultList();
	
	
	if(results!=null && results.size()>0) {
		for (User user : results) {
			UserSummaryDTO userSummaryDTO = new UserSummaryDTO(); 
			userSummaryDTO.setUserId(user.getId());
			userSummaryDTO.setUserFirstName(user.getFirstName());
			userSummaryDTO.setUserLastName(user.getLastName());
			userSummaryDTO.setUserName(user.getUserName());
			userSummaryDTO.setUserImageUrl(user.getProfilePictureURL());
			userSummaryDTOs.add(userSummaryDTO);
		}
	}
	
	likes.setCount(count);
	likes.setActivityRecordValueId(activityRecordValueId);
	likes.setUsers(userSummaryDTOs);
	return likes;
}


public static AsLikes isLikeAlreadyExist(Integer userId, Integer activityRecordValueId,EntityManager em) {
	boolean alreadyExist = false;
	
	AsLikes asLikes = null;
			
		try {
			asLikes = em.createQuery("SELECT asLikes from AsLikes asLikes"
			+" where asLikes.activityRecordValueId=:activityRecordValueId AND asLikes.createdBy=:userId", AsLikes.class  )
		      .setParameter("activityRecordValueId", activityRecordValueId)
		      .setParameter("userId", userId)
		      .getSingleResult();
			}catch(Exception ex) {
				
			}
	
	return asLikes;
}


public static Quests getQuestById(Integer questId,EntityManager em) {
	
	return em.find(Quests.class,questId);
}

public static Boolean editLogActivity(Integer activityRecordValueId, String imageURL, String comment,String title,Integer userId,EntityManager em) {
	
	try {
	AsActivityRecordValue asActivityRecordValue = 	em.find(AsActivityRecordValue.class, activityRecordValueId);
		if(asActivityRecordValue!=null) {
			asActivityRecordValue.setComment(comment);
			asActivityRecordValue.setTitle(title);
			asActivityRecordValue.setImageURL(imageURL);
			asActivityRecordValue.setModifiedBy(userId);
			asActivityRecordValue.setModifiedDate(new Date());
			em.merge(asActivityRecordValue);
		
		
			return true;
		}
		
	}catch(Exception ex) {
		return false;
	}
	return false;
}

public static Boolean deleteLogActivity(Integer activityRecordValueId,Integer userId,EntityManager em) {
	
	try {
	AsActivityRecordValue asActivityRecordValue = 	em.find(AsActivityRecordValue.class, activityRecordValueId);
		if(asActivityRecordValue!=null) {

			asActivityRecordValue.setDeleted(true);
			asActivityRecordValue.setModifiedBy(userId);
			asActivityRecordValue.setModifiedDate(new Date());
			em.merge(asActivityRecordValue);
		
		
			return true;
		}
		
	}catch(Exception ex) {
		return false;
	}
	return false;
}

public static List<LeaderboardMaxActivityDTO> leaderboardMaxActivity(Integer questId,Integer pageNumber,Integer pageSize,EntityManager em) {
	
	
	String sql = "SELECT count(*) as total, createdBy, actitvityId FROM AsActivityRecordValue where id in (Select actvityRecordValueId from QuestRecordValue where questId=:questId) group by actitvityId,createdBy order by total desc";
	
	List<Object[]> results = em.createQuery(sql) 
			      .setParameter("questId", questId)
			      .setFirstResult((pageNumber-1) * pageSize)
    		      .setMaxResults(pageSize)
			      .getResultList();
	
	LeaderboardMaxActivityDTO leaderboardMaxActivityDTO = new LeaderboardMaxActivityDTO();
	List<LeaderboardMaxActivityDTO> leaderboardMaxActivityDTOList = new ArrayList<>();	
	for (Object[] row : results) {
			leaderboardMaxActivityDTO = new LeaderboardMaxActivityDTO();
			
			leaderboardMaxActivityDTO.setScore((Long)row[0]);
			Integer userId  = ((Integer)row[1]);
			User user = findUserById(userId,em);
			AsActivity activity = findActivityById( ((Integer)row[2]),em);
			
			leaderboardMaxActivityDTO.setUserName(user.getUserName());
			leaderboardMaxActivityDTO.setUserFirstName(user.getFirstName());
			leaderboardMaxActivityDTO.setUserLastName(user.getLastName());
			leaderboardMaxActivityDTO.setUserId(userId);
			leaderboardMaxActivityDTO.setUserName(user.getFirstName()+" "+user.getLastName());
			leaderboardMaxActivityDTO.setImageURL(user.getProfilePictureURL());
			leaderboardMaxActivityDTO.setActivityName(activity.getName());
			leaderboardMaxActivityDTOList.add(leaderboardMaxActivityDTO);
	}

	return leaderboardMaxActivityDTOList;
}

public static User findUserById(Integer id, EntityManager em) {
    if (id == null) {
        return null;
    }
    return em.find(User.class, id);
}

public static AsActivity findActivityById(Integer id, EntityManager em) {
    if (id == null) {
        return null;
    }
    return em.find(AsActivity.class, id);
}


public static List<LeaderboardMaxActivityDTO> filterByUserAndActivity(Integer questId,Integer pageNumber,Integer pageSize,String userName, List<Integer> activityIds, EntityManager em) {

	List<Integer> userIds = new ArrayList();
	if(userName!=null && userName.length()>0) {
		userIds = em.createQuery("SELECT id from User where firstName like '"+userName+"%'",Integer.class) 
				.getResultList();
	}
	
	String sql = "SELECT count(*) as total, createdBy, actitvityId FROM AsActivityRecordValue where id in (Select actvityRecordValueId from QuestRecordValue where questId=:questId) ";
			if(activityIds!=null && activityIds.size()>0) {
				sql = sql + " AND actitvityId In(:activityIds)";
			}
			if(userIds.size()>0) {
				sql = sql + " AND  createdBy In(:userIds)";
			}	
			sql = sql + "group by actitvityId,createdBy order by total desc";
	
	List<Object[]> results = new ArrayList<>(); 
	
		  if(userIds.size()>0 &&  activityIds!=null && activityIds.size()>0) {
			results = em.createQuery(sql) 
			      .setParameter("questId", questId)
			      .setParameter("userIds", userIds)
			      .setParameter("activityIds", activityIds)
			      .setFirstResult((pageNumber-1) * pageSize)
    		      .setMaxResults(pageSize)
			      .getResultList();
		  }else if(userIds.size()>0) {
			  results = em.createQuery(sql) 
				      .setParameter("questId", questId)
				      .setParameter("userIds", userIds)
				      .setFirstResult((pageNumber-1) * pageSize)
	    		      .setMaxResults(pageSize)
				      .getResultList();
		  }else if(activityIds!=null && activityIds.size()>0) {
			  results = em.createQuery(sql) 
				      .setParameter("questId", questId)
				      .setParameter("activityIds", activityIds)
				      .setFirstResult((pageNumber-1) * pageSize)
	    		      .setMaxResults(pageSize)
				      .getResultList();
		  }
		  else if((activityIds==null || activityIds.size()==0) && (userName==null || userName.length()==0)) {
			  results = em.createQuery(sql) 
				      .setParameter("questId", questId)
				      .setFirstResult((pageNumber-1) * pageSize)
	    		      .setMaxResults(pageSize)
				      .getResultList();
		  }
		  
	LeaderboardMaxActivityDTO leaderboardMaxActivityDTO = new LeaderboardMaxActivityDTO();
	List<LeaderboardMaxActivityDTO> leaderboardMaxActivityDTOList = new ArrayList<>();	
	for (Object[] row : results) {
			leaderboardMaxActivityDTO = new LeaderboardMaxActivityDTO();
			
			leaderboardMaxActivityDTO.setScore((Long)row[0]);
			Integer userId  = ((Integer)row[1]);
			User user = findUserById(userId,em);
			AsActivity activity = findActivityById( ((Integer)row[2]),em);
			
			leaderboardMaxActivityDTO.setUserName(user.getUserName());
			leaderboardMaxActivityDTO.setUserFirstName(user.getFirstName());
			leaderboardMaxActivityDTO.setUserLastName(user.getLastName());
			leaderboardMaxActivityDTO.setUserId(userId);
			leaderboardMaxActivityDTO.setUserName(user.getFirstName()+" "+user.getLastName());
			leaderboardMaxActivityDTO.setImageURL(user.getProfilePictureURL());
			leaderboardMaxActivityDTO.setActivityName(activity.getName());
			leaderboardMaxActivityDTOList.add(leaderboardMaxActivityDTO);
	}

	return leaderboardMaxActivityDTOList;
}

public static List<LeaderboardMaxActivityDTO> getCountOfeachActivity(Integer questId,Integer pageNumber,Integer pageSize,String userName, List<Integer> activityIds, EntityManager em) {

	List<Integer> userIds = new ArrayList();
	if(userName!=null && userName.length()>0) {
		userIds = em.createQuery("SELECT id from User where firstName like '"+userName+"%'",Integer.class) 
				.getResultList();
	}
	
	String sql = "SELECT attributeValue, createdBy, actitvityId FROM AsActivityRecordValue where id in (Select actvityRecordValueId from QuestRecordValue where questId=:questId) ";
			if(activityIds!=null && activityIds.size()>0) {
				sql = sql + " AND actitvityId In(:activityIds)";
			}
			if(userIds.size()>0) {
				sql = sql + " AND  createdBy In(:userIds)";
			}	
	
	List<Object[]> results = new ArrayList<>(); 
	
		  if(userIds.size()>0 &&  activityIds!=null && activityIds.size()>0) {
			results = em.createQuery(sql) 
			      .setParameter("questId", questId)
			      .setParameter("userIds", userIds)
			      .setParameter("activityIds", activityIds)
			      .setFirstResult((pageNumber-1) * pageSize)
    		      .setMaxResults(pageSize)
			      .getResultList();
		  }else if(userIds.size()>0) {
			  results = em.createQuery(sql) 
				      .setParameter("questId", questId)
				      .setParameter("userIds", userIds)
				      .setFirstResult((pageNumber-1) * pageSize)
	    		      .setMaxResults(pageSize)
				      .getResultList();
		  }else if(activityIds!=null && activityIds.size()>0) {
			  results = em.createQuery(sql) 
				      .setParameter("questId", questId)
				      .setParameter("activityIds", activityIds)
				      .setFirstResult((pageNumber-1) * pageSize)
	    		      .setMaxResults(pageSize)
				      .getResultList();
		  }
		  else if((activityIds==null || activityIds.size()==0) && (userName==null || userName.length()==0)) {
			  results = em.createQuery(sql) 
				      .setParameter("questId", questId)
				      .setFirstResult((pageNumber-1) * pageSize)
	    		      .setMaxResults(pageSize)
				      .getResultList();
		  }
		  
	LeaderboardMaxActivityDTO leaderboardMaxActivityDTO = new LeaderboardMaxActivityDTO();
	List<LeaderboardMaxActivityDTO> leaderboardMaxActivityDTOList = new ArrayList<>();	
	for (Object[] row : results) {
			leaderboardMaxActivityDTO = new LeaderboardMaxActivityDTO();
			
			leaderboardMaxActivityDTO.setAttributeValue((String)row[0]);
			Integer userId  = ((Integer)row[1]);
			User user = findUserById(userId,em);
			AsActivity activity = findActivityById( ((Integer)row[2]),em);
			
			leaderboardMaxActivityDTO.setUserName(user.getUserName());
			leaderboardMaxActivityDTO.setUserFirstName(user.getFirstName());
			leaderboardMaxActivityDTO.setUserLastName(user.getLastName());
			leaderboardMaxActivityDTO.setUserId(userId);
			leaderboardMaxActivityDTO.setUserName(user.getFirstName()+" "+user.getLastName());
			leaderboardMaxActivityDTO.setImageURL(user.getProfilePictureURL());
			leaderboardMaxActivityDTO.setActivityName(activity.getName());
			leaderboardMaxActivityDTOList.add(leaderboardMaxActivityDTO);
			
	}

	
	return leaderboardMaxActivityDTOList;
}

public static List<AsCommentsDTO> commentsReplies(Integer commentsId,EntityManager em ){
	
	List<AsCommentsDTO> asCommentsDTOs = new ArrayList<>();
	
	List<AsComments> results = em.createQuery("SELECT asComments from AsComments asComments where asComments.parentCommentsId=:parentCommentsId order by asComments.createdDate desc",AsComments.class)
		      .setParameter("parentCommentsId", commentsId)
		      .getResultList();
	
	
	if(results!=null && results.size()>0) {
		for (AsComments asComments : results) {
			AsCommentsDTO asCommentsDTO = new AsCommentsDTO();
			asCommentsDTO.setActvityRecordValueId(asComments.getActivityRecordValueId());
			asCommentsDTO.setComment(asComments.getComments());
			asCommentsDTO.setCreationDate(new SimpleDateFormat("yyyy-MM-dd").format(asComments.getCreatedDate()));
			asCommentsDTO.setCreationDateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(asComments.getCreatedDate()));
			asCommentsDTO.setUserId(asComments.getCreatedBy());
			User user = getUserById(asComments.getCreatedBy(),em);
			asCommentsDTO.setUserFirstName(user.getFirstName());
			asCommentsDTO.setUserLastName(user.getLastName());
			asCommentsDTO.setUserName(user.getUserName());
			asCommentsDTO.setUserImageUrl(user.getProfilePictureURL());
			asCommentsDTOs.add(asCommentsDTO);
		}
	}
	return asCommentsDTOs;
}
}
