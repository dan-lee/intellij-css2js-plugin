package com.danlee.css2js

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class PasteObjectAsCssAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val editor = e.getRequiredData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR)

    Util.replaceAtCaret(project, editor) { text ->
      val cssProps = Util.getCssPropertiesFromObject(project, text)

      Util.printCssProperties(cssProps, inline = false)
    }
  }
}
