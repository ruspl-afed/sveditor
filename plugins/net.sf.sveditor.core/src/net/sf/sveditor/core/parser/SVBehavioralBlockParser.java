/****************************************************************************
 * Copyright (c) 2008-2010 Matthew Ballance and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Ballance - initial implementation
 ****************************************************************************/


package net.sf.sveditor.core.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.sveditor.core.db.ISVDBAddChildItem;
import net.sf.sveditor.core.db.ISVDBEndLocation;
import net.sf.sveditor.core.db.ISVDBScopeItem;
import net.sf.sveditor.core.db.SVDBItemType;
import net.sf.sveditor.core.db.SVDBLocation;
import net.sf.sveditor.core.db.SVDBParamValueAssign;
import net.sf.sveditor.core.db.SVDBTypeInfo;
import net.sf.sveditor.core.db.SVDBTypeInfoUserDef;
import net.sf.sveditor.core.db.expr.SVDBAssignExpr;
import net.sf.sveditor.core.db.expr.SVDBExpr;
import net.sf.sveditor.core.db.expr.SVDBIdentifierExpr;
import net.sf.sveditor.core.db.expr.SVDBLiteralExpr;
import net.sf.sveditor.core.db.expr.SVDBParamIdExpr;
import net.sf.sveditor.core.db.stmt.SVDBActionBlockStmt;
import net.sf.sveditor.core.db.stmt.SVDBAssignStmt;
import net.sf.sveditor.core.db.stmt.SVDBBlockStmt;
import net.sf.sveditor.core.db.stmt.SVDBBreakStmt;
import net.sf.sveditor.core.db.stmt.SVDBCaseItem;
import net.sf.sveditor.core.db.stmt.SVDBCaseStmt;
import net.sf.sveditor.core.db.stmt.SVDBCaseStmt.CaseType;
import net.sf.sveditor.core.db.stmt.SVDBContinueStmt;
import net.sf.sveditor.core.db.stmt.SVDBDelayControlStmt;
import net.sf.sveditor.core.db.stmt.SVDBDisableForkStmt;
import net.sf.sveditor.core.db.stmt.SVDBDisableStmt;
import net.sf.sveditor.core.db.stmt.SVDBDoWhileStmt;
import net.sf.sveditor.core.db.stmt.SVDBEventControlStmt;
import net.sf.sveditor.core.db.stmt.SVDBEventTriggerStmt;
import net.sf.sveditor.core.db.stmt.SVDBExprStmt;
import net.sf.sveditor.core.db.stmt.SVDBForStmt;
import net.sf.sveditor.core.db.stmt.SVDBForeachStmt;
import net.sf.sveditor.core.db.stmt.SVDBForeverStmt;
import net.sf.sveditor.core.db.stmt.SVDBForkStmt;
import net.sf.sveditor.core.db.stmt.SVDBForkStmt.JoinType;
import net.sf.sveditor.core.db.stmt.SVDBIfStmt;
import net.sf.sveditor.core.db.stmt.SVDBLabeledStmt;
import net.sf.sveditor.core.db.stmt.SVDBNullStmt;
import net.sf.sveditor.core.db.stmt.SVDBProceduralContAssignStmt;
import net.sf.sveditor.core.db.stmt.SVDBProceduralContAssignStmt.AssignType;
import net.sf.sveditor.core.db.stmt.SVDBRepeatStmt;
import net.sf.sveditor.core.db.stmt.SVDBReturnStmt;
import net.sf.sveditor.core.db.stmt.SVDBStmt;
import net.sf.sveditor.core.db.stmt.SVDBWaitForkStmt;
import net.sf.sveditor.core.db.stmt.SVDBWaitStmt;
import net.sf.sveditor.core.db.stmt.SVDBWhileStmt;
import net.sf.sveditor.core.scanner.SVKeywords;

public class SVBehavioralBlockParser extends SVParserBase {
	
