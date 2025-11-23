# OQL Script Link Plugin

This VisualVM plugin enhances the OQL Console with the ability to link to external script files for live development.

## Features

1. **Link to File** - One-time file selection to link the OQL Console to an external `.oql` or `.js` script file
2. **Execute Linked File** - Executes the current content of the linked file without modifying the editor
3. **Auto-run on Save** - Automatically re-executes the script when the file is saved (uses Java WatchService)
4. **Clear Link** - Removes the file link

## Usage

1. Open a heap dump in VisualVM
2. Navigate to the OQL Console tab
3. Look for the new buttons in the vertical toolbar (left side of the editor):
   - **Link to File...** - Opens a file dialog to select your OQL script file
   - **Execute Linked File** - Runs the current content of the linked file
   - **Auto-run on Save** - Checkbox to enable automatic execution on file save
   - **Clear Link** - Removes the link to the file

## Workflow

### Traditional Workflow (without plugin)
1. Edit script in external editor
2. Copy script content
3. Paste into OQL Console
4. Click Run
5. Repeat for every change

### New Workflow (with plugin)
1. Click "Link to File..." and select your script file
2. Enable "Auto-run on Save"
3. Edit script in your favorite IDE (IntelliJ, VS Code, etc.)
4. Save file → Script automatically executes in VisualVM!

## Technical Details

- The plugin uses reflection to access OQL Console internals
- File watching is implemented using Java's `WatchService` API
- Script execution happens without modifying the OQL Console editor content
- The link is maintained per OQL Console instance (per heap dump)
- Links are not persisted between VisualVM restarts

## Building

From the `/plugins` directory:
```bash
ant build
```

Or build all of VisualVM from the root directory:
```bash
ant build
```

## Installation

The plugin is automatically included when building VisualVM with the plugin included in the build.

## Compatibility

- Requires VisualVM with OQL Console support
- Java 8+
- Tested with VisualVM 2.x

## License

This code follows the same license as VisualVM (GPL v2 with Classpath exception).
