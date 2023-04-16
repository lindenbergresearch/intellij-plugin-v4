package org.antlr.intellij.plugin.validation;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiFile;
import org.antlr.intellij.plugin.parsing.RunANTLROnGrammarFile;
import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.v4.Tool;
import org.antlr.v4.codegen.CodeGenerator;
import org.antlr.v4.codegen.Target;
import org.antlr.v4.parse.ANTLRParser;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.antlr.v4.tool.*;
import org.antlr.v4.tool.ast.GrammarAST;
import org.antlr.v4.tool.ast.RuleRefAST;
import org.jetbrains.annotations.Nullable;
import org.stringtemplate.v4.ST;

import java.io.File;
import java.io.StringReader;
import java.util.*;


public class GrammarIssuesCollector {
    public static final Logger LOG = Logger.getInstance(GrammarIssuesCollector.class.getName());
    private static final String LANGUAGE_ARG_PREFIX = "-Dlanguage=";
    
    
    public static List<GrammarIssue> collectGrammarIssues(PsiFile file) {
        var grammarFileName = file.getVirtualFile().getPath();
        LOG.info("doAnnotate " + grammarFileName);
        var fileContents = file.getText();
        var args = RunANTLROnGrammarFile.getANTLRArgsAsList(file.getProject(), file.getVirtualFile());
        var listener = new GrammarIssuesCollectorToolListener();
        
        var languageArg = findLanguageArg(args);
        
        if (languageArg != null) {
            var language = languageArg.substring(LANGUAGE_ARG_PREFIX.length());
            
            if (!targetExists(language)) {
                var issue = new GrammarIssue(null);
                issue.setAnnotation("Unknown target language '" + language + "', analysis will be done using the default target language 'Java'");
                listener.getIssues().add(issue);
                
                args.remove(languageArg);
            }
        }
        
        final var antlr = new Tool(args.toArray(new String[args.size()]));
        if (!args.contains("-lib")) {
            // getContainingDirectory() must be identified as a read operation on file system
            ApplicationManager.getApplication().runReadAction(() -> {
                antlr.libDirectory = file.getContainingDirectory().toString();
            });
        }
        
        antlr.removeListeners();
        antlr.addListener(listener);
        try {
            var sr = new StringReader(fileContents);
            var in = new ANTLRReaderStream(sr);
            in.name = file.getName();
            
            var ast = antlr.parse(file.getName(), in);
            if (ast == null || ast.hasErrors) {
                for (var issue : listener.getIssues()) {
                    processIssue(file, issue);
                }
                return listener.getIssues();
            }
            
            var g = antlr.createGrammar(ast);
            g.fileName = grammarFileName;
            
            var vocabName = g.getOptionString("tokenVocab");
            if (vocabName != null) { // import vocab to avoid spurious warnings
                LOG.info("token vocab file " + vocabName);
                g.importTokensFromTokensFile();
            }
            
            var vfile = file.getVirtualFile();
            if (vfile == null) {
                LOG.error("doAnnotate no virtual file for " + file);
                return listener.getIssues();
            }
            g.fileName = vfile.getPath();
            antlr.process(g, false);
            
            var unusedRules = getUnusedParserRules(g);
            if (unusedRules != null) {
                for (var r : unusedRules.keySet()) {
                    var ruleDefToken = unusedRules.get(r).getToken();
                    var issue = new GrammarIssue(new GrammarInfoMessage(g.fileName, ruleDefToken, r));
                    listener.getIssues().add(issue);
                }
            }
            
            for (var issue : listener.getIssues()) {
                processIssue(file, issue);
            }
        } catch (Exception e) {
            LOG.error("antlr can't process " + file.getName(), e);
        }
        return listener.getIssues();
    }
    
    
    @Nullable
    private static String findLanguageArg(List<String> args) {
        for (var arg : args) {
            if (arg.startsWith(LANGUAGE_ARG_PREFIX)) {
                return arg;
            }
        }
        
        return null;
    }
    
    
    public static void processIssue(final PsiFile file, GrammarIssue issue) {
        var grammarFile = new File(file.getVirtualFile().getPath());
        if (issue.getMsg() == null || issue.getMsg().fileName == null) { // weird, issue doesn't have a file associated with it
            return;
        }
      
        var issueFile = new File(issue.getMsg().fileName);
        if (!grammarFile.getName().equals(issueFile.getName())) {
            return; // ignore errors from external files
        }
      
        ST msgST = null;
        if (issue.getMsg() instanceof GrammarInfoMessage) { // not in ANTLR so must hack it in
            var t = issue.getMsg().offendingToken;
            issue.getOffendingTokens().add(t);
            msgST = new ST("Unused parser rule: <arg>");
            msgST.add("arg", t.getText());
            msgST.impl.name = "info";
        } else if (issue.getMsg() instanceof GrammarSemanticsMessage) {
            var t = issue.getMsg().offendingToken;
            issue.getOffendingTokens().add(t);
        } else if (issue.getMsg() instanceof LeftRecursionCyclesMessage) {
            List<String> rulesToHighlight = new ArrayList<>();
            var lmsg = (LeftRecursionCyclesMessage) issue.getMsg();
            var cycles =
                (Collection<? extends Collection<Rule>>) lmsg.getArgs()[0];
            for (Collection<Rule> cycle : cycles) {
                for (var r : cycle) {
                    rulesToHighlight.add(r.name);
                    var nameNode = (GrammarAST) r.ast.getChild(0);
                    issue.getOffendingTokens().add(nameNode.getToken());
                }
            }
        } else if (issue.getMsg() instanceof GrammarSyntaxMessage) {
            var t = issue.getMsg().offendingToken;
            issue.getOffendingTokens().add(t);
        } else if (issue.getMsg() instanceof ToolMessage) {
            issue.getOffendingTokens().add(issue.getMsg().offendingToken);
        }
        
        var antlr = new Tool();
        if (msgST == null) {
            msgST = antlr.errMgr.getMessageTemplate(issue.getMsg());
        }
        
        var outputMsg = msgST.render();
        if (antlr.errMgr.formatWantsSingleLineMessage()) {
            outputMsg = outputMsg.replace('\n', ' ');
        }
        
        issue.setAnnotation(outputMsg);
    }
    
    
    private static Map<String, GrammarAST> getUnusedParserRules(Grammar g) {
        if (g.ast == null || g.isLexer())
            return null;
        
        var ruleNodes = g.ast.getNodesWithTypePreorderDFS(IntervalSet.of(ANTLRParser.RULE_REF));
        
        // in case of errors, we walk AST ourselves
        // ANTLR's Grammar object might have bailed on rule defs etc...
        Set<String> ruleRefs = new HashSet<>();
        Map<String, GrammarAST> ruleDefs = new HashMap<>();
        
        for (var x : ruleNodes) {
            if (x.getParent().getType() == ANTLRParser.RULE && !ruleNodes.get(0).equals(x)) {
                ruleDefs.put(x.getText(), x);
            } else if (x instanceof RuleRefAST) {
                var r = (RuleRefAST) x;
                ruleRefs.add(r.getText());
            }
        }
        
        ruleDefs.keySet().removeAll(ruleRefs);
        return ruleDefs;
    }
    
    
    public static boolean targetExists(String language) {
        var targetName = "org.antlr.v4.codegen.target." + language + "Target";
        try {
            var c = Class.forName(targetName).asSubclass(Target.class);
            var ctor = c.getConstructor(CodeGenerator.class);
            return true;
        } catch (Exception e) { // ignore errors; we're detecting presence only
        }
        return false;
    }
}
