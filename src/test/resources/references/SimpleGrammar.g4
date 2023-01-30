grammar SimpleGrammar;

TOKEN1: '$' DIGIT+ '$';

DIGIT: [0-9]+;

rule1: TOKEN1;

rule2: DIG< caret >IT rule1 DIGIT;
