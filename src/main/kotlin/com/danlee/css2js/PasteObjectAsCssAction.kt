package com.danlee.css2js

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class PasteObjectAsCssAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    Util.replaceAtCaret(e) { project, text ->
      val cssProps = Util.getCssPropertiesFromObject(project, text)

      Util.printCssProperties(cssProps, inline = false)
    }
  }
}
