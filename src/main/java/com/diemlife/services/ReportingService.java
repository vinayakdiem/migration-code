package com.diemlife.services;

import com.diemlife.dto.QuestActivityReportDTO;
import com.diemlife.dto.QuestActivityReportDailyDTO;
import com.diemlife.dto.QuestActivityReportDailyDTO.QuestActivityCommentDTO;
import com.diemlife.dto.QuestActivityReportDailyDTO.QuestActivityCompletionDTO;
import com.diemlife.dto.QuestActivityReportDailyDTO.QuestActivityImageDTO;
import com.diemlife.dto.QuestActivityReportDailyDTO.QuestActivityNotificationDTO;
import com.diemlife.dto.QuestActivityReportDailyDTO.QuestActivityPromptDTO;
import com.diemlife.models.Quests;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import play.Logger;
import play.db.jpa.JPAApi;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Time;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Service
public class ReportingService {

    public byte[] createQuestActivityReport(final Quests quest, final LocalDate startDate, final LocalDate endDate) {
        final Integer questId = quest.getId();
        //FIXME Vinayak
        final List usersReport = em.createNativeQuery("" +
                "SELECT " +
                "  u.id                                               AS userId, " +
                "  u.email                                            AS userEmail, " +
                "  u.user_name                                        AS userName, " +
                "  u.first_name                                       AS userFirstName, " +
                "  u.last_name                                        AS userLastName, " +
                "  GROUP_CONCAT(DISTINCT qa.status SEPARATOR ',')     AS activityStatus, " +
                "  GROUP_CONCAT(DISTINCT qa.group_name SEPARATOR ',') AS activityGroup, " +
                "  MIN(qa.added_date)                                 AS activityAddedDate, " +
                "  MAX(qa.last_modified_date)                         AS activityModifiedDate, " +
                "  SUM(qa.cycles_counter)                             AS activityCompletedCount, " +
                "  MIN(pnd.added_date)                                AS pushAddedDate, " +
                "  MAX(lm.id)                                         AS leaderboardMemberId " +
                "FROM diemlife.user AS u " +
                "  INNER JOIN diemlife.quest_activity qa ON qa.user_id = u.Id " +
                "  LEFT OUTER JOIN diemlife.push_notification_device pnd ON pnd.user_id = u.Id " +
                "  LEFT OUTER JOIN diemlife.leaderboard_member lm ON lm.platform_user_id = u.Id " +
                "WHERE qa.quest_id = :questId " +
                "  AND qa.status IN ('IN_PROGRESS', 'COMPLETE') " +
                "GROUP BY u.Id, u.email, u.user_name, u.first_name, u.last_name, qa.user_id, pnd.user_id, lm.platform_user_id " +
                "ORDER BY activityAddedDate DESC", QuestActivityReportDTO.class)
                .setParameter("questId", quest.getId())
                .getResultList();
        final List<LocalDate> period = getDatesBetween(startDate, endDate);
        final Map<Integer, QuestActivityReportDTO> userMapById = Stream.of(usersReport.toArray())
                .map(QuestActivityReportDTO.class::cast)
                .peek(report -> report.setCalendar(new LinkedHashMap<>()))
                .peek(report -> period.forEach(date -> report.getCalendar().put(date, new QuestActivityReportDailyDTO(date))))
                .collect(toMap(QuestActivityReportDTO::getUserId, o -> o));
        final Set<String> userNamesSet = Stream.of(usersReport.toArray())
                .map(QuestActivityReportDTO.class::cast)
                .map(QuestActivityReportDTO::getUserName)
                .collect(toSet());
        for (final LocalDate date : period) {
            getPushesMap(userMapById.keySet(), date).forEach((userId, notification) -> userMapById.get(userId).getCalendar().get(date).setNotification(notification));
            getCompletionMap(questId, userMapById.keySet(), date).forEach((userId, completion) -> userMapById.get(userId).getCalendar().get(date).setCompletion(completion));
            getCommentsMap(questId, userMapById.keySet(), date).forEach((userId, comments) -> userMapById.get(userId).getCalendar().get(date).setComments(comments));
            getImagesMap(questId, userMapById.keySet(), date).forEach((userId, images) -> userMapById.get(userId).getCalendar().get(date).setImages(images));
            getPromptsMap(questId, userNamesSet, date).forEach((userId, prompts) -> userMapById.get(userId).getCalendar().get(date).setPrompts(prompts));
        }

        final Workbook workbook = new XSSFWorkbook();
        final String formattedQuestName = quest.getTitle();
        final Sheet sheet = workbook.createSheet(formattedQuestName);
        final CellStyle headerStyle = workbook.createCellStyle();
        final Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setFont(headerFont);
        final CellStyle timestampCellStyle = workbook.createCellStyle();
        final CreationHelper createHelper = workbook.getCreationHelper();
        timestampCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("mm/dd/yyyy"));

