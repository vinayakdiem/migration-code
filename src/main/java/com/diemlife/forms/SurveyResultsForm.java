package com.diemlife.forms;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import play.data.validation.Constraints.Required;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
public class SurveyResultsForm implements Serializable {

    @Required
    private SurveyResultForm[] answers;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SurveyResultForm implements Serializable {

		@Required
        private String answer;
        
        @Required
        private Long promptEvent;
        
        @Required
		private String promptText;

		@Required
		private String type;
		
		private Double lat;
		
		private Double lon;
    }

}
