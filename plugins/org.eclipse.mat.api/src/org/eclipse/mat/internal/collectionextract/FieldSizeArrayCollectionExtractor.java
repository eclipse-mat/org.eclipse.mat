package org.eclipse.mat.internal.collectionextract;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.snapshot.model.IObject;
import org.eclipse.mat.snapshot.model.IObjectArray;

public class FieldSizeArrayCollectionExtractor extends FieldArrayCollectionExtractor {
    private final String sizeField;

    public FieldSizeArrayCollectionExtractor(String sizeField, String arrayField) {
        super(arrayField);
        if (sizeField == null)
            throw new IllegalArgumentException();
        this.sizeField = sizeField;
    }

    @Override
    public boolean hasSize() {
        return true;
    }

    @Override
    public Integer getSize(IObject coll) throws SnapshotException {
        Object value = coll.resolveValue(sizeField);
        // Allow for int or long
        if (value instanceof Integer) {
            return (Integer)value;
        } else if (value instanceof Long) {
            return ((Long)value).intValue();
        } else if (hasExtractableArray()) {
            IObjectArray array = extractEntries(coll);
            if (array != null) {
                // E.g. ArrayList
                return ExtractionUtils.getNumberOfNotNullArrayElements(array);
            } else {
                return null;
            }
        } else if (hasExtractableContents()) {
            int[] array = extractEntryIds(coll);
            if (array != null) {
                // E.g. ArrayList
                return ExtractionUtils.getNumberOfNotNullArrayElements(array);
            } else {
                return null;
            }
        } else {
            // LinkedList
            IObject header = resolveNextFields(coll);
            if (header != null)
            {
                // there should be a separate impl for linked lists
                throw new IllegalStateException("not implemented yet");
                //return getMapSize(coll, header);
            } else {
                return null;
            }
        }
    }
}
