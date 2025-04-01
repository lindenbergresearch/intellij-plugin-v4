package org.antlr.intellij.plugin;

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.BackgroundTaskUtil;
import com.intellij.openapi.progress.util.ProgressWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.ui.content.ContentFactory;
import org.antlr.intellij.plugin.configdialogs.ANTLRv4UISettingsState;
import org.antlr.intellij.plugin.parsing.ParsingUtils;
import org.antlr.intellij.plugin.parsing.RunANTLROnGrammarFile;
import org.antlr.intellij.plugin.preview.PreviewPanel;
import org.antlr.intellij.plugin.preview.PreviewState;
import org.antlr.intellij.plugin.psi.LexerRuleRefNode;
import org.antlr.intellij.plugin.psi.LexerRuleSpecNode;
import org.antlr.v4.parse.ANTLRParser;
import org.antlr.v4.runtime.RuntimeMetaData;
import org.antlr.v4.tool.LexerGrammar;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * This class is the controller for the ANTLR plugin. It receives
 * events and can send them on to its contained components. For example,
 * saving the grammar editor or flipping to a new grammar sends an event
 * to this object, which forwards on update events to the preview tool window.
 * <p>
 * The main components are related to the console tool window forever output and
 * the main panel of the preview tool window.
 * <p>
 * This controller also manages the cache of grammar/editor combinations
 * needed for the preview window. Updates must be made atomically so that
 * the grammars and editors are consistently associated with the same window.
 */
public class ANTLRv4PluginController implements ProjectComponent, Disposable {
    public static final Logger LOG = Logger.getInstance(ANTLRv4PluginController.class);
    public static final String PLUGIN_ID = "antlr-intellij-plugin-neo";
    
    public static final String PREVIEW_WINDOW_ID = "ANTLR Preview";
    public static final String CONSOLE_WINDOW_ID = "ANTLR I/O";
    
    private static final Key<GrammarEditorMouseAdapter> EDITOR_MOUSE_LISTENER_KEY = Key.create("EDITOR_MOUSE_LISTENER_KEY");
    
    private final MyVirtualFileAdapter myVirtualFileAdapter = new MyVirtualFileAdapter();
    private final MyFileEditorManagerAdapter myFileEditorManagerAdapter = new MyFileEditorManagerAdapter();
    
    private final static ANTLRv4FileType ANTL_FILE_TYPE = ANTLRv4FileType.INSTANCE;
    
    public Project project;
    public boolean projectIsClosed = false;
    public ConsoleView console;
    public Map<VirtualFile, PreviewState> previewStateCache = Collections.synchronizedMap(new HashMap<>());
    public PreviewPanel previewPanel;
    
    private ToolWindow consoleWindow;
    private ToolWindow previewWindow;
    
    private ProgressIndicator parsingProgressIndicator;
    private ProgressIndicator runIndicator;
    
    private int counter = 0;
    private boolean logDebugMessages = false;
    
    
    public ANTLRv4PluginController(Project project) {
        this.project = project;
    }
    
    
    public static ANTLRv4PluginController getInstance(Project project) {
        if (project == null) {
            LOG.error("getInstance(): project is null");
            return null;
        }
        
        var pc = project.getComponent(ANTLRv4PluginController.class);
        if (pc == null) {
            LOG.error("getInstance(): getComponent() for " + project.getName() + " returns null!");
        }
        
        return pc;
    }
    
