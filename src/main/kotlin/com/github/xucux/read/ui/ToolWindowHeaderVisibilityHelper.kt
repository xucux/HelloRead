package com.github.xucux.read.ui

import com.intellij.openapi.wm.ToolWindow

/**
 * HelloRead 工具窗口顶部栏显示控制工具。
 *
 * 说明：
 * IntelliJ 公开 API 未提供稳定的顶部栏显示开关，这里通过反射调用内部 Decorator 的
 * setHeaderVisible(boolean) 能力；若当前 IDE 版本不支持则安全降级，不抛出异常。
 */
object ToolWindowHeaderVisibilityHelper {

    fun apply(toolWindow: ToolWindow, visible: Boolean): Boolean {
        return runCatching {
            val decorator = extractDecorator(toolWindow) ?: return false
            val method = decorator.javaClass.methods.firstOrNull {
                it.name == "setHeaderVisible" && it.parameterCount == 1
            } ?: return false

            method.isAccessible = true
            method.invoke(decorator, visible)
            true
        }.getOrDefault(false)
    }

    private fun extractDecorator(toolWindow: ToolWindow): Any? {
        return runCatching {
            val getDecorator = toolWindow.javaClass.methods.firstOrNull {
                it.name == "getDecorator" && it.parameterCount == 0
            } ?: return null

            getDecorator.isAccessible = true
            getDecorator.invoke(toolWindow)
        }.getOrNull()
    }
}
