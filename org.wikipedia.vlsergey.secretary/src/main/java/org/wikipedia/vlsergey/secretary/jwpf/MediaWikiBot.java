package org.wikipedia.vlsergey.secretary.jwpf;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wikipedia.vlsergey.secretary.functions.MultiresultFunction;
import org.wikipedia.vlsergey.secretary.jwpf.actions.Edit;
import org.wikipedia.vlsergey.secretary.jwpf.actions.MultiAction;
import org.wikipedia.vlsergey.secretary.jwpf.actions.PostLogin;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryEmbeddedinPageIds;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryEmbeddedinTitles;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRevisionsByPageId;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRevisionsByPageIds;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRevisionsByPageTitles;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRevisionsByRevision;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRevisionsByRevisionIds;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryTokenEdit;
import org.wikipedia.vlsergey.secretary.jwpf.model.Page;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.model.RevisionPropery;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ActionException;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;

@Transactional(propagation = Propagation.NEVER)
public class MediaWikiBot extends HttpBot {
	public static final Charset CHARSET = Charset.forName("utf-8");
	public static final String ENCODING = "utf-8";

	private static final Logger logger = LoggerFactory
			.getLogger(MediaWikiBot.class);

	private static void addAllRevisionsToResult(final List<Page> bufferResult,
			List<Revision> result) {
		for (Page page : bufferResult) {
			addAllRevisionsToResult(page, result);
		}
	}

	private static void addAllRevisionsToResult(Page page, List<Revision> result) {
		if (page == null)
			return;

		for (Revision revision : page.getRevisions()) {
			if (revision == null)
				continue;

			result.add(revision);
		}
	}

	private static void addSingleRevisionsToResult(
			final List<Page> bufferResult, List<Revision> result) {
		for (Page page : bufferResult) {
			final Revision singleRevision = getSingleRevision(page);
			if (singleRevision != null)
				result.add(singleRevision);
		}
	}

	/**
	 * helper method generating a namespace string as required by the MW-api.
	 * 
	 * @param namespaces
	 *            namespace as
	 * @return with numbers seperated by |
	 */
	private static String createNsString(int... namespaces) {
		if (namespaces == null || namespaces.length == 0)
			return null;

		String namespaceString = new String();

		for (int nsNumber : namespaces) {
			namespaceString += nsNumber + "|";
		}

		// remove last '|'
		if (namespaceString.endsWith("|")) {
			namespaceString = namespaceString.substring(0,
					namespaceString.length() - 1);
		}

		return namespaceString;

	}

	private static Revision getSingleRevision(final List<Page> results) {
		if (results == null || results.isEmpty())
			return null;

		final Page page = results.get(0);
		return getSingleRevision(page);
	}

	private static Revision getSingleRevision(final Page page) {
		final List<? extends Revision> revisions = page.getRevisions();
		if (revisions == null || revisions.isEmpty())
			return null;

		return revisions.get(0);
	}

	private boolean loggedIn = false;

	public MediaWikiBot() throws Exception {
		super(new URI("http://ru.wikipedia.org/w"));
	}

	public void httpLogin(final String username, final String passwd)
			throws ActionException {
		for (int i = 0; i < 5; i++) {
			logger.info("Login as " + username);
			try {
				PostLogin postLogin = new PostLogin(username, passwd);
				performAction(postLogin);

				if (postLogin.needConfirmation()) {
					performAction(postLogin.getConfirmationAction());
				}
			} catch (ProcessException e) {
				e.printStackTrace();
				return;
			} catch (NullPointerException e) {
				if (i < 5 - 1) {
					logger.warn("NPE. Retry login");
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					continue;
				}
				return;
			}
			loggedIn = true;
			return;
		}
	}

	public boolean isLoggedIn() {
		return loggedIn;
	}

