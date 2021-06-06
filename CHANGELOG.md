<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# IntelliJ Neos Changelog

## [Unreleased]
### Added
- AFX tag name synchronization
- Symbol contributors for node types and Fusion prototypes

## [1.7.1]
### Fixed
- Empty EEL in AFX tag content no longer throws an exception
- NullPointerException when using node type references

### Security
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