	public static boolean isDeclAllowed(SVDBStmt stmt) {
		return (stmt.getType() == SVDBItemType.VarDeclStmt || 
				stmt.getType() == SVDBItemType.TypedefStmt);
	}
	
	public SVBehavioralBlockParser(ISVParser parser) {
		super(parser);
	}
	
	public boolean statement(ISVDBAddChildItem parent) throws SVParseException {
		return statement(parent, false, true);
	}
	
	public boolean statement(ISVDBAddChildItem parent, boolean decl_allowed, boolean ansi_decl) throws SVParseException {
		return statement_int(parent, decl_allowed, ansi_decl);
	}
	
	private static final Set<String> fDeclKeywordsANSI;
	private static final Set<String> fDeclKeywordsNonANSI;
	
	static {
		fDeclKeywordsANSI = new HashSet<String>();
		fDeclKeywordsNonANSI = new HashSet<String>();
		
		fDeclKeywordsANSI.add("const");
		fDeclKeywordsANSI.add("var");
		fDeclKeywordsANSI.add("automatic");
		fDeclKeywordsANSI.add("static");
		fDeclKeywordsANSI.add("typedef");
		
		fDeclKeywordsNonANSI.addAll(fDeclKeywordsANSI);
		fDeclKeywordsNonANSI.add("input");
		fDeclKeywordsNonANSI.add("output");
		fDeclKeywordsNonANSI.add("inout");
		fDeclKeywordsNonANSI.add("ref");
	}
	
