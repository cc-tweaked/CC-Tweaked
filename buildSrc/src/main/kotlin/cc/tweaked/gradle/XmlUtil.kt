package cc.tweaked.gradle

import groovy.util.Node
import groovy.util.NodeList

object XmlUtil {
    fun findChild(node: Node, name: String): Node? = when (val child = node.get(name)) {
        is Node -> child
        is NodeList -> child.singleOrNull() as Node?
        else -> null
    }
}
