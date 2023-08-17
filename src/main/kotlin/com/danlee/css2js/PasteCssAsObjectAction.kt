package com.danlee.css2js

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class PasteCssAsObjectAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val editor = e.getRequiredData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR)
    Util.replaceAtCaret(project, editor) { text ->
      val declarations = Util.extractDeclarations(project, text)

      Util.printDeclarations(declarations)
    }
  }
}
