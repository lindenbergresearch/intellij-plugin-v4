package org.antlr.intellij.plugin.misc;

/**
 * Simple 2-Tuple class.
 *
 * @param <A> Type for first element.
 * @param <B> Type for second element.
 */
class Tuple2<A, B> {
    public final A a;
    public final B b;
    
    
    public Tuple2(A a, B b) {
        this.a = a;
        this.b = b;
    }
    
    
    public A first() {
        return a;
    }
    
    
    public B second() {
        return b;
    }
    
    
    @Override
    public String toString() {
        return "Tuple2[" + a + ',' + b + ']';
    }
    
    
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        
        if (!(other instanceof Tuple2)) {
            return false;
        }
        
        var otherTuple2 = (Tuple2<A, B>) other;
        
        if (otherTuple2.a == null || otherTuple2.b == null)
            return false;
        
        return otherTuple2.a.equals(this.a) && otherTuple2.b.equals(this.b);
    }
    
    
    @Override
    public int hashCode() {
        final var prime = 31;
        var result = 1;
        result = prime * result + ((a == null) ? 0 : a.hashCode());
        result = prime * result + ((b == null) ? 0 : b.hashCode());
        return result;
    }
}
