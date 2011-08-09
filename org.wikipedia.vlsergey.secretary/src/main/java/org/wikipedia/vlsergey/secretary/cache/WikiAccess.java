package org.wikipedia.vlsergey.secretary.cache;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.model.RevisionPropery;
import org.wikipedia.vlsergey.secretary.jwpf.utils.JwbfException;

public class WikiAccess {

	@Autowired
	private MediaWikiBot mediaWikiBot;

	@Autowired
	private StoredRevisionDao storedRevisionDao;

	@Transactional(propagation = Propagation.NEVER)
	public String getLatestRevisionContent(String pageTitle)
			throws JwbfException {
		Revision latest = mediaWikiBot.queryRevisionLatest(pageTitle,
				new RevisionPropery[] { RevisionPropery.IDS });

		if (latest == null)
			return null;

		Revision stored = storedRevisionDao.getRevisionById(latest.getId());
		if (stored != null && StringUtils.isNotEmpty(stored.getContent()))
			return stored.getContent();

		Revision withContent = mediaWikiBot.queryRevisionByRevision(
				latest.getId(), new RevisionPropery[] { RevisionPropery.IDS,
						RevisionPropery.CONTENT }, false);

		if (withContent == null)
			// deleted
			return null;

		storedRevisionDao.getOrCreate(withContent);
		return withContent.getContent();
	}
}
