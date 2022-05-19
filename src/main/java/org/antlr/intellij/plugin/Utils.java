package org.antlr.intellij.plugin;

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
        
        String longest = s[0];
        
        for (String str : s) {
            if (str.length() > longest.length())
                longest = str;
        }
        
        return longest;
    }
    
}
