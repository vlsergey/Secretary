package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.List;
import java.util.SortedMap;

import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Statement;

public interface DictionaryUpdateListener {

	void onUpdate(EntityId property, SortedMap<EntityId, List<Statement>> result);

}
