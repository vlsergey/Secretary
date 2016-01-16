package org.wikipedia.vlsergey.secretary.jwpf.model;

import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;

import com.google.common.net.InetAddresses;

public abstract class AbstractRevision implements Revision {

	@Override
	@Transient
	public UserKey getUserKey() {
		if (isUserHidden())
			return UserKey.HIDDEN;

		final String user = getUser();
		final Long userId = getUserId();

		if (userId == null || StringUtils.isEmpty(user))
			return null;

		if (userId.longValue() == 0L) {
			try {
				return new UserKey(InetAddresses.forString(user));
			} catch (IllegalArgumentException exc) {
				return UserKey.UNKNOWN;
			}
		}

		return new UserKey(userId);
	}

	@Override
	@Transient
	public boolean isUserHidden() {
		return getUserHidden() != null && getUserHidden().booleanValue();
	}

}
