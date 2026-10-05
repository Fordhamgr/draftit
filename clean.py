import io
with open('app/build.gradle.kts', 'rb') as f:
    lines = f.readlines()

clean_lines = []
for l in lines:
    if b'\x00' in l:
        continue
    if b'dependencies { testImplementation("io.mockk:mockk:1.13.9");' in l:
        continue
    clean_lines.append(l)

with open('app/build.gradle.kts', 'wb') as f:
    f.writelines(clean_lines)
    f.write(b'\ndependencies { \n    testImplementation("io.mockk:mockk:1.13.11")\n    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")\n}\n')
