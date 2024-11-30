<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# IntelliJ Neos Changelog

## Unreleased

### Added
- Compatibility with 2024.3
- Action to create and update translations for a node type

## 1.21.0 - 2024-06-01

- Add JSON schema for node type presets (needs separate settings file named `Settings.Presets.*`)

## 1.20.1 - 2024-04-07

- Compatibility with 2024.1.*

## 1.20.0 - 2023-12-07

- Compatibility with 2023.3.*

## 1.19.0 - 2023-07-31

### Added

- Add XLIFF file type
- Allow to exclude symlinked packages automatically
- Improved detection of Neos projects by using the composer.json
- Compatibility with 2023.2 (eap)

## 1.18.0 - 2023-06-12

### Added

- Add setting for AFX attribute value completion

### Fixed

- AFX tag completion and inspection suppression

## 1.17.0 - 2023-06-07

- Allow quotes in AFX attribute names

## 1.16.0 - 2023-04-03

- Make compatible with 231.*

## 1.14.0

### Added

- Improved breadcrumbs for Fusion

## 1.13.0

### Added

- Autocompletion for Fusion paths in EEL

### Removed

- Support for TYPO3.Neos namespace
- Support for .ts2 file extension

### Fixed

- Recognize subcontext Node Type definitions

## 1.12.2

- Compatibility with 2022.1

### Fixed

- NullPointer Exception in NodeTypeReferenceContributor

## 1.12.1

### Fixed

- NullPointer Exception in NodeMigrationYamlSchemaProvider

## 1.12.0

- Compatibility with 2021.3

## 1.11.0

### Added

- Inlay parameter hints for EEL helper methods in Fusion

## 1.10.0

### Added

- Support NodeTypes in subfolders
- Suppress PHP unused declaration inspection for Flow magic methods

## 1.9.0

### Added

- Provide JSON schema for cache configuration (https://github.com/Sebobo/Shel.Neos.Schema)

### Fixed

- NullPointerException when opening files with embedded YAML

## 1.8.1

### Added

- Provide JSON schema for node migrations (https://github.com/Sebobo/Shel.Neos.Schema)

## 1.8.0

### Added

- AFX tag name synchronization
- Symbol contributors for node types and Fusion prototypes

## 1.7.1

### Fixed

- Empty EEL in AFX tag content no longer throws an exception
- NullPointerException when using node type references

## 1.7.0

### Added

- AFX is now a custom language. If you changed the injection settings, make sure "Neos Afx" is used
- Handle EEL in AFX
- Provide JSON schema for Node Types (https://github.com/Sebobo/Shel.Neos.Schema)
- Errors can now be submitted
- Node type references in childNodes and editorOptions.nodeTypes
- Suppress unknown attribute inspection for AFX meta attributes and prototypes
- Suppress unknown namespace inspection for AFX
- Suppress unknown target inspection for AFX

## 1.6.1

### Fixed

- Exception when using completion for prototypes

## 1.6.0

### Added

- Rename refactoring for Fusion prototypes
- Find usages of Fusion prototypes

### Fixed

- Index split settings files (Settings.*.yaml)
- Weird behavior when trying to navigate to prototype definitions (sometimes only the namespace was clickable)

## 1.5.0

### Added

- Completion for inline editor options in node type definitions
- Live template context for Fusion file type
