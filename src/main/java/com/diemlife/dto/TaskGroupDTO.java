/*package com.diemlife.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.diemlife.models.QuestTasksGroup;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TaskGroupDTO implements Serializable {

    private Integer id;
    private String name;
    private Integer order;
    private List<MilestoneDTO> questTasks;

    public static TaskGroupDTO toDto(QuestTasksGroup taskGroup) {
        List<MilestoneDTO> milestoneDTOList = new ArrayList<>();

        taskGroup.getQuestTasks().forEach(dto -> {
                    final MilestoneDTO milestoneDTO = MilestoneDTO.toDto(dto);
                    milestoneDTOList.add(milestoneDTO);
                }
        );

        final TaskGroupDTO taskGroupDTO = new TaskGroupDTO();
        taskGroupDTO.setId(taskGroup.getId());
        taskGroupDTO.setName(taskGroup.getGroupName());
        taskGroupDTO.setOrder(taskGroup.getGroupOrder());
        taskGroupDTO.setQuestTasks(milestoneDTOList);

        return taskGroupDTO;
    }
}
*/