package org.antlr.intellij.plugin.resolve;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.intellij.plugin.ANTLRv4FileRoot;
import org.antlr.intellij.plugin.ANTLRv4FileType;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.psi.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import static org.antlr.intellij.plugin.ANTLRv4TokenTypes.RULE_ELEMENT_TYPES;

public class TokenVocabResolver {
    
    /**
     * If this reference is the value of a {@code tokenVocab} option, returns the corresponding
     * grammar file.
     */
    @Nullable
    public static PsiFile resolveTokenVocabFile(PsiElement reference) {
        var optionValue = PsiTreeUtil.findFirstParent(reference, el -> isOptionValue(el));
        
        if (optionValue != null) {
            var option = optionValue.getParent();
            
            if (option != null) {
                var optionName = PsiTreeUtil.getDeepestFirst(option);
                
                if (optionName.getText().equals("tokenVocab")) {
                    var text = StringUtils.strip(reference.getText(), "'");
                    return findRelativeFile(text, reference.getContainingFile());
                }
            }
        }
        
        return null;
    }
    
    
    /**
     * Tries to find a declaration named {@code ruleName} in the {@code tokenVocab} file if it exists.
     */
    @Nullable
    public static PsiElement resolveInTokenVocab(GrammarElementRefNode reference, String ruleName) {
        var tokenVocab = MyPsiUtils.findTokenVocabIfAny((ANTLRv4FileRoot) reference.getContainingFile());
        
        if (tokenVocab != null) {
            var tokenVocabFile = findRelativeFile(tokenVocab, reference.getContainingFile());
            
            if (tokenVocabFile != null) {
                var lexerGrammar = PsiTreeUtil.findChildOfType(tokenVocabFile, GrammarSpecNode.class);
                var node = MyPsiUtils.findSpecNode(lexerGrammar, ruleName);
                
                if (node instanceof LexerRuleSpecNode) {
                    // fragments are not visible to the parser
                    if (!((LexerRuleSpecNode) node).isFragment()) {
                        return node;
                    }
                }
                
                if (node instanceof TokenSpecNode) {
                    return node;
                }
            }
        }
        
        return null;
    }
    
    
    private static boolean isOptionValue(PsiElement el) {
        var node = el.getNode();
        return node != null && node.getElementType().equals(RULE_ELEMENT_TYPES.get(ANTLRv4Parser.RULE_optionValue));
    }
    
    
    /**
     * Looks for an ANTLR grammar file named {@code <baseName>}.g4 next to the given {@code sibling} file.
     */
    static PsiFile findRelativeFile(String baseName, PsiFile sibling) {
        var parentDirectory = sibling.getParent();
        
        if (parentDirectory != null) {
            var candidate = parentDirectory.findFile(baseName + ANTLRv4FileType.INSTANCE.getDefaultExtension());
            
            if (candidate instanceof ANTLRv4FileRoot) {
                return candidate;
            }
        }
        
        return null;
    }
}
