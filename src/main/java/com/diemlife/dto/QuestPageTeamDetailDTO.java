/*package com.diemlife.dto;

import java.io.Serializable;

import com.diemlife.models.User;

import lombok.Data;
import lombok.RequiredArgsConstructor;



@RequiredArgsConstructor
@Data
public class QuestPageTeamDetailDTO implements Serializable {

    public String questTitle;
    public User user;
    public String questShareUrl;
    public boolean isTeamPage;
    public QuestTeamDTO questFromEdgeDetail;
    public Long amountBacked;

    public QuestPageTeamDetailDTO(String title, User doer, String questSeoUrl, boolean isTeamView, QuestTeamDTO questTeamDTO, Long currentAmount) {
        this.questTitle = title;
        this.user = doer;
        this.questShareUrl = questSeoUrl;
        this.isTeamPage = isTeamView;
        if (questTeamDTO != null) {
            this.questFromEdgeDetail = questTeamDTO;
        }
        this.amountBacked = currentAmount;
    }
}
*/