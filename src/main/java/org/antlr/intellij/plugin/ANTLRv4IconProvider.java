package org.antlr.intellij.plugin;

import com.intellij.ide.IconProvider;
import com.intellij.psi.PsiElement;
import org.antlr.intellij.plugin.psi.LexerRuleRefNode;
import org.antlr.intellij.plugin.psi.ModeSpecNode;
import org.antlr.intellij.plugin.psi.ParserRuleRefNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ANTLRv4IconProvider extends IconProvider {
    
    @Nullable
    @Override
    public Icon getIcon(@NotNull PsiElement element, int flags) {
        if (element instanceof LexerRuleRefNode) {
            return ANTLRv4Icons.LEXER_RULE;
        } else if (element instanceof ParserRuleRefNode) {
            return ANTLRv4Icons.PARSER_RULE;
        } else if (element instanceof ANTLRv4FileRoot) {
            return ANTLRv4Icons.FILE;
        } else if (element instanceof ModeSpecNode) {
            return ANTLRv4Icons.MODE;
        }
        return null;
    }
}
