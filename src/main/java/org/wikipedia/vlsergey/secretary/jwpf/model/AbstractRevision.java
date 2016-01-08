package org.wikipedia.vlsergey.secretary.jwpf.model;

import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;

import com.google.common.net.InetAddresses;

public abstract class AbstractRevision implements Revision {

	@Override
	@Transient
	public UserKey getUserKey() {
		final String user = getUser();
		final Long userId = getUserId();

		if (userId == null || StringUtils.isEmpty(user))
			return null;

		if (userId.longValue() == 0L) {
			return new UserKey(InetAddresses.forString(user));
		}

		return new UserKey(userId);
	}

}
