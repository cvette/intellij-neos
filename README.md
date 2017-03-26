IntelliJ IDEA / PhpStorm Neos Plugin
====================================
[![Build Status](https://travis-ci.org/cvette/intellij-neos.svg?branch=master)](https://travis-ci.org/cvette/intellij-neos)
[![Version](https://christianvette.de/badge/9362/version)](https://plugins.jetbrains.com/plugin/9362)
[![Downloads](https://christianvette.de/badge/9362/downloads)](https://plugins.jetbrains.com/plugin/9362)
[![Downloads last month](https://christianvette.de/badge/9362/last-month)](https://plugins.jetbrains.com/plugin/9362)
[![Gitter](https://img.shields.io/gitter/room/nwjs/nw.js.svg)](https://gitter.im/intellij-neos/Lobby)

This is a **work in progress**. There may be performance and stability problems.

Features
--------

* Fusion / TypoScript2 language support
 * Configurable syntax highlighting
 * Basic formatting
 * Brace matching
 * Breadcrumb navigation
 * Code folding
 * Structure view
 * EEL helper references (Ctrl+Click navigation to class/method)
 * Prototype references (Ctrl+Click navigation to prototype definition)
* Completion for node type definitions
* "Go to definition" for node types / supertypes / constraints in NodeTypes.yaml

Usage
-----
You can install the plugin by going to `Settings > Plugins > Browse repositories` and searching for "Neos".

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

License
-------
Copyright (C) 2016  Christian Vette

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
