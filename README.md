# WAS Studio Android

Aplicativo Android para combinar a animação principal de uma figurinha `.was` com o efeito secundário de outra, preservando os respectivos tokens.

## Compilação

GitHub Actions: abra **Actions → Build Android APK → Run workflow** e selecione a branch `main`. O artefato gerado é `WASStudioAndroid-debug-apk`.

Também pode compilar localmente com JDK 17, Android SDK 35 e Gradle 8.9: `gradle assembleDebug`.

## Limitações

Protótipo offline, sem integração Baileys e sem geração de novas assinaturas ES256. A compatibilidade com o WhatsApp ainda precisa ser testada.
