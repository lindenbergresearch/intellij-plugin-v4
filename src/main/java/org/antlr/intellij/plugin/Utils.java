package org.antlr.intellij.plugin;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Utils {
    
    
    /**
     * Returns the longest string in an array of strings.
     *
     * @param s Input strings as vararg, array, list.
     * @return Longest string, null if empty.
     */
    public static String getLongestString(String... s) {
        if (s == null)
            return null;
        
        if (s.length == 1)
            return s[0];
        
        return Arrays.
            stream(s).
            sorted().
            collect(Collectors.toList()).
            get(s.length);
        
    }
    
}
