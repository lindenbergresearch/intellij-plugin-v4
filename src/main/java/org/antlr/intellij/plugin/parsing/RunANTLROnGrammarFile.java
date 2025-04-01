package org.antlr.intellij.plugin.parsing;

import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications.Bus;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.ANTLRv4TokenTypes;
import org.antlr.intellij.plugin.configdialogs.ANTLRv4GrammarProperties;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.psi.AtAction;
import org.antlr.intellij.plugin.psi.GrammarSpecNode;
import org.antlr.v4.Tool;
import org.antlr.v4.codegen.CodeGenerator;
import org.antlr.v4.runtime.misc.Utils;
import org.antlr.v4.tool.Grammar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.stringtemplate.v4.misc.Misc;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Pattern;

import static com.intellij.psi.util.PsiTreeUtil.getChildOfType;
import static org.antlr.intellij.plugin.configdialogs.ANTLRv4GrammarPropertiesStore.getGrammarProperties;
import static org.antlr.intellij.plugin.psi.MyPsiUtils.findChildrenOfType;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

// learned how to do from Grammar-Kit by Gregory Shrago
public class RunANTLROnGrammarFile {
    public static final Logger LOG = Logger.getInstance(RunANTLROnGrammarFile.class);
    public static final String OUTPUT_DIR_NAME = "gen";
    public static final String groupDisplayId = "ANTLR 4 Parser Generation";
    
    private static final Pattern PACKAGE_DEFINITION_REGEX = Pattern.compile("package\\s+[a-z][a-z0-9_]*(\\.[a-z0-9_]+)+[0-9a-z_];");
    
