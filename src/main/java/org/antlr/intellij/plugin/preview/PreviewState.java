package org.antlr.intellij.plugin.preview;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.diagnostic.Logger;
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
    private static final Logger LOG =
        Logger.getInstance("ANTLR InputPanel");
    
    public Project project;
    public VirtualFile grammarFile;
    
    public Grammar grammar;
    public LexerGrammar lexerGrammar;
    
    public String startRuleName;
    public CharSequence manualInputText = ""; // save input when switching grammars
    
    public VirtualFile inputFile; // save input file when switching grammars
    public ParsingResult parsingResult;
    
    private final PropertiesComponent propertiesComponent;
    
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
        
        LOG.info("create preview-state with project: " + project.getName().trim() + " grammar: " + grammarFile.getName());
        
        propertiesComponent = PropertiesComponent.getInstance(project);
        recoverPreviewData();
    }
    
    
    /**
     * Saves the input text for testing grammars and the assigned start-rule
     * for later recovery.
     */
    public void persistPreviewData() {
        if (getMainGrammar() == null || startRuleName == null || startRuleName.isEmpty())
            return;
        
        // build grammar dependent config keys
        String inputTextPropertiesKey = "org.antlr.intellij.plugin.preview.input." + getGrammarName();
        String startRulePropertiesKey = "org.antlr.intellij.plugin.preview.startRule." + getGrammarName();
        
        propertiesComponent.setValue(
            inputTextPropertiesKey,
            manualInputText.toString()
        );
        
        propertiesComponent.setValue(
            startRulePropertiesKey,
            startRuleName
        );
        
        LOG.info("save start-rule for session recover: '" + startRuleName + '\'');
    }
    
    
    /**
     * Recovers the input text for testing grammars and the assigned start-rule.
     */
    public void recoverPreviewData() {
        // build grammar dependent config keys
        String inputTextPropertiesKey = "org.antlr.intellij.plugin.preview.input." + getGrammarName();
        String startRulePropertiesKey = "org.antlr.intellij.plugin.preview.startRule." + getGrammarName();
        
        manualInputText =
            propertiesComponent.getValue(inputTextPropertiesKey);
        
        startRuleName = PropertiesComponent.getInstance(project).getValue(
            startRulePropertiesKey
        );
        
        if (!existsStartRule(startRuleName)) {
            startRuleName = "";
        }
        LOG.info("recover start-rule: '" + startRuleName + '\'');
        
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
        String g = getMainGrammar() == null ?
            grammarFile.getName().replace(".g4", "") :
            getMainGrammar().name;
        
        return g.trim().replace(' ', '_');
    }
    
    
    /**
     * Test for valid grammar setup.
     *
     * @return True if valid grammar has been set.
     */
    public boolean hasValidGrammar() {
        return !(grammar == null || lexerGrammar == null);
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
     * Safely returns the current set start-rule name.
     *
     * @return The start-rule name.
     */
    public String getStartRuleName() {
        return startRuleName == null ? "" : startRuleName;
    }
    
    
    /**
     * Returns the manual input text for testing grammars.
     *
     * @return Input text.
     */
    public CharSequence getManualInputText() {
        return manualInputText == null ? "" : manualInputText;
    }
    
    
    /**
     * Update the manual input text for testing grammars.
     *
     * @param text Input text.
     */
    public void setManualInputText(CharSequence text) {
        manualInputText = text != null ? text : "";
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
