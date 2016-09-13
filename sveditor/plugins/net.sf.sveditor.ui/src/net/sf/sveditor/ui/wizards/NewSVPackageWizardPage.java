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


package net.sf.sveditor.ui.wizards;


import org.eclipse.swt.widgets.Composite;

public class NewSVPackageWizardPage extends AbstractNewSVItemFileWizardPage {
	
	public NewSVPackageWizardPage(AbstractNewSVItemFileWizard parent) {
		super(parent, "New SystemVerilog Package", 
				"SystemVerilog Package", 
				"Create a new SystemVerilog package");
		fFileExt = ".sv";
	}
	
	@Override
	protected void createCustomContent(Composite src_c) {
	}

}
