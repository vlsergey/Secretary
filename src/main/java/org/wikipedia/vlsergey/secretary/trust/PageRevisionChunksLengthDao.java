package org.wikipedia.vlsergey.secretary.trust;

import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wikipedia.vlsergey.secretary.jwpf.model.Project;
import org.wikipedia.vlsergey.secretary.trust.ProtobufHolder.PageRevisionChunksLength.Builder;

@Repository
@Transactional(readOnly = false)
public class PageRevisionChunksLengthDao {

	private static final Logger log = LoggerFactory.getLogger(PageRevisionChunksLengthDao.class);

	protected HibernateTemplate template = null;

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public PageRevisionChunksLength findByKey(PageRevisionChunksLengthPk key) {
		return template.get(PageRevisionChunksLength.class, key);
	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public PageRevisionChunksLength findByPage(Project project, Long pageId) {
		return template.get(PageRevisionChunksLength.class, new PageRevisionChunksLengthPk(project, pageId));
	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public TLongIntMap findSafe(Project project, Long pageId) {
		PageRevisionChunksLength entity = findByPage(project, pageId);
		if (entity == null) {
			return new TLongIntHashMap();
		}

		byte[] data = entity.getData();
		if (data == null || data.length == 0) {
			return new TLongIntHashMap();
		}

		try {
			org.wikipedia.vlsergey.secretary.trust.ProtobufHolder.PageRevisionChunksLength proto = org.wikipedia.vlsergey.secretary.trust.ProtobufHolder.PageRevisionChunksLength
					.parseFrom(data);
			final List<Long> revisionIdsList = proto.getRevisionIdsList();
			final List<Integer> lengthsList = proto.getLengthsList();

			if (revisionIdsList.size() != lengthsList.size()) {
				log.warn("Unable to restore page {" + project + "}" + pageId + " arrays lengths are not equals: "
						+ revisionIdsList.size() + " != " + lengthsList.size());
				return new TLongIntHashMap();
			}

			final TLongIntHashMap result = new TLongIntHashMap(revisionIdsList.size());
			for (int i = 0; i < revisionIdsList.size(); i++) {
				result.put(revisionIdsList.get(i), lengthsList.get(i));
			}
			return result;

		} catch (Exception exc) {
			log.warn("Unable to restore page {" + project + "}" + pageId + " revisions chunks lengths: " + exc, exc);
			return new TLongIntHashMap();
		}
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new HibernateTemplate(sessionFactory);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public PageRevisionChunksLength store(PageRevisionChunksLength entry) {
		template.save(entry);
		return findByKey(entry.getKey());
	}

	private synchronized void store(Project project, Long pageId, byte[] data) {
		log.info("Store page {" + project + "}#" + pageId + " chunks length data: " + data.length + " bytes");

		final PageRevisionChunksLengthPk key = new PageRevisionChunksLengthPk(project, pageId);
		PageRevisionChunksLength pageRevisionChunksLength = findByKey(key);
		if (pageRevisionChunksLength == null) {
			pageRevisionChunksLength = new PageRevisionChunksLength();
			pageRevisionChunksLength.setKey(key);
		}
		pageRevisionChunksLength.setData(data);
		template.save(pageRevisionChunksLength);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void store(Project project, Long pageId, TLongIntMap result) throws IOException {
		List<Long> revisionIds = Arrays.asList(ArrayUtils.toObject(result.keys(new long[result.size()])));
		List<Integer> lengths = Arrays.asList(ArrayUtils.toObject(result.values(new int[result.size()])));

		final Builder proto = ProtobufHolder.PageRevisionChunksLength.newBuilder();
		proto.addAllRevisionIds(revisionIds);
		proto.addAllLengths(lengths);

		byte[] data = proto.build().toByteArray();
		store(project, pageId, data);
	}
}
