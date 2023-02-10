package org.antlr.intellij.plugin.preview;

import com.intellij.icons.AllIcons.*;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTabbedPane;
import org.abego.treelayout.Configuration.Location;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.parsing.ParsingResult;
import org.antlr.intellij.plugin.parsing.ParsingUtils;
import org.antlr.intellij.plugin.parsing.PreviewParser;
import org.antlr.intellij.plugin.profiler.ProfilerPanel;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Tree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;

import static com.intellij.icons.AllIcons.Actions.*;
import static com.intellij.icons.AllIcons.General.*;
import static org.antlr.intellij.plugin.ANTLRv4PluginController.PREVIEW_WINDOW_ID;

/**
 * The top level contents of the preview tool window created by
 * intellij automatically. Since we need grammars to interpret,
 * this object creates and caches lexer/parser grammars for
 * each grammar file it gets notified about.
 */
public class PreviewPanel extends JPanel implements ParsingResultSelectionListener {
    /**
     * Readable form of current selected tab
     */
    enum SelectedTab {
        TREEVIEWER,
        HIRACHIE,
        PROFILER,
        TOKENLIST
    }
    
    
    public static final Logger LOG =
        Logger.getInstance("ANTLR PreviewPanel");
    
    // com.apple.eawt stuff stopped working correctly in java 7 and was only recently fixed in java 9;
    // perhaps in a few more years they will get around to backport whatever it was they fixed.
    // until then,  the zoomable tree viewer will only be installed if the user is running java 1.6
    private static final boolean isTrackpadZoomSupported =
        SystemInfo.isMac &&
            (
                SystemInfo.JAVA_VERSION.startsWith("1.6") ||
                    SystemInfo.JAVA_VERSION.startsWith("1.9")
            );
    
    public Project project;
    public InputPanel inputPanel;
    private UberTreeViewer treeViewer;
    public HierarchyViewer hierarchyViewer;
    public ProfilerPanel profilerPanel;
    private PropertiesPanel propertiesPanel;
    private TokenStreamViewer tokenStreamViewer;
    private JPanel leftPanel;
    private ErrorConsolePanel errorConsolePanel;
    private JTabbedPane tabbedPanel;
    /**
     * Indicates if the preview should be automatically refreshed after grammar changes.
     */
    private boolean autoRefresh = true;
    private boolean scrollFromSource = false;
    private boolean highlightSource = false;
    
    private ActionToolbar buttonBar;
    private ActionToolbar buttonBarGraph;
    private final CancelParserAction cancelParserAction = new CancelParserAction();
    
    private String currentEditorText = "";
    
    
    public PreviewPanel(Project project) {
        this.project = project;
        createGUI();
    }
    
    
    protected boolean isTabSelected(SelectedTab tab) {
        return tabbedPanel.getSelectedIndex() == tab.ordinal();
    }
    
    
    private void createGUI() {
        this.setLayout(new BorderLayout());
        
        // Had to set min size / preferred size in InputPanel.form to get slider to allow left shift of divider
        var splitPane = new JBSplitter();
        splitPane.setShowDividerIcon(true);
        splitPane.setDividerWidth(2);
        splitPane.setProportion(0.4f);
        splitPane.setAndLoadSplitterProportionKey("PreviewPanel.splitPane");
        
        var splitPaneLeft = new JBSplitter();
        splitPaneLeft.setShowDividerIcon(true);
        splitPaneLeft.setDividerWidth(2);
        splitPaneLeft.setProportion(0.8f);
        splitPaneLeft.setOrientation(true);
        splitPaneLeft.setAndLoadSplitterProportionKey("PreviewPanel.splitPaneLeft");
        
        leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout(0, 0));
        leftPanel.setBorder(
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        );
        
