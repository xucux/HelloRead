package com.github.xucux.read.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware

/**
 * 字体设置动作
 * 打开IDEA设置中的HelloRead配置页面
 */
class FontSettingsAction : AnAction(), DumbAware {
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        
        // 打开IDEA设置中的HelloRead配置页面
        ShowSettingsUtil.getInstance().showSettingsDialog(
            project,
            "com.github.xucux.read.setting.HelloReadSettingComponent"
        )
    }
}