	private boolean statement_int(ISVDBAddChildItem parent, boolean decl_allowed, boolean ansi_decl) throws SVParseException {
		debug("--> statement " + fLexer.peek() + " @ " + fLexer.getStartLocation().getLine() + " decl_allowed=" + decl_allowed);
		Set<String> decl_keywords = (ansi_decl)?fDeclKeywordsANSI:fDeclKeywordsNonANSI;
		SVDBLocation start = fLexer.getStartLocation();

		// Try for a declaration here
		if (fLexer.peekKeyword(decl_keywords) || fLexer.peekKeyword(SVKeywords.fBuiltinDeclTypes) ||
				fLexer.isIdentifier() || fLexer.peekKeyword("typedef", "struct", "enum")) {
//			boolean builtin_type = fLexer.peekKeyword(SVKeywords.fBuiltinDeclTypes);
			
			if (fLexer.peekKeyword(decl_keywords) || fLexer.peekKeyword(SVKeywords.fBuiltinDeclTypes) ||
					fLexer.peekKeyword("typedef", "struct", "enum")) {
				// Definitely a declaration
				if (!decl_allowed) {
					error("declaration in a post-declaration location");
				}
				parsers().blockItemDeclParser().parse(parent, null, start);
				return decl_allowed;
			} else {
				// May be a declaration. Let's see
				// pkg::cls #(P)::field = 2; 
				// pkg::cls #(P)::type var;
				// field.foo
				SVToken tok = fLexer.consumeToken();
				
				if (fLexer.peekOperator("::","#") || fLexer.peekId()) {
					// Likely to be a declaration. Let's read a type
					fLexer.ungetToken(tok);
					final List<SVToken> tok_l = new ArrayList<SVToken>();
					ISVTokenListener l = new ISVTokenListener() {
						public void tokenConsumed(SVToken tok) {
							tok_l.add(tok);
						}
						public void ungetToken(SVToken tok) {
							tok_l.remove(tok_l.size()-1);
						}
					}; 
					SVDBTypeInfo type = null;
					try {
						fLexer.addTokenListener(l);
						type = parsers().dataTypeParser().data_type(0);
					} finally {
						fLexer.removeTokenListener(l);
					}
					
					// Okay, what's next?
					if (fLexer.peekId()) {
						// Conclude that this is a declaration
						debug("Assume a declaration @ " + fLexer.peek());
						if (!decl_allowed) {
							error("declaration in a non-declaration location");
						}
						
						parsers().blockItemDeclParser().parse(parent, type, start);
						return decl_allowed;
					} else {
						debug("Assume a typed reference @ " + fLexer.peek());
						// Else, this is probably a typed reference
						fLexer.ungetToken(tok_l);
						// Fall through
					}
				} else {
					// More likely to not be a type
					fLexer.ungetToken(tok);
				}
				
				/*
				// Variable declarations
				List<SVToken> id_list = parsers().SVParser().scopedStaticIdentifier_l(true);
			
				if (!builtin_type && 
					((fLexer.peekKeyword() && !fLexer.peekKeyword(fDeclKeywordsNonANSI)) ||
							(fLexer.peekOperator() && !fLexer.peekOperator("#")))) {
					// likely a statement
					for (int i=id_list.size()-1; i>=0; i--) {
						fLexer.ungetToken(id_list.get(i));
					}
					debug("non-declaration statement: " + fLexer.peek());
				} else {
					for (int i=id_list.size()-1; i>=0; i--) {
						fLexer.ungetToken(id_list.get(i));
					}
					
					// First, try reading a type to see what's after
					SVDBTypeInfo type = parsers().dataTypeParser().data_type(0);
					
					if (fLexer.peekOperator(SVKeywords.fAssignmentOps)) {
						// behavioral statement
						debug("Behavioral Statement: " + fLexer.peek());
						expression_stmt(start, parent, convertTypeInfoToLVal(type));
						return false;
					} else {
						debug("Pre-var parse: " + fLexer.peek());
						if (!decl_allowed) {
							error("declaration in a non-declaration location");
						}
						
						parsers().blockItemDeclParser().parse(parent, type, start);
					
						// Bail for now
						return decl_allowed; 
					}
				}
				 */
			}
		}
		
		// time to move on to the body
		debug("non-declaration statement: " + fLexer.peek());
		decl_allowed = false;

		if (fLexer.peekKeyword("begin")) {
			block_stmt(parent);
		} else if (fLexer.peekKeyword("unique","unique0","priority")) {
			// TODO: ignore unique_priority for now
			fLexer.eatToken();
			// 'if' or 'case'
			statement(parent);
		} else if (fLexer.peekKeyword("if")) {
			parse_if_stmt(parent);
		} else if (fLexer.peekKeyword("while")) {
			SVDBWhileStmt while_stmt = new SVDBWhileStmt();
			while_stmt.setLocation(start);
			fLexer.eatToken();
			fLexer.readOperator("(");
			while_stmt.setExpr(parsers().exprParser().expression());
			fLexer.readOperator(")");
			
			parent.addChildItem(while_stmt);
			
			statement(while_stmt, false,false);
		} else if (fLexer.peekKeyword("do")) {
			SVDBDoWhileStmt do_while = new SVDBDoWhileStmt();
			do_while.setLocation(start);
			fLexer.eatToken();
			
			statement(do_while, false,false);
			fLexer.readKeyword("while");
			fLexer.readOperator("(");
			do_while.setCond(parsers().exprParser().expression());
			fLexer.readOperator(")");
			fLexer.readOperator(";");
		} else if (fLexer.peekKeyword("repeat")) {
			SVDBRepeatStmt repeat = new SVDBRepeatStmt();
			repeat.setLocation(start);
			fLexer.eatToken();
			fLexer.readOperator("(");
			repeat.setExpr(parsers().exprParser().expression());
			fLexer.readOperator(")");
			parent.addChildItem(repeat);
			statement(repeat, false,false);
		} else if (fLexer.peekKeyword("forever")) {
			SVDBForeverStmt forever = new SVDBForeverStmt();
			forever.setLocation(start);
			fLexer.eatToken();
			parent.addChildItem(forever);
			statement(forever, false,false);
		} else if (fLexer.peekKeyword("for")) {
			for_stmt(parent);
		} else if (fLexer.peekKeyword("foreach")) {
			SVDBForeachStmt foreach = new SVDBForeachStmt();
			foreach.setLocation(start);
			fLexer.eatToken();
			fLexer.readOperator("(");
			foreach.setCond(parsers().exprParser().expression());
			fLexer.readOperator(")");
			parent.addChildItem(foreach);
			statement(foreach, false,false);
		} else if (fLexer.peekKeyword("fork")) {
			SVDBForkStmt fork = new SVDBForkStmt();
			fork.setLocation(start);
			
			parent.addChildItem(fork);
			decl_allowed = true;
			fLexer.eatToken();
			
			// Read block identifier
			if (fLexer.peekOperator(":")) {
				fLexer.eatToken();
				fLexer.readId();
			}
			
			while (fLexer.peek() != null && 
					!fLexer.peekKeyword("join", "join_none", "join_any")) {
				debug("--> Fork Statement");
				// Allow declarations at the root of the fork
				decl_allowed = statement_int(fork, decl_allowed, true);
				debug("<-- Fork Statement");
			}
			fork.setEndLocation(fLexer.getStartLocation());
			// Read join
			String join_type = fLexer.readKeyword("join", "join_none", "join_any");
			if (join_type.equals("join")) {
				fork.setJoinType(JoinType.Join);
			} else if (join_type.equals("join_none")) {
				fork.setJoinType(JoinType.JoinNone);
			} else if (join_type.equals("join_any")) {
				fork.setJoinType(JoinType.JoinAny);
			}
			
			if (fLexer.peekOperator(":")) {
				fLexer.eatToken();
				fLexer.readId();
			}
		} else if (fLexer.peekKeyword("case", "casex", "casez","randcase")) {
			parse_case_stmt(parent);
		} else if (fLexer.peekKeyword("wait")) {
			SVDBWaitStmt wait_stmt;
			fLexer.eatToken();
			
			if (fLexer.peekKeyword("fork")) {
				wait_stmt = new SVDBWaitForkStmt();
				fLexer.eatToken();
				fLexer.readOperator(";");
				parent.addChildItem(wait_stmt);
			} else {
				wait_stmt = new SVDBWaitStmt();
				fLexer.readOperator("(");
				wait_stmt.setExpr(parsers().exprParser().expression());
				fLexer.readOperator(")");
				parent.addChildItem(wait_stmt);
				if (!fLexer.peekOperator(";")) {
					statement(wait_stmt, false,false);
				} else {
					fLexer.readOperator(";");
				}
			}
		} else if (fLexer.peekOperator("->", "->>", "-->")) {
			SVDBEventTriggerStmt event_trigger = new SVDBEventTriggerStmt();
			/* String tt = */ fLexer.eatToken();
			
			// TODO: handle [delay_or_event_control] after ->>
			
			event_trigger.setHierarchicalEventIdentifier(parsers().exprParser().expression());
			fLexer.readOperator(";");
			parent.addChildItem(event_trigger);
		} else if (fLexer.peekOperator("@")) {
			SVDBEventControlStmt event_stmt = new SVDBEventControlStmt();
			fLexer.eatToken();
			event_stmt.setExpr(parsers().exprParser().event_expression());
			parent.addChildItem(event_stmt);

			// statement_or_null
			statement(event_stmt, decl_allowed, ansi_decl);
		} else if (fLexer.peekOperator("#")) {
			SVDBDelayControlStmt delay_stmt = new SVDBDelayControlStmt();
			
			delay_stmt.setExpr(fParsers.exprParser().delay_expr());
			statement(delay_stmt, false, true);
		} else if (fLexer.peekKeyword("disable")) {
			SVDBDisableStmt disable_stmt;
			fLexer.eatToken();
			if (fLexer.peekKeyword("fork")) {
				fLexer.eatToken();
				disable_stmt = new SVDBDisableForkStmt();
			} else {
				disable_stmt = new SVDBDisableStmt();
				disable_stmt.setHierarchicalId(parsers().exprParser().expression());
			}
			
			fLexer.readOperator(";");
			parent.addChildItem(disable_stmt);
		} else if (fLexer.peekKeyword("end")) {
			// An unmatched 'end' signals that we're missing some
			// behavioral construct
			error("Unexpected 'end' without matching 'begin'");
		} else if (fLexer.peekKeyword("assert","assume")) {
			parsers().assertionParser().parse(parent);
		} else if (fLexer.peekKeyword("return")) {
			debug("return statement");
			SVDBReturnStmt return_stmt = new SVDBReturnStmt();
			return_stmt.setLocation(fLexer.getStartLocation());
			
			fLexer.eatToken();
			if (!fLexer.peekOperator(";")) {
				return_stmt.setExpr(parsers().exprParser().expression());
			}
			fLexer.readOperator(";");
			parent.addChildItem(return_stmt);
		} else if (fLexer.peekKeyword("break")) {
			SVDBBreakStmt break_stmt = new SVDBBreakStmt();
			break_stmt.setLocation(fLexer.getStartLocation());
			fLexer.eatToken();
			fLexer.readOperator(";");
			parent.addChildItem(break_stmt);
		} else if (fLexer.peekKeyword("continue")) {
			SVDBContinueStmt continue_stmt = new SVDBContinueStmt();
			continue_stmt.setLocation(start);
			fLexer.eatToken();
			fLexer.readOperator(";");
		} else if (fLexer.peekKeyword("assign", "deassign", "force", "release")) {
			procedural_cont_assign(parent);
		} else if (ParserSVDBFileFactory.isFirstLevelScope(fLexer.peek(), 0) ||
			ParserSVDBFileFactory.isSecondLevelScope(fLexer.peek())) {
			error("Unexpected non-behavioral statement keyword " + fLexer.peek());
		} else if (fLexer.peekOperator(";")) {
			SVDBNullStmt null_stmt = new SVDBNullStmt();
			null_stmt.setLocation(start);
			fLexer.eatToken();
			parent.addChildItem(null_stmt);
		} else if (fLexer.peekId() || 
				fLexer.peekKeyword(SVKeywords.fBuiltinTypes) ||
				fLexer.peekKeyword("this", "super") || 
				fLexer.peekOperator()) {
			SVToken id = fLexer.consumeToken();
			
			if (fLexer.peekOperator(":")) {
				// Labeled statement
				String label = id.getImage();
				fLexer.eatToken();
				SVDBLabeledStmt l_stmt = new SVDBLabeledStmt();
				l_stmt.setLocation(start);
				l_stmt.setLabel(label);
				statement(l_stmt, decl_allowed, ansi_decl);
			} else {
				fLexer.ungetToken(id);

				expression_stmt(start, parent, null);
			}
		} else {
			error("Unknown statement stem: " + fLexer.peek());
		}
		
		debug("<-- statement " + fLexer.peek() + 
				" @ " + fLexer.getStartLocation().getLine() + " " + decl_allowed);
		return decl_allowed;
	}
	
