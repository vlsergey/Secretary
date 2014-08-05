package org.wikipedia.vlsergey.secretary.jwpf.model;

import java.util.Date;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class UserContributionItem {

	public String comment;

	public Boolean minor;

	public Integer ns;

	public Long pageid;

	public Long parentid;

	public Long revid;

	public Long size;

	public Date timestamp;

	public String title;

	public Boolean top;

	public String user;

	public Long userid;

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
