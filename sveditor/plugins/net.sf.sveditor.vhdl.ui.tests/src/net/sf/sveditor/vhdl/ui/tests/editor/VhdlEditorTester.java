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


package net.sf.sveditor.vhdl.ui.tests.editor;

import java.util.ArrayList;
import java.util.List;

import net.sf.sveditor.core.SVCorePlugin;
import net.sf.sveditor.core.StringInputStream;
import net.sf.sveditor.core.db.ISVDBFileFactory;
import net.sf.sveditor.core.db.SVDBFile;
import net.sf.sveditor.core.db.SVDBMarker;
import net.sf.sveditor.core.db.index.ISVDBIndexIterator;
import net.sf.sveditor.core.db.index.cache.ISVDBIndexCache;
import net.sf.sveditor.core.db.index.cache.ISVDBIndexCacheMgr;
import net.sf.sveditor.ui.tests.utils.editor.AutoEditTester;
import net.sf.sveditor.vhdl.ui.tests.VhdlUiReleaseTests;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.commands.ICommandService;

public class VhdlEditorTester {
	private IDocument				fDoc;
	private AutoEditTester			fAutoEditTester;
	private ISVDBIndexIterator		fIndexIt;
	private SVDBFile				fSVDBFile;
	private ITextSelection			fTextSel;
	
	public VhdlEditorTester(
			AutoEditTester			auto_ed,
			ISVDBIndexIterator		index_it,
			SVDBFile				file) {
		fAutoEditTester = auto_ed;
		fIndexIt 		= index_it;
		fSVDBFile 		= file;
		fTextSel    	= null;
	}

	public VhdlEditorTester(String doc, String filename, ISVDBIndexCacheMgr cache_mgr) throws BadLocationException {
		fAutoEditTester = VhdlUiReleaseTests.createAutoEditTester();
		fAutoEditTester.setContent(doc);

//		ISVDBFileFactory factory = SVCorePlugin.createFileFactory();
		
//		List<SVDBMarker> markers = new ArrayList<SVDBMarker>();
//		fSVDBFile = factory.parse(new StringInputStream(doc), filename, markers);

//		ISVDBIndexCache cache = FileIndexIterator.createCache(cache_mgr);
//		fIndexIt = new FileIndexIterator(fSVDBFile, cache);
	}

	public IDocument getDocument() {
		if (fAutoEditTester != null) {
			return fAutoEditTester.getDocument();
		} else {
			return fDoc;
		}
	}
	
	public AutoEditTester getAutoEdit() {
		return fAutoEditTester;
	}
	
	public void setSelection(ITextSelection sel) {
		fTextSel = sel;
	}

	public ISVDBIndexIterator getIndexIterator() {
		return fIndexIt;
	}

	public SVDBFile getSVDBFile() {
		return fSVDBFile;
	}

	public ITextSelection getTextSel() {
		return fTextSel;
	}

}
