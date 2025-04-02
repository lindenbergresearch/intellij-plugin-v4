package org.antlr.intellij.plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ANTLRStartupActivity implements ProjectActivity {
    
    @Override public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        ANTLRv4PluginController.getInstance(project);
        return null;
    }
}
