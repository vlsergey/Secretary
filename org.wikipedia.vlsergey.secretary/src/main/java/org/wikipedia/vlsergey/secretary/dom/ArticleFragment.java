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
 * Created on 27.02.2008
 */
package org.wikipedia.vlsergey.secretary.dom;

import java.util.ArrayList;
import java.util.List;

public class ArticleFragment extends AbstractContainer {

    private static final long serialVersionUID = 358081004448028714L;

    private List<Content> children;

    public ArticleFragment(List<? extends Content> children) {
        this.children = new ArrayList<Content>(children);
    }

    @Override
    public List<Content> getChildren() {
        return children;
    }

    public Section getSectionByName(String sectionName) {
        for (Content content : getChildren()) {
            if (content instanceof Section) {
                Section section = (Section) content;
                if (section.getHeader().toWiki().trim().equalsIgnoreCase(
                        sectionName))
                    return section;
            }
        }
        return null;
    }

    public List<Section> getSections() {
        List<Section> result = new ArrayList<Section>();
        for (Content content : getChildren()) {
            if (content instanceof Section)
                result.add((Section) content);
        }
        return result;
    }

    public void setChildren(List<Content> children) {
        this.children = children;
    }
}
