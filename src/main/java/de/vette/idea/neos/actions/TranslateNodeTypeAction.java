package de.vette.idea.neos.actions;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.json.psi.JsonFile;
import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.xml.DomFileElement;
import com.intellij.util.xml.DomManager;
import de.vette.idea.neos.Settings;
import de.vette.idea.neos.lang.xliff.XliffDomElement;
import de.vette.idea.neos.lang.xliff.XliffFileType;
import de.vette.idea.neos.translation.TranslationId;
import de.vette.idea.neos.translation.yaml.NodeTypeTranslations;
import de.vette.idea.neos.util.ComposerUtil;
import de.vette.idea.neos.util.NeosUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLElementGenerator;
import org.jetbrains.yaml.psi.*;

import java.io.IOException;
import java.util.*;

public class TranslateNodeTypeAction extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        @Nullable Project project = getEventProject(e);
        @Nullable VirtualFile virtualFile = e.getDataContext().getData(PlatformDataKeys.VIRTUAL_FILE);

        if (project == null || virtualFile == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        if (!Settings.getInstance(project).pluginEnabled || !ActionPlaces.isPopupPlace(e.getPlace())) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        if (!NeosUtil.isNodeTypeDefinition(virtualFile)) {
            e.getPresentation().setEnabledAndVisible(false);
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        @Nullable VirtualFile virtualFile = e.getDataContext().getData(PlatformDataKeys.VIRTUAL_FILE);
        @Nullable Project project = getEventProject(e);

        if (project == null || virtualFile == null) {
            return;
        }

        translate(project, virtualFile);
    }

    /**
     * Writes the labels of the node types in the file to XLIFF files of all configured locales
     * and replaces them with "i18n"
     */
    public void translate(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        Collection<String> locales = Settings.getInstance(project).locales;

        if (locales.isEmpty()) {
            showNotification(project);
            return;
        }

        if (FileTypeRegistry.getInstance().getFileTypeByExtension("xlf") != XliffFileType.INSTANCE) {
            showFileTypeNotification(project);
            return;
        }

        List<YAMLKeyValue> pairs = getTranslateablePaths(virtualFile, project);
        if (pairs.isEmpty()) {
            return;
        }

        PsiDirectory psiDirectory = PsiManager.getInstance(project).findDirectory(virtualFile.getParent());
        JsonFile composerManifest = ComposerUtil.getComposerManifest(psiDirectory);
        PsiDirectory packageDirectory;
        if (composerManifest == null) {
            return;
        }

        packageDirectory = composerManifest.getContainingDirectory();

        final Optional<String> firstLocaleOpt = locales.stream().findFirst();
        if (firstLocaleOpt.isEmpty()) {
            return;
        }

        final String sourceLocale = firstLocaleOpt.get();

        Properties properties = new Properties();
        properties.setProperty("SOURCE_LANGUAGE", sourceLocale);

        HashMap<String, PairsByTransId> yamlPairsByTransId = extractNodeTypeTranslationIds(pairs);

        WriteCommandAction.runWriteCommandAction(project, "Translate NodeType", "", () -> {
            yamlPairsByTransId.forEach((nodeType, pairsByTransId) -> {
                // Neos looks the labels up in the package the node type name starts with, in the source
                // "NodeTypes/<name>". The package is written to the file, so that it also applies if the node type
                // is defined in another package.
                TranslationId nodeTypeLabels = NodeTypeTranslations.translationId(nodeType, "");
                String source = nodeTypeLabels.getSource();
                properties.setProperty("NEOS_PACKAGE_NAME", nodeTypeLabels.getPackageKey());

                for (String locale : locales) {
                    PsiDirectory translationDir = createTranslationDirectories(packageDirectory, locale, source);
                    processTranslationFile(project, translationDir, locale, sourceLocale, pairsByTransId, properties, source);
                }

                // replace text values in node type definition by "i18n"
                pairsByTransId.forEach((transId, yamlKeyValue) -> {
                    if (!yamlKeyValue.getValueText().equals("i18n")) {
                        YAMLKeyValue newKeyValue = YAMLElementGenerator.getInstance(project).createYamlKeyValue(yamlKeyValue.getKeyText(), "i18n");
                        yamlKeyValue.replace(newKeyValue);
                    }
                });
            });
        });
    }

    private static void showNotification(@NotNull Project project) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("Neos")
                .createNotification("You don't have any locales configured", NotificationType.ERROR)
                .addAction(new NotificationAction("Configure locales") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                        ShowSettingsUtil.getInstance().showSettingsDialog(project, "Frameworks");

                    }
                }).notify(project);
    }

    private static void showFileTypeNotification(@NotNull Project project) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("Neos")
                .createNotification("Please make sure that the file type \"XML Localization Interchange File Format\" is associated with the file name pattern \".xlf\".", NotificationType.ERROR)
                .addAction(new NotificationAction("Configure file types") {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
                        ShowSettingsUtil.getInstance().showSettingsDialog(project, "File types");

                    }
                }).notify(project);
    }

    private void processTranslationFile(
            Project project,
            PsiDirectory dir,
            String locale,
            String sourceLocale,
            Map<String, YAMLKeyValue> yamlPairsByTransId,
            Properties properties,
            String source
    ) {
        String fileName = getFileName(source);

        properties.setProperty("TARGET_LANGUAGE", "");
        if (!locale.equals(sourceLocale)) {
            properties.setProperty("TARGET_LANGUAGE", locale);
        }

        try {
            fileName = fileName.concat(XliffFileType.DOT_DEFAULT_EXTENSION);
            createOrUpdateFileInDirectory(project, dir, fileName, properties);

            Set<String> existingIds = getExistingIdsForFile(dir, fileName);
            updateTranslationFileContent(project, dir, fileName, existingIds, yamlPairsByTransId, locale, sourceLocale);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * @param source the translation source in path notation, e.g. "NodeTypes/Content/Text"
     */
    private String getFileName(String source) {
        return source.substring(source.lastIndexOf("/") + 1);
    }

    /**
     * @param source the translation source in path notation, e.g. "NodeTypes/Content/Text"
     */
    private PsiDirectory createTranslationDirectories(PsiDirectory baseDir, String locale, String source) throws IncorrectOperationException {
        PsiDirectory dir = createSubdirectory(baseDir, "Resources");
        dir = createSubdirectory(dir, "Private");
        PsiDirectory translationsDir = createSubdirectory(dir, "Translations");
        dir = createSubdirectory(translationsDir, locale);

        String[] sourceParts = source.split("/");
        for (int i = 0; i < sourceParts.length - 1; i++) {
            dir = createSubdirectory(dir, sourceParts[i]);
        }
        return dir;
    }

    private Set<String> getExistingIdsForFile(PsiDirectory dir, String fileName) {
        XmlFile xmlFile = (XmlFile) dir.findFile(fileName);
        if (xmlFile == null) {
            return Collections.emptySet();
        }

        XmlTag rootTag = xmlFile.getRootTag();
        if (rootTag == null) {
            return Collections.emptySet();
        }

        XmlTag fileTag = rootTag.findFirstSubTag("file");
        if (fileTag == null) {
            return Collections.emptySet();
        }

        XmlTag bodyTag = fileTag.findFirstSubTag("body");
        if (bodyTag == null) {
            return Collections.emptySet();
        }

        return getExistingIds(bodyTag);
    }

    class PairsByTransId extends LinkedHashMap<String,YAMLKeyValue> {}

    /**
     * Groups the pairs by node type and by the id Neos looks the label up with
     */
    private LinkedHashMap<String, PairsByTransId> extractNodeTypeTranslationIds(List<YAMLKeyValue> pairs) {
        LinkedHashMap<String, PairsByTransId> nodeTypeIds = new LinkedHashMap<>();

        for (YAMLKeyValue pair : pairs) {
            TranslationId translationId = NodeTypeTranslations.translationId(pair);
            if (translationId == null) {
                continue;
            }

            String nodeType = getTranslationKeyParts(pair).get(0);
            nodeTypeIds.computeIfAbsent(nodeType, key -> new PairsByTransId()).put(translationId.getId(), pair);
        }

        return nodeTypeIds;
    }

    /**
     * Earlier versions used the path of the key as id, e.g. "properties.title.ui.label" instead of "properties.title"
     */
    private static String getLegacyId(YAMLKeyValue pair) {
        List<String> path = getTranslationKeyParts(pair);
        return String.join(".", path.subList(1, path.size()));
    }

    private static Set<String> getExistingIds(XmlTag body) {
        Set<String> ids = new HashSet<>();
        for (XmlTag subTag : body.findSubTags("trans-unit")) {
            XmlAttribute attribute = subTag.getAttribute("id");
            if (attribute == null) {
                continue;
            }

            ids.add(attribute.getValue());
        }

        return ids;
    }

    private static PsiDirectory createSubdirectory(PsiDirectory directory, String name) throws IncorrectOperationException {
        PsiDirectory subDirectory = directory.findSubdirectory(name);

        if (subDirectory != null) {
            return subDirectory;
        }

        subDirectory = directory.createSubdirectory(name);
        return subDirectory;
    }

    /**
     * The keys whose values Neos translates, see {@link NodeTypeTranslations}. Values that are an Eel expression
     * or already refer to a label of another source are left alone.
     */
    private static List<YAMLKeyValue> getTranslateablePaths(VirtualFile virtualFile, Project project) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);

        List<YAMLKeyValue> pathsToTranslate = new ArrayList<>();
        for (YAMLKeyValue pair : PsiTreeUtil.findChildrenOfType(psiFile, YAMLKeyValue.class)) {
            if (!(pair.getValue() instanceof YAMLScalar)) {
                continue;
            }

            String value = pair.getValueText().trim();
            if (value.isEmpty() || value.startsWith("${") || TranslationId.parseShorthand(value) != null) {
                continue;
            }

            if (NodeTypeTranslations.translationId(pair) != null) {
                pathsToTranslate.add(pair);
            }
        }

        return pathsToTranslate;
    }

    private static List<String> getTranslationKeyParts(YAMLKeyValue pair) {
        PsiElement parent = pair.getParent();
        List<String> pathParts = new Vector<>();
        pathParts.add(pair.getKeyText());

        while (!(parent == null || parent instanceof YAMLDocument || parent instanceof YAMLFile)) {
            if (parent instanceof YAMLKeyValue
                    && !(parent.getParent() instanceof YAMLDocument || parent.getParent() instanceof YAMLFile)) {
                String currentKey = ((YAMLKeyValue) parent).getKeyText();
                pathParts.add(currentKey);
            }

            parent = parent.getParent();
        }

        Collections.reverse(pathParts);
        return pathParts;
    }

    private void createOrUpdateFileInDirectory(Project project, PsiDirectory dir, String fileName, Properties properties) throws IOException {
        FileTemplate template = FileTemplateManager.getInstance(project).getInternalTemplate("XLIFF File");
        String content = template.getText(properties);
        if (FileTypeManager.getInstance().isFileIgnored(fileName)) {
            throw new IncorrectOperationException("This filename is ignored (Settings | Editor | File Types | Ignore files and folders)");
        }
        PsiFile psiFile = dir.findFile(fileName);
        if (psiFile == null) {
            psiFile = PsiFileFactory.getInstance(project).createFileFromText(fileName, FileTypeRegistry.getInstance().getFileTypeByFileName(fileName), content);
            dir.add(psiFile);
        }
    }

    private void updateTranslationFileContent(Project project, PsiDirectory dir, String fileName, Set<String> existingIds, Map<String, YAMLKeyValue> yamlPairsByTransId, String locale, String sourceLocale) {
        XmlFile xmlFile = (XmlFile) dir.findFile(fileName);
        if (xmlFile == null) {
            return;
        }

        XmlTag rootTag = xmlFile.getRootTag();
        if (rootTag == null) {
            return;
        }

        XmlTag fileTag = rootTag.findFirstSubTag("file");
        if (fileTag == null) {
            return;
        }

        XmlTag bodyTag = fileTag.findFirstSubTag("body");
        if (bodyTag == null) {
            return;
        }

        existingIds = new HashSet<>(existingIds);
        migrateLegacyIds(bodyTag, yamlPairsByTransId, existingIds);

        Set<String> removedIds = new HashSet<>(existingIds);
        removedIds.removeAll(yamlPairsByTransId.keySet());

        DomManager manager = DomManager.getDomManager(project);
        DomFileElement<XliffDomElement> fileElement = manager.getFileElement(xmlFile, XliffDomElement.class);

        if (fileElement == null) {
            return;
        }

        XliffDomElement root = fileElement.getRootElement();
        removeObsoleteTranslations(root, removedIds);
        addNewTranslations(bodyTag, yamlPairsByTransId, existingIds, sourceLocale, locale);
    }

    /**
     * Renames units that were created with the path of the key as id, so their translations are kept
     */
    private void migrateLegacyIds(XmlTag bodyTag, Map<String, YAMLKeyValue> yamlPairsByTransId, Set<String> existingIds) {
        yamlPairsByTransId.forEach((id, yamlKeyValue) -> {
            String legacyId = getLegacyId(yamlKeyValue);
            if (legacyId.equals(id) || existingIds.contains(id) || !existingIds.contains(legacyId)) {
                return;
            }

            for (XmlTag transUnit : bodyTag.findSubTags("trans-unit")) {
                if (legacyId.equals(transUnit.getAttributeValue("id"))) {
                    transUnit.setAttribute("id", id);
                    existingIds.remove(legacyId);
                    existingIds.add(id);
                    break;
                }
            }
        });
    }

    private void removeObsoleteTranslations(XliffDomElement root, Set<String> removedIds) {
        root.getFiles().forEach(file -> file.getBody().getTransUnits().forEach(transUnit -> {
            if (removedIds.contains(transUnit.getId().getValue())) {
                if (transUnit.getXmlElement() != null) {
                    transUnit.getXmlElement().delete();
                }
            }
        }));
    }

    private void addNewTranslations(XmlTag bodyTag, Map<String, YAMLKeyValue> yamlPairsByTransId, Set<String> existingIds, String sourceLocale, String locale) {
        yamlPairsByTransId.forEach((id, yamlKeyValue) -> {
            if (existingIds.contains(id)) {
                return;
            }

            XmlTag transUnit = bodyTag.createChildTag("trans-unit", null, null, false);
            transUnit.setAttribute("id", id);

            String sourceValue = "";
            if (!yamlKeyValue.getValueText().equals("i18n")) {
                sourceValue = yamlKeyValue.getValueText();
            }

            XmlTag sourceTag = transUnit.createChildTag("source", null, sourceValue, false);
            transUnit.addSubTag(sourceTag, false);

            if (!locale.equals(sourceLocale)) {
                XmlTag target = transUnit.createChildTag("target", null, "", false);
                transUnit.addSubTag(target, false);
            }

            bodyTag.addSubTag(transUnit, false);
        });
    }
}
