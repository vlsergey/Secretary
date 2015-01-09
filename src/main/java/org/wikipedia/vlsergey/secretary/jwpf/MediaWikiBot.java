package org.wikipedia.vlsergey.secretary.jwpf;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wikipedia.vlsergey.secretary.dom.parser.XmlParser;
import org.wikipedia.vlsergey.secretary.functions.MultiresultFunction;
import org.wikipedia.vlsergey.secretary.jwpf.actions.Edit;
import org.wikipedia.vlsergey.secretary.jwpf.actions.ExpandTemplates;
import org.wikipedia.vlsergey.secretary.jwpf.actions.MultiAction;
import org.wikipedia.vlsergey.secretary.jwpf.actions.PostLogin;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryAllusers;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryExturlusage;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRecentChanges;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRedirectsByPageTitles;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRevisionsByAllPages;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRevisionsByBacklinks;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRevisionsByCategoryMembers;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRevisionsByCategoryMembers.CmType;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRevisionsByEmbeddedIn;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRevisionsByLinks;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRevisionsByPageId;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRevisionsByPageIds;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRevisionsByPageTitle;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRevisionsByPageTitles;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRevisionsByRecentChanges;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRevisionsByRevision;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRevisionsByRevisionIds;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryToken;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryUnreviewedPages;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryUserContributions;
import org.wikipedia.vlsergey.secretary.jwpf.actions.Review;
import org.wikipedia.vlsergey.secretary.jwpf.model.Direction;
import org.wikipedia.vlsergey.secretary.jwpf.model.ExternalUrl;
import org.wikipedia.vlsergey.secretary.jwpf.model.FilterRedirects;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Page;
import org.wikipedia.vlsergey.secretary.jwpf.model.ParsedPage;
import org.wikipedia.vlsergey.secretary.jwpf.model.ParsedRevision;
import org.wikipedia.vlsergey.secretary.jwpf.model.Project;
import org.wikipedia.vlsergey.secretary.jwpf.model.RecentChange;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.model.RevisionPropery;
import org.wikipedia.vlsergey.secretary.jwpf.model.TokenType;
import org.wikipedia.vlsergey.secretary.jwpf.model.User;
import org.wikipedia.vlsergey.secretary.jwpf.model.UserContributionItem;
import org.wikipedia.vlsergey.secretary.jwpf.model.UserContributionProperty;
import org.wikipedia.vlsergey.secretary.jwpf.model.UserProperty;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ActionException;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;

@Transactional(propagation = Propagation.NEVER)
public class MediaWikiBot extends HttpBot {
	public static final Charset CHARSET = Charset.forName("utf-8");
	public static final String ENCODING = "utf-8";

	private static final Log log = LogFactory.getLog(MediaWikiBot.class);

	private static void addAllRevisionsToResult(final Iterable<ParsedPage> bufferResult, List<ParsedRevision> result) {
		for (ParsedPage page : bufferResult) {
			addAllRevisionsToResult(page, result);
		}
	}

	private static void addAllRevisionsToResult(ParsedPage page, List<ParsedRevision> result) {
		if (page == null)
			return;

		final List<ParsedRevision> revisions = page.getRevisions();
		if (revisions == null) {
			return;
		}

		for (ParsedRevision revision : revisions) {
			if (revision == null)
				continue;

			result.add(revision);
		}
	}

	private static void addSingleRevisionsToResult(final Iterable<ParsedPage> bufferResult, List<ParsedRevision> result) {
		for (ParsedPage page : bufferResult) {
			final ParsedRevision singleRevision = getSingleRevision(page);
			if (singleRevision != null)
				result.add(singleRevision);
		}
	}

	private static Revision getSingleRevision(final List<ParsedPage> results) {
		if (results == null || results.isEmpty())
			return null;

		final ParsedPage page = results.get(0);
		return getSingleRevision(page);
	}

