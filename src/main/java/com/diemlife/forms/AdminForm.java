package com.diemlife.forms;

import java.util.List;

import play.data.validation.Constraints;

public class AdminForm extends RegistrationForm {

	@Constraints.Required(message = "Please select a role")
	public List<Integer> roles;
	
	public AdminForm() {
	}
	
	public List<Integer> getRoles() {
		return roles;
	}

	public void setRoles(List<Integer> roles) {
		this.roles = roles;
	}
}