    private final VirtualFile grammarFile;
    private final Project project;
    private final boolean forceGeneration;
    private final String title;
    private final boolean canBeCancelled;
    
    
    public RunANTLROnGrammarFile(
        VirtualFile grammarFile,
        @Nullable final Project project,
        @NotNull final String title,
        final boolean canBeCancelled,
        boolean forceGeneration
    ) {
        this.title = title;
        this.canBeCancelled = canBeCancelled;
        this.grammarFile = grammarFile;
        this.project = project;
        this.forceGeneration = forceGeneration;
    }
    
    
    public static List<String> getANTLRArgsAsList(Project project, VirtualFile vfile) {
        var argMap = getANTLRArgs(project, vfile);
        List<String> args = new ArrayList<>();
        
        for (var option : argMap.keySet()) {
            args.add(option);
            var value = argMap.get(option);
            
            if (!value.isEmpty()) {
                args.add(value);
            }
        }
        
        return args;
    }
    
    
    private static String getPackageName(Project project, VirtualFile virtualFile) {
        return ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
            var psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            
            if (psiFile instanceof PsiJavaFile) {
                return ((PsiJavaFile) psiFile).getPackageName();
            }
            
            return null;
        });
    }
    
    
    private static Map<String, String> getANTLRArgs(Project project, VirtualFile virtualFile) {
        var grammarProperties = getGrammarProperties(project, virtualFile);
        var sourcePath = getParentDir(virtualFile);
        
        var package_ = grammarProperties.getPackage();
        if (isBlank(package_) && !hasPackageDeclarationInHeader(project, virtualFile)) {
            package_ = getPackageName(project, virtualFile);
        }
        
        Map<String, String> args = new HashMap<>();
        
        if (isNotBlank(package_)) {
            args.put("-package", package_);
        }
        
        var language = grammarProperties.getLanguage();
        if (isNotBlank(language)) {
            args.put("-Dlanguage=" + language, "");
        }
        
        // create gen dir at root of project by default, but add in package if any
        var contentRoot = getContentRoot(project, virtualFile);
        var outputDirName = grammarProperties.resolveOutputDirName(project, contentRoot, package_);
        args.put("-o", outputDirName);
        
        var libDir = grammarProperties.resolveLibDir(project, sourcePath);
        var f = new File(libDir);
        if (!f.isAbsolute()) { // if not absolute file spec, it's relative to project root
            libDir = contentRoot.getPath() + File.separator + libDir;
        }
        
        args.put("-lib", libDir);
        
        var encoding = grammarProperties.getEncoding();
        if (isNotBlank(encoding)) {
            args.put("-encoding", encoding);
        }
        
        if (grammarProperties.shouldGenerateParseTreeListener()) {
            args.put("-listener", "");
        } else {
            args.put("-no-listener", "");
        }
        if (grammarProperties.shouldGenerateParseTreeVisitor()) {
            args.put("-visitor", "");
        } else {
            args.put("-no-visitor", "");
        }
        
        return args;
    }
    
    
    private static boolean hasPackageDeclarationInHeader(Project project, VirtualFile grammarFile) {
        return ApplicationManager.getApplication().runReadAction((Computable<Boolean>) () -> {
            var file = PsiManager.getInstance(project).findFile(grammarFile);
            var grammarSpecNode = getChildOfType(file, GrammarSpecNode.class);
            
            if (grammarSpecNode != null) {
                var prequelElementType = ANTLRv4TokenTypes.getRuleElementType(ANTLRv4Parser.RULE_prequelConstruct);
                
                for (var prequelConstruct : findChildrenOfType(grammarSpecNode, prequelElementType)) {
                    var atAction = getChildOfType(prequelConstruct, AtAction.class);
                    
                    if (atAction != null && atAction.getIdText().equals("header")) {
                        return PACKAGE_DEFINITION_REGEX.matcher(atAction.getActionBlockText()).find();
                    }
                }
            }
            
            return false;
        });
    }
    
    
    private static String getParentDir(VirtualFile vfile) {
        return vfile.getParent().getPath();
    }
    
    
    private static VirtualFile getContentRoot(Project project, VirtualFile virtualFile) {
        return ApplicationManager.getApplication().runReadAction((Computable<VirtualFile>) () -> {
            var file = ProjectRootManager.getInstance(project).getFileIndex().getContentRootForFile(virtualFile);
            return file != null ? file : virtualFile;
        });
    }
    
    
    /**
     *
     */
    public void run() {
        if (project == null) {
            return;
        }
        
        ANTLRv4PluginController.printToConsole(project, "-> Start ANTLR On Grammar File: " + grammarFile.getName(), ConsoleViewContentType.LOG_DEBUG_OUTPUT);
        
        var grammarProperties = getGrammarProperties(project, grammarFile);
        
        if (forceGeneration || (grammarProperties.shouldAutoGenerateParser() && isGrammarStale(grammarProperties))) {
            antlr(grammarFile);
        }
        
        var controller = ANTLRv4PluginController.getInstance(project);
        final var previewState = controller.getPreviewState(grammarFile);
        
        // is lexer file? gen .tokens file no matter what as tokens might have changed;
        // a parser that feeds off of that file will need to see the changes.
        if (previewState.hasValidGrammar()) {
            Grammar lexerGrammar = previewState.getLexerGrammar();
            var language = lexerGrammar.getOptionString(ANTLRv4GrammarProperties.PROP_LANGUAGE);
            var tool = ParsingUtils.createANTLRToolForLoadingGrammars(getGrammarProperties(project, grammarFile));
            var codeGenerator = CodeGenerator.create(tool, lexerGrammar, language);
            codeGenerator.writeVocabFile();
            ANTLRv4PluginController.printToConsole(project, "Create Vocab-Token-File: " + grammarFile.getName() + " language: " + language + " previewState: " + previewState, ConsoleViewContentType.LOG_DEBUG_OUTPUT);
        }
        
        // refresh from disk to see new files
        Set<File> generatedFiles = new HashSet<>();
        generatedFiles.add(new File(getOutputDirName()));
        generatedFiles.add(new File(grammarFile.getParent().getPath()));
        LocalFileSystem.getInstance().refreshIoFiles(generatedFiles, true, true, null);
        
        var filenames = Utils.join(generatedFiles.iterator(), ", ");
        
        ANTLRv4PluginController.printToConsole(project, "Generated files: " + filenames, ConsoleViewContentType.LOG_DEBUG_OUTPUT);
        
        
        ANTLRv4PluginController.printToConsole(project, "-> Stop ANTLR On Grammar File: " + grammarFile.getName(), ConsoleViewContentType.LOG_DEBUG_OUTPUT);
    }
    
    
    // TODO: lots of duplication with antlr() function.
    private boolean isGrammarStale(ANTLRv4GrammarProperties grammarProperties) {
        LOG.info("isGrammarStale(grammarProperties=" + grammarProperties + ")");
        var sourcePath = grammarProperties.resolveLibDir(project, getParentDir(grammarFile));
        var fullyQualifiedInputFileName = sourcePath + File.separator + grammarFile.getName();
        
        var controller = ANTLRv4PluginController.getInstance(project);
        assert controller != null;
        final var previewState = controller.getPreviewState(grammarFile);
        var g = previewState.getMainGrammar();
        // Grammar should be updated in the preview state before calling this function
        if (g == null) {
            return false;
        }
        
        var language = g.getOptionString(ANTLRv4GrammarProperties.PROP_LANGUAGE);
        var generator = CodeGenerator.create(null, g, language);
        var recognizerFileName = generator.getRecognizerFileName();
        
        var contentRoot = getContentRoot(project, grammarFile);
        var package_ = grammarProperties.getPackage();
        var outputDirName = grammarProperties.resolveOutputDirName(project, contentRoot, package_);
        var fullyQualifiedOutputFileName = outputDirName + File.separator + recognizerFileName;
        
        var inF = new File(fullyQualifiedInputFileName);
        var outF = new File(fullyQualifiedOutputFileName);
        var stale = inF.lastModified() > outF.lastModified();
        
        var state = "is grammar stale=" + (!stale ? "false" : "true") + " inFile: " + inF.getName() + " outFile" + outF.getName();
        
        LOG.info(state);
        ANTLRv4PluginController.printToConsole(project, state, ConsoleViewContentType.LOG_DEBUG_OUTPUT);
        return stale;
    }
    
    
    /**
     * Run ANTLR tool on file according to preferences in intellij for this file.
     * Returns set of generated files or empty set if error.
     */
    private void antlr(VirtualFile virtualFile) {
        if (virtualFile == null || project == null) {
            return;
        }
        
        LOG.info("antlr(\"" + virtualFile.getPath() + "\")");
        var args = getANTLRArgsAsList(project, virtualFile);
        
        var sourcePath = getParentDir(virtualFile);
        var fullyQualifiedInputFileName = sourcePath + File.separator + virtualFile.getName();
        args.add(fullyQualifiedInputFileName); // add grammar file last
        
        var lexerGrammarFileName = ParsingUtils.getLexerNameFromParserFileName(fullyQualifiedInputFileName);
        if (new File(lexerGrammarFileName).exists()) {
            // build the lexer too as the grammar surely uses it if it exists
            args.add(lexerGrammarFileName);
        }
        
        LOG.info("args: " + Utils.join(args.iterator(), " "));
        
        var antlr = new Tool(args.toArray(new String[args.size()]));
        
        var console = ANTLRv4PluginController.getInstance(project);
        
        ANTLRv4PluginController.printToConsole(project, "running antlr4 " + Misc.join(args.iterator(), " "), ConsoleViewContentType.SYSTEM_OUTPUT);
        
        antlr.removeListeners();
        var listener = new RunANTLRListener(antlr, console);
        antlr.addListener(listener);
        
        try {
            antlr.processGrammarsOnCommandLine();
        } catch (Throwable e) {
            var sw = new StringWriter();
            var pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            var msg = sw.toString();
            
            var notification =
                new Notification(
                    groupDisplayId,
                    "can't generate parser for " + virtualFile.getName(),
                    e.toString(),
                    NotificationType.INFORMATION
                );
            
            Bus.notify(notification, project);
            
            ANTLRv4PluginController.printToConsole(
                project, "can't generate parser for " + virtualFile.getName() + msg + " exception: " + e,
                ConsoleViewContentType.ERROR_OUTPUT
            );
            
            listener.hasOutput = true; // show console below
        }
        
        if (listener.hasOutput) {
            ANTLRv4PluginController.showConsoleWindow(project);
        } else {
            ANTLRv4PluginController.printToConsole(project, "running antlr4 succeeded.", ConsoleViewContentType.SYSTEM_OUTPUT);
        }
    }
    
    
    public String getOutputDirName() {
        var contentRoot = getContentRoot(project, grammarFile);
        var argMap = getANTLRArgs(project, grammarFile);
        var package_ = argMap.get("-package");
        
        return getGrammarProperties(project, grammarFile)
            .resolveOutputDirName(project, contentRoot, package_);
    }
}