	private static ParsedRevision getSingleRevision(final ParsedPage page) {
		final List<ParsedRevision> revisions = page.getRevisions();
		if (revisions == null || revisions.isEmpty())
			return null;

		return revisions.get(0);
	}

	@Value("false")
	private boolean bot;

	private boolean loggedIn = false;

	private String login;

	private String password;

	private Project project;

	private List<Long> writeActions = new LinkedList<>();

	private XmlParser xmlParser;

	public MediaWikiBot() {
	}

	protected synchronized void enforceWriteLimit() {
		// cleanup old actions
		long boundary = System.currentTimeMillis() - DateUtils.MILLIS_PER_MINUTE;
		for (Iterator<Long> iterator = writeActions.iterator(); iterator.hasNext();) {
			Long action = iterator.next();
			if (action.longValue() <= boundary) {
				iterator.remove();
			}
		}

		final int writeLimitPerMinute = getWriteLimitPerMinute();
		if (writeActions.size() >= writeLimitPerMinute) {
			long earliest = writeActions.get(0);
			long sleepUntil = earliest + DateUtils.MILLIS_PER_MINUTE + 1;
			while (System.currentTimeMillis() <= sleepUntil) {
				try {
					Thread.sleep(Math.max(System.currentTimeMillis() - sleepUntil, 1));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		writeActions.add(System.currentTimeMillis());
	}

	public ExpandTemplates expandTemplates(final String text, final String title, final boolean includeComments,
			ExpandTemplates.Prop... props) {
		ExpandTemplates expandTemplates = new ExpandTemplates(isBot(), text, title, includeComments, props);
		performAction(expandTemplates);
		return expandTemplates;
	}

	public String getLogin() {
		return login;
	}

	public String getPassword() {
		return password;
	}

	@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
	public Project getProject() {
		return project;
	}

	private int getWriteLimitPerMinute() {
		return isBot() ? 100 : 5;
	}

	public XmlParser getXmlParser() {
		return xmlParser;
	}

	public boolean isBot() {
		return bot;
	}

	public boolean isCachedRevisionValid(Revision stored) {
		if (stored == null) {
			return false;
		}

		boolean doNotCheckXml = stored.getPage() != null && stored.getPage().getNamespace() != null
				&& stored.getPage().getNamespace().intValue() == 0
				&& !getProject().isMainNamespaceHasXmlRepresentation();
		if (!doNotCheckXml && !stored.hasXml()) {
			return false;
		}

		return stored.hasContent() && StringUtils.isNotEmpty(stored.getUser()) && stored.getTimestamp() != null
				&& stored.getTimestamp().getTime() != 0 && stored.getSize() != null && stored.getSize().longValue() > 0;
	}

	public boolean isLoggedIn() {
		return loggedIn;
	}

	@PostConstruct
	public void login() throws ActionException {
		for (int i = 0; i < 5; i++) {
			log.info("Login as " + getLogin() + " at " + getProject());
			try {
				PostLogin postLogin = new PostLogin(isBot(), getLogin(), getPassword());
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
					log.warn("NPE. Retry login");
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
		log.info("queryAllusersByGroup(" + groupname + ")");

		QueryAllusers queryAllusers = new QueryAllusers(isBot(), null, null, null, null,
				Collections.singleton(groupname), null, null, Arrays.asList(properties), null, null);
		return performMultiAction(queryAllusers);
	}

	public Iterable<ExternalUrl> queryExternalUrlUsage(String protocol, String query, Namespace... namespaces)
			throws ActionException {
		QueryExturlusage a = new QueryExturlusage(isBot(), protocol, query, namespaces);
		return performMultiAction(a);
	}

	public Revision queryLatestRevision(Long pageId, RevisionPropery[] properties) throws ActionException,
			ProcessException {
		QueryRevisionsByPageIds action = new QueryRevisionsByPageIds(isBot(), Collections.singleton(pageId), properties);
		performAction(action);
		return getSingleRevision(action.getResults());
	}

	public Revision queryLatestRevision(String pageTitle, boolean followRedirects, RevisionPropery... properties)
			throws ActionException, ProcessException {
		log.info("queryRevisionLatest('" + pageTitle + "', " + Arrays.toString(properties) + ")");

		QueryRevisionsByPageTitles action = new QueryRevisionsByPageTitles(isBot(), Collections.singleton(pageTitle),
				followRedirects, properties);
		performAction(action);

		return getSingleRevision(action.getResults());
	}

	public Iterable<ParsedPage> queryLatestRevisionsByPageIds(Iterable<? extends Long> pageIds,
			RevisionPropery... properties) throws ActionException, ProcessException {
		return queryLatestRevisionsByPageIdsF(properties).apply(pageIds);
	}

	public MultiresultFunction<Long, ParsedPage> queryLatestRevisionsByPageIdsF(final RevisionPropery... properties) {
		return new MultiresultFunction<Long, ParsedPage>() {
			@Override
			public Iterable<ParsedPage> apply(Iterable<? extends Long> a) {
				QueryRevisionsByPageIds bufferAction = new QueryRevisionsByPageIds(isBot(), a, properties);
				performAction(bufferAction);
				return bufferAction.getResults();
			}
		}.makeBatched(isBot() ? 500 : 50);
	}

	public Iterable<ParsedPage> queryLatestRevisionsByPageTitles(Iterable<String> pageIds, boolean followRedirects,
			RevisionPropery... properties) throws ActionException, ProcessException {
		return queryLatestRevisionsByPageTitlesF(followRedirects, properties).apply(pageIds);
	}

	public MultiresultFunction<String, ParsedPage> queryLatestRevisionsByPageTitlesF(final boolean followRedirects,
			final RevisionPropery... properties) {
		return new MultiresultFunction<String, ParsedPage>() {
			@Override
			public Iterable<ParsedPage> apply(Iterable<? extends String> a) {
				log.info("queryLatestRevisionsByPageTitlesF: " + a + "; " + Arrays.asList(properties));

				QueryRevisionsByPageTitles bufferAction = new QueryRevisionsByPageTitles(isBot(), a, followRedirects,
						properties);
				performAction(bufferAction);
				return bufferAction.getResults();
			}
		}.makeBatched(isBot() ? 500 : 50);
	}

	public Iterable<ParsedPage> queryPagesWithRevisionByAllPages(Namespace namespace, RevisionPropery[] properties)
			throws ActionException, ProcessException {

		QueryRevisionsByAllPages query = new QueryRevisionsByAllPages(isBot(), namespace, properties);
		return performMultiAction(query);
	}

	public Iterable<ParsedPage> queryPagesWithRevisionByBacklinks(Long pageId, Namespace[] namespaces,
			RevisionPropery[] properties) throws ActionException, ProcessException {

		QueryRevisionsByBacklinks query = new QueryRevisionsByBacklinks(isBot(), pageId, namespaces, properties);
		return performMultiAction(query);
	}

	public Iterable<ParsedPage> queryPagesWithRevisionByCategoryMembers(String title, Namespace[] namespaces,
			CmType type, RevisionPropery[] properties) throws ActionException, ProcessException {

		QueryRevisionsByCategoryMembers query = new QueryRevisionsByCategoryMembers(isBot(), title, namespaces, type,
				properties);
		return performMultiAction(query);
	}

	public Iterable<ParsedPage> queryPagesWithRevisionByEmbeddedIn(String embeddedIn, Namespace[] namespaces,
			RevisionPropery[] properties) throws ActionException, ProcessException {

		QueryRevisionsByEmbeddedIn query = new QueryRevisionsByEmbeddedIn(isBot(), embeddedIn, namespaces, properties);
		return performMultiAction(query);
	}

	public Iterable<ParsedPage> queryPagesWithRevisionByLinks(Long pageId, Namespace[] namespaces,
			RevisionPropery[] properties) throws ActionException, ProcessException {

		QueryRevisionsByLinks query = new QueryRevisionsByLinks(isBot(), pageId, namespaces, properties);
		return performMultiAction(query);
	}

	public Iterable<ParsedPage> queryPagesWithRevisionByRecentChanges(Direction direction, Date start, String type,
			Boolean toponly, RevisionPropery[] properties) throws ActionException, ProcessException {

		QueryRevisionsByRecentChanges query = new QueryRevisionsByRecentChanges(isBot(), properties);
		query.grcdir = direction;
		query.grcstart = start;
		query.grctype = type;
		query.grctoponly = toponly;
		query.build();
		return performMultiAction(query);
	}

	public Iterable<RecentChange> queryRecentChanges(Direction direction, Date start, String type, Boolean toponly)
			throws ActionException {
		QueryRecentChanges queryRecentChanges = new QueryRecentChanges(bot);
		queryRecentChanges.rcdir = direction;
		queryRecentChanges.rcstart = start;
		queryRecentChanges.rctype = type;
		queryRecentChanges.rctoponly = toponly;
		queryRecentChanges.build();
		return performMultiAction(queryRecentChanges);
	}

	public Map<String, String> queryRedirectsByPageTitles(Iterable<String> pageTitles) {
		QueryRedirectsByPageTitles queryRedirectsByPageTitles = new QueryRedirectsByPageTitles(isBot(), pageTitles);
		performAction(queryRedirectsByPageTitles);
		return queryRedirectsByPageTitles.getRedirects();
	}

	public Revision queryRevisionByRevisionId(Long revisionId, boolean rvgeneratexml, RevisionPropery[] properties)
			throws ActionException, ProcessException {
		QueryRevisionsByRevision action = new QueryRevisionsByRevision(isBot(), revisionId, rvgeneratexml, properties);
		performAction(action);

		return getSingleRevision(action.getResults());
	}

	public Collection<ParsedRevision> queryRevisionsByPageId(Long pageId, Long rvstartid, Direction direction,
			RevisionPropery... properties) throws ActionException {
		List<ParsedRevision> result = new ArrayList<ParsedRevision>();

		for (ParsedPage page : performMultiAction(new QueryRevisionsByPageId(isBot(), pageId, rvstartid, direction,
				properties))) {
			addAllRevisionsToResult(page, result);
		}
		return result;
	}

	public MultiresultFunction<Long, ParsedRevision> queryRevisionsByPageIdsF(final RevisionPropery... properties) {
		return new MultiresultFunction<Long, ParsedRevision>() {

			@Override
			public Iterable<ParsedRevision> apply(Iterable<? extends Long> pageIds) {
				log.info("queryRevisionsByPageIdsF( " + properties + " ): <batch> " + pageIds);

				List<ParsedRevision> result = new ArrayList<ParsedRevision>();

				QueryRevisionsByPageIds bufferAction = new QueryRevisionsByPageIds(isBot(), pageIds, properties);
				performAction(bufferAction);

				addSingleRevisionsToResult(bufferAction.getResults(), result);
				return result;
			}
		}.makeBatched(isBot() ? 500 : 50);
	}

	public List<ParsedRevision> queryRevisionsByPageTitle(String pageTitle, Long rvstartid, Direction direction,
			RevisionPropery... properties) throws ActionException {
		log.info("queryRevisionsByPageTitle('" + pageTitle + "', " + rvstartid + ", " + direction + ", "
				+ Arrays.toString(properties));

		List<ParsedRevision> result = new ArrayList<>();

		for (ParsedPage page : performMultiAction(new QueryRevisionsByPageTitle(isBot(), pageTitle, rvstartid,
				direction, properties))) {
			addAllRevisionsToResult(page, result);
		}
		return result;
	}

	public MultiresultFunction<Long, ParsedRevision> queryRevisionsByRevisionIdsF(final boolean generateXml,
			final RevisionPropery... properties) {
		final int batchLimit = isBot() ? QueryRevisionsByRevisionIds.MAX_FOR_BOTS
				: QueryRevisionsByRevisionIds.MAX_FOR_NON_BOTS;

		return new MultiresultFunction<Long, ParsedRevision>() {

			@Override
			public Iterable<ParsedRevision> apply(Iterable<? extends Long> revisionIds) {
				List<ParsedRevision> result = new ArrayList<>();

				QueryRevisionsByRevisionIds bufferAction = new QueryRevisionsByRevisionIds(isBot(), revisionIds,
						generateXml, properties);

				final Iterable<ParsedPage> pages = performMultiAction(bufferAction);
				addAllRevisionsToResult(pages, result);

				return result;
			}
		}.makeBatched(batchLimit);
	}

	public String queryTokenEdit() throws ActionException, ProcessException {
		QueryToken queryTokenEdit = new QueryToken(isBot(), TokenType.csrf);
		performAction(queryTokenEdit);

		if (!queryTokenEdit.getTokens().containsKey(TokenType.csrf))
			throw new ActionException("Unable to obtain edit token of type " + TokenType.csrf);

		return queryTokenEdit.getTokens().get(TokenType.csrf);
	}

	public Iterable<Page> queryUnreviewedPages(Namespace[] namespaces, FilterRedirects filterRedirects)
			throws ActionException {
		return performMultiAction(new QueryUnreviewedPages(isBot(), null, null, namespaces, filterRedirects));
	}

	public Iterable<UserContributionItem> queryUserContributions(Date start, Date end, Direction direction,
			String user, Namespace[] namespaces, UserContributionProperty... properties) {
		QueryUserContributions queryUserContributions = new QueryUserContributions(isBot(), start, end,
				new String[] { user }, null, direction, namespaces, properties);
		return performMultiAction(queryUserContributions);
	}

	public void review(final Revision revision, final String comment, Integer flag_accuracy) {
		if (!isLoggedIn())
			throw new ActionException("Please login first");

		String editToken = queryTokenEdit();

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

	public void setProject(Project project) {
		this.project = project;
	}

	public void setXmlParser(XmlParser xmlParser) {
		this.xmlParser = xmlParser;
	}

	public synchronized void writeContent(final Revision currentRevision, final String text, final String summary,
			final boolean minor) throws ActionException, ProcessException {
		if (!isLoggedIn())
			throw new ActionException("Please login first");
		enforceWriteLimit();
		log.info("writeContent: " + currentRevision);

		RuntimeException last = null;
		for (int i = 0; i < 3; i++) {
			try {
				String editToken = queryTokenEdit();
				Edit edit = new Edit(isBot(), currentRevision.getPage(), currentRevision, editToken, text, summary,
						minor);
				performAction(edit);
				return;
			} catch (RuntimeException exc) {
				if (exc.getMessage().contains("Invalid token")) {
					last = exc;
					continue;
				}
				throw exc;
			}
		}
		throw last;
	}

	public synchronized void writeContent(final String pageTitle, final String prependText, final String text,
			final String appendText, final String summary, final boolean minor, final boolean nocreate)
			throws ActionException, ProcessException {
		if (!isLoggedIn())
			throw new ActionException("Please login first");
		enforceWriteLimit();
		log.info("writeContent: " + pageTitle);

		RuntimeException last = null;
		for (int i = 0; i < 3; i++) {
			try {
				String editToken = queryTokenEdit();
				Edit edit = new Edit(isBot(), pageTitle, editToken, prependText, text, appendText, summary, minor,
						nocreate);
				performAction(edit);
				return;
			} catch (RuntimeException exc) {
				if (exc.getMessage().contains("Invalid token")) {
					last = exc;
					continue;
				}
				throw exc;
			}
		}
		throw last;
	}
}
