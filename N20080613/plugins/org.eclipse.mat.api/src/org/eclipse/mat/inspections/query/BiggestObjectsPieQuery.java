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
package org.eclipse.mat.inspections.query;

import org.eclipse.mat.inspections.query.util.PieFactory;
import org.eclipse.mat.query.IQuery;
import org.eclipse.mat.query.IResultPie;
import org.eclipse.mat.query.IResultPie.Slice;
import org.eclipse.mat.query.annotations.Argument;
import org.eclipse.mat.query.annotations.Category;
import org.eclipse.mat.query.annotations.CommandName;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.IObject;
import org.eclipse.mat.util.IProgressListener;

@CommandName("pie_biggest_objects")
@Category(Category.HIDDEN)
public class BiggestObjectsPieQuery implements IQuery
{
    @Argument
    public ISnapshot snapshot;

    public IResultPie execute(IProgressListener listener) throws Exception
    {
        int[] objects = snapshot.getImmediateDominatedIds(-1);

        final long totalHeapSize = snapshot.getSnapshotInfo().getUsedHeapSize();

        int index = 0;
        int count = 0;
        long retainedHeapBySlices = 0;

        PieFactory pie = new PieFactory(snapshot);

        while (index < objects.length //
                        && (count < 3 //
                        || (retainedHeapBySlices < totalHeapSize / 4 && count < 10)))
        {
            IObject obj = snapshot.getObject(objects[index++]);

            Slice slice = pie.addSlice(obj.getObjectId());
            retainedHeapBySlices += slice.getValue();
            count++;
            
            if (slice.getValue() < totalHeapSize / 100)
                break;
            
            if (listener.isCanceled())
                break;
        }

        return pie.build();
    }
}