    /* ----------------------------------------------------------------------------------------------------------------- */
    
    
    /**
     * Print the given text to the ANTLR Console with the given content-type.
     *
     * @param text        The text/message to print.
     * @param contentType The content type for output.
     */
    public void printToConsole(String text, ConsoleViewContentType contentType) {
        var appSettings = ANTLRv4UISettingsState.getInstance();
        logDebugMessages = appSettings.isEnableDebugConsole();
        
        if (!logDebugMessages && (contentType == null || contentType.equals(ConsoleViewContentType.LOG_DEBUG_OUTPUT))) {
            return;
        }
        
        if (consoleWindow != null && console != null) {
            console.print((++counter) + " [" + ANTLRUtils.getTimeStamp() + "] " + text + '\n', contentType);
            console.requestScrollingToEnd();
        } else {
            LOG.warn("printToConsole(String, ConsoleViewContentType): console window or console is null! Message: " + text);
        }
    }
    
    
    /**
     * Print the given text to the ANTLR Console with standard content-type.
     *
     * @param text The text/message to print.
     */
    public void printToConsole(String text) {
        printToConsole(text, ConsoleViewContentType.NORMAL_OUTPUT);
    }
    
    
    /**
     * Print the given text to the ANTLR Console with standard content-type.
     *
     * @param project     The project to assign to.
     * @param text        The text/message to print.
     * @param contentType The content type for output.
     */
    public static void printToConsole(Project project, String text, ConsoleViewContentType contentType) {
        var instance = ANTLRv4PluginController.getInstance(project);
        
        if (instance != null) {
            instance.printToConsole(text, contentType);
        } else {
            LOG.warn("printToConsole(): Controller is null! Log-Message is: " + text);
        }
    }
    
    
    /**
     * Print the given text to the ANTLR Console with standard content-type.
     *
     * @param project The project to assign to.
     * @param text    The text/message to print.
     */
    public static void printToConsole(Project project, String text) {
        printToConsole(project, text, ConsoleViewContentType.NORMAL_OUTPUT);
    }
    
    
    /**
     * Request focus and scrolling to the end of the output.
     */
    public void requestConsoleFocus() {
        getConsoleWindow().show();
        console.requestScrollingToEnd();
    }
    
    
    /**
     * Set focus to console tool-window and show it.
     *
     * @param project The assigned project.
     */
    public static void showConsoleWindow(final Project project) {
        var instance = ANTLRv4PluginController.getInstance(project);
        
        if (instance == null) {
            return;
        }
        
        ApplicationManager.getApplication().invokeLater(
            () -> instance.requestConsoleFocus()
        );
    }
    
    /* ------------------------------------------------------------------------------------------------------------------ */
    
    
    /**
     * Get the state information associated with the grammar in the current
     * editor window. If there is no grammar in the editor window, return null.
     * If there is a grammar, return any existing preview state else
     * create a new one in store in the map.
     * <p>
     * Too dangerous; turning off but might be useful later.
     * public @org.jetbrains.annotations.Nullable PreviewState getPreviewState() {
     * VirtualFile currentGrammarFile = getCurrentGrammarFile();
     * if ( currentGrammarFile==null ) {
     * return null;
     * }
     * String currentGrammarFileName = currentGrammarFile.getPath();
     * if ( currentGrammarFileName==null ) {
     * return null; // we are not looking at a grammar file
     * }
     * return getPreviewState(currentGrammarFile);
     * }
     */
    
