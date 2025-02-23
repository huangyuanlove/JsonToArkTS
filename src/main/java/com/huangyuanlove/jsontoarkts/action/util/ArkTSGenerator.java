package com.huangyuanlove.jsontoarkts.action.util;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;

public class ArkTSGenerator {

    public void  generateFromJsonByDocument(String json, AnActionEvent event, String rootName) {
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        if(editor != null){
            Document document = editor.getDocument();
            Project project = event.getProject();
            WriteCommandAction.runWriteCommandAction(project, () -> {

            });
        }
    }
}
