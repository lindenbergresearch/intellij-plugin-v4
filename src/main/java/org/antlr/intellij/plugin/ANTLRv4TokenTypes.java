package org.antlr.intellij.plugin;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.antlr.intellij.adaptor.lexer.RuleIElementType;
import org.antlr.intellij.adaptor.lexer.TokenIElementType;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.intellij.lang.annotations.MagicConstant;

import java.util.List;

import static org.antlr.intellij.adaptor.lexer.PSIElementTypeFactory.*;

public class ANTLRv4TokenTypes {
    public static final List<TokenIElementType> TOKEN_ELEMENT_TYPES =
        getTokenIElementTypes(ANTLRv4Language.INSTANCE);

    public static final List<RuleIElementType> RULE_ELEMENT_TYPES =
        getRuleIElementTypes(ANTLRv4Language.INSTANCE);

    public static final TokenSet COMMENTS =
        createTokenSet(
            ANTLRv4Language.INSTANCE,
            ANTLRv4Lexer.DOC_COMMENT,
            ANTLRv4Lexer.BLOCK_COMMENT,
            ANTLRv4Lexer.LINE_COMMENT
        );

    public static final TokenSet WHITESPACES =
        createTokenSet(
            ANTLRv4Language.INSTANCE,
            ANTLRv4Lexer.WS
        );

    public static final TokenSet BRACES =
        createTokenSet(
            ANTLRv4Language.INSTANCE,
            ANTLRv4Lexer.LPAREN,
            ANTLRv4Lexer.RPAREN,
            ANTLRv4Lexer.RBRACE,
            ANTLRv4Lexer.LBRACE,
            ANTLRv4Lexer.OPT_LBRACE,
            ANTLRv4Lexer.OPT_RBRACE
        );


    public static final TokenSet KEYWORDS =
        createTokenSet(
            ANTLRv4Language.INSTANCE,
            ANTLRv4Lexer.LEXER, ANTLRv4Lexer.PROTECTED, ANTLRv4Lexer.IMPORT, ANTLRv4Lexer.CATCH,
            ANTLRv4Lexer.PRIVATE, ANTLRv4Lexer.FRAGMENT, ANTLRv4Lexer.PUBLIC, ANTLRv4Lexer.MODE,
            ANTLRv4Lexer.FINALLY, ANTLRv4Lexer.RETURNS, ANTLRv4Lexer.THROWS, ANTLRv4Lexer.GRAMMAR,
            ANTLRv4Lexer.LOCALS, ANTLRv4Lexer.PARSER
        );

    public static IElementType BAD_TOKEN_TYPE = new IElementType("BAD_TOKEN", ANTLRv4Language.INSTANCE);


    public static RuleIElementType getRuleElementType(@MagicConstant(valuesFromClass = ANTLRv4Parser.class) int ruleIndex) {
        return RULE_ELEMENT_TYPES.get(ruleIndex);
    }


    public static TokenIElementType getTokenElementType(@MagicConstant(valuesFromClass = ANTLRv4Lexer.class) int ruleIndex) {
        return TOKEN_ELEMENT_TYPES.get(ruleIndex);
    }
}
