package org.wikipedia.vlsergey.secretary.dom;

import java.io.Serializable;

public abstract class Content implements Cloneable, Serializable {
    private static final long serialVersionUID = -4662884796134218208L;

    @Override
    public String toString() {
        return toWiki();
    }

    public abstract String toWiki();
}
