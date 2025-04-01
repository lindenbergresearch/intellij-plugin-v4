package org.antlr.intellij.plugin.parsing;

import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.antlr.intellij.adaptor.parser.SyntaxErrorListener;
import org.antlr.intellij.plugin.ANTLRv4FileType;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.FilterErrorManager;
import org.antlr.intellij.plugin.configdialogs.ANTLRv4GrammarProperties;
import org.antlr.intellij.plugin.parser.ANTLRv4Lexer;
import org.antlr.intellij.plugin.parser.ANTLRv4Parser;
import org.antlr.intellij.plugin.preview.PreviewState;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.v4.Tool;
import org.antlr.v4.parse.ANTLRParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.misc.Utils;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Tree;
import org.antlr.v4.runtime.tree.Trees;
import org.antlr.v4.tool.ErrorType;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.LexerGrammar;
import org.antlr.v4.tool.ast.GrammarRootAST;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.antlr.intellij.plugin.configdialogs.ANTLRv4GrammarPropertiesStore.getGrammarProperties;

public class ParsingUtils {
    public static Grammar BAD_PARSER_GRAMMAR;
    public static LexerGrammar BAD_LEXER_GRAMMAR;
    
    
    static {
        try {
            ParsingUtils.BAD_PARSER_GRAMMAR = new Grammar("grammar BAD; a : 'bad' ;");
            ParsingUtils.BAD_PARSER_GRAMMAR.name = "BAD_PARSER_GRAMMAR";
            ParsingUtils.BAD_LEXER_GRAMMAR = new LexerGrammar("lexer grammar BADLEXER; A : 'bad' ;");
            ParsingUtils.BAD_LEXER_GRAMMAR.name = "BAD_LEXER_GRAMMAR";
        } catch (RecognitionException re) {
            ANTLRv4PluginController.LOG.error("can't init bad grammar markers");
        }
    }
    
    
    public static Token nextRealToken(CommonTokenStream tokens, int i) {
        var n = tokens.size();
        i++; // search after current i token
        
        if (i >= n || i < 0) {
            return null;
        }
        
        var t = tokens.get(i);
        while (t.getChannel() == Token.HIDDEN_CHANNEL) {
            if (t.getType() == Token.EOF) {
                var tokenSource = tokens.getTokenSource();
                
                if (tokenSource == null) {
                    return new CommonToken(Token.EOF, "EOF");
                }
                
                var tokenFactory = tokenSource.getTokenFactory();
                if (tokenFactory == null) {
                    return new CommonToken(Token.EOF, "EOF");
                }
                
                return tokenFactory.create(Token.EOF, "EOF");
            }
            
            i++;
            if (i >= n) {
                return null; // just in case no EOF
            }
            
            t = tokens.get(i);
        }
        
        return t;
    }
    
    
    public static Token previousRealToken(CommonTokenStream tokens, int i) {
        var size = tokens.size();
        i--; // search before current i token
        
        if (i >= size || i < 0) {
            return null;
        }
        
        var t = tokens.get(i);
        while (t.getChannel() == Token.HIDDEN_CHANNEL) {
            i--;
            if (i < 0) {
                return null;
            }
            
            t = tokens.get(i);
        }
        
        return t;
    }
    
    
    public static Token getTokenUnderCursor(PreviewState previewState, int offset) {
        if (previewState == null || previewState.getParsingResult() == null) return null;
        
        var parser = (PreviewParser) previewState.getParsingResult().parser;
        var tokenStream = (CommonTokenStream) parser.getInputStream();
        return ParsingUtils.getTokenUnderCursor(tokenStream, offset);
    }
    
    
    public static Token getTokenUnderCursor(CommonTokenStream tokens, int offset) {
        Comparator<Token> cmp = (a, b) -> {
            if (a.getStopIndex() < b.getStartIndex()) return -1;
            if (a.getStartIndex() > b.getStopIndex()) return 1;
            return 0;
        };
        
        if (offset < 0 || offset >= tokens.getTokenSource().getInputStream().size()) {
            return null;
        }
        
        var key = new CommonToken(Token.INVALID_TYPE, "");
        key.setStartIndex(offset);
        key.setStopIndex(offset);
        
        var tokenList = tokens.getTokens();
        var i = Collections.binarySearch(tokenList, key, cmp);
        
        if (i >= 0) {
            return tokenList.get(i);
        }
        
        return null;
    }
    
    
    /*
    [77] = {org.antlr.v4.runtime.CommonToken@16710}"[@77,263:268='import',<25>,9:0]"
    [78] = {org.antlr.v4.runtime.CommonToken@16709}"[@78,270:273='java',<100>,9:7]"
     */
    public static Token getSkippedTokenUnderCursor(CommonTokenStream tokens, int offset) {
        if (offset < 0 || offset >= tokens.getTokenSource().getInputStream().size()) {
            return null;
        }
        
        Token prevToken = null;
        for (var t : tokens.getTokens()) {
            var begin = t.getStartIndex();
            var end = t.getStopIndex();
            
            if ((prevToken == null || offset > prevToken.getStopIndex()) && offset < begin) {
                // found in between
                var tokenSource = tokens.getTokenSource();
                CharStream inputStream = null;
                if (tokenSource != null) {
                    inputStream = tokenSource.getInputStream();
                }
                
                return new CommonToken(
                    new Pair<>(tokenSource, inputStream),
                    Token.INVALID_TYPE,
                    -1,
                    prevToken != null ? prevToken.getStopIndex() + 1 : 0,
                    begin - 1
                );
            }
            
            if (offset >= begin && offset <= end) {
                return t;
            }
            
            prevToken = t;
        }
        
        return null;
    }
    
    
    public static CommonTokenStream tokenizeANTLRGrammar(String text) {
        var input = CharStreams.fromString(text);
        var lexer = new ANTLRv4Lexer(input);
        
        CommonTokenStream tokens = new TokenStreamSubset(lexer);
        tokens.fill();
        
        return tokens;
    }
    
    
    public static ParseTree getParseTreeNodeWithToken(ParseTree tree, Token token) {
        if (tree == null || token == null) {
            return null;
        }
        
        var tokenNodes = Trees.findAllTokenNodes(tree, token.getType());
        for (var t : tokenNodes) {
            var node = (TerminalNode) t;
            
            if (Objects.equals(node.getPayload(), token)) {
                return node;
            }
        }
        
        return null;
    }
    
    
    public static ParsingResult parseANTLRGrammar(String text) {
        var input = CharStreams.fromString(text);
        var lexer = new ANTLRv4Lexer(input);
        var tokens = new TokenStreamSubset(lexer);
        var parser = new ANTLRv4Parser(tokens);
        
        var listener = new SyntaxErrorListener();
        parser.removeErrorListeners();
        parser.addErrorListener(listener);
        
        lexer.removeErrorListeners();
        lexer.addErrorListener(listener);
        
        var grammarSpec = parser.grammarSpec();
        return new ParsingResult(parser, grammarSpec, listener);
    }
    
    
    public static ParsingResult parseText(
        Grammar grammar,
        LexerGrammar lexerGrammar,
        String startRuleName,
        final VirtualFile grammarFile,
        String inputText,
        Project project
    ) {
        var grammarProperties = getGrammarProperties(project, grammarFile);
        var input = grammarProperties.getCaseChangingStrategy()
            .applyTo(CharStreams.fromString(inputText, grammarFile.getPath()));
        
        var lexEngine = lexerGrammar.createLexerInterpreter(input);
        
        var syntaxErrorListener = new SyntaxErrorListener();
        lexEngine.removeErrorListeners();
        lexEngine.addErrorListener(syntaxErrorListener);
        CommonTokenStream tokens = new TokenStreamSubset(lexEngine);
        
        return parseText(grammar, lexerGrammar, startRuleName, grammarFile, syntaxErrorListener, tokens, 0);
    }
    
    
    public static ParsingResult parseText(
        Grammar grammar,
        LexerGrammar lexerGrammar,
        String startRuleName,
        final VirtualFile grammarFile,
        SyntaxErrorListener syntaxErrorListener,
        TokenStream tokens,
        int startIndex
    ) {
        if (grammar == null || lexerGrammar == null) {
            ANTLRv4PluginController.LOG.info(
                "parseText can't parse: missing lexer or parser no Grammar object for " +
                    (grammarFile != null ? grammarFile.getName() : "<unknown file>")
            );
            
            return null;
        }
        
        var grammarFileName = grammar.fileName;
        if (!new File(grammarFileName).exists()) {
            ANTLRv4PluginController.LOG.info("parseText grammar doesn't exist " + grammarFileName);
            return null;
        }
        
        if (grammar.equals(BAD_PARSER_GRAMMAR) || lexerGrammar.equals(BAD_LEXER_GRAMMAR)) {
            return null;
        }
        
        tokens.seek(startIndex);
        
        var parser = new PreviewParser(grammar, tokens);
        parser.getInterpreter().setPredictionMode(PredictionMode.LL_EXACT_AMBIG_DETECTION);
        parser.setProfile(true);
        
        parser.removeErrorListeners();
        parser.addErrorListener(syntaxErrorListener);
        
        var start = grammar.getRule(startRuleName);
        if (start == null) {
            return null; // can't find start rule
        }
        
        ParseTree t = parser.parse(start.index);
        
        if (t != null) {
            return new ParsingResult(parser, t, syntaxErrorListener);
        }
        
        return null;
    }
    
    
    public static Tool createANTLRToolForLoadingGrammars(ANTLRv4GrammarProperties grammarProperties) {
        var antlr = new Tool();
        
        antlr.errMgr = new FilterErrorManager(antlr);
        antlr.errMgr.setFormat("gnu");
        
        var listener = new LoadGrammarsToolListener(antlr);
        
        antlr.removeListeners();
        antlr.addListener(listener);
        antlr.libDirectory = grammarProperties.getLibDir();
        
        return antlr;
    }
    
    
    /**
     * Get lexer and parser grammars
     */
    public static Grammar[] loadGrammars(VirtualFile grammarFile, Project project) {
        var antlRv4PluginController = ANTLRv4PluginController.getInstance(project);
        
        if (antlRv4PluginController == null) {
            return null;
        }
        
        ANTLRv4PluginController.LOG.info("loadGrammars(fileName=" + grammarFile.getName() + ", project=" + project.getName());
        
        antlRv4PluginController.printToConsole(
            "loading grammar: file=" + grammarFile.getName() +
                " project=" + project.getName() +
                " name=" + grammarFile.getPresentableName(),
            ConsoleViewContentType.LOG_DEBUG_OUTPUT
        );
        
        var antlr = createANTLRToolForLoadingGrammars(getGrammarProperties(project, grammarFile));
        var listener = (LoadGrammarsToolListener) antlr.getListeners().get(0);
        
        var grammar = loadGrammar(grammarFile, antlr);
        
        if (grammar == null) {
            reportBadGrammar(grammarFile, antlRv4PluginController);
            return null;
        }
        
        // see if a lexer is hanging around somewhere; don't want implicit token defs to make us bail
        LexerGrammar lexerGrammar = null;
        if (grammar.getType() == ANTLRParser.PARSER) {
            lexerGrammar = loadLexerGrammarFor(grammar, project);
            
            if (lexerGrammar != null) {
                ANTLRv4PluginController.printToConsole(project, "ParsingUtils.loadGrammars.importVocab(" + lexerGrammar.name + ')', ConsoleViewContentType.LOG_DEBUG_OUTPUT);
                grammar.importVocab(lexerGrammar);
            } else {
                lexerGrammar = BAD_LEXER_GRAMMAR;
                ANTLRv4PluginController.printToConsole(project, "ParsingUtils.loadGrammars.importVocab(BAD LEXER GRAMMAR)", ConsoleViewContentType.LOG_DEBUG_OUTPUT);
            }
        }
        
        antlRv4PluginController.printToConsole("loadGrammars(" + grammarFile.getName() + ", lexerGrammar=" + (lexerGrammar != null ? lexerGrammar.name : "false") + ')', ConsoleViewContentType.LOG_DEBUG_OUTPUT);
        
        // process grammar
        antlr.process(grammar, false);
        
        if (!listener.grammarErrorMessages.isEmpty()) {
            var msg = Utils.join(listener.grammarErrorMessages.iterator(), " | ");
            antlRv4PluginController.printToConsole(msg, ConsoleViewContentType.ERROR_OUTPUT);
            try {
                String foo = new String(grammarFile.contentsToByteArray());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            ANTLRv4PluginController.printToConsole(project, "loadGrammars(" + grammar.fileName + ") abort due to errors.", ConsoleViewContentType.LOG_ERROR_OUTPUT);
            
            return null; // upon error, bail
        }
        
        // Examines the Grammar AST constructed by v3 for a v4 grammar.
        // Use ANTLR v3's ANTLRParser not ANTLRv4Parser from this plugin
        switch (grammar.getType()) {
            case ANTLRParser.PARSER:
                ANTLRv4PluginController.LOG.info("loadGrammars parser " + grammar.name);
                ANTLRv4PluginController.printToConsole(project, "loadGrammars(" + grammar.fileName + ") is a Parser Grammar", ConsoleViewContentType.LOG_DEBUG_OUTPUT);
           
                return new Grammar[]{lexerGrammar, grammar};
           
            case ANTLRParser.LEXER:
                ANTLRv4PluginController.LOG.info("loadGrammars lexer " + grammar.name);
                lexerGrammar = (LexerGrammar) grammar;
                ANTLRv4PluginController.printToConsole(project, "loadGrammars(" + grammar.fileName + ") is a Lexer Grammar", ConsoleViewContentType.LOG_DEBUG_OUTPUT);
                
                return new Grammar[]{lexerGrammar, null};
           
            case ANTLRParser.COMBINED:
                lexerGrammar = grammar.getImplicitLexer();
           
                if (lexerGrammar == null) {
                    lexerGrammar = BAD_LEXER_GRAMMAR;
                }
           
                ANTLRv4PluginController.LOG.info("loadGrammars combined: " + lexerGrammar.name + ", " + grammar.name);
                ANTLRv4PluginController.printToConsole(project, "loadGrammars(lexer=" + lexerGrammar.fileName + ", parser=" + grammar.name + ") is a Combined Grammar", ConsoleViewContentType.LOG_DEBUG_OUTPUT);
                
                return new Grammar[]{lexerGrammar, grammar};
            default:
        }
        
        ANTLRv4PluginController.LOG.info("loadGrammars invalid grammar type " + grammar.getTypeString() + " for " + grammar.fileName);
        ANTLRv4PluginController.printToConsole(project, "loadGrammars invalid grammar type " + grammar.getTypeString() + " for " + grammar.fileName, ConsoleViewContentType.ERROR_OUTPUT);
        return null;
    }
    
    
    private static void reportBadGrammar(VirtualFile grammarFile, ANTLRv4PluginController antlRv4PluginController) {
        var msg = "Empty or bad grammar found in file: " + grammarFile.getName();
        antlRv4PluginController.printToConsole(msg, ConsoleViewContentType.ERROR_OUTPUT);
    }
    
    
    @Nullable
    private static Grammar loadGrammar(VirtualFile grammarFile, Tool antlr) {
        // basically here I am mimicking the loadGrammar() method from Tool
        // so that I can check for an empty AST coming back.
        var grammarRootAST = parseGrammar(antlr, grammarFile);
        if (grammarRootAST == null) {
            return null;
        }
        
        // Create a grammar from the AST so we can figure out what type it is
        var grammar = antlr.createGrammar(grammarRootAST);
        grammar.fileName = grammarFile.getPath();
        
        return grammar;
    }
    
    
    public static String getGrammarText(VirtualFile grammarFile) throws IOException {
        var document = FileDocumentManager.getInstance().getDocument(grammarFile);
        
        return document != null ?
            document.getText() :
            new String(grammarFile.contentsToByteArray());
    }
    
    
    public static GrammarRootAST parseGrammar(Tool antlr, VirtualFile grammarFile) {
        try {
            var in = new ANTLRStringStream(getGrammarText(grammarFile));
            in.name = grammarFile.getPath();
            
            return antlr.parse(grammarFile.getPath(), in);
        } catch (IOException ioe) {
            antlr.errMgr.toolError(ErrorType.CANNOT_OPEN_FILE, ioe, grammarFile);
        }
        
        return null;
    }
    
    
    /**
     * Try to load a LexerGrammar given a parser grammar g. Derive lexer name
     * as:
     * V given tokenVocab=V in grammar or
     * XLexer given XParser.g4 filename or
     * XLexer given grammar name X
     */
    public static LexerGrammar loadLexerGrammarFor(Grammar grammar, Project project) {
        ANTLRv4PluginController.printToConsole(project, "loadLexerGrammarFor(" + grammar.name + ')', ConsoleViewContentType.LOG_DEBUG_OUTPUT);
        
        var antlr = createANTLRToolForLoadingGrammars(getGrammarProperties(project, grammar.fileName));
        var listener = (LoadGrammarsToolListener) antlr.getListeners().get(0);
        VirtualFile lexerGrammarFile;
        var vocabName = grammar.getOptionString("tokenVocab");
        
        if (vocabName != null) {
            var grammarFile = LocalFileSystem.getInstance().findFileByIoFile(new File(grammar.fileName));
            var lexerFileName = vocabName + '.' + ANTLRv4FileType.INSTANCE.getDefaultExtension();
            
            lexerGrammarFile = VfsUtil.findRelativeFile(grammarFile == null ? null : grammarFile.getParent(), lexerFileName);
            ANTLRv4PluginController.printToConsole(
                project,
                "loadLexerGrammarFor(" + grammar.name + ", lexerGrammarFile=" + (lexerGrammarFile != null ? lexerGrammarFile.getName() : "null") + ", vocabName=" + vocabName + ')',
                ConsoleViewContentType.LOG_DEBUG_OUTPUT
            );
        } else {
            lexerGrammarFile = LocalFileSystem.getInstance().findFileByIoFile(new File(getLexerNameFromParserFileName(grammar.fileName)));
            ANTLRv4PluginController.printToConsole(project, "loadLexerGrammarFor(" + grammar.name + ", lexerGrammarFile=" + (lexerGrammarFile != null ? lexerGrammarFile.getName() : "null") + ')', ConsoleViewContentType.LOG_DEBUG_OUTPUT);
        }
        
        
        LexerGrammar lexerGrammar = null;
        if (lexerGrammarFile != null && lexerGrammarFile.exists()) {
            
            try {
                lexerGrammar = (LexerGrammar) loadGrammar(lexerGrammarFile, antlr);
                
                if (lexerGrammar != null) {
                    antlr.process(lexerGrammar, false);
                } else {
                    reportBadGrammar(lexerGrammarFile, ANTLRv4PluginController.getInstance(project));
                }
            } catch (ClassCastException cce) {
                ANTLRv4PluginController.LOG.error("File: " + lexerGrammarFile + " seems not to be  a lexer grammar!", cce);
                ANTLRv4PluginController.printToConsole(project, "File: " + lexerGrammarFile + " seems not to be  a lexer grammar!", ConsoleViewContentType.ERROR_OUTPUT);
            } catch (Exception e) {
                String msg = null;
                
                if (!listener.grammarErrorMessages.isEmpty()) {
                    msg = ": " + listener.grammarErrorMessages;
                }
                
                ANTLRv4PluginController.LOG.error("File: " + lexerGrammarFile + " could not parsed as a lexer grammar!" + msg, e);
                ANTLRv4PluginController.printToConsole(project, "File: " + lexerGrammarFile + " could not parsed as a lexer grammar! Message: " + msg, ConsoleViewContentType.ERROR_OUTPUT);
            }
            
            if (!listener.grammarErrorMessages.isEmpty()) {
                lexerGrammar = null;
                var msg = Utils.join(listener.grammarErrorMessages.iterator(), " | ");
                ANTLRv4PluginController.printToConsole(project, msg, ConsoleViewContentType.ERROR_OUTPUT);
            }
        }
        
        if (lexerGrammarFile != null && lexerGrammarFile.exists()) {
            ANTLRv4PluginController.printToConsole(project, "loadLexerGrammarFor(" + grammar.name + ", lexerGrammar=" + lexerGrammar + ") SUCCEEDED", ConsoleViewContentType.LOG_DEBUG_OUTPUT);
        }
        
        return lexerGrammar;
    }
    
    
    @NotNull
    public static String getLexerNameFromParserFileName(String parserFileName) {
        var i = parserFileName.indexOf("Parser.g4");
        
        if (i >= 0) { // is filename XParser.g4?
            return parserFileName.substring(0, i) + "Lexer.g4";
        } // if not, try using the grammar name, XLexer.g4
        
        var f = new File(parserFileName);
        var fname = f.getName();
        var dot = fname.lastIndexOf(".g4");
        var parserName = fname.substring(0, dot);
        var parentDir = f.getParentFile();
        
        return new File(parentDir, parserName + "Lexer.g4").getAbsolutePath();
    }
    
    
    public static Tree findOverriddenDecisionRoot(Tree ctx) {
        return Trees.findNodeSuchThat(
            ctx,
            t -> t instanceof PreviewInterpreterRuleContext && ((PreviewInterpreterRuleContext) t).isDecisionOverrideRoot()
        );
    }
    
    
    public static List<TerminalNode> getAllLeaves(Tree t) {
        List<TerminalNode> leaves = new ArrayList<>();
        _getAllLeaves(t, leaves);
        
        return leaves;
    }
    
    
    private static void _getAllLeaves(Tree t, List<TerminalNode> leaves) {
        var n = t.getChildCount();
        if (t instanceof TerminalNode) {
            var tok = ((TerminalNode) t).getSymbol();
            
            if (tok.getType() != Token.INVALID_TYPE) {
                leaves.add((TerminalNode) t);
            }
            
            return;
        }
        
        for (var i = 0; i < n; i++) {
            _getAllLeaves(t.getChild(i), leaves);
        }
    }
    
    
    /**
     * Get ancestors where the first element of the list is the parent of t
     */
    public static List<? extends Tree> getAncestors(Tree t) {
        if (t.getParent() == null) {
            return Collections.emptyList();
        }
        
        t = t.getParent();
        List<Tree> ancestors = new ArrayList<>();
        
        while (t != null) {
            ancestors.add(t); // insert at start
            t = t.getParent();
        }
        
        return ancestors;
    }
}
