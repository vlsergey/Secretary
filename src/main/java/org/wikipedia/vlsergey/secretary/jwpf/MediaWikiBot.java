package org.wikipedia.vlsergey.secretary.jwpf;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wikipedia.vlsergey.secretary.functions.MultiresultFunction;
import org.wikipedia.vlsergey.secretary.jwpf.actions.Edit;
import org.wikipedia.vlsergey.secretary.jwpf.actions.ExpandTemplates;
import org.wikipedia.vlsergey.secretary.jwpf.actions.MultiAction;
import org.wikipedia.vlsergey.secretary.jwpf.actions.PostLogin;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryAllusers;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryCategorymembers;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryEmbeddedinPageIds;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryEmbeddedinTitles;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryExturlusage;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRevisionsByEmbeddedIn;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRevisionsByPageId;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRevisionsByPageIds;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRevisionsByPageTitle;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRevisionsByPageTitles;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRevisionsByRevision;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRevisionsByRevisionIds;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryTokenEdit;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryUnreviewedPages;
import org.wikipedia.vlsergey.secretary.jwpf.actions.Review;
import org.wikipedia.vlsergey.secretary.jwpf.model.CategoryMember;
import org.wikipedia.vlsergey.secretary.jwpf.model.CategoryMemberType;
import org.wikipedia.vlsergey.secretary.jwpf.model.Direction;
import org.wikipedia.vlsergey.secretary.jwpf.model.ExternalUrl;
import org.wikipedia.vlsergey.secretary.jwpf.model.FilterRedirects;
import org.wikipedia.vlsergey.secretary.jwpf.model.Page;
import org.wikipedia.vlsergey.secretary.jwpf.model.ParsedPage;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.model.RevisionPropery;
import org.wikipedia.vlsergey.secretary.jwpf.model.User;
import org.wikipedia.vlsergey.secretary.jwpf.model.UserProperty;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ActionException;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;

@Transactional(propagation = Propagation.NEVER)
public class MediaWikiBot extends HttpBot {
	public static final Charset CHARSET = Charset.forName("utf-8");
	public static final String ENCODING = "utf-8";

	private static final Logger logger = LoggerFactory.getLogger(MediaWikiBot.class);

	private static void addAllRevisionsToResult(final Iterable<ParsedPage> bufferResult, List<Revision> result) {
		for (ParsedPage page : bufferResult) {
			addAllRevisionsToResult(page, result);
		}
	}

	private static void addAllRevisionsToResult(ParsedPage page, List<Revision> result) {
		if (page == null)
			return;

		final List<? extends Revision> revisions = page.getRevisions();
		if (revisions == null) {
			return;
		}

		for (Revision revision : revisions) {
			if (revision == null)
				continue;

			result.add(revision);
		}
	}

	private static void addSingleRevisionsToResult(final Iterable<ParsedPage> bufferResult, List<Revision> result) {
		for (ParsedPage page : bufferResult) {
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
			namespaceString = namespaceString.substring(0, namespaceString.length() - 1);
		}

		return namespaceString;

	}

	private static Revision getSingleRevision(final List<ParsedPage> results) {
		if (results == null || results.isEmpty())
			return null;

		final ParsedPage page = results.get(0);
		return getSingleRevision(page);
	}

	private static Revision getSingleRevision(final ParsedPage page) {
		final List<? extends Revision> revisions = page.getRevisions();
		if (revisions == null || revisions.isEmpty())
			return null;

		return revisions.get(0);
	}

	@Value("false")
	private boolean bot;

	private boolean loggedIn = false;

	private String login;

	private String password;

	public MediaWikiBot() {
	}

	public ExpandTemplates expandTemplates(final String text, final String title, final boolean generateXml,
			final boolean includeComments) {
		ExpandTemplates expandTemplates = new ExpandTemplates(isBot(), text, title, generateXml, includeComments);
		performAction(expandTemplates);
		return expandTemplates;
	}

	public String getLogin() {
		return login;
	}

	public String getPassword() {
		return password;
	}

