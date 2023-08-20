package de.vette.idea.neos.lang.xliff;

import com.intellij.DynamicBundle;
import de.vette.idea.neos.lang.fusion.FusionBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public final class XliffBundle  extends DynamicBundle {
    public static final String BUNDLE = "messages.XliffBundle";

    private static final XliffBundle INSTANCE = new XliffBundle();


    protected XliffBundle() {
        super(BUNDLE);
    }

    @NotNull
    public static @Nls String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object @NotNull ... params) {
        return INSTANCE.getMessage(key, params);
    }
}
