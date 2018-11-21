import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.lang.jvm.JvmModifier
import com.intellij.patterns.PsiJavaPatterns
import com.intellij.psi.*
import com.intellij.psi.impl.java.stubs.index.JavaMethodParameterTypesIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.ProcessingContext

class UtilityClassesCompletionContributor : CompletionContributor() {

    val methodParameterTypesIndex = JavaMethodParameterTypesIndex.getInstance()

    init {
        extend(CompletionType.BASIC, PsiJavaPatterns.psiElement(), object : CompletionProvider<CompletionParameters>() {
            override fun addCompletions(
                parameters: CompletionParameters,
                context: ProcessingContext?,
                result: CompletionResultSet
            ) {
                val staticMethods = staticMethods(parameters) ?: return
                result.addAllElements(staticMethods.map(::createLookupElement))
            }

        })
    }

    private fun staticMethods(parameters: CompletionParameters): List<PsiMethod>? {
        val project = parameters.position.project
        val resolveHelper = JavaPsiFacade.getInstance(parameters.position.project).resolveHelper

        val ref = parameters.position.containingFile.findReferenceAt(parameters.offset) ?: return null
        val element = ref.element as? PsiReferenceExpression ?: return null
        val type = element.qualifierExpression?.type as? PsiClassType ?: return null
        val clazz = type.className

        val start = System.currentTimeMillis()
        methodParameterTypesIndex.get(clazz, project, GlobalSearchScope.projectScope(project))
        println((System.currentTimeMillis() - start).toString() + " ms")


        return methodParameterTypesIndex.get(clazz, project, GlobalSearchScope.projectScope(project))
            .filter { it.containingClass?.nameIdentifier?.text.containsAnyIgnoreCase("util", "helper", "extension") }
            .filter { it.hasModifier(JvmModifier.STATIC) }
            .filter { it.hasParameters() }
            .filter {
                val receiver = it.parameterList.parameters[0].type
                resolveHelper.isAccessible(it, element, null) && receiver.isAssignableFrom(type)
            }
    }

    private fun createLookupElement(method: PsiMethod): LookupElement {
        val className = method.containingClass?.name
        val parametersText = method.parameterList.asText()
        val returnType = method.returnType?.presentableText
        return LookupElementBuilder.create(method)
            .withIcon(AllIcons.Nodes.Method)
            .withPresentableText("$className.${method.name}")
            .withBoldness(true)
            .withTailText("($parametersText)")
            .withTypeText(returnType)
            .withInsertHandler(StaticMethodInsertHandler())
    }

}