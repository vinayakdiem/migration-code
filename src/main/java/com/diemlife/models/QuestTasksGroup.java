package com.diemlife.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import java.util.Date;
import java.util.List;

import static javax.persistence.FetchType.LAZY;

@Entity
@Table(name = "quest_tasks_group", schema = "diemlife")
@Getter
@Setter
@NoArgsConstructor
public class QuestTasksGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, unique = true)
    private Integer id;

    @Column(name = "quest_id")
    private Integer questId;

    @Column(name = "task_group_name")
    private String groupName;

    @Column(name = "user_id")
    private Integer userId;

    @OneToMany(fetch = LAZY, targetEntity = QuestTasks.class, mappedBy="questTasksGroup")
    @OrderBy("order")
    private List<QuestTasks> questTasks;

    @Column(name = "task_group_order")
    private Integer groupOrder;

    @Column(name = "created_date")
    private Date createdDate;

}
