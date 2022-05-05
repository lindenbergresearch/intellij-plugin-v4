package org.antlr.intellij.plugin.preview;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.parsing.ParsingResult;
import org.antlr.intellij.plugin.parsing.ParsingUtils;
import org.antlr.intellij.plugin.parsing.PreviewParser;
import org.antlr.intellij.plugin.profiler.ProfilerPanel;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.antlr.v4.runtime.tree.Tree;
import org.antlr.v4.tool.Rule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collections;

import static com.intellij.icons.AllIcons.Actions.*;
import static com.intellij.icons.AllIcons.General.Error;
import static com.intellij.icons.AllIcons.General.*;
import static org.antlr.intellij.plugin.ANTLRv4PluginController.PREVIEW_WINDOW_ID;

/**
 * The top level contents of the preview tool window created by
 * intellij automatically. Since we need grammars to interpret,
 * this object creates and caches lexer/parser grammars for
 * each grammar file it gets notified about.
 */
public class PreviewPanel extends JPanel implements ParsingResultSelectionListener {
    //com.apple.eawt stuff stopped working correctly in java 7 and was only recently fixed in java 9;
    //perhaps in a few more years they will get around to backporting whatever it was they fixed.
    // until then,  the zoomable tree viewer will only be installed if the user is running java 1.6
    private static final boolean isTrackpadZoomSupported =
        SystemInfo.isMac &&
        (
            SystemInfo.JAVA_VERSION.startsWith("1.6") ||
            SystemInfo.JAVA_VERSION.startsWith("1.9")
        );
    
    public static final Logger LOG = Logger.getInstance("ANTLR PreviewPanel");
    
    public Project project;
    public InputPanel inputPanel;
    private UberTreeViewer treeViewer;
    public HierarchyViewer hierarchyViewer;
    public ProfilerPanel profilerPanel;
    private TokenStreamViewer tokenStreamViewer;
    private JPanel leftPanel;
    
    /**
     * Indicates if the preview should be automatically refreshed after grammar changes.
     */
    private boolean autoRefresh = true;
    
    private boolean scrollFromSource = false;
    private boolean highlightSource = false;
    
    private ActionToolbar buttonBar;
    private ActionToolbar buttonBarGraph;
    private final CancelParserAction cancelParserAction = new CancelParserAction();
    
    
    public PreviewPanel(Project project) {
        this.project = project;
        createGUI();
    }
    
    
    /**
     *
     */
    private void createGUI() {
        this.setLayout(new BorderLayout());
        
        // Had to set min size / preferred size in InputPanel.form to get slider to allow left shift of divider
        Splitter splitPane = new Splitter();
        splitPane.setShowDividerIcon(true);
        splitPane.setDividerWidth(2);
        
        Splitter splitPaneLeft = new Splitter();
        splitPaneLeft.setShowDividerIcon(true);
        splitPaneLeft.setDividerWidth(2);
        splitPaneLeft.setProportion(0.8f);
        splitPaneLeft.setOrientation(true);
        
        leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout(15, 15));
        leftPanel.setBorder(
            BorderFactory.createEtchedBorder(1)
        );
        
        
        inputPanel = getEditorPanel();
        inputPanel.getComponent().setBorder(BorderFactory.createEmptyBorder(18, 1, 1, 1));
        inputPanel.addCaretListener(new CaretListener() {
            @Override
            public void caretPositionChanged(@NotNull CaretEvent event) {
                Caret caret = event.getCaret();
                
                if (scrollFromSource && caret != null) {
                    tokenStreamViewer.onInputTextSelected(caret.getOffset());
                    hierarchyViewer.selectNodeAtOffset(caret.getOffset());
                }
            }
        });
        
        
        JTabbedPane tabbedEditorPanel = new JBTabbedPane(JTabbedPane.TOP);
        tabbedEditorPanel.setBorder(BorderFactory.createEmptyBorder(1, 7, 4, 8));
        
        inputPanel.removeErrorConsole();
        tabbedEditorPanel.addTab("Console", Error, inputPanel.getErrorPane());
        inputPanel.getComponent().add(tabbedEditorPanel);
        
        splitPaneLeft.setFirstComponent(inputPanel.getComponent());
        splitPaneLeft.setSecondComponent(tabbedEditorPanel);
        // splitPaneLeft.getDivider().setBorder(BorderFactory.createLineBorder(JBColor.background().darker(), 1));
        leftPanel.add(splitPaneLeft);
        
