package org.honton.chas.report;

import java.util.Collection;

public interface MultiModeMojo<T extends MultiModeMojo<T>> {
    /**
     * Inform an aggregate project that all sub-modules have executed the appropriate phase.
     * @param subModules The completed sub-modules.
     * @param arguments For reports, Sink sink, Locale locale
     */
    void aggregateMode(Collection<T> subModules, Object... arguments);

    /**
     * Inform a non-aggregate project to execute the mojo.
     * @param arguments For reports, Sink sink, Locale locale
     */
    void nonAggregateMode(Object... arguments);
}
