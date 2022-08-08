package de.vette.idea.neos.util;

import com.intellij.json.psi.JsonFile;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class ComposerUtilTest extends BasePlatformTestCase {

    public void testExtraPackageKey()
    {
        var manifest = loadComposerFile("package-with-package-key.json");
        var packageKey = ComposerUtil.getPackageKey(manifest);
        assertEquals("Acme.PackageKey", packageKey);
    }

    public void testAutoloadPackageKey()
    {
        var manifest = loadComposerFile("package-with-autoload.json");
        var packageKey = ComposerUtil.getPackageKey(manifest);
        // autoload-order is not reliable in this implementation
        assertEquals("Acme.First.Namespace", packageKey);
    }

    public void testDirectoryStructurePackageKey()
    {
        var manifest = loadComposerFile("package-with-path.json", "Packages/Application/Acme.PathPackage/composer.json");
        var packageKey = ComposerUtil.getPackageKey(manifest);
        assertEquals("Acme.PathPackage", packageKey);
    }

    public void testNamePackageKey()
    {
        var manifest = loadComposerFile("package-with-name.json");
        var packageKey = ComposerUtil.getPackageKey(manifest);
        // a package-key derived from the package name will not get case-converted
        assertEquals("acme.example", packageKey);
    }

    @Override
    protected String getTestDataPath() {
        return "testData/composer";
    }

    private JsonFile loadComposerFile(String fixtureName) {
        return loadComposerFile(fixtureName, "composer.json");
    }

    private JsonFile loadComposerFile(String fixtureName, String filePath)
    {
        var file = myFixture.copyFileToProject(fixtureName, filePath);
        var psiFile = PsiManager.getInstance(getProject()).findFile(file);
        return (JsonFile) psiFile;
    }
}