        errorConsolePanel = new ErrorConsolePanel(
            new BorderLayout(0, 0),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        );
        
        
        inputPanel = getEditorPanel();
        inputPanel.getComponent().setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        inputPanel.addCaretListener(new CaretListener() {
            @Override
            public void caretPositionChanged(@NotNull CaretEvent event) {
                var caret = event.getCaret();
                
                if (scrollFromSource && caret != null) {
                    if (isTabSelected(SelectedTab.TOKENLIST))
                        tokenStreamViewer.onInputTextSelected(caret.getOffset());
                    
                    if (isTabSelected(SelectedTab.HIRACHIE))
                        hierarchyViewer.selectNodeAtOffset(caret.getOffset());
                }
            }
        });
        
        inputPanel.removeErrorConsole();
        
        splitPaneLeft.setFirstComponent(inputPanel.getComponent());
        splitPaneLeft.setSecondComponent(errorConsolePanel);
        // splitPaneLeft.getDivider().setBorder(BorderFactory.createLineBorder(JBColor.background().darker(), 1));
        leftPanel.add(splitPaneLeft);
        
        tabbedPanel = createParseTreeAndProfileTabbedPanel();
        tabbedPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        splitPane.setFirstComponent(leftPanel);
        splitPane.setSecondComponent(tabbedPanel);
        
        
        // keep track of panel size changes
        //  splitPane.addPropertyChangeListener(propertyChangeEvent -> treeViewer.setTreeUpdated(true));
        
