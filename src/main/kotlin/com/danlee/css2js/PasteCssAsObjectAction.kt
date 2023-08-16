package com.danlee.css2js

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class PasteCssAsObjectAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    Util.replaceAtCaret(e) { project, text ->
      val declarations = Util.extractDeclarations(project, text)

      Util.printDeclarations(declarations)
    }
  }
}
