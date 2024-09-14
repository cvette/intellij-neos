package de.vette.idea.neos.lang.eel.refactoring;

import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.actions.IntroduceActionBase;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import de.vette.idea.neos.lang.eel.psi.*;
import de.vette.idea.neos.lang.fusion.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ExtractStatement extends IntroduceActionBase implements RefactoringActionHandler {
    @Override
    protected @Nullable RefactoringActionHandler getRefactoringHandler(@NotNull RefactoringSupportProvider provider) {
        return this;
    }

    @Override
    protected boolean isAvailableForFile(PsiFile file) {
        if (!(file instanceof FusionFile) && !(file instanceof EelFile)) {
            return false;
        }
        return super.isAvailableForFile(file);
    }

    @Override
    protected boolean isAvailableInEditorOnly() {
        return true;
    }

    private boolean hasSelection(Editor editor) {
        return editor.getSelectionModel().hasSelection();
    }

    @Override
    protected boolean isAvailableOnElementInEditorAndFile(@NotNull PsiElement element, @NotNull Editor editor, @NotNull PsiFile file, @NotNull DataContext context) {
        if (!hasSelection(editor)) {
            return false;
        }
        return super.isAvailableOnElementInEditorAndFile(element, editor, file, context);
    }

    /**
     * We try to find a longest statement (in order of AST structure, see {@link #findLongestStatementInSelection}).
     * If that statement is smaller than the selection, we look into the closest parent collection (e.g. a sum) to check
     * if the selection contains a valid subset of operands of that collection and collect these.
     * Otherwise, the longest statement is our match to extract.
     * TODO I think we can always look upwards after we return the longest statement as is, if it spans the whole selection
     */
    private @Nullable PsiElement[] findMatchingStatement(PsiFile file, int startOffset, int endOffset) {
        PsiElement longestStatement = findLongestStatementInSelection(file, startOffset, endOffset);
        if (longestStatement == null) return null;

        TextRange longestStatementRange = longestStatement.getTextRange();
        // this ignores surrounding whitespace, might be interesting to handle it
        if (longestStatementRange.getStartOffset() == startOffset && longestStatementRange.getEndOffset() == endOffset) {
            return new PsiElement[]{longestStatement};
        }

        if (longestStatement instanceof EelDisjunction disjunction) {
            return collectChildren(disjunction, startOffset, endOffset);
        }

        if (longestStatement instanceof EelConjunction conjunction) {
            if (startOffset < conjunction.getTextRange().getStartOffset() || endOffset > conjunction.getTextRange().getEndOffset()) {
                // selection may be valid part of a disjunction
                if (conjunction.getParent() instanceof EelDisjunction disjunction) {
                    return collectChildren(disjunction, startOffset, endOffset);
                }
                return null;
            }
            return collectChildren(conjunction, startOffset, endOffset);
        }

        if (longestStatement instanceof EelSumCalculation sum) {
            // collect EelSumCalculations with matching operators
            if (startOffset < sum.getTextRange().getStartOffset() || endOffset > sum.getTextRange().getEndOffset()) {
                // selection may be valid part of a conjunction
                if (sum.getParent().getParent() instanceof EelConjunction conjunction) {
                    return collectChildren(conjunction, startOffset, endOffset);
                }
                return null;
            }
            return collectChildren(sum, startOffset, endOffset);
        }

        if (longestStatement instanceof EelProdCalculation prod) {
            if (startOffset < prod.getTextRange().getStartOffset() || endOffset > prod.getTextRange().getEndOffset()) {
                // selection may be valid part of an addition
                if (prod.getParent() instanceof EelSumCalculation sum) {
                    return collectChildren(sum, startOffset, endOffset);
                }
                return null;
            }
            return collectChildren(prod, startOffset, endOffset);
        }

        if (longestStatement instanceof EelSimpleExpression simple) {
            if (startOffset < simple.getTextRange().getStartOffset() || endOffset > simple.getTextRange().getEndOffset()) {
                // selection may be valid part of a multiplication
                if (simple.getParent() instanceof EelProdCalculation prod) {
                    return collectChildren(prod, startOffset, endOffset);
                }
                return new PsiElement[]{simple};
            }
        }

        return null;
    }

    private static @Nullable PsiElement findLongestStatementInSelection(PsiFile file, int startOffset, int endOffset) {
        Class<PsiElement>[] candidates = new Class[]{
            EelDisjunction.class,
            EelConjunction.class,
            EelSumCalculation.class,
            EelProdCalculation.class,
            EelSimpleExpression.class
        };
        for (Class<PsiElement> candidate : candidates) {
            PsiElement longestStatement = PsiTreeUtil.findElementOfClassAtRange(file, startOffset, endOffset, candidate);
            if (longestStatement != null) {
                return longestStatement;
            }
        }
        return null;
    }

    private @Nullable PsiElement[] collectChildren(EelSumCalculation sum, int startOffset, int endOffset) {
        return doCollectChildren(sum, startOffset, endOffset, EelProdCalculation.class, EelTypes.EEL_ADDITION_OPERATOR, EelTypes.EEL_SUBTRACTION_OPERATOR);
    }

    private @Nullable PsiElement[] collectChildren(EelProdCalculation prod, int startOffset, int endOffset) {
        return doCollectChildren(prod, startOffset, endOffset, EelSimpleExpression.class, EelTypes.EEL_MULTIPLICATION_OPERATOR, EelTypes.EEL_DIVISION_OPERATOR);
    }

    private @Nullable PsiElement[] collectChildren(EelDisjunction disjunction, int startOffset, int endOffset) {
        return doCollectChildren(disjunction, startOffset, endOffset, EelConjunction.class, EelTypes.EEL_BOOLEAN_OR);
    }

    private @Nullable PsiElement[] collectChildren(EelConjunction conjunction, int startOffset, int endOffset) {
        return doCollectChildren(conjunction, startOffset, endOffset, EelComparison.class, EelTypes.EEL_BOOLEAN_AND);
    }

    /**
     * Collects all children of a collection element within the selection, making sure that the selection does not
     * contain orphaned operators.
     * @param element The collection element (e.g. an addition or a disjunction)
     * @param startOffset The start offset of the selection
     * @param endOffset The end offset of the selection
     * @param operandType The type of the operands of the collection
     * @param operatorType The types of operators in that collection
     * @return The collected children of the collection within the selection (including operands, operators and
     *      whitespace) or null, if the selection does not match a valid statement.
     * @param <T> The type of the collection element
     * @param <S> The type of the operands of the collection
     */
    private <T extends EelElement, S extends PsiElement> @Nullable PsiElement[] doCollectChildren(T element, int startOffset, int endOffset, Class<S> operandType, IElementType...operatorType) {
        if (element.getChildren().length == 0) {
            return null;
        }

        List<PsiElement> statements = new ArrayList<>();
        boolean needsOperand = false;
        boolean hasOperand = false;
        PsiElement child = element.getFirstChild();
        while (child != null) {
            if (child.getTextRange().getEndOffset() <= startOffset) {
                if (child.getTextRange().getStartOffset() > startOffset) {
                    // element crosses the selection start boundary -> selection cuts of an expression
                    return null;
                }
                child = child.getNextSibling();
                continue;
            }
            if (child.getTextRange().getEndOffset() > endOffset) {
                // TODO check for partial expression (e.g. [a+b]+c could be a valid expression, while the whole element is out of bounds)
                break;
            }

            if (operandType.isInstance(child)) {
                needsOperand = false;
                hasOperand = true;
            } else if (!(child instanceof PsiWhiteSpace)) {
                for (IElementType operator : operatorType) {
                    if (child.getNode().getElementType() == operator) {
                        needsOperand = true;
                        if (!hasOperand) {
                            // selection starts with an operator, not a valid statement
                            return null;
                        }
                    }
                }
            }
            statements.add(child);
            child = child.getNextSibling();
        }

        if (needsOperand) {
            // not a complete statement
            return null;
        }

        return statements.toArray(new PsiElement[0]);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file, DataContext dataContext) {
        int startOffset = editor.getSelectionModel().getSelectionStart();
        int endOffset = editor.getSelectionModel().getSelectionEnd();
        if (startOffset == endOffset) {
            return;
        }

        var refactoringTitle = RefactoringBundle.message("introduce.variable.title");

        var selectedStatements = findMatchingStatement(file, startOffset, endOffset);
        if (selectedStatements == null) {
            CommonRefactoringUtil.showErrorHint(project, editor, RefactoringBundle.message("refactoring.introduce.selection.error"), refactoringTitle, null);
            return;
        }

        var assignment = PsiTreeUtil.findFirstParent(selectedStatements[0], true, element -> element instanceof FusionPropertyAssignment);
        if (!(assignment instanceof FusionPropertyAssignment)) {
            CommonRefactoringUtil.showErrorHint(project, editor, RefactoringBundle.message("refactoring.introduce.context.error"), refactoringTitle, null);
            return;
        }
        var fusionPath = ((FusionPropertyAssignment)assignment).getPath();
        var scope = suggestScope(fusionPath);
        var variableName = "extracted";

        WriteCommandAction.runWriteCommandAction(project, () -> {
            var newElement = EelElementFactory.createExpression(project, String.format("%s.%s", scope, variableName));
            var addedVariable = selectedStatements[0].getParent().addBefore(newElement, selectedStatements[0]);

            var newPath = fusionPath.getText().split("\\.");
            newPath[newPath.length - 1] = variableName;
            var newAssignmentValue = Arrays.stream(selectedStatements).map(PsiElement::getText).collect(Collectors.joining());
            var extractedEelStatement = EelElementFactory.createExpression(project, newAssignmentValue);
            var extractedAssignment = FusionElementFactory.createEelAssignment(project, String.join(".", newPath), extractedEelStatement);

            for (PsiElement element : selectedStatements) {
                if (!(element instanceof PsiWhiteSpace)) {
                    element.delete();
                }
            }

            var anchor = assignment;
            if (assignment.getPrevSibling() instanceof PsiWhiteSpace) {
                // whitespace is not the linebreak, but probably just indentation and we want to keep that.
                anchor = assignment.getPrevSibling();
            }
            var addedAssignment = anchor.getParent().addBefore(extractedAssignment, anchor);
            var lineBreak = FusionElementFactory.createFusionFile(project, "\n").getFirstChild();
            addedAssignment.getParent().addAfter(lineBreak, addedAssignment);

            // select the newly inserted words
            // the cleaner solution would be to trigger a renaming here, I guess, but we can't rename fusion for now
            var declarationName = ((FusionPropertyAssignment)addedAssignment).getPath().getLastChild();
            var usage = Objects.requireNonNull(PsiTreeUtil.findChildOfType(addedVariable, EelCompositeIdentifier.class)).getLastChild();
            CaretModel caretModel = editor.getCaretModel();
            caretModel.moveToOffset(declarationName.getTextOffset());
            caretModel.addCaret(editor.offsetToVisualPosition(usage.getTextOffset()));
            caretModel.getAllCarets().forEach(caret -> caret.selectWordAtCaret(false));
        });
    }

    protected FusionPath withParentPath(FusionPath path) {
        var parentBlock = PsiTreeUtil.findFirstParent(path, true, element -> element instanceof FusionPropertyBlock);
        if (parentBlock instanceof FusionPropertyBlock propertyBlock) {
            var result = propertyBlock.getPath().copy();
            for (var pathSegment : path.getChildren()) {
                result.add(pathSegment.copy());
            }
            return (FusionPath) result;
        }

        return path;
    }

    protected String suggestScope(FusionPath path) {
        // In case we are a path within a block, the relevant information may be in the block's path
        var fullPath = withParentPath(path);

        // here could be afx => props, but we don't refactor afx properties for now

        var lastChild = fullPath.getLastChild();

        // not every renderer is a component, but let's assume it here
        if (lastChild instanceof FusionSinglePath singlePath && singlePath.getText().equals("renderer")) {
            return "props";
        }

        if (fullPath.getChildren().length > 1) {
            var secondLastChild = fullPath.getChildren()[fullPath.getChildren().length - 2];
            // values within @private blocks need to be accessed via private.xyz
            if (secondLastChild instanceof FusionMetaProperty metaProperty && metaProperty.getText().equals("@private")) {
                return "private";
            }
        }

        return "this";
    }

    @Override
    public void invoke(@NotNull Project project, PsiElement @NotNull [] elements, DataContext dataContext) {
    }
}
