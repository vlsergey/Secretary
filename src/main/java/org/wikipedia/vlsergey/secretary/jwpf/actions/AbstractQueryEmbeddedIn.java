package org.wikipedia.vlsergey.secretary.jwpf.actions;

abstract class AbstractQueryEmbeddedIn extends AbstractQueryAction {

	public static final int MAX_FOR_BOTS = 5000;

	public static final int MAX_FOR_NON_BOTS = 500;

	public AbstractQueryEmbeddedIn(boolean bot) {
		super(bot);
	}

	protected int getLimit() {
		return (isBot() ? MAX_FOR_BOTS : MAX_FOR_NON_BOTS);
	}
}
