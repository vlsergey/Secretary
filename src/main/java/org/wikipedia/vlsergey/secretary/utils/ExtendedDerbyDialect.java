package org.wikipedia.vlsergey.secretary.utils;

import java.sql.Types;

import org.hibernate.dialect.DerbyDialect;

public class ExtendedDerbyDialect extends DerbyDialect {
	public ExtendedDerbyDialect() {
		registerColumnType(Types.VARCHAR, 32672, "varchar($l)");
		registerColumnType(Types.LONGVARCHAR, 32700, "longvarchar($l)");
		registerColumnType(Types.CLOB, 2147483647, "clob");
		registerColumnType(Types.BLOB, 2147483647, "blob");
	}
}
