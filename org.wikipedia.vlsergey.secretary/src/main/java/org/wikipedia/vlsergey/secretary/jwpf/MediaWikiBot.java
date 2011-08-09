package org.wikipedia.vlsergey.secretary.jwpf;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRevisionsByPage;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRevisionsByRevision;
import org.wikipedia.vlsergey.secretary.jwpf.model.Page;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.model.RevisionPropery;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ActionException;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;

@Transactional(propagation = Propagation.NEVER)
public class MediaWikiBot extends HttpBot {
	public static final Charset CHARSET = Charset.forName("utf-8");
	public static final String ENCODING = "utf-8";

	private static Revision getSingleRevision(final List<Page> results) {
		if (results == null || results.isEmpty())
			return null;

		final List<? extends Revision> revisions = results.get(0)
				.getRevisions();
		if (revisions == null || revisions.isEmpty())
			return null;

		return revisions.get(0);
	}

	public MediaWikiBot() throws Exception {
		super(new URI("http://ru.wikipedia.org/w/"));
	}

	public Revision queryRevisionByRevision(Long revisionId,
			RevisionPropery[] properties, boolean rvgeneratexml)
			throws ActionException, ProcessException {
		QueryRevisionsByRevision action = new QueryRevisionsByRevision(
				revisionId, properties, rvgeneratexml);
		performAction(action);

		return getSingleRevision(action.getResults());
	}

	public Revision queryRevisionLatest(String pageTitle,
			RevisionPropery[] properties) throws ActionException,
			ProcessException {
		QueryRevisionsByPage action = new QueryRevisionsByPage(
				Collections.singleton(pageTitle), properties);
		performAction(action);

		return getSingleRevision(action.getResults());
	}
}
