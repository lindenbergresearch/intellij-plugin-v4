package org.antlr.intellij.plugin.preview;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.antlr.intellij.plugin.parsing.ParsingResult;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.LexerGrammar;
import org.antlr.v4.tool.Rule;

/**
 * Track everything associated with the state of the preview window.
 * For each grammar, we need to track an InputPanel (with <= 2 editor objects)
 * that we will flip to every time we come back to a specific grammar,
 * uniquely identified by the fully-qualified grammar name.
 * <p>
 * Before parsing can begin, we need to know the start rule. That means that
 * we should not show an editor until this field is filled in.
 * <p>
 * The plug-in controller should update all of these elements atomically so
 * they are self-consistent.  We must be careful then to send these fields
 * around together as a unit instead of asking the controller for the
 * elements piecemeal. That could get g and lg for different grammar files,
 * for example.
 */
public class PreviewState {
    public Project project;
    public VirtualFile grammarFile;
    
    public Grammar grammar;
    public LexerGrammar lexerGrammar;
    
    public String startRuleName;
    public CharSequence manualInputText = ""; // save input when switching grammars
    
    public VirtualFile inputFile; // save input file when switching grammars
    public ParsingResult parsingResult;
    
    /**
     * The current input editor (inputEditor or fileEditor) for this grammar
     * in InputPanel. This can be null when a PreviewState and InputPanel
     * are created out of sync. Depends on order IDE opens files vs
     * creates preview pane.
     */
    private Editor inputEditor;
    
    
    /**
     * Create preview state class.
     *
     * @param project     The assigned project.
     * @param grammarFile The assigned grammar-file.
     */
    public PreviewState(Project project, VirtualFile grammarFile) {
        this.project = project;
        this.grammarFile = grammarFile;
    }
    
    
    /**
     * Returns the assigned input editor instance.
     * Thread-save.
     *
     * @return An editor instance.
     */
    public synchronized Editor getInputEditor() {
        return inputEditor;
    }
    
    
    /**
     * Sets the assigned input editor instance.
     * Thread-save.
     *
     * @param inputEditor An editor instance.
     */
    public synchronized void setInputEditor(Editor inputEditor) {
        releaseEditor();
        this.inputEditor = inputEditor;
    }
    
    
    /**
     * Returns the main active grammar.
     *
     * @return Grammar instance.
     */
    public Grammar getMainGrammar() {
        return
            grammar != null ? grammar : lexerGrammar;
    }
    
    
    /**
     * Returns the grammar name as valid identifier.
     *
     * @return Grammar name as String.
     */
    public String getGrammarName() {
        String g = getMainGrammar().name.trim();
        return g.replace(' ', '_');
    }
    
    
    /**
     * Check if rule exists in current grammar.
     *
     * @param name The name of the rune.
     * @return True if rule exists.
     */
    public boolean existsStartRule(String name) {
        if (grammar != null) {
            Rule rule = grammar.getRule(name);
            return (rule != null);
        }
        
        return true;
    }
    
    
    /**
     * Release input editor.
     */
    public synchronized void releaseEditor() {
        // It would appear that the project closed event occurs before these
        // close grammars sometimes. Very strange. check for null editor.
        if (inputEditor != null) {
            final EditorFactory factory = EditorFactory.getInstance();
            factory.releaseEditor(inputEditor);
            inputEditor = null;
        }
    }
    
}
