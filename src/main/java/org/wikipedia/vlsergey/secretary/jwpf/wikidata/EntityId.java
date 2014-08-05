package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

public class EntityId implements Comparable<EntityId> {

	public static EntityId item(long id) {
		return new EntityId(EntityType.item, id);
	}

	public static EntityId parse(String entityId) {
		String code = entityId.substring(0, 1);
		for (EntityType type : EntityType.values()) {
			if (type.code.equals(code)) {
				return new EntityId(type, Long.parseLong(entityId.substring(1)));
			}
		}
		throw new IllegalArgumentException("Unknown entity type: " + entityId);
	}

	public static EntityId property(long id) {
		return new EntityId(EntityType.property, id);
	}

	private final long id;

	private final EntityType type;

	public EntityId(EntityType type, long id) {
		this.type = type;
		this.id = id;
	}

	@Override
	public int compareTo(EntityId o) {
		return Long.compare(this.getId(), o.getId());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EntityId other = (EntityId) obj;
		if (id != other.id)
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	public long getId() {
		return id;
	}

	public EntityType getType() {
		return type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return type.code + id;
	}

}
