package com.co.eatupapi.messaging.user;

import com.co.eatupapi.dto.user.CreateUserRequest;
import com.co.eatupapi.dto.user.UpdateUserRequest;

import java.util.UUID;

public class UserCommandMessage {

	private UserCommandAction action;
	private UUID userId;
	private String status;
	private CreateUserRequest createRequest;
	private UpdateUserRequest updateRequest;

	public UserCommandAction getAction() {
		return action;
	}

	public void setAction(UserCommandAction action) {
		this.action = action;
	}

	public UUID getUserId() {
		return userId;
	}

	public void setUserId(UUID userId) {
		this.userId = userId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public CreateUserRequest getCreateRequest() {
		return createRequest;
	}

	public void setCreateRequest(CreateUserRequest createRequest) {
		this.createRequest = createRequest;
	}

	public UpdateUserRequest getUpdateRequest() {
		return updateRequest;
	}

	public void setUpdateRequest(UpdateUserRequest updateRequest) {
		this.updateRequest = updateRequest;
	}
}