	private SVDBExpr convertTypeInfoToLVal(SVDBTypeInfo info) throws SVParseException {
		if (info instanceof SVDBTypeInfoUserDef) {
			SVDBTypeInfoUserDef ud = (SVDBTypeInfoUserDef)info;
			if (ud.getParameters() != null && ud.getParameters().getParameters().size() > 0) {
				SVDBParamIdExpr p_id = new SVDBParamIdExpr(ud.getName());
				for (SVDBParamValueAssign pa : ud.getParameters().getParameters()) {
					p_id.addParamExpr(pa.getValue());
				}
				return p_id;
			} else {
				return new SVDBIdentifierExpr(ud.getName());
			}
		} else {
			error("Expecting user-defined type");
			return new SVDBIdentifierExpr(info.getName());
		}
	}
	
	private void expression_stmt(SVDBLocation start, ISVDBAddChildItem parent, SVDBExpr lvalue) throws SVParseException {
		debug("--> expression_stmt: " + fLexer.peek());
		if (lvalue == null) {
			lvalue = fParsers.exprParser().variable_lvalue();
		}

		// If an assignment
		if (fLexer.peekOperator(SVKeywords.fAssignmentOps)) {
			String op = fLexer.eatToken();
			SVDBAssignStmt assign_stmt = new SVDBAssignStmt();
			assign_stmt.setLocation(start);
			assign_stmt.setLHS(lvalue);
			assign_stmt.setOp(op);
			
			if (fLexer.peekOperator("#")) {
				assign_stmt.setDelayExpr(fParsers.exprParser().delay_expr());
			} else if (fLexer.peekOperator("##")) {
				// Clocking drive
				assign_stmt.setDelayExpr(fParsers.exprParser().expression());
			}

			assign_stmt.setRHS(parsers().exprParser().expression());
			parent.addChildItem(assign_stmt);
		} else {
			// Assume this is an expression of some sort
			debug("  Parsing expression statement starting with \"" + fLexer.peek() + "\"");
			SVDBExprStmt expr_stmt = new SVDBExprStmt(lvalue);
			expr_stmt.setLocation(start);
			parent.addChildItem(expr_stmt);
		}
		
		fLexer.readOperator(";");
		debug("<-- expression_stmt: " + fLexer.peek());
	}
	