        sheet.createFreezePane(1, 2);

        for (int columnIndex = 0; columnIndex < 9; columnIndex++) {
            sheet.addMergedRegion(new CellRangeAddress(0, 1, columnIndex, columnIndex));
        }
        for (int dateIndex = 0; dateIndex < period.size(); dateIndex++) {
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 9 + dateIndex * 5, 13 + dateIndex * 5));
        }

        final Row headerTop = sheet.createRow(0);
        headerTop.setRowStyle(headerStyle);
        headerTop.createCell(0, CellType.STRING).setCellValue("Email");
        headerTop.createCell(1, CellType.STRING).setCellValue("First Name");
        headerTop.createCell(2, CellType.STRING).setCellValue("Last Name");
        headerTop.createCell(3, CellType.STRING).setCellValue("Quest Status Current");
        headerTop.createCell(4, CellType.STRING).setCellValue("Quest Added Date");
        headerTop.createCell(5, CellType.STRING).setCellValue("Quest Modified Date");
        headerTop.createCell(6, CellType.STRING).setCellValue("Completed Count");
        headerTop.createCell(7, CellType.STRING).setCellValue("PND Added Date");
        headerTop.createCell(8, CellType.STRING).setCellValue("Notification Group");
        for (int dateIndex = 0; dateIndex < period.size(); dateIndex++) {
            final LocalDate date = period.get(dateIndex);
            headerTop.createCell(9 + dateIndex * 5, CellType.STRING).setCellValue(date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.US) + " " + date.toString());
        }

        final Row headerBottom = sheet.createRow(1);
        headerBottom.setRowStyle(headerStyle);
        for (int dateIndex = 0; dateIndex < period.size(); dateIndex++) {
            headerBottom.createCell(9 + dateIndex * 5, CellType.STRING).setCellValue("Notification Opened");
            headerBottom.createCell(10 + dateIndex * 5, CellType.STRING).setCellValue("Quest Completed");
            headerBottom.createCell(11 + dateIndex * 5, CellType.STRING).setCellValue("Prompt Answers");
            headerBottom.createCell(12 + dateIndex * 5, CellType.STRING).setCellValue("Photo Caption");
            headerBottom.createCell(13 + dateIndex * 5, CellType.STRING).setCellValue("Newsfeed Post");
        }

        final AtomicInteger rowsCounter = new AtomicInteger(2);
        userMapById.forEach((userId, userData) -> {
            final Row row = sheet.createRow(rowsCounter.getAndIncrement());
            row.createCell(0, CellType.STRING).setCellValue(userData.getUserEmail());
            row.createCell(1, CellType.STRING).setCellValue(userData.getUserFirstName());
            row.createCell(2, CellType.STRING).setCellValue(userData.getUserLastName());
            row.createCell(3, CellType.STRING).setCellValue(userData.getActivityStatus());
            final Cell addedDateCell = row.createCell(4, CellType.NUMERIC);
            addedDateCell.setCellStyle(timestampCellStyle);
            addedDateCell.setCellValue(userData.getActivityAddedDate());
            final Cell modifiedDateCell = row.createCell(5, CellType.NUMERIC);
            modifiedDateCell.setCellStyle(timestampCellStyle);
            modifiedDateCell.setCellValue(userData.getActivityModifiedDate());
            row.createCell(6, CellType.NUMERIC).setCellValue(userData.getActivityCompletedCount());
            final Cell pushDateCell = row.createCell(7, CellType.NUMERIC);
            pushDateCell.setCellStyle(timestampCellStyle);
            pushDateCell.setCellValue(userData.getPushAddedDate());
            row.createCell(8, CellType.STRING).setCellValue(userData.getActivityGroup());

            for (int dateIndex = 0; dateIndex < period.size(); dateIndex++) {
                final LocalDate date = period.get(dateIndex);
                final QuestActivityReportDailyDTO day = userData.getCalendar().get(date);
                row.createCell(9 + dateIndex * 5, CellType.BOOLEAN).setCellValue(day.getNotification() == null
                        ? false
                        : day.getNotification().getNotificationOpened());
                row.createCell(10 + dateIndex * 5, CellType.BOOLEAN).setCellValue(day.getCompletion() == null
                        ? false
                        : day.getCompletion().getQuestCompleted());
                row.createCell(11 + dateIndex * 5, CellType.STRING).setCellValue((day.getPrompts() == null
                        ? new ArrayList<QuestActivityPromptDTO>()
                        : day.getPrompts()).stream()
                        .map(prompt -> String.format("%s [%s] '%s' => %s", prompt.getPromptTime(), prompt.getPromptEventName(), prompt.getPromptQuestion(), prompt.getPromptAnswer()))
                        .collect(Collectors.joining("\n")));
                row.createCell(12 + dateIndex * 5, CellType.STRING).setCellValue((day.getImages() == null
                        ? new ArrayList<QuestActivityImageDTO>()
                        : day.getImages()).stream()
                        .map(QuestActivityImageDTO::getImageCaption)
                        .collect(Collectors.joining("\n")));
                row.createCell(13 + dateIndex * 5, CellType.STRING).setCellValue((day.getComments() == null
                        ? new ArrayList<QuestActivityCommentDTO>()
                        : day.getComments()).stream()
                        .map(QuestActivityCommentDTO::getNewsFeedComment)
                        .collect(Collectors.joining("\n")));
            }

            autoSizeColumns(sheet, row);
        });

        autoSizeHeader(headerStyle, sheet, headerTop);
        autoSizeHeader(headerStyle, sheet, headerBottom);

        try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.write(output);

            return output.toByteArray();
        } catch (final IOException e) {
            Logger.error("Cannot write event participants report to file", e);

            throw new IllegalStateException(e);
        }
    }

    private Map<Integer, QuestActivityNotificationDTO> getPushesMap(final Collection<Integer> userIds, final LocalDate date) {
    	//FIXME Vinayak
        return Stream.of(setQueryParameters(em.createNativeQuery("" +
                "SELECT u.Id AS userId, IF(MAX(pnmi.opened_date) BETWEEN :startDate AND :endDate, TRUE, FALSE) AS notificationOpened " +
                "FROM user u " +
                "  LEFT OUTER JOIN push_notification_device pnd ON pnd.user_id  = u.Id " +
                "  LEFT OUTER JOIN push_notification_message_info pnmi ON pnmi.user_id = pnd.user_id " +
                "WHERE u.Id IN :userIds " +
                "GROUP BY u.Id"), null, userIds, date)
                .getResultList().toArray())
                .map(Object[].class::cast)
                .map(push -> QuestActivityNotificationDTO.builder()
                        .userId(((Number) push[0]).intValue())
                        .notificationOpened(((Number) push[1]).intValue() > 0)
                        .build())
                .collect(toMap(QuestActivityNotificationDTO::getUserId, push -> push));
    }

    private Map<Integer, List<QuestActivityCommentDTO>> getCommentsMap(final Integer questId, final Collection<Integer> userIds, final LocalDate date) {
    	//FIXME Vinayak
        return Stream.of(setQueryParameters(em.createNativeQuery("" +
                "SELECT qc.user_id, qc.comments " +
                "FROM quest_comments qc " +
                "WHERE qc.created_date BETWEEN :startDate AND :endDate " +
                "  AND qc.quest_id = :questId " +
                "  AND qc.in_reply_to_comment_id IS NULL " +
                "  AND qc.user_id IN :userIds"), questId, userIds, date)
                .getResultList().toArray())
                .map(Object[].class::cast)
                .map(comment -> QuestActivityCommentDTO.builder()
                        .userId(((Number) comment[0]).intValue())
                        .newsFeedComment((String) comment[1])
                        .build())
                .collect(groupingBy(QuestActivityCommentDTO::getUserId));
    }

    private Map<Integer, List<QuestActivityImageDTO>> getImagesMap(final Integer questId, final Collection<Integer> userIds, final LocalDate date) {
    	//FIXME Vinayak
        return Stream.of(setQueryParameters(em.createNativeQuery("" +
                "SELECT qi.user_id, qi.caption " +
                "FROM quest_image qi " +
                "WHERE qi.created_date BETWEEN :startDate AND :endDate " +
                "  AND qi.quest_id = :questId " +
                "  AND qi.user_id IN :userIds"), questId, userIds, date)
                .getResultList().toArray())
                .map(Object[].class::cast)
                .map(image -> QuestActivityImageDTO.builder()
                        .userId(((Number) image[0]).intValue())
                        .imageCaption((String) image[1])
                        .build())
                .collect(groupingBy(QuestActivityImageDTO::getUserId));
    }

    private Map<Integer, QuestActivityCompletionDTO> getCompletionMap(final Integer questId, final Collection<Integer> userIds, final LocalDate date) {
    	//FIXME Vinayak
        return Stream.of(setQueryParameters(em.createNativeQuery("" +
                "SELECT" +
                "  qeh.user_id," +
                "  SUM(IF(qeh.event_description = 'QUEST_COMPLETE' AND qeh.added_date BETWEEN :startDate AND :endDate, 1, 0)) AS completed " +
                "FROM quest_event_history qeh " +
                "WHERE qeh.quest_id = :questId " +
                "  AND qeh.user_id IN :userIds " +
                "GROUP BY qeh.user_id"), questId, userIds, date)
                .getResultList().toArray())
                .map(Object[].class::cast)
                .map(event -> QuestActivityCompletionDTO.builder()
                        .userId(((Number) event[0]).intValue())
                        .questCompleted(((Number) event[1]).intValue() > 0)
                        .build())
                .collect(toMap(QuestActivityCompletionDTO::getUserId, completion -> completion));
    }

    private Map<Integer, List<QuestActivityPromptDTO>> getPromptsMap(final Integer questId, final Collection<String> userNames, final LocalDate date) {
    	//FIXME Vinayak
        return Stream.of(em.createNativeQuery("" +
                "SELECT u.id                                         AS user_id, " +
                "       IF(pr.prompt_event = 1, 'Start', 'Complete') AS prompt_event_name, " +
                "       pr.msg                                       AS prompt_question, " +
                "       pr.response                                  AS prompt_answer, " +
                "       TIME(pr.ts)                                  AS prompt_time " +
                "FROM prompt_result pr " +
                "         INNER JOIN user u ON u.user_name = pr.username " +
                "WHERE pr.quest_id = :questId " +
                "  AND pr.ts BETWEEN :startDate AND :endDate " +
                "  AND pr.username IN :userNames")
                .setParameter("questId", questId)
                .setParameter("startDate", date.toString())
                .setParameter("endDate", date.plus(1, DAYS).toString())
                .setParameter("userNames", userNames)
                .getResultList().toArray())
                .map(Object[].class::cast)
                .map(prompt -> QuestActivityPromptDTO.builder()
                        .userId(((Number) prompt[0]).intValue())
                        .promptEventName((String) prompt[1])
                        .promptQuestion((String) prompt[2])
                        .promptAnswer((String) prompt[3])
                        .promptTime((Time) prompt[4])
                        .build())
                .collect(groupingBy(QuestActivityPromptDTO::getUserId));
    }

    private Query setQueryParameters(final Query query, final Integer questId, final Collection<Integer> userIds, final LocalDate date) {
        if (questId != null) {
            query.setParameter("questId", questId);
        }
        if (userIds != null) {
            query.setParameter("userIds", userIds);
        }
        if (date != null) {
            query.setParameter("startDate", date.toString());
            query.setParameter("endDate", date.plus(1, DAYS).toString());
        }
        return query;
    }

    private static List<LocalDate> getDatesBetween(final LocalDate startDate, final LocalDate endDate) {
        final long daysBetween = DAYS.between(startDate, endDate);
        return IntStream.iterate(0, i -> i + 1)
                .limit(daysBetween + 1)
                .mapToObj(startDate::plusDays)
                .collect(toList());
    }

    private void autoSizeColumns(final Sheet sheet, final Row row) {
        final Iterator<Cell> headerCellIterator = row.cellIterator();
        while (headerCellIterator.hasNext()) {
            final Cell cell = headerCellIterator.next();
            final int columnIndex = cell.getColumnIndex();
            sheet.autoSizeColumn(columnIndex);
        }
    }

    private void autoSizeHeader(final CellStyle headerStyle, final Sheet sheet, final Row headerRow) {
        final Iterator<Cell> headerCellIterator = headerRow.cellIterator();
        while (headerCellIterator.hasNext()) {
            final Cell cell = headerCellIterator.next();
            final int columnIndex = cell.getColumnIndex();
            cell.setCellStyle(headerStyle);
            sheet.autoSizeColumn(columnIndex);
        }
    }

}
