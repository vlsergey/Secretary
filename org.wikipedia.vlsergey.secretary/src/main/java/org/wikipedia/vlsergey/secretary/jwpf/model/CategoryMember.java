package org.wikipedia.vlsergey.secretary.jwpf.model;

import org.wikipedia.vlsergey.secretary.functions.Function;

public interface CategoryMember {

	public static final Function<CategoryMember, Long> pageIdF = new Function<CategoryMember, Long>() {
		@Override
		public Long apply(CategoryMember a) {
			return a.getPageId();
		}
	};

	Integer getNamespace();

	Long getPageId();

	String getPageTitle();

}