package org.wikipedia.vlsergey.secretary.trust;

class ContributorTeam extends Contributor {

	private String teamDescription;

	private String teamName;

	public ContributorTeam(String teamName, String teamDescription) {
		super();
		this.teamName = teamName;
		this.teamDescription = teamDescription;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ContributorTeam other = (ContributorTeam) obj;
		if (teamName == null) {
			if (other.teamName != null)
				return false;
		} else if (!teamName.equals(other.teamName))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		return teamName.hashCode();
	}

	@Override
	boolean isSkipIncrementPlace() {
		return true;
	}

	@Override
	public String toString() {
		return teamName;
	}

	@Override
	String toWiki() {
		return "{{comment|" + teamName + "|" + teamDescription + "}}";
	}
}