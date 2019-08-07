Developing
----------

- Ensure you have Java 8 SDK *newer than* Java 8.112 (newer than what IntelliJ displays in "About IntelliJ IDEA" dialog)
- clone the repository
- run `./gradlew runIde` - for the first time, this will take quite a while, as it downloads IntelliJ.
- First, go to "Configure -> Project Defaults -> Project Structure" (or "File -> Project Structure"),
  then ensure underneath "Platform Settings" -> "SDKs", there is a SDK for the JDK 1.8 version you have. If not, create it.
- to open the project in IntelliJ, Use "File -> Open" in IntelliJ and select the "build.gradle" in here.
  - then, select "Use Auto-Import"; and select JDK 1.8 (newer than rev 112).
- to run the project from within IDEA, go to "View -> Tool Windows -> Gradle", and in the Gradle tool window, do the following:
  - expand "Tasks -> intellij -> runIde"
  - right-click it and select "Run"
  - (you can also select "debug" to run it in debug mode!)
- in debug mode, you can hot-reload classes by choosing "Run -> Reload changed classes" as explained on https://www.jetbrains.com/help/idea/2016.3/reloading-classes.html
- have fun developing :)

Development Tips and Tricks
---------------------------

- install GrammarKit if you want to work on the parser
- to log, use `NeosProjectComponent.getLogger().warn("Foo")`
  and run `tail -f build/idea-sandbox/system/log/idea.log`.
- If you use debug level, you have to add the "Neos" debug category via Help > Debug Log Settings.


Parsing
--------------------------

EEL in Fusion (Fusion Language):
- Outer Language: Fusion
- Inner Language: Eel
- Two-Layer Lexer: FusionEelLexer

EEL in HTML (AFX Language):
- Outer Language: HTML
- Inner Language: Eel
- Two-Layer Lexer: AfxEelLexer