	/**
	 * generates an iterable with the results from a series of MultiAction when
	 * given the first of the actions. The result type can vary to match the
	 * result type of the MultiActions.
	 * 
	 * 
	 * @param initialAction
	 *            first action to perform, provides a next action.
	 * @param <R>
	 *            type like String
	 * @return iterable providing access to the result values from the responses
	 *         to the initial and subsequent actions. Attention: when the values
	 *         from the subsequent actions are accessed for the first time, the
	 *         connection to the MediaWiki must still exist, /*++ unless ...
	 * 
	 * @throws ActionException
	 *             on problems with http, cookies and io
	 * @supportedBy MediaWiki 1.9.x API, 1.10.x API
	 */
	private <R> Iterable<R> performMultiAction(MultiAction<R> initialAction)
			throws ActionException {
		if (!isLoggedIn())
			throw new ActionException("Please login first");

		/**
		 * Iterable-class which will store all results which are already known
		 * and perform the next action when more titles are needed
		 */
		@SuppressWarnings("hiding")
		class MultiActionResultIterable<R> implements Iterable<R> {

			/**
			 * matching Iterator, containing an index variable and a reference
			 * to a MultiActionResultIterable
			 */
			class MultiActionResultIterator<R> implements Iterator<R> {

				private MultiActionResultIterable<R> generatingIterable;

				private int index = 0;

				/**
				 * constructor, relies on generatingIterable != null
				 */
				MultiActionResultIterator(
						MultiActionResultIterable<R> generatingIterable) {
					this.generatingIterable = generatingIterable;
				}

				/**
				 * if a new query is needed to request more; more results are
				 * requested.
				 * 
				 * @return true if has next
				 */
				public boolean hasNext() {
					while (index >= generatingIterable.knownResults.size()
							&& generatingIterable.nextAction != null) {
						generatingIterable.loadMoreResults();
					}
					return index < generatingIterable.knownResults.size();
				}

				/**
				 * if a new query is needed to request more; more results are
				 * requested.
				 * 
				 * @return a element of iteration
				 */
				public R next() {
					while (index >= generatingIterable.knownResults.size()
							&& generatingIterable.nextAction != null) {
						generatingIterable.loadMoreResults();
					}
					return generatingIterable.knownResults.get(index++);
				}

				/**
				 * is not supported
				 */
				public void remove() {
					throw new UnsupportedOperationException();
				}

			}

			private ArrayList<R> knownResults = new ArrayList<R>();

			private MultiAction<R> nextAction = null;

			public MultiActionResultIterable(MultiAction<R> initialAction) {
				this.nextAction = initialAction;
			}

			public Iterator<R> iterator() {
				return new MultiActionResultIterator<R>(this);
			}

			/**
			 * request more results if local interation seems to be empty.
			 */
			private void loadMoreResults() {
				if (nextAction != null) {

					try {
						performAction(nextAction);
						knownResults.addAll(nextAction.getResults());

						nextAction = nextAction.getNextAction();

					} catch (RuntimeException e) {
						throw e;
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			}
		}

		return new MultiActionResultIterable<R>(initialAction);
	}

	public Iterable<Long> queryEmbeddedInPageIds(String template,
			int... namespaces) throws ActionException {
		logger.info("queryEmbeddedInPageIds(" + template + ", "
				+ Arrays.toString(namespaces) + ")");

		QueryEmbeddedinPageIds a = new QueryEmbeddedinPageIds(template,
				createNsString(namespaces));
		return performMultiAction(a);
	}

	public Iterable<String> queryEmbeddedInPageTitles(String template,
			int... namespaces) throws ActionException {
		logger.info("queryEmbeddedInPageTitles(" + template + ", "
				+ Arrays.toString(namespaces) + ")");

		QueryEmbeddedinTitles a = new QueryEmbeddedinTitles(template,
				createNsString(namespaces));
		return performMultiAction(a);
	}

	public Revision queryRevisionByPageId(Long pageId,
			RevisionPropery[] properties) throws ActionException,
			ProcessException {
		logger.info("queryRevisionByPageId(" + pageId + ", "
				+ Arrays.toString(properties) + ")");

		QueryRevisionsByPageIds action = new QueryRevisionsByPageIds(
				Collections.singleton(pageId), properties);
		performAction(action);

		return getSingleRevision(action.getResults());
	}

	public Revision queryRevisionByRevisionId(Long revisionId,
			RevisionPropery[] properties, boolean rvgeneratexml)
			throws ActionException, ProcessException {
		logger.info("queryRevisionByRevisionId(" + revisionId + ", "
				+ Arrays.toString(properties) + ", " + rvgeneratexml + ")");

		QueryRevisionsByRevision action = new QueryRevisionsByRevision(
				revisionId, properties, rvgeneratexml);
		performAction(action);

		return getSingleRevision(action.getResults());
	}

	public Revision queryRevisionLatest(String pageTitle,
			RevisionPropery... properties) throws ActionException,
			ProcessException {
		logger.info("queryRevisionLatest('" + pageTitle + "', "
				+ Arrays.toString(properties) + ")");

		QueryRevisionsByPageTitles action = new QueryRevisionsByPageTitles(
				Collections.singleton(pageTitle), properties);
		performAction(action);

		return getSingleRevision(action.getResults());
	}

	public Iterable<Revision> queryRevisionsByPageId(Long pageId,
			Long rvstartid, Direction direction, RevisionPropery... properties)
			throws ActionException {
		logger.info("queryRevisionsByPageId(" + pageId + ", " + rvstartid
				+ ", " + direction + ", " + Arrays.toString(properties));

		List<Revision> result = new ArrayList<Revision>();

		for (Page page : performMultiAction(new QueryRevisionsByPageId(pageId,
				rvstartid, direction, properties))) {
			addAllRevisionsToResult(page, result);
		}
		return result;
	}

	public Collection<Revision> queryRevisionsByPageIds(Iterable<Long> pageIds,
			RevisionPropery... properties) throws ActionException,
			ProcessException {
		logger.info("queryRevisionsByPageIds: " + pageIds + "; "
				+ Arrays.asList(properties));

		List<Revision> result = new ArrayList<Revision>();

		List<Long> buffer = new ArrayList<Long>(50);
		for (Long pageId : pageIds) {
			buffer.add(pageId);

			if (buffer.size() == 50) {
				logger.info("queryRevisionsByPageIds(...): " + buffer);

				QueryRevisionsByPageIds bufferAction = new QueryRevisionsByPageIds(
						buffer, properties);
				performAction(bufferAction);

				addSingleRevisionsToResult(bufferAction.getResults(), result);

				buffer.clear();
				synchronized (this) {
					try {
						this.wait(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}

		if (!buffer.isEmpty()) {
			logger.info("queryRevisionsByPageIds(...): " + buffer);

			QueryRevisionsByPageIds bufferAction = new QueryRevisionsByPageIds(
					buffer, properties);
			performAction(bufferAction);
			addSingleRevisionsToResult(bufferAction.getResults(), result);
			buffer.clear();
		}

		return result;
	}

	public MultiresultFunction<Long, Revision> queryRevisionsByPageIdsF(
			final RevisionPropery... properties) {
		return new MultiresultFunction<Long, Revision>() {

			@Override
			public Iterable<Revision> apply(Iterable<Long> pageIds) {
				List<Revision> result = new ArrayList<Revision>();

				QueryRevisionsByPageIds bufferAction = new QueryRevisionsByPageIds(
						pageIds, properties);
				performAction(bufferAction);

				addSingleRevisionsToResult(bufferAction.getResults(), result);
				return result;
			}
		}.batchlazy(50);
	}

	public Collection<Revision> queryRevisionsByRevisionIds(
			Iterable<Long> revisionIds, RevisionPropery... properties)
			throws ActionException, ProcessException {
		logger.info("queryRevisionsByRevisionIds: " + revisionIds + "; "
				+ Arrays.asList(properties));

		List<Revision> result = new ArrayList<Revision>();

		List<Long> buffer = new ArrayList<Long>(50);
		for (Long revisionId : revisionIds) {
			buffer.add(revisionId);

			if (buffer.size() == 50) {
				logger.info("queryRevisionsByRevisionIds(...): " + buffer);

				QueryRevisionsByRevisionIds bufferAction = new QueryRevisionsByRevisionIds(
						buffer, properties);
				performAction(bufferAction);

				addAllRevisionsToResult(bufferAction.getResults(), result);

				buffer.clear();
				synchronized (this) {
					try {
						this.wait(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}

		if (!buffer.isEmpty()) {
			logger.info("queryRevisionsByRevisionIds(...): " + buffer);

			QueryRevisionsByRevisionIds bufferAction = new QueryRevisionsByRevisionIds(
					buffer, properties);
			performAction(bufferAction);
			addAllRevisionsToResult(bufferAction.getResults(), result);
			buffer.clear();
		}

		return result;
	}

	public MultiresultFunction<Long, Revision> queryRevisionsByRevisionIdsF(
			final RevisionPropery... properties) {
		return new MultiresultFunction<Long, Revision>() {

			@Override
			public Iterable<Revision> apply(Iterable<Long> revisionIds) {
				List<Revision> result = new ArrayList<Revision>();

				QueryRevisionsByRevisionIds bufferAction = new QueryRevisionsByRevisionIds(
						revisionIds, properties);
				performAction(bufferAction);

				addSingleRevisionsToResult(bufferAction.getResults(), result);
				return result;
			}
		}.batchlazy(50);
	}

	public String queryTokenEdit(Revision revision) throws ActionException,
			ProcessException {
		QueryTokenEdit queryTokenEdit = new QueryTokenEdit(revision);
		performAction(queryTokenEdit);

		if (queryTokenEdit.getEditToken() == null)
			throw new ActionException("Unable to obtain edit token for "
					+ revision);

		return queryTokenEdit.getEditToken();
	}

	public String queryTokenEdit(String pageTitle) throws ActionException,
			ProcessException {
		QueryTokenEdit queryTokenEdit = new QueryTokenEdit(pageTitle);
		performAction(queryTokenEdit);

		if (queryTokenEdit.getEditToken() == null)
			throw new ActionException("Unable to obtain edit token for '"
					+ pageTitle + "'");

		return queryTokenEdit.getEditToken();
	}

	public final void writeContent(final Page page,
			final Revision currentRevision, final String text,
			final String summary, final boolean minor, final boolean bot)
			throws ActionException, ProcessException {
		logger.info("writeContent: " + page.getTitle());
		if (!isLoggedIn())
			throw new ActionException("Please login first");

		String editToken = queryTokenEdit(currentRevision);

		Edit edit = new Edit(page, currentRevision, editToken, text, summary,
				minor, bot);
		performAction(edit);
	}

	public final void writeContent(final String pageTitle,
			final String prependText, final String text,
			final String appendText, final String summary, final boolean minor,
			final boolean bot, final boolean nocreate) throws ActionException,
			ProcessException {
		logger.info("writeContent: " + pageTitle);
		if (!isLoggedIn())
			throw new ActionException("Please login first");

		String editToken = queryTokenEdit(pageTitle);

		Edit edit = new Edit(pageTitle, editToken, prependText, text,
				appendText, summary, minor, bot, nocreate);
		performAction(edit);
	}

}