	@PostConstruct
	public void httpLogin() throws ActionException {
		for (int i = 0; i < 5; i++) {
			logger.info("Login as " + getLogin());
			try {
				PostLogin postLogin = new PostLogin(getLogin(), getPassword());
				performAction(postLogin);

				if (postLogin.needConfirmation()) {
					performAction(postLogin.getConfirmationAction());
				}
			} catch (ProcessException e) {
				e.printStackTrace();
				return;
			} catch (NullPointerException e) {
				e.printStackTrace();
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

	public boolean isBot() {
		return bot;
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
	private <R> Iterable<R> performMultiAction(MultiAction<R> initialAction) throws ActionException {
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
				MultiActionResultIterator(MultiActionResultIterable<R> generatingIterable) {
					this.generatingIterable = generatingIterable;
				}

				/**
				 * if a new query is needed to request more; more results are
				 * requested.
				 * 
				 * @return true if has next
				 */
				@Override
				public boolean hasNext() {
					while (index >= generatingIterable.knownResults.size() && generatingIterable.nextAction != null) {
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
				@Override
				public R next() {
					while (index >= generatingIterable.knownResults.size() && generatingIterable.nextAction != null) {
						generatingIterable.loadMoreResults();
					}
					return generatingIterable.knownResults.get(index++);
				}

				/**
				 * is not supported
				 */
				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}

			}

			private ArrayList<R> knownResults = new ArrayList<R>();

			private MultiAction<R> nextAction = null;

			public MultiActionResultIterable(MultiAction<R> initialAction) {
				this.nextAction = initialAction;
			}

			@Override
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

	public Iterable<User> queryAllusersByGroup(String groupname, UserProperty... properties) {
		logger.info("queryAllusersByGroup(" + groupname + ")");

		QueryAllusers queryAllusers = new QueryAllusers(isBot(), null, null, null, null,
				Collections.singleton(groupname), null, null, Arrays.asList(properties), null, null);
		return performMultiAction(queryAllusers);
	}

	public Iterable<CategoryMember> queryCategoryMembers(String categoryTitle, CategoryMemberType type,
			int... namespaces) throws ActionException {
		logger.info("queryCategoryMembers(" + categoryTitle + ", " + type + ", " + Arrays.toString(namespaces) + ")");

		QueryCategorymembers a = new QueryCategorymembers(isBot(), categoryTitle, createNsString(namespaces),
				type.getQueryString());
		return performMultiAction(a);
	}

	public Iterable<Long> queryEmbeddedInPageIds(String template, int... namespaces) throws ActionException {
		logger.info("queryEmbeddedInPageIds(" + template + ", " + Arrays.toString(namespaces) + ")");

		QueryEmbeddedinPageIds a = new QueryEmbeddedinPageIds(isBot(), template, createNsString(namespaces));
		return performMultiAction(a);
	}

	public Iterable<String> queryEmbeddedInPageTitles(String template, int... namespaces) throws ActionException {
		logger.info("queryEmbeddedInPageTitles(" + template + ", " + Arrays.toString(namespaces) + ")");

		QueryEmbeddedinTitles a = new QueryEmbeddedinTitles(isBot(), template, createNsString(namespaces));
		return performMultiAction(a);
	}

	public Iterable<ExternalUrl> queryExternalUrlUsage(String protocol, String query, int... namespaces)
			throws ActionException {
		QueryExturlusage a = new QueryExturlusage(isBot(), protocol, query, createNsString(namespaces));
		return performMultiAction(a);
	}

	public Revision queryLatestRevision(Long pageId, RevisionPropery[] properties) throws ActionException,
			ProcessException {
		logger.info("queryRevisionByPageId(" + pageId + ", " + Arrays.toString(properties) + ")");

		QueryRevisionsByPageIds action = new QueryRevisionsByPageIds(isBot(), Collections.singleton(pageId), properties);
		performAction(action);

		return getSingleRevision(action.getResults());
	}

	public Revision queryLatestRevision(String pageTitle, RevisionPropery... properties) throws ActionException,
			ProcessException {
		logger.info("queryRevisionLatest('" + pageTitle + "', " + Arrays.toString(properties) + ")");

		QueryRevisionsByPageTitles action = new QueryRevisionsByPageTitles(isBot(), Collections.singleton(pageTitle),
				properties);
		performAction(action);

		return getSingleRevision(action.getResults());
	}

	public Iterable<Revision> queryLatestRevisionsByPageIds(Iterable<Long> pageIds, RevisionPropery... properties)
			throws ActionException, ProcessException {
		return queryLatestRevisionsByPageIdsF(properties).apply(pageIds);
	}

	public MultiresultFunction<Long, Revision> queryLatestRevisionsByPageIdsF(final RevisionPropery... properties) {
		return new MultiresultFunction<Long, Revision>() {
			@Override
			public Iterable<Revision> apply(Iterable<Long> a) {
				logger.info("queryLatestRevisionsByPageIdsF: " + a + "; " + Arrays.asList(properties));

				List<Revision> result = new ArrayList<Revision>();
				QueryRevisionsByPageIds bufferAction = new QueryRevisionsByPageIds(isBot(), a, properties);
				performAction(bufferAction);
				addSingleRevisionsToResult(bufferAction.getResults(), result);

				return result;
			}
		}.makeBatched(isBot() ? 500 : 50);
	}

	public Iterable<Revision> queryLatestRevisionsByPageTitles(Iterable<String> pageIds, RevisionPropery... properties)
			throws ActionException, ProcessException {
		return queryLatestRevisionsByPageTitlesF(properties).apply(pageIds);
	}

	public MultiresultFunction<String, Revision> queryLatestRevisionsByPageTitlesF(final RevisionPropery... properties) {
		return new MultiresultFunction<String, Revision>() {
			@Override
			public Iterable<Revision> apply(Iterable<String> a) {
				logger.info("queryLatestRevisionsByPageTitlesF: " + a + "; " + Arrays.asList(properties));

				List<Revision> result = new ArrayList<Revision>();
				QueryRevisionsByPageTitles bufferAction = new QueryRevisionsByPageTitles(isBot(), a, properties);
				performAction(bufferAction);
				addSingleRevisionsToResult(bufferAction.getResults(), result);

				return result;
			}
		}.makeBatched(isBot() ? 500 : 50);
	}

	public Iterable<ParsedPage> queryPagesWithRevisionByEmbeddedIn(String embeddedIn, int[] namespaces,
			RevisionPropery[] properties) throws ActionException, ProcessException {

		QueryRevisionsByEmbeddedIn query = new QueryRevisionsByEmbeddedIn(isBot(), embeddedIn,
				createNsString(namespaces), properties);
		return performMultiAction(query);
	}

	public Revision queryRevisionByRevisionId(Long revisionId, boolean rvgeneratexml, RevisionPropery[] properties)
			throws ActionException, ProcessException {
		logger.info("queryRevisionByRevisionId(" + revisionId + ", " + Arrays.toString(properties) + ", "
				+ rvgeneratexml + ")");

		QueryRevisionsByRevision action = new QueryRevisionsByRevision(isBot(), revisionId, rvgeneratexml, properties);
		performAction(action);

		return getSingleRevision(action.getResults());
	}

	public Collection<Revision> queryRevisionsByPageId(Long pageId, Long rvstartid, Direction direction,
			RevisionPropery... properties) throws ActionException {
		logger.info("queryRevisionsByPageId(" + pageId + ", " + rvstartid + ", " + direction + ", "
				+ Arrays.toString(properties));

		List<Revision> result = new ArrayList<Revision>();

		for (ParsedPage page : performMultiAction(new QueryRevisionsByPageId(isBot(), pageId, rvstartid, direction,
				properties))) {
			addAllRevisionsToResult(page, result);
		}
		return result;
	}

	public MultiresultFunction<Long, Revision> queryRevisionsByPageIdsF(final RevisionPropery... properties) {
		return new MultiresultFunction<Long, Revision>() {

			@Override
			public Iterable<Revision> apply(Iterable<Long> pageIds) {
				logger.info("queryRevisionsByPageIdsF( " + properties + " ): <batch> " + pageIds);

				List<Revision> result = new ArrayList<Revision>();

				QueryRevisionsByPageIds bufferAction = new QueryRevisionsByPageIds(isBot(), pageIds, properties);
				performAction(bufferAction);

				addSingleRevisionsToResult(bufferAction.getResults(), result);
				return result;
			}
		}.makeBatched(isBot() ? 500 : 50);
	}

	public List<Revision> queryRevisionsByPageTitle(String pageTitle, Long rvstartid, Direction direction,
			RevisionPropery... properties) throws ActionException {
		logger.info("queryRevisionsByPageTitle('" + pageTitle + "', " + rvstartid + ", " + direction + ", "
				+ Arrays.toString(properties));

		List<Revision> result = new ArrayList<Revision>();

		for (ParsedPage page : performMultiAction(new QueryRevisionsByPageTitle(isBot(), pageTitle, rvstartid,
				direction, properties))) {
			addAllRevisionsToResult(page, result);
		}
		return result;
	}

	public MultiresultFunction<Long, Revision> queryRevisionsByRevisionIdsF(final boolean generateXml,
			final RevisionPropery... properties) {
		final int batchLimit = isBot() ? QueryRevisionsByRevisionIds.MAX_FOR_BOTS
				: QueryRevisionsByRevisionIds.MAX_FOR_NON_BOTS;

		return new MultiresultFunction<Long, Revision>() {

			@Override
			public Iterable<Revision> apply(Iterable<Long> revisionIds) {
				List<Revision> result = new ArrayList<Revision>();

				QueryRevisionsByRevisionIds bufferAction = new QueryRevisionsByRevisionIds(isBot(), revisionIds,
						generateXml, properties);

				final Iterable<ParsedPage> pages = performMultiAction(bufferAction);
				addAllRevisionsToResult(pages, result);

				return result;
			}
		}.makeBatched(batchLimit);
	}

	public String queryTokenEdit(Revision revision) throws ActionException, ProcessException {
		logger.info("queryTokenEdit( " + revision + " )");

		QueryTokenEdit queryTokenEdit = new QueryTokenEdit(isBot(), revision);
		performAction(queryTokenEdit);

		if (queryTokenEdit.getEditToken() == null)
			throw new ActionException("Unable to obtain edit token for " + revision);

		return queryTokenEdit.getEditToken();
	}

	public String queryTokenEdit(String pageTitle) throws ActionException, ProcessException {
		logger.info("queryTokenEdit( '" + pageTitle + "' )");

		QueryTokenEdit queryTokenEdit = new QueryTokenEdit(isBot(), pageTitle);
		performAction(queryTokenEdit);

		if (queryTokenEdit.getEditToken() == null)
			throw new ActionException("Unable to obtain edit token for '" + pageTitle + "'");

		return queryTokenEdit.getEditToken();
	}

	public Iterable<Page> queryUnreviewedPages(int[] namespaces, FilterRedirects filterRedirects)
			throws ActionException {
		return performMultiAction(new QueryUnreviewedPages(isBot(), null, null, namespaces, filterRedirects));
	}

	public void review(final Revision revision, final String comment, Integer flag_accuracy) {
		logger.info("review " + flag_accuracy + ": " + revision + " (" + comment + ")");
		if (!isLoggedIn())
			throw new ActionException("Please login first");

		String editToken = queryTokenEdit(revision);

		Review review = new Review(isBot(), revision, editToken, comment, null, flag_accuracy);
		performAction(review);
	}

	public void setBot(boolean bot) {
		this.bot = bot;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void writeContent(final Page page, final Revision currentRevision, final String text, final String summary,
			final boolean minor) throws ActionException, ProcessException {
		logger.info("writeContent: " + page.getTitle());
		if (!isLoggedIn())
			throw new ActionException("Please login first");

		String editToken = queryTokenEdit(currentRevision);

		Edit edit = new Edit(isBot(), page, currentRevision, editToken, text, summary, minor);
		performAction(edit);
	}

	public void writeContent(final String pageTitle, final String prependText, final String text,
			final String appendText, final String summary, final boolean minor, final boolean nocreate)
			throws ActionException, ProcessException {
		logger.info("writeContent: " + pageTitle);
		if (!isLoggedIn())
			throw new ActionException("Please login first");

		String editToken = queryTokenEdit(pageTitle);

		Edit edit = new Edit(isBot(), pageTitle, editToken, prependText, text, appendText, summary, minor, nocreate);
		performAction(edit);
	}

}
