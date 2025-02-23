package com.huangyuanlove.jsontoarkts.action;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.huangyuanlove.jsontoarkts.action.ui.UserInputEditor;
import com.huangyuanlove.jsontoarkts.action.util.NotificationUtil;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.json.JsonLanguage;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;

public class CodeGenerateAction extends AnAction {

    boolean withTrace = true;


    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        event.getPresentation().setEnabledAndVisible(event.getProject() != null);
    }


    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);
        if (project == null) {
            return;
        }
        DumbService dumbService = DumbService.getInstance(project);
        if (dumbService.isDumb()) {
            dumbService.showDumbModeNotification("JsonToArkTS plugin is not available during indexing");
            return;
        }

        //获取当前所在文件的名字
        String fileName = null;

        FileEditor fileEditor = FileEditorManager.getInstance(project).getSelectedEditor();
        if (fileEditor != null) {
            fileName = fileEditor.getFile().getName();
            fileName = fileName.substring(0, fileName.lastIndexOf("."));

            JFrame mDialog = new JFrame();
            mDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            mDialog.setTitle(fileName);

            JPanel mainPanel = new JPanel(new BorderLayout());
            UserInputEditor userInputEditor = new UserInputEditor(JsonLanguage.INSTANCE, project, "");
            mainPanel.add(new JScrollPane(userInputEditor), BorderLayout.CENTER);
            mDialog.add(mainPanel, BorderLayout.CENTER);


            Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
            String inputJsonString = userInputEditor.getText();



            JPanel bottomPanel = new JPanel();
            bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));


            JPanel optionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
            JLabel classNameLabel = new JLabel("class name");
            JTextField classNameField = new JTextField(20);
            classNameField.setText(fileName);
            optionPanel.add(classNameLabel);
            optionPanel.add(classNameField);


            JCheckBox autoTraceCheckBox = new JCheckBox("with @Trace");
            autoTraceCheckBox.setSelected(true);

            autoTraceCheckBox.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if(e.getStateChange() == ItemEvent.SELECTED){
                        withTrace = true;
                    }else if(e.getStateChange() == ItemEvent.DESELECTED){
                        withTrace = false;
                    }
                }
            });
            optionPanel.add(autoTraceCheckBox);

            bottomPanel.add(optionPanel);






            JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
            JButton generateButton = new JButton("Generate");
            generateButton.addActionListener(e -> {

                try {
                    //检查一下Json字符串是否合法
                    JsonElement root = JsonParser.parseString(inputJsonString);
                    if(root!=null){
                        ProgressManager.getInstance().run(new Task.Backgroundable(project, "JsonToArkTS plugin", false) {
                            @Override
                            public void run(@NotNull ProgressIndicator indicator) {
                                try {
                                    
                                    new ArkTSGenerator().generateFromJsonByDocument(json, event, finalRootName, parseType, generateSetGet);
                                   
                                    NotificationUtil.showErrorNotification(project, "done");
                                } catch (Exception e) {
                                    if (e instanceof IOException) {
                                        messageDelegate.catchException(new FileIOException(), event);
                                    } else {
                                        messageDelegate.catchException(e, event);
                                    }
                                } finally {
                                    indicator.stop();
                                    if (event.getProject() != null) {
                                        ProjectView.getInstance(event.getProject()).refresh();
                                    }
                                    VirtualFile data = event.getData(LangDataKeys.VIRTUAL_FILE);
                                    if (data != null) {
                                        data.refresh(false, true);
                                    }
                                }
                            }
                        });
                    }

                }catch (Exception e1) {
                    NotificationUtil.showErrorNotification(project, e1.getMessage());
                }

            });

            JButton formatButton = new JButton("Format");
            formatButton.addActionListener(e -> {

                try {
                    JsonElement root = JsonParser.parseString(inputJsonString);
                    userInputEditor.setText(gson.toJson(root));

                }catch (Exception e1) {
                    NotificationUtil.showErrorNotification(project, e1.getMessage());
                }
            });

            actionPanel.add(generateButton);
            actionPanel.add(formatButton);

            bottomPanel.add(actionPanel);


            mDialog.add(bottomPanel, BorderLayout.SOUTH);

            mDialog.pack();
            mDialog.setLocationRelativeTo(null);
            mDialog.setVisible(true);


        }

    }


}
