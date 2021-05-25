package de.vette.idea.neos.lang.afx.parser;

import com.intellij.psi.xml.*;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lexer.FlexLexer;import de.vette.idea.neos.lang.afx.psi.AfxElementTypes;


%%

%{
  public int bracesNestingLevel = 0;

  public _AfxLexer() {
    this((java.io.Reader)null);
  }

  private int myPrevState = YYINITIAL;

  public int yyprevstate() {
    return myPrevState;
  }

  private int popState(){
    final int prev = myPrevState;
    myPrevState = YYINITIAL;
    return prev;
  }

  protected void pushState(int state){
    myPrevState = state;
  }
%}

%eof{
  bracesNestingLevel = 0;
%eof}

%public
%class _AfxLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

%state COMMENT
%state START_TAG_NAME
%state END_TAG_NAME
%state BEFORE_TAG_ATTRIBUTES
%state TAG_ATTRIBUTES
%state ATTRIBUTE_VALUE_START
%state ATTRIBUTE_VALUE_DQ
%state ATTRIBUTE_VALUE_SQ
%state ATTRIBUTE_VALUE_EEL
%state EEL_IN_TAG_CONTENT
%state PROCESSING_INSTRUCTION
%state TAG_CHARACTERS
%state C_COMMENT_START
%state C_COMMENT_END

ALPHA=[:letter:]
DIGIT=[0-9]
WHITE_SPACE_CHARS=[ \n\r\t\f\u2028\u2029\u0085]+

TAG_NAME=({ALPHA}|"_"|":")({ALPHA}|{DIGIT}|"_"|":"|"."|"-")*
/* see http://www.w3.org/TR/html5/syntax.html#syntax-attribute-name */
ATTRIBUTE_NAME=([^ \n\r\t\f\"\'<>/=])+

%%

<YYINITIAL> {WHITE_SPACE_CHARS} { return XmlTokenType.XML_REAL_WHITE_SPACE; }
<YYINITIAL> "<" {TAG_NAME} { yybegin(START_TAG_NAME); yypushback(yylength()); }

<YYINITIAL> \\\$ {
  return XmlTokenType.XML_DATA_CHARACTERS;
}

<YYINITIAL> "{" { yybegin(EEL_IN_TAG_CONTENT); return AfxElementTypes.AFX_EEL_START_DELIMITER; }

<START_TAG_NAME> "<" { return XmlTokenType.XML_START_TAG_START; }

<YYINITIAL> "</" {TAG_NAME} { yybegin(END_TAG_NAME); yypushback(yylength()); }
<YYINITIAL, END_TAG_NAME> "</" { return XmlTokenType.XML_END_TAG_START; }

<START_TAG_NAME, END_TAG_NAME> {TAG_NAME} { yybegin(BEFORE_TAG_ATTRIBUTES); return XmlTokenType.XML_NAME; }

<TAG_ATTRIBUTES,ATTRIBUTE_VALUE_START, START_TAG_NAME, END_TAG_NAME, TAG_CHARACTERS> {WHITE_SPACE_CHARS} { return TokenType.WHITE_SPACE; }

<BEFORE_TAG_ATTRIBUTES, TAG_ATTRIBUTES> ">" { yybegin(YYINITIAL); return XmlTokenType.XML_TAG_END; }
<BEFORE_TAG_ATTRIBUTES, TAG_ATTRIBUTES> "/>" { yybegin(YYINITIAL); return XmlTokenType.XML_EMPTY_ELEMENT_END; }
<BEFORE_TAG_ATTRIBUTES> {WHITE_SPACE_CHARS} { yybegin(TAG_ATTRIBUTES); return TokenType.WHITE_SPACE; }
<TAG_ATTRIBUTES> {ATTRIBUTE_NAME} { return XmlTokenType.XML_NAME; }
<TAG_ATTRIBUTES> "=" { yybegin(ATTRIBUTE_VALUE_START); return XmlTokenType.XML_EQ; }
<BEFORE_TAG_ATTRIBUTES, TAG_ATTRIBUTES, START_TAG_NAME, END_TAG_NAME> [^] { yybegin(YYINITIAL); yypushback(1); break; }

<ATTRIBUTE_VALUE_START> ">" { yybegin(YYINITIAL); return XmlTokenType.XML_TAG_END; }
<ATTRIBUTE_VALUE_START> "/>" { yybegin(YYINITIAL); return XmlTokenType.XML_EMPTY_ELEMENT_END; }

<ATTRIBUTE_VALUE_START> [^ \n\r\t\f'\"\{\}\>]([^ \n\r\t\f\>]|(\/[^\>]))* { yybegin(TAG_ATTRIBUTES); return XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN; }
<ATTRIBUTE_VALUE_START> "\"" { yybegin(ATTRIBUTE_VALUE_DQ); return XmlTokenType.XML_ATTRIBUTE_VALUE_START_DELIMITER; }
<ATTRIBUTE_VALUE_START> "'" { yybegin(ATTRIBUTE_VALUE_SQ); return XmlTokenType.XML_ATTRIBUTE_VALUE_START_DELIMITER; }
<ATTRIBUTE_VALUE_START> "{" { yybegin(ATTRIBUTE_VALUE_EEL); return AfxElementTypes.AFX_EEL_START_DELIMITER; }

<ATTRIBUTE_VALUE_EEL, EEL_IN_TAG_CONTENT> {
  "{" { bracesNestingLevel++; return AfxElementTypes.AFX_EEL_VALUE; }
  "}" {
          if (bracesNestingLevel > 0) { bracesNestingLevel--; return AfxElementTypes.AFX_EEL_VALUE; }
          if (yystate() == EEL_IN_TAG_CONTENT) yybegin(YYINITIAL);
          if (yystate() == ATTRIBUTE_VALUE_EEL) yybegin(TAG_ATTRIBUTES);
          return AfxElementTypes.AFX_EEL_END_DELIMITER;
      }

  \\\$ { return AfxElementTypes.AFX_EEL_VALUE; }
  [^] { return AfxElementTypes.AFX_EEL_VALUE;}
}

<ATTRIBUTE_VALUE_DQ> {
  "\"" { yybegin(TAG_ATTRIBUTES); return XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER; }
  \\\$ { return XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN; }
  [^] { return XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN;}
}

<ATTRIBUTE_VALUE_SQ> {
  "'" { yybegin(TAG_ATTRIBUTES); return XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER; }
  \\\$ { return XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN; }
  [^] { return XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN;}
}

<YYINITIAL> ([^<{&\$# \n\r\t\f]|(\\\$)|(\\#))* { return XmlTokenType.XML_DATA_CHARACTERS; }
<YYINITIAL> [^] { return XmlTokenType.XML_DATA_CHARACTERS; }
[^] { return TokenType.BAD_CHARACTER; }
