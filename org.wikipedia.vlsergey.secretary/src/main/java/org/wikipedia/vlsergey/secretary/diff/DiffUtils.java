/*
 * Copyright 2001-2008 Fizteh-Center Lab., MIPT, Russia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Created on 12.03.2008
 */
package org.wikipedia.vlsergey.secretary.diff;

import java.io.StringWriter;

import org.wikipedia.vlsergey.secretary.diff.DiffPrint.ContextPrint;
import org.wikipedia.vlsergey.secretary.diff.DiffPrint.UnifiedPrint;

public class DiffUtils {

	public static String getDiff(String oldVariant, String newVariant) {
		Diff d = new Diff(oldVariant.split("\\n"), newVariant.split("\\n"));
		Diff.change script = d.diff_2(false);

		ContextPrint print = new UnifiedPrint(oldVariant.split("\\n"), newVariant.split("\\n"));
		StringWriter writer = new StringWriter();
		print.setOutput(writer);
		print.print_script(script);
		String result = writer.toString();
		return result;
	}

}
