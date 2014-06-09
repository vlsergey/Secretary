package org.wikipedia.vlsergey.secretary.trust;

import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wikipedia.vlsergey.secretary.jwpf.model.Project;

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
			ObjectInputStream objectInputStream = new ObjectInputStream(new GZIPInputStream(new ByteArrayInputStream(
					data)));
			try {
				long[] revisionIds = (long[]) objectInputStream.readObject();
				int[] lengths = (int[]) objectInputStream.readObject();

				final TLongIntHashMap result = new TLongIntHashMap(revisionIds.length);
				for (int i = 0; i < revisionIds.length; i++) {
					long revisionId = revisionIds[i];
					int length = lengths[i];
					result.put(revisionId, length);
				}
				return result;
			} finally {
				objectInputStream.close();
			}
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
		long[] revisionIds = result.keys(new long[result.size()]);
		int[] lengths = result.values(new int[result.size()]);

		ByteArrayOutputStream baos = new ByteArrayOutputStream(result.size() * 12 + 100);
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(new GZIPOutputStream(baos));
		try {
			objectOutputStream.writeObject(revisionIds);
			objectOutputStream.writeObject(lengths);
		} finally {
			objectOutputStream.close();
		}

		byte[] data = baos.toByteArray();
		store(project, pageId, data);
	}
}
