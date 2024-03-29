package de.vette.idea.neos.lang.eel.parser;

import com.intellij.lexer.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;
import de.vette.idea.neos.lang.eel.psi.EelTypes;

%%

%{
  private int eelNestingLevel = 0;

  public EelLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class EelLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

WHITE_SPACE = [ \t\f]+
CRLF = \n | \r | \r\n
ARROW = "=>"
COLON = ":"
DOT = "."

VALUE_BOOLEAN = true|TRUE|false|FALSE
VALUE_POSITIVE_NUMBER = [0-9] [0-9]* ("." [0-9] [0-9]*)?
VALUE_SEPARATOR = ","

VALUE_STRING_SINGLE_QUOTE =             [\']
ESCAPED_SINGLE_QUOTE =                  "\\\\"* "\\\'"
VALUE_STRING_IN_SINGLE_QUOTE =          [^\n\r\'\\]+
VALUE_STRING_DOUBLE_QUOTE =             [\"]
ESCAPED_DOUBLE_QUOTE =                  "\\\\"* "\\\""
VALUE_STRING_IN_DOUBLE_QUOTE =          [^\n\r\"\\]+

LEFT_BRACE = "{"
RIGHT_BRACE = "}"
LEFT_BRACKET = "["
RIGHT_BRACKET = "]"
LEFT_PAREN = "("
RIGHT_PAREN = ")"

EEL_IDENTIFIER = [a-zA-Z_] [a-zA-Z0-9_\-]*
BACKSLASH = "\\"

BOOLEAN_AND = "||" | "or" | "OR"
BOOLEAN_OR = "&&" | "and" | "AND"

ADDITION_OPERATOR = "+"
SUBTRACTION_OPERATOR = "-"
MODULO_OPERATOR = "%" | "/"
DIVISION_OPERATOR = "/"
MULTIPLICATION_OPERATOR = "*"
COMPARISION_OPERATOR = "==" | "!=" | "<=" | ">=" | "<" | ">"
NEGATION_OPERATOR = "not" | "!"

IF_KEYWORD = "?"
IF_SEPARATOR = {COLON}

// Value states
%states VALUE_STRING_EXPECTED_IN_SINGLE_QUOTE, VALUE_STRING_EXPECTED_IN_DOUBLE_QUOTE

// Expression states
%states VALUE_STRING_EXPECTED_IN_SINGLE_QUOTE_EXPRESSION
%states VALUE_STRING_EXPECTED_IN_DOUBLE_QUOTE_EXPRESSION

%%

<YYINITIAL> {
    {LEFT_BRACE}                            { eelNestingLevel++; return EelTypes.EEL_OBJECT_LEFT_BRACE; }
    {RIGHT_BRACE}                           { eelNestingLevel--; return EelTypes.EEL_OBJECT_RIGHT_BRACE; }
    {LEFT_PAREN}                            { return EelTypes.EEL_LEFT_PAREN; }
    {RIGHT_PAREN}                           { return EelTypes.EEL_RIGHT_PAREN; }
    {ADDITION_OPERATOR}                     { return EelTypes.EEL_ADDITION_OPERATOR; }
    {SUBTRACTION_OPERATOR}                  { return EelTypes.EEL_SUBTRACTION_OPERATOR; }
    {MULTIPLICATION_OPERATOR}               { return EelTypes.EEL_MULTIPLICATION_OPERATOR; }
    {DIVISION_OPERATOR}                     { return EelTypes.EEL_DIVISION_OPERATOR; }
    {MODULO_OPERATOR}                       { return EelTypes.EEL_MODULO_OPERATOR; }
    {NEGATION_OPERATOR}                     { return EelTypes.EEL_NEGATION_OPERATOR; }
    {LEFT_BRACKET}                          { return EelTypes.EEL_LEFT_BRACKET; }
    {RIGHT_BRACKET}                         { return EelTypes.EEL_RIGHT_BRACKET; }
    {VALUE_STRING_SINGLE_QUOTE}             { yybegin(VALUE_STRING_EXPECTED_IN_SINGLE_QUOTE_EXPRESSION); return EelTypes.VALUE_STRING_QUOTE; }
    {VALUE_STRING_DOUBLE_QUOTE}             { yybegin(VALUE_STRING_EXPECTED_IN_DOUBLE_QUOTE_EXPRESSION); return EelTypes.VALUE_STRING_QUOTE; }
    {VALUE_POSITIVE_NUMBER}                 { return EelTypes.VALUE_NUMBER; }
    {VALUE_BOOLEAN}                         { return EelTypes.VALUE_BOOLEAN; }
    {BOOLEAN_AND}                           { return EelTypes.EEL_BOOLEAN_AND; }
    {BOOLEAN_OR}                            { return EelTypes.EEL_BOOLEAN_OR; }
    {IF_KEYWORD}                            { return EelTypes.IF_KEYWORD; }
    {IF_SEPARATOR}                          { return EelTypes.IF_SEPARATOR; }
    {COMPARISION_OPERATOR}                  { return EelTypes.EEL_COMPARISON_OPERATOR; }
    {ARROW}                                 { return EelTypes.EEL_ARROW; }
    {VALUE_SEPARATOR}                       { return EelTypes.VALUE_SEPARATOR; }
    {EEL_IDENTIFIER}                        { return EelTypes.EEL_IDENTIFIER; }
    {EEL_IDENTIFIER}/{LEFT_PAREN}           { return EelTypes.EEL_FUNCTION; }
    {DOT}                                   { return EelTypes.EEL_DOT; }
}

<VALUE_STRING_EXPECTED_IN_SINGLE_QUOTE> {
    {VALUE_STRING_SINGLE_QUOTE}             { yybegin(YYINITIAL); return EelTypes.VALUE_STRING_QUOTE; }
}

<VALUE_STRING_EXPECTED_IN_SINGLE_QUOTE_EXPRESSION> {
    {VALUE_STRING_SINGLE_QUOTE}             { yybegin(YYINITIAL); return EelTypes.VALUE_STRING_QUOTE; }
}

<VALUE_STRING_EXPECTED_IN_SINGLE_QUOTE, VALUE_STRING_EXPECTED_IN_SINGLE_QUOTE_EXPRESSION> {
    {CRLF}                                  { return EelTypes.CRLF; }
    {ESCAPED_SINGLE_QUOTE}+                 { return EelTypes.VALUE_STRING_ESCAPED_QUOTE; }
    {BACKSLASH}+                            { return EelTypes.VALUE_STRING; }
    {VALUE_STRING_IN_SINGLE_QUOTE}          { return EelTypes.VALUE_STRING; }
}

<VALUE_STRING_EXPECTED_IN_DOUBLE_QUOTE> {
    {VALUE_STRING_DOUBLE_QUOTE}             { yybegin(YYINITIAL); return EelTypes.VALUE_STRING_QUOTE; }
}

<VALUE_STRING_EXPECTED_IN_DOUBLE_QUOTE_EXPRESSION> {
    {VALUE_STRING_DOUBLE_QUOTE}             { yybegin(YYINITIAL); return EelTypes.VALUE_STRING_QUOTE; }
}

<VALUE_STRING_EXPECTED_IN_DOUBLE_QUOTE, VALUE_STRING_EXPECTED_IN_DOUBLE_QUOTE_EXPRESSION> {
    {CRLF}                                  { return EelTypes.CRLF; }
    {ESCAPED_DOUBLE_QUOTE}+                 { return EelTypes.VALUE_STRING_ESCAPED_QUOTE; }
    {BACKSLASH}+                            { return EelTypes.VALUE_STRING; }
    {VALUE_STRING_IN_DOUBLE_QUOTE}          { return EelTypes.VALUE_STRING; }
}

{WHITE_SPACE}                               { return TokenType.WHITE_SPACE; }
{CRLF}                                      { yybegin(YYINITIAL); eelNestingLevel = 0; return EelTypes.CRLF; }
.                                           { return TokenType.BAD_CHARACTER; }