	public void action_block(SVDBActionBlockStmt parent) throws SVParseException {
		if (fLexer.peekOperator(";")) {
			SVDBLocation start = fLexer.getStartLocation();
			fLexer.eatToken();
			SVDBStmt stmt = new SVDBNullStmt();
			stmt.setLocation(start);
			parent.addChildItem(stmt);
		} else if (fLexer.peekKeyword("else")) {
			fLexer.eatToken();
			statement(parent, false, true);
		} else {
			statement(parent, false, true);
			if (fLexer.peekKeyword("else")) {
				fLexer.eatToken();
				statement(parent, false, true);
			}
		}
	}
	
	private SVDBForStmt for_stmt(ISVDBAddChildItem parent) throws SVParseException {
		SVDBLocation start = fLexer.getStartLocation();
		fLexer.eatToken();
		fLexer.readOperator("(");
		SVDBForStmt stmt = new SVDBForStmt();
		stmt.setLocation(start);
		if (fLexer.peek() != null && !fLexer.peekOperator(";")) {
			SVToken first = fLexer.peekToken();
			SVDBTypeInfo type = parsers().dataTypeParser().data_type(0);
			
			if (fLexer.peekOperator()) {
				// If an operator, then likely not a declaration
				fLexer.ungetToken(first);
				type = null;
			}
			SVDBBlockStmt init_block = null;
			SVDBStmt init_stmt;
			while (true) {
				SVDBExpr expr = parsers().exprParser().expression();
				
				if (fLexer.peekOperator(",")) {
					fLexer.eatToken();
				} else {
					break;
				}
			}
		}
		fLexer.readOperator(";");
		
		if (!fLexer.peekOperator(";")) {
			
			while (true) {
				SVDBExpr expr = parsers().exprParser().expression();
				
				if (fLexer.peekOperator(",")) {
					fLexer.eatToken();
				} else {
					break;
				}
			}
		}
		fLexer.readOperator(";");
		
		if (!fLexer.peekOperator(")")) {
			while (true) {
				SVDBExpr expr = parsers().exprParser().expression();
				
				if (fLexer.peekOperator(",")) {
					fLexer.eatToken();
				} else {
					break;
				}
			}
		}
		
		fLexer.readOperator(")");
		parent.addChildItem(stmt);
		
		statement(stmt, false,false);
		
		return stmt;
	}
	
