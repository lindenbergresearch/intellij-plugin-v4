package org.antlr.intellij.plugin.psi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class PsiTreeMatcher<T, R> {
    private final List<Function<T, Boolean>> predicates;
    private R attribute = null;
    
    /*|--------------------------------------------------------------------------|*/
    
    
    public PsiTreeMatcher() {
        predicates = new ArrayList<>();
    }
    
    
    public PsiTreeMatcher(R attribute) {
        this.attribute = attribute;
        predicates = new ArrayList<>();
    }
    
    
    public PsiTreeMatcher(List<Function<T, Boolean>> predicates, R attribute) {
        this.predicates = predicates;
        this.attribute = attribute;
    }
    
    
    /*|--------------------------------------------------------------------------|*/
    
    
    public boolean hasAssignedAttribute() {
        return attribute != null;
    }
    
    
    public void assignAttribute(R attribute) {
        this.attribute = attribute;
    }
    
    
    public R getAttribute() {
        return attribute;
    }
    
    
    public void addPremise(Function<T, Boolean> premise) {
        predicates.add(premise);
    }
    
    @SafeVarargs
    public final void addPremise(Function<T, Boolean>... premise) {
        Collections.addAll(predicates, premise);
    }
    
    public PsiTreeMatcher<T, R> and(Function<T, Boolean> premise) {
        predicates.add(premise);
        return this;
    }
    
    public boolean matches(T specimen) {
        for (var premise : predicates) {
            if (!premise.apply(specimen))
                return false;
        }
        
        return true;
    }
}
