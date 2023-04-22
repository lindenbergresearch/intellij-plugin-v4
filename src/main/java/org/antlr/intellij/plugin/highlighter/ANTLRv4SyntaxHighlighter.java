package org.antlr.intellij.plugin.highlighter;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import org.antlr.intellij.plugin.ANTLRv4TokenTypes;
import org.antlr.intellij.plugin.adaptors.ANTLRv4LexerAdaptor;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;
import static org.antlr.intellij.plugin.ANTLRv4TokenTypes.*;


public class ANTLRv4SyntaxHighlighter extends SyntaxHighlighterBase {
    public static final TextAttributesKey KEYWORD =
        createTextAttributesKey("ANTLRv4_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
    
    public static final TextAttributesKey SPECIAL_KEYWORD =
        createTextAttributesKey("ANTLRv4_SPECIAL_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
    
    
    public static final TextAttributesKey RULENAME =
        createTextAttributesKey("ANTLRv4_RULENAME", DefaultLanguageHighlighterColors.CLASS_NAME);
    
    public static final TextAttributesKey TOKENNAME =
        createTextAttributesKey("ANTLRv4_TOKENNAME", DefaultLanguageHighlighterColors.CONSTANT);
    
    public static final TextAttributesKey STRING =
        createTextAttributesKey("ANTLRv4_STRING", DefaultLanguageHighlighterColors.STRING);
    
    public static final TextAttributesKey LINE_COMMENT =
        createTextAttributesKey("ANTLRv4_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
    
    public static final TextAttributesKey DOC_COMMENT =
        createTextAttributesKey("ANTLRv4_DOC_COMMENT", DefaultLanguageHighlighterColors.DOC_COMMENT);
    
    public static final TextAttributesKey BLOCK_COMMENT =
        createTextAttributesKey("ANTLRv4_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT);
    
    public static final TextAttributesKey INT =
        createTextAttributesKey("ANTLRv4_INT", DefaultLanguageHighlighterColors.NUMBER);
    
    public static final TextAttributesKey BRACE =
        createTextAttributesKey("ANTLRv4_BRACES", DefaultLanguageHighlighterColors.BRACES);
    
    
    private static final TextAttributesKey[] BAD_CHAR_KEYS = pack(HighlighterColors.BAD_CHARACTER);
    private static final TextAttributesKey[] STRING_KEYS = pack(STRING);
    private static final TextAttributesKey[] COMMENT_KEYS = new TextAttributesKey[]{LINE_COMMENT, DOC_COMMENT, BLOCK_COMMENT};
    private static final TextAttributesKey[] EMPTY_KEYS = new TextAttributesKey[0];
    
    
    @Override
    public @NotNull Lexer getHighlightingLexer() {
        return new ANTLRv4LexerAdaptor(new ANTLRv4Lexer(null));
    }
    
    
    @Override
    public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
        if (KEYWORDS.contains(tokenType)) {
            return pack(KEYWORD);
        }
        
        if (ANTLRv4TokenTypes.SPECIAL_KEYWORD.contains(tokenType)) {
            return pack(SPECIAL_KEYWORD);
        }
        
        if (BRACES.contains(tokenType)) {
            return pack(BRACE);
        }
        
        if (Objects.equals(tokenType, TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.TOKEN_REF))) {
            return pack(TOKENNAME);
        }
        
        if (Objects.equals(tokenType, TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.POUND))) {
            // give the pound sign the same color as the semantic highlighting for alz labels
            // is configured. This is a mix of both highlighter.
            return pack(ANTLRv4SemanticHighlighter.RULE_LABEL);
        }
        
        if (Objects.equals(tokenType, TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.RULE_REF))) {
            return pack(RULENAME);
        }
        
        if (Objects.equals(tokenType, TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.INT))) {
            return pack(INT);
        }
        
        if (Objects.equals(tokenType, TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.STRING_LITERAL))
            || Objects.equals(tokenType, TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.UNTERMINATED_STRING_LITERAL))) {
            return STRING_KEYS;
        }
        
        if (Objects.equals(tokenType, TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.BLOCK_COMMENT))) {
            return COMMENT_KEYS;
        }
        
        if (Objects.equals(tokenType, TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.DOC_COMMENT))) {
            return COMMENT_KEYS;
        }
        
        if (Objects.equals(tokenType, TOKEN_ELEMENT_TYPES.get(ANTLRv4Lexer.LINE_COMMENT))) {
            return COMMENT_KEYS;
        }
        
        if (Objects.equals(tokenType, BAD_TOKEN_TYPE)) {
            return BAD_CHAR_KEYS;
        }
        
        return EMPTY_KEYS;
    }
}
