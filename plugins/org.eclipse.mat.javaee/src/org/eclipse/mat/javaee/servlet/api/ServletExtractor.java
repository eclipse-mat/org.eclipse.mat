/*******************************************************************************
 * Copyright (c) 2014 Red Hat Inc
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Red Hat Inc - initial API and implementation
 *******************************************************************************/
package org.eclipse.mat.javaee.servlet.api;

import org.eclipse.mat.snapshot.model.IObject;

public interface ServletExtractor {
    String getName(IObject object);
    String getServletClass(IObject object);
	String getApplication(IObject object);
}
