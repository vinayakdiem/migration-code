package com.diemlife.constants;

public final class PromptType {

	public enum PromptEventType {
		AT_QUEST_START(0),
		AT_QUEST_START_IMMEDIATE(1),
		AT_QUEST_COMPLETE(9007199254740992L);

		private final long code;

		PromptEventType(final long code) {
			this.code = code;
		}

		public long getCode() {
			return code;
		}
	}

	// Type Ids
	public static final int UNKNOWN_ID = -1;
	public static final int FREE_FORM_ID = 0;
	public static final int YES_NO_ID = 1;
	public static final int TRUE_FALSE_ID = 2;
	public static final int SCALE_OF_ONE_TO_FIVE_ID = 100;
	public static final int SCALE_OF_ONE_TO_TEN_ID = 101;
	public static final int SCALE_OF_ZERO_TO_SEVEN_ID = 120;
	public static final int AGREE_DISAGREE_TWO_OPTIONS_ID = 200;
	public static final int AGREE_DISAGREE_THREE_OPTIONS_ID = 201;
	public static final int AGREE_DISAGREE_FIVE_OPTIONS_ID = 202;
	public static final int LIKELY_UNLIKELY_TWO_OPTIONS_ID = 300;
	public static final int LIKELY_UNLIKELY_THREE_OPTIONS_ID = 301;
	public static final int LIKELY_UNLIKELY_FIVE_OPTIONS_ID = 302;
	public static final int COUNT_ID = 400;
	public static final int INTEGER_ID = 401;
	public static final int FLOAT_ID = 402;
	public static final int PERCENT_ID = 403;
	public static final int LOCATION_TRACKING_ID = 500;
	
	// Type tags
	public static final String UNKNOWN_TAG = "UNKNOWN";
	public static final String FREE_FORM_TAG = "FREE_FORM";
	public static final String YES_NO_TAG = "YES_NO";
	public static final String TRUE_FALSE_TAG = "TRUE_FALSE";
	public static final String SCALE_OF_ONE_TO_FIVE_TAG = "SCALE_OF_ONE_TO_FIVE";
	public static final String SCALE_OF_ONE_TO_TEN_TAG = "SCALE_OF_ONE_TO_TEN";
	public static final String SCALE_OF_ZERO_TO_SEVEN_TAG = "SCALE_OF_ZERO_TO_SEVEN";
	public static final String AGREE_DISAGREE_TWO_OPTIONS_TAG = "AGREE_DISAGREE_TWO_OPTIONS";
	public static final String AGREE_DISAGREE_THREE_OPTIONS_TAG = "AGREE_DISAGREE_THREE_OPTIONS";
	public static final String AGREE_DISAGREE_FIVE_OPTIONS_TAG = "AGREE_DISAGREE_FIVE_OPTIONS";
	public static final String LIKELY_UNLIKELY_TWO_OPTIONS_TAG = "LIKELY_UNLIKELY_TWO_OPTIONS";
	public static final String LIKELY_UNLIKELY_THREE_OPTIONS_TAG = "LIKELY_UNLIKELY_THREE_OPTIONS";
	public static final String LIKELY_UNLIKELY_FIVE_OPTIONS_TAG = "LIKELY_UNLIKELY_FIVE_OPTIONS";
	public static final String COUNT_TAG = "COUNT";
	public static final String INTEGER_TAG = "INTEGER";
	public static final String FLOAT_TAG = "FLOAT";
	public static final String PERCENT_TAG = "PERECENT";
	public static final String LOCATION_TRACKING_TAG = "LOCATION_TRACKING";
	
