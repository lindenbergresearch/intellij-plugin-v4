package org.antlr.intellij.plugin.parsing;

import com.intellij.execution.ui.ConsoleViewContentType;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.v4.Tool;
import org.antlr.v4.tool.ANTLRMessage;
import org.antlr.v4.tool.ANTLRToolListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to track errors during antlr run on a grammar for generation,
 * not for annotation of grammar.
 */
public class RunANTLRListener implements ANTLRToolListener {
    public final List<String> all = new ArrayList<>();
    public Tool tool;
    public ANTLRv4PluginController antlRv4PluginController;
    public boolean hasOutput = false;
    
    
    public RunANTLRListener(Tool tool, ANTLRv4PluginController antlRv4PluginController) {
        this.tool = tool;
        this.antlRv4PluginController = antlRv4PluginController;
    }
    
    
    @Override
    public void info(String msg) {
        if (msg == null || msg.isEmpty()) {
            return;
        }
        
        if (tool.errMgr.formatWantsSingleLineMessage()) {
            msg = msg.replace('\n', ' ');
        }
        
        antlRv4PluginController.printToConsole(msg, ConsoleViewContentType.NORMAL_OUTPUT);
        hasOutput = true;
    }
    
    
    @Override
    public void error(ANTLRMessage msg) {
        track(msg, ConsoleViewContentType.LOG_ERROR_OUTPUT);
    }
    
    
    @Override
    public void warning(ANTLRMessage msg) {
        track(msg, ConsoleViewContentType.LOG_WARNING_OUTPUT);
    }
    
    
    private void track(ANTLRMessage msg, ConsoleViewContentType errType) {
        var msgST = tool.errMgr.getMessageTemplate(msg);
        var outputMsg = msgST.render();
        
        if (tool.errMgr.formatWantsSingleLineMessage()) {
            outputMsg = outputMsg.replace('\n', ' ');
        }
        
        antlRv4PluginController.printToConsole(outputMsg, errType);
        hasOutput = true;
    }
}
