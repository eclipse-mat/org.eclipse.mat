/*******************************************************************************
 * Copyright (c) 2008, 2015 SAP AG, IBM Corporation and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *    IBM Corporation - enhancements and fixes
 *    James Livingston - expose collection utils as API
 *******************************************************************************/
package org.eclipse.mat.internal.collectionextract;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.snapshot.model.IObject;

public class ConcurrentSkipListCollectionExtractor extends HashMapCollectionExtractor
{
    public ConcurrentSkipListCollectionExtractor(String arrayField, String keyField, String valueField)
    {
        super(null, arrayField, keyField, valueField);
    }

    @Override
    public boolean hasCapacity()
    {
        return false;
    }

    @Override
    public Integer getCapacity(IObject coll) throws SnapshotException
    {
        return null;
    }

    @Override
    public boolean hasFillRatio()
    {
        return false;
    }

    @Override
    public Double getFillRatio(IObject coll) throws SnapshotException
    {
        return null;
    }

    public Double getCollisionRatio(IObject coll) throws SnapshotException
    {
        return 0.0;
    }
}
