package de.vette.idea.neos.actions;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.json.psi.JsonFile;
import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
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
import de.vette.idea.neos.util.ComposerUtil;
import de.vette.idea.neos.util.NeosUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.yaml.YAMLElementGenerator;
import org.jetbrains.yaml.psi.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

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

        List<YAMLKeyValue> pairs = getTranslateablePaths(virtualFile, project);
        String nodeType = extractNodeType(pairs);

        PsiDirectory psiDirectory = PsiManager.getInstance(project).findDirectory(virtualFile.getParent());
        JsonFile composerManifest = ComposerUtil.getComposerManifest(psiDirectory);
        PsiDirectory packageDirectory;
        if (composerManifest == null) {
            return;
        }

        String packageName = ComposerUtil.getPackageKey(composerManifest);
        packageDirectory = composerManifest.getContainingDirectory();
        Collection<String> locales = Settings.getInstance(project).locales;

        if (locales.isEmpty()) {
            showNotification(project);
            return;
        }

        final Optional<String> firstLocaleOpt = locales.stream().findFirst();
        if (firstLocaleOpt.isEmpty()) {
            return;
        }

        final String sourceLocale = firstLocaleOpt.get();

        Properties properties = new Properties();
        properties.setProperty("NEOS_PACKAGE_NAME", packageName);
        properties.setProperty("SOURCE_LANGUAGE", sourceLocale);

        HashMap<String, YAMLKeyValue> yamlPairsByTransId = extractNodeTypeTranslationIds(pairs);

        WriteCommandAction.runWriteCommandAction(project, "Translate NodeType", "", () -> {
            for (String locale : locales) {
                PsiDirectory translationDir = createTranslationDirectories(packageDirectory, locale, nodeType);
                processTranslationFile(project, translationDir, locale, sourceLocale, yamlPairsByTransId, properties, nodeType);
            }

            // replace text values in node type defintion by "i18n"
            yamlPairsByTransId.forEach((transId, yamlKeyValue) -> {
                if (!yamlKeyValue.getValueText().equals("i18n")) {
                    YAMLKeyValue newKeyValue = YAMLElementGenerator.getInstance(project).createYamlKeyValue(yamlKeyValue.getKeyText(), "i18n");
                    yamlKeyValue.replace(newKeyValue);
                }
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

    private void processTranslationFile(
            Project project,
            PsiDirectory dir,
            String locale,
            String sourceLocale,
            Map<String, YAMLKeyValue> yamlPairsByTransId,
            Properties properties,
            String nodeType
    ) {
        String fileName = getFileName(nodeType);

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

    private String getFileName(String nodeType) {
        String[] nodeTypeParts = nodeType.split("\\.");
        return nodeTypeParts[nodeTypeParts.length - 1];
    }

    private String extractNodeType(List<YAMLKeyValue> pairs) {
        String nodeType = getTranslationKeyParts(pairs.get(0)).get(0);
        String[] nodeTypeParts = nodeType.split(":");
        return (nodeTypeParts.length == 2) ? nodeTypeParts[1] : nodeType;
    }

    private PsiDirectory createTranslationDirectories(PsiDirectory baseDir, String locale, String nodeType) throws IncorrectOperationException {
        PsiDirectory dir = createSubdirectory(baseDir, "Resources");
        dir = createSubdirectory(dir, "Private");
        PsiDirectory translationsDir = createSubdirectory(dir, "Translations");
        dir = createSubdirectory(translationsDir, locale);
        dir = createSubdirectory(dir, "NodeTypes");

        String[] nodeTypeParts = nodeType.split("\\.");
        for (int i = 0; i < nodeTypeParts.length - 1; i++) {
            dir = createSubdirectory(dir, nodeTypeParts[i]);
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

    private LinkedHashMap<String, YAMLKeyValue> extractNodeTypeTranslationIds(List<YAMLKeyValue> pairs) {
        LinkedHashMap<String, YAMLKeyValue> nodeTypeIds = new LinkedHashMap<>();
        for (YAMLKeyValue pair : pairs) {
            List<String> path = getTranslationKeyParts(pair);
            path = path.subList(1, path.size()); // removes the node type name
            String id = String.join(".", path);
            nodeTypeIds.put(id, pair);
        }
        return nodeTypeIds;
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

    private static List<YAMLKeyValue> getTranslateablePaths(VirtualFile virtualFile, Project project) {
        @NotNull PsiManager psiManager = PsiManager.getInstance(project);
        PsiFile psiFile = psiManager.findFile(virtualFile);

        @NotNull Collection<YAMLKeyValue> yamlKeyValuePairs = PsiTreeUtil.findChildrenOfType(psiFile, YAMLKeyValue.class);

        List<YAMLKeyValue> pathsToTranslate = new Vector<>();
        for (YAMLKeyValue pair : yamlKeyValuePairs) {
            String value = pair.getValueText();
            String key = pair.getKeyText();

            if (value.equals("i18n")) {
                pathsToTranslate.add(pair);
                continue;
            }

            // *.label
            if (key.equals("label")) {
                YAMLKeyValue parentPair = getParentKeyValue(pair);

                // *.ui.label
                if (parentPair != null && parentPair.getKeyText().equals("ui")) {
                    pathsToTranslate.add(pair);
                    continue;
                }

                // *.groups|tabs|views.*.label
                YAMLKeyValue parentParentPair = getParentKeyValue(parentPair);
                if (parentParentPair == null) {
                    continue;
                }

                String keyText =  parentParentPair.getKeyText();
                if (keyText.equals("groups") || keyText.equals("views") || keyText.equals("tabs")) {
                    pathsToTranslate.add(pair);
                }

                continue;
            }

            // *.help.message
            if (key.equals("message")) {
                if (hasParentYamlKeyValueText(pair, "help")) {
                    pathsToTranslate.add(pair);
                }

                continue;
            }

            // *.editorOptions.placeholder
            if (key.equals("placeholder")) {
                if (hasParentYamlKeyValueText(pair, "editorOptions")) {
                    pathsToTranslate.add(pair);
                }
            }
        }

        return pathsToTranslate;
    }

    @Nullable
    private static YAMLKeyValue getParentKeyValue(YAMLKeyValue pair) {
        AtomicReference<YAMLKeyValue> keyValue = new AtomicReference<>();
        Optional.of(pair).map(YAMLKeyValue::getParent).map(PsiElement::getParent).ifPresent(psiElement -> {
            if (!(psiElement instanceof YAMLKeyValue)) {
                return;
            }

            keyValue.set((YAMLKeyValue) psiElement);
        });

        return keyValue.get();
    }

    public static boolean hasParentYamlKeyValueText(YAMLKeyValue pair, String text) {
        YAMLKeyValue parentPair = getParentKeyValue(pair);
        if (parentPair == null) {
            return false;
        }

        return parentPair.getKeyText().equals(text);
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
            throw new IncorrectOperationException("This filename is ignored (Settings | Editor | FileDomElement Types | Ignore files and folders)");
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
