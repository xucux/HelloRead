package com.github.xucux.read.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware

/**
 * 工具窗口齿轮菜单：打开阅读设置
 */
class OpenReadingSettingsGearAction : AnAction("阅读设置"), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ShowSettingsUtil.getInstance().showSettingsDialog(
            project,
            "com.github.xucux.read.setting.HelloReadSettingComponent"
        )
    }
}
