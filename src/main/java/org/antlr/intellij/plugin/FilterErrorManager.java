package org.antlr.intellij.plugin;

import org.antlr.v4.Tool;
import org.antlr.v4.tool.ANTLRMessage;
import org.antlr.v4.tool.ErrorManager;
import org.antlr.v4.tool.ErrorType;

import java.util.ArrayList;

/**
 * An ErrorManager with the ability to filter (ignore) several ErrorTypes.
 */
public class FilterErrorManager extends ErrorManager {
    
    /**
     * Holds all the excluded ErrorTypes.
     */
    final static ArrayList<ErrorType> excludedErrors = new ArrayList<>();
    
    
    public FilterErrorManager(Tool tool) {
        super(tool);
        
        excludedErrors.add(ErrorType.CANNOT_FIND_TOKENS_FILE_REFD_IN_GRAMMAR);
        excludedErrors.add(ErrorType.CANNOT_FIND_TOKENS_FILE_GIVEN_ON_CMDLINE);
    }
    
    
    @Override
    public void emit(ErrorType errorType, ANTLRMessage msg) {
        // check for excluded
        if (excludedErrors.contains(errorType)) {
            return; // ignore these
        }
        
        // forward error
        super.emit(errorType, msg);
    }
}
