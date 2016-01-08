package org.wikipedia.vlsergey.secretary.jwpf.model;

import java.net.InetAddress;
import java.util.Date;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.net.InetAddresses;

public class ParsedUser implements User {

	private Long editcount;

	private Set<String> groups;

	private Set<String> implicitgroups;

	private String name;

	private Date registration;

	private Set<String> rights;

	private Long userId;

	@Override
	public Long getEditcount() {
		return editcount;
	}

	public Set<String> getGroups() {
		return groups;
	}

	public Set<String> getImplicitgroups() {
		return implicitgroups;
	}

	@Override
	public InetAddress getInetAddress() {
		if (userId == null || userId.longValue() != 0l || StringUtils.isEmpty(getName()))
			return null;

		return InetAddresses.forString(getName());
	}

	@Override
	public String getName() {
		return name;
	}

	public Date getRegistration() {
		return registration;
	}

	public Set<String> getRights() {
		return rights;
	}

	@Override
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
