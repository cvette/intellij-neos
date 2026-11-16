/*
 *  IntelliJ IDEA plugin to support the Neos CMS.
 *  Copyright (C) 2016  Christian Vette
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.vette.idea.neos.translation

import com.intellij.ide.highlighter.HtmlFileType
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.lang.PhpFileType
import de.vette.idea.neos.NeosProjectService
import de.vette.idea.neos.indexes.NodeTypesYamlFileIndex
import de.vette.idea.neos.lang.fusion.FusionFileType
import de.vette.idea.neos.translation.yaml.NodeTypeTranslations
import de.vette.idea.neos.translation.yaml.YamlTranslationUsage
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * Finds the usages of a translation unit in Fusion, AFX, YAML, PHP and Fluid templates.
 *
 * The word index narrows the search down to the files that contain the words of the id. Labels of node types
 * ("i18n") do not contain the id, their definitions are found through the index of node types.
 */
class TranslationUsageSearcher : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {

    override fun processQuery(parameters: ReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>) {
        val project = parameters.project
        if (DumbService.isDumb(project) || !NeosProjectService.isEnabled(project)) return

        val tag = XliffIndexing.unitTagOf(parameters.elementToSearch) ?: return
        val id = XliffIndexing.translationIdOf(tag) ?: return
        val scope = parameters.effectiveSearchScope

        val reported = HashSet<PsiElement>()
        fun report(reference: TranslationUnitReference): Boolean =
            !reported.add(reference.element) || consumer.process(reference)

        val psiManager = PsiManager.getInstance(project)
        for (file in candidateFiles(project, scope, id)) {
            ProgressManager.checkCanceled()
            val psiFile = psiManager.findFile(file) ?: continue
            if (!processOccurrences(psiFile, id, ::report)) return
        }

        for (file in nodeTypeFiles(project, id, scope)) {
            ProgressManager.checkCanceled()
            val psiFile = psiManager.findFile(file) ?: continue
            for (scalar in PsiTreeUtil.findChildrenOfType(psiFile, YAMLScalar::class.java)) {
                if (scalar.textValue.trim() != NodeTypeTranslations.I18N) continue
                val reference = unitReferences(scalar).firstOrNull { it.translationId == id } ?: continue
                if (!report(reference)) return
            }
        }
    }

    /**
     * Looks at every occurrence of the id in the text of the file, also inside of injected code like AFX
     */
    private fun processOccurrences(
        psiFile: PsiFile,
        id: TranslationId,
        report: (TranslationUnitReference) -> Boolean,
    ): Boolean {
        val text = psiFile.viewProvider.contents
        val injectedLanguageManager = InjectedLanguageManager.getInstance(psiFile.project)

        var offset = text.indexOf(id.id)
        while (offset >= 0) {
            val leaf = injectedLanguageManager.findInjectedElementAt(psiFile, offset) ?: psiFile.findElementAt(offset)
            val reference = generateSequence(leaf) { it.parent }
                .take(MAX_REFERENCE_DEPTH)
                .flatMap { unitReferences(it) }
                .firstOrNull { it.translationId == id }
            if (reference != null && !report(reference)) return false

            offset = text.indexOf(id.id, offset + id.id.length)
        }
        return true
    }

    private fun unitReferences(element: PsiElement): List<TranslationUnitReference> =
        element.references.filterIsInstance<TranslationUnitReference>()

    private fun candidateFiles(project: Project, scope: SearchScope, id: TranslationId): Collection<VirtualFile> = when (scope) {
        is GlobalSearchScope -> if (hasWords(id.id)) indexedCandidates(project, scope, id) else allCandidates(scope)
        is LocalSearchScope -> scope.virtualFiles.toList()
        else -> emptyList()
    }

    /**
     * The files that contain the words of the id, according to the word index: Fusion files (see
     * [de.vette.idea.neos.lang.fusion.usages.FusionWordsScanner]), the YAML configuration of Neos, PHP files
     * and Fluid templates.
     */
    private fun indexedCandidates(project: Project, scope: GlobalSearchScope, id: TranslationId): Collection<VirtualFile> {
        val files = ArrayList<VirtualFile>()
        PsiSearchHelper.getInstance(project).processCandidateFilesForText(scope, UsageSearchContext.ANY, true, id.id) { file ->
            ProgressManager.checkCanceled()
            when (file.fileType) {
                FusionFileType.INSTANCE, PhpFileType.INSTANCE, HtmlFileType.INSTANCE -> files.add(file)
                YAMLFileType.YML -> if (YamlTranslationUsage.isNeosConfiguration(file)) files.add(file)
            }
            true
        }
        return files
    }

    /**
     * All Fusion files and the YAML configuration of Neos, for ids the word index cannot be asked for
     * because they consist of punctuation only.
     */
    private fun allCandidates(scope: GlobalSearchScope): Collection<VirtualFile> =
        FileTypeIndex.getFiles(FusionFileType.INSTANCE, scope) +
                FileTypeIndex.getFiles(YAMLFileType.YML, scope).filter(YamlTranslationUsage::isNeosConfiguration)

    private fun hasWords(text: String): Boolean = text.any(Character::isJavaIdentifierPart)

    /**
     * The definitions of the node type a source like "NodeTypes/Content/Text" belongs to
     */
    private fun nodeTypeFiles(project: Project, id: TranslationId, scope: SearchScope): Collection<VirtualFile> {
        if (!id.source.startsWith(NODE_TYPES_SOURCE)) return emptyList()

        val localName = id.source.removePrefix(NODE_TYPES_SOURCE).replace('/', '.')
        val globalScope = scope as? GlobalSearchScope ?: GlobalSearchScope.allScope(project)
        val files = FileBasedIndex.getInstance()
            .getContainingFiles(NodeTypesYamlFileIndex.KEY, "${id.packageKey}:$localName", globalScope)

        return if (scope is LocalSearchScope) files.filter { it in scope.virtualFiles } else files
    }

    companion object {
        private const val MAX_REFERENCE_DEPTH = 4
        private const val NODE_TYPES_SOURCE = "NodeTypes/"
    }
}
