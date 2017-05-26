package de.vette.idea.neos.lang.afx.lexer;

import com.intellij.lexer.*;
import com.intellij.psi.tree.IElementType;
import static de.vette.idea.neos.lang.afx.psi.AfxTypes.*;

%%

%{
  private int eelNestingLevel = 0;

  public AfxLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class AfxLexer
%extends AfxBaseFlexLexer
%function advance
%type IElementType
%unicode
%ignorecase

%state HTML_TEXT
%state HTML_OPEN_TAG_OPEN
%state HTML_CLOSE_TAG_OPEN
%state HTML_TAG
%state SCRIPT_TAG
%state SCRIPT_CDATA
%state STYLE_TAG
%state STYLE_CDATA
%state HTML_ATTR
%state HTML_ATTR_VALUE
%state HTML_ATTR_SQ
%state HTML_ATTR_DQ
%state HTML_COMMENT
%state EEL_EXPRESSION

WHITE_SPACE =[ \t\r\n]+
LEFT_BRACE = "{"
RIGHT_BRACE = "}"
EEL_CONTENT = [^{}]+

%%
<YYINITIAL> {
	[^] {
		rollbackMatch();
		pushState(HTML_TEXT);
	}
}

<HTML_TEXT, SCRIPT_TAG, SCRIPT_CDATA, STYLE_TAG, STYLE_CDATA, HTML_TAG, HTML_ATTR_SQ, HTML_ATTR_DQ, HTML_COMMENT> {
    {LEFT_BRACE} {
        pushState(EEL_EXPRESSION);
        eelNestingLevel++;
        return LEFT_BRACE;
    }
}

<EEL_EXPRESSION> {
    {LEFT_BRACE}                            { eelNestingLevel++; if (eelNestingLevel == 1) { return LEFT_BRACE; } else { return EEL_CONTENT; } }
    {RIGHT_BRACE}                           { eelNestingLevel--; if (eelNestingLevel == 0) { popState(); return RIGHT_BRACE; } else { return EEL_CONTENT; }}
    {EEL_CONTENT}                           { return EEL_CONTENT; }
}

<HTML_TEXT> {
	"<!--" {
		pushState(HTML_COMMENT);
		return T_TEXT;
	}

	"<" / [a-zA-Z0-9:] {
		pushState(HTML_OPEN_TAG_OPEN);
		return T_TEXT;
	}

	"</" / [a-zA-Z0-9:]  {
		pushState(HTML_CLOSE_TAG_OPEN);
		return T_TEXT;
	}

	[^<{]+ {
		return T_TEXT;
	}

	[^] {
		return T_TEXT;
	}
}

<HTML_OPEN_TAG_OPEN> {
	"script" / [^a-zA-Z0-9:] {
		pushState(SCRIPT_TAG);
		return T_TEXT;
	}

	"style" / [^a-zA-Z0-9:] {
		pushState(STYLE_TAG);
		return T_TEXT;
	}
}

<HTML_OPEN_TAG_OPEN, HTML_CLOSE_TAG_OPEN> {
	[a-zA-Z0-9:]+ {
		pushState(HTML_TAG);
		return T_TEXT;
	}
}

<SCRIPT_TAG> {
	"/>" {
		popState(HTML_OPEN_TAG_OPEN);
		popState(HTML_TEXT);
		pushState(SCRIPT_CDATA);
		return T_TEXT;
	}

	">" {
		popState(HTML_OPEN_TAG_OPEN);
		popState(HTML_TEXT);
		pushState(SCRIPT_CDATA);
		return T_TEXT;
	}
}

<SCRIPT_CDATA> {
	"</" / "script" [^a-zA-Z0-9:] {
		popState(HTML_TEXT);
		pushState(HTML_CLOSE_TAG_OPEN);
		return T_TEXT;
	}

	[^] {
		return T_TEXT;
	}
}

<STYLE_TAG> {
	"/>" {
		popState(HTML_OPEN_TAG_OPEN);
		popState(HTML_TEXT);
		pushState(STYLE_CDATA);
		return T_TEXT;
	}

	">" {
		popState(HTML_OPEN_TAG_OPEN);
		popState(HTML_TEXT);
		pushState(STYLE_CDATA);
		return T_TEXT;
	}
}

<STYLE_CDATA> {
	"</" / "style" [^a-zA-Z0-9:] {
		popState(HTML_TEXT);
		pushState(HTML_CLOSE_TAG_OPEN);
		return T_TEXT;
	}

	[^] {
		return T_TEXT;
	}
}

<HTML_TAG> {
	"/>" {
		popState(HTML_OPEN_TAG_OPEN, HTML_CLOSE_TAG_OPEN);
		popState(HTML_TEXT);
		return T_TEXT;
	}

	">" {
		popState(HTML_OPEN_TAG_OPEN, HTML_CLOSE_TAG_OPEN);
		popState(HTML_TEXT);
		return T_TEXT;
	}
}

<SCRIPT_TAG, STYLE_TAG, HTML_TAG> {
	[^ \t\r\n/>={]+ {
		pushState(HTML_ATTR);
		return T_TEXT;
	}

	{WHITE_SPACE} {
		return T_TEXT;
	}

	[^] {
		return T_TEXT; // fallback
	}
}

<HTML_ATTR> {
	"=" / [ \t\r\n]* [^ \t\r\n/>{] {
		pushState(HTML_ATTR_VALUE);
		return T_TEXT;
	}

	{WHITE_SPACE} {
		return T_TEXT;
	}

	[^] {
		rollbackMatch();
		popState(SCRIPT_TAG, STYLE_TAG, HTML_TAG);
	}
}

<HTML_ATTR_VALUE> {
	['] {
		pushState(HTML_ATTR_SQ);
		return T_TEXT;
	}

	[\"] {
		pushState(HTML_ATTR_DQ);
		return T_TEXT;
	}

	[^ \t\r\n/>{'\"][^ \t\r\n/>{]* {
		popState(HTML_ATTR);
		popState(SCRIPT_TAG, STYLE_TAG, HTML_TAG);
		return T_TEXT;
	}

	{WHITE_SPACE} {
		return T_TEXT;
	}

	[^] {
		rollbackMatch();
		popState(HTML_ATTR);
		popState(SCRIPT_TAG, STYLE_TAG, HTML_TAG);
	}
}

<HTML_ATTR_SQ> {
	['] {
		popState(HTML_ATTR_VALUE);
		popState(HTML_ATTR);
		popState(SCRIPT_TAG, STYLE_TAG, HTML_TAG);
		return T_TEXT;
	}

	[^'{]+ {
		return T_TEXT;
	}

	"{" {
		return T_TEXT;
	}
}

<HTML_ATTR_DQ> {
	[\"] {
		popState(HTML_ATTR_VALUE);
		popState(HTML_ATTR);
		popState(SCRIPT_TAG, STYLE_TAG, HTML_TAG);
		return T_TEXT;
	}

	[^\"{]+ {
		return T_TEXT;
	}

	"{" {
		return T_TEXT;
	}
}

<HTML_COMMENT> {
	"-->" {
		popState(HTML_TEXT);
		return T_TEXT;
	}

	[^] {
		return T_TEXT;
	}
}

<HTML_TEXT, HTML_OPEN_TAG_OPEN, HTML_CLOSE_TAG_OPEN, HTML_TAG, SCRIPT_TAG, SCRIPT_CDATA, STYLE_TAG, STYLE_CDATA, HTML_ATTR, HTML_ATTR_SQ, HTML_ATTR_DQ, HTML_COMMENT> {
	[^] {
		// throw new RuntimeException('Lexer failed');
		return com.intellij.psi.TokenType.BAD_CHARACTER;
	}
}