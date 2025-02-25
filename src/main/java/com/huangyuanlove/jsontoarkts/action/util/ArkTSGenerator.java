package com.huangyuanlove.jsontoarkts.action.util;

import com.google.gson.JsonElement;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;

public class ArkTSGenerator {

    public void  generateFromJsonByDocument(JsonElement json, AnActionEvent event, String rootName,boolean withTrace,DefaultProp defaultProp) {
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        if(editor != null){
            Document document = editor.getDocument();
            Project project = event.getProject();
            WriteCommandAction.runWriteCommandAction(project, () -> {
                JsonToClass jsonToClass = new JsonToClass(withTrace,defaultProp);
                jsonToClass.visitRoot(json,rootName);
                String result = jsonToClass.toCode();

                SelectionModel selectionModel = editor.getSelectionModel();
                CaretModel caretModel = editor.getCaretModel();
                int offset = 0;
                if (selectionModel.hasSelection()) {
                    offset = selectionModel.getSelectionEnd();
                }else{
                    offset = caretModel.getOffset();
                }
                document.insertString(offset,result);


            });
        }
    }






}