	private void procedural_cont_assign(ISVDBAddChildItem parent) throws SVParseException {
		SVDBLocation start = fLexer.getStartLocation();
		String type_s = fLexer.readKeyword("assign", "deassign", "force", "release");
		AssignType type = null;
		if (type_s.equals("assign")) {
			type = AssignType.Assign;
		} else if (type_s.equals("deassign")) {
			type = AssignType.Deassign;
		} else if (type_s.equals("force")) {
			type = AssignType.Force;
		} else if (type_s.equals("release")) {
			type = AssignType.Release;
		}
		SVDBProceduralContAssignStmt assign = new SVDBProceduralContAssignStmt(type);
		assign.setLocation(start);
		parent.addChildItem(assign);
		
		SVDBExpr expr = fParsers.exprParser().variable_lvalue();
		if (type == AssignType.Assign || type == AssignType.Force) {
			fLexer.readOperator("=");
			expr = new SVDBAssignExpr(expr, "=", fParsers.exprParser().expression());
		}
		assign.setExpr(expr);
		
		fLexer.readOperator(";");
	}
	
	private void block_stmt(ISVDBAddChildItem parent) throws SVParseException {
		boolean decl_allowed = true;
		SVDBBlockStmt block = new SVDBBlockStmt();
		block.setLocation(fLexer.getStartLocation());
		
		parent.addChildItem(block);
		
		// Declarations are permitted at block-start
		fLexer.eatToken();
		if (fLexer.peekOperator(":")) {
			fLexer.eatToken();
			fLexer.readId();
		}

		try {
			while (fLexer.peek() != null && !fLexer.peekKeyword("end")) {
				decl_allowed = statement_int(block, decl_allowed, true);
				//			decl_allowed = isDeclAllowed((SVDBStmt)block.getItems().get(block.getItems().size()-1));
			}
		} finally {
			block.setEndLocation(fLexer.getStartLocation());
		}
		
		fLexer.readKeyword("end");
		if (fLexer.peekOperator(":")) {
			fLexer.eatToken();
			fLexer.readId();
		}
	}
	
