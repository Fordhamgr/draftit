with open('app/build.gradle.kts', 'rb') as f:
    content = f.read()
if b'}t e s t' in content:
    content = content.split(b'}t e s t')[0] + b'}'
with open('app/build.gradle.kts', 'wb') as f:
    f.write(content + b'\ndependencies { testImplementation("io.mockk:mockk:1.13.9"); testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3") }\n')
