package org.antlr.intellij.plugin.preview;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.codeInsight.hint.HintManagerImpl;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorMarkupModel;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.ComponentWithBrowseButton.BrowseFolderActionListener;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.LightweightHint;
import com.intellij.ui.components.JBScrollPane;
import lombok.Getter;
import lombok.Setter;
import org.antlr.intellij.adaptor.parser.SyntaxError;
import org.antlr.intellij.plugin.ANTLRv4Icons;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.actions.MyActionUtils;
import org.antlr.intellij.plugin.parsing.ParsingUtils;
import org.antlr.intellij.plugin.parsing.PreviewParser;
import org.antlr.intellij.plugin.profiler.ProfilerPanel;
import org.antlr.runtime.CommonToken;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.LexerNoViableAltException;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.AmbiguityInfo;
import org.antlr.v4.runtime.atn.ContextSensitivityInfo;
import org.antlr.v4.runtime.atn.LookaheadEventInfo;
import org.antlr.v4.runtime.atn.PredicateEvalInfo;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.misc.Utils;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.tool.ast.GrammarAST;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Not a view itself but delegates to one.
public class InputPanel implements Disposable {
    private static final Logger LOG = Logger.getInstance(InputPanel.class);
    
    private static final Key<SyntaxError> SYNTAX_ERROR = Key.create("SYNTAX_ERROR");
    private static final int MAX_STACK_DISPLAY = 30;
    private static final int MAX_HINT_WIDTH = 110;
    
    private static final int TOKEN_INFO_LAYER = HighlighterLayer.SELECTION; // Show token info over errors
    private static final int ERROR_LAYER = HighlighterLayer.ERROR;
    
    //  private static final String missingStartRuleLabelText = "No rule selected.";
    private static final String grammarFileLabelText = "%s";
    private static final String startRuleLabelText = "Rule:";
    
    /**
     * switchToGrammar() was seeing an empty slot instead of a previous
     * editor or placeHolder. Figured it was an order of operations thing
     * and synchronized add/remove ops. Works now w/o error.
     */
    private final Object swapEditorComponentLock = new Object();
    private final PreviewPanel previewPanel;
    private final PreviewEditorMouseListener editorMouseListener;
    private final List<CaretListener> caretListeners = new ArrayList<>();
    
    /**
     * state for grammar in current editor, not editor where user is typing preview input!
     */
    @Getter @Setter
    private PreviewState previewState;
    private JRadioButton inputRadioButton;
    private JRadioButton fileRadioButton;
    private JTextArea placeHolder;
    private JLabel startRuleLabel;
    private JPanel radioButtonPanel;
    private JPanel startRuleAndInputPanel;
    private TextFieldWithBrowseButton fileChooser;
    private JPanel outerMostPanel;
    private JLabel startRuleLabel2;
    private ComboBox<String> comboBox;
    private JScrollPane errorScrollPane;
    private final ErrorConsolePanel errorConsolePanel;
    
    
    private void createUIComponents() {
        outerMostPanel = new JPanel(new BorderLayout(0, 0));
        var jPanel = new JPanel(new BorderLayout(0, 0));
        var errorConsole = new JTextArea();
        comboBox = new ComboBox<>();
        
        
        comboBox.addItemListener(itemEvent -> {
            /*
             * IMPORTANT:
             * ----------
             *
             * Adding items (start-rules) to combobox and select the saved one always triggers
             * an ItemEvent, so ignore if it's not done by the user via UI.
             * Also ignore deselection events.
             *
             */
            if (!comboBox.hasFocus() || previewState == null || itemEvent.getStateChange() != ItemEvent.SELECTED) {
                return;
            }
            
            var controller = ANTLRv4PluginController.getInstance(previewState.getProject());
            if (controller == null) {
                return;
            }
            
            controller.printToConsole("comboBox.addItemListener(" + itemEvent + ", " + previewState, ConsoleViewContentType.LOG_DEBUG_OUTPUT);
            
            var newStartRuleName = itemEvent.getItem().toString();
            
            if (newStartRuleName.equals(previewState.getPlainStartRuleName())) {
                controller.printToConsole("comboBox(): startRuleName already set: " + newStartRuleName, ConsoleViewContentType.LOG_WARNING_OUTPUT);
                return;
            }
            
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                controller.setStartRuleNameEvent(
                    previewState.getGrammarFile(),
                    itemEvent.getItem().toString()
                );
            }
        });
        
        // Wrap tree viewer component in scroll pane
        errorScrollPane = new JBScrollPane(
            errorConsole,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
        );
        
