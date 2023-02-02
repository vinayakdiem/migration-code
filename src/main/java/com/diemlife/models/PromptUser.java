package com.diemlife.models;

public class PromptUser {

    private final String username;
    private final long questId;
    private final long promptEvent;
    private final String msg;
    private final int msgType;
    private String[] promptOptions;

    public PromptUser(final String username,
                      final long questId,
                      final long promptEvent,
                      final String msg,
                      final int msgType,
                      final String[] promptOptions) {
        this.username = username;
        this.questId = questId;
        this.promptEvent = promptEvent;
        this.msg = msg;
        this.msgType = msgType;
        this.promptOptions = promptOptions;
    }

    public PromptUser(final String username,
                      final long questId,
                      final long promptEvent,
                      final String msg,
                      final int msgType) {
        this(username, questId, promptEvent, msg, msgType, null);
    }

    public String getUsername() {
        return username;
    }

    public long getQuestId() {
        return questId;
    }

    public long getPromptEvent() {
        return promptEvent;
    }

    public String getMsg() {
        return msg;
    }

    public int getMsgType() {
        return msgType;
    }

    public String[] getPromptOptions() {
        return this.promptOptions;
    }

	// TODO -- remove this perhaps later when data layer for prompts is cleaned up
	public void addPromptOptions(String[] promptOptions) {
		if (this.promptOptions == null) {
			this.promptOptions = promptOptions;
		}
	}
}

