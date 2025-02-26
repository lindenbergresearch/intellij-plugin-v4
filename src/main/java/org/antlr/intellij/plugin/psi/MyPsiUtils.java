package org.antlr.intellij.plugin.psi;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiElementFilter;
import com.intellij.psi.util.PsiTreeUtil;
import org.antlr.intellij.plugin.ANTLRv4FileRoot;
import org.antlr.intellij.plugin.ANTLRv4Language;
import org.antlr.intellij.plugin.ANTLRv4TokenTypes;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.antlr.intellij.plugin.ANTLRv4TokenTypes.RULE_ELEMENT_TYPES;
import static org.antlr.intellij.plugin.ANTLRv4TokenTypes.TOKEN_ELEMENT_TYPES;

public class MyPsiUtils {
    
    /**
     * Tests a Rule Psi IElement against an ANTLRv4 token-id-constant from the parser.
     *
     * @param type         IElementType to check.
     * @param antlrTokenID The ANTLRv4 internal token-id to match.
     * @return True if the IElementType matches its ANTLRv4 counterpart.
     */
    public static boolean isRuleIElement(IElementType type, int antlrTokenID) {
        return Objects.equals(type, RULE_ELEMENT_TYPES.get(antlrTokenID));
    }
    
    
    /**
     * Tests a Token Psi IElement against an ANTLRv4 token-id-constant from the lexer.
     *
     * @param type         IElementType to check.
     * @param antlrTokenID The ANTLRv4 internal token-id to match.
     * @return True if the IElementType matches its ANTLRv4 counterpart.
     */
    public static boolean isTokenIElement(IElementType type, int antlrTokenID) {
        return Objects.equals(type, TOKEN_ELEMENT_TYPES.get(antlrTokenID));
    }
    
    
    /**
     * Tests a PsiElement for matching its ANTLRv4 rule-id counterpart in the parser.
     *
     * @param element      PsiElement to check.
     * @param antlrTokenID The ANTLRv4 internal token-id to match.
     * @return True if the PsiElement matches its ANTLRv4 counterpart.
     */
    public static boolean isRuleElement(PsiElement element, int antlrTokenID) {
        return isRuleIElement(element.getNode().getElementType(), antlrTokenID);
    }
    
    
    /**
     * Tests a PsiElement for matching its ANTLRv4 token-id counterpart in the lexer.
     *
     * @param element      PsiElement to check.
     * @param antlrTokenID The ANTLRv4 internal token-id to match.
     * @return True if the PsiElement matches its ANTLRv4 counterpart.
     */
    public static boolean isTokenElement(PsiElement element, int antlrTokenID) {
        return isTokenIElement(element.getNode().getElementType(), antlrTokenID);
    }
    
    
    @Nullable
    public static PsiElement findFirstChildOfType(final PsiElement parent, IElementType type) {
        return findFirstChildOfType(parent, TokenSet.create(type));
    }
    
    
    /**
     * traverses the psi tree depth-first, returning the first it finds with the given types
     *
     * @param parent the element whose children will be searched
     * @param types  the types to search for
     * @return the first child, or null;
     */
    @Nullable
    public static PsiElement findFirstChildOfType(final PsiElement parent, final TokenSet types) {
        var iterator = findChildrenOfType(parent, types).iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        
        return null;
    }
    
    
    public static Iterable<PsiElement> findChildrenOfType(final PsiElement parent, IElementType type) {
        return findChildrenOfType(parent, TokenSet.create(type));
    }
    
    
    /**
     * Like PsiTreeUtil.findChildrenOfType, except no collection is created and it doesn't use recursion.
     *
     * @param parent the element whose children will be searched
     * @param types  the types to search for
     * @return an iterable that will traverse the psi tree depth-first, including only the elements
     * whose type is contained in the provided tokenset.
     */
    public static Iterable<PsiElement> findChildrenOfType(final PsiElement parent, final TokenSet types) {
        var psiElements = PsiTreeUtil.collectElements(parent, input -> {
            var node = input.getNode();
            return node != null && types.contains(node.getElementType());
        });
        
        return Arrays.asList(psiElements);
    }
    
    
    /**
     * Finds the first {@link RuleSpecNode} or {@link ModeSpecNode} matching the {@code ruleName} defined in
     * the given {@code grammar}.
     * <p>
     * Rule specs can be either children of the {@link RulesNode}, or under one of the {@code mode}s defined in
     * the grammar. This means we have to walk the whole grammar to find matching candidates.
     */
    public static PsiElement findSpecNode(GrammarSpecNode grammar, final String ruleName) {
        PsiElementFilter definitionFilter = element1 -> {
            if (!(element1 instanceof RuleSpecNode)) {
                return false;
            }
            
            var id = ((RuleSpecNode) element1).getNameIdentifier();
            return id != null && id.getText().equals(ruleName);
        };
        
        var ruleSpec = PsiTreeUtil.collectElements(grammar, definitionFilter);
        if (ruleSpec.length > 0) {
            return ruleSpec[0];
        }
        
        return null;
    }
    
    
    public static PsiElement createLeafFromText(Project project, PsiElement context, String text, IElementType type) {
        var factory = (PsiFileFactoryImpl) PsiFileFactory.getInstance(project);
        var el = factory.createElementFromText(text,
            ANTLRv4Language.INSTANCE,
            type,
            context
        );
        
        if (el == null) {
            return null;
        }
        
        return PsiTreeUtil.getDeepestFirst(el); // forces parsing of file!!
        // start rule depends on root passed in
    }
    
    
    public static void replacePsiFileFromText(final Project project, final PsiFile psiFile, String text) {
        final var newPsiFile = createFile(project, text);
        
        WriteCommandAction.runWriteCommandAction(project, () -> {
            psiFile.deleteChildRange(psiFile.getFirstChild(), psiFile.getLastChild());
            psiFile.addRange(newPsiFile.getFirstChild(), newPsiFile.getLastChild());
        });
    }
    
    
    public static PsiFile createFile(Project project, String text) {
        var fileName = "a.g4"; // random name but must be .g4
        var factory = (PsiFileFactoryImpl) PsiFileFactory.getInstance(project);
        return factory.createFileFromText(fileName, ANTLRv4Language.INSTANCE,
            text, false, false);
    }
    
    
    public static PsiElement[] collectAtActions(PsiElement root, final String tokenText) {
        return PsiTreeUtil.collectElements(root, element -> {
            var psiElement = element.getContext();
            if (psiElement != null) {
                psiElement = psiElement.getContext();
            }
            
            return psiElement instanceof AtAction &&
                element instanceof ParserRuleRefNode &&
                element.getText().equals(tokenText);
        });
    }
    
    
    /**
     * Search all internal and leaf nodes looking for token or internal node
     * with specific text.
     * This saves having to create lots of java classes just to identify psi nodes.
     */
    public static PsiElement[] collectNodesWithName(PsiElement root, final String tokenText) {
        return PsiTreeUtil.collectElements(root, element -> {
            var tokenTypeName = element.getNode().getElementType().toString();
            return tokenTypeName.equals(tokenText);
        });
    }
    
    
    public static PsiElement[] collectNodesWithText(PsiElement root, final String text) {
        return PsiTreeUtil.collectElements(root, element -> element.getText().equals(text));
    }
    
    
    public static PsiElement[] collectChildrenOfType(PsiElement root, final IElementType tokenType) {
        List<PsiElement> elems = new ArrayList<>();
        for (var child : root.getChildren()) {
            if (child.getNode().getElementType().equals(tokenType)) {
                elems.add(child);
            }
        }
        return elems.toArray(new PsiElement[0]);
    }
    
    
    public static PsiElement findChildOfType(PsiElement root, final IElementType tokenType) {
        for (var child : root.getChildren()) {
            if (child.getNode().getElementType().equals(tokenType)) {
                return child;
            }
        }
        
        return null;
    }
    
    
    public static PsiElement[] collectChildrenWithText(PsiElement root, final String text) {
        List<PsiElement> elems = new ArrayList<>();
        
        for (var child : root.getChildren()) {
            if (child.getText().equals(text)) {
                elems.add(child);
            }
        }
        
        return elems.toArray(new PsiElement[0]);
    }
    
    
    // Look for stuff like: options { tokenVocab=ANTLRv4Lexer; superClass=Foo; }
    public static String findTokenVocabIfAny(ANTLRv4FileRoot file) {
        String vocabName = null;
        var options = collectNodesWithName(file, "option");
        
        for (var option : options) {
            var tokenVocab = collectChildrenWithText(option, "tokenVocab");
            
            if (tokenVocab.length > 0) {
                var optionNode = tokenVocab[0].getParent();// tokenVocab[0] is id node
                var ids = collectChildrenOfType(optionNode, ANTLRv4TokenTypes.RULE_ELEMENT_TYPES.get(ANTLRv4Parser.RULE_optionValue));
                vocabName = ids[0].getText();
            }
        }
        
        return vocabName;
    }
    
    
    public static PsiElement findElement(PsiElement startNode, int offset) {
        if (startNode == null) {
            return null;
        }
        
        System.out.println(Thread.currentThread().getName() + ": visit root " + startNode +
            ", offset=" + offset +
            ", class=" + startNode.getClass().getSimpleName() +
            ", text=" + startNode.getNode().getText() +
            ", node range=" + startNode.getTextRange());
        
        var firstChild = startNode.getFirstChild();
        while (firstChild != null) {
            var result = findElement(firstChild, offset);
            
            if (result != null) {
                return result;
            }
            
            firstChild = firstChild.getNextSibling();
        }
        
        return null;
    }
}
