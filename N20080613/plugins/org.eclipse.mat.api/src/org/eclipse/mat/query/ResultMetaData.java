/*******************************************************************************
 * Copyright (c) 2008 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.mat.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds meta-data of the query result needed to fine-tune the display of the
 * result.
 */
public final class ResultMetaData
{

    /**
     * {@link ResultMetaData} factory
     */
    public static final class Builder
    {
        ResultMetaData data = new ResultMetaData();

        /**
         * Add a named {@link ContextProvider} to display additional context
         * menus.
         */
        public Builder addContext(ContextProvider provider)
        {
            data.provider.add(provider);
            return this;
        }

        /**
         * Indicates that the table or tree is already sorted by the query and
         * (a) prevents sorting by the UI and (b) sets the sort indicators to
         * the right columns.
         */
        public Builder setIsPreSortedBy(int columnIndex, Column.SortDirection direction)
        {
            data.preSortedColumnIndex = columnIndex;
            data.preSortedSortDirection = direction;

            return this;
        }

        /**
         * Indicates that the calculation of the precise retained sizes of the
         * default content provider {@link IStructuredResult} should be started
         * right away.
         */
        public Builder setShowPreciseRetainedSizes()
        {
            data.showPreciseRetainedSize = true;
            data.showApproximateRetainedSize = false;
            return this;
        }

        /**
         * Indicates that the calculation of the approximate retained sizes of
         * the default content provider {@link IStructuredResult} should be
         * started right away.
         */
        public Builder setShowApproximateRetainedSizes()
        {
            data.showPreciseRetainedSize = false;
            data.showApproximateRetainedSize = true;
            return this;
        }

        /**
         * Creates and returns the ResultMetaData object.
         */
        public ResultMetaData build()
        {
            ResultMetaData answer = data;
            data = null;

            answer.provider = Collections.unmodifiableList(answer.provider);

            return answer;
        }
    }

    private List<ContextProvider> provider = new ArrayList<ContextProvider>();

    private int preSortedColumnIndex;
    private Column.SortDirection preSortedSortDirection;

    private boolean showPreciseRetainedSize = false;
    private boolean showApproximateRetainedSize = false;

    private ResultMetaData()
    {}

    /**
     * Returns the named context providers.
     */
    public List<ContextProvider> getContextProviders()
    {
        return provider;
    }

    /**
     * True if the result is already sorted.
     */
    public boolean isPreSorted()
    {
        return preSortedSortDirection != null;
    }

    /**
     * The index of the column by which the result is pre-sorted (if it is
     * actually pre-sorted)
     */
    public int getPreSortedColumnIndex()
    {
        return preSortedColumnIndex;
    }

    /**
     * The direction by which the result is pre-sorted (if it is actually
     * pre-sorted)
     */
    public Column.SortDirection getPreSortedDirection()
    {
        return preSortedSortDirection;
    }

    /**
     * True if the precise retained size is to be calculated right away.
     */
    public boolean isShowPreciseRetainedSize()
    {
        return showPreciseRetainedSize;
    }

    /**
     * True if the approximate retained size is to be calculated right away.
     */
    public boolean isShowApproximateRetainedSize()
    {
        return showApproximateRetainedSize;
    }

}
