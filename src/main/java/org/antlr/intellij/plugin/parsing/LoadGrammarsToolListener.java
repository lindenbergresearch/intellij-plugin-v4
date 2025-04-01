package org.antlr.intellij.plugin.parsing;

import org.antlr.v4.Tool;
import org.antlr.v4.tool.ANTLRMessage;
import org.antlr.v4.tool.DefaultToolListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Track errors, warnings from loading grammars. Really just
 * swallows them for now. The external annotator shows errors.
 */
public class LoadGrammarsToolListener extends DefaultToolListener {
    public List<String> grammarErrorMessages = new ArrayList<>();
    public List<String> grammarWarningMessages = new ArrayList<>();
    public List<String> grammarInfoMessages = new ArrayList<>();
    
    
    public LoadGrammarsToolListener(Tool tool) {
        super(tool);
    }
    
    
    @Override
    public void error(ANTLRMessage msg) {
        var msgST = tool.errMgr.getMessageTemplate(msg);
        var s = msgST.render();
        
        System.out.println(msg.toString());
        
//        if (tool.errMgr.formatWantsSingleLineMessage()) {
//            s = s.replace(System.lineSeparator(), " ");
//        }
        
        grammarErrorMessages.add(s);
    }
    
    
    @Override
    public void warning(ANTLRMessage msg) {
        var msgST = tool.errMgr.getMessageTemplate(msg);
        var s = msgST.render();
        
//        if (tool.errMgr.formatWantsSingleLineMessage()) {
//            s = s.replace(System.lineSeparator(), " ");
//        }
        
        grammarWarningMessages.add(s);
    }
    
    
    @Override
    public void info(String msg) {
        grammarInfoMessages.add(msg);
    }
    
    
    public void clear() {
        grammarErrorMessages.clear();
        grammarWarningMessages.clear();
        grammarInfoMessages.clear();
    }
}
