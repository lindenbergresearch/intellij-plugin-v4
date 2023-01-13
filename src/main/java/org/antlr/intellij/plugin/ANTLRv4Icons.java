package org.antlr.intellij.plugin;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public class ANTLRv4Icons {
    public static final Icon FILE =
        IconLoader.getIcon("/icons/org/antlr/intellij/plugin/antlr.svg", ANTLRv4Icons.class);
    
    public static final Icon LEXER_RULE =
        IconLoader.getIcon("/icons/org/antlr/intellij/plugin/lexer-rule.svg", ANTLRv4Icons.class);
    
    public static final Icon PARSER_RULE =
        IconLoader.getIcon("/icons/org/antlr/intellij/plugin/parser-rule.svg", ANTLRv4Icons.class);
    
    public static final Icon MODE =
        IconLoader.getIcon("/icons/org/antlr/intellij/plugin/mode.svg", ANTLRv4Icons.class);
    
    
    public static Icon getToolWindow() {
        return IconLoader.getIcon("/icons/org/antlr/intellij/plugin/toolWindowAntlr.svg", ANTLRv4Icons.class);
    }
}
