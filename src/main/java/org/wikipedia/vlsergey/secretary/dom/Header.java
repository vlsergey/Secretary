package org.wikipedia.vlsergey.secretary.dom;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class Header extends AbstractContainer {

	private static final long serialVersionUID = 1L;

	private Content content;

	private int id;

	private int level;

	public Header(int level, int id, Content content) {
		super();
		this.level = level;
		this.id = id;
		this.content = content;

		String wiki = getContent().toWiki(true).trim();
		String qqq = StringUtils.repeat("=", level);
		if (!wiki.startsWith(qqq) || !wiki.endsWith(qqq)) {
			throw new IllegalArgumentException("Name '" + wiki + "' doesn't confirm to level '" + level + "'");
		}
	}

	@Override
	public List<? extends Content> getChildren() {
		return Collections.singletonList(this.content);
	}

	public Content getContent() {
		return content;
	}

	public int getId() {
		return id;
	}

	public int getLevel() {
		return level;
	}

	public String getName() {
		String wiki = content.toWiki(true).trim();
		return StringUtils.substring(wiki, level, wiki.length() - level * 2);
	}

	public void setContent(Content content) {
		this.content = content;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setLevel(int level) {
		this.level = level;
	}
}