        JTabbedPane tabbedPanel = createParseTreeAndProfileTabbedPanel();
        tabbedPanel.setBorder(BorderFactory.createEtchedBorder(1));
        splitPane.setFirstComponent(leftPanel);
        splitPane.setSecondComponent(tabbedPanel);
        splitPane.setProportion(0.4f);
        //  splitPane.getDivider().setBorder(BorderFactory.createLineBorder(JBColor.background().darker(), 2));
        
        this.buttonBar = createButtonBar();
        this.add(buttonBar.getComponent(), BorderLayout.WEST);
        this.add(splitPane, BorderLayout.CENTER);
    }
    
    
    /**
     * @return
     */
    private ActionToolbar createButtonBarGraph() {
        ToggleAction autoScaleDiagram =
            new ToggleAction(
                "Auto-Scale",
                "Set proper zoom-level upon live-testing grammars.",
                Replace
            ) {
                
                @Override
                public boolean isSelected(@NotNull AnActionEvent e) {
                    return treeViewer.autoscaling;
                }
                
                
                @Override
                public void setSelected(@NotNull AnActionEvent e, boolean state) {
                    treeViewer.autoscaling = state;
                    treeViewer.repaint();
                }
            };
        
        AnAction zoomActualSize = new AnAction("Actual Size", "Set zoom-level to 1:1.", ActualZoom) {
            @Override
            public void update(@NotNull AnActionEvent e) {
                super.update(e);
                if (treeViewer.getScale() == 1) {
                    e.getPresentation().setEnabled(false);
                }
            }
            
            
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                treeViewer.setScaleLevel(1.0);
                treeViewer.updatePreferredSize();
            }
        };
        
        AnAction zoomOut = new AnAction("Zoom Out", null, ZoomOut) {
            @Override
            public void update(@NotNull AnActionEvent e) {
                super.update(e);
                if (treeViewer.getScale() - UberTreeViewer.SCALING_INCREMENT < UberTreeViewer.MIN_SCALE_FACTOR) {
                    e.getPresentation().setEnabled(false);
                }
            }
            
            
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                treeViewer.setRelativeScaling(-UberTreeViewer.SCALING_INCREMENT);
                treeViewer.updatePreferredSize();
            }
        };
        
        AnAction zoomIn = new AnAction("Zoom In", null, ZoomIn) {
            @Override
            public void update(@NotNull AnActionEvent e) {
                super.update(e);
                if (treeViewer.getScale() + UberTreeViewer.SCALING_INCREMENT > UberTreeViewer.MAX_SCALE_FACTOR) {
                    e.getPresentation().setEnabled(false);
                }
            }
            
            
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                treeViewer.setRelativeScaling(UberTreeViewer.SCALING_INCREMENT);
                treeViewer.updatePreferredSize();
            }
        };
        
        AnAction fitScreen = new AnAction("Fit Screen", "Fit content to screen.", FitContent) {
            @Override
            public void update(@NotNull AnActionEvent e) {
                super.update(e);
                e.getPresentation().setEnabled(!treeViewer.autoscaling);
            }
            
            
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                treeViewer.doAutoScale();
                treeViewer.updatePreferredSize();
            }
        };
        
        AnAction fitSelected = new AnAction("Fit Selected Node", "Zoom to selected tree node.", ShortcutFilter) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                treeViewer.focusSelectedNode();
                treeViewer.updatePreferredSize();
            }
        };
        
        /* --------------------------------------------------------------------- */
        
        ToggleAction useCompactLabels = new ToggleAction("Compact Labels", "Use compact labeling for tree-nodes.", AllIcons.Json.Array) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return treeViewer.isCompactLabels();
            }
            
            
            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                treeViewer.setCompactLabels(state);
            }
        };
        
        
        DefaultActionGroup actionGroup = new DefaultActionGroup(
            autoScaleDiagram,
            fitScreen,
            fitSelected
        );
        
        actionGroup.addSeparator();
        
        actionGroup.addAll(
            zoomActualSize,
            zoomIn,
            zoomOut
        );
        
        actionGroup.addSeparator();
        
        actionGroup.addAll(
            useCompactLabels
        );
        
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(PREVIEW_WINDOW_ID, actionGroup, true);
        toolbar.setTargetComponent(this);
        
        return toolbar;
    }
    
    
    private ActionToolbar createButtonBar() {
        final AnAction refreshAction = new ToggleAction("Refresh Preview Automatically",
                                                        "Refresh preview automatically upon grammar changes", AllIcons.Actions.Refresh
        ) {
            
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return autoRefresh;
            }
            
            
            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                autoRefresh = state;
            }
        };
        
        ToggleAction scrollFromSourceBtn = new ToggleAction("Scroll from Source", null, AutoscrollFromSource) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return scrollFromSource;
            }
            
            
            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                scrollFromSource = state;
            }
        };
        
        ToggleAction scrollToSourceBtn = new ToggleAction("Highlight Source", null, Find) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return highlightSource;
            }
            
            
            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                highlightSource = state;
            }
            
            
        };
        
        
        DefaultActionGroup actionGroup = new DefaultActionGroup(
            refreshAction,
            cancelParserAction,
            scrollFromSourceBtn,
            scrollToSourceBtn
        );
        
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(PREVIEW_WINDOW_ID, actionGroup, false);
        ;
        toolbar.setTargetComponent(this);
        
        return toolbar;
    }
    
    
    private InputPanel getEditorPanel() {
        LOG.info("createEditorPanel" + " " + project.getName());
        return new InputPanel(this);
    }
    
    
    public ProfilerPanel getProfilerPanel() {
        return profilerPanel;
    }
    
    
    private JTabbedPane createParseTreeAndProfileTabbedPanel() {
        JBTabbedPane tabbedPane = new JBTabbedPane(JBTabbedPane.TOP);
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        
        LOG.info("createParseTreePanel" + " " + project.getName());
        Pair<UberTreeViewer, JPanel> pair = createParseTreePanel();
        treeViewer = pair.a;
        setupContextMenu(treeViewer);
        tabbedPane.addTab("Parse tree", AllIcons.Hierarchy.Subtypes, pair.b);
        
        hierarchyViewer = new HierarchyViewer(null);
        hierarchyViewer.addParsingResultSelectionListener(this);
        tabbedPane.addTab("Hierarchy", ShowAsTree, hierarchyViewer);
        
        profilerPanel = new ProfilerPanel(project, this);
        tabbedPane.addTab("Profiler", AllIcons.Toolwindows.ToolWindowProfiler, profilerPanel.getComponent());
        
        tokenStreamViewer = new TokenStreamViewer();
        tokenStreamViewer.addParsingResultSelectionListener(this);
        tabbedPane.addTab("Tokens", ShowHiddens, tokenStreamViewer);
        
        return tabbedPane;
    }
    
    
    private static void setupContextMenu(final UberTreeViewer treeViewer) {
        treeViewer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    ParseTreeContextualMenu.showPopupMenu(treeViewer, e);
                }
            }
        });
    }
    
    
    private Pair<UberTreeViewer, JPanel> createParseTreePanel() {
        // wrap tree and slider in panel
        JPanel treePanel = new JPanel(new BorderLayout(10, 10));
        
        final UberTreeViewer viewer =
            isTrackpadZoomSupported ?
                new TrackpadZoomingTreeView(null, null) :
                new UberTreeViewer(null, null);
        
        
        viewer.setDoubleBuffered(true);
        viewer.addParsingResultSelectionListener(this);
        
        this.buttonBarGraph = createButtonBarGraph();
        
        
        // Wrap tree viewer component in scroll pane
        JScrollPane scrollPane = new JBScrollPane(
            viewer,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
        );
        
        scrollPane.setWheelScrollingEnabled(true);
        scrollPane.setBorder(BorderFactory.createEtchedBorder());
        
        treePanel.add(buttonBarGraph.getComponent(), BorderLayout.NORTH);
        treePanel.add(scrollPane, BorderLayout.CENTER);
        
        viewer.scrollPane = scrollPane;
        viewer.setBackground(JBColor.background().darker().darker());
        
        return new Pair<>(viewer, treePanel);
    }
    
    
    /**
     * Notify the preview tool window contents that the grammar file has changed
     */
    public void grammarFileSaved(VirtualFile grammarFile) {
        String grammarFileName = grammarFile.getPath();
        LOG.info("grammarFileSaved " + grammarFileName + " " + project.getName());
        ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
        PreviewState previewState = controller.getPreviewState(grammarFile);
        
        ensureStartRuleExists(grammarFile);
        inputPanel.grammarFileSaved();
        
        // if the saved grammar is not a pure lexer and there is a start rule, reparse
        // means that switching grammars must refresh preview
        if (previewState.g != null && previewState.startRuleName != null) {
            updateParseTreeFromDoc(previewState.grammarFile);
        } else {
            clearTabs(null); // blank tree
        }
        
        profilerPanel.grammarFileSaved(previewState, grammarFile);
    }
    
    
    private void ensureStartRuleExists(VirtualFile grammarFile) {
        PreviewState previewState = ANTLRv4PluginController.getInstance(project).getPreviewState(grammarFile);
        // if start rule no longer exists, reset display/state.
        if (previewState.g != null &&
            previewState.g != ParsingUtils.BAD_PARSER_GRAMMAR &&
            previewState.startRuleName != null) {
            Rule rule = previewState.g.getRule(previewState.startRuleName);
            if (rule == null) {
                previewState.startRuleName = null;
                inputPanel.resetStartRuleLabel();
            }
        }
    }
    
    
    /**
     * Notify the preview tool window contents that the grammar file has changed
     */
    public void grammarFileChanged(VirtualFile newFile) {
        switchToGrammar(newFile);
    }
    
    
    /**
     * Load grammars and set editor component.
     */
    private void switchToGrammar(VirtualFile grammarFile) {
        String grammarFileName = grammarFile.getPath();
        LOG.info("switchToGrammar " + grammarFileName + " " + project.getName());
        ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
        PreviewState previewState = controller.getPreviewState(grammarFile);
        
        // recover last start rule
        String startRule = PropertiesComponent.getInstance(project).getValue("org.antlr.intellij.plugin.preview.startRule");
        
        if (startRule != null) {
            previewState.startRuleName = startRule;
            LOG.info("recover start-rule from saved session: '" + startRule + "'");
        }
        
        inputPanel.switchToGrammar(previewState, grammarFile);
        profilerPanel.switchToGrammar(previewState, grammarFile);
        
        if (previewState.startRuleName != null) {
            updateParseTreeFromDoc(grammarFile); // regens tree and profile data
        } else {
            clearTabs(null); // blank tree
        }
        
        setEnabled(previewState.g != null || previewState.lg == null);
    }
    
    
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.setEnabledRecursive(this, enabled);
    }
    
    
    private void setEnabledRecursive(Component component, boolean enabled) {
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                child.setEnabled(enabled);
                setEnabledRecursive(child, enabled);
            }
        }
    }
    
    
    public void closeGrammar(VirtualFile grammarFile) {
        String grammarFileName = grammarFile.getPath();
        LOG.info("closeGrammar " + grammarFileName + " " + project.getName());
        
        inputPanel.resetStartRuleLabel();
        inputPanel.clearErrorConsole();
        clearParseTree(); // wipe tree
        
        ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
        PreviewState previewState = controller.getPreviewState(grammarFile);
        inputPanel.releaseEditor(previewState);
    }
    
    
    private void clearTabs(@Nullable ParseTree tree) {
        ApplicationManager.getApplication().invokeLater(() -> {
            treeViewer.setRuleNames(Collections.emptyList());
            treeViewer.setTree(tree);
            hierarchyViewer.setRuleNames(Collections.emptyList());
            hierarchyViewer.setTree(null);
            tokenStreamViewer.clear();
        });
    }
    
    
    private void updateTreeViewer(final PreviewState preview, final ParsingResult result) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (result.parser instanceof PreviewParser) {
                AltLabelTextProvider provider = new AltLabelTextProvider(result.parser, preview.g);
                treeViewer.setTreeTextProvider(provider);
                treeViewer.setTree(result.tree);
                hierarchyViewer.setTreeTextProvider(provider);
                hierarchyViewer.setTree(result.tree);
                tokenStreamViewer.setParsingResult(result.parser);
            } else {
                treeViewer.setRuleNames(Arrays.asList(preview.g.getRuleNames()));
                treeViewer.setTree(result.tree);
                hierarchyViewer.setRuleNames(Arrays.asList(preview.g.getRuleNames()));
                hierarchyViewer.setTree(result.tree);
            }
        });
    }
    
    
    void clearParseTree() {
        clearTabs(null);
    }
    
    
    private void indicateInvalidGrammarInParseTreePane() {
        showError("Issues with parser and/or lexer grammar(s) prevent preview; see ANTLR 'Tool Output' pane");
    }
    
    
    private void showError(String message) {
        
        clearTabs(new TerminalNodeImpl(new CommonToken(Token.INVALID_TYPE, message)));
    }
    
    
    private void indicateNoStartRuleInParseTreePane() {
        showError("No Start Rule!");
    }
    
    
    public void updateParseTreeFromDoc(VirtualFile grammarFile) {
        ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
        if (controller == null) return;
        
        PreviewState previewState = controller.getPreviewState(grammarFile);
        
        LOG.info("updateParseTreeFromDoc " + grammarFile + " rule " + previewState.startRuleName);
        
        if (previewState.g == null || previewState.lg == null) {
            // likely error in grammar prevents it from loading properly into previewState; bail
            indicateInvalidGrammarInParseTreePane();
            return;
        }
        
        Editor editor = inputPanel.getInputEditor();
        if (editor == null) return;
        final String inputText = editor.getDocument().getText();
        
        // The controller will call us back when it's done parsing
        controller.parseText(grammarFile, inputText);
    }
    
    
    public InputPanel getInputPanel() {
        return inputPanel;
    }
    
    
    public void autoRefreshPreview(VirtualFile virtualFile) {
        final ANTLRv4PluginController controller = ANTLRv4PluginController.getInstance(project);
        
        if (autoRefresh
            && controller != null
            && inputPanel.previewState != null
            && inputPanel.previewState.startRuleName != null) {
            ApplicationManager.getApplication().invokeLater(() -> controller.grammarFileSavedEvent(virtualFile));
        }
    }
    
    
    public void onParsingCompleted(PreviewState previewState, long duration) {
        cancelParserAction.setEnabled(false);
        buttonBar.updateActionsImmediately();
        buttonBarGraph.updateActionsImmediately();
        
        if (previewState.parsingResult != null) {
            updateTreeViewer(previewState, previewState.parsingResult);
            profilerPanel.setProfilerData(previewState, duration);
            inputPanel.showParseErrors(previewState.parsingResult.syntaxErrorListener.getSyntaxErrors());
        } else if (previewState.startRuleName == null) {
            indicateNoStartRuleInParseTreePane();
        } else {
            indicateInvalidGrammarInParseTreePane();
        }
    }
    
    
    public void notifySlowParsing() {
        cancelParserAction.setEnabled(true);
        buttonBar.updateActionsImmediately();
    }
    
    
    public void onParsingCancelled() {
        cancelParserAction.setEnabled(false);
        buttonBar.updateActionsImmediately();
        showError("Parsing was aborted");
    }
    
    
    /**
     * Fired when a token is selected in the {@link TokenStreamViewer} to let us know that we should highlight
     * the corresponding text in the editor.
     */
    @Override
    public void onLexerTokenSelected(Token token) {
        if (!highlightSource || scrollFromSource) {
            return;
        }
        
        inputPanel.clearInputEditorHighlighters();
        if (token == null) return;
        
        int startIndex = token.getStartIndex();
        int stopIndex = token.getStopIndex();
        
        Editor editor = inputPanel.getInputEditor();
        Interval sourceInterval = Interval.of(startIndex, stopIndex + 1);
        inputPanel.highlightAndOfferHint(editor, 0, sourceInterval, JBColor.GREEN, EffectType.ROUNDED_BOX, token.toString());
        // inputPanel.getInputEditor().getSelectionModel().setSelection(startIndex, stopIndex + 1);
    }
    
    
    @Override
    public void onParserRuleSelected(Tree tree) {
        if (!highlightSource || scrollFromSource) {
            return;
        }
        
        inputPanel.clearInputEditorHighlighters();
        if (tree == null) return;
        
        int startIndex;
        int stopIndex;
        String msg;
        
        if (tree instanceof ParserRuleContext) {
            startIndex = ((ParserRuleContext) tree).getStart().getStartIndex();
            stopIndex = ((ParserRuleContext) tree).getStop().getStopIndex();
            msg = "Rule: " + ((ParserRuleContext) tree).getText();
        } else if (tree instanceof TerminalNode) {
            startIndex = ((TerminalNode) tree).getSymbol().getStartIndex();
            stopIndex = ((TerminalNode) tree).getSymbol().getStopIndex();
            msg = "Terminal-Node: " + ((TerminalNode) tree).getSymbol().getText();
        } else {
            return;
        }
        
        Editor editor = inputPanel.getInputEditor();
        if (startIndex >= 0 && stopIndex + 1 <= editor.getDocument().getTextLength()) {
//            editor.getSelectionModel().removeSelection();
//            editor.getSelectionModel().setSelection(startIndex, stopIndex + 1);
            
            //   Editor editor = inputPanel.getInputEditor();
            Interval sourceInterval = Interval.of(startIndex, stopIndex + 1);
            inputPanel.highlightAndOfferHint(editor, startIndex, sourceInterval, (JBColor) JBColor.MAGENTA, EffectType.ROUNDED_BOX, msg);
        }
    }
    
}
