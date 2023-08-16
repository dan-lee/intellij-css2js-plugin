package com.danlee.css2js

import com.intellij.lang.javascript.TypeScriptFileType
import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.lang.javascript.psi.JSProperty
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.css.CssFile
import com.intellij.psi.css.CssFileType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilBase
import java.awt.datatransfer.DataFlavor
import java.util.regex.Pattern

fun String.kebabToCamelCase(): String {
  val pattern = "-+([a-z])".toRegex()
  return replace(pattern) { it.groupValues[1].uppercase() }
}

fun String.camelToKebabCase(): String {
  return buildString {
    if (this@camelToKebabCase.first().isUpperCase()) {
      append('-')
    }
    this@camelToKebabCase.forEach { char ->
      if (char.isUpperCase()) {
        append('-')
        append(char.lowercaseChar())
      } else {
        append(char)
      }
    }
  }
}

class Util {
  companion object {
    fun getCssFile(project: Project, cssText: String): CssFile? {
      val psiFileFactory = PsiFileFactory.getInstance(project)

      var css = cssText

      // simple heuristic to add a selector if it's missing
      if (!css.contains('{')) {
        css = ".foo {\n$css\n}"
      }

      if (css.trim().startsWith('{')) {
        css = ".foo $css"
      }

      if (!css.trim().endsWith('}')) {
        css = "$css\n}"
      }

      val psiFile = psiFileFactory.createFileFromText("tmp.css", CssFileType.INSTANCE, css)

      return psiFile as? CssFile
    }

    fun extractDeclarations(project: Project, text: String): List<Pair<String, String>> {
      val cssFile = getCssFile(project, text) ?: return emptyList()
      val stylesheet = cssFile.stylesheet
      val (ruleset) = stylesheet.rulesets

      val declarations = mutableListOf<Pair<String, String>>()

      ruleset.block?.let { block ->
        block.declarations.forEach { declaration ->
          declaration.value?.text?.let { value ->
            declarations.add(Pair(declaration.propertyName, value))
          }
        }
      }

      return declarations
    }

    fun old_extractDeclarations(cssFile: CssFile): List<Pair<String, String>> {
      val stylesheet = cssFile.stylesheet
      val (ruleset) = stylesheet.rulesets

      val declarations = mutableListOf<Pair<String, String>>()

      ruleset.block?.let { block ->
        block.declarations.forEach { declaration ->
          declaration.value?.text?.let { value ->
            declarations.add(Pair(declaration.propertyName, value))
          }
        }
      }

      return declarations
    }

    fun getCssPropertiesFromObject(project: Project, tsText: String): List<Pair<String, String>> {
      val psiFileFactory = PsiFileFactory.getInstance(project)

      var ts = tsText.trim()
      ts = ts.trimStart('{').trimEnd('}')
      ts = "const foo = {$ts}"

      val psiFile = psiFileFactory.createFileFromText("tmp.ts", TypeScriptFileType.INSTANCE, ts)

      val objectLiteralExpression =
        PsiTreeUtil.findChildOfType(psiFile, JSObjectLiteralExpression::class.java)

//      PsiTreeUtil.collectElements(psiFile) { true }.forEach { println(it) }

      return objectLiteralExpression?.children
        ?.filterIsInstance<JSProperty>()
        ?.map { property ->
          val key = property.name ?: throw IllegalArgumentException("No key found for property")
          val value = (property.value as? JSLiteralExpression)?.value.toString()
          key to value
        } ?: throw IllegalArgumentException("No object literal expression found")
    }

    private fun surround(value: String, by: String): String {
      return "$by$value$by"
    }

    private fun quoteValue(value: String): String {
      val quoteChr = "'"

      if (value.contains('\n')) {
        return surround(value, "`")
      }

      if (value.contains('\'') && value.contains('"')) {
        val escapedValue = value.replace(Regex(quoteChr), """\\$quoteChr""")
        return surround(escapedValue, quoteChr)
      }

      if (value.contains("'")) {
        return surround(value, "\"")
      }

      if (Pattern.matches("^[+-]?\\d*\\.?\\d+([eE][+-]?\\d+)?$", value)) {
        return value
      }

      return surround(value, quoteChr)
    }

    fun printDeclarations(declarations: List<Pair<String, String>>): String {
      return if (declarations.isNotEmpty()) {
        declarations.joinToString(",\n", postfix = ",") { (prop, value) ->
          val prop = if (prop.startsWith("--var")) surround(prop, "'") else prop.kebabToCamelCase()

          "$prop: ${quoteValue(value)}"
        }
      } else {
        ""
      }
    }

    fun printCssProperties(cssProperties: List<Pair<String, String>>, inline: Boolean): String {
      val separator = if (inline) "; " else ";\n"
      return if (cssProperties.isNotEmpty()) {
        cssProperties.joinToString(separator, postfix = ";") { (prop, value) ->
          "${prop.camelToKebabCase()}: $value"
        }
      } else {
        ""
      }
    }

    fun reformat(project: Project, editor: Editor, startOffset: Int, endOffset: Int) {
      val codeStyleManager: CodeStyleManager = CodeStyleManager.getInstance(project)
      val psiFile = PsiUtilBase.getPsiFileInEditor(editor, project)

      codeStyleManager.reformatText(psiFile ?: return, startOffset, endOffset)
      editor.caretModel.moveToOffset(endOffset)
    }

    fun replaceAtCaret(e: AnActionEvent, replace: (project: Project, text: String) -> String) {
      val project = e.project ?: return
      val editor = e.getRequiredData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR)
      val caretModel = editor.caretModel

      val clipboard = CopyPasteManager.getInstance().contents ?: return
      val clipboardText = clipboard.getTransferData(DataFlavor.stringFlavor) as String
      val modifiedText = replace(project, clipboardText) ?: return

      WriteCommandAction.runWriteCommandAction(project) {
        val startOffset = caretModel.offset

        EditorModificationUtil.insertStringAtCaret(editor, modifiedText, false, true)
        val endOffset = caretModel.offset

        reformat(project, editor, startOffset, endOffset)
      }
    }

    fun replaceModifiedContent(input: String, modify: (String) -> String): String {
      val firstBracketIndex = input.indexOf('{')
      val lastBracketIndex = input.lastIndexOf('}')

      val before: String
      val originalContent: String
      val after: String

      if (firstBracketIndex != -1 && lastBracketIndex != -1) {
        before = input.substring(0, firstBracketIndex + 1)
        originalContent = input.substring(firstBracketIndex + 1, lastBracketIndex).trim()
        after = input.substring(lastBracketIndex)
      } else if (firstBracketIndex != -1) {
        before = input.substring(0, firstBracketIndex + 1)
        originalContent = input.substring(firstBracketIndex + 1).trim()
        after = ""
      } else if (lastBracketIndex != -1) {
        before = ""
        originalContent = input.substring(0, lastBracketIndex).trim()
        after = input.substring(lastBracketIndex)
      } else {
        before = ""
        originalContent = input
        after = ""
      }

      val modifiedContent = modify(originalContent)

      return "$before\n$modifiedContent\n$after"
    }
  }
}
