package org.wikipedia.vlsergey.secretary.jwpf.model;

import java.util.Date;
import java.util.Set;

public class User {

	private Long editcount;

	private Set<String> groups;

	private Set<String> implicitgroups;

	private String name;

	private Date registration;

	private Set<String> rights;

	private Long userId;

	public Long getEditcount() {
		return editcount;
	}

	public Set<String> getGroups() {
		return groups;
	}

	public Set<String> getImplicitgroups() {
		return implicitgroups;
	}

	public String getName() {
		return name;
	}

	public Date getRegistration() {
		return registration;
	}

	public Set<String> getRights() {
		return rights;
	}

	public Long getUserId() {
		return userId;
	}

	public void setEditcount(Long editcount) {
		this.editcount = editcount;
	}

	public void setGroups(Set<String> groups) {
		this.groups = groups;
	}

	public void setImplicitgroups(Set<String> implicitgroups) {
		this.implicitgroups = implicitgroups;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setRegistration(Date registration) {
		this.registration = registration;
	}

	public void setRights(Set<String> rights) {
		this.rights = rights;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

}
