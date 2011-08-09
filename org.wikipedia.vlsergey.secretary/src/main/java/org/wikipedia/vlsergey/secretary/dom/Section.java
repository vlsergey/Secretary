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
 * Created on 20.03.2008
 */
package org.wikipedia.vlsergey.secretary.dom;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class Section extends AbstractContainer {
    private static final Map<Integer, Text> headers;

    static {
        headers = new LinkedHashMap<Integer, Text>();
        for (int i = 1; i <= 6; i++)
            headers.put(i, new Text(StringUtils.repeat("=", i)));
    }

    private Text afterHeaderSpaces;

    private ArticleFragment content;

    private Content header;

    private int level;

    public Section(int level, Content header, Text afterHeaderSpaces,
            ArticleFragment content) {
        this.level = level;
        this.header = header;
        this.afterHeaderSpaces = afterHeaderSpaces;
        this.content = content;
    }

    public Text getAfterHeaderSpaces() {
        return afterHeaderSpaces;
    }

    @Override
    public List<Content> getChildren() {
        List<Content> result = new ArrayList<Content>();

        addToChildren(result, headers.get(this.level));
        addToChildren(result, header);
        addToChildren(result, headers.get(this.level));
        addToChildren(result, afterHeaderSpaces);
        addToChildren(result, content);

        return result;
    }

    public ArticleFragment getContent() {
        return content;
    }

    public Content getHeader() {
        return header;
    }

    public int getLevel() {
        return level;
    }

    public void setAfterHeaderSpaces(Text afterHeaderSpaces) {
        this.afterHeaderSpaces = afterHeaderSpaces;
    }

    public void setContent(ArticleFragment content) {
        this.content = content;
    }

    public void setHeader(Content header) {
        this.header = header;
    }

    public void setLevel(int level) {
        this.level = level;
    }

}
