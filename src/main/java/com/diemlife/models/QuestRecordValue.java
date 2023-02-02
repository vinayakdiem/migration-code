package com.diemlife.models;

import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * * Created by Raj on 08/13/22.
 */
@Entity
@Table(name = "quest_record_value")
public class QuestRecordValue implements Serializable {

	private Integer id;
	private Integer questId;
	private Integer actvityRecordValueId;
	private Integer pillarId;
	
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", nullable = false, unique = true)
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(name = "quest_id")
	public Integer getQuestId() {
		return questId;
	}

	public void setQuestId(Integer questId) {
		this.questId = questId;
	}

	@Column(name = "actvity_record_value_id")
	public Integer getActvityRecordValueId() {
		return actvityRecordValueId;
	}

	public void setActvityRecordValueId(Integer actvityRecordValueId) {
		this.actvityRecordValueId = actvityRecordValueId;
	}

	@Column(name = "pillar_id")
	public Integer getPillarId() {
		return pillarId;
	}

	public void setPillarId(Integer pillarId) {
		this.pillarId = pillarId;
	}

		
}
