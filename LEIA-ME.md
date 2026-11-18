# Zera Tela

App Android que limita o tempo diário de uso de apps e bloqueia a abertura quando o limite passa.
Nome: Zera Tela · pacote: com.zera.tela · Android 8.0 ou superior.

## Gerar o APK (sem Android Studio)
1. Crie um repositório no GitHub e envie o conteúdo desta pasta (inclusive `.github`).
2. Aba **Actions** > **Gerar APK** > rode (ou espere o push terminar).
3. Ao fim, baixe o artefato **ZeraTela-apk** e extraia o `app-debug.apk`.
4. Envie o APK para o celular e instale (permita "instalar apps desconhecidos" se pedir).

Com Android Studio: abra a pasta, espere o sync e use Build > Build APK.

## Primeiro uso
1. Abra o app e toque em **Permitir acesso ao uso**; ative Zera Tela.
2. Toque em **Ativar o bloqueio**, abra Apps instalados > Zera Tela e ative.
   Se estiver cinza (Android 13+): Configurações > Apps > Zera Tela > menu ⋮ > Permitir configurações restritas.
3. **Adicionar apps**: marque um ou vários, defina horas/minutos e toque em Adicionar.
   Toque num app da lista para mudar o limite dele.

## Como funciona
- O uso é medido pelo histórico de eventos do sistema (Acesso ao uso), desde 00:00, e zera à meia-noite.
- O serviço de acessibilidade vê qual app abriu. Se passou do limite, manda para a tela inicial e cobre a tela com uma das 20 frases (sorteada a cada tentativa, sem repetir a anterior). Frases em `Phrases.kt`.
- Se o limite estourar com o app aberto, ele é bloqueado em até 3 segundos.
- Nada é lido da tela e nada sai do aparelho.

## Limitações
- Quem desativa o serviço de acessibilidade ou desinstala o app remove o bloqueio.
- Em Xiaomi, Samsung etc., desative a otimização de bateria do Zera Tela para o sistema não encerrar o serviço.

## Atualizar o app
1. Suba o novo `ZeraTela.zip` no repositório (Add file > Upload files, mesmo nome) e confirme.
2. Espere o Actions ficar verde e baixe o ZeraTela-apk.
3. Instale o novo `app-debug.apk` por cima do atual. Os limites ficam salvos.
   (Desde a versão 1.1 o APK usa uma assinatura fixa. Se o Android recusar a instalação por conflito,
   desinstale a versão antiga uma vez e instale de novo; depois as atualizações entram por cima.)
