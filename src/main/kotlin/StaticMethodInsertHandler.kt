import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.psi.*
import com.siyeh.ig.psiutils.ImportUtils

class StaticMethodInsertHandler :
    InsertHandler<LookupElement> {
    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val element = context.file.findElementAt(context.startOffset) ?: return
        val ref = element.parent as? PsiReferenceExpression ?: return
        val qualifierExpression = ref.qualifierExpression ?: return

        val method = item.psiElement as? PsiMethod ?: return
        val containingClass = method.containingClass ?: return

        ImportUtils.addImportIfNeeded(containingClass, ref)

        val text = replacementText(containingClass, method, qualifierExpression) ?: return

        val factory = JavaPsiFacade.getElementFactory(context.project)
        val expression = factory.createExpressionFromText(text, qualifierExpression)
        ref.replace(expression)
        context.commitDocument()
        if (method.hasMultipleParameters()) {
            EditorModificationUtil.moveCaretRelatively(context.editor, -1)
        }
    }

    private fun replacementText(clazz: PsiClass, method: PsiMethod, qualifierExpression: PsiExpression): String? {
        val className = clazz.qualifiedName ?: return null
        val maybeComma = if (method.hasMultipleParameters()) ", " else ""
        val probablyQualifier = qualifierExpression.text
        return "$className.${method.name}($probablyQualifier$maybeComma)"
    }


}