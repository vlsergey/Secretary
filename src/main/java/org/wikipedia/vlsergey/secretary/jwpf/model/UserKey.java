package org.wikipedia.vlsergey.secretary.jwpf.model;

import java.net.InetAddress;

import com.google.common.net.InetAddresses;
import com.google.common.primitives.UnsignedBytes;

public class UserKey implements Comparable<UserKey> {

	private static final InetAddress INETADDRESS_LOCALHOST = InetAddresses.forString("127.0.0.1");

	public static final UserKey LOCALHOST = new UserKey(INETADDRESS_LOCALHOST);

	private final InetAddress inetAddress;

	private final Long userId;

	public UserKey(final InetAddress inetAddress) {
		super();
		this.inetAddress = inetAddress;
		this.userId = 0l;
	}

	public UserKey(Long userId) {
		super();
		if (userId.longValue() == 0l)
			throw new IllegalArgumentException();

		this.inetAddress = null;
		this.userId = userId;
	}

	@Override
	public int compareTo(UserKey o) {
		int result = Long.compare(this.userId.longValue(), o.userId.longValue());
		if (result != 0)
			return result;

		assert this.isAnonymous() == o.isAnonymous();
		if (this.isAnonymous()) {
			result = UnsignedBytes.lexicographicalComparator().compare(this.inetAddress.getAddress(),
					o.inetAddress.getAddress());
			if (result != 0)
				return result;
		}

		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserKey other = (UserKey) obj;
		if (inetAddress == null) {
			if (other.inetAddress != null)
				return false;
		} else if (!inetAddress.equals(other.inetAddress))
			return false;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}

	public InetAddress getInetAddress() {
		return inetAddress;
	}

	public Long getUserId() {
		return userId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((inetAddress == null) ? 0 : inetAddress.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
		return result;
	}

	public boolean isAnonymous() {
		return userId.longValue() == 0L;
	}

	@Override
	public String toString() {
		return inetAddress != null ? inetAddress.getHostAddress() : "#" + userId;
	}

}
