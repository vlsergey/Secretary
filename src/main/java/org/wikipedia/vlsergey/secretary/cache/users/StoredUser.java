package org.wikipedia.vlsergey.secretary.cache.users;

import java.net.InetAddress;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.wikipedia.vlsergey.secretary.jwpf.model.User;

@Entity(name = "StoredUser")
public class StoredUser implements User {

	private Long editcount;

	private String gender;

	@EmbeddedId
	private StoredUserPk key;

	private String name;

	@Override
	public Long getEditcount() {
		return editcount;
	}

	public String getGender() {
		return gender;
	}

	@Override
	@Transient
	public InetAddress getInetAddress() {
		return null;
	}

	public StoredUserPk getKey() {
		return key;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	@Transient
	public Long getUserId() {
		return getKey().getUserId();
	}

	public void setEditcount(Long editcount) {
		this.editcount = editcount;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public void setKey(StoredUserPk key) {
		this.key = key;
	}

	public void setName(String name) {
		this.name = name;
	}

}
