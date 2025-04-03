package org.antlr.intellij.plugin.profiler;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.table.JBTable;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.Getter;
import org.antlr.intellij.plugin.ANTLRv4PluginController;
import org.antlr.intellij.plugin.preview.InputPanel;
import org.antlr.intellij.plugin.preview.PreviewPanel;
import org.antlr.intellij.plugin.preview.PreviewState;
import org.antlr.runtime.CommonToken;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.atn.SemanticContext.PrecedencePredicate;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class ProfilerPanel implements Disposable {
    private static final Logger LOG =
        Logger.getInstance(ProfilerPanel.class);
    
    public static final Color AMBIGUITY_COLOR = new JBColor(new Color(157, 31, 31), new Color(157, 31, 31));
    public static final Color FULLCTX_COLOR = new JBColor(new Color(255, 128, 0), new Color(255, 128, 0));
    public static final Color PREDEVAL_COLOR = new JBColor(new Color(110, 139, 61), new Color(110, 139, 61));
    public static final Color DEEPESTLOOK_COLOR = new JBColor(new Color(0, 128, 128), new Color(0, 128, 128));
    
    public static final Key<DecisionEventInfo> DECISION_EVENT_INFO_KEY = Key.create("DECISION_EVENT_INFO");
    public static final Key<DecisionInfo> DECISION_INFO_KEY = Key.create("DECISION_INFO_KEY");
    
    public Project project;
    public PreviewState previewState;
    public PreviewPanel previewPanel;
    
    protected JPanel outerPanel;
    protected JPanel statsPanel;
    protected JLabel parseTimeField;
    protected JLabel predictionTimeField;
    protected JLabel lookaheadBurdenField;
    protected JLabel cacheMissRateField;
    protected JLabel inputSizeField;
    protected JLabel numTokensField;
    protected JCheckBox expertCheckBox;
    protected JLabel ambiguityColorLabel;
    protected JLabel contextSensitivityColorLabel;
    protected JLabel predEvaluationColorLabel;
    @Getter protected JBTable profilerDataTable;
    protected JLabel deepestLookaheadLabel;
    
    
    public ProfilerPanel(Project project, PreviewPanel previewPanel) {
        this.project = project;
        this.previewPanel = previewPanel;
    }
    
    
    public static String getSemanticContextDisplayString(
        PredicateEvalInfo pred,
        PreviewState previewState,
        SemanticContext semctx,
        int alt,
        boolean result
    ) {
        var g = previewState.getGrammar();
        var semanticContextDisplayString = g.getSemanticContextDisplayString(semctx);
        if (semctx instanceof PrecedencePredicate) {
            var ruleIndex = previewState.getParsingResult().parser.getATN().decisionToState.get(pred.decision).ruleIndex;
            var rule = g.getRule(ruleIndex);
            var precedence = ((SemanticContext.PrecedencePredicate) semctx).precedence;
            // precedence = n - originalAlt + 1, So:
            alt = rule.getOriginalNumberOfAlts() - precedence + 1;
        }
        
        return semanticContextDisplayString + " => alt " + alt + " is " + result;
    }
    
    
    public void grammarFileSaved(PreviewState previewState, VirtualFile grammarFile) {
        // leave model and such alone.
    }
    
    
    public void switchToGrammar(PreviewState previewState, VirtualFile grammarFile) {
        this.previewState = previewState;
        var model = new DefaultTableModel();
        profilerDataTable.setModel(model);
        profilerDataTable.setRowSorter(new TableRowSorter<AbstractTableModel>(model));
    }
    
    
    public void mouseEnteredGrammarEditorEvent(VirtualFile vfile, EditorMouseEvent e) {
        // clear grammar highlighters related to decision info
        InputPanel.removeHighlighters(e.getEditor(), ProfilerPanel.DECISION_INFO_KEY);
    }
    
    
    public JPanel getComponent() {
        return outerPanel;
    }
    
    
    public void setProfilerData(PreviewState previewState, long parseTime_ns) {
        this.previewState = previewState;
        var parser = previewState.getParsingResult().parser;
        var parseInfo = parser.getParseInfo();
        updateTableModelPerExpertCheckBox(parseInfo, parser);
        var parseTimeMS = parseTime_ns / (1000.0 * 1000.0);
        
        // microsecond decimal precision
        NumberFormat formatter = new DecimalFormat("#.###");
        parseTimeField.setText(formatter.format(parseTimeMS));
        var predTimeMS = parseInfo.getTotalTimeInPrediction() / (1000.0 * 1000.0);
        
        predictionTimeField.setText(
            String.format("%s = %3.2f%%", formatter.format(predTimeMS), 100 * (predTimeMS) / parseTimeMS)
        );
        
        var tokens = parser.getInputStream();
        var numTokens = tokens.size();
        var lastToken = tokens.get(numTokens - 1);
        var numChar = lastToken.getStopIndex();
        var numLines = lastToken.getLine();
        
        if (lastToken.getType() == Token.EOF) {
            if (numTokens <= 1) {
                numLines = 0;
            } else {
                var secondToLastToken = tokens.get(numTokens - 2);
                numLines = secondToLastToken.getLine();
            }
        }
        
        inputSizeField.setText(String.format(
            "%d char, %d lines",
            numChar,
            numLines
        ));
        
        numTokensField.setText(String.valueOf(numTokens));
        double look =
            parseInfo.getTotalSLLLookaheadOps() +
            parseInfo.getTotalLLLookaheadOps();
        
        lookaheadBurdenField.setText(
            String.format("%d/%d = %3.2f", (long) look, numTokens, look / numTokens)
        );
        
        double atnLook = parseInfo.getTotalATNLookaheadOps();
        cacheMissRateField.setText(
            String.format("%d/%d = %3.2f%%", (long) atnLook, (long) look, atnLook * 100.0 / look)
        );
    }
    
    
    public void updateTableModelPerExpertCheckBox(ParseInfo parseInfo, Parser parser) {
        AbstractTableModel model;
        
        if (expertCheckBox.isSelected()) {
            model = new ExpertProfilerTableDataModel(parseInfo, parser);
        } else {
            model = new SimpleProfilerTableDataModel(parseInfo, parser);
        }
        
        profilerDataTable.setModel(model);
        profilerDataTable.setRowSorter(new TableRowSorter<>(model));
    }
    
    
    public void selectDecisionInGrammar(PreviewState previewState, int decision) {
        final var controller = ANTLRv4PluginController.getInstance(previewState.getProject());
        
        if (controller == null) {
            return;
        }
        
        final var grammarEditor = controller.getEditor(previewState.getGrammarFile());
        
        if (grammarEditor == null) {
            return;
        }
        
        var decisionState = previewState.getGrammar().atn.getDecisionState(decision);
        var region = previewState.getGrammar().getStateToGrammarRegion(decisionState.stateNumber);
        
        if (region == null) {
            LOG.error("decision " + decision + " has state " + decisionState.stateNumber + " but no region");
            return;
        }
        
        InputPanel.removeHighlighters(grammarEditor, ProfilerPanel.DECISION_INFO_KEY);
        
        var tokens = previewState.getGrammar().tokenStream;
        
        if (region.a >= tokens.size() || region.b >= tokens.size()) {
            return;
        }
        
        var startToken = (CommonToken) tokens.get(region.a);
        var stopToken = (CommonToken) tokens.get(region.b);
        var effectColor = JBColor.darkGray;
        var decisionInfo = previewState.getParsingResult().parser.getParseInfo().getDecisionInfo()[decision];
        
        if (!decisionInfo.predicateEvals.isEmpty()) {
            effectColor = new JBColor(PREDEVAL_COLOR, AMBIGUITY_COLOR);
        }
        
        if (!decisionInfo.contextSensitivities.isEmpty()) {
            effectColor = new JBColor(FULLCTX_COLOR, AMBIGUITY_COLOR);
        }
        
        if (!decisionInfo.ambiguities.isEmpty()) {
            effectColor = new JBColor(AMBIGUITY_COLOR, AMBIGUITY_COLOR);
        }
        
        var attr =
            new TextAttributes(JBColor.BLACK, JBColor.WHITE, effectColor,
                EffectType.ROUNDED_BOX, Font.PLAIN
            );
        
        var markupModel = grammarEditor.getMarkupModel();
        
        final var rangeHighlighter = markupModel.addRangeHighlighter(
            startToken.getStartIndex(),
            stopToken.getStopIndex() + 1,
            HighlighterLayer.SELECTION, // layer
            attr,
            HighlighterTargetArea.EXACT_RANGE
        );
        
        rangeHighlighter.putUserData(DECISION_INFO_KEY, decisionInfo);
        
        var scrollingModel = grammarEditor.getScrollingModel();
        var caretModel = grammarEditor.getCaretModel();
        caretModel.moveToOffset(startToken.getStartIndex());
        scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE);
    }
    
    
    public void highlightInputPhrases(PreviewState previewState, int decision) {
        if (previewState == null || previewState.getParsingResult() == null) {
            return;
        }
        
        var inputEditor = previewState.getInputEditor();
        var scrollingModel = inputEditor.getScrollingModel();
        var caretModel = inputEditor.getCaretModel();
        var markupModel = inputEditor.getMarkupModel();
        
        InputPanel.clearDecisionEventHighlighters(inputEditor);
        
        var parseInfo = previewState.getParsingResult().parser.getParseInfo();
        var decisionInfo = parseInfo.getDecisionInfo()[decision];
        
        Token firstToken = null;
        // deepest lookahead
        var maxLook = Math.max(decisionInfo.LL_MaxLook, decisionInfo.SLL_MaxLook);
        
        // ignore k=1
        if (maxLook > 1) {
            var maxLookEvent = decisionInfo.SLL_MaxLookEvent;
            
            if (decisionInfo.LL_MaxLook > decisionInfo.SLL_MaxLook) {
                maxLookEvent = decisionInfo.LL_MaxLookEvent;
            }
            
            firstToken = addDecisionEventHighlighter(previewState, markupModel,
                maxLookEvent,
                DEEPESTLOOK_COLOR,
                EffectType.BOLD_DOTTED_LINE
            );
        }
        
        // pred evals
        for (var predEvalInfo : decisionInfo.predicateEvals) {
            var t = addDecisionEventHighlighter(previewState, markupModel, predEvalInfo, PREDEVAL_COLOR, EffectType.ROUNDED_BOX);
            if (firstToken == null) firstToken = t;
        }
        
        // context-sensitivities
        for (var ctxSensitivityInfo : decisionInfo.contextSensitivities) {
            var t = addDecisionEventHighlighter(previewState, markupModel, ctxSensitivityInfo, FULLCTX_COLOR, EffectType.ROUNDED_BOX);
            if (firstToken == null) firstToken = t;
        }
        
        // ambiguities (might overlay context-sensitivities)
        for (var ambiguityInfo : decisionInfo.ambiguities) {
            var t = addDecisionEventHighlighter(previewState, markupModel, ambiguityInfo, AMBIGUITY_COLOR, EffectType.ROUNDED_BOX);
            if (firstToken == null) firstToken = t;
        }
        
        if (firstToken != null) {
            caretModel.moveToOffset(firstToken.getStartIndex());
            scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE);
        }
    }
    
    
    public Token addDecisionEventHighlighter(
        PreviewState previewState, MarkupModel markupModel,
        DecisionEventInfo info, Color errorStripeColor,
        EffectType effectType
    ) {
        var tokens = previewState.getParsingResult().parser.getInputStream();
        var startToken = tokens.get(info.startIndex);
        var stopToken = tokens.get(info.stopIndex);
        var textAttributes =
            new TextAttributes(JBColor.BLACK, JBColor.WHITE, errorStripeColor,
                effectType, Font.PLAIN
            );
        
        textAttributes.setErrorStripeColor(errorStripeColor);
        
        final var rangeHighlighter =
            markupModel.addRangeHighlighter(
                startToken.getStartIndex(), stopToken.getStopIndex() + 1,
                HighlighterLayer.ADDITIONAL_SYNTAX, textAttributes,
                HighlighterTargetArea.EXACT_RANGE
            );
        
        rangeHighlighter.putUserData(DECISION_EVENT_INFO_KEY, info);
        rangeHighlighter.setErrorStripeMarkColor(errorStripeColor);
        return startToken;
    }
    
    
    private void createUIComponents() {
        expertCheckBox = new JBCheckBox();
        expertCheckBox.setSelected(false);
        expertCheckBox.addActionListener(e -> {
            if (previewState.getParsingResult() == null) {
                // nothing has been parsed yet (no text in the editor)
                return;
            }
            
            var parseInfo = previewState.getParsingResult().parser.getParseInfo();
            updateTableModelPerExpertCheckBox(parseInfo, previewState.getParsingResult().parser);
        });
        
        profilerDataTable = new JBTable() {
            @Override
            protected @NotNull JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    @Override public String getToolTipText(MouseEvent e) {
                        var p = e.getPoint();
                        var index = columnModel.getColumnIndexAtX(p.x);
                        var realIndex = columnModel.getColumn(index).getModelIndex();
                        var model = getModel();
                        
                        if (model instanceof ProfilerTableDataModel) {
                            return ((ProfilerTableDataModel) model).getColumnToolTips()[realIndex];
                        }
                        
                        return model.getColumnName(realIndex);
                    }
                };
            }
            
            
            @Override
            public TableCellRenderer getDefaultRenderer(Class<?> columnClass) {
                return new ProfileTableCellRenderer();
            }
        };
        
        var selectionModel = getListSelectionModel();
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ambiguityColorLabel = new JBLabel("Ambiguity");
        ambiguityColorLabel.setForeground(AMBIGUITY_COLOR);
        contextSensitivityColorLabel = new JBLabel("Context sensitivity");
        contextSensitivityColorLabel.setForeground(FULLCTX_COLOR);
        predEvaluationColorLabel = new JBLabel("Predicate evaluation");
        predEvaluationColorLabel.setForeground(PREDEVAL_COLOR);
        deepestLookaheadLabel = new JBLabel("Deepest lookahead");
        deepestLookaheadLabel.setForeground(DEEPESTLOOK_COLOR);
    }
    
    
    private @NotNull ListSelectionModel getListSelectionModel() {
        var selectionModel = profilerDataTable.getSelectionModel();
        selectionModel.addListSelectionListener(
            e -> {
                // previewState, project set later
                if (e.getValueIsAdjusting()) {
                    return; // this seems to be "mouse down" but not mouse up
                }
                // get state for current grammar editor tab
                if (project == null) {
                    return;
                }
                
                if (previewState != null && profilerDataTable.getModel().getClass() != DefaultTableModel.class) {
                    var selectedRow = profilerDataTable.getSelectedRow();
                    if (selectedRow == -1) {
                        selectedRow = 0;
                    }
                    
                    var decision = profilerDataTable.convertRowIndexToModel(selectedRow);
                    var numberOfDecisions = previewState.getGrammar().atn.getNumberOfDecisions();
                    
                    if (decision <= numberOfDecisions) {
                        selectDecisionInGrammar(previewState, decision);
                        highlightInputPhrases(previewState, decision);
                    }
                }
            }
        );
        
        return selectionModel;
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
        outerPanel = new JPanel();
        outerPanel.setLayout(new BorderLayout(0, 0));
        statsPanel = new JPanel();
        statsPanel.setLayout(new GridLayoutManager(12, 3, new Insets(0, 5, 0, 0), -1, -1));
        outerPanel.add(statsPanel, BorderLayout.EAST);
        final JLabel label1 = new JLabel();
        label1.setText("Parse time (ms):");
        statsPanel.add(label1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(130, 16), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Prediction time (ms):");
        statsPanel.add(label2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(130, 16), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Lookahead burden:");
        statsPanel.add(label3, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(130, 16), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("DFA cache miss rate:");
        statsPanel.add(label4, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(130, 16), null, 0, false));
        final Spacer spacer1 = new Spacer();
        statsPanel.add(spacer1, new GridConstraints(11, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(-1, 14), null, 0, false));
        final Spacer spacer2 = new Spacer();
        statsPanel.add(spacer2, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        parseTimeField = new JLabel();
        parseTimeField.setText("0");
        statsPanel.add(parseTimeField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        predictionTimeField = new JLabel();
        predictionTimeField.setText("0");
        statsPanel.add(predictionTimeField, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lookaheadBurdenField = new JLabel();
        lookaheadBurdenField.setText("0");
        statsPanel.add(lookaheadBurdenField, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cacheMissRateField = new JLabel();
        cacheMissRateField.setText("0");
        statsPanel.add(cacheMissRateField, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Input size:");
        statsPanel.add(label5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(130, 16), null, 0, false));
        inputSizeField = new JLabel();
        inputSizeField.setText("0");
        statsPanel.add(inputSizeField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Number of tokens:");
        statsPanel.add(label6, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        numTokensField = new JLabel();
        numTokensField.setText("0");
        statsPanel.add(numTokensField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        statsPanel.add(panel1, new GridConstraints(7, 0, 4, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        ambiguityColorLabel.setText("Ambiguity");
        panel1.add(ambiguityColorLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        contextSensitivityColorLabel.setText("Context-sensitivity");
        panel1.add(contextSensitivityColorLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        predEvaluationColorLabel.setText("Predicate evaluation");
        panel1.add(predEvaluationColorLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        deepestLookaheadLabel.setText("Deepest lookahead");
        panel1.add(deepestLookaheadLabel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        expertCheckBox.setText("Show expert columns");
        statsPanel.add(expertCheckBox, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        outerPanel.add(scrollPane1, BorderLayout.CENTER);
        profilerDataTable.setPreferredScrollableViewportSize(new Dimension(800, 400));
        scrollPane1.setViewportView(profilerDataTable);
    }
    
    
    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {return outerPanel;}
    
    
    class ProfileTableCellRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(
            JTable table, Object value,
            boolean isSelected, boolean hasFocus,
            int row, int column
        ) {
            var c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (previewState == null || previewState.getParsingResult() == null) {
                return c;
            }
            
            var parseInfo = previewState.getParsingResult().parser.getParseInfo();
            var decision = profilerDataTable.convertRowIndexToModel(row);
            var decisions = parseInfo.getDecisionInfo();
            
            if (decision >= decisions.length) {
                return c;
            }
            
            var decisionInfo = decisions[decision];
            if (!decisionInfo.ambiguities.isEmpty()) {
                setForeground(AMBIGUITY_COLOR);
            } else if (!decisionInfo.contextSensitivities.isEmpty()) {
                setForeground(FULLCTX_COLOR);
            } else if (!decisionInfo.predicateEvals.isEmpty()) {
                setForeground(PREDEVAL_COLOR);
            }
            
            return c;
        }
    }
    
    /* ------------------------------------------------------------------------------------------------------------------ */
    
    
    /**
     * Usually not invoked directly, see class javadoc.
     */
    @Override public void dispose() {
        LOG.debug("Dispose called: " + this.getClass().getName());
    }
}
