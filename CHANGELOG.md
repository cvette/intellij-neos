<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# IntelliJ Neos Changelog

## [Unreleased]

### Added

- "Extract AFX Component" refactoring: moves the selected AFX or the tag at the caret into a new component prototype and passes on the used props
- JSON schema for `Routes.*.yaml` files
- Eel support for `${...}` values in node type definitions and other YAML configuration files
- Context-aware completion in Eel expressions: helpers, helper methods, FlowQuery operations, context variables and prototype properties
- Completion of prototype properties in Fusion blocks, including inherited properties
- Completion of node type names in node type definitions: super types, constraints, child node types and editor options
- Index of XLIFF translation units by package, source and id, including plural forms and labels overridden by other packages
- Go to the translation unit, XLIFF file or package from the arguments of `I18n.translate()` and `I18n.id()` in Fusion, AFX and YAML
- Completion of packages, sources and ids in the arguments of the translation helper
- Quick documentation that shows a label in all locales, at its usage and on the id of a translation unit
- Go to the translation unit from `i18n` values in node type definitions and from shorthand ids (`Package:Source:id`) in node type definitions and other configuration, with a gutter icon that shows the label
- Completion of shorthand translation ids in YAML configuration
- Find usages of a translation unit in Fusion, AFX and YAML, including the labels of node types that are set to `i18n`
- Rename of a translation unit id in all locales and at its usages
- Gutter icon on translation units that leads to the same unit in other locales
- Translation units in "Go to Symbol", by their shorthand id
- Inspections for labels used in Fusion, AFX and YAML: translation not found and translation missing in some locales, with quick-fixes that create the translation units and XLIFF files
- Inspection for texts in node type definitions that are not translated (disabled by default)
- Inspections for XLIFF files: duplicate ids, units without translation, target language that does not match the directory, units out of sync with other locales, and unused units (disabled by default)
- Folding of translation helper calls to the text of the label, with an option to collapse them by default
- Unused method suppression for `inject*` methods and for controllers that extend a custom base controller

### Fixed

- "Translate NodeType" created translation units with ids Neos does not look up, e.g. `properties.title.ui.label` instead of `properties.title`. Existing units are renamed and keep their translations
- "Translate NodeType" replaced Eel expressions and labels of other sources with `i18n`
- Node type references were added to root level keys of all YAML files
- Quoted PHP class names in YAML files were not resolved
- Template lookup for controller actions containing "Action" or "Controller" in their name
- "Go to template" line marker was active in projects without enabled Neos support
- Performance warning caused by line markers for Fusion prototypes and controller templates not being registered on leaf elements
- Precedence of `&&` and `||` in Eel expressions
- Braces in Eel string literals ended the expression in Fusion and AFX
- Fusion comments on the last line of a file and directly after an opening brace
- A backslash before the closing backtick kept a Fusion DSL value open
- Typing `${` inserts the closing brace of the Eel expression everywhere, not only at the end of a file
- Incomplete Eel expressions are parsed as far as possible instead of being marked as an error as a whole
- Readable error messages for syntax errors in Eel expressions

### Removed

- Keyboard shortcut for the internal error reporting test action

## [1.28.0] - 2026-11-13

### Added

- Compatibility with 2026.3

## [1.27.0] - 2026-07-31

### Added

- Compatibility with 2026.2

## [1.26.0] - 2026-03-27

### Added

- Compatibility with 2026.1

## [1.25.0] - 2025-12-11

### Added

- Compatibility with 2025.3

## [1.24.1] - 2025-08-14

### Added

- Added refactor action to move Fusion prototypes

## [1.24.0] - 2025-05-26

### Added

- Added refactor action to move Fusion prototypes

## [1.23.0] - 2024-12-01

### Added

- Compatibility with 2024.3
- Action to create and update translations for a node type

## [1.21.0] - 2024-06-01

- Add JSON schema for node type presets (needs separate settings file named `Settings.Presets.*`)

## [1.20.1] - 2024-04-07

- Compatibility with 2024.1.*

## [1.20.0] - 2023-12-07

- Compatibility with 2023.3.*

## [1.19.0] - 2023-07-31

### Added

- Add XLIFF file type
- Allow to exclude symlinked packages automatically
- Improved detection of Neos projects by using the composer.json
- Compatibility with 2023.2 (eap)

## [1.18.0] - 2023-06-12

### Added

- Add setting for AFX attribute value completion

### Fixed

- AFX tag completion and inspection suppression

## [1.17.0] - 2023-06-07

- Allow quotes in AFX attribute names

## [1.16.0] - 2023-04-03

- Make compatible with 231.*

## [1.14.0]

### Added

- Improved breadcrumbs for Fusion

## [1.13.0]

### Added

- Autocompletion for Fusion paths in EEL

### Removed

- Support for TYPO3.Neos namespace
- Support for .ts2 file extension

### Fixed

- Recognize subcontext Node Type definitions

## [1.12.2]

- Compatibility with 2022.1

### Fixed

- NullPointer Exception in NodeTypeReferenceContributor

## [1.12.1]

