package org.wikipedia.vlsergey.secretary.trust;

class ContributorUser extends Contributor {

	private boolean skipIncrementPlace = false;

	private String userName;

	public ContributorUser(String userName) {
		super();
		this.userName = userName;
	}

	public ContributorUser(String userName, boolean skipIncrementPlace) {
		super();
		this.userName = userName;
		this.skipIncrementPlace = skipIncrementPlace;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ContributorUser other = (ContributorUser) obj;
		if (userName == null) {
			if (other.userName != null)
				return false;
		} else if (!userName.equals(other.userName))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		return userName.hashCode();
	}

	@Override
	public boolean isSkipIncrementPlace() {
		return skipIncrementPlace;
	}

	@Override
	public String toString() {
		return userName;
	}

	@Override
	String toWiki() {
		return "[[User:" + userName + "|" + userName + "]]";
	}

}