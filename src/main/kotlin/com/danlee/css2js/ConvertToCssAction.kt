package com.danlee.css2js

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction

class ConvertToCssAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project
    val editor = e.getData(CommonDataKeys.EDITOR) ?: return
    val document = editor.document

    val selectionModel = editor.selectionModel
    val selectedText = selectionModel.selectedText

    if (selectedText.isNullOrEmpty() || project == null) {
      return
    }

    val replacement = Util.replaceModifiedContent(selectedText) { extracted ->
      val declarations = Util.getCssPropertiesFromObject(project, extracted)

      Util.printCssProperties(declarations, false)
    }

    WriteCommandAction.runWriteCommandAction(project) {
      val startOffset: Int = selectionModel.selectionStart
      val endOffset: Int = selectionModel.selectionEnd

      document.replaceString(startOffset, endOffset, replacement)

      val finalEndOffset = startOffset + replacement.length
      Util.reformat(project, editor, startOffset, finalEndOffset)
    }
  }
}