	private void parse_if_stmt(ISVDBAddChildItem parent) throws SVParseException {
		SVDBLocation start = fLexer.getStartLocation();
		String if_stem = fLexer.eatToken();
		
		debug("beginning of \"if\": " + if_stem);
		
		if (!if_stem.equals("if")) {
			fLexer.readKeyword("if");
		}
		
		fLexer.readOperator("(");
		SVDBIfStmt if_stmt = new SVDBIfStmt(parsers().exprParser().expression()); 
		fLexer.readOperator(")");
		if_stmt.setLocation(start);
		
		debug("--> parse body of if");
		statement(if_stmt);
		debug("<-- parse body of if");
		
		if (fLexer.peekKeyword("else")) {
			fLexer.eatToken();
			statement(if_stmt);
		}
	}
	
	private void parse_case_stmt(ISVDBAddChildItem parent) throws SVParseException {
		SVDBLocation start = fLexer.getStartLocation();
		String type_s = fLexer.eatToken();
		CaseType type = null;
		
		if (type_s.equals("case")) {
			type = CaseType.Case;
		} else if (type_s.equals("casex")) {
			type = CaseType.Casex;
		} else if (type_s.equals("casez")) {
			type = CaseType.Casez;
		} else if (type_s.equals("randcase")) {
			type = CaseType.Randcase;
		}
		
		SVDBCaseStmt case_stmt = new SVDBCaseStmt(type);
		fLexer.readOperator("(");
		case_stmt.setExpr(parsers().exprParser().expression());
		fLexer.readOperator(")");
		parent.addChildItem(case_stmt);
		
		if (fLexer.peekKeyword("matches", "inside")) {
			// TODO: ignore for now
			fLexer.eatToken();
		}
		
		while (fLexer.peek() != null && !fLexer.peekKeyword("endcase")) {
			SVDBCaseItem item = new SVDBCaseItem();
			// Read a series of comma-separated expressions
			if (type != CaseType.Randcase && fLexer.peekKeyword("default")) {
				item.addExpr(new SVDBLiteralExpr("default"));
				fLexer.eatToken();
			} else {
				while (fLexer.peek() != null) {
					item.addExpr(fParsers.exprParser().expression());
					if (type != CaseType.Randcase && fLexer.peekOperator(",")) {
						fLexer.eatToken();
					} else {
						break;
					}
				}
			}
			fLexer.readOperator(":");
			statement(item);
			case_stmt.addCaseItem(item);
		}
		fLexer.readKeyword("endcase");
	}
	
}
