package com.huangyuanlove.jsontoarkts.action.ui;

import com.intellij.lang.Language;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.ui.LanguageTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UserInputEditor extends LanguageTextField {

    public UserInputEditor(Language language, @Nullable Project project, @NotNull String value) {
        super(language, project, value);
    }

    @Override
    protected @NotNull EditorEx createEditor() {
        EditorEx editorEx = super.createEditor();

        editorEx.setVerticalScrollbarVisible(true);
        editorEx.setHorizontalScrollbarVisible(true);
        editorEx.setPlaceholder("Enter JSON");
        EditorSettings settings = editorEx.getSettings();
        settings.setLineNumbersShown(true);
        settings.setAllowSingleLogicalLineFolding(true);
        settings.setAutoCodeFoldingEnabled(true);
        settings.setFoldingOutlineShown(true);
        settings.setRightMarginShown(true);
        return editorEx;
    }
}