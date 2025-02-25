package com.huangyuanlove.jsontoarkts.action;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.huangyuanlove.jsontoarkts.action.ui.UserInputEditor;
import com.huangyuanlove.jsontoarkts.action.util.ArkTSGenerator;
import com.huangyuanlove.jsontoarkts.action.util.DefaultProp;
import com.huangyuanlove.jsontoarkts.action.util.NotificationUtil;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.json.JsonLanguage;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class CodeGenerateAction extends AnAction {

    boolean withTrace = true;
    DefaultProp defaultProp = DefaultProp.nullable;
    Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
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



        FileEditor fileEditor = FileEditorManager.getInstance(project).getSelectedEditor();
        if (fileEditor != null) {
            //获取当前所在文件的名字作为默认的类名
            String fileName = null;
            fileName = fileEditor.getFile().getName();
            fileName = fileName.substring(0, fileName.lastIndexOf("."));

            //弹窗主体
            JFrame mDialog = new JFrame();
            mDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            mDialog.setTitle("JsonToArkTS");

            //输入框容器，使得输入框可滑动
            JPanel mainPanel = new JPanel(new BorderLayout());
            UserInputEditor userInputEditor = new UserInputEditor(JsonLanguage.INSTANCE, project, "");
            userInputEditor.setPreferredSize(new Dimension(userInputEditor.getWidth(), 375)); //
            JBScrollPane scrollPane = new JBScrollPane(userInputEditor);
            mainPanel.add(scrollPane, BorderLayout.CENTER);

            mDialog.add(mainPanel, BorderLayout.CENTER);


            //底部功能区主体
            JPanel bottomPanel = new JPanel();
            bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
            //类名输入框
            JPanel classNamePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
            JLabel classNameLabel = new JLabel("class name");
            JTextField classNameField = new JTextField(20);
            classNameField.setText(fileName);
            classNamePanel.add(classNameLabel);
            classNamePanel.add(classNameField);

            bottomPanel.add(classNamePanel);


            //生成类的配置选项
            JPanel optionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
            //是否对属性添加@Trace注解
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


            //属性是否可空
            JRadioButton nullable = new JRadioButton("with nullable");
            nullable.setSelected(true);
            nullable.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if(e.getStateChange() == ItemEvent.SELECTED){
                        defaultProp = DefaultProp.nullable;
                    }
                }
            });

            //属性是否有默认值
            JRadioButton defaultValue = new JRadioButton("with default value");
            defaultValue.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if(e.getStateChange() == ItemEvent.SELECTED){
                        defaultProp = DefaultProp.defaultValue;
                    }
                }
            });
            ButtonGroup propValueGroup = new ButtonGroup();
            propValueGroup.add(nullable);
            propValueGroup.add(defaultValue);

            optionPanel.add(nullable);
            optionPanel.add(defaultValue);
            bottomPanel.add(optionPanel);




            //生成按钮
            JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
            JButton generateButton = new JButton("Generate");
            String finalFileName = fileName;

            generateButton.addActionListener(e -> {

                try {
                    //检查一下Json字符串是否合法
                    String inputJsonString = userInputEditor.getText();
                    JsonElement root = JsonParser.parseString(inputJsonString);
                    if(root!=null){

                        new ArkTSGenerator().generateFromJsonByDocument(root, event, finalFileName,withTrace,defaultProp);

                        NotificationUtil.showInfoNotification(project, "done");
                        mDialog.setVisible(false);
                        if (event.getProject() != null) {
                            ProjectView.getInstance(event.getProject()).refresh();
                        }
                        VirtualFile data = event.getData(LangDataKeys.VIRTUAL_FILE);
                        if (data != null) {
                            data.refresh(false, true);
                        }

                    }

                }catch (Exception e1) {
                    NotificationUtil.showErrorNotification(project, e1.getMessage());
                }

            });

            //格式化按钮,将输入的Json字符串格式化
            JButton formatButton = new JButton("Format");
            formatButton.addActionListener(e -> {

                try {
                    String inputJsonString = userInputEditor.getText();
                    JsonElement root = JsonParser.parseString(inputJsonString);
                    String result = gson.toJson(root);
                    userInputEditor.setText(result);

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
