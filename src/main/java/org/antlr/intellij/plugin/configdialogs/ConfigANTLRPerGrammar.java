package org.antlr.intellij.plugin.configdialogs;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.antlr.intellij.plugin.parsing.CaseChangingStrategy;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

import static org.antlr.intellij.plugin.configdialogs.ANTLRv4GrammarPropertiesStore.getGrammarProperties;
import static org.antlr.intellij.plugin.configdialogs.ANTLRv4GrammarPropertiesStore.getOrCreateGrammarProperties;

/**
 * The UI that allows viewing/modifying grammar settings for a given grammar file.
 *
 * @see ANTLRv4ProjectSettings
 */
public class ConfigANTLRPerGrammar extends DialogWrapper {
    protected JCheckBox autoGenerateParsersCheckBox;
    protected JTextField languageField;
    private JPanel dialogContents;
    private JCheckBox generateParseTreeListenerCheckBox;
    private JCheckBox generateParseTreeVisitorCheckBox;
    private JTextField packageField;
    private TextFieldWithBrowseButton outputDirField;
    private TextFieldWithBrowseButton libDirField;
    private JTextField fileEncodingField;
    private JComboBox<CaseChangingStrategy> caseTransformation;
    
    
    private ConfigANTLRPerGrammar(final Project project) {
        super(project, false);
    }
    
    
    public static ConfigANTLRPerGrammar getDialogForm(final Project project, String qualFileName) {
        ConfigANTLRPerGrammar grammarFrom = new ConfigANTLRPerGrammar(project);
        grammarFrom.init();
        grammarFrom.initAntlrFields(project, qualFileName);
        return grammarFrom;
    }
    
    
    public static ConfigANTLRPerGrammar getProjectSettingsForm(final Project project, String qualFileName) {
        ConfigANTLRPerGrammar grammarFrom = new ConfigANTLRPerGrammar(project);
        grammarFrom.initAntlrFields(project, qualFileName);
        grammarFrom.generateParseTreeListenerCheckBox.setVisible(false);
        grammarFrom.generateParseTreeVisitorCheckBox.setVisible(false);
        grammarFrom.autoGenerateParsersCheckBox.setVisible(false);
        return grammarFrom;
    }
    
    
    private void initAntlrFields(Project project, String qualFileName) {
        FileChooserDescriptor dirChooser =
            FileChooserDescriptorFactory.createSingleFolderDescriptor();
        outputDirField.addBrowseFolderListener("Select output dir", null, project, dirChooser);
        outputDirField.setTextFieldPreferredWidth(50);
        
        dirChooser =
            FileChooserDescriptorFactory.createSingleFolderDescriptor();
        libDirField.addBrowseFolderListener("Select lib dir", null, project, dirChooser);
        libDirField.setTextFieldPreferredWidth(50);
        
        loadValues(project, qualFileName);
    }
    
    
    public void loadValues(Project project, String qualFileName) {
        ANTLRv4GrammarProperties grammarProperties = getGrammarProperties(project, qualFileName);
        
        autoGenerateParsersCheckBox.setSelected(grammarProperties.shouldAutoGenerateParser());
        outputDirField.setText(grammarProperties.getOutputDir());
        libDirField.setText(grammarProperties.getLibDir());
        fileEncodingField.setText(grammarProperties.getEncoding());
        packageField.setText(grammarProperties.getPackage());
        languageField.setText(grammarProperties.getLanguage());
        caseTransformation.setSelectedItem(grammarProperties.getCaseChangingStrategy());
        generateParseTreeListenerCheckBox.setSelected(grammarProperties.shouldGenerateParseTreeListener());
        generateParseTreeVisitorCheckBox.setSelected(grammarProperties.shouldGenerateParseTreeVisitor());
    }
    
    
    public void saveValues(Project project, String qualFileName) {
        ANTLRv4GrammarProperties grammarProperties = getOrCreateGrammarProperties(project, qualFileName);
        
        grammarProperties.autoGen = autoGenerateParsersCheckBox.isSelected();
        grammarProperties.outputDir = getOutputDirText();
        grammarProperties.libDir = getLibDirText();
        grammarProperties.encoding = getFileEncodingText();
        grammarProperties.pkg = getPackageFieldText();
        grammarProperties.language = getLanguageText();
        grammarProperties.caseChangingStrategy = getCaseChangingStrategy();
        grammarProperties.generateListener = generateParseTreeListenerCheckBox.isSelected();
        grammarProperties.generateVisitor = generateParseTreeVisitorCheckBox.isSelected();
    }
    
    
    boolean isModified(ANTLRv4GrammarProperties originalProperties) {
        return !Objects.equals(originalProperties.getOutputDir(), getOutputDirText())
            || !Objects.equals(originalProperties.getLibDir(), getLibDirText())
            || !Objects.equals(originalProperties.getEncoding(), getFileEncodingText())
            || !Objects.equals(originalProperties.getPackage(), getPackageFieldText())
            || !Objects.equals(originalProperties.getLanguage(), getLanguageText())
            || !Objects.equals(originalProperties.caseChangingStrategy, getCaseChangingStrategy());
    }
    
    
    String getLanguageText() {
        return languageField.getText();
    }
    
    
    String getPackageFieldText() {
        return packageField.getText();
    }
    
    
    String getFileEncodingText() {
        return fileEncodingField.getText();
    }
    
    
    String getLibDirText() {
        return libDirField.getText();
    }
    
    
    String getOutputDirText() {
        return outputDirField.getText();
    }
    
    
    private CaseChangingStrategy getCaseChangingStrategy() {
        return (CaseChangingStrategy) caseTransformation.getSelectedItem();
    }
    
    
    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return dialogContents;
    }
    
    
    @Override
    public String toString() {
        return "ConfigANTLRPerGrammar{" +
            " generateParseTreeListenerCheckBox=" + generateParseTreeListenerCheckBox +
            ", generateParseTreeVisitorCheckBox=" + generateParseTreeVisitorCheckBox +
            ", packageField=" + packageField +
            ", outputDirField=" + outputDirField +
            ", libDirField=" + libDirField +
            '}';
    }
    
    
    private void createUIComponents() {
        caseTransformation = new ComboBox<>(CaseChangingStrategy.values());
    }
    
    
    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }
    
    
    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        dialogContents = new JPanel();
        dialogContents.setLayout(new GridLayoutManager(10, 2, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("Location of imported grammars");
        dialogContents.add(label1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        final JLabel label2 = new JLabel();
        label2.setText("Grammar file encoding; e.g., euc-jp");
        dialogContents.add(label2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        fileEncodingField = new JTextField();
        dialogContents.add(fileEncodingField, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        generateParseTreeVisitorCheckBox = new JCheckBox();
        generateParseTreeVisitorCheckBox.setText("generate parse tree visitor");
        dialogContents.add(generateParseTreeVisitorCheckBox, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        generateParseTreeListenerCheckBox = new JCheckBox();
        generateParseTreeListenerCheckBox.setSelected(true);
        generateParseTreeListenerCheckBox.setText("generate parse tree listener (default)");
        dialogContents.add(generateParseTreeListenerCheckBox, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Package/namespace for the generated code");
        dialogContents.add(label3, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        packageField = new JTextField();
        dialogContents.add(packageField, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Output directory where all output is generated");
        dialogContents.add(label4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        outputDirField = new TextFieldWithBrowseButton();
        dialogContents.add(outputDirField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        libDirField = new TextFieldWithBrowseButton();
        dialogContents.add(libDirField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        autoGenerateParsersCheckBox = new JCheckBox();
        autoGenerateParsersCheckBox.setText("Auto-generate parsers upon save");
        dialogContents.add(autoGenerateParsersCheckBox, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Language (e.g., Java, Python2, CSharp, ...)");
        dialogContents.add(label5, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        languageField = new JTextField();
        dialogContents.add(languageField, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final Spacer spacer1 = new Spacer();
        dialogContents.add(spacer1, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Case transformation in the Preview window");
        dialogContents.add(label6, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        dialogContents.add(caseTransformation, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }
    
    
    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {return dialogContents;}
}
