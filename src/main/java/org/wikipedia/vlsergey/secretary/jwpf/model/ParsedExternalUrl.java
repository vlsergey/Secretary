package org.wikipedia.vlsergey.secretary.jwpf.model;

public class ParsedExternalUrl implements ExternalUrl {
	private Integer namespace;

	private Long pageId;

	private String pageTitle;

	private String url;

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

	@Override
	public String getUrl() {
		return url;
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

	public void setUrl(String url) {
		this.url = url;
	}

}