### Fixed

- NullPointer Exception in NodeMigrationYamlSchemaProvider

## [1.12.0]

- Compatibility with 2021.3

## [1.11.0]

### Added

- Inlay parameter hints for EEL helper methods in Fusion

## [1.10.0]

### Added

- Support NodeTypes in subfolders
- Suppress PHP unused declaration inspection for Flow magic methods

## [1.9.0]

### Added

- Provide JSON schema for cache configuration (https://github.com/Sebobo/Shel.Neos.Schema)

### Fixed

- NullPointerException when opening files with embedded YAML

## [1.8.1]

### Added

- Provide JSON schema for node migrations (https://github.com/Sebobo/Shel.Neos.Schema)

## [1.8.0]

### Added

- AFX tag name synchronization
- Symbol contributors for node types and Fusion prototypes

## [1.7.1]

### Fixed

- Empty EEL in AFX tag content no longer throws an exception
- NullPointerException when using node type references

## [1.7.0]

### Added

- AFX is now a custom language. If you changed the injection settings, make sure "Neos Afx" is used
- Handle EEL in AFX
- Provide JSON schema for Node Types (https://github.com/Sebobo/Shel.Neos.Schema)
- Errors can now be submitted
- Node type references in childNodes and editorOptions.nodeTypes
- Suppress unknown attribute inspection for AFX meta attributes and prototypes
- Suppress unknown namespace inspection for AFX
- Suppress unknown target inspection for AFX

## [1.6.1]

### Fixed

- Exception when using completion for prototypes

## [1.6.0]

### Added

- Rename refactoring for Fusion prototypes
- Find usages of Fusion prototypes

### Fixed

- Index split settings files (Settings.*.yaml)
- Weird behavior when trying to navigate to prototype definitions (sometimes only the namespace was clickable)

## [1.5.0]

### Added

- Completion for inline editor options in node type definitions
- Live template context for Fusion file type

[Unreleased]: https://github.com/cvette/intellij-neos/compare/v1.28.0...HEAD
[1.28.0]: https://github.com/cvette/intellij-neos/compare/v1.27.0...v1.28.0
[1.27.0]: https://github.com/cvette/intellij-neos/compare/v1.26.0...v1.27.0
[1.26.0]: https://github.com/cvette/intellij-neos/compare/v1.25.0...v1.26.0
[1.25.0]: https://github.com/cvette/intellij-neos/compare/v1.24.1...v1.25.0
[1.24.1]: https://github.com/cvette/intellij-neos/compare/v1.24.0...v1.24.1
[1.24.0]: https://github.com/cvette/intellij-neos/compare/v1.23.0...v1.24.0
[1.23.0]: https://github.com/cvette/intellij-neos/compare/v1.21.0...v1.23.0
[1.21.0]: https://github.com/cvette/intellij-neos/compare/v1.20.1...v1.21.0
[1.20.1]: https://github.com/cvette/intellij-neos/compare/v1.20.0...v1.20.1
[1.20.0]: https://github.com/cvette/intellij-neos/compare/v1.19.0...v1.20.0
[1.19.0]: https://github.com/cvette/intellij-neos/compare/v1.18.0...v1.19.0
[1.18.0]: https://github.com/cvette/intellij-neos/compare/v1.17.0...v1.18.0
[1.17.0]: https://github.com/cvette/intellij-neos/compare/v1.16.0...v1.17.0
[1.16.0]: https://github.com/cvette/intellij-neos/compare/v1.14.0...v1.16.0
[1.14.0]: https://github.com/cvette/intellij-neos/compare/v1.13.0...v1.14.0
[1.13.0]: https://github.com/cvette/intellij-neos/compare/v1.12.2...v1.13.0
[1.12.2]: https://github.com/cvette/intellij-neos/compare/v1.12.1...v1.12.2
[1.12.1]: https://github.com/cvette/intellij-neos/compare/v1.12.0...v1.12.1
[1.12.0]: https://github.com/cvette/intellij-neos/compare/v1.11.0...v1.12.0
[1.11.0]: https://github.com/cvette/intellij-neos/compare/v1.10.0...v1.11.0
[1.10.0]: https://github.com/cvette/intellij-neos/compare/v1.9.0...v1.10.0
[1.9.0]: https://github.com/cvette/intellij-neos/compare/v1.8.1...v1.9.0
[1.8.1]: https://github.com/cvette/intellij-neos/compare/v1.8.0...v1.8.1
[1.8.0]: https://github.com/cvette/intellij-neos/compare/v1.7.1...v1.8.0
[1.7.1]: https://github.com/cvette/intellij-neos/compare/v1.7.0...v1.7.1
[1.7.0]: https://github.com/cvette/intellij-neos/compare/v1.6.1...v1.7.0
[1.6.1]: https://github.com/cvette/intellij-neos/compare/v1.6.0...v1.6.1
[1.6.0]: https://github.com/cvette/intellij-neos/compare/v1.5.0...v1.6.0
[1.5.0]: https://github.com/cvette/intellij-neos/commits/v1.5.0
