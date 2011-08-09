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
 * Created on 26.02.2008
 */
package org.wikipedia.vlsergey.secretary.dom;

import java.util.ArrayList;
import java.util.List;

public class TableCell extends AbstractContainer {

    private Content attributes;

    private Content attributesBorder;

    private Content border;

    private Content content;

    public TableCell(Content beginning, Content attributes,
            Content attributesBorder, Content content) {
        this.border = beginning;
        this.attributes = attributes;
        this.attributesBorder = attributesBorder;
        this.content = content;
    }

    public Content getAttributes() {
        return attributes;
    }

    public Content getAttributesBorder() {
        return attributesBorder;
    }

    public Content getBorder() {
        return border;
    }

    @Override
    public List<Content> getChildren() {
        List<Content> result = new ArrayList<Content>();
        addToChildren(result, border);
        addToChildren(result, attributes);
        addToChildren(result, attributesBorder);
        addToChildren(result, content);
        return result;
    }

    public Content getContent() {
        return content;
    }

    public void setAttributes(Content attributes) {
        this.attributes = attributes;
    }

    public void setAttributesBorder(Content attributesBorder) {
        this.attributesBorder = attributesBorder;
    }

    public void setBorder(Content beginning) {
        this.border = beginning;
    }

    public void setContent(Content content) {
        this.content = content;
    }

}
