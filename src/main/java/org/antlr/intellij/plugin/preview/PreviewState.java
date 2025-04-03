package org.antlr.intellij.plugin.preview;

import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;
import org.antlr.intellij.plugin.ANTLRv4FileType;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.parsing.ParsingResult;
import org.antlr.intellij.plugin.parsing.ParsingUtils;
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
        Logger.getInstance(PreviewState.class);
    
    @Getter private final Project project;
    @Getter private final VirtualFile grammarFile;
    
    @Getter @Setter private Grammar grammar;
    @Setter @Getter private LexerGrammar lexerGrammar;
    
    private String startRuleName;
    @Getter @Setter private boolean validStartRule;
    private CharSequence manualInputText; // save input when switching grammars
    
    @Getter @Setter private VirtualFile inputFile; // save input file when switching grammars
    @Getter @Setter private ParsingResult parsingResult;
    @Getter @Setter private double parseTime;
    
    @Getter private final PropertiesComponent propertiesComponent;
    
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
        
        var message = "create PreviewState() for grammar: " + getGrammarName();
        LOG.info(message);
        ANTLRv4PluginController.printToConsole(project, message, ConsoleViewContentType.LOG_DEBUG_OUTPUT);
        
        propertiesComponent = PropertiesComponent.getInstance(project);
        
        manualInputText = "";
        startRuleName = "";
        validStartRule = false;
