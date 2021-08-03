/*
 * Copyright © Magento, Inc. All rights reserved.
 * See COPYING.txt for license details.
 */

package com.magento.idea.magento2plugin.actions.refactoring.dialog;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.PhpLangUtil;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.magento.idea.magento2plugin.actions.generation.dialog.AbstractDialog;
import com.magento.idea.magento2plugin.actions.generation.dialog.validator.annotation.FieldValidation;
import com.magento.idea.magento2plugin.actions.generation.dialog.validator.annotation.RuleRegistry;
import com.magento.idea.magento2plugin.actions.generation.dialog.validator.rule.NotEmptyRule;
import com.magento.idea.magento2plugin.actions.refactoring.PhpUnitMultipleTestsRefactorAction;
import com.magento.idea.magento2plugin.actions.refactoring.util.PhpUnitRefactoringUtil;
import com.magento.idea.magento2plugin.util.GetFirstClassOfFile;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import org.jetbrains.annotations.NotNull;

public class RefactorMultiplePhpUnitTestsDialog extends AbstractDialog {

    private static final String CLASS_FQN_REGEX = "(\\\\*\\w*[\\\\\\w*]*)";

    private final @NotNull Project project;
    private final @NotNull PsiDirectory directory;

    @FieldValidation(rule = RuleRegistry.NOT_EMPTY,
            message = {NotEmptyRule.MESSAGE, "Include List"})
    private JTextArea includeListArea;
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;

    // labels
    private JLabel IncludeListLabel;//NOPMD
    private JLabel IncludeListAreaErrorMessage;//NOPMD

    /**
     * Multiple PHPUnit test refactoring dialog constructor.
     *
     * @param project Project
     * @param directory PsiDirectory
     */
    public RefactorMultiplePhpUnitTestsDialog(
            final @NotNull Project project,
            final @NotNull PsiDirectory directory
    ) {
        super();

        this.project = project;
        this.directory = directory;

        setContentPane(contentPane);
        setModal(true);
        setTitle(PhpUnitMultipleTestsRefactorAction.ACTION_DESCRIPTION);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(event -> onOK());
        buttonCancel.addActionListener(event -> onCancel());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent event) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(
                event -> onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );
    }

    /**
     * Open new dialog window.
     *
     * @param project Project
     * @param directory PsiDirectory
     */
    public static void open(
            final @NotNull Project project,
            final @NotNull PsiDirectory directory
    ) {
        final RefactorMultiplePhpUnitTestsDialog dialog =
                new RefactorMultiplePhpUnitTestsDialog(project, directory);
        dialog.pack();
        dialog.centerDialog(dialog);
        dialog.setVisible(true);
    }

    /**
     * Fire generation process if all fields are valid.
     */
    private void onOK() {
        if (!validateFormFields()) {
            return;
        }
        final List<String> includeList = getIncludeList();

        if (!includeList.isEmpty()) {
            visitDirectory(directory, includeList);
        }

        this.setVisible(false);
    }

    private void visitDirectory(
            final @NotNull PsiDirectory directory,
            final @NotNull List<String> includeList
    ) {
        final PsiFile[] files = directory.getFiles();

        for (final PsiFile file : files) {
            if (!(file instanceof PhpFile)) {
                continue;
            }
            final PhpClass phpClass = GetFirstClassOfFile.getInstance().execute((PhpFile) file);

            if (phpClass != null && (includeList.contains(phpClass.getFQN())
                    || includeList.contains(
                            phpClass.getFQN().startsWith("\\")
                                    ? phpClass.getFQN().substring(1)
                                    : phpClass.getFQN()))
            ) {
                new PhpUnitRefactoringUtil(phpClass).fixDeprecatedMethods();
            }
        }

        for (final PsiDirectory subDirectory : directory.getSubdirectories()) {
            visitDirectory(subDirectory, includeList);
        }
    }

    /**
     * Get include list from user input.
     *
     * @return List[String]
     */
    private List<String> getIncludeList() {
        final List<String> includeList = new LinkedList<>();
        final String input = includeListArea.getText().trim();
        final Pattern pattern = Pattern.compile(CLASS_FQN_REGEX, Pattern.MULTILINE);

        for (final String line : input.split("\n")) {
            final Matcher matcher = pattern.matcher(line);

            while (matcher.find()) {
                final String fqnCandidate = matcher.group();

                if (!fqnCandidate.isEmpty()
                        && (PhpLangUtil.isFqn(fqnCandidate)
                                || PhpLangUtil.isFqn("\\".concat(fqnCandidate)))
                ) {
                    includeList.add(fqnCandidate);
                }
            }
        }

        return includeList;
    }
}
