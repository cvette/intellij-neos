package de.vette.idea.neos.lang.fusion.parser;

import com.intellij.lexer.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.TokenType;
import de.vette.idea.neos.lang.fusion.psi.FusionTypes;

%%

%{
  private int eelNestingLevel = 0;

  public FusionLexer() {
    this((java.io.Reader)null);
  }
%}

%public
%class FusionLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

SINGLE_LINE_COMMENT = (\/\/|#).*(\n | \r | \r\n)

C_STYLE_COMMENT=("/*"[^"*"]{COMMENT_TAIL})|"/*"
DOC_COMMENT="/*""*"+("/"|([^"/""*"]{COMMENT_TAIL}))?
COMMENT_TAIL=([^"*"]*("*"+[^"*""/"])?)*("*"+"/")?

WHITE_SPACE = [ \t\f]*
CRLF = \n | \r | \r\n
EQUALS = "="
COLON = ":"
DOT = "."
INCLUDE_KEYWORD = "include"
DECLARATION_SEPARATOR = {COLON}
NAMESPACE_KEYWORD = "namespace"
NAMESPACE_ALIAS = [a-zA-Z]+[a-zA-Z0-9]*
PACKAGE_KEY = [a-zA-Z0-9\.]+
RESOURCE_KEYWORD = "resource://"
OBJECT_TYPE_PART = [a-zA-Z0-9\.]+

VALUE_NULL = NULL|null
VALUE_BOOLEAN = true|TRUE|false|FALSE
VALUE_NUMBER = [\-]?[0-9] [0-9]* ("." [0-9] [0-9]*)?
VALUE_POSITIVE_NUMBER = [0-9] [0-9]* ("." [0-9] [0-9]*)?
VALUE_SEPARATOR = ","

VALUE_STRING_SINGLE_QUOTE =             [\']
ESCAPED_SINGLE_QUOTE =                  "\\\\"* "\\\'"
VALUE_STRING_IN_SINGLE_QUOTE =          [^\n\r\'\\]*
VALUE_STRING_DOUBLE_QUOTE =             [\"]
ESCAPED_DOUBLE_QUOTE =                  "\\\\"* "\\\""
VALUE_STRING_IN_DOUBLE_QUOTE =          [^\n\r\"\\]*

BACKTICK = "`"
ESCAPED_BACKTICK = "\\\\"* "\\\`"
VALUE_STRING_IN_BACKTICKS = [^\n\r\`\\]*

LEFT_BRACE = "{"
RIGHT_BRACE = "}"
LEFT_BRACKET = "["
RIGHT_BRACKET = "]"
PROTOTYPE_KEYWORD = "prototype"
LEFT_PAREN = "("
RIGHT_PAREN = ")"
META_PROPERTY_KEYWORD = "@"
META_PROPERTY_NAME = [a-zA-Z0-9:_\-]+
PATH_PART = [a-zA-Z0-9:_\-]+

EEL_IDENTIFIER = [a-zA-Z_] [a-zA-Z0-9_\-]*
PATH_SEPARATOR = {DOT}
ANY_STRING = [^ \t\f\n\r]*
COPY_OPERATOR = "<"
UNSET_OPERATOR = ">"
ASSIGNMENT_OPERATOR = "="
BACKSLASH = "\\"

EXPRESSION_KEYWORD = "$"
DECLARATION_SEPARATOR_LOOKAHEAD = [ \t\f]*:

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

// Declaration states
%state INCLUDE_FOUND
%state NAMESPACE_ALIAS, NAMESPACE_FOUND
%state RESOURCE

// Path states
%state META_PROPERTY_FOUND
%state PATH_PART, PATH_FOUND
%state PROTOTYPE_FOUND, PROTOTYPE_IN_PATH_FOUND, PROTOTYPE_EXPECTED, PROTOTYPE_IN_PATH_NEXT, OBJECT_TYPE_IN_PROTOTYPE_FOUND, OBJECT_TYPE_IN_PROTOTYPE_IN_PATH_FOUND
%state OPERATOR_OR_LEFT_BRACE_EXPECTED

// Value states
%states VALUE_EXPECTED,
%states VALUE_STRING_EXPECTED_IN_SINGLE_QUOTE, VALUE_STRING_EXPECTED_IN_DOUBLE_QUOTE, VALUE_STRING_EXPECTED_IN_BACKTICKS
%states VALUE_STRING_EXPECTED_IN_SINGLE_QUOTE_PATH, VALUE_STRING_EXPECTED_IN_DOUBLE_QUOTE_PATH
%states OBJECT_TYPE_FOUND, DSL_IDENTIFIER_FOUND

// Expression states
%states EXPRESSION_FOUND
%states VALUE_STRING_EXPECTED_IN_SINGLE_QUOTE_EXPRESSION
%states VALUE_STRING_EXPECTED_IN_DOUBLE_QUOTE_EXPRESSION

// Comment states
%state MULTI_LINE_COMMENT
%state CRLF_EXPECTED
%state CRLF_OR_LEFT_BRACE_EXPECTED
%state CRLF_OR_BLOCK_EXPECTED

%%

<YYINITIAL> {
    {INCLUDE_KEYWORD}/{DECLARATION_SEPARATOR_LOOKAHEAD}   { yybegin(INCLUDE_FOUND); return FusionTypes.INCLUDE_KEYWORD; }
    {NAMESPACE_KEYWORD}/{DECLARATION_SEPARATOR_LOOKAHEAD} { yybegin(NAMESPACE_FOUND); return FusionTypes.NAMESPACE_KEYWORD; }
    {PATH_PART}                                           { yybegin(PATH_FOUND); return FusionTypes.PATH_PART; }
    {PATH_SEPARATOR}                                      { yybegin(PATH_FOUND); return FusionTypes.PATH_SEPARATOR; }
    {VALUE_STRING_SINGLE_QUOTE}                           { yybegin(VALUE_STRING_EXPECTED_IN_SINGLE_QUOTE_PATH); return FusionTypes.VALUE_STRING_QUOTE; }
    {VALUE_STRING_DOUBLE_QUOTE}                           { yybegin(VALUE_STRING_EXPECTED_IN_DOUBLE_QUOTE_PATH); return FusionTypes.VALUE_STRING_QUOTE; }
    {META_PROPERTY_KEYWORD}                               { yybegin(META_PROPERTY_FOUND); return FusionTypes.META_PROPERTY_KEYWORD; }
    {PROTOTYPE_KEYWORD}/{LEFT_PAREN}                      { yybegin(PROTOTYPE_IN_PATH_FOUND); return FusionTypes.PROTOTYPE_KEYWORD; }
    {RIGHT_BRACE}                                         { return FusionTypes.RIGHT_BRACE; }
    {SINGLE_LINE_COMMENT}                                 { return FusionTypes.SINGLE_LINE_COMMENT; }

    {DOC_COMMENT}                                         { return FusionTypes.DOC_COMMENT; }
    {C_STYLE_COMMENT}                                     { return FusionTypes.C_STYLE_COMMENT; }
}

<PATH_FOUND> {
    {PATH_PART}                             { return FusionTypes.PATH_PART; }
    {PATH_SEPARATOR}                        { return FusionTypes.PATH_SEPARATOR; }
    {META_PROPERTY_KEYWORD}                 { yybegin(META_PROPERTY_FOUND); return FusionTypes.META_PROPERTY_KEYWORD; }
    {PROTOTYPE_KEYWORD}/{LEFT_PAREN}        { yybegin(PROTOTYPE_IN_PATH_FOUND); return FusionTypes.PROTOTYPE_KEYWORD; }
    {LEFT_BRACE}                            { yybegin(CRLF_OR_BLOCK_EXPECTED); return FusionTypes.LEFT_BRACE; }
    {UNSET_OPERATOR}                        { yybegin(CRLF_EXPECTED); return FusionTypes.UNSET_OPERATOR; }
    {ASSIGNMENT_OPERATOR}                   { yybegin(VALUE_EXPECTED); return FusionTypes.ASSIGNMENT_OPERATOR; }
    {COPY_OPERATOR}                         { yybegin(VALUE_EXPECTED); return FusionTypes.COPY_OPERATOR; }
    {VALUE_STRING_SINGLE_QUOTE}             { yybegin(VALUE_STRING_EXPECTED_IN_SINGLE_QUOTE_PATH); return FusionTypes.VALUE_STRING_QUOTE; }
    {VALUE_STRING_DOUBLE_QUOTE}             { yybegin(VALUE_STRING_EXPECTED_IN_DOUBLE_QUOTE_PATH); return FusionTypes.VALUE_STRING_QUOTE; }
    {WHITE_SPACE}                           { yybegin(OPERATOR_OR_LEFT_BRACE_EXPECTED); return TokenType.WHITE_SPACE; }
}


<OPERATOR_OR_LEFT_BRACE_EXPECTED> {
    {LEFT_BRACE}                            { yybegin(CRLF_OR_BLOCK_EXPECTED); return FusionTypes.LEFT_BRACE; }
    {UNSET_OPERATOR}                        { yybegin(CRLF_EXPECTED); return FusionTypes.UNSET_OPERATOR; }
    {ASSIGNMENT_OPERATOR}                   { yybegin(VALUE_EXPECTED); return FusionTypes.ASSIGNMENT_OPERATOR; }
    {COPY_OPERATOR}                         { yybegin(VALUE_EXPECTED); return FusionTypes.COPY_OPERATOR; }
}

<VALUE_EXPECTED> {
    {VALUE_BOOLEAN}                         { yybegin(CRLF_EXPECTED); return FusionTypes.VALUE_BOOLEAN; }
    {VALUE_NULL}                            { yybegin(CRLF_EXPECTED); return FusionTypes.VALUE_NULL; }
    {VALUE_NUMBER}                          { yybegin(CRLF_EXPECTED); return FusionTypes.VALUE_NUMBER; }
    {VALUE_STRING_SINGLE_QUOTE}             { yybegin(VALUE_STRING_EXPECTED_IN_SINGLE_QUOTE); return FusionTypes.VALUE_STRING_QUOTE; }
    {VALUE_STRING_DOUBLE_QUOTE}             { yybegin(VALUE_STRING_EXPECTED_IN_DOUBLE_QUOTE); return FusionTypes.VALUE_STRING_QUOTE; }
    {EXPRESSION_KEYWORD}/{LEFT_BRACE}       { yybegin(EXPRESSION_FOUND); return FusionTypes.EXPRESSION_KEYWORD; }
    {OBJECT_TYPE_PART}                      { yybegin(CRLF_OR_LEFT_BRACE_EXPECTED); return FusionTypes.UNQUALIFIED_TYPE; }
    {OBJECT_TYPE_PART}/{COLON}              { yybegin(OBJECT_TYPE_FOUND); return FusionTypes.OBJECT_TYPE_NAMESPACE; }
    {OBJECT_TYPE_PART}/{BACKTICK}           { yybegin(DSL_IDENTIFIER_FOUND); return FusionTypes.DSL_IDENTIFIER; }
}

<OBJECT_TYPE_FOUND> {
    {COLON}                                 { return FusionTypes.OBJECT_TYPE_SEPARATOR; }
    {OBJECT_TYPE_PART}                      { yybegin(CRLF_OR_LEFT_BRACE_EXPECTED); return FusionTypes.UNQUALIFIED_TYPE; }
}

<DSL_IDENTIFIER_FOUND> {
    {BACKTICK}                              { yybegin(VALUE_STRING_EXPECTED_IN_BACKTICKS); return FusionTypes.BACKTICK; }
}

<CRLF_OR_LEFT_BRACE_EXPECTED> {
    {LEFT_BRACE}                            { yybegin(CRLF_OR_BLOCK_EXPECTED); return FusionTypes.LEFT_BRACE; }
    {CRLF}                                  { yybegin(YYINITIAL); return FusionTypes.CRLF; }
}

<PROTOTYPE_IN_PATH_FOUND> {
    {OBJECT_TYPE_PART}                      { return FusionTypes.UNQUALIFIED_TYPE; }
    {OBJECT_TYPE_PART}/{COLON}              { yybegin(OBJECT_TYPE_IN_PROTOTYPE_IN_PATH_FOUND); return FusionTypes.OBJECT_TYPE_NAMESPACE; }
    {COLON}                                 { return FusionTypes.OBJECT_TYPE_SEPARATOR; }
    {LEFT_PAREN}                            { return FusionTypes.LEFT_PAREN; }
    {RIGHT_PAREN}                           { yybegin(PROTOTYPE_IN_PATH_NEXT); return FusionTypes.RIGHT_PAREN; }
}

<OBJECT_TYPE_IN_PROTOTYPE_IN_PATH_FOUND> {
    {COLON}                                 { return FusionTypes.OBJECT_TYPE_SEPARATOR; }
    {OBJECT_TYPE_PART}                      { return FusionTypes.UNQUALIFIED_TYPE; }
    {RIGHT_PAREN}                           { yybegin(PROTOTYPE_IN_PATH_NEXT); return FusionTypes.RIGHT_PAREN; }
}

<PROTOTYPE_IN_PATH_NEXT> {
    {COPY_OPERATOR}                         { yybegin(PROTOTYPE_EXPECTED); return FusionTypes.COPY_OPERATOR; }
    {UNSET_OPERATOR}                        { yybegin(CRLF_EXPECTED); return FusionTypes.UNSET_OPERATOR; }
    {PATH_SEPARATOR}                        { yybegin(PATH_FOUND); return FusionTypes.PATH_SEPARATOR; }
    {LEFT_BRACE}                            { yybegin(YYINITIAL); return FusionTypes.LEFT_BRACE; }
}


<PROTOTYPE_FOUND> {
    {OBJECT_TYPE_PART}                      { return FusionTypes.UNQUALIFIED_TYPE; }
    {OBJECT_TYPE_PART}/{COLON}              { yybegin(OBJECT_TYPE_IN_PROTOTYPE_FOUND); return FusionTypes.OBJECT_TYPE_NAMESPACE; }
    {COLON}                                 { return FusionTypes.OBJECT_TYPE_SEPARATOR; }
    {LEFT_PAREN}                            { return FusionTypes.LEFT_PAREN; }
    {RIGHT_PAREN}                           { yybegin(CRLF_OR_LEFT_BRACE_EXPECTED); return FusionTypes.RIGHT_PAREN; }
}

<OBJECT_TYPE_IN_PROTOTYPE_FOUND> {
    {COLON}                                 { return FusionTypes.OBJECT_TYPE_SEPARATOR; }
    {OBJECT_TYPE_PART}                      { return FusionTypes.UNQUALIFIED_TYPE; }
    {RIGHT_PAREN}                           { yybegin(CRLF_OR_LEFT_BRACE_EXPECTED); return FusionTypes.RIGHT_PAREN; }
}

<PROTOTYPE_EXPECTED> {
    {PROTOTYPE_KEYWORD}                     { yybegin(PROTOTYPE_FOUND); return FusionTypes.PROTOTYPE_KEYWORD; }
}

<META_PROPERTY_FOUND> {
    {META_PROPERTY_NAME}                    { yybegin(PATH_FOUND); return FusionTypes.META_PROPERTY_NAME; }
    {WHITE_SPACE}                           { yybegin(YYINITIAL); return TokenType.BAD_CHARACTER; }
}

<CRLF_EXPECTED, CRLF_OR_BLOCK_EXPECTED> {
    {CRLF}                                  { yybegin(YYINITIAL); return FusionTypes.CRLF; }
}

<CRLF_OR_BLOCK_EXPECTED> {
    {RIGHT_BRACE}                           { yybegin(YYINITIAL); return FusionTypes.RIGHT_BRACE; }
}


<INCLUDE_FOUND> {
    {DECLARATION_SEPARATOR}                 { yybegin(INCLUDE_FOUND); return FusionTypes.INCLUDE_SEPARATOR; }
    {RESOURCE_KEYWORD}                      { yybegin(RESOURCE); return FusionTypes.RESOURCE_KEYWORD; }
    {ANY_STRING}                            { yybegin(CRLF_EXPECTED); return FusionTypes.INCLUDE_PATH; }
}

<RESOURCE> {
    {ANY_STRING}                            { yybegin(CRLF_EXPECTED); return FusionTypes.RESOURCE_PATH; }
}

<NAMESPACE_FOUND> {
    {DECLARATION_SEPARATOR}                 { return FusionTypes.NAMESPACE_SEPARATOR; }
    {NAMESPACE_ALIAS}                       { return FusionTypes.NAMESPACE_ALIAS; }
    {EQUALS}                                { yybegin(NAMESPACE_ALIAS); return FusionTypes.NAMESPACE_ALIAS_SEPARATOR; }
}

<NAMESPACE_ALIAS> {
    {PACKAGE_KEY}                           { yybegin(CRLF_EXPECTED); return FusionTypes.PACKAGE_KEY; }
}

<VALUE_STRING_EXPECTED_IN_SINGLE_QUOTE> {
    {VALUE_STRING_SINGLE_QUOTE}             { yybegin(CRLF_EXPECTED); return FusionTypes.VALUE_STRING_QUOTE; }
}

<VALUE_STRING_EXPECTED_IN_SINGLE_QUOTE_EXPRESSION> {
    {VALUE_STRING_SINGLE_QUOTE}             { yybegin(EXPRESSION_FOUND); return FusionTypes.VALUE_STRING_QUOTE; }
}

<VALUE_STRING_EXPECTED_IN_SINGLE_QUOTE_PATH> {
    {VALUE_STRING_SINGLE_QUOTE}             { yybegin(PATH_FOUND); return FusionTypes.VALUE_STRING_QUOTE; }
    {ESCAPED_SINGLE_QUOTE}*                 { return FusionTypes.VALUE_STRING_ESCAPED_QUOTE; }
    {BACKSLASH}*                            { return FusionTypes.VALUE_STRING; }
    {VALUE_STRING_IN_SINGLE_QUOTE}          { return FusionTypes.VALUE_STRING; }
}

<VALUE_STRING_EXPECTED_IN_SINGLE_QUOTE, VALUE_STRING_EXPECTED_IN_SINGLE_QUOTE_EXPRESSION> {
    {CRLF}                                  { return FusionTypes.CRLF; }
    {ESCAPED_SINGLE_QUOTE}*                 { return FusionTypes.VALUE_STRING_ESCAPED_QUOTE; }
    {BACKSLASH}*                            { return FusionTypes.VALUE_STRING; }
    {VALUE_STRING_IN_SINGLE_QUOTE}          { return FusionTypes.VALUE_STRING; }
}

<VALUE_STRING_EXPECTED_IN_DOUBLE_QUOTE> {
    {VALUE_STRING_DOUBLE_QUOTE}             { yybegin(CRLF_EXPECTED); return FusionTypes.VALUE_STRING_QUOTE; }
}

<VALUE_STRING_EXPECTED_IN_DOUBLE_QUOTE_EXPRESSION> {
    {VALUE_STRING_DOUBLE_QUOTE}             { yybegin(EXPRESSION_FOUND); return FusionTypes.VALUE_STRING_QUOTE; }
}

<VALUE_STRING_EXPECTED_IN_DOUBLE_QUOTE_PATH> {
    {VALUE_STRING_DOUBLE_QUOTE}             { yybegin(PATH_FOUND); return FusionTypes.VALUE_STRING_QUOTE; }
    {ESCAPED_DOUBLE_QUOTE}*                 { return FusionTypes.VALUE_STRING_ESCAPED_QUOTE; }
    {BACKSLASH}*                            { return FusionTypes.VALUE_STRING; }
    {VALUE_STRING_IN_DOUBLE_QUOTE}          { return FusionTypes.VALUE_STRING; }
}

<VALUE_STRING_EXPECTED_IN_DOUBLE_QUOTE, VALUE_STRING_EXPECTED_IN_DOUBLE_QUOTE_EXPRESSION> {
    {CRLF}                                  { return FusionTypes.CRLF; }
    {ESCAPED_DOUBLE_QUOTE}*                 { return FusionTypes.VALUE_STRING_ESCAPED_QUOTE; }
    {BACKSLASH}*                            { return FusionTypes.VALUE_STRING; }
    {VALUE_STRING_IN_DOUBLE_QUOTE}          { return FusionTypes.VALUE_STRING; }
}

<VALUE_STRING_EXPECTED_IN_BACKTICKS> {
    {CRLF}                                  { return FusionTypes.CRLF; }
    {ESCAPED_BACKTICK}*                     { return FusionTypes.ESCAPED_BACKTICK; }
    {BACKSLASH}*                            { return FusionTypes.VALUE_STRING; }
    {VALUE_STRING_IN_BACKTICKS}             { return FusionTypes.VALUE_STRING; }
    {BACKTICK}                              { yybegin(CRLF_EXPECTED); return FusionTypes.BACKTICK; }
}

<EXPRESSION_FOUND> {
    {LEFT_BRACE}                            { eelNestingLevel++; if (eelNestingLevel == 1) { return FusionTypes.EEL_LEFT_BRACE; } else { return FusionTypes.EEL_OBJECT_LEFT_BRACE; } }
    {RIGHT_BRACE}                           { eelNestingLevel--; if (eelNestingLevel == 0) { yybegin(CRLF_EXPECTED); return FusionTypes.EEL_RIGHT_BRACE; } else { return FusionTypes.EEL_OBJECT_RIGHT_BRACE; }}
    {LEFT_PAREN}                            { return FusionTypes.EEL_LEFT_PAREN; }
    {RIGHT_PAREN}                           { return FusionTypes.EEL_RIGHT_PAREN; }
    {ADDITION_OPERATOR}                     { return FusionTypes.EEL_ADDITION_OPERATOR; }
    {SUBTRACTION_OPERATOR}                  { return FusionTypes.EEL_SUBTRACTION_OPERATOR; }
    {MULTIPLICATION_OPERATOR}               { return FusionTypes.EEL_MULTIPLICATION_OPERATOR; }
    {DIVISION_OPERATOR}                     { return FusionTypes.EEL_DIVISION_OPERATOR; }
    {MODULO_OPERATOR}                       { return FusionTypes.EEL_MODULO_OPERATOR; }
    {NEGATION_OPERATOR}                     { return FusionTypes.EEL_NEGATION_OPERATOR; }
    {LEFT_BRACKET}                          { return FusionTypes.EEL_LEFT_BRACKET; }
    {RIGHT_BRACKET}                         { return FusionTypes.EEL_RIGHT_BRACKET; }
    {VALUE_STRING_SINGLE_QUOTE}             { yybegin(VALUE_STRING_EXPECTED_IN_SINGLE_QUOTE_EXPRESSION); return FusionTypes.VALUE_STRING_QUOTE; }
    {VALUE_STRING_DOUBLE_QUOTE}             { yybegin(VALUE_STRING_EXPECTED_IN_DOUBLE_QUOTE_EXPRESSION); return FusionTypes.VALUE_STRING_QUOTE; }
    {VALUE_POSITIVE_NUMBER}                 { return FusionTypes.VALUE_NUMBER; }
    {VALUE_BOOLEAN}                         { return FusionTypes.VALUE_BOOLEAN; }
    {EXPRESSION_KEYWORD}                    { return FusionTypes.EXPRESSION_KEYWORD; }
    {BOOLEAN_AND}                           { return FusionTypes.EEL_BOOLEAN_AND; }
    {BOOLEAN_OR}                            { return FusionTypes.EEL_BOOLEAN_OR; }
    {IF_KEYWORD}                            { return FusionTypes.IF_KEYWORD; }
    {IF_SEPARATOR}                          { return FusionTypes.IF_SEPARATOR; }
    {COMPARISION_OPERATOR}                  { return FusionTypes.EEL_COMPARISON_OPERATOR; }
    {VALUE_SEPARATOR}                       { return FusionTypes.VALUE_SEPARATOR; }
    {EEL_IDENTIFIER}                        { return FusionTypes.EEL_IDENTIFIER; }
    {EEL_IDENTIFIER}/{LEFT_PAREN}           { return FusionTypes.EEL_FUNCTION; }
    {PATH_SEPARATOR}                        { return FusionTypes.EEL_DOT; }
}

{WHITE_SPACE}                               { return TokenType.WHITE_SPACE; }
{CRLF}                                      { yybegin(YYINITIAL); eelNestingLevel = 0; return FusionTypes.CRLF; }
.                                           { return TokenType.BAD_CHARACTER; }
