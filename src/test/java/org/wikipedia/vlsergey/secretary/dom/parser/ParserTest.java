package org.wikipedia.vlsergey.secretary.dom.parser;

import org.junit.Test;

public class ParserTest {

	@Test
	public void testArmenia() {
		new Parser()
				.parse("'''Театр Армении''' — наряду с [[Театр в Древней Греции|греческим]] и [[Римский театр|римским]] один из древнейших театров мира европейского типа."
						+ "<ref>{{книга |автор= Г. Гоян. |часть = |заглавие = 2000 лет армянского театра. Театр древней Армении |оригинал = |ссылка= http://armenianhouse.org/goyan/ancient-armenian/p1-8.html |место= М. |издательство = Искусство |год= 1952 |том= I |страницы = 86}}</ref>.\r\n");
	}
}
