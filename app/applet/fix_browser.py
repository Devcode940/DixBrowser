with open('src/main/java/com/example/BrowserScreen.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
in_settings_item = False
brace_count = 0

for i, line in enumerate(lines):
    if "fun SettingsItem" in line:
        in_settings_item = True
    
    if in_settings_item:
        new_lines.append(line)
        brace_count += line.count('{') - line.count('}')
        if brace_count == 0 and "}" in line:
            in_settings_item = False
            break
    else:
        new_lines.append(line)

with open('src/main/java/com/example/BrowserScreen.kt', 'w') as f:
    f.writelines(new_lines)

print("Cleaned up BrowserScreen.kt successfully")
