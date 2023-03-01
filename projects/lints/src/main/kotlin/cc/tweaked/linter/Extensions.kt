// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package cc.tweaked.linter

import com.google.errorprone.VisitorState
import com.google.errorprone.util.ASTHelpers
import com.sun.tools.javac.code.Symbol
import com.sun.tools.javac.code.Types
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.util.SimpleAnnotationValueVisitor8

inline fun <reified T> VisitorState.getContext(create: () -> T): T {
    val provider: T? = context.get(T::class.java)
    if (provider != null) return provider

    val newProvider = create()
    context.put(T::class.java, newProvider)
    return newProvider
}

fun Symbol.MethodSymbol.getAnySuperMethod(types: Types): Symbol.MethodSymbol? =
    ASTHelpers.findSuperMethods(this, types).firstOrNull()

fun Symbol.MethodSymbol.getSuperMethods(types: Types): Collection<Symbol.MethodSymbol> =
    ASTHelpers.findSuperMethods(this, types)

// Annotation helpers

typealias AnnotationGetter<T> = (AnnotationValue) -> T

object AnnotationGetters {
    fun <E : Enum<E>> enum(type: Class<E>): AnnotationGetter<E> =
        { value -> java.lang.Enum.valueOf(type, value.toString()) }

    inline fun <reified E : Enum<E>> enum(): AnnotationGetter<E> = enum(E::class.java)

    inline fun <reified T> exact(): AnnotationGetter<T> = { value -> value.value as T }

    fun <T> list(element: AnnotationGetter<T>): AnnotationGetter<List<T>> = { value ->
        value.accept(
            object : SimpleAnnotationValueVisitor8<List<T>, Unit>() {
                override fun visitArray(vals: MutableList<out AnnotationValue>, state: Unit): List<T> =
                    vals.map(element)
            },
            Unit,
        )!!
    }
}

fun AnnotationMirror.getValue(name: String): AnnotationValue? {
    val value = elementValues.entries.find { it.key.simpleName.contentEquals(name) } ?: return null
    return value.value
}

fun <T> AnnotationMirror.getValue(name: String, getter: AnnotationGetter<T>): T? = getValue(name)?.let(getter)

fun Element.getAnnotation(name: String): AnnotationMirror? =
    annotationMirrors.find {
        val type = it.annotationType.asElement() as TypeElement
        type.qualifiedName.contentEquals(name)
    }
