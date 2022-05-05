package org.antlr.intellij.plugin.formatter;

import com.intellij.formatting.*;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.antlr.intellij.plugin.ANTLRv4Language;
import org.antlr.intellij.plugin.ANTLRv4TokenTypes;
import org.jetbrains.annotations.NotNull;

public class SimpleFormattingModelBuilder implements FormattingModelBuilder {
    
    private static SpacingBuilder createSpaceBuilder(CodeStyleSettings settings) {
        return new SpacingBuilder(settings, ANTLRv4Language.INSTANCE)
            .around(ANTLRv4TokenTypes.OPERATOR)
            .spaceIf(settings.getCommonSettings(ANTLRv4Language.INSTANCE.getID()).SPACE_AROUND_ASSIGNMENT_OPERATORS);
//            .before(ANTLRv4TokenTypes.)
//            .none();
    }
    
    
    @Override
    public @NotNull FormattingModel createModel(@NotNull FormattingContext formattingContext) {
        final CodeStyleSettings codeStyleSettings = formattingContext.getCodeStyleSettings();
        
        return
            FormattingModelProvider
                .createFormattingModelForPsiFile(
                    formattingContext.getContainingFile(),
                    new SimpleBlock(
                        formattingContext.getNode(),
                        Wrap.createWrap(WrapType.NONE, false),
                        Alignment.createAlignment(),
                        createSpaceBuilder(codeStyleSettings)
                    ),
                    codeStyleSettings
                );
    }
    
}
