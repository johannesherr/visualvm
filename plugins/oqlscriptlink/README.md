# OQL Development Tools Plugin

This VisualVM plugin enhances the OQL Console and Objects view with productivity features for heap dump analysis.

## Features

### OQL Console Enhancements

1. **Link to File** - One-time file selection to link the OQL Console to an external `.oql` or `.js` script file
2. **Execute Linked File** - Executes the current content of the linked file without modifying the editor
3. **Auto-run on Save** - Automatically re-executes the script when the file is saved (uses Java WatchService)
4. **Clear Link** - Removes the file link

### Objects View Enhancements

5. **Copy Object ID** - Right-click on any object → "Copy Object ID for OQL" copies the memory address in hex format (e.g., `0x7f8a4c001000`) for use with `heap.findObject()`
6. **Copy Property Path** - Right-click on any property → "Copy Property Path for OQL" copies the full property path (e.g., `foo.bar.items[2].name`) for use in OQL queries

### Threads View Enhancements

7. **Copy Cleaned Thread Dump** - Button in toolbar that copies the thread dump to clipboard with "local variable:" lines removed for compatibility with IntelliJ's "Analyze Stack Trace or Thread Dump..." feature

## Usage

### OQL Console - Live Script Development

1. Open a heap dump in VisualVM
2. Navigate to the OQL Console tab
3. Look for the new buttons in the vertical toolbar (left side of the editor):
   - **Link to File...** - Opens a file dialog to select your OQL script file
   - **Execute Linked File** - Runs the current content of the linked file
   - **Auto-run on Save** - Checkbox to enable automatic execution on file save
   - **Clear Link** - Removes the link to the file

### Objects View - Copy Object ID

1. Navigate to the Objects view
2. Expand the tree to find an object of interest
3. Right-click on the object node
4. Select **"Copy Object ID for OQL"**
5. In OQL Console, use: `heap.findObject("0x7f8a4c001000")` (paste your copied ID)

### Objects View - Copy Property Path

1. Navigate to the Objects view
2. Expand an object's fields (e.g., expand `myObject` → `items` → `[2]` → `name`)
3. Right-click on any child property in the tree
4. Select **"Copy Property Path for OQL"**
5. In OQL Console, use the path: `select o.items[2].name from com.example.MyClass o`

### Threads View - Copy Cleaned Thread Dump

1. Navigate to the Threads view
2. Switch to HTML/text mode (if not already in that view)
3. Click the **Copy** button in the toolbar (💾 icon)
4. The thread dump is copied to clipboard with "local variable:" lines removed
5. In IntelliJ IDEA, use **Analyze → Analyze Stack Trace or Thread Dump...**
6. Press **Ctrl+V** (Cmd+V on Mac) to paste the cleaned thread dump
7. IntelliJ will correctly parse and display the thread dump

**Why this is needed:** VisualVM's thread dump includes local variable information that IntelliJ's analyzer doesn't understand. This feature strips those lines automatically.

## Workflow Examples

### Traditional Workflow (without plugin)
1. Browse Objects view to find interesting object
2. Manually note down object structure
3. Type out property paths in OQL Console
4. Edit OQL script in external editor
5. Copy script content
6. Paste into OQL Console
7. Click Run
8. Repeat for every change

### New Workflow (with plugin) ✨
1. Browse Objects view to find interesting object
2. Right-click → **Copy Object ID** → paste into OQL to find specific instance
3. Right-click on properties → **Copy Property Path** → paste into OQL query
4. Create OQL script file in your IDE
5. In OQL Console: **Link to File...** → select your script
6. Enable **Auto-run on Save**
7. Edit script in IDE → **Save** → Instant results in VisualVM!

### Complete Example

**Scenario:** You want to analyze all `User` objects and find ones with specific email domains.

1. **Explore in Objects view:**
   - Find a `User` instance
   - Right-click → "Copy Object ID" → `0x7f8a4c001000`
   - Expand to `email` field
   - Right-click on `email` → "Copy Property Path" → `email`

2. **Test in OQL Console:**
   ```javascript
   // Find the specific user first
   heap.findObject("0x7f8a4c001000")

   // Verify the property path works
   select u.email from com.example.User u
   ```

3. **Develop complex query in IDE:**
   - Create `analyze-users.oql` in VS Code
   - Link OQL Console to this file
   - Enable auto-run on save

   ```javascript
   // analyze-users.oql
   select {
     user: u,
     email: u.email,
     domain: u.email.toString().split("@")[1]
   }
   from com.example.User u
   where u.email.toString().contains("@example.com")
   ```

4. **Iterate rapidly:**
   - Edit query in VS Code
   - Save → Auto-executes in VisualVM
   - Refine query based on results
   - No copy-paste needed!

## Technical Details

### OQL Console Features
- Uses reflection to access OQL Console internals
- File watching implemented using Java's `WatchService` API
- Script execution happens without modifying the OQL Console editor content
- File link maintained per OQL Console instance (per heap dump)
- Links are not persisted between VisualVM restarts

### Objects View Features
- Context menu actions registered via `HeapViewerNodeAction.Provider`
- Object IDs retrieved using `Instance.getInstanceId()` and formatted as hex
- Property paths built by traversing node hierarchy from leaf to root
- Handles fields, array indices, and nested properties
- Uses system clipboard for copy operations

### Threads View Features
- Button added to JavaThreadsView toolbar via enhancer pattern
- Thread dump text extracted from HTMLView component using reflection
- HTML tags stripped and entities decoded
- Lines starting with "local " (trimmed) are filtered out
- Compatible with IntelliJ IDEA's "Analyze Stack Trace or Thread Dump..." feature
- Uses system clipboard for copy operations

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
