package com.diemlife.dto;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;

import com.diemlife.models.ActivityRecord;
import com.diemlife.models.QuestTasks;
import com.fasterxml.jackson.annotation.JsonGetter;

import forms.QuestActionPointForm;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import play.Logger;

@Getter
@Setter
@NoArgsConstructor
public class MilestoneDTO extends QuestTasks implements Serializable {

    private QuestActionPointForm geoPoint;
    private URL geoMarker;
    private URL completedGeoMarker;
    private MilestoneVideoDTO embeddedVideo;
    private MilestoneCompletionDTO lastCompletion;
    private QuestLiteDTO linkedQuest;
    private LinkPreviewDTO linkPreview;
    private List<ActivityRecord> activityRecords;
    private List<AsActivityDTO> activities;

    public static MilestoneDTO toDto(final QuestTasks task) {
        final MilestoneDTO dto = new MilestoneDTO();
        dto.setId(task.getId());
        dto.setQuestId(task.getQuestId());
        dto.setUserId(task.getUserId());
        dto.setLinkedQuestId(task.getLinkedQuestId());
        dto.setTask(task.getTask());
        dto.setTaskCompleted(task.getTaskCompleted());
        dto.setTaskCompletionDate(task.getTaskCompletionDate());
        dto.setCreatedDate(task.getCreatedDate());
        dto.setCreatedBy(task.getCreatedBy());
        dto.setLastModifiedDate(task.getLastModifiedDate());
        dto.setLastModifiedBy(task.getLastModifiedBy());
        dto.setOrder(task.getOrder());
        dto.setRadiusInKm(task.getRadiusInKm());
        dto.setImageUrl(task.getImageUrl());
        dto.setLinkUrl(task.getLinkUrl());
        dto.setQuestTasksGroup(task.getQuestTasksGroup());
        dto.setActivityRecordListId(task.getActivityRecordListId());
        dto.setTitle(task.getTitle());
        
        if (task.getVideo() != null) {
            dto.setEmbeddedVideo(MilestoneVideoDTO.toDto(task.getVideo()));
        }

        if (task.getPoint() != null) {
            dto.setGeoPoint(new QuestActionPointForm(
                    Double.valueOf(task.getPoint().getCoordinate().x).floatValue(),
                    Double.valueOf(task.getPoint().getCoordinate().y).floatValue()
            ));
            if (isNotBlank(task.getPinUrl())) {
                try {
                    dto.setGeoMarker(URI.create(task.getPinUrl()).toURL());
                } catch (final MalformedURLException e) {
                    Logger.warn("Can't add custom pin point icon to milestone: " + e.getMessage());

                    dto.setGeoMarker(null);
                }
            }
            if (isNotBlank(task.getPinCompletedUrl())) {
                try {
                    dto.setCompletedGeoMarker(URI.create(task.getPinCompletedUrl()).toURL());
                } catch (final MalformedURLException e) {
                    Logger.warn("Can't add custom completed task pin point icon to milestone: " + e.getMessage());

                    dto.setCompletedGeoMarker(null);
                }
            }
        }

        return dto;
    }

    public MilestoneDTO withLastCompletion(final MilestoneCompletionDTO lastCompletion) {
        this.lastCompletion = lastCompletion;
        return this;
    }

    public MilestoneDTO withLinkedQuest(final QuestLiteDTO linkedQuest) {
        this.linkedQuest = linkedQuest;
        return this;
    }

    public MilestoneDTO withLinkPreview(final LinkPreviewDTO linkPreview) {
        this.linkPreview = linkPreview;
        return this;
    }

    @JsonGetter("isTaskCompleted")
    public Boolean getIsTaskCompleted() {
        return Boolean.TRUE.toString().equalsIgnoreCase(this.getTaskCompleted());
    }

}
