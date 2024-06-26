import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameterList

fun String?.containsAnyIgnoreCase(vararg candidates: String): Boolean {
    if(this == null) {
        return false
    }
    return candidates.any { this.contains(it, true) }
}


fun PsiParameterList.asText() = this.parameters.joinToString(separator = ", ") {
    val clazz = (it.type as? PsiClassType)?.className ?: it.type.presentableText
    "${clazz} ${it.name}"
}

fun PsiMethod.hasMultipleParameters() = this.parameterList.parameters.size > 1
