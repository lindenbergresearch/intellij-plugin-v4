package org.antlr.intellij.plugin.configdialogs;

import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.options.colors.AttributesDescriptor;
import com.intellij.openapi.options.colors.ColorDescriptor;
import com.intellij.openapi.options.colors.ColorSettingsPage;
import org.antlr.intellij.plugin.ANTLRv4SyntaxHighlighter;
import org.antlr.intellij.plugin.Icons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Map;

public class ANTLRv4ColorsPage implements ColorSettingsPage {
    private static final AttributesDescriptor[] ATTRIBUTES = new AttributesDescriptor[]{
        new AttributesDescriptor("Lexer rule", ANTLRv4SyntaxHighlighter.TOKENNAME),
        new AttributesDescriptor("Parser rule", ANTLRv4SyntaxHighlighter.RULENAME),
        new AttributesDescriptor("Keyword", ANTLRv4SyntaxHighlighter.KEYWORD),
        new AttributesDescriptor("String", ANTLRv4SyntaxHighlighter.STRING),
        new AttributesDescriptor("Number", ANTLRv4SyntaxHighlighter.INT),
        new AttributesDescriptor("Braces", ANTLRv4SyntaxHighlighter.BRACE),
        new AttributesDescriptor("Block comment", ANTLRv4SyntaxHighlighter.BLOCK_COMMENT),
        new AttributesDescriptor("Line comment", ANTLRv4SyntaxHighlighter.LINE_COMMENT),
        new AttributesDescriptor("Doc comment", ANTLRv4SyntaxHighlighter.DOC_COMMENT),

    };


    @Nullable
    @Override
    public Icon getIcon() {
        return Icons.FILE;
    }


    @NotNull
    @Override
    public SyntaxHighlighter getHighlighter() {
        return new ANTLRv4SyntaxHighlighter();
    }


    @NotNull
    @Override
    public String getDemoText() {
        return
            "grammar Foo;\n\n" + "options { tokenVocab=DSPLLexer; }\n\n" +
                '\n' +
                "compilationUnit : STUFF EOF;\n" +
                '\n' +
                "STUFF : 'stuff' -> pushMode(OTHER_MODE);\n" +
                "WS : [ \\t]+ -> channel(HIDDEN);\n" +
                "NEWLINE : [\\r\\n]+ -> type(WS);\n" +
                "BAD_CHAR : . -> skip;\n" +
                "INT : [1..9] [0..9]*\n" +
                '\n' +
                "mode OTHER_MODE;\n" +
                '\n' +
                "KEYWORD : 'keyword' -> popMode;\n";
    }


    @Nullable
    @Override
    public Map<String, TextAttributesKey> getAdditionalHighlightingTagToDescriptorMap() {
        return null;
    }


    @NotNull
    @Override
    public AttributesDescriptor @NotNull [] getAttributeDescriptors() {
        return ATTRIBUTES;
    }


    @NotNull
    @Override
    public ColorDescriptor @NotNull [] getColorDescriptors() {
        return ColorDescriptor.EMPTY_ARRAY;
    }


    @NotNull
    @Override
    public String getDisplayName() {
        return "ANTLR";
    }
}
