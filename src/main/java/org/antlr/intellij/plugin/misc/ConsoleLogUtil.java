package org.antlr.intellij.plugin.misc;

import lombok.Getter;

/**
 * Utility methods for the console log.
 */
public class ConsoleLogUtil {
    
    /**
     * StackTrace collected debug info.
     */
    @Getter
    public static class DebugInfo {
        int lineNumber;
        String methodName;
        String className;
        String shortClassName;
        String fileName;
        String timeStamp;
        String simpleTimeStamp;
        
        
        public String getMethodName(int maxLength) {
            return StringUtil.elided(methodName, maxLength);
        }
        
        
        public String getClassName(int maxLength) {
            return StringUtil.elided(className, maxLength);
        }
        
        
        public String getShortClassName(int maxLength) {
            return StringUtil.elided(shortClassName, maxLength);
        }
        
        
        public String getFileName(int maxLength) {
            return StringUtil.elided(fileName, maxLength);
        }
        
        
        public String compactInfo() {
            return getShortClassName(25) + '.' + getMethodName(25) + '(' + getLineNumber() + ')';
        }
        
        
        public String fullInfo() {
            return getFileName() + " -> " + getClassName() + '.' + getMethodName() + ':' + getLineNumber();
        }
        
        
        @Override public String toString() {
            return "DebugInfo{" +
                   "lineNumber=" + lineNumber +
                   ", methodName='" + methodName + '\'' +
                   ", className='" + className + '\'' +
                   ", shortClassName='" + shortClassName + '\'' +
                   ", fileName='" + fileName + '\'' +
                   ", timeStamp='" + timeStamp + '\'' +
                   ", simpleTimeStamp='" + simpleTimeStamp + '\'' +
                   '}';
        }
    }
    
    
    /**
     * Collects debug info based on the give stacktrace.
     *
     * @param stackTrace The StackTrace to analyze.
     * @return An instance of DebugInfo.
     */
    public static DebugInfo collectDebugInfo(StackTraceElement[] stackTrace) {
        var debugInfo = new DebugInfo();
        
        // find correct stacktrace element
        var stackTraceElement = stackTrace[2];
        
        if (stackTraceElement.getMethodName().contains("printToConsole") && stackTrace.length > 3) {
            stackTraceElement = stackTrace[3];
        }
        
        // common fields
        debugInfo.methodName = stackTraceElement.getMethodName();
        debugInfo.className = stackTraceElement.getClassName();
        debugInfo.fileName = stackTraceElement.getFileName();
        debugInfo.lineNumber = stackTraceElement.getLineNumber();
        debugInfo.simpleTimeStamp = StringUtil.getSimpleTimeStamp();
        debugInfo.timeStamp = StringUtil.getTimeStamp();
        
        // short classname
        var classElem = stackTraceElement.getClassName().split("\\.");
        debugInfo.shortClassName = classElem.length > 0 ? classElem[classElem.length - 1] : "";
        
        return debugInfo;
    }
}
