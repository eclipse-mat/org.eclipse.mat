/*******************************************************************************
 * Copyright (c) 2015 Red Hat Inc
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Red Hat Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.mat.javaee.jboss.query;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.mat.SnapshotException;
import org.eclipse.mat.collect.ArrayInt;
import org.eclipse.mat.javaee.GraftedResultTree;
import org.eclipse.mat.javaee.IOverviewNode;
import org.eclipse.mat.javaee.JavaEEPlugin;
import org.eclipse.mat.javaee.SimpleObjectQuery;
import org.eclipse.mat.javaee.ejb.api.StatefulEjbExtractor;
import org.eclipse.mat.javaee.impl.EjbExtractors;
import org.eclipse.mat.query.Column;
import org.eclipse.mat.query.IContextObject;
import org.eclipse.mat.query.annotations.CommandName;
import org.eclipse.mat.snapshot.ISnapshot;
import org.eclipse.mat.snapshot.model.IClass;
import org.eclipse.mat.snapshot.model.IObject;
import org.eclipse.mat.snapshot.query.Icons;

@CommandName("jboss_management_operations")
public class ManagementOperationQuery extends SimpleObjectQuery<ManagementOperationQuery.StatefulEJbOverviewNode> {
    private static final Column[] COLUMNS = {
        new Column("Component", String.class),
        new Column("Module", String.class),
        new Column("Application", String.class),
        new Column("Distinct Name", String.class),
        new Column("Class", String.class),
        new Column("Instances", Integer.class),
        new Column("Size", Long.class),
    };
    private static final int[] GRAFTED_COLUMN_MAPPING = {0, 1, -1, -1, -1, -1, -1, 2};

    final static class StatefulEJbOverviewNode implements IOverviewNode {
        private final IObject request;
        // TODO: do we need to cache this?
        private final StatefulEjbExtractor extractor;

        public StatefulEJbOverviewNode(IObject request) {
            this.request = request;
            this.extractor = EjbExtractors.getStatefulEjbExtractor(request);
        }

        public long getRetainedHeap() {
            return request.getRetainedHeapSize();
        }

        public int getId() {
            return request.getObjectId();
        }

        private StatefulEjbExtractor getExtractor() {
            return extractor;
        }

        public String getComponentName() {
            return getExtractor().getComponentName(request);
        }

        public String getModuleName() {
            return getExtractor().getModuleName(request);
        }

        public String getApplicationName() {
            return getExtractor().getApplicationName(request);
        }

        public String getDistinctName() {
            return getExtractor().getDistinctName(request);
        }

        public IClass getComponentClass() {
            return getExtractor().getComponentClass(request);
        }

        public Integer getInstanceCount() {
            return getExtractor().getInstanceCount(request);
        }

        public Map<IObject, IObject> getInstances() {
            return getExtractor().getInstances(request);
        }
    }

    private static class StatefulEJbInstanceNode {
        private final IObject key;
        private final IObject instance;

        public StatefulEJbInstanceNode(IObject key, IObject instance) {
            this.key = key;
            this.instance = instance;
        }

        public IObject getKey() {
            return key;
        }

        public IObject getInstance() {
            return instance;
        }

        public long getRetainedHeap() {
            return instance.getRetainedHeapSize();
        }
    }


    public ManagementOperationQuery() {
        super(StatefulEJbOverviewNode.class);
    }

    protected ArrayInt findObjects(ISnapshot snapshot) throws SnapshotException {
        return EjbExtractors.findStatefulEjbs(snapshot);
    }

    protected StatefulEJbOverviewNode createOverviewNode(IObject obj) {
        return new StatefulEJbOverviewNode(obj);
    }

    protected boolean overviewHasChildren(StatefulEJbOverviewNode row) {
        return true;
    }

    protected Column[] getColumns() {
        return COLUMNS;
    }

    @Override
    protected Object getOverviewColumnValue(StatefulEJbOverviewNode row, int columnIndex) {
        switch (columnIndex) {
        case 0:
            return row.getComponentName();
        case 1:
            return row.getModuleName();
        case 2:
            return row.getApplicationName();
        case 3:
            return row.getDistinctName();
        case 4:
            return row.getComponentClass().getName();
        case 5:
            return row.getInstanceCount();
        case 6:
            return row.getRetainedHeap();
        default:
            return super.getOverviewColumnValue(row, columnIndex);
        }
    }

    @Override
    protected List<StatefulEJbInstanceNode> getOverviewChildren(StatefulEJbOverviewNode row) {
        Map<IObject, IObject> instances = row.getInstances();
        List<StatefulEJbInstanceNode> results = new ArrayList<StatefulEJbInstanceNode>(instances.size());
        for (Map.Entry<IObject,IObject> i: instances.entrySet()) {
            results.add(new StatefulEJbInstanceNode(i.getKey(), i.getValue()));
        }
        return results;
    }


    @Override
    protected Object getColumnValue(Object row, int columnIndex) {
        if (row instanceof StatefulEJbInstanceNode) {
            StatefulEJbInstanceNode node = (StatefulEJbInstanceNode)row;
            switch (columnIndex) {
            case 0:
                return node.getKey().getDisplayName();
            case 1:
                return null;
            case 2:
                return null;
            case 3:
                return null;
            case 4:
                return node.getInstance();
            case 5:
                return null;
            case 6:
                return node.getRetainedHeap();
            default:
                JavaEEPlugin.error("Unexpected column index " + columnIndex);
                return null;
            }
        } else {
            return super.getColumnValue(row, columnIndex);
        }
    }


    @Override
    protected boolean hasChildren(Object row) {
        if (row instanceof StatefulEJbInstanceNode) {
            StatefulEJbInstanceNode node = (StatefulEJbInstanceNode) row;
            return !node.getInstance().getOutboundReferences().isEmpty();
        } else {
            return super.hasChildren(row);
        }
    }

    @Override
    protected List<?> getChildren(Object row) {
        if (row instanceof StatefulEJbInstanceNode) {
            StatefulEJbInstanceNode node = (StatefulEJbInstanceNode) row;
            // Create an outbound tree and graft it onto the main tree
            IObject instance = node.getInstance();
            return GraftedResultTree.graftOutbound(instance.getSnapshot(), instance.getObjectId(), GRAFTED_COLUMN_MAPPING);
        } else {
            return super.getChildren(row);
        }
    }

    @Override
    protected IContextObject getContext(Object row) {
        if (row instanceof StatefulEJbInstanceNode) {
            final StatefulEJbInstanceNode node = (StatefulEJbInstanceNode) row;
            return new IContextObject() {
                public int getObjectId() {
                    return node.getInstance().getObjectId();
                }
            };
        } else {
            return super.getContext(row);
        }
    }

    @Override
    public URL getIcon(Object row) {
        if (row instanceof StatefulEJbInstanceNode) {
            StatefulEJbInstanceNode node = (StatefulEJbInstanceNode) row;
            return Icons.outbound(snapshot, node.getInstance().getObjectId());
        } else {
            return super.getIcon(row);
        }
    }
}
