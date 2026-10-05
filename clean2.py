import io
with open('app/build.gradle.kts', 'rb') as f:
    lines = f.readlines()

content = b''.join(lines)
if b'}' not in content.split(b'compilerOptions')[1]:
    # We are missing the closing brace for configureEach
    content = content.replace(b'dependencies { \n', b'}\ndependencies { \n')

with open('app/build.gradle.kts', 'wb') as f:
    f.write(content)
