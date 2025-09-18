package com.github.xucux.read.util

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications

/**
 * <br>
 * @since 2025/9/17
 * @author admin
 * @version 1.0.0
 */
class PopNotifyUtil {

    companion object {
        /**
         * 弹出消息通知
         * @param title 标题
         * @param content 内容
         */
        fun notify(title: String, content: String) {
            val notification = Notification(
                "HelloRead Plugin Notifications",
                title,
                content,
                NotificationType.WARNING
            )
            
            com.intellij.openapi.project.ProjectManager.getInstance().openProjects.firstOrNull()?.let { project ->
                Notifications.Bus.notify(notification, project)
            } ?: run {
                Notifications.Bus.notify(notification)
            }
        }
    }

}