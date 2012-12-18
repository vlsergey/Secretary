package org.wikipedia.vlsergey.secretary.jwpf.model;

public class CategoryMemberImpl implements CategoryMember {
	private Integer namespace;

	private Long pageId;

	private String pageTitle;

	@Override
	public Integer getNamespace() {
		return namespace;
	}

	@Override
	public Long getPageId() {
		return pageId;
	}

	@Override
	public String getPageTitle() {
		return pageTitle;
	}

	public void setNamespace(Integer namespace) {
		this.namespace = namespace;
	}

	public void setPageId(Long pageId) {
		this.pageId = pageId;
	}

	public void setPageTitle(String title) {
		this.pageTitle = title;
	}

}
