package com.huangyuanlove.jsontoarkts.action;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.huangyuanlove.jsontoarkts.action.util.ArkTSGenerator;
import com.huangyuanlove.jsontoarkts.action.util.NotificationUtil;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.BuildNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.LanguageTextField;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class CodeGenerateAction extends AnAction {

   private final GenerateConfig generateConfig = new GenerateConfig();


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

        //获取当前所在文件的名字
        String fileName = null;

        FileEditor fileEditor = FileEditorManager.getInstance(project).getSelectedEditor();
        if (fileEditor != null) {
            fileName = fileEditor.getFile().getName();
            fileName = fileName.substring(0, fileName.lastIndexOf("."));

            JFrame mDialog = new JFrame();
            mDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            mDialog.setTitle("JsonToArkTS");

            JPanel mainPanel = new JPanel(new BorderLayout());

            LanguageTextField userInputEditor = new LanguageTextField();
            userInputEditor.setEnabled(true);
            ApplicationInfo appInfo = ApplicationInfo.getInstance();
            String majorVersion = appInfo.getMajorVersion(); // 主版本号（如 "2024"）
            String minorVersion = appInfo.getMinorVersion(); // 次版本号（如 "1"）
            String buildNumber = appInfo.getBuild().asString(); // 完整构建号（如 "241.14494.240"）


            StringBuffer info = new StringBuffer(String.format("%s.%s (Build %s)", majorVersion, minorVersion, buildNumber));

            BuildNumber build = appInfo.getBuild();

            info.append("\n").append("Base Version: ").append(build.getProductCode());
            info.append("\n").append("Baseline Version: ").append(build.getBaselineVersion());



//            UserInputEditor userInputEditor = new UserInputEditor(JsonLanguage.INSTANCE, project, "");
            userInputEditor.setText(info.toString());
            userInputEditor.setOneLineMode(false);
            userInputEditor.setPreferredSize(new Dimension(userInputEditor.getWidth(), 375)); //

            JBScrollPane scrollPane = new JBScrollPane(userInputEditor);

            mainPanel.add(scrollPane, BorderLayout.CENTER);
            mDialog.add(mainPanel, BorderLayout.CENTER);


            Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();


            JPanel bottomPanel = new JPanel();
            bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));


            JPanel classNamePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
            JLabel classNameLabel = new JLabel("class name");
            JTextField classNameField = new JTextField(20);
            classNameField.setText(fileName);
            classNamePanel.add(classNameLabel);
            classNamePanel.add(classNameField);

            bottomPanel.add(classNamePanel);



            JPanel optionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));

            JCheckBox autoTraceCheckBox = new JCheckBox("with @Trace");
            generateConfig.withTrace = true;
            autoTraceCheckBox.setSelected(true);

            autoTraceCheckBox.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if(e.getStateChange() == ItemEvent.SELECTED){
                        generateConfig.withTrace = true;
                    }else if(e.getStateChange() == ItemEvent.DESELECTED){
                        generateConfig.withTrace = false;
                    }
                }
            });
            optionPanel.add(autoTraceCheckBox);




            JCheckBox defaultValue = new JCheckBox("with default value");
            generateConfig.withDefaultValue = true;
            defaultValue.setSelected(true);
            defaultValue.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if(e.getStateChange() == ItemEvent.SELECTED){
                        generateConfig.withDefaultValue = true;
                    }else if(e.getStateChange() == ItemEvent.DESELECTED){
                        generateConfig.withDefaultValue = false;
                    }
                }
            });


            JCheckBox nullable = new JCheckBox("with nullable");
            nullable.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if(e.getStateChange() == ItemEvent.SELECTED){
                        generateConfig.nullable = true;
                    }else if(e.getStateChange() == ItemEvent.DESELECTED){
                        //不可空则一定有默认值
                        generateConfig.nullable = false;
                        defaultValue.setSelected(true);

                    }

                }
            });

            JCheckBox fromJson = new JCheckBox("with fromJSON");
            fromJson.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if(e.getStateChange() == ItemEvent.SELECTED){
                        generateConfig.withFromJson = true;
                    }else if(e.getStateChange() == ItemEvent.DESELECTED){
                        //不可空则一定有默认值
                        generateConfig.withFromJson = false;
                        defaultValue.setSelected(true);

                    }

                }
            });
            fromJson.setSelected(true);
            optionPanel.add(fromJson);



            optionPanel.add(nullable);
            optionPanel.add(defaultValue);
            bottomPanel.add(optionPanel);
            bottomPanel.add(optionPanel);





            JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
            JButton generateButton = new JButton("Generate");


            generateButton.addActionListener(e -> {

                try {
                    //检查一下Json字符串是否合法
                    String inputJsonString = userInputEditor.getText();
                    JsonElement root = JsonParser.parseString(inputJsonString);
                    if(root!=null){
                        String finalFileName = classNameField.getText();

                        new ArkTSGenerator().generateFromJsonByDocument(root, event,  finalFileName,generateConfig);

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


            EditorEx editorEx =  userInputEditor.getEditor(true);
            editorEx.setVerticalScrollbarVisible(true);
            editorEx.setHorizontalScrollbarVisible(true);
            editorEx.setPlaceholder("Enter JSON");
            editorEx.setOneLineMode(false);

            EditorSettings settings = editorEx.getSettings();
            settings.setLineNumbersShown(true);
            settings.setAllowSingleLogicalLineFolding(true);
            settings.setAutoCodeFoldingEnabled(true);
            settings.setFoldingOutlineShown(true);
            settings.setRightMarginShown(true);


        }

    }


}