//
//        reloadPreviewData();
    }
    
    
    private String getDefaultStartRuleName() {
        if (grammar != null && grammar.getRuleNames() != null) {
            return grammar.getRuleNames()[0];
        }
        
        return "";
    }
    
    
    /**
     * Saves the input text for testing grammars and the assigned start-rule
     * for later recovery.
     */
    public void persistPreviewData() {
        if (getMainGrammar() == null || startRuleName == null || startRuleName.isEmpty()) {
            startRuleName = "";
            validStartRule = false;
            return;
        }
        
        // build grammar dependent config keys
        var inputTextPropertiesKey = "org.antlr.intellij.plugin.preview.input." + getGrammarName();
        var startRulePropertiesKey = "org.antlr.intellij.plugin.preview.startRule." + getGrammarName();
        var validRulePropertiesKey = "org.antlr.intellij.plugin.preview.validStartRule." + getGrammarName();
        
        if (!existsStartRule(startRuleName)) {
            // fallback
            startRuleName = getDefaultStartRuleName();
            if (!existsStartRule(startRuleName)) {
                startRuleName = "";
                validStartRule = false;
            } else {
                validStartRule = true;
            }
        }
        
        propertiesComponent.setValue(
            inputTextPropertiesKey,
            manualInputText.toString()
        );
        
        propertiesComponent.setValue(
            startRulePropertiesKey,
            startRuleName
        );
        
        propertiesComponent.setValue(
            validRulePropertiesKey,
            validStartRule
        );
        
        LOG.info("save start-rule for session recover: '" + startRuleName + '\'');
        ANTLRv4PluginController.printToConsole(
            project,
            "persistPreviewData(name=" + startRuleName + ", valid=" + (validStartRule ? "true" : "false") + ')',
            ConsoleViewContentType.LOG_DEBUG_OUTPUT
        );
    }
    
    
    /**
     * Recovers the input text for testing grammars and the assigned start-rule.
     */
    public void reloadPreviewData() {
        // build grammar dependent config keys
        var inputTextPropertiesKey = "org.antlr.intellij.plugin.preview.input." + getGrammarName();
        var startRulePropertiesKey = "org.antlr.intellij.plugin.preview.startRule." + getGrammarName();
        var validRulePropertiesKey = "org.antlr.intellij.plugin.preview.validStartRule." + getGrammarName();
        
        manualInputText = propertiesComponent.getValue(inputTextPropertiesKey);
        startRuleName = propertiesComponent.getValue(startRulePropertiesKey);
        validStartRule = propertiesComponent.getBoolean(validRulePropertiesKey);
        
        
        ANTLRv4PluginController.printToConsole(
            project,
            "reloadPreviewData(" + startRuleName + ", valid: " + (validStartRule ? "true" : "false") + ", grammar=" + (hasValidGrammar() ? "true" : "false") + ')',
            ConsoleViewContentType.LOG_DEBUG_OUTPUT
        );
        
        if (!hasValidGrammar()) {
            validStartRule = false;
            return;
        }
        
        if (!existsStartRule(startRuleName)) {
            ANTLRv4PluginController.printToConsole(
                project,
                "start-rule does not exist or is invalid: startRule='" + startRuleName + "', valid: " + (validStartRule ? "true" : "false"),
                ConsoleViewContentType.LOG_WARNING_OUTPUT
            );
            
            startRuleName = getDefaultStartRuleName();
            if (existsStartRule(startRuleName)) {
                validStartRule = true;
                ANTLRv4PluginController.printToConsole(
                    project,
                    "using default startRule from grammar:" + startRuleName,
                    ConsoleViewContentType.LOG_DEBUG_OUTPUT
                );
            } else {
                validStartRule = false;
                startRuleName = "";
            }
        } else {
            validStartRule = true;
        }
        
        
        if (!existsStartRule(startRuleName) || !validStartRule) {
            ANTLRv4PluginController.printToConsole(project, "unable to find a start-rule for grammar: " + startRuleName, ConsoleViewContentType.LOG_WARNING_OUTPUT);
        } else {
            ANTLRv4PluginController.printToConsole(
                project,
                "reloadPreviewData(name=" + startRuleName + ", valid=" + "true" + ')',
                ConsoleViewContentType.LOG_DEBUG_OUTPUT
            );
        }
        
        LOG.info("reload start-rule: " + startRuleName + ' ' + validStartRule);
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
        ANTLRv4PluginController.printToConsole(project, "PreviewState.getInputEditor(): " + inputEditor, ConsoleViewContentType.LOG_DEBUG_OUTPUT);
        
        if (inputEditor != null) {
            ANTLRv4PluginController.printToConsole(project, "PreviewState.getInputEditor[releaseEditor](): " + inputEditor.getVirtualFile(), ConsoleViewContentType.LOG_DEBUG_OUTPUT);
            releaseEditor();
            this.inputEditor = inputEditor;
        }
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
        var g = getMainGrammar() == null ?
            grammarFile.getName().replace('.' + ANTLRv4FileType.INSTANCE.getDefaultExtension(), "") :
            getMainGrammar().name;
        
        return g.trim().replace(' ', '_');
    }
    
    
    /**
     * Test for valid grammar setup.
     *
     * @return True if valid grammar has been set.
     */
    public boolean hasValidGrammar() {
        ANTLRv4PluginController.printToConsole(project, "hasValidGrammar(grammar=" + (grammar != null ? "true" : false) + ", lexerGrammar=" + (lexerGrammar != null ? "true" : false) + ')', ConsoleViewContentType.LOG_DEBUG_OUTPUT);
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
            var rule = grammar.getRule(name);
            return (rule != null);
        }
        
        return false;
    }
    
    
    /**
     * Safely returns the current set start-rule name.
     *
     * @return The start-rule name.
     */
    public String getStartRuleName() {
        //reloadPreviewData();
        ANTLRv4PluginController.printToConsole(project, "getStartRuleName(current=" + startRuleName + ')', ConsoleViewContentType.LOG_DEBUG_OUTPUT);
        
        if (!hasValidStartRule()) {
            if (existsStartRule(getDefaultStartRuleName())) {
                startRuleName = getDefaultStartRuleName();
                validStartRule = true;
                persistPreviewData();
                ANTLRv4PluginController.printToConsole(project, "using default startRule from grammar:" + startRuleName, ConsoleViewContentType.LOG_DEBUG_OUTPUT);
            } else {
                return null;
            }
        }
        
        return startRuleName;
    }
    
    
    public String getPlainStartRuleName() {
        return startRuleName;
    }
    
    
    /**
     * Test for valid start rule.
     *
     * @return True if a valid start rule was found.
     */
    public boolean hasValidStartRule() {
        return startRuleName != null && !startRuleName.isEmpty() && existsStartRule(startRuleName);
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
        if (text != null && !text.isEmpty() && !manualInputText.equals(text)) {
            manualInputText = text;
            persistPreviewData();
        }
    }
    
    
    /**
     * Release input editor.
     */
    public synchronized void releaseEditor() {
        // It would appear that the project closed event occurs before these
        // close grammars sometimes. Very strange. check for null editor.
        if (inputEditor != null) {
            ANTLRv4PluginController.printToConsole(project, "releaseEditor(" + inputEditor.getVirtualFile() + ')', ConsoleViewContentType.LOG_DEBUG_OUTPUT);
            final var factory = EditorFactory.getInstance();
            factory.releaseEditor(inputEditor);
            inputEditor = null;
        }
    }
    
    
    public boolean isBadGrammar() {
        return grammar.equals(ParsingUtils.BAD_PARSER_GRAMMAR);
    }
    
    
    public boolean isBadLexerGrammar() {
        return grammar.equals(ParsingUtils.BAD_LEXER_GRAMMAR);
    }
    
    
    public void setStartRuleName(String startRuleName) {
        ANTLRv4PluginController.printToConsole(project, "setStartRuleName(" + startRuleName + ')', ConsoleViewContentType.LOG_DEBUG_OUTPUT);
        if (startRuleName != null && !startRuleName.equals(this.startRuleName)) {
            this.startRuleName = startRuleName;
            persistPreviewData();
        }
    }
    
    
    public Rule getGrammarRule() {
        if (validStartRule) {
            return grammar.getRule(getStartRuleName());
        }
        
        return null;
    }
    
    
    @Override
    public String toString() {
        var sr = startRuleName;
        var srValid = hasValidStartRule();
        var srExists = sr != null && srValid && existsStartRule(sr);
        
        return "PreviewState { \n" +
               "\tproject          =" + project +
               "\n\tgrammarFile    =" + grammarFile.getName() +
               "\n\tgrammar        =" + grammar +
               "\n\tvalidGrammar   =" + hasValidGrammar() +
               "\n\tisBadGrammar   =" + isBadGrammar() +
               "\n\tlexerGrammar   =" + lexerGrammar +
               
               "\n\tstartRule      =" + startRuleName +
               "\n\tvalidStartRule =" + validStartRule +
               "\n\texistsStartRule=" + existsStartRule(startRuleName) +
               "\n\texistsDefRule  =" + existsStartRule(getDefaultStartRuleName()) +
               
               "\n\tmanualInputText=" + manualInputText +
               "\n\tinputFile      =" + (inputFile != null ? inputFile.getName() : "-") +
               
               "\n\tparsingResult  =" + parsingResult +
               "\n\tparseTime      =" + parseTime +
               "\n}";
    }
}