	public static int tagToId(String tag) {
		int ret;
		switch (tag) {
			case FREE_FORM_TAG:
				ret = FREE_FORM_ID;
				break;
			case YES_NO_TAG:
				ret = YES_NO_ID;
				break;
			case TRUE_FALSE_TAG:
				ret = TRUE_FALSE_ID;
				break;
			case SCALE_OF_ONE_TO_FIVE_TAG:
				ret = SCALE_OF_ONE_TO_FIVE_ID;
				break;
			case SCALE_OF_ONE_TO_TEN_TAG:
				ret = SCALE_OF_ONE_TO_TEN_ID;
				break;
			case SCALE_OF_ZERO_TO_SEVEN_TAG:
				ret = SCALE_OF_ZERO_TO_SEVEN_ID;
				break;
			case AGREE_DISAGREE_TWO_OPTIONS_TAG:
				ret = AGREE_DISAGREE_TWO_OPTIONS_ID;
				break;
			case AGREE_DISAGREE_THREE_OPTIONS_TAG:
				ret = AGREE_DISAGREE_THREE_OPTIONS_ID;
				break;
			case AGREE_DISAGREE_FIVE_OPTIONS_TAG:
				ret = AGREE_DISAGREE_FIVE_OPTIONS_ID;
				break;
			case LIKELY_UNLIKELY_TWO_OPTIONS_TAG:
				ret = LIKELY_UNLIKELY_TWO_OPTIONS_ID;
				break;
			case LIKELY_UNLIKELY_THREE_OPTIONS_TAG:
				ret = LIKELY_UNLIKELY_THREE_OPTIONS_ID;
				break;
			case LIKELY_UNLIKELY_FIVE_OPTIONS_TAG:
				ret = LIKELY_UNLIKELY_FIVE_OPTIONS_ID;
				break;
			case COUNT_TAG:
				ret = COUNT_ID;
				break;
			case INTEGER_TAG:
				ret = INTEGER_ID;
				break;
			case FLOAT_TAG:
				ret = FLOAT_ID;
				break;
			case PERCENT_TAG:
				ret = PERCENT_ID;
				break;
			case LOCATION_TRACKING_TAG:
				ret = LOCATION_TRACKING_ID;
				break;
			default:
				ret = UNKNOWN_ID;
				break;
		}
		
		return ret;
	}
	
	public static String idToTag(int id) {
		String ret;
		switch (id) {
			case FREE_FORM_ID:
				ret = FREE_FORM_TAG;
				break;
			case YES_NO_ID:
				ret = YES_NO_TAG;
				break;
			case TRUE_FALSE_ID:
				ret = TRUE_FALSE_TAG;
				break;
			case SCALE_OF_ONE_TO_FIVE_ID:
				ret = SCALE_OF_ONE_TO_FIVE_TAG;
				break;
			case SCALE_OF_ONE_TO_TEN_ID:
				ret = SCALE_OF_ONE_TO_TEN_TAG;
				break;
			case SCALE_OF_ZERO_TO_SEVEN_ID:
				ret = SCALE_OF_ZERO_TO_SEVEN_TAG;
				break;
			case AGREE_DISAGREE_TWO_OPTIONS_ID:
				ret = AGREE_DISAGREE_TWO_OPTIONS_TAG;
				break;
			case AGREE_DISAGREE_THREE_OPTIONS_ID:
				ret = AGREE_DISAGREE_THREE_OPTIONS_TAG;
				break;
			case AGREE_DISAGREE_FIVE_OPTIONS_ID:
				ret = AGREE_DISAGREE_FIVE_OPTIONS_TAG;
				break;
			case LIKELY_UNLIKELY_TWO_OPTIONS_ID:
				ret = LIKELY_UNLIKELY_TWO_OPTIONS_TAG;
				break;
			case LIKELY_UNLIKELY_THREE_OPTIONS_ID:
				ret = LIKELY_UNLIKELY_THREE_OPTIONS_TAG;
				break;
			case LIKELY_UNLIKELY_FIVE_OPTIONS_ID:
				ret = LIKELY_UNLIKELY_FIVE_OPTIONS_TAG;
				break;
			case COUNT_ID:
				ret = COUNT_TAG;
				break;
			case INTEGER_ID:
				ret = INTEGER_TAG;
				break;
			case FLOAT_ID:
				ret = FLOAT_TAG;
				break;
			case PERCENT_ID:
				ret = PERCENT_TAG;
				break;
			case LOCATION_TRACKING_ID:
                ret = LOCATION_TRACKING_TAG;
				break;
			default:
				ret = UNKNOWN_TAG;
				break;
		}
		
		return ret;
	}
}