        errorScrollPane.setWheelScrollingEnabled(true);
        
        jPanel.add(errorScrollPane);
        jPanel.setBorder(
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        );
    }
    
    
    public void removeErrorConsole() {
        getComponent().remove(errorScrollPane);
    }
    
    
    public InputPanel(final PreviewPanel previewPanel) {
        var layout = new WrappedFlowLayout(0, 0);
        $$$setupUI$$$();
        layout.setAlignment(FlowLayout.LEFT);
        this.startRuleAndInputPanel.setLayout(layout);
        this.previewPanel = previewPanel;
        errorConsolePanel = previewPanel.getErrorConsolePanel();
        
        var singleFileDescriptor = FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor();
        var browseActionListener =
            new BrowseFolderActionListener<>(
                "Select Input File", null,
                fileChooser,
                previewPanel.getProject(),
                singleFileDescriptor,
                TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
            ) {
                @Override
                protected void onFileChosen(@NotNull VirtualFile chosenFile) {
                    // this next line is the code taken from super; pasted in
                    // to avoid compile error on super.onFileCho[o]sen
                    TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT.setText(
                        fileChooser.getChildComponent(),
                        chosenFileToResultingText(chosenFile)
                    );
                    InputPanel.this.onFileChosen(chosenFile);
                }
            };
        fileChooser.getTextField().addActionListener(e -> {
            var chosenFile = VirtualFileManager.getInstance()
                .getFileSystem("file")
                .findFileByPath(fileChooser.getText());
            onFileChosen(chosenFile);
        });
        
        fileChooser.addActionListener(browseActionListener);
        fileChooser.addActionListener(e -> fileRadioButton.setSelected(true));
        
        fileChooser.setTextFieldPreferredWidth(40);
        
        inputRadioButton.addActionListener(e -> selectInputEvent());
        fileRadioButton.addActionListener(e -> selectFileEvent());
        
        resetStartRuleLabel();
        setupStartRuleLabelUI();
        editorMouseListener = new PreviewEditorMouseListener(this);
    }
    
    
    /**
     * Clear decision stuff but leave syntax errors
     */
    public static void clearDecisionEventHighlighters(Editor editor) {
        removeHighlighters(editor, ProfilerPanel.DECISION_EVENT_INFO_KEY);
    }
    
    
    /**
     * Remove any previous underlining or boxing, but not errors or decision event info
     */
    public static void clearTokenInfoHighlighters(Editor editor) {
        var markupModel = editor.getMarkupModel();
        for (var r : markupModel.getAllHighlighters()) {
            if (r.getUserData(ProfilerPanel.DECISION_EVENT_INFO_KEY) == null &&
                r.getUserData(SYNTAX_ERROR) == null) {
                markupModel.removeHighlighter(r);
            }
        }
    }
    
    
    /**
     * Display syntax errors, hints in tooltips if under the cursor
     */
    public static void showTooltips(Editor editor, @NotNull PreviewState previewState, int offset) {
        if (previewState.getParsingResult() == null) {
            return; // no results?
        }
        
        // Turn off any tooltips if none under the cursor
        // find the highlighter associated with this offset
        var highlightersAtOffset = MyActionUtils.getRangeHighlightersAtOffset(editor, offset);
        if (highlightersAtOffset.isEmpty()) {
            return;
        }
        
        List<String> msgList = new ArrayList<>();
        var foundDecisionEvent = false;
        
        for (var r : highlightersAtOffset) {
            var eventInfo = r.getUserData(ProfilerPanel.DECISION_EVENT_INFO_KEY);
            String msg;
            if (eventInfo != null) {
                // TODO: move decision event stuff to profiler?
                if (eventInfo instanceof AmbiguityInfo) {
                    msg = "Ambiguous upon alts " + eventInfo.configs.getAlts().toString();
                } else if (eventInfo instanceof ContextSensitivityInfo) {
                    msg = "Context-sensitive";
                } else if (eventInfo instanceof LookaheadEventInfo) {
                    var k = eventInfo.stopIndex - eventInfo.startIndex + 1;
                    msg = "Deepest lookahead k=" + k;
                } else if (eventInfo instanceof PredicateEvalInfo evalInfo) {
                    msg = ProfilerPanel.getSemanticContextDisplayString(
                        evalInfo,
                        previewState,
                        evalInfo.semctx, evalInfo.predictedAlt,
                        evalInfo.evalResult
                    );
                    msg += (!evalInfo.fullCtx ? " (DFA)" : "");
                } else {
                    msg = "Unknown decision event: " + eventInfo;
                }
                foundDecisionEvent = true;
            } else {
                // error tool tips
                var errorUnderCursor = r.getUserData(SYNTAX_ERROR);
                
                if (errorUnderCursor != null) {
                    msg = getErrorDisplayString(errorUnderCursor);
                } else {
                    msg = "Unknown error";
                }
                
                if (msg.length() > MAX_HINT_WIDTH) {
                    msg = msg.substring(0, MAX_HINT_WIDTH) + "...";
                }
                
                if (msg.indexOf('<') >= 0) {
                    msg = msg.replaceAll("<", "&lt;");
                }
            }
            
            msgList.add(msg);
        }
        
        var combinedMsg = Utils.join(msgList.iterator(), "\n");
        var hintMgr = (HintManagerImpl) HintManager.getInstance();
        
        if (foundDecisionEvent) {
            showDecisionEventToolTip(editor, offset, hintMgr, combinedMsg);
        } else {
            showPreviewEditorErrorToolTip(editor, offset, hintMgr, combinedMsg);
        }
    }
    
    
    public static void showPreviewEditorErrorToolTip(Editor editor, int offset, HintManagerImpl hintMgr, String msg) {
        var flags = HintManager.HIDE_BY_ANY_KEY |
                    HintManager.HIDE_BY_TEXT_CHANGE |
                    HintManager.HIDE_BY_SCROLLING;
        
        var timeout = 0; // default?
        hintMgr.showErrorHint(editor, msg, offset, offset + 1, HintManager.ABOVE, flags, timeout);
    }
    
    
    public static void showDecisionEventToolTip(Editor editor, int offset, HintManagerImpl hintMgr, String msg) {
        var flags = HintManager.HIDE_BY_ANY_KEY |
                    HintManager.HIDE_BY_TEXT_CHANGE |
                    HintManager.HIDE_BY_SCROLLING;
        
        var timeout = 0; // default?
        var infoLabel = HintUtil.createInformationLabel(msg);
        var hint = new LightweightHint(infoLabel);
        final var pos = editor.offsetToLogicalPosition(offset);
        final var p = HintManagerImpl.getHintPosition(hint, editor, pos, HintManager.ABOVE);
        hintMgr.showEditorHint(hint, editor, p, flags, timeout, false);
    }
    
    
    public static void removeHighlighters(Editor editor, Key<?> key) {
        // Remove anything with user data accessible via key
        var markupModel = editor.getMarkupModel();
        for (var r : markupModel.getAllHighlighters()) {
            if (r.getUserData(key) != null) {
                markupModel.removeHighlighter(r);
            }
        }
    }
    
    
    public static String getErrorDisplayString(SyntaxError e) {
        return "line " + e.getLine() + ':' + e.getCharPositionInLine() + ' ' + e.getMessage();
    }
    
    
    private void onFileChosen(VirtualFile chosenFile) {
        if (previewState != null) {
            previewState.setInputFile(chosenFile);
        }
        
        selectFileEvent();
    }
    
    
    public JPanel getComponent() {
        return outerMostPanel;
    }
    
    
    public void selectInputEvent() {
        inputRadioButton.setSelected(true);
        previewPanel.clearParseTree();
        clearErrorConsole();
        
        // wipe old and make new one
        if (previewState != null) {
            releaseEditor(previewState);
            createManualInputPreviewEditor(previewState);
        }
    }
    
    
    public void createManualInputPreviewEditor(final PreviewState previewState) {
        final var factory = EditorFactory.getInstance();
        var doc = factory.createDocument("");
        
        var editor = createPreviewEditor(previewState.getGrammarFile(), doc, false);
        setEditorComponent(editor.getComponent()); // do before setting state
        previewState.setInputEditor(editor);
        
        // Set text last to trigger change events
        ApplicationManager.getApplication().runWriteAction(
            () -> doc.setText(previewState.getManualInputText())
        );
        
        doc.addDocumentListener(
            new DocumentListener() {
                @Override
                public void documentChanged(@NotNull DocumentEvent e) {
                    previewState.setManualInputText(e.getDocument().getCharsSequence());
                }
            }
        );
        
        
        EditorFactory.getInstance().getEventMulticaster().addSelectionListener(new SelectionListener() {
            @Override
            public void selectionChanged(@NotNull SelectionEvent e) {
                var editor = e.getEditor();
                var start = e.getNewRange().getStartOffset();
                var end = e.getNewRange().getEndOffset();
                
                if (editor.getVirtualFile() != null) {
                    return;
                }
                
                if (start != end) {
                    handleTextSelection(start, end, editor);
                }
            }
        }, this);
    }
    
    
    private void handleTextSelection(int start, int end, Editor editor) {
        if (!(editor.getDocument() instanceof DocumentEx)) return;
        editor.getMarkupModel().removeAllHighlighters();
        
        var project = editor.getProject();
        if (project == null) return;
        
        var result = previewState.getParsingResult();
        
        if (result == null) {
            return;
        }
        
        var parser = (PreviewParser) previewState.getParsingResult().parser;
        var tokenStream = (CommonTokenStream) parser.getInputStream();
        var allTokens = tokenStream.getTokens();
        
        var selectedTokens = allTokens.stream()
            .filter(token -> token.getStopIndex() >= start && token.getStartIndex() <= end)
            .toList();
        
        var list = editor.getVirtualFile() + " [";
        for (var token : selectedTokens) {
            list += ("text='" + token.getText() + " type=" + token + "' (" + token.getStartIndex() + '-' + token.getStopIndex() + "), ");
        }
        list += "]";
        
        ANTLRv4PluginController.printToConsole(project, "Selection: " + list, ConsoleViewContentType.LOG_DEBUG_OUTPUT);
    }
    
    
    public void selectFileEvent() {
        fileRadioButton.setSelected(true);
        
        if (previewState == null) {
            return;
        }
        
        var inputFile = previewState.getInputFile();
        if (inputFile == null) {
            errorConsolePanel.add("Invalid input file!");
            return;
        }
        
        var inputDocument = FileDocumentManager.getInstance().getDocument(inputFile);
        
        if (inputDocument == null) {
            errorConsolePanel.add("Input file does not exist or cannot be loaded: " + inputFile.getPath());
            return;
        }
        
        // get state for grammar in current editor, not editor where user is typing preview input!
        var controller = ANTLRv4PluginController.getInstance(previewPanel.getProject());
        
        if (controller == null) {
            return;
        }
        
        // wipe old and make new one
        releaseEditor(previewState);
        var editor = createPreviewEditor(controller.getCurrentGrammarFile(), inputDocument, true);
        setEditorComponent(editor.getComponent()); // do before setting state
        previewState.setInputEditor(editor);
        clearErrorConsole();
        
        previewPanel.updateParseTreeFromDoc(controller.getCurrentGrammarFile(), false);
    }
    
    
    public Editor createPreviewEditor(final VirtualFile grammarFile, Document doc, boolean readOnly) {
        final var factory = EditorFactory.getInstance();
        
        doc.addDocumentListener(
            new DocumentListener() {
                @Override
                public void documentChanged(@NotNull DocumentEvent event) {
                    if (previewPanel.isAutoRefresh()) {
                        previewPanel.updateParseTreeFromDoc(grammarFile, false);
                    }
                }
            }
        );
        
        final var editor = readOnly
            ? factory.createViewer(doc, previewPanel.getProject())
            : factory.createEditor(doc, previewPanel.getProject());
        
        
        editor.getComponent().setBorder(
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        );
        
        // force right margin
        ((EditorMarkupModel) editor.getMarkupModel()).setErrorStripeVisible(true);
        var settings = editor.getSettings();
        settings.setWhitespacesShown(true);
        settings.setLeadingWhitespaceShown(true);
        settings.setLineNumbersShown(true);
        settings.setLineMarkerAreaShown(true);
        installListeners(editor);
        
        return editor;
    }
    
    
    public void grammarFileSaved() {
        clearParseErrors();
    }
    
    
    public void switchToGrammar(PreviewState previewState, VirtualFile grammarFile) {
        this.previewState = previewState;
        
        if (previewState.getInputFile() != null) {
            fileChooser.setText(previewState.getInputFile().getPath());
            selectFileEvent();
        } else {
            selectInputEvent();
        }
        
        clearParseErrors();
        
        if (previewState.hasValidStartRule()) {
            setStartRuleName(grammarFile, previewState.getPlainStartRuleName());
        } else {
            resetStartRuleLabel();
        }
    }
    
    
    public void setEditorComponent(JComponent editor) {
        var layout = (BorderLayout) outerMostPanel.getLayout();
        
        // atomically remove old
        synchronized (swapEditorComponentLock) {
            var editorSpotComp = layout.getLayoutComponent(BorderLayout.CENTER);
            
            if (editorSpotComp != null) {
                editorSpotComp.setVisible(false);
                // remove old editor if it's there
                outerMostPanel.remove(editorSpotComp);
            }
            
            //  editor.setBorder(new BevelBorder(BevelBorder.LOWERED));
            outerMostPanel.add(editor, BorderLayout.CENTER);
        }
    }
    
    
    public Editor getInputEditor() {
        if (previewState == null) {
            // seems there are some out of sequence issues with InputPanels
            // being created, but before we get a switchToGrammar event, which
            // creates the previewState.
            return null;
        }
        var editor = previewState.getInputEditor();
        if (editor == null) {
            createManualInputPreviewEditor(previewState); // ensure we always have an input window
            return previewState.getInputEditor();
        }
        
        return editor;
    }
    
    
    public void releaseEditor(PreviewState previewState) {
        uninstallListeners(previewState.getInputEditor());
        
        // release the editor
        ANTLRv4PluginController.printToConsole(
            previewState.getProject(),
            "InputPanel.releaseEditor(" +
            previewState.getInputEditor() +
            ", lexerGrammarFile=" +
            previewState.getGrammarFile() +
            ')', ConsoleViewContentType.LOG_DEBUG_OUTPUT
        );
        
        previewState.releaseEditor();
        
        // restore the GUI
        setEditorComponent(placeHolder);
    }
    
    
    public void installListeners(Editor editor) {
        if (editor instanceof EditorEx) {
            // Avoid showing the default context menu
            ((EditorEx) editor).setContextMenuGroupId("AntlrContextMenu");
        }
        
        editor.addEditorMouseMotionListener(editorMouseListener);
        editor.addEditorMouseListener(editorMouseListener);
        
        for (var listener : caretListeners) {
            editor.getCaretModel().addCaretListener(listener);
        }
    }
    
    
    public void uninstallListeners(Editor editor) {
        if (editor == null) {
            return;
        }
        
        editor.removeEditorMouseListener(editorMouseListener);
        editor.removeEditorMouseMotionListener(editorMouseListener);
        
        for (var listener : caretListeners) {
            editor.getCaretModel().removeCaretListener(listener);
        }
    }
    
    
    public void setStartRuleName(VirtualFile grammarFile, String startRuleName) {
        if (!previewState.hasValidGrammar()) {
            startRuleLabel.setText("");
            return;
        }
        
        var grammar = previewState.getGrammar();
        var rules = grammar.rules;
        
        if (startRuleName.equals(comboBox.getItem())) {
            return;
        }
        
        ANTLRv4PluginController.printToConsole(
            previewState.getProject(),
            "InputPanel.setStartRuleName(" +
            grammarFile.getName() + ", '" +
            startRuleName + "')",
            ConsoleViewContentType.LOG_DEBUG_OUTPUT
        );
        
        final var labelGrammar = String.format(
            grammarFileLabelText,
            grammarFile.getName()
        );
        
        comboBox.setEnabled(true);
        comboBox.removeAllItems();
        
        var item = "";
        
        for (var ruleName : rules.keySet()) {
            comboBox.addItem(ruleName);
            if (startRuleName.equals(ruleName)) {
                item = ruleName;
            }
        }
        
        comboBox.setSelectedItem(item);
        startRuleLabel.setForeground(JBColor.foreground());
        startRuleLabel.setIcon(ANTLRv4Icons.FILE);
        startRuleLabel.setText(labelGrammar);
    }
    
    
    void setupStartRuleLabelUI() {
        startRuleLabel.setForeground(JBColor.foreground());
        startRuleLabel.setIcon(ANTLRv4Icons.FILE);
        startRuleLabel.setText("");
        
        startRuleLabel2.setText(startRuleLabelText);
        
        startRuleLabel.setBorder(
            BorderFactory.createEmptyBorder(0, 15, 0, 5)
        );
        
        startRuleLabel2.setBorder(
            BorderFactory.createEmptyBorder(0, 10, 0, 7)
        );
    }
    
    
    public void resetStartRuleLabel() {
        var grammarName = "???";
        
        if (previewState != null) {
            grammarName = previewState.getGrammarFile().getName();
        }
        
        startRuleLabel.setText(String.format(grammarName));
        startRuleLabel.setForeground(JBColor.RED);
        startRuleLabel.setIcon(ANTLRv4Icons.FILE);
        startRuleLabel2.setText(startRuleLabelText);
        
        
        comboBox.removeAllItems();
        comboBox.addItem("<none>");
        comboBox.setEnabled(false);
    }
    
    
    public void clearErrorConsole() {
        errorConsolePanel.clear();
    }
    
    
    public void displayErrorInParseErrorConsole(SyntaxError e) {
        var msg = getErrorDisplayString(e);
        // errorScrollPane.setVisible(true);
        errorConsolePanel.add(msg);
    }
    
    
    public void clearParseErrors() {
        var editor = getInputEditor();
        if (editor == null) return;
        
        clearInputEditorHighlighters();
        
        HintManager.getInstance().hideAllHints();
        
        clearErrorConsole();
    }
    
    
    /**
     * Clear all input highlighters
     */
    public void clearInputEditorHighlighters() {
        var editor = getInputEditor();
        if (editor == null) return;
        
        var markupModel = editor.getMarkupModel();
        markupModel.removeAllHighlighters();
    }
    
    
    /**
     * Display error messages to the console and also add annotations
     * to the preview input window.
     */
    public void showParseErrors(final List<SyntaxError> errors) {
        if (errors.isEmpty()) {
            clearInputEditorHighlighters();
            return;
        }
        for (var e : errors) {
            annotateErrorsInPreviewInputEditor(e);
            displayErrorInParseErrorConsole(e);
        }
    }
    
    
    /**
     * Display error messages to the console.
     *
     * @param error Error text message.
     */
    public void logErrorToConsole(String error) {
        errorConsolePanel.add("ERROR: " + error + '\n');
    }
    
    
    /**
     * Show token information if the ctrl-key is down and mouse movement occurs
     */
    public void showTokenInfoUponCtrlKey(Editor editor, PreviewState previewState, int offset) {
        var tokenUnderCursor = ParsingUtils.getTokenUnderCursor(previewState, offset);
        
        if (tokenUnderCursor == null) {
            var parser = (PreviewParser) previewState.getParsingResult().parser;
            var tokenStream = (CommonTokenStream) parser.getInputStream();
            tokenUnderCursor = ParsingUtils.getSkippedTokenUnderCursor(tokenStream, offset);
        }
        
        if (tokenUnderCursor == null) {
            return;
        }
        
        var channelInfo = "";
        var channel = tokenUnderCursor.getChannel();
        
        if (channel != Token.DEFAULT_CHANNEL) {
            var chNum = channel == Token.HIDDEN_CHANNEL ? "hidden" : String.valueOf(channel);
            channelInfo = ", Channel " + chNum;
        }
        
        var color = JBColor.PINK;
        var tokenInfo =
            String.format(
                "#%d Type %s, Line %d:%d%s",
                tokenUnderCursor.getTokenIndex(),
                previewState.getGrammar().getTokenDisplayName(tokenUnderCursor.getType()),
                tokenUnderCursor.getLine(),
                tokenUnderCursor.getCharPositionInLine(),
                channelInfo
            );
        
        if (channel == -1) {
            tokenInfo = "Skipped";
            color = JBColor.gray;
        }
        
        var sourceInterval = Interval.of(
            tokenUnderCursor.getStartIndex(),
            tokenUnderCursor.getStopIndex() + 1
        );
        
        highlightAndOfferHint(editor, offset, sourceInterval, color, EffectType.ROUNDED_BOX, tokenInfo);
    }
    
    
    /**
     * Show tokens/region associated with parse tree parent of this token
     * if the alt-key is down and mouse movement occurs.
     */
    public void showParseRegion(Editor editor, PreviewState previewState, int offset) {
        var tokenUnderCursor = ParsingUtils.getTokenUnderCursor(previewState, offset);
        if (tokenUnderCursor == null) {
            return;
        }
        
        var tree = previewState.getParsingResult().tree;
        var nodeWithToken =
            (TerminalNode) ParsingUtils.getParseTreeNodeWithToken(tree, tokenUnderCursor);
        if (nodeWithToken == null) {
            // hidden token
            return;
        }
        
        var parser = (PreviewParser) previewState.getParsingResult().parser;
        var tokenStream = (CommonTokenStream) parser.getInputStream();
        var parent = (ParserRuleContext) nodeWithToken.getParent();
        var tokenInterval = parent.getSourceInterval();
        var startToken = tokenStream.get(tokenInterval.a);
        var stopToken = tokenStream.get(tokenInterval.b);
        var sourceInterval =
            Interval.of(startToken.getStartIndex(), stopToken.getStopIndex() + 1);
        
        var stack = parser.getRuleInvocationStack(parent);
        Collections.reverse(stack);
        
        if (stack.size() > MAX_STACK_DISPLAY) {
            // collapse contiguous dups to handle left-recursive stacks
            List<Pair<String, Integer>> smaller = new ArrayList<>();
            var last = 0;
            smaller.add(new Pair<>(stack.get(0), 1)); // init to having first element, count of 1
            for (var i = 1; i < stack.size(); i++) {
                var s = stack.get(i);
                if (smaller.get(last).a.equals(s)) {
                    smaller.set(last, new Pair<>(s, smaller.get(last).b + 1));
                } else {
                    smaller.add(new Pair<>(s, 1));
                    last++;
                }
            }
            stack = new ArrayList<>();
            for (var pair : smaller) {
                if (pair.b > 1) {
                    stack.add(pair.a + '^' + pair.b);
                } else {
                    stack.add(pair.a);
                }
            }
        }
        
        var stackS = Utils.join(stack.toArray(), " -> ");
        highlightAndOfferHint(editor, offset, sourceInterval, JBColor.YELLOW, EffectType.ROUNDED_BOX, stackS);
    }
    
    
    /**
     * Highlight a part of the text in the input panel editor and show a hint.
     *
     * @param editor         The target editor instance.
     * @param offset         The offset-interval.
     * @param sourceInterval Interval of text.
     * @param color          Color to highlight with.
     * @param effectType     Effect-type to highlight with.
     * @param hintText       The text displayed as hint.
     */
    public void highlightAndOfferHint(Editor editor, int offset, Interval sourceInterval, JBColor color, EffectType effectType, String hintText) {
        var caretModel = editor.getCaretModel();
        editor.getMarkupModel().removeAllHighlighters();
        
        final var textAttributes = new TextAttributes();
        textAttributes.setForegroundColor(color);
        textAttributes.setEffectColor(color);
        textAttributes.setEffectType(effectType);
        
        var markupModel = editor.getMarkupModel();
        markupModel.addRangeHighlighter(
            sourceInterval.a,
            sourceInterval.b,
            InputPanel.TOKEN_INFO_LAYER, // layer
            textAttributes,
            HighlighterTargetArea.EXACT_RANGE
        );
        
        if (hintText.contains("<")) {
            hintText = hintText.replaceAll("<", "&lt;");
        }
        
        // HINT
        caretModel.moveToOffset(offset); // info tooltip only shows at cursor :(
        HintManager.getInstance().showInformationHint(editor, hintText);
    }
    
    
    /**
     * Highlight a specific range of text in the current input-editor.
     *
     * @param textAttributes The text attributes to set for the highlighting.
     * @param startOffset    The start-index.
     * @param endOffset      The stop-index.
     * @param layer          The layer to be used.
     */
    public void highlightRange(TextAttributes textAttributes, int startOffset, int endOffset, int layer) {
        var editor = getInputEditor();
        editor.getMarkupModel().removeAllHighlighters();
        
        // invalid parameters
        if (textAttributes == null || startOffset < 0 || endOffset < 0)
            return;
        
        if (endOffset < startOffset) {
            var n = endOffset;
            endOffset = startOffset;
            startOffset = n;
        }
        
        editor.getMarkupModel().addRangeHighlighter(
            startOffset,
            endOffset,
            layer,
            textAttributes,
            HighlighterTargetArea.EXACT_RANGE
        );
    }
    
    
    public void setCursorToGrammarElement(Project project, PreviewState previewState, int offset) {
        var tokenUnderCursor = ParsingUtils.getTokenUnderCursor(previewState, offset);
        if (tokenUnderCursor == null) {
            return;
        }
        
        var parser = (PreviewParser) previewState.getParsingResult().parser;
        var atnState = parser.inputTokenToStateMap.get(tokenUnderCursor);
        if (atnState == null) { // likely an error token
            //LOG.error("no ATN state for input token " + tokenUnderCursor);
            return;
        }
        
        var region = previewState.getGrammar().getStateToGrammarRegion(atnState);
        var token = (CommonToken) previewState.getGrammar().tokenStream.get(region.a);
        
        jumpToGrammarPosition(project, token.getStartIndex());
    }
    
    
    public void setCursorToGrammarRule(Project project, PreviewState previewState, int offset) {
        var tokenUnderCursor = ParsingUtils.getTokenUnderCursor(previewState, offset);
        if (tokenUnderCursor == null) {
            return;
        }
        
        var tree = previewState.getParsingResult().tree;
        var nodeWithToken =
            (TerminalNode) ParsingUtils.getParseTreeNodeWithToken(tree, tokenUnderCursor);
        if (nodeWithToken == null) {
            // hidden token
            return;
        }
        
        var parent = (ParserRuleContext) nodeWithToken.getParent();
        var ruleIndex = parent.getRuleIndex();
        var rule = previewState.getGrammar().getRule(ruleIndex);
        var ruleNameNode = (GrammarAST) rule.ast.getChild(0);
        var start = ((CommonToken) ruleNameNode.getToken()).getStartIndex();
        
        jumpToGrammarPosition(project, start);
    }
    
    
    public void jumpToGrammarPosition(Project project, int start) {
        final var controller = ANTLRv4PluginController.getInstance(project);
        
        if (controller == null) {
            return;
        }
        
        final var grammarEditor = controller.getEditor(previewState.getGrammarFile());
        
        if (grammarEditor == null) {
            return;
        }
        
        var caretModel = grammarEditor.getCaretModel();
        caretModel.moveToOffset(start);
        var scrollingModel = grammarEditor.getScrollingModel();
        scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE);
        grammarEditor.getContentComponent().requestFocus();
    }
    
    
    public void setCursorToHierarchyViewElement(int offset) {
        previewPanel.hierarchyViewer.selectNodeAtOffset(offset);
    }
    
    
    public void annotateErrorsInPreviewInputEditor(SyntaxError e) {
        var editor = getInputEditor();
        if (editor == null) {
            return;
        }
        
        var markupModel = editor.getMarkupModel();
        
        int a, b; // Start and stop index
        var cause = e.getException();
        if (cause instanceof LexerNoViableAltException) {
            a = ((LexerNoViableAltException) cause).getStartIndex();
            b = ((LexerNoViableAltException) cause).getStartIndex() + 1;
        } else {
            var offendingToken = e.getOffendingSymbol();
            a = offendingToken.getStartIndex();
            b = offendingToken.getStopIndex() + 1;
        }
        
        final var attr = new TextAttributes();
        
        attr.setForegroundColor(JBColor.RED);
        attr.setEffectColor(JBColor.RED);
        attr.setEffectType(EffectType.WAVE_UNDERSCORE);
        
        var highlighter =
            markupModel.addRangeHighlighter(
                a,
                b,
                ERROR_LAYER, // layer
                attr,
                HighlighterTargetArea.EXACT_RANGE
            );
        
        highlighter.putUserData(SYNTAX_ERROR, e);
    }
    
    
    public void addCaretListener(CaretListener caretListener) {
        this.caretListeners.add(caretListener);
    }
    
    
    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        outerMostPanel.setLayout(new BorderLayout(10, 10));
        startRuleAndInputPanel = new JPanel();
        startRuleAndInputPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 9));
        startRuleAndInputPanel.setMinimumSize(new Dimension(233, 40));
        startRuleAndInputPanel.setPreferredSize(new Dimension(295, 40));
        startRuleAndInputPanel.setToolTipText("srthuertzu");
        outerMostPanel.add(startRuleAndInputPanel, BorderLayout.NORTH);
        radioButtonPanel = new JPanel();
        radioButtonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        radioButtonPanel.setAlignmentX(0.0f);
        radioButtonPanel.setAlignmentY(0.0f);
        startRuleAndInputPanel.add(radioButtonPanel);
        inputRadioButton = new JRadioButton();
        inputRadioButton.setSelected(true);
        inputRadioButton.setText("Input");
        radioButtonPanel.add(inputRadioButton);
        fileRadioButton = new JRadioButton();
        fileRadioButton.setText("File");
        radioButtonPanel.add(fileRadioButton);
        fileChooser = new TextFieldWithBrowseButton();
        fileChooser.setEditable(false);
        radioButtonPanel.add(fileChooser);
        startRuleLabel = new JLabel();
        startRuleLabel.setText("Label");
        startRuleAndInputPanel.add(startRuleLabel);
        startRuleLabel2 = new JLabel();
        startRuleLabel2.setText("Label");
        startRuleAndInputPanel.add(startRuleLabel2);
        startRuleAndInputPanel.add(comboBox);
        placeHolder = new JTextArea();
        placeHolder.setBackground(Color.lightGray);
        placeHolder.setEditable(false);
        placeHolder.setEnabled(true);
        placeHolder.setMargin(new Insets(0, 0, 0, 0));
        placeHolder.setText("");
        outerMostPanel.add(placeHolder, BorderLayout.EAST);
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(fileRadioButton);
        buttonGroup.add(inputRadioButton);
    }
    
    
    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {return outerMostPanel;}
    
    /* ------------------------------------------------------------------------------------------------------------------ */
    
    
    @Override public void dispose() {
        LOG.debug("Dispose called: " + this.getClass().getName());
    }
}