    // These "get current editor file" routines should only be used
    // when you are sure the user is in control and is viewing the
    // right file (i.e., don't use these during project loading etc...)
    public static VirtualFile getCurrentEditorFile(Project project) {
        var fileEditorManager = FileEditorManager.getInstance(project);
        
        if (fileEditorManager != null && fileEditorManager.getSelectedEditor() != null) {
            return fileEditorManager.getSelectedEditor().getFile();
        }
        
        return null;
    }
    
    
    public static VirtualFile getCurrentGrammarFile(Project project) {
        var editorFile = getCurrentEditorFile(project);
        
        if (editorFile == null) {
            return null;
        }
        
        if (editorFile.getName().endsWith('.' + ANTL_FILE_TYPE.getDefaultExtension())) {
            return editorFile;
        }
        
        return null;
    }
    
    
    @Override
    public void projectOpened() {
        createToolWindows();
        installListeners();
    }
    
    
    public void createToolWindows() {
        LOG.info("createToolWindows " + project.getName());
        var toolWindowManager = ToolWindowManager.getInstance(project);
        
        previewPanel = new PreviewPanel(project);
        
        var contentFactory = ContentFactory.getInstance();
        
        toolWindowManager.invokeLater(() -> {
            var content = contentFactory.createContent(previewPanel, "", false);
            content.setCloseable(false);
            
            previewWindow = toolWindowManager.registerToolWindow(PREVIEW_WINDOW_ID, true, ToolWindowAnchor.BOTTOM);
            previewWindow.getContentManager().addContent(content);
            previewWindow.setIcon(ANTLRv4Icons.getToolWindow());
            previewWindow.show();
        });
        
        var factory = TextConsoleBuilderFactory.getInstance();
        var consoleBuilder = factory.createBuilder(project);
        this.console = consoleBuilder.getConsole();
        
        toolWindowManager.invokeLater(() -> {
            var consoleComponent = console.getComponent();
            var content = contentFactory.createContent(consoleComponent, "ANTLR Console", false);
            content.setCloseable(false);
            
            consoleWindow = toolWindowManager.registerToolWindow(CONSOLE_WINDOW_ID, true, ToolWindowAnchor.BOTTOM);
            consoleWindow.getContentManager().addContent(content);
            consoleWindow.setIcon(ANTLRv4Icons.getToolWindow());
            
            var plugin = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID));
            
            var version = "no plugin-descriptor found";
            if (plugin != null) {
                version = plugin.getName() + " v" + plugin.getVersion() + ", ANTLR Runtime: v" + RuntimeMetaData.VERSION +
                    ", Java: v" + SystemInfo.JAVA_VERSION +
                    ", running on: " + SystemInfo.getOsNameAndVersion() + ' ' + SystemInfo.OS_ARCH;
            }
            
            LOG.info(version);
            printToConsole(version);
            printToConsole("Project: " + project.getName());
        });
    }
    
    
    @Override
    public void projectClosed() {
//        LOG.info("projectClosed " + project.getName());
//        //synchronized ( shutdownLock ) { // They should be called from EDT only so no lock
//        projectIsClosed = true;
//        uninstallListeners();
//
//        for (var it : grammarToPreviewState.values()) {
//            previewPanel.inputPanel.releaseEditor(it);
//        }
//
//        console = null;
//        previewPanel = null;
//        previewWindow = null;
//        consoleWindow = null;
//        project = null;
//        grammarToPreviewState = null;
    }
    
    
    // seems that intellij can kill and reload a project w/o user knowing.
    // a ptr was left around that pointed at a disposed project. led to
    // problem in switchGrammar. Probably was a listener still attached and trigger
    // editor listeners released in editorReleased() events.
    public void uninstallListeners() {
        VirtualFileManager.getInstance().removeVirtualFileListener(myVirtualFileAdapter);
        
        if (!project.isDisposed()) {
            var msgBus = project.getMessageBus().connect(project);
            msgBus.disconnect();
        }
    }
    
    
    @Override
    public void dispose() {
        LOG.info(" dispose(" + project.getName() + ')');
        
        projectIsClosed = true;
        uninstallListeners();
        
        for (var it : previewStateCache.values()) {
            previewPanel.inputPanel.releaseEditor(it);
        }
        
        console.dispose();
        
        console = null;
        previewPanel = null;
        previewWindow = null;
        consoleWindow = null;
        project = null;
        previewStateCache = null;
    }
    
    
    @NotNull
    @Override
    public String getComponentName() {
        return "antlr.ProjectComponent";
    }
    
    
    public void installListeners() {
        LOG.info("installListeners() " + project.getName());
        // Listen for .g4 file saves
        VirtualFileManager.getInstance().addVirtualFileListener(myVirtualFileAdapter, this);
        
        // Listen for editor window changes
        var msgBus = project.getMessageBus().connect(this);
        msgBus.subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            myFileEditorManagerAdapter
        );
        
        var factory = EditorFactory.getInstance();
        factory.addEditorFactoryListener(
            new EditorFactoryAdapter() {
                @Override
                public void editorCreated(@NotNull EditorFactoryEvent event) {
                    final var editor = event.getEditor();
                    final var doc = editor.getDocument();
                    var virtualFile = FileDocumentManager.getInstance().getFile(doc);
                    
                    if (virtualFile != null && virtualFile.getName().endsWith('.' + ANTL_FILE_TYPE.getDefaultExtension())) {
                        var listener = new GrammarEditorMouseAdapter();
                        var listener2 = new GrammarEditorMouseMotionListener(project);
                        editor.putUserData(EDITOR_MOUSE_LISTENER_KEY, listener);
                        editor.addEditorMouseListener(listener);
                        editor.addEditorMouseMotionListener(listener2);
                        
                        var file = event.getEditor().getVirtualFile();
                        printToConsole("editorCreated(file=" + (file != null ? file.getName() : "null)" + ')'), ConsoleViewContentType.LOG_DEBUG_OUTPUT);
                    }
                }
                
                
                @Override
                public void editorReleased(@NotNull EditorFactoryEvent event) {
                    LOG.info("editorReleased(" + event + ')');
                    
                    var editor = event.getEditor();
                    
                    if (editor.getProject() != null && !Objects.equals(editor.getProject(), project)) {
                        return;
                    }
                    
                    var listener = editor.getUserData(EDITOR_MOUSE_LISTENER_KEY);
                    if (listener != null) {
                        editor.removeEditorMouseListener(listener);
                        editor.putUserData(EDITOR_MOUSE_LISTENER_KEY, null);
                    }
                    printToConsole("editorReleased(file=" + event.getEditor().getVirtualFile() + ')', ConsoleViewContentType.LOG_DEBUG_OUTPUT);
                }
            }, this
        );
    }
    
    
    /**
     * The test ANTLR rule action triggers this event. This can occur
     * only occur when the current editor the showing a grammar, because
     * that is the only time that the action is enabled. We will see
     * a file changed event when the project loads the first grammar file.
     */
    public void setStartRuleNameEvent(VirtualFile grammarFile, String startRuleName) {
        var previewState = getPreviewState(grammarFile);
        
        printToConsole(project, "setStartRuleNameEvent(" + grammarFile.getName() + ", " + startRuleName + ')');
        previewState.setStartRuleName(startRuleName);
        
        if (previewPanel != null) {
            previewPanel.getInputPanel().setStartRuleName(grammarFile, startRuleName); // notify the view
            previewPanel.updateParseTreeFromDoc(grammarFile, true);
        } else {
            LOG.error("setStartRuleNameEvent called before preview panel created");
        }
    }
    
    
    public void grammarFileSavedEvent(VirtualFile grammarFile) {
        printToConsole("grammarFileSavedEvent(" + grammarFile.getName() + ")", ConsoleViewContentType.LOG_DEBUG_OUTPUT);
        updateGrammarObjectsFromFile(grammarFile, true); // force reload
        
        if (previewPanel != null) {
            previewPanel.grammarFileSaved(grammarFile);
        } else {
            LOG.error("grammarFileSavedEvent called before preview panel created");
        }
    }
    
    
    public void currentEditorFileChangedEvent(VirtualFile oldFile, VirtualFile newFile) {
        LOG.info("currentEditorFileChangedEvent(" + oldFile + ", " + newFile + ')');
        printToConsole("currentEditorFileChangedEvent(" + oldFile + ", " + newFile + ')', ConsoleViewContentType.LOG_DEBUG_OUTPUT);
        if (newFile == null) { // all files must be closed I guess
            return;
        }
        
        var fileSuffix = '.' + ANTLRv4FileType.INSTANCE.getDefaultExtension();
        
        if (newFile.getName().endsWith(".g")) {
            var text = "currentEditorFileChangedEvent ANTLR 4 cannot handle .g files";
            
            LOG.info(text);
            printToConsole(text, ConsoleViewContentType.LOG_ERROR_OUTPUT);
            //  hidePreview();
            return;
        }
        
        if (!newFile.getName().endsWith(fileSuffix)) {
            //  hidePreview();
            return;
        }
        
        // When switching from a lexer grammar, update its objects in case the grammar was modified.
        // The updated objects might be needed later by another dependant grammar.
        if (oldFile != null && oldFile.getName().endsWith(fileSuffix)) {
            updateGrammarObjectsFromFile(oldFile, true);
        }
        
        var previewState = getPreviewState(newFile);
        if (!previewState.hasValidGrammar()) { // only load grammars if none is there
            updateGrammarObjectsFromFile(newFile, false);
        }
        
        if (previewPanel != null) {
            previewPanel.grammarFileChanged(newFile);
        }
    }
    
    
    public void mouseEnteredGrammarEditorEvent(VirtualFile virtualFile, EditorMouseEvent e) {
        if (previewPanel != null) {
            var profilerPanel = previewPanel.getProfilerPanel();
            
            if (profilerPanel != null) {
                profilerPanel.mouseEnteredGrammarEditorEvent(virtualFile, e);
            }
        }
    }
    
    
    public void editorFileClosedEvent(VirtualFile grammarFile) {
        // hopefully called only from swing EDT
        var fileSuffix = '.' + ANTLRv4FileType.INSTANCE.getDefaultExtension();
        
        if (!grammarFile.getName().endsWith(fileSuffix)) {
            hidePreview();
            return;
        }
        
        // Dispose of state, editor, and such for this file
        var previewState = previewStateCache.get(grammarFile);
        if (previewState == null) { // project closing must have done already
            return;
        }
        
        previewState.setGrammar(null); // wack old ref to the Grammar for text in editor
        previewState.setLexerGrammar(null);
        
        previewPanel.closeGrammar(grammarFile);
        previewStateCache.remove(grammarFile);
        
        // close tool window
        hidePreview();
    }
    
    
    private void hidePreview() {
        if (previewPanel != null) {
            previewPanel.setEnabled(false);
        }
//        if (previewWindow != null) {
//            previewWindow.hide(null);
//        }
    }
    
    
    /**
     * Make sure to run after updating grammars in previewState
     */
    public void runANTLRTool(final VirtualFile grammarFile) {
        var title = "ANTLR Code Generation";
        var canBeCancelled = true;
        var forceGeneration = false;
        
        var gen = new RunANTLROnGrammarFile(
            grammarFile,
            project,
            title,
            canBeCancelled,
            forceGeneration
        );
        
        ProgressManager.getInstance().runProcessWithProgressSynchronously(
            () -> gen.run(),
            title,
            canBeCancelled,
            project
        );
    }
    
    
    /**
     * Look for state information concerning this grammar file and update
     * the Grammar objects.  This does not necessarily update the grammar file
     * in the current editor window.  Either we are already looking at
     * this grammar or we will have seen a grammar file changed event.
     * (I hope!)
     */
    private void updateGrammarObjectsFromFile(VirtualFile grammarFile, boolean generateTokensFile) {
        LOG.info("updateGrammarObjectsFromFile(" + grammarFile + ", " + generateTokensFile + ')');
        printToConsole("updateGrammarObjectsFromFile(" + grammarFile + ", " + generateTokensFile + ')', ConsoleViewContentType.LOG_DEBUG_OUTPUT);
        updatePreviewStateByGrammar(grammarFile);
        
        // if grammarFileName is a separate lexer, we need to look for
        // its matching parser, if any, that is loaded in an editor
        // (don't go looking on disk).
        var previewState = getAssociatedParserIfLexer(grammarFile.getPath());
        if (previewState != null) {
            if (generateTokensFile) {
                // Run the tool to regenerate the .tokens file, which will be
                // needed in the parser grammar
                runANTLRTool(grammarFile);
                printToConsole("create needed token-file by main grammar: " + grammarFile.getName(), ConsoleViewContentType.LOG_DEBUG_OUTPUT);
            }
            
            // try to load lexer again and associate with this parser grammar.
            // must update parser too as tokens have changed
            var grammar = updatePreviewStateByGrammar(previewState.getGrammarFile());
            printToConsole("reloading grammar: " + grammar, ConsoleViewContentType.LOG_INFO_OUTPUT);
        }
    }
    
    
    private String updatePreviewStateByGrammar(VirtualFile grammarFile) {
        var grammarFileName = grammarFile.getPath();
        var grammars = ParsingUtils.loadGrammars(grammarFile, project);
        var previewState = getPreviewState(grammarFile);
        
        if (grammars != null) {
            synchronized (previewState) { // build atomically
                previewState.setLexerGrammar((LexerGrammar) grammars[0]);
                previewState.setGrammar(grammars[1]);
                previewState.reloadPreviewData();
            }
        } else {
            printToConsole("updatePreviewStateByGrammar(" + grammarFile.getPath() + ") no valid grammars!", ConsoleViewContentType.LOG_WARNING_OUTPUT);
            previewState.setLexerGrammar(null);
            previewState.setGrammar(null);
        }
        return grammarFileName;
    }
    
    
    // TODO there could be multiple grammars importing/tokenVocab'ing this lexer grammar
    public PreviewState getAssociatedParserIfLexer(String grammarFileName) {
        for (var previewState : previewStateCache.values()) {
            if (previewState != null && previewState.getGrammar() != null &&
                (sameFile(grammarFileName, previewState.getLexerGrammar().fileName) || Objects.equals(previewState.getLexerGrammar(), ParsingUtils.BAD_LEXER_GRAMMAR))) {
                // s has a lexer with same filename, see if there is a parser grammar
                // (not a combined grammar)
                if (previewState.getGrammar() != null && previewState.getGrammar().getType() == ANTLRParser.PARSER) {
                    return previewState;
                }
            }
            
            if (previewState != null && previewState.getGrammar() != null && previewState.getGrammar().importedGrammars != null) {
                for (var importedGrammar : previewState.getGrammar().importedGrammars) {
                    if (grammarFileName.equals(importedGrammar.fileName)) {
                        return previewState;
                    }
                }
            }
        }
        return null;
    }
    
    
    private boolean sameFile(String pathOne, String pathTwo) {
        // use new File() to support both / and \ in paths
        return FileUtil.filesEqual(new File(pathOne), new File(pathTwo));
    }
    
    
    public void parseText(final VirtualFile grammarFile, String inputText) {
        if (grammarFile == null) {
            return;
        }
        
        // Wipes out the console and also any error annotations
        previewPanel.inputPanel.clearParseErrors();
        
        final var previewState = getPreviewState(grammarFile);
        
        //abortCurrentParsing();
        
        var start = System.nanoTime();
        
        // Parse text in a background thread to avoid freezing the UI if the grammar is badly written
        // and takes ages to interpret the input.
        parsingProgressIndicator = BackgroundTaskUtil.executeAndTryWait(
            (indicator) -> {
                runIndicator = indicator;
                previewState.setParsingResult(
                    ParsingUtils.parseText(
                        previewState.getGrammar(), previewState.getLexerGrammar(), previewState.getStartRuleName(),
                        grammarFile, inputText, project
                    )
                );
                
                return () -> previewPanel.onParsingCompleted(previewState, System.nanoTime() - start);
            },
            () -> {
                previewPanel.notifySlowParsing((System.nanoTime() - start) / 1_000_000.f);
                abortCurrentParsing();
            },
            ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS * 3,
            false
        );
    }
    
    
    public void abortCurrentParsing() {
        if (runIndicator != null) {
            runIndicator.cancel();
            runIndicator = null;
        }
        
        if (parsingProgressIndicator != null) {
            parsingProgressIndicator.cancel();
            parsingProgressIndicator = null;
        }
        
        previewPanel.onParsingCancelled();
        ANTLRv4PluginController.printToConsole(project, "Parsing aborted for grammar: " + getCurrentGrammarFile().getName(), ConsoleViewContentType.LOG_WARNING_OUTPUT);
    }
    
    
    public PreviewPanel getPreviewPanel() {
        return previewPanel;
    }
    
    
    public ConsoleView getConsole() {
        return console;
    }
    
    
    public ToolWindow getConsoleWindow() {
        return consoleWindow;
    }
    
    
    public ToolWindow getPreviewWindow() {
        return previewWindow;
    }
    
    
    public @NotNull PreviewState getPreviewState(VirtualFile grammarFile) {
        ANTLRv4PluginController.printToConsole(project, "getPreviewState(" + grammarFile.getPath() + ')', ConsoleViewContentType.LOG_DEBUG_OUTPUT);
        if (previewStateCache.containsKey(grammarFile)) {
            return previewStateCache.get(grammarFile);
        }
        
        var stateForCurrentGrammar = new PreviewState(project, grammarFile);
        previewStateCache.put(grammarFile, stateForCurrentGrammar);
        
        return stateForCurrentGrammar;
    }
    
    
    public Editor getEditor(VirtualFile virtualFile) {
        final var fileDocumentManager = FileDocumentManager.getInstance();
        final var document = fileDocumentManager.getDocument(virtualFile);
        if (document == null) {
            return null;
        }
        
        var factory = EditorFactory.getInstance();
        final var editors = factory.getEditors(document, previewPanel.project);
        if (editors.length == 0) {
            // no editor found for this file. likely an out-of-sequence issue
            // where Intellij is opening a project and doesn't fire events
            // in order we'd expect.
            ANTLRv4PluginController.printToConsole(project, "getEditor(" + virtualFile.getName() + ") No editor found for file!", ConsoleViewContentType.LOG_DEBUG_OUTPUT);
            return null;
        }
        
        if (editors.length > 1) {
            ANTLRv4PluginController.printToConsole(project, "getEditor(" + virtualFile.getName() + ") Too many editors: " + editors.length + " found for file!", ConsoleViewContentType.LOG_DEBUG_OUTPUT);
        }
        
        return editors[0]; // hope just one
    }
    
    
    public VirtualFile getCurrentGrammarFile() {
        return getCurrentGrammarFile(project);
    }
    
    
    /* ------------------------------------------------------------------------------------------------------------------ */
    
    
    private static class GrammarEditorMouseMotionListener implements EditorMouseMotionListener {
        private final Project project;
        private PsiElement lastElement;
        
        
        public GrammarEditorMouseMotionListener(Project project) {
            this.project = project;
        }
        
        
        @Override
        public void mouseMoved(@NotNull EditorMouseEvent e) {
            //EditorMouseMotionListener.super.mouseMoved(e);
            var fileSuffix = '.' + ANTLRv4FileType.INSTANCE.getDefaultExtension();
            var virtualFile = FileDocumentManager.getInstance().getFile(e.getEditor().getDocument());
            
            if (virtualFile == null || !virtualFile.getName().endsWith(fileSuffix)) {
                return;
            }
            
            var psiDocumentManager = PsiDocumentManager.getInstance(project);
            var psiFile = psiDocumentManager.getPsiFile(e.getEditor().getDocument());
            var psiElement = (psiFile != null) ? psiFile.findElementAt(e.getOffset()) : null;
            
            if (lastElement != null && lastElement.equals(psiElement)) {
                return;
            }
            
            lastElement = psiElement;
            
            if (psiFile != null && psiElement instanceof LexerRuleRefNode lexerRuleRefNode) {
                var psiReference = lexerRuleRefNode.getReference();
                
                if (psiReference != null && psiReference.resolve() != null) {
                    var ruleSpecNode = (LexerRuleSpecNode) psiReference.resolve();
                    var nodeFirstChild = ruleSpecNode.getChildren()[0];
                    e.getEditor().getContentComponent().setToolTipText(nodeFirstChild.getText());
                }
            } else {
                e.getEditor().getContentComponent().setToolTipText(null);
            }
        }
    }
    
    
    private class GrammarEditorMouseAdapter implements EditorMouseListener {
        
        
        @Override
        public void mouseClicked(EditorMouseEvent editorMouseEvent) {
            var doc = editorMouseEvent.getEditor().getDocument();
            var fileSuffix = '.' + ANTLRv4FileType.INSTANCE.getDefaultExtension();
            var virtualFile = FileDocumentManager.getInstance().getFile(doc);
            
            if (virtualFile != null && virtualFile.getName().endsWith(fileSuffix)) {
                mouseEnteredGrammarEditorEvent(virtualFile, editorMouseEvent);
            }
        }
    }
    
    
    private class MyVirtualFileAdapter implements VirtualFileListener {
        @Override
        public void contentsChanged(VirtualFileEvent event) {
            final var virtualFile = event.getFile();
            var fileSuffix = '.' + ANTLRv4FileType.INSTANCE.getDefaultExtension();
            
            if (!virtualFile.getName().endsWith(fileSuffix)) {
                return;
            }
            
            if (!projectIsClosed && !ApplicationManager.getApplication().isUnitTestMode()) {
                grammarFileSavedEvent(virtualFile);
            }
        }
    }
    
    
    private class MyFileEditorManagerAdapter implements FileEditorManagerListener {
        @Override
        public void selectionChanged(@NotNull FileEditorManagerEvent event) {
            if (!projectIsClosed) {
                currentEditorFileChangedEvent(event.getOldFile(), event.getNewFile());
            }
        }
        
        
        @Override
        public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile virtualFile) {
            if (!projectIsClosed) {
                editorFileClosedEvent(virtualFile);
            }
        }
    }
}