        this.buttonBar = createButtonBar();
        this.add(buttonBar.getComponent(), BorderLayout.WEST);
        this.add(splitPane, BorderLayout.CENTER);
    }
    
    
    private ActionToolbar createButtonBarGraph() {
        ToggleAction toggleAutoscaling = new ToggleAction(
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
                treeViewer.setTreeInvalidated(true);
                
            }
        };
        
        AnAction zoomActualSize = new AnAction(
            "Actual Size", "Set zoom-level to 1:1.",
            ActualZoom
        ) {
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
                treeViewer.setTreeInvalidated(true);
            }
        };
        
        /* --------------------------------------------------------------------- */
        
        AnAction zoomOut = new AnAction(
            "Zoom Out",
            null,
            ZoomOut
        ) {
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
                treeViewer.setTreeInvalidated(true);
            }
        };
        
        AnAction zoomIn = new AnAction(
            "Zoom In",
            null,
            ZoomIn
        ) {
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
                treeViewer.setTreeInvalidated(true);
                
            }
        };
        
        AnAction fitScreen = new AnAction(
            "Fit Screen",
            "Fit content to screen.",
            FitContent
        ) {
            @Override
            public void update(@NotNull AnActionEvent e) {
                super.update(e);
                e.getPresentation().setEnabled(!treeViewer.autoscaling);
            }
            
            
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                treeViewer.doAutoScale();
                treeViewer.setTreeInvalidated(true);
            }
        };
        
        AnAction fitSelected = new AnAction(
            "Fit Selected Node",
            "Zoom to selected tree node.",
            ShortcutFilter
        ) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                treeViewer.focusSelectedNode();
                treeViewer.setTreeInvalidated(true);
            }
        };
        
        /* --------------------------------------------------------------------- */
        
        ToggleAction toggleCompactLabels = new ToggleAction(
            "Compact Labels",
            "Use compact labeling for tree-nodes.",
            Json.Array
        ) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return treeViewer.isCompactLabels();
            }
            
            
            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                treeViewer.setCompactLabels(state);
            }
        };
        
        ToggleAction toggleObjectExplorer = new ToggleAction(
            "Object Explorer",
            "Show Object Explorer to show additional properties for tree-nodes.",
            GroupByPrefix
        ) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return propertiesPanel.isVisible();
            }
            
            
            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                propertiesPanel.setVisible(state);
                treeViewer.setTreeInvalidated(true);
            }
        };
        
        
        ToggleAction toggleParseInfo = new ToggleAction(
            "Parse Info",
            "Show common parsing information in tree-viewer.",
            ShowImportStatements
        ) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return treeViewer.isShowParsingInfo();
            }
            
            
            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                treeViewer.setShowParsingInfo(state);
            }
        };
        
        /* --------------------------------------------------------------------- */
        
        ToggleAction toggleTopLayout = new ToggleAction(
            "Top-Down Layout",
            "Layout orientation from top to bottom.",
            Chooser.Bottom
        ) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return treeViewer.hasLayoutOrientation(Location.Top);
            }
            
            
            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                treeViewer.setLayoutOrientation(Location.Top);
                treeViewer.setTreeInvalidated(true);
            }
        };
        
        ToggleAction toggleBottomLayout = new ToggleAction(
            "Bottom-Up Layout",
            "Layout orientation from bottom to top.",
            Chooser.Top
        ) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return treeViewer.hasLayoutOrientation(Location.Bottom);
            }
            
            
            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                treeViewer.setLayoutOrientation(Location.Bottom);
                treeViewer.setTreeInvalidated(true);
            }
        };
        
        
        ToggleAction toggleLeftLayout = new ToggleAction(
            "Left-Right Layout",
            "Layout orientation from left to right.",
            Chooser.Right
        ) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return treeViewer.hasLayoutOrientation(Location.Left);
            }
            
            
            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                treeViewer.setLayoutOrientation(Location.Left);
                treeViewer.setTreeInvalidated(true);
            }
        };
        
        ToggleAction toggleRightLayout = new ToggleAction(
            "Right-Left Layout",
            "Layout orientation from right to left.",
            Chooser.Left
        ) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return treeViewer.hasLayoutOrientation(Location.Right);
            }
            
            
            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                treeViewer.setLayoutOrientation(Location.Right);
                treeViewer.setTreeInvalidated(true);
            }
        };
        /*|--------------------------------------------------------------------------|*/
        
        final AnAction decNodesGap = new AnAction(
            "Decrease Gap Between Nodes",
            null,
            Collapseall
        ) {
            @Override
            public void update(@NotNull AnActionEvent e) {
                super.update(e);
                if (treeViewer.exceedsGapBounds(treeViewer.getGapBetweenNodes(), -UberTreeViewer.NODE_GAP_INCREMENT)) {
                    e.getPresentation().setEnabled(false);
                }
            }
            
            
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                treeViewer.setRelativeNodesGap(-UberTreeViewer.NODE_GAP_INCREMENT);
                treeViewer.setTreeInvalidated(true);
            }
        };
        
        final AnAction incNodesGap = new AnAction(
            "Increase Gap Between Nodes",
            null,
            Expandall
        ) {
            @Override
            public void update(@NotNull AnActionEvent e) {
                super.update(e);
                if (treeViewer.exceedsGapBounds(treeViewer.getGapBetweenNodes(), UberTreeViewer.NODE_GAP_INCREMENT)) {
                    e.getPresentation().setEnabled(false);
                }
            }
            
            
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                treeViewer.setRelativeNodesGap(UberTreeViewer.NODE_GAP_INCREMENT);
                treeViewer.setTreeInvalidated(true);
            }
        };
        
        final AnAction resetNodesGap = new AnAction(
            "Reset to Default Gap Between Nodes",
            null,
            Reset
        ) {
            @Override
            public void update(@NotNull AnActionEvent e) {
                super.update(e);
                if (treeViewer.getGapBetweenNodes() == UberTreeViewer.DEFAULT_GAP_BETWEEN_NODES) {
                    e.getPresentation().setEnabled(false);
                }
            }
            
            
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                treeViewer.setRelativeNodesGap(UberTreeViewer.NODE_GAP_INCREMENT);
                treeViewer.setTreeInvalidated(true);
            }
        };
        
        
        /* --------------------------------------------------------------------- */
        
        var actionGroup = new DefaultActionGroup(
            toggleAutoscaling,
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
            toggleParseInfo,
            toggleCompactLabels,
            toggleObjectExplorer
        );
        
        actionGroup.addSeparator();
        
        actionGroup.addAll(
            toggleTopLayout,
            toggleBottomLayout,
            toggleLeftLayout,
            toggleRightLayout
        );
        
        actionGroup.addSeparator();
        
        actionGroup.addAll(
            decNodesGap,
            incNodesGap,
            resetNodesGap
        );
        
        var toolbar =
            ActionManager.getInstance().createActionToolbar(
                PREVIEW_WINDOW_ID,
                actionGroup,
                true
            );
        
        toolbar.setTargetComponent(this);
        
        return toolbar;
    }
    
    
    private ActionToolbar createButtonBar() {
        final AnAction refreshAction = new ToggleAction(
            "Refresh Preview Automatically",
            "Refresh preview automatically upon grammar changes",
            Actions.Refresh
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
        
        ToggleAction scrollFromSourceBtn = new ToggleAction(
            "Scroll from Source",
            "",
            AutoscrollFromSource
        ) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return scrollFromSource;
            }
            
            
            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                scrollFromSource = state;
            }
        };
        
        ToggleAction scrollToSourceBtn = new ToggleAction(
            "Highlight Source",
            "",
            Find
        ) {
            @Override
            public boolean isSelected(@NotNull AnActionEvent e) {
                return highlightSource;
            }
            
            
            @Override
            public void setSelected(@NotNull AnActionEvent e, boolean state) {
                highlightSource = state;
            }
            
            
        };
        
        /* --------------------------------------------------------------------- */
        
        DefaultActionGroup actionGroup = new DefaultActionGroup(
            refreshAction,
            cancelParserAction,
            scrollFromSourceBtn,
            scrollToSourceBtn
        );
        
        ActionToolbar toolbar =
            ActionManager.getInstance().createActionToolbar(
                PREVIEW_WINDOW_ID,
                actionGroup,
                false
            );
        
        toolbar.setTargetComponent(this);
        
        return toolbar;
    }
    
    
    private InputPanel getEditorPanel() {
        return new InputPanel(this);
    }
    
    
    public ErrorConsolePanel getErrorConsolePanel() {
        return errorConsolePanel;
    }
    
    
    public ProfilerPanel getProfilerPanel() {
        return profilerPanel;
    }
    
    
    private JTabbedPane createParseTreeAndProfileTabbedPanel() {
        var splitter = new JBSplitter();
        splitter.setShowDividerIcon(true);
        splitter.setDividerWidth(2);
        splitter.setProportion(0.8f);
        splitter.setOrientation(false);
        splitter.setAndLoadSplitterProportionKey("PreviewPanel.parseTreeSplitter");
        
        propertiesPanel =
            new PropertiesPanel(
                new BorderLayout(0, 0),
                BorderFactory.createEmptyBorder(2, 0, 0, 2)
                //  BorderFactory.createEtchedBorder(1)
            );
        
        
        var tabbedPane = new JBTabbedPane(JBTabbedPane.TOP);
        
        var pair = createParseTreePanel();
        treeViewer = pair.a;
        setupContextMenu(treeViewer);
        
        pair.b.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        splitter.setFirstComponent(pair.b);
        splitter.setSecondComponent(propertiesPanel);
        
        // keep track of panel size changes
        //   splitter.addPropertyChangeListener(propertyChangeEvent -> treeViewer.setTreeUpdated(true));
        
        tabbedPane.addTab("Parse tree", Hierarchy.Subtypes, splitter);
        pair.a.previewPanel = this;
        
        hierarchyViewer = new HierarchyViewer(null);
        hierarchyViewer.addParsingResultSelectionListener(this);
        tabbedPane.addTab("Hierarchy", ShowAsTree, hierarchyViewer);
        
        profilerPanel = new ProfilerPanel(project, this);
        tabbedPane.addTab("Profiler", Toolwindows.ToolWindowProfiler, profilerPanel.getComponent());
        
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
        var treePanel = new JPanel(new BorderLayout(2, 4));
        
        final var uberTreeViewer =
            isTrackpadZoomSupported ?
                new TrackpadZoomingTreeView(this) :
                new UberTreeViewer(this);
        
        
        uberTreeViewer.setDoubleBuffered(true);
        uberTreeViewer.addParsingResultSelectionListener(this);
        
        this.buttonBarGraph = createButtonBarGraph();
        buttonBarGraph.getComponent().setBorder(
            BorderFactory.createEmptyBorder(5, 0, 0, 0)
        );
        
        
        // Wrap tree uberTreeViewer component in scroll pane
        JScrollPane scrollPane = new JBScrollPane(
            uberTreeViewer,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS
        );
        
        scrollPane.setDoubleBuffered(true);
        scrollPane.setWheelScrollingEnabled(true);
        scrollPane.setBorder(BorderFactory.createEtchedBorder());
        scrollPane.add(uberTreeViewer.getInfoLabel());
        
        treePanel.add(buttonBarGraph.getComponent(), BorderLayout.NORTH);
        treePanel.add(scrollPane, BorderLayout.CENTER);
        
        uberTreeViewer.scrollPane = scrollPane;
        
        return new Pair<>(uberTreeViewer, treePanel);
    }
    
    
    /**
     * Notify the preview tool window contents that the grammar file has changed
     */
    public void grammarFileSaved(VirtualFile grammarFile) {
        //switchToGrammar(grammarFile);
        
        
        var controller = ANTLRv4PluginController.getInstance(project);
        var previewState = controller.getPreviewState(grammarFile);
        
        ensureStartRuleExists(grammarFile);
        
        //DEBUG LOG.info("PreviewPanel: grammarFileSaved: startrule: " + previewState.startRuleName);
        
        // if the saved grammar is not a pure lexer and there is a start rule, reparse
        // means that switching grammars must refresh preview
        if (previewState.grammar != null && previewState.startRuleName != null) {
            updateParseTreeFromDoc(previewState.grammarFile, true);
        } else {
            clearTabs(null); // blank tree
        }
        
        if (previewState.hasValidGrammar()) {
            //DEBUG  LOG.info("grammarFileSaved: valid grammar.");
        } else {
            //DEBUG  LOG.info("grammarFileSaved: INVALID grammar!");
            setEnabled(false);
            return;
        }
        
        
        inputPanel.switchToGrammar(previewState, grammarFile);
        inputPanel.setStartRuleName(grammarFile, previewState.startRuleName);
        profilerPanel.grammarFileSaved(previewState, grammarFile);
        
        setEnabled(true);
        updateTreeViewer(previewState, previewState.parsingResult);
    }
    
    
    private void ensureStartRuleExists(VirtualFile grammarFile) {
        var previewState = ANTLRv4PluginController.getInstance(project).getPreviewState(grammarFile);
        
        // if start rule no longer exists, reset display/state.
        if (previewState.grammar != null &&
            previewState.grammar != ParsingUtils.BAD_PARSER_GRAMMAR &&
            previewState.startRuleName != null) {
            var rule = previewState.grammar.getRule(previewState.startRuleName);
            
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
        //DEBUG LOG.info("grammarFileChanged(" + newFile.getName() + ")");
        switchToGrammar(newFile);
    }
    
    
    /**
     * Load grammars and set editor component.
     */
    private void switchToGrammar(VirtualFile grammarFile) {
        var grammarFileName = grammarFile.getPath();
        var controller = ANTLRv4PluginController.getInstance(project);
        
        //DEBUG LOG.info("switchToGrammar: " + grammarFileName);
        
        // should not happen
        if (controller == null)
            return;
        
        var previewState = controller.getPreviewState(grammarFile);
        
        // update references in tree-viewer
        treeViewer.grammarFile = grammarFile;
        treeViewer.previewState = previewState;
        
        autoSetStartRule(previewState);
        
        //  showError("switchToGrammar: " + grammarFileName);
        
        inputPanel.switchToGrammar(previewState, grammarFile);
        profilerPanel.switchToGrammar(previewState, grammarFile);
        
        ensureStartRuleExists(grammarFile);
        inputPanel.grammarFileSaved();
        
        // refresh tree viewer
        if (previewState.grammar != null && previewState.startRuleName != null) {
            updateParseTreeFromDoc(previewState.grammarFile, true);
            //DEBUG LOG.info("switchToGrammar -> updateParseTreeFromDoc: " + grammarFileName);
            
        } else {
            //DEBUG LOG.info("switchToGrammar -> BAD GRAMMAR: " + grammarFileName);
            showError("Error while parsing grammar." + "See ANTLR Tool Output for more information.");
            clearTabs(null); // blank tree
        }
        
        //DEBUG LOG.info("switchToGrammar has valid grammar? " + previewState.hasValidGrammar());
        
        setEnabled(previewState.hasValidGrammar());
    }
    
    
    /**
     * From 1.18, automatically set the start rule name to the first rule in the grammar
     * if none has been specified
     */
    protected void autoSetStartRule(PreviewState previewState) {
        if (previewState.grammar == null || previewState.grammar.rules.size() == 0) {
            // If there is no grammar all of a sudden, we need to unset the previous rule name
            previewState.startRuleName = null;
        } else if (previewState.startRuleName == null) {
            var rules = previewState.grammar.rules;
            previewState.startRuleName = rules.getElement(0).name;
        }
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
        LOG.info("closeGrammar " + grammarFileName + ' ' + project.getName());
        
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
            var provider = new AltLabelTextProvider(result.parser, preview.grammar);
            
            if (isTabSelected(SelectedTab.TREEVIEWER)) {
                treeViewer.setTreeTextProvider(provider);
                treeViewer.setTree(result.tree);
            }
            
            if (isTabSelected(SelectedTab.HIRACHIE)) {
                hierarchyViewer.setTreeTextProvider(provider);
                hierarchyViewer.setTree(result.tree);
            }
            
            if (result.parser instanceof PreviewParser && isTabSelected(SelectedTab.TOKENLIST)) {
                tokenStreamViewer.setParsingResult(result.parser);
            }
        });
    }
    
    
    void clearParseTree() {
        clearTabs(null);
        errorConsolePanel.clear();
    }
    
    
    private void indicateInvalidGrammarInParseTreePane() {
        showError("Issues with parser and/or lexer grammar(s) prevent preview; see ANTLR 'Tool Output' pane");
    }
    
    
    private void showError(String message) {
        clearTabs(null);
        errorConsolePanel.add(message);
    }
    
    
    private void indicateNoStartRuleInParseTreePane() {
        showError("No Start Rule!");
    }
    
    
    public void updateParseTreeFromDoc(VirtualFile grammarFile, boolean forceUpdate) {
        var controller = ANTLRv4PluginController.getInstance(project);
        //DEBUG LOG.info("updateParseTreeFromDoc");
        if (controller == null)
            return;
        
        var previewState = controller.getPreviewState(grammarFile);
        
        
        if (!previewState.hasValidGrammar()) {
            //DEBUG  LOG.info("updateParseTreeFromDoc: invalid grammar!");
            // likely error in grammar prevents it from loading properly into previewState; bail
            indicateInvalidGrammarInParseTreePane();
            return;
        }
        
        var editor = inputPanel.getInputEditor();
        
        if (editor == null)
            return;
        
        final var inputText = editor.getDocument().getText();
        
        // nothing changed and no forced update
        if (inputText.equals(currentEditorText) && !forceUpdate) {
            return;
        }
        
        currentEditorText = inputText;
        //DEBUG LOG.info("update text: " + inputText);
        
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
        previewState.parseTime = duration;
        
        if (previewState.parsingResult != null) {
            if (isTabSelected(SelectedTab.TREEVIEWER)) {
                updateTreeViewer(previewState, previewState.parsingResult);
            }
            
            if (isTabSelected(SelectedTab.PROFILER)) {
                profilerPanel.setProfilerData(previewState, duration);
            }
            
            inputPanel.showParseErrors(previewState.parsingResult.syntaxErrorListener.getSyntaxErrors());
            return;
        }
        
        if (previewState.startRuleName == null) {
            indicateNoStartRuleInParseTreePane();
            return;
        }
        
        indicateInvalidGrammarInParseTreePane();
    }
    
    
    public void notifySlowParsing(double time) {
        showError("WARNING: Slow parsing" + time + "ms detected; check grammar and input!");
        cancelParserAction.setEnabled(true);
        buttonBar.updateActionsImmediately();
    }
    
    
    public void onParsingCancelled() {
        cancelParserAction.setEnabled(false);
        buttonBar.updateActionsImmediately();
        //showError("Parsing was aborted");
    }
    
    
    public PropertiesPanel getPropertiesPanel() {
        return propertiesPanel;
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
