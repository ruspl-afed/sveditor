/****************************************************************************
 * Copyright (c) 2008-2014 Matthew Ballance and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Ballance - initial implementation
 ****************************************************************************/


package net.sf.sveditor.ui.prop_pages;

import net.sf.sveditor.core.db.project.SVProjectFileWrapper;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public interface ISVProjectPropsPage {
	
	String getName();
	
	Image  getIcon();
	
	void init(SVProjectFileWrapper project_wrapper);
	
	Control createContents(Composite parent);
	
	void perfomOk();

}
